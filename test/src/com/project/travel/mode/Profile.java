package com.project.travel.mode;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.project.travel.form.ProfileForm.Gender;
import com.project.travel.form.ProfileForm.Interest;


//indicate that this class is an Entity
@Entity
@Cache
public class Profile {
	@Index
    String displayName;
    String mainEmail;
    Gender gender;
    @Index
    Interest interest; 
    String phone;
    String age;
    
    String imagekey;

    @Id String userId;

    private List<String> travelKeysToAttend = new ArrayList<>(0);

    public Profile (String userId, String displayName, String mainEmail, Gender gender, Interest interest, String phone,String age) {
        this.userId = userId;
        this.displayName = displayName;
        this.mainEmail = mainEmail;
        this.gender = gender;
        this.interest = interest;
        this.phone = phone;
        this.age = age;
    }
    
    public Profile (String userId, String displayName, String mainEmail, Gender gender, Interest interest, String phone,String age, String imagekey) {
        this.userId = userId;
        this.displayName = displayName;
        this.mainEmail = mainEmail;
        this.gender = gender;
        this.interest = interest;
        this.phone = phone;
        this.age = age;
        this.imagekey = imagekey;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getMainEmail() {
        return mainEmail;
    }

    public Gender getGender() {
        return gender;
    }

    public String getUserId() {
        return userId;
    }
    
    public Interest getInterest() {
        return interest;
    }
    public String getphone() {
        return phone;
    }
    public String getage() {
        return age;
    }
    
    public String getImagekey(){
       	return imagekey;
    }

    public void setImagekey(){
    	this.imagekey = imagekey;
    }

    public List<String> getTravelKeysToAttend() {
        return ImmutableList.copyOf(travelKeysToAttend);
    }


    private Profile() {}


    public void update(String displayName, Gender gender, Interest interest, String phone, String age, String imagekey) {
        if (displayName != null) {
            this.displayName = displayName;
        }
        if (gender != null) {
            this.gender = gender;
        }
        if (interest != null) {
            this.interest = interest;
        }
        if (phone != null) {
            this.phone = phone;
        }
        if (age != null) {
            this.age = age;
        }
        
        if (imagekey != null) {
            this.imagekey = imagekey;
        }
    }


    public void addToTravelKeysToAttend(String travelKey) {
        travelKeysToAttend.add(travelKey);
    }


    public void unregisterFromTravel(String travelKey) {
        if (travelKeysToAttend.contains(travelKey)) {
            travelKeysToAttend.remove(travelKey);
        } else {
            throw new IllegalArgumentException("Invalid travelKey: " + travelKey);
        }
    }

}
