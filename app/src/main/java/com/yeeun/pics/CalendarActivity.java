package com.yeeun.pics;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

import com.applandeo.materialcalendarview.CalendarView;
import com.applandeo.materialcalendarview.EventDay;
import com.applandeo.materialcalendarview.listeners.OnCalendarPageChangeListener;
import com.applandeo.materialcalendarview.listeners.OnDayClickListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;

import org.w3c.dom.Text;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;


public class CalendarActivity extends AppCompatActivity {

    public static int CREATE_EVENT = 2;
    public static int GET_EVENT = 3;

    private com.google.api.services.calendar.Calendar mService = null;

    GoogleAccountCredential mCredential;
    private static final String[] SCOPES = {CalendarScopes.CALENDAR};

    //private TextView accountID;
    //private TextView eventList;

    int mID = 0;
    String data;
    String scheduleTitle;
    String startDateString;
    String endDateString;
    String calendarID;


    static ProgressDialog mProgress;

    File selectedFile;


    private FloatingActionButton addEventBtn;
    private CalendarView calendarView;

    ArrayList<EventData> eventLists = new ArrayList<>();
    ArrayList<EventData> showEventLists = new ArrayList<>();
    ArrayList<EventDay> calendarEvents = new ArrayList<>();

    ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        listView = (ListView) findViewById(R.id.listView);
        final MyAdapter myAdapter = new MyAdapter(this, showEventLists);
        listView.setAdapter(myAdapter);

        //accountID = (TextView) findViewById(R.id.calendar_id);
        //eventList = (TextView) findViewById(R.id.event_list);
        addEventBtn = (FloatingActionButton) findViewById(R.id.button_main_add_event);
        calendarView = (CalendarView) findViewById(R.id.calendarView);
        calendarView.setOnDayClickListener(new OnDayClickListener() {
            @Override
            public void onDayClick(EventDay eventDay) {
                Calendar cal = eventDay.getCalendar();
                showEventLists.clear();
                for( EventData eventData : eventLists ) {
                    if(eventData.getStartYear() == cal.get(Calendar.YEAR)
                     && eventData.getStartMonth() == cal.get(Calendar.MONTH)
                     && eventData.getStartDay() == cal.get(Calendar.DATE)){
                        showEventLists.add(new EventData(eventData.getTitle(), eventData.getStartYear(), eventData.getStartMonth(), eventData.getStartDay(),
                                eventData.getStartHour(), eventData.getStartMin(), eventData.getEndHour(), eventData.getEndMin()));
                    }
                }
                myAdapter.notifyDataSetChanged();
            }
        });

        Intent intent = getIntent();
        String accID =  intent.getStringExtra("name");
        calendarID = intent.getStringExtra("id");
        data = intent.getStringExtra("data");

        mProgress = new ProgressDialog(this);
        mProgress.setMessage("서버와 통신 중입니다.");

        MainActivity.mProgress.hide();
        //공유하기를 통한 일정추가시
        if(data != null){
            Intent intent2 = new Intent(CalendarActivity.this, AddScheduleActivity.class);
            intent2.putExtra("res", data);
            startActivityForResult(intent2, 0);
        }

        //accountID.setText("계정 아이디:" + accID);

