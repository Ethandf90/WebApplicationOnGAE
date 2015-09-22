package com.project.travel.form;

// Pojo representing a profile form on the client side.
public class ProfileForm {

    private String displayName,phone,age;

    private Gender gender;
    private Interest interest;
    
    private String imagekey;
       
    
//    private String 

    private ProfileForm () {}

    public ProfileForm(String displayName, Gender gender, Interest interest, String homecity,String age) {
        this.displayName = displayName;
        this.gender = gender;
        this.imagekey = "";
    }
    
    public ProfileForm(String displayName, Gender gender, Interest interest, String homecity,String age, String imagekey) {
    	this.displayName = displayName;
        this.gender = gender;
        this.imagekey = imagekey;
    }
    
    
    public String getDisplayName() {
        return displayName;
    }
    public Gender getGender() {
        return gender;
    } 
    
    public Interest getinterest() {
        return interest;
    } 
    
    public static enum Gender {
    	NOT_SPECIFIED,
    	Male,
        Female,
        Other        
    }   
    public static enum Interest {
    	NOT_SPECIFIED,
    	Toronto,
        London,
        LA,
        Pyongyang,
        Hongkong,
        Chennai,
        
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
}
