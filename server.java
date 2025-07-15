package com.example.accidentdetectionapp;

import android.content.Context;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class HttpServer {

    private static final String TAG = "HttpServer";
    private static final int PORT = 8080;
    private final Context context;

    private OnCrashListener crashListener;

    public interface OnCrashListener {
        void onCrash(double pitch, double roll, long timestamp);
    }

    public void setOnCrashListener(OnCrashListener listener) {
        this.crashListener = listener;
    }

    public HttpServer(Context context) {
        this.context = context;
    }

    public void startServer() {
        try {
            ServerSocket serverSocket = new ServerSocket(PORT);
            Log.i(TAG, "Server started on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (IOException e) {
            Log.e(TAG, "Server error: " + e.getMessage());
        }
    }

    private void handleClient(Socket clientSocket) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            OutputStream out = clientSocket.getOutputStream();

            // Read headers
            StringBuilder request = new StringBuilder();
            String line;
            int contentLength = 0;
            while ((line = in.readLine()) != null && !line.isEmpty()) {
                request.append(line).append("\n");
                if (line.toLowerCase().startsWith("content-length:")) {
                    contentLength = Integer.parseInt(line.split(":")[1].trim());
                }
            }

            // Read body
            char[] bodyChars = new char[contentLength];
            in.read(bodyChars, 0, contentLength);
            String body = new String(bodyChars);

            if (request.toString().contains("POST /accident")) {
                try {
                    JSONObject json = new JSONObject(body);
                    double pitch = json.getDouble("pitch");
                    double roll = json.getDouble("roll");
                    long timestamp = json.getLong("timestamp");

                    Log.i(TAG, "Received crash alert: Pitch=" + pitch + ", Roll=" + roll + ", Time=" + timestamp);

                    if (crashListener != null) {
                        crashListener.onCrash(pitch, roll, timestamp);
                    }

                    String response = "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\n\r\nOK";
                    out.write(response.getBytes());
                } catch (Exception e) {
                    String response = "HTTP/1.1 400 Bad Request\r\nContent-Type: text/plain\r\n\r\nInvalid JSON";
                    out.write(response.getBytes());
                }
            } else {
                String response = "HTTP/1.1 404 Not Found\r\nContent-Type: text/plain\r\n\r\nNot Found";
                out.write(response.getBytes());
            }

            out.flush();
            clientSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "Client error: " + e.getMessage());
        }
    }
}