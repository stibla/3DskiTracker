package com.stibla.threedskitracker;

import java.util.ArrayList;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

public class ActivityMap extends Activity implements OnMapReadyCallback {

    // Google Map
    private GoogleMap googleMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        if (ActivityMain.idDiary == 0) {
            SharedPreferences prefs = getApplicationContext().getSharedPreferences("com.stibla.threedskitracker", Context.MODE_PRIVATE);
            ActivityMain.idDiary = prefs.getLong("com.stibla.threedskitracker.idDiary", 0);
        }

        setContentView(R.layout.map_activity);
        getActionBar().setDisplayHomeAsUpEnabled(true);


        try {
            // Loading map
            initilizeMap();
        } catch (Exception e) {

            e.printStackTrace();
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void initilizeMap() {
        if (googleMap == null) {
            /*googleMap = */
            ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMapAsync(this);

        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        // check if map is created successfully or not
        if (googleMap == null) {
            Toast.makeText(getApplicationContext(),
                    "Sorry! unable to create maps", Toast.LENGTH_SHORT)
                    .show();
        } else {
            googleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);

            final DatabaseAdapter dbHelper = new DatabaseAdapter(getApplicationContext());
            dbHelper.open();
            Cursor cursor = dbHelper.fetchTrack(ActivityMain.idDiary);

            ArrayList<LatLng> coordList = new ArrayList<LatLng>();

            do {
                coordList.add(new LatLng(cursor.getFloat(cursor.getColumnIndex("latitude")),
                        cursor.getFloat(cursor.getColumnIndex("longitude"))));
            } while (cursor.moveToNext());

            PolylineOptions polylineOptions = new PolylineOptions();
            polylineOptions.addAll(coordList);
            polylineOptions
                    .width(4)
                    .color(Color.BLUE);
            googleMap.addPolyline(polylineOptions);

            cursor.moveToFirst();
            googleMap.addMarker(new MarkerOptions().position(new LatLng(cursor.getFloat(cursor.getColumnIndex("latitude")),
                    cursor.getFloat(cursor.getColumnIndex("longitude")))).title("START"));

            cursor.moveToLast();
            googleMap.addMarker(new MarkerOptions().position(new LatLng(cursor.getFloat(cursor.getColumnIndex("latitude")),
                    cursor.getFloat(cursor.getColumnIndex("longitude")))).title("END"));

            //cursor.close();
            //dbHelper.close();

            //googleMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            //	@Override
            //	public void onMapLoaded() {
            //		final DatabaseAdapter dbHelper = new DatabaseAdapter(getApplicationContext());
            //		dbHelper.open();
            //		Cursor
            cursor = dbHelper.fetchTrackMinMax(ActivityMain.idDiary);
            double max_lat = cursor.getDouble(cursor.getColumnIndex("max_lat"));
            double min_lat = cursor.getDouble(cursor.getColumnIndex("min_lat"));
            double max_lon = cursor.getDouble(cursor.getColumnIndex("max_lon"));
            double min_lon = cursor.getDouble(cursor.getColumnIndex("min_lon"));

            googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(
                    new LatLngBounds(
                            new LatLng(min_lat, min_lon),
                            new LatLng(max_lat, max_lon)), 1));
            cursor.close();
            dbHelper.close();
            //	}
            //});

        }
    }

   /* @Override
    protected void onResume() {
        super.onResume();
        initilizeMap();
    }*/

}
