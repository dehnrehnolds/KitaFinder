package com.example.robert.kitafinder;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.robert.kitafinder.data.KitaContract;
import com.example.robert.kitafinder.data.Constants;
import com.example.robert.kitafinder.data.KitaProvider;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.opencsv.CSVReader;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Locale;

/*

-->>>Das muss in den onClickListener vom Such-Button
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
            SharedPreferences.Editor edit = prefs.edit();
            edit.putBoolean(getString(R.string.pref_previously_started_key), Boolean.TRUE);
            edit.commit();
 */

public class SearchActivity extends FragmentActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {

    // ------- Private Variables -------
    private GoogleApiClient mGoogleApiClient;
    private final String TAG = SearchActivity.class.getSimpleName();
    public Location mLastLocation;
    public Location mLastSearchAddress;
    public Location mEditTextAddress;
    private boolean mLocationExists = false;
    private EditText mAddressInput;

    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;
    private LocationRequest mLocationRequest = LocationRequest.create();
    private AddressResultReceiver mResultReceiver;
    private boolean googleApiClientConnected = false;

    // should the database be upddated?? (must be set false after first start automatically.
    // maybe re-activation through settings menue)
    private static boolean updateDB = true;
    public static int mInserted = 0;

    class AddressResultReceiver extends ResultReceiver {
        public AddressResultReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {

            // Display the address string
            // or an error message sent from the intent service.
            Log.d(TAG, "onReceiveResults");
            if (resultData != null) {
                mAddressInput.setHint(resultData
                        .getString(Constants.RESULT_DATA_KEY)
                        .replace(", Deutschland", "")
                        .replace(", ", "\n")
                );
            }

            // Show a toast message if an address was found.
            if (resultCode == Constants.SUCCESS_RESULT) {
                makeToast(getString(R.string.address_found));
            }

        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);


        /*
        Get shared preferences and lookup if app has been started before.
        If not: start SearchActivity()
        (Later we will decide this by weather the flag "search location" in shared preferences is set)

        TO BE RE-DESIGNED
         */
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
//        SharedPreferences.Editor editor = prefs.edit();
        boolean previouslyStarted = prefs.getBoolean(getString(R.string.pref_previously_started_key), false);
        //if it has not been started, start the search activity

        if (previouslyStarted) {
            Intent intent = new Intent(this, ResultActivity.class);
            this.startActivity(intent);
        } elseif (updateDB){
            new FetchDbFromCsv(this).execute();
            Log.d(TAG, "FetchDbFromCsv.execute()");
            updateDB = false;
        }

//        editor.putBoolean(getString(R.string.pref_address_changed_key), true);
//        editor.commit();

        // New Google Api-Client
        if (mGoogleApiClient == null) {
            Log.d(TAG, "GoogleApiClient == null");
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();

        }

//        //---- Test query -----
//
//
//        else Log.d(TAG, "queryC.moveToFirst() existiert nicht");
//        Log.d(TAG, "number of rows: " +queryC.getCount());
//        queryC.close();
//        //  ^
//        //  |

//        final Button refreshLoc = findViewById(R.id.refresh_loc_button);
//        refreshLoc.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View v) {
//                startLocationUpdates();
//            }
//        });

        final Button refreshAdd = findViewById(R.id.refresh_add_button);
        refreshAdd.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startLocationUpdates();
                getLastLocation();
            }
        });

        SharedPreferences filterPref = getSharedPreferences("filter", Context.MODE_PRIVATE);
        final SharedPreferences.Editor filterEditor = filterPref.edit();

        // --------------- searchRadius seekBar --------------
        SeekBar seekBarRadius = findViewById(R.id.search_radius_slider);
        seekBarRadius.setProgress(1);
        seekBarRadius.setSecondaryProgress(1);
        seekBarRadius.incrementProgressBy(1);
        seekBarRadius.setMax(10);
        final TextView seekBarRadiusValue = findViewById(R.id.search_radius_text);
        int searchRadius = filterPref.getInt(getString(R.string.search_radius), -1);

        if (searchRadius <= 0) {
            Log.d(TAG, "searchRadius <= 0");
            seekBarRadiusValue.setText(String.format("500m", searchRadius));
            seekBarRadius.setProgress(searchRadius);
        } else if (searchRadius <= 10) {
            Log.d(TAG, "searchRadius >0 und <10");
            seekBarRadiusValue.setText(String.format("%dkm", searchRadius));
            seekBarRadius.setProgress(searchRadius);
        } else
            Log.e(TAG, "searchRadius out of range!");

        seekBarRadius.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                if (progress == 0) {
                    seekBarRadiusValue.setText("500m");
                } else if (progress <= 10) {
                    seekBarRadiusValue.setText(String.format("%dkm", progress));
                } else Log.e(TAG, "search Radius out of range");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Log.d(TAG, "seekBarRadius.getProgress() = " + seekBar.getProgress());
                if (seekBar.getProgress() >= 0 && seekBar.getProgress() <= 10) {
                    filterEditor.putInt(getString(R.string.search_radius), seekBar.getProgress());
                    Log.d(TAG, String.format("Put searchRadius %d to sharedPrefs.", seekBar.getProgress()));
                } else {
                    filterEditor.putInt(getString(R.string.search_radius), -1);
                    Log.e(TAG, "progress out of Range");
                }
                filterEditor.apply();
            }
        });


