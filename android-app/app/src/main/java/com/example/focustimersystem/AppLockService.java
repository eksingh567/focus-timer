package com.example.focustimersystem;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AppLockService extends Service {
    private static final String TAG = "AppLockService";
    private static final String CHANNEL_ID = "focus_timer_lock_channel";
    private String serverStatusUrl = "http://10.0.2.2:8000/status";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private boolean running = false;

    private void startMonitoring() {
        running = true;
        executor.execute(() -> {
            while (running) {
                try {
                    String savedIp = getSharedPreferences("FocusPrefs", MODE_PRIVATE).getString("hub_ip", "10.0.2.2");
                    serverStatusUrl = "http://" + savedIp + ":8000/status";
                    
                    JSONObject status = fetchStatus(serverStatusUrl);
                    String mode = status.optString("mode", "unlock");
                    Set<String> blockedApps = jsonArrayToSet(status.optJSONArray("blocked_apps"));

                    Log.d(TAG, "Mode: " + mode + ", Blocked: " + blockedApps);

                    if ("lock".equalsIgnoreCase(mode)) {
                        String foregroundApp = getForegroundAppPackage();
                        Log.d(TAG, "Foreground app: " + foregroundApp);
                        if (foregroundApp != null && blockedApps.contains(foregroundApp)) {
                            if (!foregroundApp.equals(getPackageName())) {
                                Intent intent = new Intent(AppLockService.this, LockScreenActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                intent.putExtra("blocked_app", foregroundApp);
                                startActivity(intent);
                            }
                        }
                    }
                    Thread.sleep(1500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    Log.e(TAG, "Error in monitor loop", e);
                }
            }
        });
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("FocusTimer app lock running")
                .setContentText("Blocking selected apps while laptop timer is active")
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(101, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            startForeground(101, notification);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!running) {
            startMonitoring();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        running = false;
        executor.shutdownNow();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Focus Timer Lock",
                    NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null)
                manager.createNotificationChannel(channel);
        }
    }

    private JSONObject fetchStatus(String url) {
        HttpURLConnection connection = null;
        BufferedReader reader = null;

        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(2000);
            connection.setReadTimeout(2000);
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line = reader.readLine();
                return new JSONObject(line != null ? line.trim() : "{}");
            }
        } catch (Exception ignored) {
        } finally {
            try {
                if (reader != null)
                    reader.close();
            } catch (Exception ignored) {
            }
            if (connection != null)
                connection.disconnect();
        }
        return new JSONObject();
    }

    private Set<String> jsonArrayToSet(JSONArray arr) {
        Set<String> set = new HashSet<>();
        if (arr == null)
            return set;
        for (int i = 0; i < arr.length(); i++) {
            set.add(arr.optString(i));
        }
        return set;
    }

    private String getForegroundAppPackage() {
        UsageStatsManager usageStatsManager = (UsageStatsManager) getSystemService(USAGE_STATS_SERVICE);
        if (usageStatsManager == null)
            return null;

        long endTime = System.currentTimeMillis();
        long beginTime = endTime - 3000;
        UsageEvents events = usageStatsManager.queryEvents(beginTime, endTime);
        UsageEvents.Event event = new UsageEvents.Event();
        String lastPackage = null;

        while (events.hasNextEvent()) {
            events.getNextEvent(event);
            if (event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                lastPackage = event.getPackageName();
            }
        }
        return lastPackage;
    }
}
