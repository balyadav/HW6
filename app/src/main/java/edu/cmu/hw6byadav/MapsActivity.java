package edu.cmu.hw6byadav;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends MainActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private List<Address> addressList = null;
    Location lastKnownLocation = null;
    LatLngBounds.Builder builder = null;
    float ETAinHours = 0f;
    private static final float carSpeed = 40f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        String gpsProvider = LocationManager.GPS_PROVIDER;
        LocationManager locationManager =
                (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        lastKnownLocation = locationManager.getLastKnownLocation(gpsProvider);
        Log.v("lastKnownLocation",lastKnownLocation.toString());
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            // ActivityCompat#requestPermissions here to request the missing permissions, and then overriding
            return;
        }

        Button searchLocation = (Button)findViewById(R.id.searchLocation);
        searchLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onMapSearch(v);
            }
        });
        AutoCompleteTextView editText = (AutoCompleteTextView) findViewById(R.id.editText);
        Address[] addressArray = new Address[5];
        addressArray[0] = new Address(Locale.CANADA);
        addressArray[1] = new Address(Locale.US);
        addressArray[2] = new Address(Locale.CHINA);
        addressArray[3] = new Address(Locale.FRENCH);
        addressArray[4] = new Address(Locale.GERMANY);
        ArrayAdapter<Address> arrayAdapter = new ArrayAdapter<Address>(getApplicationContext(), android.R.layout.simple_list_item_2, addressArray);
        editText.setAdapter(arrayAdapter);
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
    }

    public void onMapSearch(final View view){
        AutoCompleteTextView editText = (AutoCompleteTextView) findViewById(R.id.editText);
        String location = editText.getText().toString();
//        List<Address> addressList = null;
        if(location != null || !location.equals("")){
            Geocoder geocoder = new Geocoder(this);
            try{
                addressList = geocoder.getFromLocationName(location, 3);
                Log.v("addressList", addressList.toString());
            } catch(IOException e){
                e.printStackTrace();
                Log.v("IOException occured", "true");
            }
            if(addressList.isEmpty()){
                new AlertDialog.Builder(MapsActivity.this)
                        .setTitle("No results found!")
                        .setMessage("No results found for input! Refine your search!")
                        .setCancelable(false)
                        .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Log.v("Dialog displayed", "OK clicked");
                            }
                        }).show();
                return;
            }
            mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                @Override
                public void onMapClick(LatLng latLng) {
                    hideKeyboard(view);
                }
            });
            Address address = addressList.get(0); //First result from search
            Log.v("address fetched", address.toString());
            LatLng goTo = new LatLng(address.getLatitude(), address.getLongitude());
            Log.v("goto toString()", goTo.toString());
            mMap.addMarker(new MarkerOptions().position(
                    new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude()))
                    .title("Current Location")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));;
            if(lastKnownLocation != null) {
                builder = new LatLngBounds.Builder().include(goTo).include(new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude()));
                ETAinHours = calculateDistance(address, lastKnownLocation)/convertCarSpeedToMetersPerHour();
            }else{
                builder = new LatLngBounds.Builder().include(goTo).include(new LatLng(-10, 154));
            }
            mMap.addMarker(new MarkerOptions().position(goTo).title(Float.toString(ETAinHours) + " hours in ETA"));
            mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 100));
            hideKeyboard(view);
        }
    }


    private float calculateDistance(Address from, Location to){
        if(from == null || to == null)
            return 0f;
        Location fromLocation = new Location("");
        fromLocation.setLatitude(from.getLatitude());
        fromLocation.setLongitude(from.getLongitude());

        return fromLocation.distanceTo(to);
    }

    private float convertCarSpeedToMetersPerHour(){
        return carSpeed * 1609.34f;
    }

    public void hideKeyboard(View view) {
        InputMethodManager inputMethodManager =(InputMethodManager)getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
}
