package com.example.blogapp;

import java.util.Date;

public class Comments {

    private  String message,User_id;
    private Date timestamp;

    public  Comments()
    {

    }

    public Comments(String message, String user_id, Date timestamp) {
        this.message = message;
        User_id = user_id;
        this.timestamp = timestamp;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getUser_id() {
        return User_id;
    }

    public void setUser_id(String user_id) {
        User_id = user_id;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
}
