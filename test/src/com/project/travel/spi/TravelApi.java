package com.project.travel.spi;

import static com.project.travel.service.OfyService.factory;
import static com.project.travel.service.OfyService.ofy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Named;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.api.server.spi.response.ConflictException;
import com.google.api.server.spi.response.ForbiddenException;
import com.google.api.server.spi.response.NotFoundException;
import com.google.api.server.spi.response.UnauthorizedException;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.appengine.api.users.User;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Work;
import com.googlecode.objectify.cmd.Query;
import com.project.travel.Constants;
import com.project.travel.form.ProfileForm.Interest;
import com.project.travel.form.TravelForm;
import com.project.travel.form.TravelQueryForm;
import com.project.travel.form.ProfileForm;
import com.project.travel.form.ProfileForm.Gender;
import com.project.travel.mode.Travel;
import com.project.travel.mode.Profile;


//Defines travel APIs.
@Api(
		name = "travel", version = "v1",
        scopes = { Constants.EMAIL_SCOPE }, 
        clientIds = {  Constants.WEB_CLIENT_ID,  Constants.IOS_CLIENT_ID,
        				Constants.API_EXPLORER_CLIENT_ID },
//        audiences = {Constants.IOS_AUDIENCE},
        description = "API for the Backend application."
	)
public class TravelApi {

//    private static final Boolean True = null;
//    private static final Boolean False = null;

    private static String extractDefaultDisplayNameFromEmail(String email) {
        return email == null ? null : email.substring(0, email.indexOf("@"));
    }

    @ApiMethod(name = "saveProfile", path = "profile", httpMethod = HttpMethod.POST)
    // The request that invokes this method should provide data that conforms to the fields defined in ProfileForm
    public Profile saveProfile(final User user, ProfileForm profileForm)
            throws UnauthorizedException {

        if (user == null) {
            throw new UnauthorizedException("Authorization required");
        }

        String mainEmail = user.getEmail();
        String userId = user.getUserId();

        String displayName = profileForm.getDisplayName();
        Gender gender = profileForm.getGender();
        Interest interest = profileForm.getinterest();
        String phone = profileForm.getphone();
        String age = profileForm.getage();
        
        String imagekey = profileForm.getImagekey();

        Profile profile = ofy().load().key(Key.create(Profile.class, userId))
                .now();

        if (profile == null) {
            if (displayName == null) {
                displayName = extractDefaultDisplayNameFromEmail(user
                        .getEmail());
            }
            if (gender == null) {
            	gender = Gender.NOT_SPECIFIED;
            }
            
            if (interest == null) {
            	interest = Interest.NOT_SPECIFIED;
            }
            
            if (phone == null) {
            	phone = "";
            }
            
            if (age == null) {
            	age = "";
            }
            
            if(imagekey == null){
        		imagekey = "";
        	    }
            // Now create a new Profile entity
            profile = new Profile(userId, displayName, mainEmail, gender, interest, phone, age, imagekey);
        } else {
            // The Profile entity already exists
            // Update the Profile entity
            profile.update(displayName, gender, interest, phone, age, imagekey);
        }

        // Save the entity in the datastore
        ofy().save().entity(profile).now();

        // Return the profile
        return profile;
    }

    //Returns a Profile object associated with the given user object. The cloud endpoints system automatically inject the User object.
    @ApiMethod(name = "getProfile", path = "profile", httpMethod = HttpMethod.GET)
    public Profile getProfile(final User user) throws UnauthorizedException {
        if (user == null) {
            throw new UnauthorizedException("Authorization required");
        }

        // load the Profile Entity
        String userId = user.getUserId();
        Key key = Key.create(Profile.class, userId);

        Profile profile = (Profile) ofy().load().key(key).now();
        return profile;
    }


