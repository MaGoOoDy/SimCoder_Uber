package com.example.android.ubersimcoder;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.bumptech.glide.Glide;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;
import java.util.Map;

public class DriverMapActivity extends FragmentActivity implements OnMapReadyCallback {

    // constants
    private static final String TAG = "DriverMapActivity";
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 100;
    private static final int PERMISSIONS_REQUEST_ENABLE_GPS = 101;
    private static final int ERROR_DIALOG_REQUEST = 102;

    // vars
    private GoogleMap mMap;
    private boolean mLocationPermissionGranted = false;
//    private boolean everythingIsOK = false;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallback;
    private LatLng newLatLng;

    // firebase instances
    private FirebaseDatabase mFirebaseDatabase;
    private FirebaseAuth mAuth;
    private DatabaseReference AssignedRiderRef;
    private DatabaseReference AssignedRiderLocationRef;
    private ValueEventListener AssignedRiderLocationListener;

    // vars 2
    private Button mLogout;
    private LinearLayout mRiderInfo;
    private ImageView mRiderPrfileImage;
    private TextView mRiderName;
    private TextView mRiderPhone;
    private Boolean currentLogoutDriverStatus = false;
    private String driverID;
    private String riderId = "";
    Marker RiderMarker;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_map);

        // initiate instances
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mAuth = FirebaseAuth.getInstance();

        driverID = mAuth.getCurrentUser().getUid();

        // start receiving location updates
        getMyLocation();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);



        // initiate the vars
        mLogout = findViewById(R.id.logout);
        mRiderInfo = findViewById(R.id.riderInfo);
        mRiderPrfileImage = findViewById(R.id.riderProfileImage);
        mRiderName = findViewById(R.id.riderName);
        mRiderPhone = findViewById(R.id.riderPhone);




        // OnClickListeners
        mLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                currentLogoutDriverStatus = true;

                // we are removing driver from database before Stopping this activity when using the Intent, beacuse the Intent will call onStop() method of the Activity Lifecycle
                removeAvailableDriver();

                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(DriverMapActivity.this,MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
                return;
            }
        });


        getAssignedRider();

    }




    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        mMap.setMyLocationEnabled(true);
    }




    // in this method we receive the location updates
    private void getMyLocation(){

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            // permission is granted
            // and we can now use getLastLocation method
            mFusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (location != null) {
                        newLatLng = new LatLng(location.getLatitude(), location.getLongitude());

                        mMap.moveCamera(CameraUpdateFactory.newLatLng(newLatLng));
                        mMap.animateCamera(CameraUpdateFactory.zoomTo(11));

                        Log.d(TAG, "onSuccess: Latitude = " + location.getLatitude());
                        Log.d(TAG, "onSuccess: Longitude = " + location.getLongitude());
                    } else {
                        Toast.makeText(DriverMapActivity.this, "Location is null", Toast.LENGTH_SHORT).show();
                    }
                }
            });

        } else {

            // permission is not granted.
            // we have to request the user to grant the the ACCESS_FINE_LOCATION permission.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
            }
        }

        // either we have permission since beginning or obtained it after the dialog showed up, in both cases we will (start new location or get the last known location)
        // then move the camera to the location
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);

                for (Location location : locationResult.getLocations()) {

                    if (location != null) {
                        newLatLng = new LatLng(location.getLatitude(), location.getLongitude());

                        mMap.moveCamera(CameraUpdateFactory.newLatLng(newLatLng));
                        mMap.animateCamera(CameraUpdateFactory.zoomTo(17));  // TODO :  old value is 11



                        // ToDo : NEED to adjust this method ro minimize the number of requsts to the database, so that it will do the following:
                        //  (1) - it has to check if the friver is ONLINE mode, if yes it will start update location ( step 2 ), if the Driver is on Offline mode it will update only a local variable and will not send it
                        //      to database unless the driver change its mode to ONLINE mode.
                        //  (2) - if driver is ONLINE mode, it will not send location updates to database unless if there is change in the location with more than 30 meters from previous location
                        addAvailableDriver();

                        Log.d(TAG, "onSuccess: Latitude = " + location.getLatitude());
                        Log.d(TAG, "onSuccess: Longitude = " + location.getLongitude());
                    } else {
                        Toast.makeText(DriverMapActivity.this, "Location is null", Toast.LENGTH_SHORT).show();
                    }
                }

            }
        };
    }






    private void startLocationUpdates() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            mFusedLocationProviderClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null);


        }else{
            // permission is not granted.
            // we have to request the user to grant the the ACCESS_FINE_LOCATION permission.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
            }
        }

    }

    private void stopLocationUpdates() {
        mFusedLocationProviderClient.removeLocationUpdates(mLocationCallback);
    }



    @Override
    protected void onResume() {
        super.onResume();

        if(checkMapServices()) {
            if(mLocationPermissionGranted){
                startLocationUpdates();
            }else{
                getLocationPermission();
            }
        }


    }



    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if(!currentLogoutDriverStatus){

            removeAvailableDriver();
        }
    }





    private boolean checkMapServices(){
        if(isServicesOK()){
            if(isMapsEnabled()){
                return true;
            }
        }
        return false;
    }

    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("This application requires GPS to work properly, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        Intent enableGpsIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivityForResult(enableGpsIntent, PERMISSIONS_REQUEST_ENABLE_GPS);
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    public boolean isMapsEnabled(){
        final LocationManager manager = (LocationManager) getSystemService( Context.LOCATION_SERVICE );

        if ( !manager.isProviderEnabled( LocationManager.GPS_PROVIDER ) ) {
            buildAlertMessageNoGps();
            return false;
        }
        return true;
    }

    private void getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;

            // at this point all the required conditions ( google play services, GPS and the permissions) are obtained properly
            // we can now start receiving the location updates
            startLocationUpdates();

        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    public boolean isServicesOK(){
        Log.d(TAG, "isServicesOK: checking google services version");

        int available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(DriverMapActivity.this);

        if(available == ConnectionResult.SUCCESS){
            //everything is fine and the user can make map requests
            Log.d(TAG, "isServicesOK: Google Play Services is working");
            return true;
        }
        else if(GoogleApiAvailability.getInstance().isUserResolvableError(available)){
            //an error occured but we can resolve it
            Log.d(TAG, "isServicesOK: an error occured but we can fix it");
            Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(DriverMapActivity.this, available, ERROR_DIALOG_REQUEST);
            dialog.show();
        }else{
            Toast.makeText(this, "You can't make map requests", Toast.LENGTH_SHORT).show();
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        mLocationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                } else {
                Toast.makeText(this, "app will not work because you didn't granted the required permission", Toast.LENGTH_SHORT).show();
//                finish();
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: called.");
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ENABLE_GPS: {
                if(mLocationPermissionGranted){

                    // at this point all the required conditions ( google play services, GPS and the permissions) are obtained properly
                    // we can now start receiving the location updates
                    startLocationUpdates();
                }
                else{
                    getLocationPermission();
                }
            }
        }

    }





    //this method will check if the driver is available ( not offline ) and send his location to
    // the database, so when riders make a trip request the location of each available drivers will
    // be used to calculate the driver that need minimum time to reach to the rider.
    private void addAvailableDriver() {

        // get current user id from Firebase Authentication
        String driver_id = mAuth.getCurrentUser().getUid();
        double latitude = newLatLng.latitude;
        double longitude = newLatLng.longitude;



        DatabaseReference driverAvailability = FirebaseDatabase.getInstance().getReference("Drivers Available For Request");
        GeoFire geoFireDriverAvailability = new GeoFire(driverAvailability);

        DatabaseReference driverWorking = FirebaseDatabase.getInstance().getReference("Drivers Working");
        GeoFire geoFireDriverWorking = new GeoFire(driverWorking);




        switch(riderId){
            case "":
                geoFireDriverWorking.removeLocation(driver_id);
                geoFireDriverAvailability.setLocation(driver_id, new GeoLocation(latitude,longitude));
                break;

            default:
                 geoFireDriverAvailability.removeLocation(driver_id);
                 geoFireDriverWorking.setLocation(driver_id, new GeoLocation(latitude,longitude));
                 break;


        }
    }


    // this method will remove the driver from the (AvailableDriver) node, so we know that this driver is not available.
    private void removeAvailableDriver(){
        // get current user id from Firebase Authentication
        String driver_id = mAuth.getCurrentUser().getUid();

        DatabaseReference driverERef = FirebaseDatabase.getInstance().getReference("Drivers Available For Request");
        GeoFire geoFire = new GeoFire(driverERef);
        geoFire.removeLocation(driver_id);



    }


    private void getAssignedRider() {
        AssignedRiderRef = FirebaseDatabase.getInstance().getReference().child("users").child("drivers").child(driverID).child("CustomerID");

         AssignedRiderRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){

                    // retrieving Rider ID from database
                    riderId = dataSnapshot.getValue().toString();

                    getAssignedRiderLocation();
                    getAssignedRiderInfo();

                }else{
                    // if the rider cancel the request, the following will happen:
                    // - remove marker
                    // - remove listeners

                    riderId = "";

                    // remove marker
                    if(RiderMarker != null) {
                        RiderMarker.remove();
                    }

                    // remove listeners
                    // we have to check first if the Listener is exist or not
                    if (AssignedRiderLocationListener != null){
                        AssignedRiderLocationRef.removeEventListener(AssignedRiderLocationListener);
                    }

                    // if we don't have a rider or we don't have a trip assigned we have to set the visisbility of (riderInfo) LinearLayout to (GONE)
                    mRiderInfo.setVisibility(View.GONE);


                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void getAssignedRiderLocation() {
        AssignedRiderLocationRef = FirebaseDatabase.getInstance().getReference().child("Customer Requests").child(riderId).child("l");

        AssignedRiderLocationListener = AssignedRiderLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() && !riderId.equals("") ){
                    List<Object> RiderLocationMap = (List<Object>) dataSnapshot.getValue();

                    double LocationLat = 0; // this is only to instantiate the variable
                    double LocationLong = 0; // this is only to instantiate the variable


                    // since the (RiderLocationMap) node now has the Rider location, we have to convert the value of the (0) and (1) fields from String to double.
                    // why, because GeoFire store the location as a String.
                    if(RiderLocationMap.get(0) != null){
                        LocationLat = Double.parseDouble(RiderLocationMap.get(0).toString());
                    }

                    if(RiderLocationMap.get(1) != null){
                        LocationLong = Double.parseDouble(RiderLocationMap.get(1).toString());
                    }


                    // creating a marker with the Rider location to appear in the rider screen
                    LatLng RiderLatLong = new LatLng(LocationLat,LocationLong);

                    RiderMarker = mMap.addMarker(new MarkerOptions()
                            .position(RiderLatLong)
                            .title("Pickup Location"));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }


    // this method will listen to the Rider info in the node users-->riders-->riserID, so it will retrieve rider info and populated into the (RiderSettingActivity)
    private void getAssignedRiderInfo(){
//        // firstly we have to change the visibility of riderInfo Linear Layout to (VISIBLE)
//        mRiderInfo.setVisibility(View.VISIBLE);

        DatabaseReference mRiderDatabase = FirebaseDatabase.getInstance().getReference().child("users").child("riders").child(riderId);
        mRiderDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() && dataSnapshot.getChildrenCount()>0){
                    Map<String,Object> map = (Map<String,Object>) dataSnapshot.getValue();

                    // firstly we have to change the visibility of riderInfo Linear Layout to (VISIBLE)
                    mRiderInfo.setVisibility(View.VISIBLE);

                    if(map.get("name") != null){
                        mRiderName.setText(map.get("name").toString());
                    }

                    if(map.get("phone") != null){
                        mRiderPhone.setText(map.get("phone").toString());
                    }

                    if(map.get("ProfileImageUrl") != null){
                        Glide.with(getApplication()).load(map.get("ProfileImageUrl").toString()).into(mRiderPrfileImage);
                    }
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
}


