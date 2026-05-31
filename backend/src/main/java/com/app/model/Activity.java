package com.app.model;

import javax.persistence.*;

@Entity
public class Activity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String userName;
    private String activityDate;
    private String status;
    private String type;

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getUserName() { return userName; }
    public String getActivityDate() { return activityDate; }
    public String getStatus() { return status; }
    public String getType() { return type; }

    public void setName(String v) { this.name = v; }
    public void setUserName(String v) { this.userName = v; }
    public void setActivityDate(String v) { this.activityDate = v; }
    public void setStatus(String v) { this.status = v; }
    public void setType(String v) { this.type = v; }
}