    private static Profile getProfileFromUser(User user) {
        // First fetch the user's Profile from the datastore.
        Profile profile = ofy().load().key(
                Key.create(Profile.class, user.getUserId())).now();
        if (profile == null) {
            // Create a new Profile if it doesn't exist.
            // Use default displayName and gender
            String email = user.getEmail();
            String phone = null;
            String age = null;
			profile = new Profile(user.getUserId(),
                    extractDefaultDisplayNameFromEmail(email), email, Gender.NOT_SPECIFIED, 
                    Interest.NOT_SPECIFIED, phone, age);
        }
        return profile;
    }

    // UnauthorizedException when the user is not signed in.
    @ApiMethod(name = "createTravel", path = "travel", httpMethod = HttpMethod.POST)
    public Travel createTravel(final User user, final TravelForm travelForm)
        throws UnauthorizedException {
        if (user == null) {
            throw new UnauthorizedException("Authorization required");
        }
        // Allocate Id first, in order to make the transaction idempotent.
        final String userId = user.getUserId();
        Key<Profile> profileKey = Key.create(Profile.class, userId);
        final Key<Travel> travelKey = factory().allocateId(profileKey, Travel.class);
        final long travelId = travelKey.getId();
        final Queue queue = QueueFactory.getDefaultQueue();
        
        ///////////////////////////////////////////////////////
        List<String> emails = new ArrayList<>();
        
        List<Profile> interestedUsers = getInterestedUsers(travelForm.getCity());
        Profile userProfile = getProfileFromUser(user);
        for(Profile interestedUser: interestedUsers){
//        	emails.add(interestedUser.getMainEmail());
            if(!interestedUser.equals(userProfile)){
            	//delete the organizer from the list
                emails.add(interestedUser.getMainEmail());
            }
        }
        //only string can be sent as parameter
        final String emailString = emails.toString();
        //////////////////////////////////////////////////////////////////
        
        // Start a transaction.
        Travel travel = ofy().transact(new Work<Travel>() {
            //Override the run()
            public Travel run() {
                // Fetch user's Profile.
                Profile profile = getProfileFromUser(user);
                Gender gender = profile.getGender();
                String phone = profile.getphone();
                
                
                Travel travel = new Travel(travelId, userId, travelForm, gender, phone);
                // Save Travel and Profile.
                ofy().save().entities(travel, profile).now();
                //send to the organizer
                queue.add(ofy().getTransaction(),
                        TaskOptions.Builder.withUrl("/tasks/send_confirmation_email")
                        .param("email", profile.getMainEmail())
                        .param("travelInfo", travel.toString()));
                //send to those whose interest is the travel city
                queue.add(ofy().getTransaction(),
                        TaskOptions.Builder.withUrl("/tasks/send_invitation_email")
                        .param("email", emailString)
                        .param("organizer", profile.getDisplayName())
                        .param("travelInfo", travel.toString()));
                return travel;
            }
        });
        return travel;
    }
    
    ///////////////
    public List<Travel> filterPlayground() {
        // Query<Travel> query = ofy().load().type(Travel.class).order("name");
        Query<Travel> query = ofy().load().type(Travel.class);

     // If we have any inequality filters, order by the field first.
        query = query.filter("city =", "London").filter("seatsAvailable <", 30).
                filter("seatsAvailable >" , 0).order("seatsAvailable").order("name").
                order("month");

        return query.list();
    }
    
    private List<Profile> getInterestedUsers(String interest){
    	Query<Profile> query = ofy().load().type(Profile.class);
    	query = query.filter("interest =", interest).order("displayName");
    	
    	return query.list();
    }


    @ApiMethod(
            name = "queryTravels_nofilters",
            path = "queryTravels_nofilters",
            httpMethod = HttpMethod.POST
    )
    public List<Travel> queryTravels_nofilters() {
        // Find all entities of type Travel
        Query<Travel> query = ofy().load().type(Travel.class).order("name");

        return query.list();
    }

