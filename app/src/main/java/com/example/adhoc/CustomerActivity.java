package com.example.adhoc;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class CustomerActivity extends AppCompatActivity {
    Button mLogout, mRequest, mSettings;
    LatLng destination;
    String TAG = "CustomerActivity Debug";
    String rideId;
    int size = 0;

    private LinearLayout mDriverInfo;
    private TextView mDriverName, mDriverPhone, mDriverCar;
    private RadioGroup mRadioGroup;
    private String requestService;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer);

        mDriverInfo = findViewById(R.id.driverInfo);
        mDriverName = findViewById(R.id.driverName);
        mDriverPhone = findViewById(R.id.driverPhone);
        mDriverCar = findViewById(R.id.driverCar);

        mRadioGroup = findViewById(R.id.radioGroup);
        mRadioGroup.check(R.id.small);

        mLogout = findViewById(R.id.logout);
        mRequest = findViewById(R.id.request);
        mSettings = findViewById(R.id.settings);

        mLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(CustomerActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        });

        mRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (destination == null){
                    Toast.makeText(CustomerActivity.this, "Select a destination!", Toast.LENGTH_SHORT).show();
                    return;
                }
                String userid = FirebaseAuth.getInstance().getUid();
                DatabaseReference ref = FirebaseDatabase.getInstance().getReference("customerRequest").child(userid);
                ref.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                // get total available quest
                                size = (int) dataSnapshot.getChildrenCount()+1;
                            }
                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });
                int selectedId = mRadioGroup.getCheckedRadioButtonId();
                final RadioButton radioButton = findViewById(selectedId);

                if (radioButton.getText() == null) {
                    return;
                }

                requestService = radioButton.getText().toString();

                GeoFire geofire = new GeoFire(ref);
                rideId = userid+size;
                geofire.setLocation(userid+size, new GeoLocation(destination.latitude, destination.longitude), new GeoFire.CompletionListener() {
                    @Override
                    public void onComplete(String key, DatabaseError error) {
                        mDriverInfo.setVisibility(View.GONE);
                        mDriverName.setText("");
                        mDriverPhone.setText("");
                        mDriverCar.setText("");
                    }
                });
                mRequest.setText("Getting your driver.....");
                getClosestDriver();
            }
        });
        handleSearchBar();
        mSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CustomerActivity.this, CustomerSettingsActivity.class);
                startActivity(intent);
                return;
            }
        });
    }
    public void toast(final String text) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            public void run() {
                Toast.makeText(CustomerActivity.this, text, Toast.LENGTH_SHORT).show();
            }
        });
    }
    private int radius = 1;
    private Boolean driverFound = false;
    private String driverFoundID;
    public void getClosestDriver(){
        if (radius>50 && !driverFound){
            toast("Driver not found!");
            mRequest.setText("Call Uber");
            radius = 1;
            return;
        }
        DatabaseReference driverLocation = FirebaseDatabase.getInstance().getReference().child("driversAvailable");

        GeoFire geoFire = new GeoFire(driverLocation);
        GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(destination.latitude, destination.longitude), radius);
        geoQuery.removeAllListeners();

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if (!driverFound){
                    DatabaseReference mCustomerDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(key);
                    mCustomerDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if(dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0){
                                Map<String, Object> driverMap = (Map <String, Object>)dataSnapshot.getValue();
                                if (driverFound){
                                    return;
                                }

                                if(driverMap.get("service").equals(requestService) ){
                                    driverFound = true;
                                    driverFoundID = dataSnapshot.getKey();

                                    DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundID).child("customerRideId");
                                    String customerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                                    HashMap map = new HashMap();
                                    map.put("customerId", customerId);
                                    map.put("rideId", rideId);
                                    driverRef.updateChildren(map);
                                    radius = 1;

                                    getDriverInfo();
                                    mRequest.setText("Found Driver!");
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });

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
                if (!driverFound)
                {
                    radius++;
                    getClosestDriver();
                }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }

    private void getDriverInfo(){
        mDriverInfo.setVisibility(View.VISIBLE);

        DatabaseReference mDriversDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundID);
        mDriversDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0){
                    Map<String, Object> map = (Map <String, Object>)dataSnapshot.getValue();
                    if(map.get("name") != null ){
                        mDriverName.setText(map.get("name").toString());
                    }
                    if(map.get("phone") != null ){
                        mDriverPhone.setText(map.get("phone").toString());
                    }
                    if(map.get("car") != null ){
                        mDriverCar.setText(map.get("car").toString());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void handleSearchBar(){
        Places.initialize(getApplicationContext(),"AIzaSyC1S7BUHYryWvinNdNtu1s7mn-d1IMgXP0");
        PlacesClient placesClient = Places.createClient(this);

        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);

        autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG));

        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                // TODO: Get info about the selected place.
                destination = place.getLatLng();
                Log.i(TAG, "Place: " + place.getName() + ", " + place.getId()+","+place.getLatLng());
            }

            @Override
            public void onError(Status status) {
                // TODO: Handle the error.
                Log.i(TAG, "An error occurred: " + status);
            }
        });
    }
}
