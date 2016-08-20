package com.lynxiayel.sensortimechecker;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;
import android.widget.Toast;

import com.lynxiayel.sensortimechecker.SensorService.SensorServiceBinder;

public class MainActivity extends AppCompatActivity {

    private final int REQUEST_SENSOR_PERMISSION = 1;
    private final int ALLOWED_ERROR=5000; //in milliseconds
    private final String NANO_SINCE_BOOT_INC_SLEEP = "Nanoseconds since last boot (including  sleep)";
    private final String MICRO_SINCE_BOOT_INC_SLEEP = "Microseconds since last boot (including  sleep)";
    private final String MILLI_SINCE_BOOT_INC_SLEEP = "Milliseconds since last boot (including  sleep)";
    private final String NANO_SINCE_BOOT_EXL_SLEEP = "Nanoseconds since last boot (excluding  sleep)";
    private final String MICRO_SINCE_BOOT_EXL_SLEEP = "Microseconds since last boot (excluding  sleep)";
    private final String MILLI_SINCE_BOOT_EXL_SLEEP = "Milliseconds since last boot (excluding  sleep)";
    private final String NANO_UTC = "Nanoseconds of the UTC time";
    private final String MICRO_UTC = "Microseconds of the UTC time";
    private final String MILLI_UTC = "Milliseconds of the UTC time";
    private final String NANO_EPOCH = "Nanoseconds of the EPOCH time";
    private final String MICRO_EPOCH = "Microseconds of the EPOCH time";
    private final String MILLI_EPOCH = "Milliseconds of the EPOCH time";
    private final String NA = "N/A";
    private TextView timestamp_value;
    private TextView timestamp_meaning;
    private SensorService sensorService;
    private ServiceConnection mCon;
    private boolean isSensorServiceBound;
    private Long timestamp;
    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        timestamp_value = (TextView) findViewById(R.id.timestamp_value);
        timestamp_meaning = (TextView) findViewById(R.id.timestamp_meaning);
        sensorService = new SensorService();

        mCon = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                SensorServiceBinder binder = (SensorServiceBinder) service;
                sensorService = binder.getService();
                sensorService.setHandler(mHandler);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
            }
        };
        mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                timestamp = (Long) msg.obj;
                timestamp_value.setText(timestamp.toString());
                check();
            }
        };

        handlePermission();
//        init(true);
    }

    private void init(boolean permissionGranted) {
        if (!permissionGranted) {
            timestamp_value.setText(NA);
            timestamp = (long) -1;
            return;
        }
        myBindService();
    }

    private void check() {
        Long sinceLastBootIncludingSleep = android.os.SystemClock.elapsedRealtime();
        Long sinceLastBootExcludingSleep = android.os.SystemClock.uptimeMillis();
        Long curTimeEpochMilli = System.currentTimeMillis();
//        Log.d("curTime", curTimeEpochMilli.toString());
//        Log.d("lastbootIncSlp", sinceLastBootIncludingSleep.toString());
//        Log.d("lastbootExlSlp", sinceLastBootExcludingSleep.toString());
//        Log.d("sensorevent", "sensorevent " + timestamp.toString());
        if (timestamp != null && timestamp != -1) {
            if (Math.abs(timestamp / 1e6 - curTimeEpochMilli) < ALLOWED_ERROR) {
                timestamp_meaning.setText(NANO_EPOCH);
            } else if (Math.abs(timestamp / 1e3 - curTimeEpochMilli) < ALLOWED_ERROR) {
                timestamp_meaning.setText(MICRO_EPOCH);
            } else if (Math.abs(timestamp - curTimeEpochMilli) < ALLOWED_ERROR) {
                timestamp_meaning.setText(MILLI_EPOCH);
            } else if (Math.abs(timestamp / 1e6 - sinceLastBootIncludingSleep) < ALLOWED_ERROR) {
                timestamp_meaning.setText(NANO_SINCE_BOOT_INC_SLEEP);
            } else if (Math.abs(timestamp / 1e3 - sinceLastBootIncludingSleep) < ALLOWED_ERROR) {
                timestamp_meaning.setText(MICRO_SINCE_BOOT_INC_SLEEP);
            } else if (Math.abs(timestamp - sinceLastBootIncludingSleep) < ALLOWED_ERROR) {
                timestamp_meaning.setText(MILLI_SINCE_BOOT_INC_SLEEP);
            } else if (Math.abs(timestamp / 1e6 - sinceLastBootExcludingSleep) < ALLOWED_ERROR) {
                timestamp_meaning.setText(NANO_SINCE_BOOT_EXL_SLEEP);
            } else if (Math.abs(timestamp / 1e3 - sinceLastBootExcludingSleep) < ALLOWED_ERROR) {
                timestamp_meaning.setText(MICRO_SINCE_BOOT_EXL_SLEEP);
            } else if (Math.abs(timestamp - sinceLastBootExcludingSleep) < ALLOWED_ERROR) {
                timestamp_meaning.setText(MILLI_SINCE_BOOT_EXL_SLEEP);
            }

        }
    }

    private void handlePermission() {
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.BODY_SENSORS)
                != PackageManager.PERMISSION_GRANTED) {

            // No explanation needed, we can request the permission.
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.BODY_SENSORS},
                    REQUEST_SENSOR_PERMISSION);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_SENSOR_PERMISSION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    init(true);
                } else {
                    Toast.makeText(this, "Sorry we cannot look into the SensorEvent.timestamp if you don't grant the permission.", Toast.LENGTH_LONG).show();
                    init(false);
                }
            }
        }
    }

    @Override
    protected void onPause() {
        if (isSensorServiceBound && mCon!=null)
        {
            myUnbindService();
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isSensorServiceBound) {
            myBindService();
        }
    }

    private void myBindService() {
        Intent intent = new Intent(this, SensorService.class);
        bindService(intent, mCon, Context.BIND_AUTO_CREATE);
        isSensorServiceBound = true;
    }
    private void myUnbindService() {
        unbindService(mCon);
        isSensorServiceBound = false;
    }
}
