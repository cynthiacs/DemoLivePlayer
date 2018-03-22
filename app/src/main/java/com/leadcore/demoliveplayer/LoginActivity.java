package com.leadcore.demoliveplayer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        userEt = findViewById(R.id.useredit);
        pswEt = findViewById(R.id.passedit);
        signinbtn = findViewById(R.id.signinbtn);
        signinbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mMode == MODE_SIGNIN) {
                    Intent intent = new Intent(LoginActivity.this, VideoListActivity.class);
                    startActivity(intent);
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
    }

    @Override
    public void onBackPressed() {
        if (mMode == MODE_SIGNUP) {
            mMode = MODE_SIGNIN;
            signinbtn.setText(R.string.signin_btn);
            signupbtn.setVisibility(View.VISIBLE);
        }else {
            super.onBackPressed();
        }
    }
}
