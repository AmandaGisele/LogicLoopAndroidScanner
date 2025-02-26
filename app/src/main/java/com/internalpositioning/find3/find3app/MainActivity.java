package com.internalpositioning.find3.find3app;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private final String TAG = "MainActivity";
    private WifiManager wifiManager;
    private WebSocketClient mWebSocketClient;
    private Timer timer;
    private RemindTask oneSecondTimer;
    private PendingIntent recurringLl24;
    private AlarmManager alarms;
    private String[] autocompleteLocations = new String[]{"bedroom", "living room", "kitchen", "bathroom", "office"};

    @Override
    protected void onDestroy() {
        Log.d(TAG, "MainActivity onDestroy()");
        if (alarms != null) alarms.cancel(recurringLl24);
        if (timer != null) timer.cancel();
        if (mWebSocketClient != null) {
            mWebSocketClient.close();
        }
        android.app.NotificationManager mNotificationManager = (android.app.NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(0);
        Intent scanService = new Intent(this, ScanService.class);
        stopService(scanService);
        super.onDestroy();
    }

    class RemindTask extends TimerTask {
        private Integer counter = 0;

        public void resetCounter() {
            counter = 0;
        }

        public void run() {
            runOnUiThread(() -> {
                counter++;
                if (mWebSocketClient != null) {
                    if (mWebSocketClient.isClosed()) {
                        connectWebSocket();
                    }
                }
                TextView rssi_msg = findViewById(R.id.textOutput);
                String currentText = rssi_msg.getText().toString();
                if (currentText.contains("ago: ")) {
                    String[] currentTexts = currentText.split("ago: ");
                    currentText = currentTexts[1];
                }
                rssi_msg.setText(counter + " seconds ago: " + currentText);
            });
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize WifiManager
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        // Check permissions
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WAKE_LOCK, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.INTERNET, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.CHANGE_WIFI_STATE, Manifest.permission.ACCESS_WIFI_STATE}, 1);
        }

        TextView rssi_msg = findViewById(R.id.textOutput);
        rssi_msg.setText("not running");

        // Check to see if there are preferences
        SharedPreferences sharedPref = MainActivity.this.getPreferences(Context.MODE_PRIVATE);
        EditText familyNameEdit = findViewById(R.id.familyName);
        familyNameEdit.setText(sharedPref.getString("familyName", ""));
        EditText deviceNameEdit = findViewById(R.id.deviceName);
        deviceNameEdit.setText(sharedPref.getString("deviceName", ""));
        EditText serverAddressEdit = findViewById(R.id.serverAddress);
        serverAddressEdit.setText(sharedPref.getString("serverAddress", ((EditText) findViewById(R.id.serverAddress)).getText().toString()));
        CheckBox checkBoxAllowGPS = findViewById(R.id.allowGPS);
        checkBoxAllowGPS.setChecked(sharedPref.getBoolean("allowGPS", false));

        AutoCompleteTextView textView = findViewById(R.id.locationName);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, autocompleteLocations);
        textView.setAdapter(adapter);

        ToggleButton toggleButtonTracking = findViewById(R.id.toggleScanType);
        toggleButtonTracking.setOnCheckedChangeListener((buttonView, isChecked) -> {
            TextView rssi_msg1 = findViewById(R.id.textOutput);
            rssi_msg1.setText("not running");
            Log.d(TAG, "toggle set to false");
            if (alarms != null) alarms.cancel(recurringLl24);
            android.app.NotificationManager mNotificationManager = (android.app.NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.cancel(0);
            if (timer != null) timer.cancel();

            CompoundButton scanButton = findViewById(R.id.toggleButton);
            scanButton.setChecked(false);
        });

        ToggleButton toggleButton = findViewById(R.id.toggleButton);
        toggleButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                TextView rssi_msg12 = findViewById(R.id.textOutput);
                String familyName = ((EditText) findViewById(R.id.familyName)).getText().toString().toLowerCase();
                if (familyName.equals("")) {
                    rssi_msg12.setText("family name cannot be empty");
                    buttonView.toggle();
                    return;
                }

                String serverAddress = ((EditText) findViewById(R.id.serverAddress)).getText().toString().toLowerCase();
                if (serverAddress.equals("")) {
                    rssi_msg12.setText("server address cannot be empty");
                    buttonView.toggle();
                    return;
                }
                if (!serverAddress.contains("http")) {
                    rssi_msg12.setText("must include http or https in server name");
                    buttonView.toggle();
                    return;
                }
                String deviceName = ((EditText) findViewById(R.id.deviceName)).getText().toString().toLowerCase();
                if (deviceName.equals("")) {
                    rssi_msg12.setText("device name cannot be empty");
                    buttonView.toggle();
                    return;
                }
                boolean allowGPS = ((CheckBox) findViewById(R.id.allowGPS)).isChecked();
                Log.d(TAG, "allowGPS is checked: " + allowGPS);
                String locationName = ((EditText) findViewById(R.id.locationName)).getText().toString().toLowerCase();

                CompoundButton trackingButton = findViewById(R.id.toggleScanType);
                if (!trackingButton.isChecked()) {
                    locationName = "";
                } else {
                    if (locationName.equals("")) {
                        rssi_msg12.setText("location name cannot be empty when learning");
                        buttonView.toggle();
                        return;
                    }
                }

                SharedPreferences sharedPref1 = MainActivity.this.getPreferences(Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref1.edit();
                editor.putString("familyName", familyName);
                editor.putString("deviceName", deviceName);
                editor.putString("serverAddress", serverAddress);
                editor.putString("locationName", locationName);
                editor.putBoolean("allowGPS", allowGPS);
                editor.commit();

                rssi_msg12.setText("running");
                // 24/7 alarm
                ll24 = new Intent(MainActivity.this, AlarmReceiverLife.class);
                Log.d(TAG, "setting familyName to [" + familyName + "]");
                ll24.putExtra("familyName", familyName);
                ll24.putExtra("deviceName", deviceName);
                ll24.putExtra("serverAddress", serverAddress);
                ll24.putExtra("locationName", locationName);
                ll24.putExtra("allowGPS", allowGPS);
                recurringLl24 = PendingIntent.getBroadcast(MainActivity.this, 0, ll24, PendingIntent.FLAG_CANCEL_CURRENT);
                alarms = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                alarms.setRepeating(AlarmManager.RTC_WAKEUP, SystemClock.currentThreadTimeMillis(), 60000, recurringLl24);
                timer = new Timer();
                oneSecondTimer = new RemindTask();
                timer.scheduleAtFixedRate(oneSecondTimer, 1000, 1000);
                connectWebSocket();

                String scanningMessage = "Scanning for " + familyName + "/" + deviceName;
                if (!locationName.equals("")) {
                    scanningMessage += " at " + locationName;
                }
                NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(MainActivity.this)
                        .setSmallIcon(R.drawable.ic_stat_name)
                        .setContentTitle(scanningMessage)
                        .setContentIntent(recurringLl24);
                Intent resultIntent = new Intent(MainActivity.this, MainActivity.class);
                resultIntent.setAction("android.intent.action.MAIN");
                resultIntent.addCategory("android.intent.category.LAUNCHER");
                PendingIntent resultPendingIntent = PendingIntent.getActivity(MainActivity.this, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                notificationBuilder.setContentIntent(resultPendingIntent);

                android.app.NotificationManager notificationManager =
                        (android.app.NotificationManager) MainActivity.this.getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.notify(0, notificationBuilder.build());

                final TextView myClickableUrl = findViewById(R.id.textInstructions);
                myClickableUrl.setText("See your results in realtime: " + serverAddress + "/view/location/" + familyName + "/" + deviceName);
                Linkify.addLinks(myClickableUrl, Linkify.WEB_URLS);
            } else {
                TextView rssi_msg13 = findViewById(R.id.textOutput);
                rssi_msg13.setText("not running");
                Log.d(TAG, "toggle set to false");
                alarms.cancel(recurringLl24);
                android.app.NotificationManager mNotificationManager = (android.app.NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                mNotificationManager.cancel(0);
                timer.cancel();
            }
        });
    }

    private void connectWebSocket() {
        URI uri;
        try {
            String serverAddress = ((EditText) findViewById(R.id.serverAddress)).getText().toString();
            String familyName = ((EditText) findViewById(R.id.familyName)).getText().toString();
            String deviceName = ((EditText) findViewById(R.id.deviceName)).getText().toString();
            serverAddress = serverAddress.replace("http", "ws");
            uri = new URI(serverAddress + "/ws?family=" + familyName + "&device=" + deviceName);
            Log.d("Websocket", "connect to websocket at " + uri.toString());
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }

        mWebSocketClient = new WebSocketClient(uri) {
            @Override
            public void onOpen(ServerHandshake serverHandshake) {
                Log.i("Websocket", "Opened");
                mWebSocketClient.send("Hello");
            }

            @Override
            public void onMessage(String s) {
                final String message = s;
                runOnUiThread(() -> {
                    Log.d("Websocket", "message: " + message);
                    JSONObject json;
                    JSONObject fingerprint;
                    JSONObject sensors;
                    JSONObject bluetooth;
                    JSONObject wifi;
                    String deviceName = "";
                    String locationName = "";
                    String familyName = "";
                    try {
                        json = new JSONObject(message);
                    } catch (Exception e) {
                        Log.d("Websocket", "json error: " + e.toString());
                        return;
                    }
                    try {
                        fingerprint = new JSONObject(json.get("sensors").toString());
                        Log.d("Websocket", "fingerprint: " + fingerprint);
                    } catch (Exception e) {
                        Log.d("Websocket", "json error: " + e.toString());
                        return;
                    }
                    try {
                        sensors = new JSONObject(fingerprint.get("s").toString());
                        deviceName = fingerprint.get("d").toString();
                        familyName = fingerprint.get("f").toString();
                        locationName = fingerprint.get("l").toString();
                        Log.d("Websocket", "sensors: " + sensors);
                    } catch (Exception e) {
                        Log.d("Websocket", "json error: " + e.toString());
                        return;
                    }
                    try {
                        wifi = sensors.getJSONObject("wifi");
                        Log.d("Websocket", "wifi: " + wifi);
                    } catch (Exception e) {
                        Log.d("Websocket", "json error: " + e.toString());
                        return;
                    }
                    try {
                        bluetooth = sensors.getJSONObject("bluetooth");
                        Log.d("Websocket", "bluetooth: " + bluetooth);
                    } catch (Exception e) {
                        Log.d("Websocket", "json error: " + e.toString());
                        return;
                    }
                    Log.d("Websocket", bluetooth.toString());
                    Integer bluetoothPoints = bluetooth.length();
                    Integer wifiPoints = wifi.length();
                    Long secondsAgo;
                    try {
                        secondsAgo = fingerprint.getLong("t");
                    } catch (Exception e) {
                        Log.w("Websocket", e);
                        return;
                    }

                    if ((System.currentTimeMillis() - secondsAgo) / 1000 > 3) {
                        return;
                    }
                    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd HH:mm:ss");
                    Date resultdate = new Date(secondsAgo);
                    String message1 = "1 second ago: added " + bluetoothPoints.toString() + " bluetooth and " + wifiPoints.toString() + " wifi points for " + familyName + "/" + deviceName;
                    oneSecondTimer.resetCounter();
                    if (!locationName.equals("")) {
                        message1 += " at " + locationName;
                    }
                    TextView rssi_msg = findViewById(R.id.textOutput);
                    Log.d("Websocket", message1);
                    rssi_msg.setText(message1);
                });
            }

            @Override
            public void onClose(int i, String s, boolean b) {
                Log.i("Websocket", "Closed " + s);
            }

            @Override
            public void onError(Exception e) {
                Log.i("Websocket", "Error " + e.getMessage());
                runOnUiThread(() -> {
                    TextView rssi_msg = findViewById(R.id.textOutput);
                    rssi_msg.setText("cannot connect to server, fingerprints will not be uploaded");
                });
            }
        };
        mWebSocketClient.connect();
    }

    private void startWifiScan() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }
        wifiManager.startScan();
    }

    private final BroadcastReceiver wifiScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false);
            if (success) {
                handleScanResults();
            } else {
                // scan failure handling
                Log.e(TAG, "Wi-Fi scan failed");
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        registerReceiver(wifiScanReceiver, filter);
        startWifiScan();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(wifiScanReceiver);
    }

    private void handleScanResults() {
        List<ScanResult> results = wifiManager.getScanResults();
        JSONArray wifiArray = new JSONArray();
        for (ScanResult result : results) {
            JSONObject wifiObject = new JSONObject();
            try {
                wifiObject.put("BSSID", result.BSSID);
                wifiObject.put("SSID", result.SSID);
                wifiObject.put("level", result.level);
                wifiArray.put(wifiObject);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        sendToFind3Server(wifiArray);
    }

    private void sendToFind3Server(JSONArray wifiArray) {
        if (mWebSocketClient != null && mWebSocketClient.isOpen()) {
            JSONObject data = new JSONObject();
            try {
                data.put("wifi", wifiArray);
                mWebSocketClient.send(data.toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            Log.e(TAG, "WebSocket is not connected");
        }
    }
}
