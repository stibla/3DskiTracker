package com.stibla.threedskitracker;

import java.util.Locale;

import com.stibla.threedskitracker.RangeSeekBar.OnRangeSeekBarChangeListener;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;

public class ActivityFullScreen extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        if (ActivityMain.idDiary == 0) {
            SharedPreferences prefs = getApplicationContext().getSharedPreferences("com.stibla.threedskitracker", Context.MODE_PRIVATE);
            ActivityMain.idDiary = prefs.getLong("com.stibla.threedskitracker.idDiary", 0);
            ActivityMain.altitudeOffset = Float.valueOf(PreferenceManager.getDefaultSharedPreferences(this).getString("altitudeOffset", "-45"));
        }

        setContentView(R.layout.full_screen);

        ImageButton button = (ImageButton) findViewById(R.id.button_fullScreen_motion_rotation);

        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (ActivityMain.motionOrRotation == 1) {
                    ((ImageButton) findViewById(R.id.button_fullScreen_motion_rotation)).setImageResource(R.drawable.rotation); //("Rotation");
                    ActivityMain.motionOrRotation = 2;
                } else {
                    ((ImageButton) findViewById(R.id.button_fullScreen_motion_rotation)).setImageResource(R.drawable.motion); //("Motion");
                    ActivityMain.motionOrRotation = 1;
                }
            }
        });

        if (ActivityMain.motionOrRotation == 2) {
            ((ImageButton) findViewById(R.id.button_fullScreen_motion_rotation)).setImageResource(R.drawable.rotation); //("Rotation");
        } else {
            ((ImageButton) findViewById(R.id.button_fullScreen_motion_rotation)).setImageResource(R.drawable.motion); //("Motion");
        }

        ActivityMain.RangeSeekMin = 0;
        ActivityMain.RangeSeekMax = ActivityMain.GLsizei - 1;
        RangeSeekBar<Integer> seekBar = new RangeSeekBar<Integer>(0, ActivityMain.GLsizei - 1, this);
        seekBar.setOnRangeSeekBarChangeListener(new OnRangeSeekBarChangeListener<Integer>() {
            @Override
            public void onRangeSeekBarValuesChanged(RangeSeekBar<?> bar, Integer minValue, Integer maxValue) {
                ActivityMain.RangeSeekMin = minValue;
                ActivityMain.RangeSeekMax = maxValue;
                refreshFullScreenAnalyse();
            }
        });

        refreshFullScreenAnalyse();

        ViewGroup layout = (ViewGroup) findViewById(R.id.rangeSeekBar);
        layout.addView(seekBar);

    }

    @Override
    public void onResume() {
        ActivityMain.IsFullScreen = true;
        super.onResume();
    }

    public void refreshFullScreenAnalyse() {
        //android.util.Log.w("TrackDbAdapter", "min:" + ActivityMain.RangeSeekMin + " max:" + ActivityMain.RangeSeekMax);
        final DatabaseAdapter dbHelper = new DatabaseAdapter(this);
        dbHelper.open();
        int count = 0;
        long prevTime = 0;
        double prevAlt = 0.0d;
        double prevLat = 0.0d;
        double prevLon = 0.0d;
        double currSpeed = 0.0f;
        double maxSpeed = 0.0f;
        double avgSpeed = 0.0f;
        double descent = 0.0f;
        double bluePiste = 0.0f;
        double redPiste = 0.0f;
        double blackPiste = 0.0f;
        double ascent = 0.0f;
        double maxAltitude = 0.0d;
        double minAltitude = 1000000.0d;
        double verticalDescent = 0.0d;
        double verticalAscent = 0.0d;
        double startDistance = 0.0d;
        long movingTime = 0;
        long movingTimeDesc = 0;
        long startTime = 0;
        long restTime = 0;

        //android.util.Log.w("TrackDbAdapter", "RADIUS_OF_EARTH:" + ActivityMain.RADIUS_OF_EARTH);
        Cursor cursor = dbHelper.fetchTrack(ActivityMain.idDiary);
        if (cursor.getCount() == 0) return;
        do {
            long time = cursor.getLong(cursor.getColumnIndex("time"));
            double alt = cursor.getDouble(cursor.getColumnIndex("altitude"));
            double lat = cursor.getDouble(cursor.getColumnIndex("latitude"));
            double lon = cursor.getDouble(cursor.getColumnIndex("longitude"));

            if (count > 0 && count < ActivityMain.RangeSeekMin) {
                double coordX = ((ActivityMain.RADIUS_OF_EARTH + alt) * java.lang.Math.sin((90 - lat) * java.lang.Math.PI / 180) * java.lang.Math.cos(lon * java.lang.Math.PI / 180));
                double coordY = ((ActivityMain.RADIUS_OF_EARTH + alt) * java.lang.Math.sin((90 - lat) * java.lang.Math.PI / 180) * java.lang.Math.sin(lon * java.lang.Math.PI / 180));
                double coordZ = ((ActivityMain.RADIUS_OF_EARTH + alt) * java.lang.Math.cos((90 - lat) * java.lang.Math.PI / 180));
                double prevCoordX = ((ActivityMain.RADIUS_OF_EARTH + prevAlt) * java.lang.Math.sin((90 - prevLat) * java.lang.Math.PI / 180) * java.lang.Math.cos(prevLon * java.lang.Math.PI / 180));
                double prevCoordY = ((ActivityMain.RADIUS_OF_EARTH + prevAlt) * java.lang.Math.sin((90 - prevLat) * java.lang.Math.PI / 180) * java.lang.Math.sin(prevLon * java.lang.Math.PI / 180));
                double prevCoordZ = ((ActivityMain.RADIUS_OF_EARTH + prevAlt) * java.lang.Math.cos((90 - prevLat) * java.lang.Math.PI / 180));
                double distance = java.lang.Math.sqrt(java.lang.Math.pow(prevCoordX - coordX, 2) + java.lang.Math.pow(prevCoordY - coordY, 2) + java.lang.Math.pow(prevCoordZ - coordZ, 2)) / 1000.0f;
                startDistance += distance;
            }

            if (count == ActivityMain.RangeSeekMin) {
                if (maxAltitude < alt)
                    maxAltitude = alt;
                if (minAltitude > alt)
                    minAltitude = alt;
                startTime = time;
            }

            if (count > ActivityMain.RangeSeekMin && count <= ActivityMain.RangeSeekMax + 1) {
                double timeDiff = time - prevTime;
                double coordX = ((ActivityMain.RADIUS_OF_EARTH + alt) * java.lang.Math.sin((90 - lat) * java.lang.Math.PI / 180) * java.lang.Math.cos(lon * java.lang.Math.PI / 180));
                double coordY = ((ActivityMain.RADIUS_OF_EARTH + alt) * java.lang.Math.sin((90 - lat) * java.lang.Math.PI / 180) * java.lang.Math.sin(lon * java.lang.Math.PI / 180));
                double coordZ = ((ActivityMain.RADIUS_OF_EARTH + alt) * java.lang.Math.cos((90 - lat) * java.lang.Math.PI / 180));
                double prevCoordX = ((ActivityMain.RADIUS_OF_EARTH + prevAlt) * java.lang.Math.sin((90 - prevLat) * java.lang.Math.PI / 180) * java.lang.Math.cos(prevLon * java.lang.Math.PI / 180));
                double prevCoordY = ((ActivityMain.RADIUS_OF_EARTH + prevAlt) * java.lang.Math.sin((90 - prevLat) * java.lang.Math.PI / 180) * java.lang.Math.sin(prevLon * java.lang.Math.PI / 180));
                double prevCoordZ = ((ActivityMain.RADIUS_OF_EARTH + prevAlt) * java.lang.Math.cos((90 - prevLat) * java.lang.Math.PI / 180));
                double distance = java.lang.Math.sqrt(java.lang.Math.pow(prevCoordX - coordX, 2) + java.lang.Math.pow(prevCoordY - coordY, 2) + java.lang.Math.pow(prevCoordZ - coordZ, 2)) / 1000.0f;

                if (prevAlt < alt) {
                    ascent += distance;
                    verticalAscent += (alt - prevAlt);
                } else {
                    descent += distance;
                    verticalDescent += (prevAlt - alt);
                    if (distance > 0) {
                        if (((prevAlt - alt) / (distance * 1000) * 100) >= ActivityMain.MAX_RED_PISTE) {
                            blackPiste += distance;
                        }
                        if (((prevAlt - alt) / (distance * 1000) * 100) < ActivityMain.MAX_RED_PISTE && ((prevAlt - alt) / (distance * 1000) * 100) >= ActivityMain.MAX_BLUE_PISTE) {
                            redPiste += distance;
                        }
                        if (((prevAlt - alt) / (distance * 1000) * 100) < ActivityMain.MAX_BLUE_PISTE) {
                            bluePiste += distance;
                        }
                    } else {
                        bluePiste += distance;
                    }

                }

                currSpeed = 0;
                if (timeDiff > 0) {
                    currSpeed = (float) (distance / (timeDiff / 3600000.0f));
                }

                if (currSpeed > ActivityMain.MAX_SPEED_REST) {
                    movingTime += timeDiff;
                    if (prevAlt >= alt) {
                        movingTimeDesc += timeDiff;
                    }
                } else {
                    restTime += timeDiff;
                }

                if (maxSpeed < currSpeed && prevAlt >= alt) {
                    maxSpeed = currSpeed;
                }

                if (movingTimeDesc != 0) {
                    avgSpeed = (float) (descent / (movingTimeDesc / 3600000.0f));
                }

                if (maxAltitude < alt)
                    maxAltitude = alt;
                if (minAltitude > alt)
                    minAltitude = alt;

            }

            prevAlt = alt;
            prevLat = lat;
            prevLon = lon;
            prevTime = time;
            count++;
        } while (cursor.moveToNext() && count <= ActivityMain.RangeSeekMax + 1);

        cursor.close();
        dbHelper.close();
        ((TextView) findViewById(R.id.textView_fullScreen_startPoint)).setText(String.format(Locale.US, "%.3f", startDistance) + " km");
        ((TextView) findViewById(R.id.textView_fullScreen_Distance)).setText(String.format(Locale.US, "%.3f", ascent + descent) + " km");
        ((TextView) findViewById(R.id.textView_fullScreen_Descent)).setText(String.format(Locale.US, "%.3f", descent) + " km");
        ((TextView) findViewById(R.id.textView_fullScreen_BluePiste)).setText(String.format(Locale.US, "%.3f", bluePiste) + " km / " + String.format(Locale.US, "%.1f", bluePiste / descent * 100) + " %");
        ((TextView) findViewById(R.id.textView_fullScreen_RedPiste)).setText(String.format(Locale.US, "%.3f", redPiste) + " km / " + String.format(Locale.US, "%.1f", redPiste / descent * 100) + " %");
        ((TextView) findViewById(R.id.textView_fullScreen_BlackPiste)).setText(String.format(Locale.US, "%.3f", blackPiste) + " km / " + String.format(Locale.US, "%.1f", blackPiste / descent * 100) + " %");
        ((TextView) findViewById(R.id.textView_fullScreen_Ascent)).setText(String.format(Locale.US, "%.3f", ascent) + " km");
        ((TextView) findViewById(R.id.textView_fullScreen_MaxSpeed)).setText(String.format(Locale.US, "%.1f", maxSpeed) + " km/h");
        ((TextView) findViewById(R.id.textView_fullScreen_AvgSpeed)).setText(String.format(Locale.US, "%.1f", avgSpeed) + " km/h");
        ((TextView) findViewById(R.id.textView_fullScreen_MaxAlt)).setText(String.format(Locale.US, "%.0f", maxAltitude + ActivityMain.altitudeOffset) + " m");
        ((TextView) findViewById(R.id.textView_fullScreen_MinAlt)).setText(String.format(Locale.US, "%.0f", minAltitude + ActivityMain.altitudeOffset) + " m");
        //((TextView) findViewById(R.id.textView_fullScreen_StartTime)).setText(String.format(Locale.US, "%td.%tm.%tY %tH:%tM:%tS", (startTime), (startTime), (startTime), (startTime), (startTime), (startTime)));
        ((TextView) findViewById(R.id.textView_fullScreen_StartTime)).setText(String.format(Locale.US, "%tH:%tM:%tS", (startTime), (startTime), (startTime)));

        ((TextView) findViewById(R.id.textView_fullScreen_Duration)).setText(
                (String.format(Locale.US, "%d", ((movingTime + restTime) / (60 * 60 * 1000))).length() == 1 ? "0" : "") + String.format(Locale.US, "%d", ((movingTime + restTime) / (60 * 60 * 1000))) + ":" +
                        (String.format(Locale.US, "%d", (((movingTime + restTime) - ((movingTime + restTime) / (60 * 60 * 1000)) * (60 * 60 * 1000)) / (60 * 1000))).length() == 1 ? "0" : "") + String.format(Locale.US, "%d", (((movingTime + restTime) - ((movingTime + restTime) / (60 * 60 * 1000)) * (60 * 60 * 1000)) / (60 * 1000))) + ":" +
                        (String.format(Locale.US, "%d", (((movingTime + restTime) - ((movingTime + restTime) / (60 * 60 * 1000)) * (60 * 60 * 1000) - (((movingTime + restTime) - ((movingTime + restTime) / (60 * 60 * 1000)) * (60 * 60 * 1000)) / (60 * 1000)) * (60 * 1000)) / (1000))).length() == 1 ? "0" : "") + String.format(Locale.US, "%d", (((movingTime + restTime) - ((movingTime + restTime) / (60 * 60 * 1000)) * (60 * 60 * 1000) - (((movingTime + restTime) - ((movingTime + restTime) / (60 * 60 * 1000)) * (60 * 60 * 1000)) / (60 * 1000)) * (60 * 1000)) / (1000))));

        ((TextView) findViewById(R.id.textView_fullScreen_MovingTime)).setText(
                (String.format(Locale.US, "%d", (movingTime / (60 * 60 * 1000))).length() == 1 ? "0" : "") + String.format(Locale.US, "%d", (movingTime / (60 * 60 * 1000))) + ":" +
                        (String.format(Locale.US, "%d", ((movingTime - (movingTime / (60 * 60 * 1000)) * (60 * 60 * 1000)) / (60 * 1000))).length() == 1 ? "0" : "") + String.format(Locale.US, "%d", ((movingTime - (movingTime / (60 * 60 * 1000)) * (60 * 60 * 1000)) / (60 * 1000))) + ":" +
                        (String.format(Locale.US, "%d", ((movingTime - (movingTime / (60 * 60 * 1000)) * (60 * 60 * 1000) - ((movingTime - (movingTime / (60 * 60 * 1000)) * (60 * 60 * 1000)) / (60 * 1000)) * (60 * 1000)) / (1000))).length() == 1 ? "0" : "") + String.format(Locale.US, "%d", ((movingTime - (movingTime / (60 * 60 * 1000)) * (60 * 60 * 1000) - ((movingTime - (movingTime / (60 * 60 * 1000)) * (60 * 60 * 1000)) / (60 * 1000)) * (60 * 1000)) / (1000))));

        ((TextView) findViewById(R.id.textView_fullScreen_MovingTimeDesc)).setText(
                (String.format(Locale.US, "%d", (movingTimeDesc / (60 * 60 * 1000))).length() == 1 ? "0" : "") + String.format(Locale.US, "%d", (movingTimeDesc / (60 * 60 * 1000))) + ":" +
                        (String.format(Locale.US, "%d", ((movingTimeDesc - (movingTimeDesc / (60 * 60 * 1000)) * (60 * 60 * 1000)) / (60 * 1000))).length() == 1 ? "0" : "") + String.format(Locale.US, "%d", ((movingTimeDesc - (movingTimeDesc / (60 * 60 * 1000)) * (60 * 60 * 1000)) / (60 * 1000))) + ":" +
                        (String.format(Locale.US, "%d", ((movingTimeDesc - (movingTimeDesc / (60 * 60 * 1000)) * (60 * 60 * 1000) - ((movingTimeDesc - (movingTimeDesc / (60 * 60 * 1000)) * (60 * 60 * 1000)) / (60 * 1000)) * (60 * 1000)) / (1000))).length() == 1 ? "0" : "") + String.format(Locale.US, "%d", ((movingTimeDesc - (movingTimeDesc / (60 * 60 * 1000)) * (60 * 60 * 1000) - ((movingTimeDesc - (movingTimeDesc / (60 * 60 * 1000)) * (60 * 60 * 1000)) / (60 * 1000)) * (60 * 1000)) / (1000))));

        ((TextView) findViewById(R.id.textView_fullScreen_RestTime)).setText(
                (String.format(Locale.US, "%d", (restTime / (60 * 60 * 1000))).length() == 1 ? "0" : "") + String.format(Locale.US, "%d", (restTime / (60 * 60 * 1000))) + ":" +
                        (String.format(Locale.US, "%d", ((restTime - (restTime / (60 * 60 * 1000)) * (60 * 60 * 1000)) / (60 * 1000))).length() == 1 ? "0" : "") + String.format(Locale.US, "%d", ((restTime - (restTime / (60 * 60 * 1000)) * (60 * 60 * 1000)) / (60 * 1000))) + ":" +
                        (String.format(Locale.US, "%d", ((restTime - (restTime / (60 * 60 * 1000)) * (60 * 60 * 1000) - ((restTime - (restTime / (60 * 60 * 1000)) * (60 * 60 * 1000)) / (60 * 1000)) * (60 * 1000)) / (1000))).length() == 1 ? "0" : "") + String.format(Locale.US, "%d", ((restTime - (restTime / (60 * 60 * 1000)) * (60 * 60 * 1000) - ((restTime - (restTime / (60 * 60 * 1000)) * (60 * 60 * 1000)) / (60 * 1000)) * (60 * 1000)) / (1000))));

        ((TextView) findViewById(R.id.textView_fullScreen_VerticalDescent)).setText(String.format(Locale.US, "%.0f", verticalDescent) + " m");
        ((TextView) findViewById(R.id.textView_fullScreen_VerticalAscent)).setText(String.format(Locale.US, "%.0f", verticalAscent) + " m");
        ((TextView) findViewById(R.id.textView_fullScreen_Slope)).setText(String.format(Locale.US, "%.1f", (verticalDescent / (descent * 1000)) * 100) + " % / " + String.format(Locale.US, "%.1f", java.lang.Math.asin(verticalDescent / (descent * 1000)) * (180 / java.lang.Math.PI)) + " ");
    }

}
