package com.google.cloud.android.reminderapp;

import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.tsengvn.typekit.TypekitContextWrapper;

/**
 * AlarmSoundService를 통해 예외 사항을 거친 알람이
 * 애니메이션, 소리 그리고 진동을 통해 울리도록 도와주는 Class
 *
 */
public class AlarmActivity extends AppCompatActivity {

    public static Handler ahandler; // 알람 화면 처리 핸들러(알람이 끝나면 알람 화면 종료하도록)
    TextView textView;
    ImageButton rBtn, backBtn;
    ImageView alarmimage;
    VoicePlayer mVoicePlayer;
    String alarmText, fileName;
    CountDownTimer timer;
    AudioManager mAudioManager;
    Ringtone ringtone;
    Animation anim;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm);

        textView = (TextView) findViewById(R.id.text);
        rBtn = (ImageButton) findViewById(R.id.rBtn);
        backBtn = (ImageButton) findViewById(R.id.backBtn);
        alarmimage = (ImageView)findViewById(R.id.alarmImage);

        //알람이 울릴시 흔들리는 시계가 효과
        anim = AnimationUtils.loadAnimation(getApplicationContext(),R.anim.shake);
        alarmimage.startAnimation(anim);

        mVoicePlayer = new VoicePlayer(getApplicationContext());
        mAudioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);

        //진동 - 참조 : http://bitsoul.tistory.com/129
        final Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        //알람 벨소리
        //Uri ringtoneUri = RingtoneManager.getActualDefaultRingtoneUri(getApplicationContext(), R.raw.rec_start);
        Uri ring_uri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.alarm);

        if(mAudioManager.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE) { //진동
            vibrator.vibrate(new long[]{500, 1000}, 0); //진동 패턴: 대기, 진동,.. / 0: 무한 반복, -1: 반복 없음.
        }
        else if(mAudioManager.getRingerMode() == AudioManager.RINGER_MODE_SILENT) { //무음
            //가만히 있기
        }
        else if(mAudioManager.getRingerMode() == AudioManager.RINGER_MODE_NORMAL) { //벨소리
//            ringtone = RingtoneManager.getRingtone(getApplicationContext(), ringtoneUri);
            ringtone = RingtoneManager.getRingtone(getApplicationContext(), ring_uri);
            ringtone.play();
        }

        //6초짜리 벨소리 때문에 6000 -> 이러면 6초 울리고 6초 쉬넹...
        //그냥 1000으로 해야겠다.
        timer = new CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                if(ringtone != null) {
                    ringtone.play();
                }
            }

            @Override
            public void onFinish() {
                if(vibrator != null) {
                    vibrator.cancel();
                }
                if(ringtone != null) {
                    ringtone.stop();
                }
                alarmimage.clearAnimation();
                timer.cancel();
            }
        };

        rBtn.setOnClickListener(new View.OnClickListener() { //R버튼을 누르면 리마인더에 해당하는 텍스트가 보여지고, 해당 파일을 재생해준다.
            public void onClick(View v) {

                System.out.println("알람 액티비티 button setonclicklistener " + mVoicePlayer.mIsPlaying2);

                if(mVoicePlayer.mIsPlaying2) {
                    mVoicePlayer.stopPlaying2();
                    rBtn.setImageResource(R.drawable.play_btn3);
                }
                else {
                    mVoicePlayer.startPlaying2(16000, 1024, fileName); //*** 그냥 playWaveFileAlarm을 사용하면 안되고, 다른 Thread를 이용해야함!
                    //리스트 화면에서 알람화면으로 전환되고 나서 OutOfMemory에러가 나서, stop_btn2이미지 크기를 줄였다...!!!
                    //이 부분은 bitmap관련해서 heap메모리와 관련있는 것 같은데, 나중에 따로 공부좀 해봐야 함.
                    //일단은 임시방편으로 이미지 크기를 줄여서 넘어감...
                    rBtn.setImageResource(R.drawable.stop_btn);
                    if(vibrator != null) {
                        vibrator.cancel(); //진동 취소
                    }
                    if(ringtone != null) {
                        ringtone.stop();
                    }
                    alarmimage.clearAnimation();
                    timer.cancel();
                }
            }
        });

        backBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(vibrator != null) {
                    vibrator.cancel();
                    timer.cancel();
                }
                if(ringtone != null) {
                    ringtone.stop();
                    timer.cancel();
                }
                if(mVoicePlayer != null && mVoicePlayer.mIsPlaying2) {
                    mVoicePlayer.stopPlaying2();
                }
                alarmimage.clearAnimation();
                finish();
            }
        });

        //화면이 OFF 되어있는 상태에서도 알람 화면이 나오도록 하는 코드. // 참고 : http://cofs.tistory.com/173
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);

        ahandler = new Handler() {
            public void handleMessage(Message msg) {
                if(((String) msg.obj).equals("stop")) {
                    rBtn.setImageResource(R.drawable.play_btn3);
                }
            }
        };
    }
    /**
     * Activity가  Start state에 있을 때, 알람의 text와 녹음 파일을 받아
     * Activity의 textView에 알람 텍스트를 보여주고
     * 타이머를 시작하여 진동 혹은 소리가 울릴 수 있도록 한다.
     */
    @Override
    protected void onStart() {
        super.onStart();

        Intent intent = getIntent();
        alarmText = intent.getStringExtra("alarmtext");
        fileName = intent.getStringExtra("filename");

        textView.setText(alarmText);

        //진동 60초 타이머
        timer.cancel();
        timer.start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        //알람 화면이 사라질 경우(예를 들어 홈버튼, 최근 사용 앱 확인 버튼을 누르는 경우) 알람을 종료한다
//        button.callOnClick(); // 화면이 꺼져있으면 AlarmActivity가 2번 호출되는 듯한 현상이 일어난다. 즉, onStop이 한번 더 호출되어
        //시작도 전에 종료가 된다. 그래서 onUserLeaveHint()를 사용하기로 했다.
    }



    @Override
    protected void onUserLeaveHint() { //홈버튼, 멀티버튼을 누르거나 이 액티비티에서 다른 액티비티로 넘어갈 때 onPause직전에 호출된다.
        //다른 액티비티로 넘어갈 때 호출되지 않는 방법은 해당 인텐트에 FLAG_ACTIVITY_NO_USER_ACTION를 추가하면 되는듯.
        //하지만 AlarmActivity에서 다른 액티비를 호출하는 일은 없고, 단지 홈버튼을 누르거나 최근 사용 앱 확인버튼(멀티버튼)을 누를 경우
        //종료하는 것이 목적이므로 그냥 써도 될 것 같다.
        super.onUserLeaveHint();
        System.out.println("알람 액티비티 onUserLeaveHin()");
        backBtn.callOnClick();
    }

    @Override
    public void onBackPressed() { //back button을 누르면 알람이 종료되도록 한다.
        super.onBackPressed();

        System.out.println("알람 액티비티 backPressed");
        backBtn.callOnClick();
    }

    /**
     * 액티비티의 글꼴을 바꾸기 위해 불러지는 함수이다.
     * CustomStartApp과 연결되어 있다.
     */
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(TypekitContextWrapper.wrap(newBase));
    }
}
