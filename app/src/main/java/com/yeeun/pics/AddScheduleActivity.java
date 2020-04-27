package com.yeeun.pics;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TimePicker;

import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AddScheduleActivity extends AppCompatActivity {

    private EditText title;
    private DatePicker startDate;
    private DatePicker endDate;

    private TimePicker startTime;
    private TimePicker endTime;

    private Button submitBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_schedule);

        title = (EditText) findViewById(R.id.title);
        startDate = (DatePicker) findViewById(R.id.startDate);
        endDate = (DatePicker) findViewById(R.id.endDate);

        startTime = (TimePicker) findViewById(R.id.startTime);
        endTime = (TimePicker) findViewById(R.id.endTime);

        submitBtn = (Button) findViewById(R.id.submit);

        //날짜 설정
        startDate.updateDate(2020,03,22);
        //시간 설정
        startTime.setHour(0);
        startTime.setMinute(20);

        submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                String scheduleTitle = title.getText().toString();

                int startYear = startDate.getYear()-1900;
                int startMonth = startDate.getMonth();
                int startDay = startDate.getDayOfMonth();
                int startHour = startTime.getHour()-9;
                int startMinute = startTime.getMinute();

                int endYear = endDate.getYear()-1900;
                int endMonth = endDate.getMonth();
                int endDay = endDate.getDayOfMonth();
                int endHour = endTime.getHour()-9;
                int endMinute = endTime.getMinute();


                Date startDate = (Date)new Date(startYear, startMonth, startDay, startHour, startMinute);
                Date endDate = (Date)new Date(endYear, endMonth, endDay, endHour, endMinute);

                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

                String startDateString = simpleDateFormat.format(startDate);
                String endDateString = simpleDateFormat.format(endDate);

                System.out.println(startDateString);
                System.out.println(endDateString);

                intent.putExtra("title", scheduleTitle);
                intent.putExtra("startDate", startDateString);
                intent.putExtra("endDate", endDateString);

                setResult(0, intent);
                finish();
            }
        });
    }
}
