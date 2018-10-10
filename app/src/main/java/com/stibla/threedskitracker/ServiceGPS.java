package com.stibla.threedskitracker;

import java.util.Locale;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;

public class ServiceGPS extends Service implements LocationListener {

    private LocationManager locationManager;
    private DatabaseAdapter dbHelperTrackDb;
    private int count = 0;
    private long prevTime = 0;
    private double prevAlt = 0.0d;
    private double prevLat = 0.0d;
    private double prevLon = 0.0d;
    private double maxAltitude = 0.0d;
    private double minAltitude = 0.0d;
    private double verticalDescent = 0.0d;
    private double verticalAscent = 0.0d;
    private long movingTime = 0;
    private long movingTimeDesc = 0;
    private long restTime = 0;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        dbHelperTrackDb = new DatabaseAdapter(this.getApplicationContext());
        //android.util.Log.w("TrackDbAdapter", "ServiceGPS.onCreate " + ActivityMain.minTimeLocationManager);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        SharedPreferences prefs = getApplicationContext().getSharedPreferences("com.stibla.threedskitracker", Context.MODE_PRIVATE);

        if (prefs.getLong("com.stibla.threedskitracker.idDiary", 0) == 0) {
            DatabaseAdapter dbHelperTrackDb = new DatabaseAdapter(getApplicationContext());
            dbHelperTrackDb.open();
            ActivityMain.idDiary = dbHelperTrackDb.newDiary();
            prefs.edit().putLong("com.stibla.threedskitracker.idDiary", ActivityMain.idDiary).apply();
            dbHelperTrackDb.close();

            ActivityMain.currSpeed = 0.0f;
            ActivityMain.maxSpeed = 0.0f;
            ActivityMain.avgSpeed = 0.0f;
            ActivityMain.descent = 0.0f;
            ActivityMain.ascent = 0.0f;
            count = 0;
            maxAltitude = 0.0d;
            minAltitude = 1000000.0d;
            verticalDescent = 0.0d;
            verticalAscent = 0.0d;
            movingTime = 0;
            movingTimeDesc = 0;
            restTime = 0;
            prevTime = 0;
            prevAlt = 0.0d;
            prevLat = 0.0d;
            prevLon = 0.0d;
            refreshCurrentStat();

        } else {
            ActivityMain.idDiary = prefs.getLong("com.stibla.threedskitracker.idDiary", 0);
            ActivityMain.ascent = prefs.getFloat("com.stibla.threedskitracker.ascent", 0);
            ActivityMain.descent = prefs.getFloat("com.stibla.threedskitracker.descent", 0);
            ActivityMain.avgSpeed = prefs.getFloat("com.stibla.threedskitracker.avgSpeed", 0);
            ActivityMain.maxSpeed = prefs.getFloat("com.stibla.threedskitracker.maxSpeed", 0);
            ActivityMain.currSpeed = prefs.getFloat("com.stibla.threedskitracker.currSpeed", 0);
            count = prefs.getInt("com.stibla.threedskitracker.ServiceGPS.count", 0);
            maxAltitude = Double.longBitsToDouble(prefs.getLong("com.stibla.threedskitracker.ServiceGPS.maxAltitude", 0));
            minAltitude = Double.longBitsToDouble(prefs.getLong("com.stibla.threedskitracker.ServiceGPS.minAltitude", 0));
            verticalDescent = Double.longBitsToDouble(prefs.getLong("com.stibla.threedskitracker.ServiceGPS.verticalDescent", 0));
            verticalAscent = Double.longBitsToDouble(prefs.getLong("com.stibla.threedskitracker.ServiceGPS.verticalAscent", 0));
            movingTime = prefs.getLong("com.stibla.threedskitracker.ServiceGPS.movingTime", 0);
            movingTimeDesc = prefs.getLong("com.stibla.threedskitracker.ServiceGPS.movingTimeDesc", 0);
            restTime = prefs.getLong("com.stibla.threedskitracker.ServiceGPS.restTime", 0);
            prevTime = prefs.getLong("com.stibla.threedskitracker.ServiceGPS.prevTime", 0);
            prevAlt = Double.longBitsToDouble(prefs.getLong("com.stibla.threedskitracker.ServiceGPS.prevAlt", 0));
            prevLat = Double.longBitsToDouble(prefs.getLong("com.stibla.threedskitracker.ServiceGPS.prevLat", 0));
            prevLon = Double.longBitsToDouble(prefs.getLong("com.stibla.threedskitracker.ServiceGPS.prevLon", 0));
            ActivityMain.minTimeLocationManager = Long.valueOf(PreferenceManager.getDefaultSharedPreferences(this).getString("minTimeLocationManager", "1"));
        }

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, ActivityMain.minTimeLocationManager * 1000, 0, this);
        String title, text;

        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            title = "3DskiTracker";
            text = "Looking for GPS signal";
        } else {
            title = "3DskiTracker";
            text = "GPS is disabled";
        }
        addNotification(title.subSequence(0, title.length()), (text).subSequence(0, (text).length()));
        //android.util.Log.w("TrackDbAdapter", "ServiceGPS.onStartCommand " + ActivityMain.minTimeLocationManager);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        //android.util.Log.w("TrackDbAdapter", "ServiceGPS.onDestroy " + ActivityMain.minTimeLocationManager);
        dbHelperTrackDb.open();
        dbHelperTrackDb.updateDiaryStatus(ActivityMain.idDiary, 2);
        dbHelperTrackDb.close();
        ActivityMain.idDiary = 0;

        SharedPreferences prefs = getApplicationContext().getSharedPreferences("com.stibla.threedskitracker", Context.MODE_PRIVATE);
        prefs.edit().putLong("com.stibla.threedskitracker.idDiary", 0).apply();

        cancelNotification();

        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        Intent i = new Intent("COM_STIBLA_THREEDSKITRACKER_TRACK_REFRESH");
        lbm.sendBroadcast(i);
        locationManager.removeUpdates(this);
        super.onDestroy();
    }

    @Override
    public void onLocationChanged(Location location) {
        //android.util.Log.w("TrackDbAdapter", "ServiceGPS.onLocationChanged " + ActivityMain.minTimeLocationManager);
        if (location.getAccuracy() <= ActivityMain.MAX_ACCURACY && ActivityMain.idDiary != 0) {
            String title = "3DskiTracker";
            String text = "Tracking " +
                    (String.format(Locale.US, "%d", ((movingTime + restTime) / (60 * 60 * 1000))).length() == 1 ? "0" : "") + String.format(Locale.US, "%d", ((movingTime + restTime) / (60 * 60 * 1000))) + ":" +
                    (String.format(Locale.US, "%d", (((movingTime + restTime) - ((movingTime + restTime) / (60 * 60 * 1000)) * (60 * 60 * 1000)) / (60 * 1000))).length() == 1 ? "0" : "") + String.format(Locale.US, "%d", (((movingTime + restTime) - ((movingTime + restTime) / (60 * 60 * 1000)) * (60 * 60 * 1000)) / (60 * 1000))) + ":" +
                    (String.format(Locale.US, "%d", (((movingTime + restTime) - ((movingTime + restTime) / (60 * 60 * 1000)) * (60 * 60 * 1000) - (((movingTime + restTime) - ((movingTime + restTime) / (60 * 60 * 1000)) * (60 * 60 * 1000)) / (60 * 1000)) * (60 * 1000)) / (1000))).length() == 1 ? "0" : "") + String.format(Locale.US, "%d", (((movingTime + restTime) - ((movingTime + restTime) / (60 * 60 * 1000)) * (60 * 60 * 1000) - (((movingTime + restTime) - ((movingTime + restTime) / (60 * 60 * 1000)) * (60 * 60 * 1000)) / (60 * 1000)) * (60 * 1000)) / (1000)));
            ++count; // + ";" + location.getLatitude() + ";" + location.getLongitude() + ";" + location.getAltitude();
            addNotification(title.subSequence(0, title.length()), (text).subSequence(0, (text).length()));

            dbHelperTrackDb.open();

            if (count > 1) {
                long time = location.getTime() - prevTime;
                double coordX = ((ActivityMain.RADIUS_OF_EARTH + location.getAltitude()) * java.lang.Math.sin((90 - location.getLatitude()) * java.lang.Math.PI / 180) * java.lang.Math.cos(location.getLongitude() * java.lang.Math.PI / 180));
                double coordY = ((ActivityMain.RADIUS_OF_EARTH + location.getAltitude()) * java.lang.Math.sin((90 - location.getLatitude()) * java.lang.Math.PI / 180) * java.lang.Math.sin(location.getLongitude() * java.lang.Math.PI / 180));
                double coordZ = ((ActivityMain.RADIUS_OF_EARTH + location.getAltitude()) * java.lang.Math.cos((90 - location.getLatitude()) * java.lang.Math.PI / 180));
                double prevCoordX = ((ActivityMain.RADIUS_OF_EARTH + prevAlt) * java.lang.Math.sin((90 - prevLat) * java.lang.Math.PI / 180) * java.lang.Math.cos(prevLon * java.lang.Math.PI / 180));
                double prevCoordY = ((ActivityMain.RADIUS_OF_EARTH + prevAlt) * java.lang.Math.sin((90 - prevLat) * java.lang.Math.PI / 180) * java.lang.Math.sin(prevLon * java.lang.Math.PI / 180));
                double prevCoordZ = ((ActivityMain.RADIUS_OF_EARTH + prevAlt) * java.lang.Math.cos((90 - prevLat) * java.lang.Math.PI / 180));
                double distance = java.lang.Math.sqrt(java.lang.Math.pow(prevCoordX - coordX, 2) + java.lang.Math.pow(prevCoordY - coordY, 2) + java.lang.Math.pow(prevCoordZ - coordZ, 2)) / 1000.0f;

                if (prevAlt < location.getAltitude()) {
                    ActivityMain.ascent += distance;
                    verticalAscent += (location.getAltitude() - prevAlt);
                } else {
                    ActivityMain.descent += distance;
                    verticalDescent += (prevAlt - location.getAltitude());
                }

                ActivityMain.currSpeed = 0;
                if (time > 0) {
                    ActivityMain.currSpeed = (float) (distance / (time / 3600000.0f));
                }

                if (ActivityMain.currSpeed > ActivityMain.MAX_SPEED_REST) {
                    movingTime += time;
                    if (prevAlt >= location.getAltitude()) {
                        movingTimeDesc += time;
                    }
                } else {
                    restTime += time;
                }

                if (ActivityMain.maxSpeed < ActivityMain.currSpeed && prevAlt >= location.getAltitude()) {
                    ActivityMain.maxSpeed = ActivityMain.currSpeed;
                }

                if (movingTimeDesc != 0) {
                    ActivityMain.avgSpeed = (float) (ActivityMain.descent / (movingTimeDesc / 3600000.0f));
                }

                if (maxAltitude < location.getAltitude())
                    maxAltitude = location.getAltitude();
                if (minAltitude > location.getAltitude())
                    minAltitude = location.getAltitude();

                dbHelperTrackDb.updateDiary(ActivityMain.idDiary, movingTime, restTime, ActivityMain.descent, ActivityMain.ascent, ActivityMain.maxSpeed, ActivityMain.avgSpeed, maxAltitude, minAltitude, verticalDescent, verticalAscent);
                refreshCurrentStat();

            } else {
                if (maxAltitude < location.getAltitude())
                    maxAltitude = location.getAltitude();
                if (minAltitude > location.getAltitude())
                    minAltitude = location.getAltitude();
            }

            if (prevAlt != location.getAltitude() ||
                    prevLat != location.getLatitude() ||
                    prevLon != location.getLongitude()) {
                dbHelperTrackDb.insertTrack(location.getAltitude(), //double
                        location.getLatitude(), //double
                        location.getLongitude(), //double
                        location.getSpeed(),  //float
                        location.getAccuracy(),  //float
                        location.getTime(), //long
                        //location.getBearing(), //float
                        //location.getElapsedRealtimeNanos(), //long
                        //(ActivityMain.RADIUS_OF_EARTH + location.getAltitude()) * java.lang.Math.sin(location.getLatitude() * java.lang.Math.PI / 180) * java.lang.Math.cos(location.getLongitude() * java.lang.Math.PI / 180),
                        //(ActivityMain.RADIUS_OF_EARTH + location.getAltitude()) * java.lang.Math.sin(location.getLatitude() * java.lang.Math.PI / 180) * java.lang.Math.sin(location.getLongitude() * java.lang.Math.PI / 180),
                        //(ActivityMain.RADIUS_OF_EARTH + location.getAltitude()) * java.lang.Math.cos(location.getLatitude() * java.lang.Math.PI / 180),
                        //System.currentTimeMillis(),
                        ActivityMain.idDiary //location.getTime()
                );
            }
            prevAlt = location.getAltitude();
            prevLat = location.getLatitude();
            prevLon = location.getLongitude();
            prevTime = location.getTime();

            SharedPreferences prefs = getApplicationContext().getSharedPreferences("com.stibla.threedskitracker", Context.MODE_PRIVATE);
            prefs.edit().putLong("com.stibla.threedskitracker.idDiary", 0).apply();
            prefs.edit().putLong("com.stibla.threedskitracker.idDiary", ActivityMain.idDiary).apply();
            prefs.edit().putFloat("com.stibla.threedskitracker.ascent", ActivityMain.ascent).apply();
            prefs.edit().putFloat("com.stibla.threedskitracker.descent", ActivityMain.descent).apply();
            prefs.edit().putFloat("com.stibla.threedskitracker.avgSpeed", ActivityMain.avgSpeed).apply();
            prefs.edit().putFloat("com.stibla.threedskitracker.maxSpeed", ActivityMain.maxSpeed).apply();
            prefs.edit().putFloat("com.stibla.threedskitracker.currSpeed", ActivityMain.currSpeed).apply();
            prefs.edit().putInt("com.stibla.threedskitracker.ServiceGPS.count", count).apply();
            prefs.edit().putLong("com.stibla.threedskitracker.ServiceGPS.maxAltitude", Double.doubleToLongBits(maxAltitude)).apply();
            prefs.edit().putLong("com.stibla.threedskitracker.ServiceGPS.minAltitude", Double.doubleToLongBits(minAltitude)).apply();
            prefs.edit().putLong("com.stibla.threedskitracker.ServiceGPS.verticalDescent", Double.doubleToLongBits(verticalDescent)).apply();
            prefs.edit().putLong("com.stibla.threedskitracker.ServiceGPS.verticalAscent", Double.doubleToLongBits(verticalAscent)).apply();
            prefs.edit().putLong("com.stibla.threedskitracker.ServiceGPS.movingTime", movingTime).apply();
            prefs.edit().putLong("com.stibla.threedskitracker.ServiceGPS.movingTimeDesc", movingTimeDesc).apply();
            prefs.edit().putLong("com.stibla.threedskitracker.ServiceGPS.restTime", restTime).apply();
            prefs.edit().putLong("com.stibla.threedskitracker.ServiceGPS.prevTime", prevTime).apply();
            prefs.edit().putLong("com.stibla.threedskitracker.ServiceGPS.prevAlt", Double.doubleToLongBits(prevAlt)).apply();
            prefs.edit().putLong("com.stibla.threedskitracker.ServiceGPS.prevLat", Double.doubleToLongBits(prevLat)).apply();
            prefs.edit().putLong("com.stibla.threedskitracker.ServiceGPS.prevLon", Double.doubleToLongBits(prevLon)).apply();

            dbHelperTrackDb.close();
        }
    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    private void addNotification(CharSequence title, CharSequence text) {
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle(title)
                        .setContentText(text)
                        .setOngoing(true);

        Intent notificationIntent = new Intent(this, ActivityMain.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(contentIntent);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        manager.notify("3D_SKI_TRACK_NOTIFY", 1, builder.build());

    }

    private void cancelNotification() {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel("3D_SKI_TRACK_NOTIFY", 1);
    }

    private void refreshCurrentStat() {
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        Intent i = new Intent("COM_STIBLA_THREEDSKITRACKER_TRACK_REFRESH_CURR");
        lbm.sendBroadcast(i);
    }
}
