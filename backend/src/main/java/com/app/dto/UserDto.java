package com.app.dto;

import com.app.model.User;

public class UserDto {
    private Long   id;
    private String firstName;
    private String lastName;
    private String email;
    private String role;
    private String department;

    public UserDto(User u) {
        this.id         = u.getId();
        this.firstName  = u.getFirstName();
        this.lastName   = u.getLastName();
        this.email      = u.getEmail();
        this.role       = u.getRole();
        this.department = u.getDepartment();
    }

    public Long   getId()         { return id; }
    public String getFirstName()  { return firstName; }
    public String getLastName()   { return lastName; }
    public String getEmail()      { return email; }
    public String getRole()       { return role; }
    public String getDepartment() { return department; }
}
