package com.example.accidentdetectionapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.github.anastr.speedviewlib.SpeedView;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int REQUEST_PERMISSIONS = 1;
    private static final int REQUEST_BACKGROUND_LOCATION = 2;
    private static final int REQUEST_OTHER_PERMISSIONS = 3;
    private static final float SPEED_LIMIT = 60.0f;
    private static final String PHONE_NUMBER_1 = "+916360286938";
    private static final String PHONE_NUMBER_2 = "+919900388573";

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private TextView statusText, currentSpeedText, speedLimitText;
    private SpeedView speedometer;
    private Marker userMarker;

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText = findViewById(R.id.status_text);
        currentSpeedText = findViewById(R.id.current_speed_text);
        speedLimitText = findViewById(R.id.speed_limit_text);
        speedometer = findViewById(R.id.speedometer);

        speedometer.setUnit("km/h");
        speedometer.setMaxSpeed(130);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        requestPermissions();

        // Start HTTP Server for crash alerts
        startHttpServer();
    }

    private void requestPermissions() {
        String[] locationPermissions = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        };
        String[] otherPermissions = {
                Manifest.permission.SEND_SMS,
                Manifest.permission.CALL_PHONE,
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_NETWORK_STATE
        };
        String[] backgroundLocationPermission = {Manifest.permission.ACCESS_BACKGROUND_LOCATION};

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, locationPermissions, REQUEST_PERMISSIONS);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, backgroundLocationPermission, REQUEST_BACKGROUND_LOCATION);
        } else if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, otherPermissions, REQUEST_OTHER_PERMISSIONS);
        } else {
            startLocationService();
            requestBatteryOptimizationExemption();
        }
    }

    private void startLocationService() {
        Intent serviceIntent = new Intent(this, LocationService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    private void requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(android.net.Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        }
    }

    private void startHttpServer() {
        new Thread(() -> {
            HttpServer server = new HttpServer(MainActivity.this);
            server.setOnCrashListener(MainActivity.this::handleCrashDetected);
            server.startServer();
        }).start();
    }

    public void handleCrashDetected(double pitch, double roll, long timestamp) {
        Log.i(TAG, "Accident Detected! Pitch: " + pitch + ", Roll: " + roll + ", Timestamp: " + timestamp);

        runOnUiThread(() -> {
           // Toast.makeText(this, "🚨 Accident detected! 🚨", Toast.LENGTH_LONG).show();
            statusText.setText("Accident Detected!\nPitch: " + pitch + "\nRoll: " + roll);
        });

        // Get current location and send SMS/calls
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
               // String message
                String message = "Bro I have met With an accident  at: " + "Latitude " + location.getLatitude() + ", Longitude " + location.getLongitude() + " Please reach here immediately I Need your help!!!";
                        /*;
                if (location != null) {
                    message = "⚠ Accident Detected! Pitch: " + pitch + ", Roll: " + roll +
                            "\nLocation: Latitude: " + location.getLatitude() + ", Longitude: " + location.getLongitude() +
                            "\nCheck on Arjun immediately.\nClick here to view location: https://www.google.com/maps/search/?api=1&query=" +
                            location.getLatitude() + "," + location.getLongitude();
                } else {
                    message = "⚠ Accident Detected! Pitch: " + pitch + ", Roll: " + roll +
                            "\nLocation unavailable.\nCheck on Arjun immediately.";
                }*/

                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
                    sendSMS(PHONE_NUMBER_1, message);
                    sendSMS(PHONE_NUMBER_2, message);
                } else {
                    Log.e(TAG, "SMS permission not granted");
                }
//for  call

             /*   private void makeCall Intent callIntent;
                (PHONE_NUMBER_1) {
                    Intent callIntent = new Intent(Intent.ACTION_CALL);
                    callIntent.setData(android.net.Uri.parse("tel:" + PHONE_NUMBER_1));
                    callIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    try {
                        startActivity(callIntent);
                        Log.i(TAG, "Call initiated to " + PHONE_NUMBER_1);
                    } catch (SecurityException e) {
                        Log.e(TAG, "Call failed: " + e.getMessage());
                    }
                }
*/


                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                    makeCall();
                } else {
                    Log.e(TAG, "Call permission not granted");
                }
            });
        } else {
            Log.e(TAG, "Location permission not granted for accident notification");
        }
    }

    private void sendSMS(String phoneNumber, String message) {
        try {
            android.telephony.SmsManager smsManager = android.telephony.SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNumber, null, message, null, null);
            Log.i(TAG, "SMS sent to " + phoneNumber);
        } catch (Exception e) {
            Log.e(TAG, "SMS failed: " + e.getMessage());
        }
    }

    private void makeCall() {
        Intent callIntent = new Intent(Intent.ACTION_CALL);
        callIntent.setData(Uri.parse("tel:" + MainActivity.PHONE_NUMBER_1));
        callIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(callIntent);
            Log.i(TAG, "Call initiated to " + MainActivity.PHONE_NUMBER_1);
        } catch (SecurityException e) {
            Log.e(TAG, "Call failed: " + e.getMessage());
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            updateMapLocation();
        }
    }

    private void updateMapLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            statusText.setText("Location permission denied");
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                if (userMarker == null) {
                    userMarker = mMap.addMarker(new MarkerOptions().position(latLng).title("You"));
                } else {
                    userMarker.setPosition(latLng);
                }
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));

                float speedKmh = location.getSpeed() * 3.6f;
                speedometer.speedTo(speedKmh);
                currentSpeedText.setText(String.format("Speed: %.2f km/h", speedKmh));
                speedLimitText.setText(String.format("Limit: %.0f km/h", SPEED_LIMIT));
                statusText.setText("Lat: " + location.getLatitude() + ", Lon: " + location.getLongitude());
            } else {
                statusText.setText("Unable to get location");
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, REQUEST_BACKGROUND_LOCATION);
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS, Manifest.permission.CALL_PHONE, Manifest.permission.INTERNET, Manifest.permission.ACCESS_NETWORK_STATE}, REQUEST_OTHER_PERMISSIONS);
            }
        } else if (requestCode == REQUEST_BACKGROUND_LOCATION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS, Manifest.permission.CALL_PHONE, Manifest.permission.INTERNET, Manifest.permission.ACCESS_NETWORK_STATE}, REQUEST_OTHER_PERMISSIONS);
        } else if (requestCode == REQUEST_OTHER_PERMISSIONS && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startLocationService();
            requestBatteryOptimizationExemption();
        } else {
            statusText.setText("Required permissions denied. Please enable in settings.");
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(android.net.Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        }
    }
}