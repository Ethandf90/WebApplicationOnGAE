package com.project.travel.form;

import com.google.common.collect.ImmutableList;

import java.util.Date;
import java.util.List;


public class TravelForm {

    private String name;
    private String description;
    private List<String> topics;
    private String city;
    private Date startDate;
    private Date endDate;
    private int maxAttendees;

    private TravelForm() {}

    public TravelForm(String name, String description, List<String> topics, String city,
                          Date startDate, Date endDate, int maxAttendees) {
        this.name = name;
        this.description = description;
        this.topics = topics == null ? null : ImmutableList.copyOf(topics);
        this.city = city;
        this.startDate = startDate == null ? null : new Date(startDate.getTime());
        this.endDate = endDate == null ? null : new Date(endDate.getTime());
        this.maxAttendees = maxAttendees;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getTopics() {
        return topics;
    }

    public String getCity() {
        return city;
    }

    public Date getStartDate() {
        return startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public int getMaxAttendees() {
        return maxAttendees;
    }
}
