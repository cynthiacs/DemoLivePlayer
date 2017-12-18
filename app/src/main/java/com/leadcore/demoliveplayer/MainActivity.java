package com.leadcore.demoliveplayer;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.example.cloudmedia.CloudMedia;

public class MainActivity extends AppCompatActivity {

    private final static String TAG = "MainActivity";
    private EditText userEt;
    private EditText pswEt;
    private Button loginBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        userEt = (EditText)findViewById(R.id.useredit);
        pswEt = (EditText)findViewById(R.id.passedit);
        loginBtn = (Button)findViewById(R.id.loginbtn);
        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String userName = userEt.getText().toString();
                Intent intent = new Intent(MainActivity.this, VideoListActivity.class);
                startActivity(intent);
            }
        });
    }
}