//        mLatitudeLabel = getResources().getString(R.string.latitude_label);
//        mLongitudeLabel = getResources().getString(R.string.longitude_label);
//        mAccuracyLabel = getResources().getString(R.string.accuracy_label);
//        mLatitudeText = (TextView) findViewById((R.id.latitude_text));
//        mLongitudeText = (TextView) findViewById((R.id.longitude_text));
//        mAccuracyText = (TextView) findViewById(R.id.accuracy_text);
//        mAddressText = (TextView) findViewById(R.id.address_text);
        mAddressInput = (EditText) findViewById(R.id.address_input);

        mResultReceiver = new AddressResultReceiver(new Handler());

        final Button searchB = findViewById(R.id.search_button);
        searchB.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // creating intent for launching ResultActivity
                Intent ButtonIntent = new Intent(v.getContext(), ResultActivity.class);
                // opening SharedPrefs & editor
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                SharedPreferences.Editor editor = prefs.edit();

                // if no LastSearchAdress is stored in the activities intent, try to get the
                // address last saved to the DB
                if (mLastSearchAddress == null) {
                    Log.d(TAG, "mLastSearchAddress == null and replace by DB entry 'Zu Hause'");
                    mLastSearchAddress = new Location("Zu Hause");
                    String selection = KitaContract.LocationEntry.COLUMN_MAPST + " = ?";
                    String[] args = new String[]{"3"};

                    Cursor addressCursor = getContentResolver().query(
                            KitaContract.LocationEntry.CONTENT_URI,
                            null,
                            selection,
                            args,
                            null);

                    if (addressCursor != null && addressCursor.moveToFirst()) {
                        double addressLat = addressCursor.getDouble(Constants.COL_LAT);
                        double addressLong = addressCursor.getDouble(Constants.COL_LONG);
                        mLastSearchAddress.setLatitude(addressLat);
                        mLastSearchAddress.setLongitude(addressLong);
                        addressCursor.close();
                    } else {
                        Log.e(TAG, "No Last searchAddress found");
                        // set dummie address in case of first launch
                        mLastSearchAddress.setLatitude(33);
                        mLastSearchAddress.setLongitude(45);
                    }

                } else {
                    Log.d(TAG, "mLastSearchAddress != null");
                }

                if (!(mAddressInput.getText().toString().equals(""))) {
                    mEditTextAddress = new Location("Zu Hause");
                    Log.d(TAG, "Text found in EditText!");
                    Log.d(TAG, "Address found: " + mAddressInput.getText().toString());
                    try {
                        mEditTextAddress = convertAddress(mAddressInput.getText().toString());
                    } catch (Exception e) {
                        Log.e(TAG, e.getMessage());
                    }
                    if (mEditTextAddress.distanceTo(mLastSearchAddress) > 50) {
                        editor.putBoolean(getString(R.string.pref_address_changed_list), true);
                        editor.putBoolean(getString(R.string.pref_address_changed_map), true);
                        Log.d(TAG, "addressChanged changed to true");
                        ButtonIntent.putExtra("address", mEditTextAddress);
                        Log.d(TAG, "editTextAddress used for ButtonIntent of ResultActivity");
                        mLastSearchAddress = mEditTextAddress;
                    } else {
                        editor.putBoolean(getString(R.string.pref_address_changed_list), false);
                        editor.putBoolean(getString(R.string.pref_address_changed_map), false);
                        Log.d(TAG, "    addressChanged changed to false");
                        ButtonIntent.putExtra("address", mLastSearchAddress);
                        Log.d(TAG, "mLastSearchAddress used for ButtonIntent of ResultActivity");
                    }


                } else if (mLocationExists) {
                    Log.d(TAG, "Using device location or previously stored location");

                    // is the address last used different to the new Location (>50m)
                    if (mLastSearchAddress.distanceTo(mLastLocation) > 50) {
                        Log.d(TAG, "mLastSearchAddress:");
                        Log.d(TAG, "    Lat: " + mLastSearchAddress.getLatitude());
                        Log.d(TAG, "    Long: " + mLastSearchAddress.getLongitude());
                        Log.d(TAG, "mLastLocation:");
                        Log.d(TAG, "    Lat: " + mLastLocation.getLatitude());
                        Log.d(TAG, "    Long: " + mLastLocation.getLongitude());
                        editor.putBoolean(getString(R.string.pref_address_changed_list), true);
                        editor.putBoolean(getString(R.string.pref_address_changed_map), true);
                        Log.d(TAG, "    addressChanged changed to true");
                        //saving the address
                        ButtonIntent.putExtra("address", mLastLocation);
                        Log.d(TAG, "mLastLocation used for ButtonIntent of ResultActivity");
                        mLastSearchAddress = mLastLocation;

                        // if the address last used is the same (<50m) as the new address, use the old
                        // address and thatfore distances won't be newly calculated in ListFragment
                    } else {
                        editor.putBoolean(getString(R.string.pref_address_changed_list), false);
                        editor.putBoolean(getString(R.string.pref_address_changed_map), false);
                        Log.d(TAG, "    addressChanged changed to false");
                        ButtonIntent.putExtra("address", mLastSearchAddress);
                        Log.d(TAG, "mLastSearchAddress used for ButtonIntent of ResultActivity");
                    }
                } else {
                    Log.d(TAG, "    No location obtained, yet!");
                    return;
                }
                editor.apply();
                v.getContext().startActivity(ButtonIntent);

            }
        });

    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume()");
        if (this.getIntent().getParcelableExtra("address") != null) {
            Log.d(TAG, "get address from intent extra and store in mLastSearchAddress");
            Log.d(TAG, "    Lat: " + mLastSearchAddress.getLatitude());
            Log.d(TAG, "    Long: " + mLastSearchAddress.getLongitude());
            mLastSearchAddress = this.getIntent().getParcelableExtra("address");
        }
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (googleApiClientConnected) stopLocationUpdates();
        else Log.e(TAG, "Cannot stop location updates due to googleApiClient not connected, yet");
    }

    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    protected void onStop() {
        Log.d(TAG, "onStop()");
        mGoogleApiClient.disconnect();
        if (mLastSearchAddress == null && mEditTextAddress == null) {
            Log.d(TAG, "mLastSearchAddress AND mEditTextAddress are NULL");
            return;
        } else if (mLastSearchAddress == null) {
            Log.d(TAG, "mLastSearchAddress is NULL and set to mEditTextAddress");
            mLastSearchAddress = mEditTextAddress;
        } else Log.d(TAG, "mLastSearchAddress != null");
        // save address for next time SearchActivity gets called
        this.getIntent().putExtra("address", mLastSearchAddress);
        Log.d(TAG, "put mLastSearchAdress to intent extra & into DB");
        Log.d(TAG, "    Lat: " + mLastSearchAddress.getLatitude());
        Log.d(TAG, "    Long: " + mLastSearchAddress.getLongitude());

        // save address for next time, app gets launched
        String selection = KitaContract.LocationEntry.COLUMN_TYPE + " = ?";
        String[] args = new String[]{"3"};
        ContentValues contentV = new ContentValues();
        contentV.put(KitaContract.LocationEntry.COLUMN_LAT, mLastSearchAddress.getLatitude());
        contentV.put(KitaContract.LocationEntry.COLUMN_LONG, mLastSearchAddress.getLongitude());
        contentV.put(KitaContract.LocationEntry.COLUMN_DIST, "0");
        getContentResolver().update(KitaContract.LocationEntry.CONTENT_URI,
                contentV,
                selection,
                args
        );

        super.onStop();

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        googleApiClientConnected = true;
        if (!checkPermissions()) requestPermissions();
        else {
            getLastLocation();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    public void onConnectionFailed(@NonNull ConnectionResult result) {
        Log.e(TAG, "Connection to Google API Services failed:" + result.getErrorCode());

    }

    private boolean checkPermissions() {
        int permissionState = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    private void startLocationPermissionRequest() {
        ActivityCompat.requestPermissions(SearchActivity.this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                REQUEST_PERMISSIONS_REQUEST_CODE);
    }

    private void requestPermissions() {
        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION);

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.");

            showSnackbar(R.string.permission_rationale, android.R.string.ok,
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // Request permission
                            startLocationPermissionRequest();
                        }
                    });

        } else {
            Log.i(TAG, "Requesting permission");
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            startLocationPermissionRequest();
        }
    }


    public void onLocationChanged(Location location) {
        Log.d(TAG, "Location changed");
        makeToast("Got new Location");
        if (location != null) {
            Log.d(TAG, "Location not null");
            getLastLocation();
        }
    }

    public void getLastLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Permission denied");

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_PERMISSIONS_REQUEST_CODE);

        } else {
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            Log.d(TAG, "permission granted");
            if (mLastLocation != null) {
                Log.d(TAG, "Location exists");
                mLocationExists = true;
                // permission has been granted, continue as usual
                stopLocationUpdates();

//                mLatitudeText.setText(String.format(Locale.GERMAN, "%s: %2.2f°",
//                        mLatitudeLabel,
//                        mLastLocation.getLatitude()));
//                mLongitudeText.setText(String.format(Locale.GERMAN, "%s: %2.2f°",
//                        mLongitudeLabel,
//                        mLastLocation.getLongitude()));
//                mAccuracyText.setText(String.format(Locale.GERMAN, "%s: %1.0fm",
//                        mAccuracyLabel,
//                        mLastLocation.getAccuracy()));

                startIntentService();
            } else {
                Log.d(TAG, "Location doesn't exists");
                Log.w(TAG, "getLastLocation:exception");
                makeToast(getString(R.string.no_location_detected));
                startLocationUpdates();
            }
        }

    }

    protected void startLocationUpdates() {
        try {
            Log.d(TAG, "Location update started");
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, mLocationRequest, this);
        } catch (SecurityException s) {
            Log.e(TAG, s.getMessage());
        }
    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
    }

    protected void startIntentService() {
        Intent intent = new Intent(this, FetchAddressIntentService.class);
        intent.putExtra(Constants.RECEIVER, mResultReceiver);
        intent.putExtra(Constants.LOCATION_DATA_EXTRA, mLastLocation);
        startService(intent);
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Log.i(TAG, "onRequestPermissionResult");
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length <= 0) {
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                Log.i(TAG, "User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted.
                getLastLocation();
            } else {
                // Permission denied.

                // Notify the user via a SnackBar that they have rejected a core permission for the
                // app, which makes the Activity useless. In a real app, core permissions would
                // typically be best requested during a welcome-screen flow.

                // Additionally, it is important to remember that a permission might have been
                // rejected without asking the user for permission (device policy or "Never ask
                // again" prompts). Therefore, a user interface affordance is typically implemented
                // when permissions are denied. Otherwise, your app could appear unresponsive to
                // touches or interactions which have required permissions.
                showSnackbar(R.string.permission_denied_explanation, R.string.settings,
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                // Build intent that displays the App settings screen.
                                Intent intent = new Intent();
                                intent.setAction(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package",
                                        BuildConfig.APPLICATION_ID, null);
                                intent.setData(uri);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }
                        });
            }

        }
    }

    private void makeToast(final String text) {
        Toast addressFoundToast = Toast.makeText(this,
                text,
                Toast.LENGTH_SHORT
        );
        addressFoundToast.show();
    }

    /**
     * Shows a {@link Snackbar}.
     *
     * @param mainTextStringId The id for the string resource for the Snackbar text.
     * @param actionStringId   The text of the action item.
     * @param listener         The listener associated with the Snackbar action.
     */
    private void showSnackbar(final int mainTextStringId, final int actionStringId,
                              View.OnClickListener listener) {
        Snackbar.make(findViewById(android.R.id.content),
                getString(mainTextStringId),
                Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(actionStringId), listener).show();
    }

    public Location convertAddress(String address) {
        Geocoder geoCoder = new Geocoder(this);
        Location locationFromAddress = new Location("Zu Hause");
        if (address != null && !address.isEmpty()) {
            try {
                List<Address> addressList = geoCoder.getFromLocationName(address, 1);
                if (addressList != null && addressList.size() > 0) {
                    double lat = addressList.get(0).getLatitude();
                    double lng = addressList.get(0).getLongitude();
                    Log.d(TAG, "lat: " + lat + "  long: " + lng);
                    locationFromAddress.setLatitude(addressList.get(0).getLatitude());
                    locationFromAddress.setLongitude(addressList.get(0).getLongitude());

                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, e.getMessage());
            }
        }
        return locationFromAddress;
    }
}