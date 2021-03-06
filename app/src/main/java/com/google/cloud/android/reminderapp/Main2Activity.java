package com.google.cloud.android.reminderapp;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.tsengvn.typekit.TypekitContextWrapper;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Main2Activity extends AppCompatActivity {
    Button countList;
//    Button record;
//    Button play;
    public static DataBase db;
    public static VoiceRecorder mVoiceRecorder;
    public static VoicePlayer mVoicePlayer;
    public static String userAccount;

    private SpeechService mSpeechService;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 1; //추가
    private static final String FRAGMENT_MESSAGE_DIALOG = "message_dialog";

    long bpTime = 0;
    Toast bpToast;

    boolean isFirstAuth = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        db = new DataBase(Main2Activity.this);
        mVoiceRecorder = new VoiceRecorder(this);
        mVoicePlayer = new VoicePlayer(this);

        countList = (Button) findViewById(R.id.countlist);
        bpToast = Toast.makeText(this, "뒤로가기를 한번 더 누르면 앱이 종료됩니다.", Toast.LENGTH_SHORT);


    }

    @Override
    protected void onStart() {
        super.onStart();

        int playCount = db.getAllPlayListNum();
        String pCntStr = "" + playCount;
        countList.setText(pCntStr);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED) {
            isFirstAuth = false;
        }
        else if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)  ) {
            showPermissionMessageDialog();
        }
        else {
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.WRITE_CALENDAR, Manifest.permission.RECORD_AUDIO},0);
        }


        //        //  출처: http://shnoble.tistory.com/80 [노블의 개발이야기]
        // 사용자 계정 얻어오기.
        if(isFirstAuth) {
            chooseAccountIntent();
        }

    }


    /**
     * 이 메소드는 뒤로가기 버튼을 눌렀을 경우 앱이 바로 꺼지는 것을 방지한다.
     * 2초 안에 다시 뒤로가기 버튼을 눌렀을 경우 앱을 종료한다.
     */
    @Override
    public void onBackPressed() {
        //참고 : http://best421.tistory.com/71
        //이렇게 해도 cancel이 안되는 이유는 onBackPressed()에 들어올 때마다 toast가 새로 생성되기 때문에
        //결국 cancel하는 toast는 다른 값이 된다. -> 전역변수로 설정하자.
//        Toast toast = Toast.makeText(this, "뒤로가기를 한번 더 누르면 앱이 종료됩니다.", Toast.LENGTH_SHORT);
        if(bpTime == 0) {
            System.out.println("토스트1 : "+ bpToast);
            ViewGroup group = (ViewGroup) bpToast.getView();
            TextView messageTextView = (TextView) group.getChildAt(0);
            messageTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
            bpToast.show();
            bpTime = System.currentTimeMillis();
        }
        else {
            long sec = System.currentTimeMillis() - bpTime;

            if(sec > 2000) {
                System.out.println("토스트2 : " + bpToast);
                bpToast.show();
                bpTime = System.currentTimeMillis();
            }
            else {
                bpToast.cancel();
                super.onBackPressed();
                finish();
            }
        }
    }

    /**
     * 이 메소드는 사용자가 녹음 버튼을 누를 때, 네트워크 상태를 체크한 뒤, 네트워크가 연결되어 있을 경우 RecordActivity를 실행한다.
     *
     * @param v
     */

    public void onButtonRecordClicked(View v) {
        //네트워크 연결 상태 체크 ** 중요 ** - 출처 : http://developer88.tistory.com/104
        //네트워크 연결 시에만 녹음 및 stt가능
        NetworkInfo mNetworkState = getNetworkInfo();
        //근데 이 경우, 무료 와이파이 존이 연결이 된 것으로 표시만 되고, 실제로 연결이 되지 않은 경우를 판별하지 못함.
        if(mNetworkState != null && mNetworkState.isConnected()) {
            Intent intent = new Intent(getApplicationContext(), RecordActivity.class);
            startActivity(intent);
        }
        else {
            System.out.println("네트워크 연결 상태 확인 좀 해라");
            Toast.makeText(this, "네트워크 연결 상태를 확인해주세요.", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * 이 메소드는 사용자가 재생 버튼을 누를 때, 현재 리마인더 목록을 확인 후
     * 비어 있으면 토스트 메시지를, 1개 이상의 리마인더가 있을 경우 재생 목록을 보여주는
     * PlayListActivity를 실행한다.
     *
     * @param v
     */

    public void onButtonPlayClicked(View v) {
        if(db.getAllPlayListNum() > 0) {
            Intent intent = new Intent(getApplicationContext(), PlayListActivity.class);
            startActivity(intent);
        }
        else {
            Toast.makeText(this, "재생할 목록이 비어있습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 이 메소드는 녹음 전, 네트워크를 통해 구글 STT로 전달된 사용자 발화를 Text로 변환받아야 하기 때문에
     * 네트워크 연결 여부를 사전에 확인하기 위한 메소드이다.
     *
     * @return networkInfo 네트워크 인터페이스의 상태를 나타낸다.
     */

    private NetworkInfo getNetworkInfo() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo;
    }

    /**
     * Permission을 체크한다. Permission 되어있지 않을 경우 다이얼로그를 통해
     * 유저에게 이를 알린다.
     *
     * @param requestCode 사용자가 record하는 것의 permission code 0 혹은 REQUEST_RECORD_AUDIO_PERMISSION과 같지 않을 경우 권한을 받아야 한다.
     * @param permissions
     * @param  grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION || requestCode == 0) {
            if (permissions.length == 1 && grantResults.length == 1
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            } else {
                //showPermissionMessageDialog();
            }
        } else {
            //super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    /**
     * 녹음과 관련된 Permission을 유저에게 확인받기 위해 다이얼로그를 띄운다.
     */
    private void showPermissionMessageDialog() {
        MessageDialogFragment
                .newInstance(getString(R.string.permission_message))
                .show(getSupportFragmentManager(), FRAGMENT_MESSAGE_DIALOG);
    }

    /**
     * 다이얼로그가 무시되었을 경우 다시 다이얼로그를 띄운다.
     */
    public void onMessageDialogDismissed() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO},
                REQUEST_RECORD_AUDIO_PERMISSION);
    }

    /**
     * DBInstance를 다른 Class에서도 사용할 수 있도록 하기 위해 사용한다.
     */
    public static DataBase getDBInstance() {
        return db;
    }

    /**
     * 액티비티의 글꼴을 바꾸기 위해 불러지는 함수이다.
     * CustomStartApp과 연결되어 있다.
     */
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(TypekitContextWrapper.wrap(newBase));
    }


    /**
     * 리마인더를 구글 캘린더와 연결하기 이전에 구글 계정을 이 앱과 연동하기 위해 필요한 함수이다.
     * intent를 통해 기기 내에 등록된 계정을 선택할 수 있는 Activity가 뜬다.
     */
//    출처: http://shnoble.tistory.com/80 [노블의 개발이야기]
    int REQUEST_CODE = 300;
    private void chooseAccountIntent() {
        Intent intent = AccountManager.newChooseAccountIntent(
                null, null, new String[]{"com.google"}, null, null, null, null);
        startActivityForResult(intent, REQUEST_CODE);
    }

    /**
     * chooseAccountIntent를 통해 구글 계정의 이름과 타입을 받아온 뒤,
     * SharedPreferences를 이용하여 userAccount의 값을 수정한다
     *
     * @param requestCode 다양한 Activity와의 소통이 있을 수 있으므로, 이전 chooseAccountIntent의 REQUEST_CODE 값과 일치하는지 확인한다.
     * @param resultCode
     * @param data 사용자가 선택한 계정에 관한 정보가 담겨있다.
     */

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE) {
            if (data != null) {
                Bundle extras = data.getExtras();
                final String accountName = extras.getString(AccountManager.KEY_ACCOUNT_NAME);
                final String accountType = extras.getString(AccountManager.KEY_ACCOUNT_TYPE);
                System.out.println("Account Name: " + accountName);
                System.out.println("Account Type: " + accountType);

                userAccount = accountName;

                //SharedPreferences 사용해서 누적된 알람의 개수 저장
                SharedPreferences uaPref = getSharedPreferences("uaPref", MODE_PRIVATE);
                SharedPreferences.Editor editor = uaPref.edit();
                editor.putString("userAccount", userAccount); // 값 수정.
                editor.commit();
            }
        }
    }

}
