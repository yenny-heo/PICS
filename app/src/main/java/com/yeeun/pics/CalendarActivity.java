package com.yeeun.pics;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CalendarActivity extends AppCompatActivity {

    private com.google.api.services.calendar.Calendar mService = null;

    GoogleAccountCredential mCredential;
    private static final String[] SCOPES = {CalendarScopes.CALENDAR};

    private TextView accountID;

    int mID = 0;
    int preMID;
    String scheduleTitle;
    String startDateString;
    String endDateString;
    String id;

    File selectedFile;


    private Button addEventBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        accountID = (TextView) findViewById(R.id.calendar_id);
        addEventBtn = (Button) findViewById(R.id.button_main_add_event);


        Intent intent = getIntent();
        String accID =  intent.getStringExtra("name");
        id = intent.getStringExtra("id");
        preMID = intent.getIntExtra("preMID", 0);
        //preMID == 2이면 바로 AddSchedule로 이동함

        accountID.setText("계정 아이디:" + accID);

        // Google Calendar API 사용하기 위해 필요한 인증 초기화( 자격 증명 credentials, 서비스 객체 )
        // OAuth 2.0를 사용하여 구글 계정 선택 및 인증하기 위한 준비
        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(),
                Arrays.asList(SCOPES)
        ).setBackOff(new ExponentialBackOff());

        mCredential.setSelectedAccountName(accID);


        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final CharSequence[] items= {"이미지로 추가", "직접 추가"};
        builder.setTitle("방식 선택");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
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
                        mID = 2;        //이벤트 생성

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

    private String getRealPathFromURI(Uri contentURI) {

        String result;
        String [] proj = { MediaStore.Images.Media.DATA };
        Cursor cursor = getContentResolver().query(contentURI, proj, null, null, null);

        if (cursor == null) { // Source is Dropbox or other similar local file path
            result = contentURI.getPath();
        } else {
            cursor.moveToFirst();
            int idx = cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.DATA);
            result = cursor.getString(idx);
            cursor.close();
        }
        return result;
    }

    //AddScheduleActivity에서 돌아올 때 실행
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        switch (resultCode){
            case -1://이미지 선택 후
                    Uri dataUri = data.getData();

                    String dataPath = getRealPathFromURI(dataUri);
                    selectedFile = new File(dataPath);

                    //서버와 통신
                    new Thread() {
                        public void run() {
                            FileUploadUtils.sendToServer(selectedFile);
                            String res = FileUploadUtils.res;
                            System.out.println(res);

                            Intent intent2 = new Intent(CalendarActivity.this, AddScheduleActivity.class);
                            startActivityForResult(intent2, 0);
                        }
                    }.start();

                    break;

            case 0://직접 추가 후
                    scheduleTitle = data.getStringExtra("title");
                    startDateString = data.getStringExtra("startDate");
                    endDateString = data.getStringExtra("endDate");
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
                DateTime startDate = new DateTime(startDateString);
                DateTime endDate = new DateTime(endDateString);
                System.out.println(startDate.toString());
                return addEvent(id, startDate, endDate);

            } catch (Exception e) {
                mLastError = e;
                e.printStackTrace();
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
        }


        private String addEvent(String calendarID, DateTime startDate, DateTime endDate) {
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
