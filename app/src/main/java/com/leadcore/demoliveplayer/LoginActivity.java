package com.leadcore.demoliveplayer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Animatable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.cmteam.cloudmedia.CloudMedia;
import com.leadcore.demoliveplayer.customviews.CustomDialog;

/**
 * Created by Shenqing on 2018/3/6.
 */

public class LoginActivity extends Activity {
    private final static String TAG = "LoginActivity";
    private EditText userEt;
    private EditText pswEt;
    private Button signinbtn;
    private Button signupbtn;
    private final static int MODE_SIGNIN = 0;
    private final static int MODE_SIGNUP = 1;
    private int mMode = MODE_SIGNIN;
    private CloudMedia mCloudMedia;
    private final static String IP = "139.224.128.15";//"192.168.199.68";//
    private final static String PORT = "8085";
    private final static int MSG_SIGNIN_RESULT = 0;
    private final static int MSG_SIGNUP_RESULT = 1;
    private String mAccount;
    private CustomDialog mWaitDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        userEt = findViewById(R.id.useredit);
        userEt.setText("A159308");
        pswEt = findViewById(R.id.passedit);
        pswEt.setText("123456");
        signinbtn = findViewById(R.id.signinbtn);
        signinbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mMode == MODE_SIGNIN) {
                    if (userEt.getText().length() <= 0) {
                        Log.d(TAG, "user name can't be null");
                        Toast.makeText(LoginActivity.this,R.string.user_warning,Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (pswEt.getText().length() <= 0) {
                        Log.d(TAG, "password  can't be null");
                        Toast.makeText(LoginActivity.this,R.string.psw_warning,Toast.LENGTH_SHORT).show();
                        return;
                    }
                    mAccount = userEt.getText().toString();
                    showWaitingDialog();
                    signin();
                }
            }
        });
        signupbtn = findViewById(R.id.signupbtn);
        signupbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mMode = MODE_SIGNUP;
                signupbtn.setVisibility(View.GONE);
                signinbtn.setText(R.string.signup_btn);
            }
        });
        mCloudMedia = CloudMedia.get();
    }

    @SuppressLint("HandlerLeak")
    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_SIGNIN_RESULT:
                    dismissWaitDialog();
                    if ((boolean)msg.obj) {
                        Intent intent = new Intent(LoginActivity.this, VideoListActivity.class);
                        intent.putExtra("account", mAccount);
                        startActivity(intent);
                    }else {
                        Toast.makeText(LoginActivity.this, R.string.signin_failed, Toast.LENGTH_SHORT).show();
                    }
                    break;
                case MSG_SIGNUP_RESULT:
                    break;
            }
        }
    };

    private void signin() {
        new Thread() {
            @Override
            public void run() {
                boolean loginsuccess = mCloudMedia.login(IP, PORT, mAccount, pswEt.getText().toString());
                Log.d(TAG, "get loginresult:"+loginsuccess);
                mHandler.sendMessage(mHandler.obtainMessage(MSG_SIGNIN_RESULT, loginsuccess));
            }
        }.start();
    }

    private void showWaitingDialog() {
        if (mWaitDialog != null) {
            if (mWaitDialog.isShowing())
                return;
            mWaitDialog.show();
            return;
        }
        LayoutInflater inflater = (LayoutInflater)this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View waitView = inflater.inflate(R.layout.waiting_dialog, null);
        mWaitDialog = new CustomDialog.Builder(this)
                .create(waitView, R.style.MyWaitDailog, Gravity.CENTER);
        ImageView images = waitView.findViewById(R.id.images);
        ((Animatable)images.getDrawable()).start();
        mWaitDialog.setDialogOnKeyDownListner(new CustomDialog.DialogOnKeyDownListner() {
            @Override
            public void onKeyDownListener(int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    Log.d(TAG, "Dialog back key down");
                    dismissWaitDialog();
                }
            }
        });
        mWaitDialog.show();
    }

    private void dismissWaitDialog() {
        if (mWaitDialog != null) {
            mWaitDialog.dismiss();
            mWaitDialog = null;
        }
    }

    @Override
    public void onBackPressed() {
        if (mWaitDialog != null && mWaitDialog.isShowing()) {
            dismissWaitDialog();
            return;
        }
        if (mMode == MODE_SIGNUP) {
            mMode = MODE_SIGNIN;
            signinbtn.setText(R.string.signin_btn);
            signupbtn.setVisibility(View.VISIBLE);
        }else {
            super.onBackPressed();
        }
    }
}
