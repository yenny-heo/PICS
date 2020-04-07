package com.yeeun.pics;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class CalendarActivity extends AppCompatActivity {

    private com.google.api.services.calendar.Calendar mService = null;

    private TextView calendarID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        calendarID = (TextView) findViewById(R.id.calendar_id);

        Intent intent = getIntent();
        String  id = intent.getStringExtra("CalendarId");
        calendarID.setText(id);



        Event event = new Event()
                .setSummary("일정")
                .setLocation("서울시")
                .setDescription("캘린더에 이벤트 추가하는 것을 테스트합니다.");

        java.util.Calendar calander;

        calander = java.util.Calendar.getInstance();
        //simpledateformat = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ssZ", Locale.KOREA);
        // Z에 대응하여 +0900이 입력되어 문제 생겨 수작업으로 입력
        SimpleDateFormat simpledateformat = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss+09:00", Locale.KOREA);
        String datetime = simpledateformat.format(calander.getTime());

        DateTime startDateTime = new DateTime(datetime);
        EventDateTime start = new EventDateTime()
                .setDateTime(startDateTime)
                .setTimeZone("Asia/Seoul");
        event.setStart(start);

        Log.d( "@@@", datetime );


        DateTime endDateTime = new  DateTime(datetime);
        EventDateTime end = new EventDateTime()
                .setDateTime(endDateTime)
                .setTimeZone("Asia/Seoul");
        event.setEnd(end);

        //String[] recurrence = new String[]{"RRULE:FREQ=DAILY;COUNT=2"};
        //event.setRecurrence(Arrays.asList(recurrence));


        try {
            event = mService.events().insert(id, event).execute();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("Exception", "Exception : " + e.toString());
        }
        System.out.printf("Event created: %s\n", event.getHtmlLink());
        Log.e("Event", "created : " + event.getHtmlLink());
        String eventStrings = "created : " + event.getHtmlLink();
    }
}
