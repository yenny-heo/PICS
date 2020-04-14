package com.yeeun.pics;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;

import java.text.SimpleDateFormat;
import java.util.Date;

public class AddScheduleActivity extends AppCompatActivity {

    private EditText title;
    private DatePicker startDate;
    private DatePicker endDate;
    private Button submitBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_schedule);

        title = (EditText) findViewById(R.id.title);
        startDate = (DatePicker) findViewById(R.id.startDate);
        endDate = (DatePicker) findViewById(R.id.endDate);
        submitBtn = (Button) findViewById(R.id.submit);

        submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                String scheduleTitle = title.getText().toString();

                int startYear = startDate.getYear()-1900;
                int startMonth = startDate.getMonth();
                int startDay = startDate.getDayOfMonth();

                int endYear = endDate.getYear()-1900;
                int endMonth = endDate.getMonth();
                int endDay = endDate.getDayOfMonth();

                Date startDate = (Date)new Date(startYear, startMonth, startDay);
                Date endDate = (Date)new Date(endYear, endMonth, endDay);

                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");

                String startDateString = simpleDateFormat.format(startDate);
                String endDateString = simpleDateFormat.format(endDate);

                intent.putExtra("title", scheduleTitle);
                intent.putExtra("startDate", startDateString);
                intent.putExtra("endDate", endDateString);

                setResult(0, intent);
                finish();
            }
        });
    }
}
