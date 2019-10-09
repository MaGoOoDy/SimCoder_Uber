package com.example.android.ubersimcoder;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class RiderSettingsActivity extends AppCompatActivity {

    private EditText mNameField, mPhoneField;
    private Button mBack, mConfirm;
    private ImageView mProfileImage;

    private Uri resultUri;

    private FirebaseAuth mAuth;
    private DatabaseReference mRiderDatabase;

    private String riderID;
    private String mName;
    private String mPhone;
    private String mProfileImageUrl;
    private String TAG;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rider_settings);

        mNameField = findViewById(R.id.name);
        mPhoneField = findViewById(R.id.phone);
        mBack = findViewById(R.id.back);
        mConfirm = findViewById(R.id.confirm);
        mProfileImage = findViewById(R.id.profileImage);

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

        mProfileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, 1);
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

                    if(map.get("ProfileImageUrl") != null){
                        mProfileImageUrl = map.get("ProfileImageUrl").toString();
                        Glide.with(getApplication()).load(mProfileImageUrl).into(mProfileImage);
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

        // saving the profile image
        if(resultUri != null) {
            StorageReference imagePath = FirebaseStorage.getInstance().getReference().child("profile_images").child(riderID);

            Bitmap bitmap = null;
            try {
                bitmap = MediaStore.Images.Media.getBitmap(getApplication().getContentResolver(),resultUri);
            } catch (IOException e) {
                e.printStackTrace();
            }


            // compressing the photo so it will not take more space in Firebase Storage
            ByteArrayOutputStream boas = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 20, boas);
            byte[] data = boas.toByteArray();
            UploadTask uploadTask = imagePath.putBytes(data);

            // if the image not stored, we will finish the Activity and return.
            uploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {

                    finish();
                    return;
                }
            });


            // listening to the Storage node, if the image stored successfully in Firebase Storage we will save the Image Url into Firebase Database node (users --> riders --> riderID --> profileImageUrl)
            uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();

                    while (!uriTask.isSuccessful());
                    Uri downloadUri = uriTask.getResult();

                    final String imageURL = String.valueOf(downloadUri);
                    Log.d(TAG, "onSuccess: ImageURL = " + imageURL);

                    Map newImage = new HashMap();
                    newImage.put("ProfileImageUrl", imageURL);
                    mRiderDatabase.updateChildren(newImage);

                    // after successfully updating the node (), we will finish the activity.
                    finish();
                    return;

                }
            });





        }else{
            finish();
        }


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == 1 && resultCode == Activity.RESULT_OK){
            final Uri imageUri = data.getData();
            resultUri = imageUri;

            // populating the (mProfileImage) ImageView with the image came from the Uri
            mProfileImage.setImageURI(resultUri);
        }
    }
}
