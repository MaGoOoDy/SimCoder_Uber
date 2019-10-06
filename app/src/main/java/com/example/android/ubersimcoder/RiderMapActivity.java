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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class RiderMapActivity extends FragmentActivity implements OnMapReadyCallback {

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

    // FirebaseDatabase Refs
    private DatabaseReference DriverAvailableRef;
    private DatabaseReference DriverRef;
    private DatabaseReference DriverWorkingRef;



    // vars 2
    private Button mLogout,mRequestRide;
    private LatLng pickupLocation;
    private int radius = 1;
    private boolean driverFound = false;
    private String driverFoundID;
    private ArrayList<String> DriverFoundList = new ArrayList<String>();
    private Boolean currentLogoutRiderStatus = false;
    private String rider_id;
    Marker DriverMarker;
    private Marker RiderMarker;
    private Boolean requestBol = false;
    GeoQuery geoQuery;
    private ValueEventListener driverLocationRefListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rider_map);

        // initiate instances
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mAuth = FirebaseAuth.getInstance();

        rider_id = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // instantiate Firebase Database Refs
        DriverAvailableRef = FirebaseDatabase.getInstance().getReference().child("Drivers Available For Request");



        // start receiving location updates
        getMyLocation();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);



        // initiate the vars
        mLogout = findViewById(R.id.logout);
        mRequestRide = findViewById(R.id.CallDriver);


        // OnClickListener
        mLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentLogoutRiderStatus = true;
                removeAvailableRider();

                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(RiderMapActivity.this,MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
                return;
            }
        });

        mRequestRide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(requestBol){
                    // if the request is cancelled, this will happen
                    // - clear everything happened because of the previous request
                    // - cancel the listeners.

                    requestBol = false;

                    // removing Listnenres
                    geoQuery.removeAllListeners();
                    DriverWorkingRef.removeEventListener(driverLocationRefListener);


                    // removing the RiderID from the (users -> drivers -> driverID ) node, this will happen by setting the node value to true.
                    if(driverFoundID != null){
                        DriverRef = FirebaseDatabase.getInstance().getReference().child("users").child("drivers").child(driverFoundID);
                        DriverRef.setValue(true);
                        driverFoundID = null;
                    }

                    // setting driverFound variable to false, so we initiall assuming that we didn't assigned any driver.
                    driverFound = false;

                    // resetting the raduis to 1
                    radius = 1;

                    // clear everything happened because of the previous request
                    String rider_id = mAuth.getCurrentUser().getUid();
                    DatabaseReference driverERef = FirebaseDatabase.getInstance().getReference("Customer Requests");
                    GeoFire geoFire = new GeoFire(driverERef);
                    geoFire.removeLocation(rider_id);


                    // removing rider Markup
                    if (DriverMarker != null){
                        DriverMarker.remove();
                    }

//                    if (RiderMarker != null){
//                        RiderMarker.remove();
//                    }

                    mRequestRide.setText("request a ride");

                }else{
                    // if the the request is not cancelled, we can make a new request.

                    requestBol = true;
                    pickupLocation = new LatLng(newLatLng.latitude, newLatLng.longitude);
                    addAvailableRiderForRequest(pickupLocation);

                    MarkerOptions markerOptions = new MarkerOptions()
                            .position(pickupLocation)
                            .title("pickup location")
                            .snippet("the driver will come to this location");
                    RiderMarker = mMap.addMarker(markerOptions);

                    mRequestRide.setText("finding a driver ...");

                    // calling the method [assignDriverForTheRequest()] to find the driver
                    assignDriverForTheRequest();


                }



            }
        });

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
                        mMap.animateCamera(CameraUpdateFactory.zoomTo(18));

                        Log.d(TAG, "onSuccess: Latitude = " + location.getLatitude());
                        Log.d(TAG, "onSuccess: Longitude = " + location.getLongitude());
                    } else {
                        Toast.makeText(RiderMapActivity.this, "Location is null", Toast.LENGTH_SHORT).show();
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

        // either we have permission since begining or obtained it after the dialog showed up, in both cases we will (start new location or get the last known location)
        // then move the camera to the location
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);

                for (Location location : locationResult.getLocations()) {

                    if (location != null) {
                        newLatLng = new LatLng(location.getLatitude(), location.getLongitude());

                        mMap.moveCamera(CameraUpdateFactory.newLatLng(newLatLng));
                        mMap.animateCamera(CameraUpdateFactory.zoomTo(19));  // TODO :  old value is 11

                        Log.d(TAG, "onSuccess: Latitude = " + location.getLatitude());
                        Log.d(TAG, "onSuccess: Longitude = " + location.getLongitude());
                    } else {
                        Toast.makeText(RiderMapActivity.this, "Location is null", Toast.LENGTH_SHORT).show();
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

        if(!currentLogoutRiderStatus){

            removeAvailableRider();
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

        int available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(RiderMapActivity.this);

        if(available == ConnectionResult.SUCCESS){
            //everything is fine and the user can make map requests
            Log.d(TAG, "isServicesOK: Google Play Services is working");
            return true;
        }
        else if(GoogleApiAvailability.getInstance().isUserResolvableError(available)){
            //an error occured but we can resolve it
            Log.d(TAG, "isServicesOK: an error occured but we can fix it");
            Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(RiderMapActivity.this, available, ERROR_DIALOG_REQUEST);
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
    //this method will get the rider location and send his location to
    // the database, so when riders make a trip request the location of each available drivers will
    // be used to calculate the driver that need minimum time to reach to the rider.
    private void addAvailableRiderForRequest(LatLng pickupLocation) {

        // get current user id from Firebase Authentication
//        String rider_id = mAuth.getCurrentUser().getUid();
        double latitude = pickupLocation.latitude;
        double longitude = pickupLocation.longitude;

        DatabaseReference riderRef = FirebaseDatabase.getInstance().getReference("Customer Requests");
        GeoFire geoFire = new GeoFire(riderRef);
        geoFire.setLocation(rider_id, new GeoLocation(latitude,longitude));


    }


    // this method will remove the driver from the (AvailableDriver) node, so we know that this driver is not available.
    private void removeAvailableRider(){
        // get current user id from Firebase Authentication
        String rider_id = mAuth.getCurrentUser().getUid();

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Customer Requests").child(rider_id);
        databaseReference.removeValue();


        DatabaseReference driverERef = FirebaseDatabase.getInstance().getReference("Customer Requests");
        GeoFire geoFire = new GeoFire(driverERef);
        geoFire.removeLocation(rider_id);
    }


    // this method will do the following :
    // 1- find the closest drivers.
    // 2- calculate the required ETA for each driver to reach the rider.
    // 3- make a list of the driver based on the ETA
    // 4- start send the (Trip request) to the drivers based on there order in the list, if a driver accept the request then
    //      this method will assign that driver for this trip
    private void assignDriverForTheRequest(){

        // initially we will make sure that there is no driver in the DriverFoundList, so wee use the clear() method to remove all elements inside the DriverFoundList
//        DriverFoundList.clear();


        GeoFire geoFire = new GeoFire(DriverAvailableRef);


        geoQuery = geoFire.queryAtLocation(new GeoLocation(pickupLocation.latitude,pickupLocation.longitude), radius);
        geoQuery.removeAllListeners();

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                // any time a driver found withing the radius this (onKeyEntered) will be called, and we got the (Key + location)
                // after all drivers found, the ( onGeoQueryReady ) will be called
//                DriverFoundList.add(key);


                if(!driverFound && requestBol){
                    driverFound = true;
                    driverFoundID = key;


                    // TODO :   here we will add a method to store the 5 closest drivers, then we send request to these drivers based on their distance to the rider.
                    //          so if the driver accept the ride, we will send him the complete details for the rider and the trip
                    DriverRef = FirebaseDatabase.getInstance().getReference().child("users").child("drivers").child(driverFoundID);
                    HashMap driverMap = new HashMap();
                    driverMap.put("CustomerID",rider_id);
                    DriverRef.updateChildren(driverMap);

                    // TODO :   if the driver accept the trip, we will do the followings:
                    //          1- notify the rider with the driver details
                    //          2- show the the rider the ability to massage the driver
                    //          3- if the driver arrived to the rider location, the following will happen:
                    //                  - the rider will be notified that hte driver is arrived.
                    //                  - the (show up) timer will start
                    //                  - a button will appear to both of them to allow them call each other.


                    mRequestRide.setText("getting driver details ...");


                    gettingAssignedDriverLocation();


                }


            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                // when all driver within the radius have been found, this method ( onGeoQueryReady ) will be called.
                if(!driverFound){
                    radius = radius + 1 ;
                    assignDriverForTheRequest();
                }

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });

    }



    private void gettingAssignedDriverLocation() {

        // this node will be only available when the driver accept the request, so when the driver accept the request his ID will be added to the (DriverLocationRef) node, and his
        // location will be stored as a child, with the help of that we set a listener to detect when the driver accept the request.
        DriverWorkingRef = FirebaseDatabase.getInstance().getReference().child("Drivers Working").child(driverFoundID).child("l");
;        driverLocationRefListener = DriverWorkingRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.d(TAG, "onDataChange: enter the methos");
                // we have to check if the (dataSnapshot) is exist, this to avoid the crash.
                if(dataSnapshot.exists() && requestBol){
                    Log.d(TAG, "snapshoooot existtttt");
                    List<Object> driverLocationMap = (List<Object>) dataSnapshot.getValue();

                    double LocationLat = 0; // this is only to instantiate the variable
                    double LocationLong = 0; // this is only to instantiate the variable

                    mRequestRide.setText("Driver Found..");

                    // since the (DriverLocationRef) node now has the driver location, we have to convert the value of the (0) and (1) fields from String to double.
                    // why, because GeoFire store the location as a String.
                    if(driverLocationMap.get(0) != null){
                        LocationLat = Double.parseDouble(driverLocationMap.get(0).toString());
                    }

                    if(driverLocationMap.get(1) != null){
                        LocationLong = Double.parseDouble(driverLocationMap.get(1).toString());
                    }

                    // creating a marker with the driver location to appear in the rider screen
                    LatLng DriverLatLong = new LatLng(LocationLat,LocationLong);

                    // if the driver details lost for any reason ( driver phone switch off, Driver GPS switch off, driver cancel the request after he accepted... etc)
                    // we need to remove his MArker location from the Map
                    if(DriverMarker != null){
                        DriverMarker.remove();
                    }

                    // TODO : create a specific method with the name ( gettingDistanceAndDuration() ) in order to retrieve the Distance and the Duration of the Trip.
                    //      Follow MitchTabian Video no.19 from his (Google Maps and Direction API course) in order to get the Distance and Estimated Duration of the Effective path(direction) between the Driver and the Rider

                    // Rider Location
                    Location location1= new Location("");
                    location1.setLatitude(pickupLocation.latitude);
                    location1.setLongitude(pickupLocation.longitude);

                    // Driver Location
                    Location location2= new Location("");
                    location2.setLatitude(DriverLatLong.latitude);
                    location2.setLongitude(DriverLatLong.longitude);

                    float distance = location1.distanceTo(location2);

                    // to notify the rider that the driver is arriving
                    if(distance < 100) {
                        mRequestRide.setText("Driver is here");
                    }else{
                        mRequestRide.setText("Driver Found: " + String.valueOf(distance));
                    }





                    DriverMarker = mMap.addMarker(new MarkerOptions()
                            .position(DriverLatLong)
                            .title("Your Driver Location"));

                    Log.d(TAG, "onDataChange: distance " + String.valueOf(distance));
                }else{
                    Log.d(TAG, "onDataChange: no datasnaaaaaapshooot");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }



}
