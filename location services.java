package com.example.accidentdetectionapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class LocationService extends Service {
    private static final String TAG = "LocationService";
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "LocationServiceChannel";
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private static final String PICO_W_IP = "http://172.20.10.5:80/speed"; // Pico W URL

    @Override
    public void onCreate() {
        super.onCreate();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, getNotification());
        startLocationUpdates();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create()
                .setInterval(5000) // Update every 5 seconds
                .setFastestInterval(2000) // Fastest 2 seconds
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    Log.d(TAG, "LocationResult is null");
                    return;
                }
                Log.d(TAG, "Received location update in background");
                for (Location location : locationResult.getLocations()) {
                    sendSpeedLevel(location.getSpeed() * 3.6f); // Convert m/s to km/h
                }
            }
        };

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
        } else {
            Log.e(TAG, "Location permission not granted");
            stopSelf();
        }
    }

    private void sendSpeedLevel(float speedKmh) {
        // Check network availability
        android.net.ConnectivityManager cm = (android.net.ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        android.net.NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        if (!networkInfo.isConnected()) {
            Log.e(TAG, "No network available for sending speed level");
            return;
        } else if (networkInfo == null) {
            Log.e(TAG, "No network available for sending speed level");
            return;
        }

        int level;
        if (speedKmh >= 20) {
            level = 1;
        } else if (speedKmh > 20 && speedKmh <= 40) {
            level = 2;
        } else if (speedKmh >40 && speedKmh <= 130) {
            level = 3;
        } else {
            level = 0;
        }

        new Thread(() -> {
            try {
                URL url = new URL(PICO_W_IP);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(5000); // 5-second timeout
                conn.setReadTimeout(5000);

                String json = "{\"level\":" + level + "}";
                byte[] postData = json.getBytes(StandardCharsets.UTF_8);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(postData);
                }

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    Log.d(TAG, "Speed level sent: " + level);
                } else {
                    Log.e(TAG, "HTTP error: " + responseCode);
                }
                conn.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Failed to send speed level: " + e.getMessage());
            }
        }).start();
    }

    private Notification getNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Accident Detection")
                .setContentText("Monitoring location in background")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Location Service", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }
}