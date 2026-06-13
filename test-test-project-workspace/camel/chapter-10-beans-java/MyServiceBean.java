package com.example;

public class MyServiceBean {
    private String prefix = "DEFAULT";

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String formatMessage(String body) {
        return "[" + prefix + "] >> " + body;
    }
}
