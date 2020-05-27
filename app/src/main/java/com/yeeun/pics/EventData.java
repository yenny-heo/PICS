package com.yeeun.pics;

public class EventData {
    private int startHour;
    private int startMin;

    private int endHour;
    private int endMin;
    public EventData(int startHour, int startMin, int endHour, int endMin){
        this.startHour = startHour;
        this.startMin = startMin;
        this.endHour = endHour;
        this.endMin = endMin;
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
