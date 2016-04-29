package com.lynxiayel.sensortimechecker;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;

public class SensorService extends Service implements SensorEventListener {
    private SensorManager sm;
    private final SensorServiceBinder mBinder = new SensorServiceBinder();
    public Long timestamp;
    private Handler handler;

    public SensorService() {
    }

    public class SensorServiceBinder extends Binder {
        SensorService getService() {
            return SensorService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        sm = (SensorManager) getSystemService(SENSOR_SERVICE);
        timestamp = (long) -1;
        Sensor acc_sensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (acc_sensor != null) {
            sm.registerListener(this, acc_sensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
        return mBinder;
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        timestamp = event.timestamp;
        Message msg = new Message();
        msg.obj = timestamp;
        if (handler != null) {
            handler.sendMessage(msg);
//            sm.unregisterListener(this);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public void setHandler(Handler handler) {
        this.handler = handler;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        sm.unregisterListener(this);
        return super.onUnbind(intent);
    }
}
