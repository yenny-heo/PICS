package com.yeeun.pics;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Calendar;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class CalendarActivity extends AppCompatActivity {

    private com.google.api.services.calendar.Calendar mService = null;

    GoogleAccountCredential mCredential;
    private Button addEventBtn;
    private static final String[] SCOPES = {CalendarScopes.CALENDAR};

    private TextView accountID;

    int mID = 0;
    String scheduleTitle;
    String startDateString;
    String endDateString;
    String id;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        accountID = (TextView) findViewById(R.id.calendar_id);
        addEventBtn = (Button) findViewById(R.id.button_main_add_event);

        Intent intent = getIntent();
        String accID =  intent.getStringExtra("name");
        id = intent.getStringExtra("id");
        accountID.setText("계정 아이디:" + accID);

        // Google Calendar API 사용하기 위해 필요한 인증 초기화( 자격 증명 credentials, 서비스 객체 )
        // OAuth 2.0를 사용하여 구글 계정 선택 및 인증하기 위한 준비
        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(),
                Arrays.asList(SCOPES)
        ).setBackOff(new ExponentialBackOff());

        mCredential.setSelectedAccountName(accID);

        addEventBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addEventBtn.setEnabled(false);
                mID = 2;        //이벤트 생성

                Intent intent = new Intent(CalendarActivity.this, AddScheduleActivity.class);
                startActivityForResult(intent, 0);

                addEventBtn.setEnabled(true);
            }
        });

    }

    //AddScheduleActivity에서 돌아올 때 실행
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        switch (resultCode){
            case 0:
                scheduleTitle = data.getStringExtra("title");
                startDateString = data.getStringExtra("startDate");
                endDateString = data.getStringExtra("endDate");

                getResultsFromApi();

        }
    }

    private String getResultsFromApi() {

        new MakeRequestTask(this, mCredential).execute();
        return null;
    }

    private class MakeRequestTask extends AsyncTask<Void, Void, String> {

        private Exception mLastError = null;
        private CalendarActivity mActivity;
        List<String> eventStrings = new ArrayList<String>();


        public MakeRequestTask(CalendarActivity activity, GoogleAccountCredential credential) {

            mActivity = activity;

            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();

            mService = new com.google.api.services.calendar.Calendar
                    .Builder(transport, jsonFactory, credential)
                    .setApplicationName("Google Calendar API Android Quickstart")
                    .build();
        }

        @Override
        protected void onPreExecute() {
            //    mResultText.setText("");
        }

        /*
         * 백그라운드에서 Google Calendar API 호출 처리
         */
        @Override
        protected String doInBackground(Void... params) {
            try {
                return addEvent();

            } catch (Exception e) {
                mLastError = e;
                cancel(true);
                return null;
            }

        }


        //doInBackground의 리턴값 받아옴
        @Override
        protected void onPostExecute(String output) {

        }


        @Override
        protected void onCancelled() {
//            if (mLastError != null) {
//                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
//                    showGooglePlayServicesAvailabilityErrorDialog(
//                            ((GooglePlayServicesAvailabilityIOException) mLastError)
//                                    .getConnectionStatusCode());
//                } else if (mLastError instanceof UserRecoverableAuthIOException) {
//                    startActivityForResult(
//                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
//                            MainActivity.REQUEST_AUTHORIZATION);
//                } else {
//                    //          mStatusText.setText("MakeRequestTask The following error occurred:\n" + mLastError.getMessage());
//                }
//            } else {
//                //        mStatusText.setText("요청 취소됨.");
//            }
        }


        private String addEvent() {
            String calendarID = id;

            Event event = new Event()
                    .setSummary(scheduleTitle)
                    .setLocation("서울시")
                    .setDescription("설명");

            java.util.Calendar calander;

            calander = java.util.Calendar.getInstance();
            //simpledateformat = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ssZ", Locale.KOREA);
            // Z에 대응하여 +0900이 입력되어 문제 생겨 수작업으로 입력
            //SimpleDateFormat simpledateformat = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss+09:00", Locale.KOREA);
            //String datetime = simpledateformat.format(calander.getTime());
            //시작시간 setting
            DateTime startDate = new DateTime(startDateString);
            DateTime endDate = new DateTime(endDateString);

            EventDateTime start = new EventDateTime()
                    //.setDateTime(startDateTime)
                    .setDate(startDate)
                    .setTimeZone("Asia/Seoul");
            event.setStart(start);

            Log.d( "@@@", startDateString);

            //끝나는 시간 setting
            EventDateTime end = new EventDateTime()
                    .setDate(endDate)
                    .setTimeZone("Asia/Seoul");
            event.setEnd(end);

            //String[] recurrence = new String[]{"RRULE:FREQ=DAILY;COUNT=2"};
            //event.setRecurrence(Arrays.asList(recurrence));


            try {
                event = mService.events().insert(calendarID, event).execute();
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("Exception", "Exception : " + e.toString());
            }
            System.out.printf("Event created: %s\n", event.getHtmlLink());
            Log.e("Event", "created : " + event.getHtmlLink());
            String eventStrings = "created : " + event.getHtmlLink();
            return eventStrings;
        }
    }
}
