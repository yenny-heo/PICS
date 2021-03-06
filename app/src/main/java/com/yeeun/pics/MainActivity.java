package com.yeeun.pics;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
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

import com.google.api.services.calendar.model.*;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.os.StrictMode;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;


public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks {


    /**
     * Google Calendar API에 접근하기 위해 사용되는 구글 캘린더 API 서비스 객체
     */

    private com.google.api.services.calendar.Calendar mService = null;

    /**
     * Google Calendar API 호출 관련 메커니즘 및 AsyncTask을 재사용하기 위해 사용
     */
    private  int mID = 0;


    GoogleAccountCredential mCredential;
    String accName;
    String id;
    String res;

    private TextView mStatusText;
    private TextView mResultText;
    private Button mGetEventButton;

    private Button addCalendarBtn;
    Toast toast;
    static ProgressDialog mProgress;

    Intent intent;
    String action, type;
    File selectedFile;

    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;

    int READ_PERMISSION = 0;

    private static final String PREF_ACCOUNT_NAME = "accountName";
    private static final String[] SCOPES = {CalendarScopes.CALENDAR};



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        setContentView(R.layout.activity_main);

        addCalendarBtn = (Button) findViewById(R.id.button_main_add_calendar);
        //mGetEventButton = (Button) findViewById(R.id.button_main_get_event);

        //mStatusText = (TextView) findViewById(R.id.textview_main_status);
        //mResultText = (TextView) findViewById(R.id.textview_main_result);

        /**
         * 버튼 클릭으로 동작 테스트
         */
        addCalendarBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addCalendarBtn.setEnabled(false);
               // mStatusText.setText("");
                mID = 1;           //캘린더 생성
                getResultsFromApi();
                addCalendarBtn.setEnabled(true);
            }
        });

        // Google Calendar API 호출중에 표시되는 ProgressDialog
        mProgress = new ProgressDialog(this);
        mProgress.setMessage("Google Calendar API 호출 중입니다.");


        // Google Calendar API 사용하기 위해 필요한 인증 초기화( 자격 증명 credentials, 서비스 객체 )
        // OAuth 2.0를 사용하여 구글 계정 선택 및 인증하기 위한 준비
        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(),
                Arrays.asList(SCOPES)
        ).setBackOff(new ExponentialBackOff()); // I/O 예외 상황을 대비해서 백오프 정책 사용

        //공유하기를 통해 이미지 서버에 보내기
        intent = getIntent();
        action = intent.getAction();
        type = intent.getType();
        if (Intent.ACTION_SEND.equals(action) && type != null) {
            mID = 2;
            System.out.println("Hello\n");
            getResultsFromApi();
        } else {
            // Handle other intents, such as being started from the home screen
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

    /**
     * 다음 사전 조건을 모두 만족해야 Google Calendar API를 사용할 수 있다.
     *
     * 사전 조건
     *     - Google Play Services 설치
     *     - 유효한 구글 계정 선택
     *     - 안드로이드 디바이스에서 인터넷 사용 가능
     *
     * 하나라도 만족하지 않으면 해당 사항을 사용자에게 알림.
     */
    private String getResultsFromApi() {
        if(checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE}, 2);
        }
        else if (!isGooglePlayServicesAvailable()) { // Google Play Services를 사용할 수 없는 경우
            acquireGooglePlayServices();
        } else if (mCredential.getSelectedAccountName() == null) { // 유효한 Google 계정이 선택되어 있지 않은 경우
            chooseAccount();
        } else if (!isDeviceOnline()) {    // 인터넷을 사용할 수 없는 경우
            toast.makeText(getApplicationContext(), "No network connection available", Toast.LENGTH_LONG).show();
        } else {
            // Google Calendar API 호출
            new MakeRequestTask(this, mCredential).execute();
        }
        return null;
    }

    /**
     * 안드로이드 디바이스에 최신 버전의 Google Play Services가 설치되어 있는지 확인
     */
    private boolean isGooglePlayServicesAvailable() {

        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();

        final int connectionStatusCode = apiAvailability.isGooglePlayServicesAvailable(this);
        return connectionStatusCode == ConnectionResult.SUCCESS;
    }

    /**
     * Google Play Services 업데이트로 해결가능하다면 사용자가 최신 버전으로 업데이트하도록 유도하기위해
     * 대화상자를 보여줌.
     */
    private void acquireGooglePlayServices() {

        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        final int connectionStatusCode = apiAvailability.isGooglePlayServicesAvailable(this);

        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
        }
    }

    /**
     * 안드로이드 디바이스에 Google Play Services가 설치 안되어 있거나 오래된 버전인 경우 보여주는 대화상자
     */
    void showGooglePlayServicesAvailabilityErrorDialog(final int connectionStatusCode) {

        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();

        Dialog dialog = apiAvailability.getErrorDialog(
                MainActivity.this,
                connectionStatusCode,
                REQUEST_GOOGLE_PLAY_SERVICES
        );
        dialog.show();
    }

    /*
     * Google Calendar API의 자격 증명( credentials ) 에 사용할 구글 계정을 설정한다.
     *
     * 전에 사용자가 구글 계정을 선택한 적이 없다면 다이얼로그에서 사용자를 선택하도록 한다.
     * GET_ACCOUNTS 퍼미션이 필요하다.
     */
    @AfterPermissionGranted(REQUEST_PERMISSION_GET_ACCOUNTS)
    private void chooseAccount() {
        // GET_ACCOUNTS 권한을 가지고 있다면
        if (EasyPermissions.hasPermissions(this, Manifest.permission.GET_ACCOUNTS)) {
            // SharedPreferences에서 저장된 Google 계정 이름을 가져온다.
            String accountName = getPreferences(Context.MODE_PRIVATE).getString(PREF_ACCOUNT_NAME, null);
            if (accountName != null) {
                // 선택된 구글 계정 이름으로 설정한다.
                mCredential.setSelectedAccount(new Account(accountName, "com.android.example"));
                System.out.println(mCredential.getSelectedAccountName());
                accName  = accountName;
                getResultsFromApi();
            } else {
                // 사용자가 구글 계정을 선택할 수 있는 다이얼로그를 보여준다.
                System.out.println("계정 다이얼로그\n");
                startActivityForResult(mCredential.newChooseAccountIntent(),REQUEST_ACCOUNT_PICKER);
            }

            // GET_ACCOUNTS 권한을 가지고 있지 않다면
        } else {
            System.out.println("계정 없음\n");
            // 사용자에게 GET_ACCOUNTS 권한을 요구하는 다이얼로그를 보여준다.(주소록 권한 요청함)
            EasyPermissions.requestPermissions(
                    (Activity)this,
                    "This app needs to access your Google account (via Contacts).",
                    REQUEST_PERMISSION_GET_ACCOUNTS,
                    Manifest.permission.GET_ACCOUNTS);
        }
    }



    /*
     * 구글 플레이 서비스 업데이트 다이얼로그, 구글 계정 선택 다이얼로그, 인증 다이얼로그에서 되돌아올때 호출된다.
     */
    @Override
    protected void onActivityResult(
            int requestCode,  // onActivityResult가 호출되었을 때 요청 코드로 요청을 구분
            int resultCode,   // 요청에 대한 결과 코드
            Intent data
    ) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:

                if (resultCode != RESULT_OK) {
                    toast.makeText(getApplicationContext(), "구글 플레이 서비스 설치를 해주세요", Toast.LENGTH_LONG).show();

                } else {
                    getResultsFromApi();
                }
                break;


            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null && data.getExtras() != null) {
                    String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(PREF_ACCOUNT_NAME, accountName);
                        editor.apply();
                        mCredential.setSelectedAccount(new Account(accountName, "com.android.example"));
                        accName = accountName;
                        toast.makeText(getApplicationContext(), "계정: "+mCredential.getSelectedAccountName(), Toast.LENGTH_LONG).show();
                        getResultsFromApi();
                    }
                }
                break;


            case REQUEST_AUTHORIZATION:

                if (resultCode == RESULT_OK) {
                    getResultsFromApi();
                }
                break;
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (requestCode == 2) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //권한 설정 하면
                getResultsFromApi();

            } else {
                //권한 설정 안하면
                toast.makeText(getApplicationContext(), "저장소 권한 허용을 해주세요", Toast.LENGTH_LONG).show();
            }
            return;
        }
        else{
            EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
        }

        // other 'case' lines to check for other
        // permissions this app might request
    }
    /*
     * EasyPermissions 라이브러리를 사용하여 요청한 권한을 사용자가 승인한 경우 호출된다.
     */
    @Override
    public void onPermissionsGranted(int requestCode, List<String> requestPermissionList) {

        // 아무일도 하지 않음
    }


    /*
     * EasyPermissions 라이브러리를 사용하여 요청한 권한을 사용자가 거부한 경우 호출된다.
     */
    @Override
    public void onPermissionsDenied(int requestCode, List<String> requestPermissionList) {

        // 아무일도 하지 않음
    }

    /*
     * 안드로이드 디바이스가 인터넷 연결되어 있는지 확인한다. 연결되어 있다면 True 리턴, 아니면 False 리턴
     */
    private boolean isDeviceOnline() {

        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

        return (networkInfo != null && networkInfo.isConnected());
    }


    /*
     * 캘린더 이름에 대응하는 캘린더 ID를 리턴
     */
    private String getCalendarID(String calendarTitle){

        String id = null;

        // Iterate through entries in calendar list
        String pageToken = null;
        do {
            CalendarList calendarList = null;
            try {
                calendarList = mService.calendarList().list().setPageToken(pageToken).execute();
            } catch (UserRecoverableAuthIOException e) {
                startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
            }catch (IOException e) {
                e.printStackTrace();
            }
            List<CalendarListEntry> items = calendarList.getItems();

            for (CalendarListEntry calendarListEntry : items) {

                //calendar 목록 불러와서, 이름이 일치하는지 확인
                if ( calendarListEntry.getSummary().toString().equals(calendarTitle)) {
                    id = calendarListEntry.getId().toString();//이름이 일치하면 id를 받아옴
                }
            }
            pageToken = calendarList.getNextPageToken();
        } while (pageToken != null);

        return id;
    }


    /*
     * 비동기적으로 Google Calendar API 호출
     */
    private class MakeRequestTask extends AsyncTask<Void, Void, String> {

        private Exception mLastError = null;
        private MainActivity mActivity;
        List<String> eventStrings = new ArrayList<String>();

        public MakeRequestTask(MainActivity activity, GoogleAccountCredential credential) {

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
            mProgress.show();
        }

        /*
         * 백그라운드에서 Google Calendar API 호출 처리
         */
        @Override
        protected String doInBackground(Void... params) {
            try {
                if ( mID == 1 || mID == 2) { // 캘린더 추가
                    mProgress.setMessage("서버와 통신 중입니다.");
                    return createCalendar();
                }

            } catch (Exception e) {
                mLastError = e;
                cancel(true);
                return null;
            }

            return null;
        }




        /*
         * 선택되어 있는 Google 계정에 새 캘린더를 추가한다.
         */
        private String createCalendar() throws IOException {
            id = getCalendarID("Pics");

            //Pics라는 이름의 캘린더가 없는 경우
            if ( id == null ) {
                // 새로운 캘린더 생성
                com.google.api.services.calendar.model.Calendar calendar = new Calendar();

                calendar.setSummary("Pics")//캘린더 제목 설정
                        .setTimeZone("Asia/Seoul");//캘린더 시간대 설정

                // 구글 캘린더에 새로 만든 캘린더를 추가
                Calendar createdCalendar = mService.calendars().insert(calendar).execute();

                // 추가한 캘린더의 ID를 가져옴.
                String calendarId = createdCalendar.getId();

                // 구글 캘린더의 캘린더 목록에서 새로 만든 캘린더를 검색
                CalendarListEntry calendarListEntry = mService.calendarList().get(calendarId).execute();

                // 캘린더의 배경색 변경
                calendarListEntry.setBackgroundColor("#ff96e7");

                // 변경한 내용을 구글 캘린더에 반영
                CalendarListEntry updatedCalendarListEntry =
                        mService.calendarList()
                                .update(calendarListEntry.getId(), calendarListEntry)
                                .setColorRgbFormat(true)
                                .execute();

                id = getCalendarID("Pics");
            }
            //공유하기를 통해 일정 추가시 데이터 전송
            if(mID == 2){
                if (type.startsWith("image/")) {
                    Log.d(this.getClass().getName(), "image");
                    Log.d(this.getClass().getName(), intent.toString());
                    Uri dataUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
                    if (dataUri != null) {
                        Log.d(this.getClass().getName(), dataUri.toString());
                        Log.d(this.getClass().getName(),"image 받아옴");
                        System.out.println("공유 dataUri: "+dataUri);
                        String dataPath = getRealPathFromURI(dataUri);

                        selectedFile = new File(dataPath);

                        //서버와 통신
                        (MainActivity.this).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                FileUploadUtils.sendToServer(selectedFile);
                                res = FileUploadUtils.res;
                                Intent intent = new Intent(MainActivity.this, CalendarActivity.class);
                                intent.putExtra("name", accName);
                                intent.putExtra("id", id);
                                intent.putExtra("data", res);
                                startActivity(intent);
                            }
                        });
                    }
                    // Update UI to reflect image being shared }
                }
                //서버에서 받아온 데이터로 일정 추가하기
            }
            // 새로 추가한 캘린더의 ID를 리턴
            return "캘린더가 생성되었습니다.";
        }


        //doInBackground의 리턴값 받아옴
        @Override
        protected void onPostExecute(String output) {
            if(mID == 1) {
                mProgress.hide();
                Intent intent = new Intent(MainActivity.this, CalendarActivity.class);
                intent.putExtra("name", accName);
                intent.putExtra("id", id);
                startActivity(intent);
            }
        }


        @Override
        protected void onCancelled() {
            mProgress.hide();
            if (mLastError != null) {
                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                    showGooglePlayServicesAvailabilityErrorDialog(
                            ((GooglePlayServicesAvailabilityIOException) mLastError)
                                    .getConnectionStatusCode());
                } else if (mLastError instanceof UserRecoverableAuthIOException) {

                    startActivityForResult(
                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
                            MainActivity.REQUEST_AUTHORIZATION);
                } else {

                    System.out.println("MakeRequestTask The following error occurred:\n" + mLastError.getMessage());
                }
            } else {

        //        mStatusText.setText("요청 취소됨.");
            }
        }


    }


}