        // Google Calendar API 사용하기 위해 필요한 인증 초기화( 자격 증명 credentials, 서비스 객체 )
        // OAuth 2.0를 사용하여 구글 계정 선택 및 인증하기 위한 준비
        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(),
                Arrays.asList(SCOPES)
        ).setBackOff(new ExponentialBackOff());

        mCredential.setSelectedAccountName(accID);

        //일정 받아오기
        mID = GET_EVENT;
        getResultsFromApi();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final CharSequence[] items= {"이미지로 추가", "직접 추가"};
        builder.setTitle("방식 선택");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                showEventLists.clear();
                switch (i) {
                    case 0://이미지로 추가
                        try {
                            if (ActivityCompat.checkSelfPermission(CalendarActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                                ActivityCompat.requestPermissions(CalendarActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                            } else {
                                Intent intent1 = new Intent(Intent.ACTION_PICK);
                                intent1.setType("image/*");
                                startActivityForResult(intent1, 1);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        break;
                    case 1://직접 추가
                        addEventBtn.setEnabled(false);
                        Intent intent2 = new Intent(CalendarActivity.this, AddScheduleActivity.class);
                        startActivityForResult(intent2, 0);
                        addEventBtn.setEnabled(true);
                        break;
                }
            }
        });


        final AlertDialog alertDialog = builder.create();

        addEventBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.show();
            }
        });



    }
    public static Calendar DatetoCalendar(DateTime dt) throws ParseException {
        Calendar cal = Calendar.getInstance();
        String date = dt.toString();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        cal.setTime(sdf.parse(date));
        return cal;
    }

    public static Calendar DateTimetoCalendar(DateTime dt) throws ParseException {
        Calendar cal = Calendar.getInstance();
        String date = dt.toString();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        cal.setTime(sdf.parse(date));
        return cal;
    }

    //갤러리 접근권한에서 돌아올 때 실행
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {

        switch (requestCode) {
            case 1:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Intent intent1 = new Intent(Intent.ACTION_PICK);
                    intent1.setType("image/*");
                    startActivityForResult(intent1, 1);
                } else {
                    //do something like displaying a message that he didn`t allow the app to access gallery and you wont be able to let him select from gallery
                }
                break;
        }
    }


    public String uriToFilename(Uri uri) {
        String path = null;
        Context context = getApplicationContext();
        if (Build.VERSION.SDK_INT < 11) {
            path = UriParser.getPath(context, uri);
        } else if (Build.VERSION.SDK_INT < 19) {
            path = UriParser.getPath(context.getApplicationContext(), uri);
        } else {
            path = UriParser.getPath(context.getApplicationContext(), uri);
        }

        uri = Uri.parse(path);

        Cursor cursor = context.getContentResolver().query(uri, null, null, null, null );
        cursor.moveToNext();
        String resutlPath = cursor.getString( cursor.getColumnIndex( "_data" ) );
        cursor.close();
        return resutlPath;
    }


    //이미지 선택 인텐트, AddScheduleActivity에서 돌아올 때 실행
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        switch (resultCode){
            case -1://이미지 선택 후
                    Uri dataUri = data.getData();

                    String dataPath = uriToFilename(dataUri);
                    selectedFile = new File(dataPath);

                    mProgress.show();
                    //서버와 통신
                    new Thread() {
                        public void run() {
                            FileUploadUtils.sendToServer(selectedFile);
                            String res = FileUploadUtils.res;
                            //테스트 데이터
                            //res = "{\"startYear\" : \"2020\",\"startMonth\" : \"4\",\"startDay\" : \"27\",\"startHour\" : \"0\",\"startMin\" : \"0\",\"endYear\" : \"2020\",\"endMonth\" : \"4\",\"endDay\" : \"27\",\"endHour\" : \"3\",\"endMin\" : \"3\"}";
                            System.out.println(res);


                            if(res != null) {
                                Intent intent2 = new Intent(CalendarActivity.this, AddScheduleActivity.class);
                                intent2.putExtra("res", res);
                                startActivityForResult(intent2, 0);
                            }
                        }
                    }.start();

                    break;

            case 0://직접 추가 후
                    mProgress.setMessage("일정 추가중 입니다.");
                    mProgress.show();
                    scheduleTitle = data.getStringExtra("title");
                    startDateString = data.getStringExtra("startDate");
                    endDateString = data.getStringExtra("endDate");
                    mID = CREATE_EVENT;       //이벤트 생성
                    getResultsFromApi();
                    break;

        }
    }

    private String getResultsFromApi() {
        new MakeRequestTask(this, mCredential).execute();
        return null;
    }

    private class MakeRequestTask extends AsyncTask<Void, Void, String> {

        private Exception mLastError = null;
        private CalendarActivity mActivity;


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
                if(mID == CREATE_EVENT) {
                    DateTime startDate = new DateTime(startDateString);
                    DateTime endDate = new DateTime(endDateString);
                    System.out.println(startDate.toString());
                    return addEvent(calendarID, startDate, endDate);
                }
                if(mID == GET_EVENT) {
                    return getEvent();
                }

            } catch (Exception e) {
                mLastError = e;
                e.printStackTrace();
                cancel(true);
                return null;
            }
            return null;
        }

        //doInBackground의 리턴값 받아옴
        @Override
        protected void onPostExecute(String output) {
            if(mID == CREATE_EVENT){
                //이벤트 생성시 캘린더 업데이트
                mID = GET_EVENT;
                getResultsFromApi();
            }
            if(mID == GET_EVENT){//get Event
                calendarView.setEvents(calendarEvents);
                mProgress.hide();
                //eventList.setText(TextUtils.join("\n\n", eventStrings));
            }
        }

        @Override
        protected void onCancelled() {
        }

        private String getEvent() throws IOException, ParseException {
            eventLists.clear();
            calendarEvents.clear();
            Events events = mService.events().list(calendarID)//"primary")
                    //.setTimeMin(now)
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .execute();
            List<Event> items = events.getItems();

            for (Event event : items) {

                DateTime start = event.getStart().getDateTime();
                DateTime end = event.getEnd().getDateTime();
                Calendar startCalendar = null;
                Calendar endCalendar = null;
                //시작 시간
                if(start != null) {
                    startCalendar = DateTimetoCalendar(start);
                }
                else {
                    // 모든 이벤트가 시작 시간을 갖고 있지는 않다. 그런 경우 시작 날짜만 사용
                    start = event.getStart().getDate();
                    startCalendar = DatetoCalendar(start);
                }
                //끝 시간
                if(end != null) {
                    endCalendar = DateTimetoCalendar(end);
                }
                else {
                    // 모든 이벤트가 시작 시간을 갖고 있지는 않다. 그런 경우 시작 날짜만 사용
                    end = event.getStart().getDate();
                    endCalendar = DatetoCalendar(end);
                }
                String title = event.getSummary();
                int sy = startCalendar.get(Calendar.YEAR);
                int sM = startCalendar.get(Calendar.MONTH);
                int sd = startCalendar.get(Calendar.DATE);
                int sh = startCalendar.get(Calendar.HOUR_OF_DAY);
                int sm = startCalendar.get(Calendar.MINUTE);
                int eh = endCalendar.get(Calendar.HOUR_OF_DAY);
                int em = endCalendar.get(Calendar.MINUTE);

                EventData mEventData = new EventData(title, sy, sM, sd, sh, sm, eh, em);
                eventLists.add(mEventData);


                System.out.println(mEventData.getTitle()+ " "+mEventData.getStartHour()+" "+mEventData.getEndHour());
                calendarEvents.add(new EventDay(startCalendar, R.drawable.event));

            }

            System.out.println("set Event!!\n");
            return eventLists.size() + "개의 데이터를 가져왔습니다.";
        }

        private String addEvent(String calendarID, DateTime startDate, DateTime endDate) {
            System.out.println("일정 추가!!\n");
            Event event = new Event()
                    .setSummary(scheduleTitle)
                    .setLocation("서울시")
                    .setDescription("설명");

            EventDateTime start = new EventDateTime()
                    .setDateTime(startDate)
                    .setTimeZone("Asia/Seoul");
            event.setStart(start);

            Log.d( "@@@", startDateString);

            //끝나는 시간 setting
            EventDateTime end = new EventDateTime()
                    .setDateTime(endDate)
                    .setTimeZone("Asia/Seoul");
            event.setEnd(end);

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
