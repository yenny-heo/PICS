package com.yeeun.pics;

public class EventData {
    private String title;

    private int startYear;
    private int startMonth;
    private int startDay;

    private int startHour;
    private int startMin;

    private int endHour;
    private int endMin;
    public EventData(String title,int startYear, int startMonth, int startDay, int startHour, int startMin, int endHour, int endMin){
        this.title = title;

        this.startYear = startYear;
        this.startMonth = startMonth;
        this.startDay = startDay;

        this.startHour = startHour;
        this.startMin = startMin;

        this.endHour = endHour;
        this.endMin = endMin;
    }
    public String getTitle(){
        return this.title;
    }

    public int getStartYear() {
        return this.startYear;
    }

    public int getStartMonth() {
        return this.startMonth;
    }

    public int getStartDay() {
        return this.startDay;
    }

    public int getStartHour(){
        return this.startHour;
    }

    public int getStartMin(){
        return this.startMin;
    }

    public int getEndHour(){
        return this.endHour;
    }

    public int getEndMin(){
        return this.endMin;
    }

}
