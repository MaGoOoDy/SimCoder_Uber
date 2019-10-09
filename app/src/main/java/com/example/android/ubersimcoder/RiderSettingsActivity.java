package com.example.android.ubersimcoder;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class RiderSettingsActivity extends AppCompatActivity {

    private EditText mNameField, mPhoneField;
    private Button mBack, mConfirm;

    private FirebaseAuth mAuth;
    private DatabaseReference mRiderDatabase;

    private String riderID;
    private String mName;
    private String mPhone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rider_settings);

        mNameField = findViewById(R.id.name);
        mPhoneField = findViewById(R.id.phone);
        mBack = findViewById(R.id.back);
        mConfirm = findViewById(R.id.confirm);

        mAuth = FirebaseAuth.getInstance();
        riderID = mAuth.getCurrentUser().getUid();

        mRiderDatabase = FirebaseDatabase.getInstance().getReference().child("users").child("riders").child(riderID);

        // this function should come after the declaration of (mRiderDatabase)
        // this method will retrieve the rider info from database
        getRiderInfo();

        mConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveRiderInformation();
            }
        });

        mBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // here when we click (back) button we will not save any changes, so we just will finish the current Activity and return to the previous activity (RiderMapActivity)
                finish();
                return;
            }
        });

    }



    // this method will listen to the Rider info in the node users-->riders-->riserID, so it will retrieve rider info and populated into the (RiderSettingActivity)
    private void getRiderInfo(){
        mRiderDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() && dataSnapshot.getChildrenCount()>0){
                    Map<String,Object> map = (Map<String,Object>) dataSnapshot.getValue();

                    if(map.get("name") != null){
                        mName = map.get("name").toString();
                        mNameField.setText(mName);
                    }

                    if(map.get("phone") != null){
                        mPhone = map.get("phone").toString();
                        mPhoneField.setText(mPhone);
                    }
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }


    private void saveRiderInformation(){
        mName = mNameField.getText().toString();
        mPhone = mPhoneField.getText().toString();

        Map riderInfo = new HashMap();
        riderInfo.put("name", mName);
        riderInfo.put("phone", mPhone);
        mRiderDatabase.updateChildren(riderInfo);

        finish();
    }
}
