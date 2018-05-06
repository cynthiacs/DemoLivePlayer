package com.leadcore.demoliveplayer;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.alivc.player.AliVcMediaPlayer;
import com.alivc.player.MediaPlayer;
import com.leadcore.demoliveplayer.customviews.CustomDialog;

import java.util.Map;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class VideoPlayerActivity extends AppCompatActivity {
    private final static String TAG = "VideoPlayerActivity";
    private SurfaceView mSurfaceView;
    private AliVcMediaPlayer mPlayer;
    private String mUrl;
    private ExceptionReceiver mReceiver;
    CustomDialog mWarningDialog;

    private static final int STARTPLAYER_REQUEST_CODE = 1;
    public static final String STREAM_EXCEPTION_MSG = "stream_exception";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);
        Intent intent = new Intent();
        intent.putExtra("nodeId", getIntent().getExtras().getString("nodeId"));
        setResult(STARTPLAYER_REQUEST_CODE, intent);
        mSurfaceView = findViewById(R.id.surfaceview);
        mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                surfaceHolder.setKeepScreenOn(true);
                Log.d(TAG, "surfaceCreated "+mPlayer);
                if(mPlayer != null) {
                    mPlayer.setVideoSurface(mSurfaceView.getHolder().getSurface());
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
                Log.d(TAG, "surfaceChanged");
                if (mPlayer != null) {
                    mPlayer.setSurfaceChanged();
                }
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                Log.d(TAG, "surfaceDestroyed");
            }
        });
        initPlayer();
        mReceiver = new ExceptionReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(STREAM_EXCEPTION_MSG);
        registerReceiver(mReceiver, filter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        if (mPlayer !=  null) {
            mUrl = getIntent().getExtras().getString("url");
            mPlayer.prepareAndPlay(mUrl);
//            Log.d(TAG, "delay 5s to play");
//            mHandler.sendEmptyMessageDelayed(100, 5000);
        }
    }

    @SuppressLint("HandlerLeak")
    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 100) {
                Log.d(TAG, "receive play msg");
                mPlayer.prepareAndPlay(mUrl);
            }
            super.handleMessage(msg);
        }
    };

    private void initPlayer() {
        mPlayer = new AliVcMediaPlayer(this, mSurfaceView);
        mPlayer.setPreparedListener(new MediaPlayer.MediaPlayerPreparedListener() {
            @Override
            public void onPrepared() {
                Log.d(TAG, "AliVcMediaPlayer prepared!");
                Log.d(TAG, "video width:"+mPlayer.getVideoWidth()+", height:"+mPlayer.getVideoHeight());
                Map<String,String> logInfos = mPlayer.getAllDebugInfo();
                for(String in : logInfos.keySet()) {
                    String str = logInfos.get(in);
                    Log.d(TAG, in+":"+str+"|");
                }
            }
        });
        mPlayer.setErrorListener(new MediaPlayer.MediaPlayerErrorListener() {
            public void onError(int i, String s) {
                Log.d(TAG, "AliVcMediaPlayer onError:"+s);
                Map<String,String> logInfos = mPlayer.getAllDebugInfo();
                for(String in : logInfos.keySet()) {
                    String str = logInfos.get(in);
                    Log.d(TAG, in+":"+str+"|");
                }
                showWarningDialog(R.string.msg_warning_neterror);
            }
        });
        mPlayer.setCompletedListener(new MediaPlayer.MediaPlayerCompletedListener() {
            @Override
            public void onCompleted() {
                Log.d(TAG, "AliVcMediaPlayer onCompleted!");
//                Toast.makeText(VideoPlayerActivity.this,
//                        getText(R.string.video_complete_push), Toast.LENGTH_LONG).show();
//                VideoPlayerActivity.this.finish();
                showWarningDialog(R.string.msg_warning_puserror);
            }
        });
        mPlayer.setStoppedListener(new MediaPlayer.MediaPlayerStoppedListener() {
            @Override
            public void onStopped() {
                Log.d(TAG, "AliVcMediaPlayer onStopped!");
            }
        });
        mPlayer.setBufferingUpdateListener(new MediaPlayer.MediaPlayerBufferingUpdateListener() {
            @Override
            public void onBufferingUpdateListener(int i) {
                Log.d(TAG, "AliVcMediaPlayer onBufferingUpdateListener:"+i);
            }
        });
        mPlayer.enableNativeLog();
    }

    private void showWarningDialog(int resStringId) {
        LayoutInflater inflater = (LayoutInflater)this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View warningView = inflater.inflate(R.layout.warning_layout, null);
        TextView msgTv = warningView.findViewById(R.id.warning_msg);
        msgTv.setText(resStringId);
        Button btn = warningView.findViewById(R.id.warning_btn);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mWarningDialog.dismiss();
                VideoPlayerActivity.this.finish();
            }
        });
        mWarningDialog = new CustomDialog.Builder(this)
                .create(warningView, R.style.MyWaringDailog, Gravity.CENTER);
        mWarningDialog.setDialogOnKeyDownListner(new CustomDialog.DialogOnKeyDownListner() {
            @Override
            public void onKeyDownListener(int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    Log.d(TAG, "Dialog back key down");
                    mWarningDialog.dismiss();
                    VideoPlayerActivity.this.finish();
                }
            }
        });
        mWarningDialog.show();
    }

    private void stopPlay() {
        if (mPlayer != null) {
            mPlayer.stop();
        }
    }

    private class ExceptionReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (STREAM_EXCEPTION_MSG.equals(intent.getAction())) {
//                Toast.makeText(VideoPlayerActivity.this,
//                        getText(R.string.video_exception_start_push), Toast.LENGTH_LONG).show();
                showWarningDialog(R.string.msg_warning_puserror);
//                VideoPlayerActivity.this.finish();
            }
        }
    }

    private void destroyPlay() {
        if (mPlayer != null) {
            mPlayer.stop();
            mPlayer.destroy();
        }
    }

    @Override
    protected void onDestroy() {
        stopPlay();
        destroyPlay();
        if (mReceiver != null) {
            unregisterReceiver(mReceiver);
        }
        super.onDestroy();
    }
}
