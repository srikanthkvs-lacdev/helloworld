package com.app.dto;

public class LoginResponse {
    private boolean success;
    private String message;
    private UserDto user;

    public LoginResponse(boolean success, String message, UserDto user) {
        this.success = success;
        this.message = message;
        this.user    = user;
    }

    public boolean isSuccess()  { return success; }
    public String getMessage()  { return message; }
    public UserDto getUser()    { return user; }
}
