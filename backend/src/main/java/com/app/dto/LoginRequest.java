package com.app.dto;

public class LoginRequest {
    private String email;
    private String password;

    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public void setEmail(String v) { this.email = v; }
    public void setPassword(String v) { this.password = v; }
}