     //Queries against the datastore with the given filters and returns the result.
     // Normally this kind of method is supposed to get invoked by a GET HTTP method,
     //  do it with POST in order to receive travelQueryForm Object via the POST body!!!!!!!!
    @ApiMethod(
            name = "queryTravels",
            path = "queryTravels",
            httpMethod = HttpMethod.POST
    )
    public List<Travel> queryTravels(TravelQueryForm travelQueryForm) {
        Iterable<Travel> travelIterable = travelQueryForm.getQuery();
        List<Travel> result = new ArrayList<>(0);
        List<Key<Profile>> organizersKeyList = new ArrayList<>(0);
        for (Travel travel : travelIterable) {
            organizersKeyList.add(Key.create(Profile.class, travel.getOrganizerUserId()));
            result.add(travel);
        }
        // To avoid separate datastore gets for each Travel, pre-fetch the Profiles.
        ofy().load().keys(organizersKeyList);
        return result;
    }



     //Returns a list of Travels that the user created.
     //In order to receive the websafeTravelKey via the JSON params, uses a POST method.
    @ApiMethod(
            name = "getTravelsCreated",
            path = "getTravelsCreated",
            httpMethod = HttpMethod.POST
    )
    public List<Travel> getTravelsCreated(final User user) throws UnauthorizedException {
        // If not signed in, throw a 401 error.
        if (user == null) {
            throw new UnauthorizedException("Authorization required");
        }
        String userId = user.getUserId();
        Key<Profile> userKey = Key.create(Profile.class, userId);
        return ofy().load().type(Travel.class)
                .ancestor(userKey)
                .order("name").list();
    }

    /**
     * Just a wrapper for Boolean.
     * We need this wrapped Boolean because endpoints functions must return
     * an object instance, they can't return a Type class such as
     * String or Integer or Boolean
     */
    public static class WrappedBoolean {

        private final Boolean result;
        private final String reason;

        public WrappedBoolean(Boolean result) {
            this.result = result;
            this.reason = "";
        }

        public WrappedBoolean(Boolean result, String reason) {
            this.result = result;
            this.reason = reason;
        }

        public Boolean getResult() {
            return result;
        }

        public String getReason() {
            return reason;
        }
    }

    // 	Returns a Travel object with the given travelId.
    @ApiMethod(
            name = "getTravel",
            path = "travel/{websafeTravelKey}",
            httpMethod = HttpMethod.GET
    )
    public Travel getTravel(
            @Named("websafeTravelKey") final String websafeTravelKey)
            throws NotFoundException {
        Key<Travel> travelKey = Key.create(websafeTravelKey);
        Travel travel = ofy().load().key(travelKey).now();
        if (travel == null) {
            throw new NotFoundException("No Travel found with key: " + websafeTravelKey);
        }
        return travel;
    }

    @ApiMethod(
            name = "registerForTravel",
            path = "travel/{websafeTravelKey}/registration",
            httpMethod = HttpMethod.POST
    )

