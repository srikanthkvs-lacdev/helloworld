package com.app.dto;

public class SearchResultDto {
    private String id;
    private String name;
    private String type;
    private String date;
    private String status;

    public SearchResultDto(String id, String name, String type, String date, String status) {
        this.id     = id;
        this.name   = name;
        this.type   = type;
        this.date   = date;
        this.status = status;
    }

    public String getId()     { return id; }
    public String getName()   { return name; }
    public String getType()   { return type; }
    public String getDate()   { return date; }
    public String getStatus() { return status; }
}
