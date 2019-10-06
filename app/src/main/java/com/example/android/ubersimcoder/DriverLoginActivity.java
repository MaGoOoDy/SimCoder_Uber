package com.example.android.ubersimcoder;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class DriverLoginActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText mEmail, mPassword;
    private Button mRegister, mLogin;
    private TextView mTextDriver;

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_login);

        mEmail = findViewById(R.id.Et_email);
        mPassword = findViewById(R.id.Et_password);
        mRegister = findViewById(R.id.btn_register);
        mLogin = findViewById(R.id.btn_login);
        mTextDriver = findViewById(R.id.txt_driver);

        mRegister.setOnClickListener(this);
        mLogin.setOnClickListener(this);


        mAuth = FirebaseAuth.getInstance();

        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {

                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

                if(user!=null){
                    Intent intent = new Intent(DriverLoginActivity.this, DriverMapActivity.class);
                    startActivity(intent);
                    finish();
                }
            }
        };

    }

    @Override
    public void onClick(View v) {

        final String email = mEmail.getText().toString();
        final String password = mPassword.getText().toString();

        switch (v.getId() ) {

            case R.id.btn_register:
                // creating the user
                mAuth.createUserWithEmailAndPassword(email,password).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (!task.isSuccessful()) {
                            Toast.makeText(DriverLoginActivity.this, "Error during registering", Toast.LENGTH_SHORT).show();

                            //Todo: -  better to make all the possibilities of the registeration errors ...... [ check the firebase docs ]

                        }else{

                            String user_id = mAuth.getCurrentUser().getUid();
                            DatabaseReference current_user_db = FirebaseDatabase.getInstance().getReference().child("users").child("drivers").child(user_id);
                            current_user_db.setValue(true);


                        }
                    }
                });
                break;

            case R.id.btn_login:
                // to do it later after finishing the [ RiderLoginActivity ]

                mAuth.signInWithEmailAndPassword(email,password).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (!task.isSuccessful()) {
                            Toast.makeText(DriverLoginActivity.this, "Error during logging", Toast.LENGTH_SHORT).show();
                            //Todo: -  better to make all the possibilities of the login errors ...... [ check the firebase docs ]

                        }else{
                            Toast.makeText(DriverLoginActivity.this, "log in successfully", Toast.LENGTH_SHORT).show();




                            // Todo :
                            //        - use (Intent) to go to the next Activity
                        }
                    }
                });



                break;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthStateListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mAuth.removeAuthStateListener(mAuthStateListener);
    }
}