    public WrappedBoolean registerForTravel(final User user,
            @Named("websafeTravelKey") final String websafeTravelKey)
            throws UnauthorizedException, NotFoundException,
            ForbiddenException, ConflictException {
        // If not signed in, throw a 401 error.
        if (user == null) {
            throw new UnauthorizedException("Authorization required");
        }

        // Get the userId
        final String userId = user.getUserId();

        WrappedBoolean result = ofy().transact(new Work<WrappedBoolean>() {
            @Override
            public WrappedBoolean run() {
                try {

                // Get the travel key
                // Will throw ForbiddenException if the key cannot be created
                Key<Travel> travelKey = Key.create(websafeTravelKey);

                // Get the Travel entity from the datastore
                Travel travel = ofy().load().key(travelKey).now();

                // 404 when there is no Travel with the given travelId.
                if (travel == null) {
                    return new WrappedBoolean (false,
                            "No Travel found with key: "
                                    + websafeTravelKey);
                }

                // Get the user's Profile entity
                Profile profile = getProfileFromUser(user);

                // Has the user already registered to attend this travel?
                if (profile.getTravelKeysToAttend().contains(
                        websafeTravelKey)) {
                    return new WrappedBoolean (false, "Already registered");
                } else if (travel.getSeatsAvailable() <= 0) {
                    return new WrappedBoolean (false, "No seats available");
                } else {
                    // All looks good, go ahead and book the seat
                    profile.addToTravelKeysToAttend(websafeTravelKey);
                    travel.bookSeats(1);

                    // Save the Travel and Profile entities
                    ofy().save().entities(profile, travel).now();
                    // We are booked!
                    return new WrappedBoolean(true, "Registration successful");
                }

                }
                catch (Exception e) {
                    return new WrappedBoolean(false, "Unknown exception");

                }
            }
        });
        // if result is false
        if (!result.getResult()) {
            if (result.getReason().contains("No Travel found with key")) {
                throw new NotFoundException (result.getReason());
            }
            else if (result.getReason() == "Already registered") {
                throw new ConflictException("You have already registered");
            }
            else if (result.getReason() == "No seats available") {
                throw new ConflictException("There are no seats available");
            }
            else {
                throw new ForbiddenException("Unknown exception");
            }
        }
        return result;
    }

    @ApiMethod(
            name = "unregisterFromTravel",
            path = "travel/{websafeTravelKey}/registration",
            httpMethod = HttpMethod.DELETE
    )
    public WrappedBoolean unregisterFromTravel(final User user,
                                            @Named("websafeTravelKey")
                                            final String websafeTravelKey)
            throws UnauthorizedException, NotFoundException, ForbiddenException, ConflictException {
        // If not signed in, throw a 401 error.
        if (user == null) {
            throw new UnauthorizedException("Authorization required");
        }

        WrappedBoolean result = ofy().transact(new Work<WrappedBoolean>() {
            @Override
            public WrappedBoolean run() {
                Key<Travel> travelKey = Key.create(websafeTravelKey);
                Travel travel = ofy().load().key(travelKey).now();
                // 404 when there is no Travel with the given travelId.
                if (travel == null) {
                    return new  WrappedBoolean(false,
                            "No Travel found with key: " + websafeTravelKey);
                }

                // Un-registering from the Travel.
                Profile profile = getProfileFromUser(user);
                if (profile.getTravelKeysToAttend().contains(websafeTravelKey)) {
                    profile.unregisterFromTravel(websafeTravelKey);
                    travel.giveBackSeats(1);
                    ofy().save().entities(profile, travel).now();
                    return new WrappedBoolean(true);
                } else {
                    return new WrappedBoolean(false, "You are not registered for this travel");
                }
            }
        });
        // if result is false
        if (!result.getResult()) {
            if (result.getReason().contains("No Travel found with key")) {
                throw new NotFoundException (result.getReason());
            }
            else {
                throw new ForbiddenException(result.getReason());
            }
        }
        // NotFoundException is actually thrown here.
        return new WrappedBoolean(result.getResult());
    }

///////////////////////////////////
    @ApiMethod(
            name = "getTravelsToAttend",
            path = "getTravelsToAttend",
            httpMethod = HttpMethod.GET
    )
    public Collection<Travel> getTravelsToAttend(final User user)
            throws UnauthorizedException, NotFoundException {
        // If not signed in, throw a 401 error.
        if (user == null) {
            throw new UnauthorizedException("Authorization required");
        }
        Profile profile = ofy().load().key(Key.create(Profile.class, user.getUserId())).now();
        if (profile == null) {
            throw new NotFoundException("Profile doesn't exist.");
        }
        List<String> keyStringsToAttend = profile.getTravelKeysToAttend();
        List<Key<Travel>> keysToAttend = new ArrayList<>();
        for (String keyString : keyStringsToAttend) {
            keysToAttend.add(Key.<Travel>create(keyString));
        }
        return ofy().load().keys(keysToAttend).values();
    }
}
