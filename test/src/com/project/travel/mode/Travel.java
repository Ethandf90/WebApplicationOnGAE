package com.project.travel.mode;

import static com.project.travel.service.OfyService.ofy;

import com.googlecode.objectify.condition.IfNotDefault;
import com.google.api.server.spi.config.AnnotationBoolean;
import com.google.api.server.spi.config.ApiResourceProperty;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Parent;
import com.project.travel.form.ProfileForm.Gender;
import com.project.travel.form.TravelForm;

import java.util.Calendar;
import java.util.Date;
import java.util.List;


@Entity
@Cache
public class Travel {

    private static final String DEFAULT_CITY = "Default City";
    private static final List<String> DEFAULT_TOPICS = ImmutableList.of("Default", "Topic");

    @Id
    private long id;

    @Index
    private String name;

    private String description;

    @Parent
    @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
    private Key<Profile> profileKey;

    @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
    private String organizerUserId;

    @Index
    private List<String> topics;

    @Index(IfNotDefault.class) private String city;

    private Date startDate;

    private Date endDate;

    @Index
    private int month;

    @Index
    private int maxAttendees;

    @Index
    private int seatsAvailable;
    
    private Gender organizerGender;
    private String organizerPhone;

    private Travel() {}

    public Travel(final long id, final String organizerUserId,
                      final TravelForm travelForm, final Gender organizerGender, final String organizerPhone) {
        Preconditions.checkNotNull(travelForm.getName(), "The name is required");
        this.id = id;
        this.profileKey = Key.create(Profile.class, organizerUserId);
        this.organizerUserId = organizerUserId;
        updateWithTravelForm(travelForm);
        this.organizerGender = organizerGender;
        this.organizerPhone = organizerPhone;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
    public Key<Profile> getProfileKey() {
        return profileKey;
    }

    // Get a String version of the key
    public String getWebsafeKey() {
        return Key.create(profileKey, Travel.class, id).getString();
    }

    @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
    public String getOrganizerUserId() {
        return organizerUserId;
    }


    public String getOrganizerDisplayName() {
        // Profile organizer = ofy().load().key(Key.create(Profile.class, organizerUserId)).now();
        Profile organizer = ofy().load().key(getProfileKey()).now();
        if (organizer == null) {
            return organizerUserId;
        } else {
            return organizer.getDisplayName();
        }
    }
    
    public String getOrganizerMainMail(){
    	Profile organizer = ofy().load().key(getProfileKey()).now();
    	if (organizer == null) {
            return organizerUserId;
        } else {
            return organizer.getMainEmail();
        }
    }

    public List<String> getTopics() {
        return topics == null ? null : ImmutableList.copyOf(topics);
    }

    public String getCity() {
        return city;
    }

    public Date getStartDate() {
        return startDate == null ? null : new Date(startDate.getTime());
    }

    public Date getEndDate() {
        return endDate == null ? null : new Date(endDate.getTime());
    }

    public int getMonth() {
        return month;
    }

    public int getMaxAttendees() {
        return maxAttendees;
    }

    public int getSeatsAvailable() {
        return seatsAvailable;
    }
   
	public Gender getOrganizerGender() {
		return organizerGender;
	}

	public void setOrganizerGender(Gender organizerGender) {
		this.organizerGender = organizerGender;
	}
	
	public String getOrganizerPhone() {
		return organizerPhone;
	}

	//This method is used upon object creation as well as updating existing Travels.
    public void updateWithTravelForm(TravelForm travelForm) {
        this.name = travelForm.getName();
        this.description = travelForm.getDescription();
        List<String> topics = travelForm.getTopics();
        this.topics = topics == null || topics.isEmpty() ? DEFAULT_TOPICS : topics;
        this.city = travelForm.getCity() == null ? DEFAULT_CITY : travelForm.getCity();

        Date startDate = travelForm.getStartDate();
        this.startDate = startDate == null ? null : new Date(startDate.getTime());
        Date endDate = travelForm.getEndDate();
        this.endDate = endDate == null ? null : new Date(endDate.getTime());
        if (this.startDate != null) {
            // Getting the starting month for a composite query.
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(this.startDate);
            // Calendar.MONTH is zero based, so adding 1.
            this.month = calendar.get(calendar.MONTH) + 1;
        }
        // Check maxAttendees value against the number of already allocated seats.
        int seatsAllocated = maxAttendees - seatsAvailable;
        if (travelForm.getMaxAttendees() < seatsAllocated) {
            throw new IllegalArgumentException(seatsAllocated + " seats are already allocated, "
                    + "but you tried to set maxAttendees to " + travelForm.getMaxAttendees());
        }
        // The initial number of seatsAvailable is the same as maxAttendees.
        // However, if there are already some seats allocated, we should subtract that numbers.
        this.maxAttendees = travelForm.getMaxAttendees();
        this.seatsAvailable = this.maxAttendees - seatsAllocated;
    }

    public void bookSeats(final int number) {
        if (seatsAvailable < number) {
            throw new IllegalArgumentException("There are no seats available.");
        }
        seatsAvailable = seatsAvailable - number;
    }

    public void giveBackSeats(final int number) {
        if (seatsAvailable + number > maxAttendees) {
            throw new IllegalArgumentException("The number of seats will exceeds the capacity.");
        }
        seatsAvailable = seatsAvailable + number;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder("Id: " + id + "\n")
                .append("Name: ").append(name).append("\n");
        if (city != null) {
            stringBuilder.append("City: ").append(city).append("\n");
        }
        if (topics != null && topics.size() > 0) {
            stringBuilder.append("Topics:\n");
            for (String topic : topics) {
                stringBuilder.append("\t").append(topic).append("\n");
            }
        }
        if (startDate != null) {
            stringBuilder.append("StartDate: ").append(startDate.toString()).append("\n");
        }
        if (endDate != null) {
            stringBuilder.append("EndDate: ").append(endDate.toString()).append("\n");
        }
        stringBuilder.append("Max Attendees: ").append(maxAttendees).append("\n");
        return stringBuilder.toString();
    }

}
