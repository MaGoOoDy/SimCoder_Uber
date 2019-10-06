package com.example.android.ubersimcoder;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Button btnDriverLogin, btnRiderLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        btnDriverLogin = findViewById(R.id.btn_driver);
        btnRiderLogin = findViewById(R.id.btn_rider);

        btnDriverLogin.setOnClickListener(this);
        btnRiderLogin.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {

            case R.id.btn_driver:
                Intent intent = new Intent(MainActivity.this,DriverLoginActivity.class);
                startActivity(intent);
                finish();
                break;

            case R.id.btn_rider:
                intent = new Intent(MainActivity.this,RiderLoginActivity.class);
                startActivity(intent);
                finish();

                break;
        }
    }
}
