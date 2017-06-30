package edu.cmu.hw6byadav;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.Toast;

import com.twitter.sdk.android.core.models.Tweet;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.ConfigurationBuilder;

public class TweetActivity extends MainActivity implements LocationListener {
    private LocationManager mLocationManager = null;
    private static final int LOCATION_INTERVAL = 100;
    private static final float LOCATION_DISTANCE = 0f;
    private Location mLastLocation;
    private final String TAG = "BALJEETTWEETADDRESSAPP";
    private String addressText;

    public static String TWITTER_CONSUMER_KEY = "ozX8AZUOf19wEb7JviZeEFHR1";
    public static String TWITTER_CONSUMER_SECRET = "548wmkcRj37L9DzOcmT4TVwFTSWtNIMjn2FeeS5l3u6qpTshmI";
    public static String PREFERENCE_TWITTER_LOGGED_IN="TWITTER_LOGGED_IN";

    private final int REQUEST_PERMISSION_FINE_LOCATION = 1;

    private Button tweetButton;
    Dialog auth_dialog;
    WebView web;
    SharedPreferences pref;
    twitter4j.Twitter twitter;
    RequestToken requestToken;
    AccessToken accessToken;
    String oauth_url, oauth_verifier, profile_url;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tweet);

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Consider calling
            // ActivityCompat#requestPermissions here to request the missing permissions, and then overriding
            Log.v("fine loc permission", "not granted");
            ActivityCompat.requestPermissions(TweetActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSION_FINE_LOCATION);
        }

        pref = PreferenceManager.getDefaultSharedPreferences(TweetActivity.this);
        SharedPreferences.Editor edit = pref.edit();
        edit.putString("CONSUMER_KEY", TWITTER_CONSUMER_KEY);
        edit.putString("CONSUMER_SECRET", TWITTER_CONSUMER_SECRET);
        edit.commit();

        mLocationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        mLastLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        try {
            mLocationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
                    TweetActivity.this);
        } catch (java.lang.SecurityException ex) {
            Log.v(TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.v(TAG, "gps provider does not exist " + ex.getMessage());
        } catch (Exception e){
            Log.v("other exception caught", e.getMessage());
        }

        twitter = new TwitterFactory().getInstance();
        twitter.setOAuthConsumer(pref.getString("CONSUMER_KEY", ""), pref.getString("CONSUMER_SECRET", ""));

        tweetButton = (Button) findViewById(R.id.tweetLocationButton);
        tweetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isOnline())
                    checkIn();
                else{
                    new AlertDialog.Builder(TweetActivity.this)
                        .setTitle("No connectivity!")
                        .setMessage("Please enable wifi or connect to phone network to fetch location.")
                        .setCancelable(false)
                        .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Log.v("Dialog displayed", "OK clicked");
                            }
                        }).show();
                }
            }
        });
    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation.set(location);
        Log.v("onlocationChanged", "true");
        Log.v("new location", location.toString());
        //prepare the addressTweet singleton
        if(isOnline())
            new fetchAddress(location).execute();

    }
    public boolean isOnline() {
        ConnectivityManager conMgr = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = conMgr.getActiveNetworkInfo();

        if(netInfo == null || !netInfo.isConnected() || !netInfo.isAvailable()){
//            Toast.makeText(TweetActivity.this, "No Internet connection!", Toast.LENGTH_LONG).show()
            return false;
        }
        return true;
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.v("onStatusChanged", "true");
    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.v("onProviderEnabled", "true");
    }

    @Override
    public void onProviderDisabled(String provider) {
        Toast.makeText(TweetActivity.this, "Please enable location service", Toast.LENGTH_SHORT).show();
    }

    private void checkIn(){
        if (!pref.getBoolean(PREFERENCE_TWITTER_LOGGED_IN,false)){
            new TokenGet().execute(); //no Token obtained, first time use
        }else{
            new PostTweet().execute(); //when Tokens are obtained , ready to Post
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
//            Log.v("inside switch", "true");
            case REQUEST_PERMISSION_FINE_LOCATION:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(TweetActivity.this, "Permission Granted!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(TweetActivity.this, "Permission Denied!", Toast.LENGTH_SHORT).show();
                }
        }

    }

    private class PostTweet extends AsyncTask<String, String, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        protected String doInBackground(String... args) {

            ConfigurationBuilder builder = new ConfigurationBuilder();
            builder.setOAuthConsumerKey(pref.getString("CONSUMER_KEY", ""));
            builder.setOAuthConsumerSecret(pref.getString("CONSUMER_SECRET", ""));

            AccessToken accessToken = new AccessToken(pref.getString("ACCESS_TOKEN", ""), pref.getString("ACCESS_TOKEN_SECRET", ""));
            twitter4j.Twitter twitter = new TwitterFactory(builder.build()).getInstance(accessToken);

            AddressTweet addressTweet = AddressTweet.getInstance();
            String status = "@08723Mapp" + " " +
                    "byadav " + addressTweet.getAddressToTweet();
            Log.v("Tweet data:", status);
            twitter4j.Status response = null;
//            try {
//                response = twitter.updateStatus(status);
//            } catch (twitter4j.TwitterException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            }
            return (response != null) ? response.toString() : "";
        }

        protected void onPostExecute(String res) {
            if (res != null) {
                // progress.dismiss();
                Toast.makeText(TweetActivity.this, "Tweet successfully Posted", Toast.LENGTH_SHORT).show();

            } else {
                //progress.dismiss();
                Toast.makeText(getApplicationContext(), "Error while tweeting !", Toast.LENGTH_SHORT).show();

            }
        }
    }

    private class TokenGet extends AsyncTask<String, String, String> {
        @Override
        protected String doInBackground(String... args) {
            try {
                requestToken = twitter.getOAuthRequestToken();
                oauth_url = requestToken.getAuthorizationURL();
            } catch (TwitterException e) {
                e.printStackTrace();
            }
            return oauth_url;
        }

        @Override
        protected void onPostExecute(String oauth_url) {
            if(oauth_url != null){
                auth_dialog = new Dialog(TweetActivity.this);
                auth_dialog = new Dialog(TweetActivity.this);
                auth_dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                auth_dialog.setContentView(R.layout.oauth_webview);

                web = (WebView)auth_dialog.findViewById(R.id.webViewOAuth);
                web.getSettings().setJavaScriptEnabled(true);
                web.loadUrl(oauth_url);
                web.setWebViewClient(new WebViewClient() {
                    boolean authComplete = false;

                    @Override
                    public void onPageStarted(WebView view, String url, Bitmap favicon){
                        super.onPageStarted(view, url, favicon);                 }

                    @Override
                    public void onPageFinished(WebView view, String url) {
                        super.onPageFinished(view, url);
                        if (url.contains("oauth_verifier") && authComplete == false){
                            authComplete = true;
                            Uri uri = Uri.parse(url);
                            oauth_verifier = uri.getQueryParameter("oauth_verifier");
                            auth_dialog.dismiss();
                            new AccessTokenGet().execute();
                        }else if(url.contains("denied")){
                            auth_dialog.dismiss();
                            Toast.makeText(getApplicationContext(), "Sorry !, Permission Denied", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                Log.d("Debug", auth_dialog.toString());
                auth_dialog.show();
                auth_dialog.setCancelable(true);
            }else{
                Toast.makeText(getApplicationContext(), "Sorry !, Error or Invalid Credentials", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private class AccessTokenGet extends AsyncTask<String, String, Boolean> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Boolean doInBackground(String... args) {
            try {
                accessToken = twitter.getOAuthAccessToken(requestToken, oauth_verifier);
                SharedPreferences.Editor edit = pref.edit();
                edit.putString("ACCESS_TOKEN", accessToken.getToken());
                edit.putString("ACCESS_TOKEN_SECRET", accessToken.getTokenSecret());
                edit.putBoolean(PREFERENCE_TWITTER_LOGGED_IN, true);

                User user = twitter.showUser(accessToken.getUserId());
                profile_url = user.getOriginalProfileImageURL();
                edit.putString("NAME", user.getName());
                edit.putString("IMAGE_URL", user.getOriginalProfileImageURL());
                edit.commit();
            } catch (TwitterException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean response) {
            if(response){
                //progress.hide(); after login, tweet Post right away
                new PostTweet().execute();
            }
        }
    }

    private class fetchAddress extends AsyncTask<Location, Void, String> {
        private Location fLoc;
        List<Address> addresses = null;
        fetchAddress(Location loc){
            fLoc = loc;
        }
        protected String doInBackground(Location... params){
            Geocoder geocoder = new Geocoder(getBaseContext(), Locale.getDefault());
            Location loc = fLoc;
            try {
                addresses = geocoder.getFromLocation(loc.getLatitude(), loc.getLongitude(), 1); // get just a single address.
                Log.v("address string", addresses.get(0).getAddressLine(0) + addresses.get(0).getThoroughfare());
                Log.v("address attempt", addresses.get(0).toString());
            } catch (IOException ioException) {
                Log.v("ioException", "true");
                // Catch invalid latitude or longitude values.
                ioException.printStackTrace();
                return ("IO Exception OR Network Error ");
            }catch (IllegalArgumentException illegalArgumentException) {
                Log.v("illegalArgumentEx", "true");
                illegalArgumentException.printStackTrace();
                // Error message to post in the log
                return "invalid_lat_long_used";
            }catch(Exception e){
                e.printStackTrace();
                Log.v("Exception in Async", "true");
            }

            if (addresses != null || addresses.size() > 0) {
                Address address = addresses.get(0);
                // Format the first line of address (if available), city, and country name.
                addressText = String.format("%s, %s, %s ",
                        address.getMaxAddressLineIndex() > 0 ? address.getAddressLine(0) : "", address.getLocality(),
                        address.getPostalCode());
                Log.v("logging addressText", "in doInBackground");
                // Return the text
                return addressText;
            } else {
                return "no_address_found";
            }
        }

        @Override
        protected void onPostExecute(String result) {
//            mMap.addMarker(new MarkerOptions().position(cmu).title(addressText));
            Address currentAddress = addresses.get(0);
            AddressTweet tweetThis = AddressTweet.getInstance();
            tweetThis.setAddressToTweet(currentAddress.getAddressLine(0), "", currentAddress.getAddressLine(1), "", "");
            Log.v("marker title", "should be updated");
            Log.v("addressText:", addressText);
        }
    }
}
