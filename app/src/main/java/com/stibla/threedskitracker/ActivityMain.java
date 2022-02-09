package com.stibla.threedskitracker;

import java.util.Locale;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;

public class ActivityMain extends Activity {

	private MyReceiver myReceiver;
	private MyReceiverCurr myReceiverCurr;
	private Context context;
	public static Menu menu;	
	public static long idDiary;
	public static int width, height;
	public static float angleCubeX = 0.0f;    
	public static float angleCubeY = 0.0f;   
	public static float increase = -3.2f;
	public static float translateX = 0.0f;
	public static float translateY = 0.0f;	 
	public static int motionOrRotation = 1; //1 - rotation, 2 - motion
	public static int GLsizei;
	public static int RangeSeekMin;
	public static int RangeSeekMax;
	public static boolean IsFullScreen;	
	public static float currSpeed = 0.0f;
	public static float maxSpeed = 0.0f;
	public static float avgSpeed = 0.0f;
	public static float descent = 0.0f;
	public static float ascent = 0.0f;	
	public static long minTimeLocationManager = 1;
	public static float altitudeOffset = -45.0f;
	
	public static final int RADIUS_OF_EARTH = 6378100;
	public static final int NO_OF_GROUND_LINE = 12;
	public static final float MAX_ACCURACY = 20.0f;
	public static final float TRACK_LINE_WIDTH = 6.0f;
	public static final float TRACK_LINE_WIDTH_NON_SELECTED = 1.0f;
	public static final float TRACK_LINE_WIDTH_SELECTED = 8.0f;
	public static final float TRACK_TRIANGLE_WIDTH = 0.03f;
	public static final float MAX_BLUE_PISTE = 25.0f;
	public static final float MAX_RED_PISTE = 40.0f;
	public static final float GROUND_LINE_WIDTH = 1.0f;
	public static final float ROTATION_SPEED = 5.0f;
	public static final float ZOOM_SPEED = 1.05f;
	public static final float TEXT_SIZE = 100.0f;
	public static final float TEXT_HEIGHT = 0.1f;
	public static final float MAX_SPEED_REST = 1.0f;

	private class MyReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			refreshDiary();
			refreshGlobalDiary();
		}
	}
	
	private class MyReceiverCurr extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			refreshCurrent();
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		//android.util.Log.w("TrackDbAdapter", "ActivityMain.onCreate");
		super.onCreate(savedInstanceState);
		context = this;
		setContentView(R.layout.activity_main);
		
		Button button = (Button) findViewById(R.id.startStopBTN);

		button.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (isMyServiceRunning(ServiceGPS.class)) {
					getApplicationContext().stopService(new Intent(getApplicationContext(), ServiceGPS.class));
					((Button) findViewById(R.id.startStopBTN)).setText("START NEW RUN");
					(findViewById(R.id.linearLayout_activityMain_listDiary)).setVisibility(View.VISIBLE);
					(findViewById(R.id.linearLayout_activityMain_GlobalStats)).setVisibility(View.VISIBLE);
					(findViewById(R.id.linearLayout_activityMain_CurrStats)).setVisibility(View.GONE);
					refreshGlobalDiary();
				} else {
					if (testGPS()) {
						SharedPreferences prefs = getApplicationContext().getSharedPreferences("com.stibla.threedskitracker", Context.MODE_PRIVATE);			
						prefs.edit().putLong("com.stibla.threedskitracker.idDiary", 0).apply();
							
						getApplicationContext().startService(new Intent(getApplicationContext(), ServiceGPS.class));
						((Button) findViewById(R.id.startStopBTN)).setText("STOP");
						refreshCurrent();						
						(findViewById(R.id.linearLayout_activityMain_listDiary)).setVisibility(View.GONE);
						(findViewById(R.id.linearLayout_activityMain_GlobalStats)).setVisibility(View.GONE);
						(findViewById(R.id.linearLayout_activityMain_CurrStats)).setVisibility(View.VISIBLE);
					} else {
						buildAlertMessageNoGps();
					}
				}
			}
		});

		refreshDiary();

		try {
			ActivityMain.minTimeLocationManager = Long.valueOf(PreferenceManager.getDefaultSharedPreferences(this).getString("minTimeLocationManager", "1"));
			ActivityMain.altitudeOffset = Float.valueOf(PreferenceManager.getDefaultSharedPreferences(this).getString("altitudeOffset", "-45"));
        } catch (Exception e) {
        
        }

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_main, menu);
		ActivityMain.menu = menu;
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_settings:
			getFragmentManager().beginTransaction()
				.replace(android.R.id.content, new ActivitySettings())
				.addToBackStack(null)
				.commit();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	@Override
	public void onPause() {
		super.onPause();
		LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(myReceiver);
		LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(myReceiverCurr);
		//android.util.Log.w("TrackDbAdapter", "ActivityMain.onPause");
	}
		
	@Override
	public void onResume() {
		//android.util.Log.w("TrackDbAdapter", "ActivityMain.onResume");
		if (isMyServiceRunning(ServiceGPS.class)) {
			((Button) findViewById(R.id.startStopBTN)).setText("STOP");
			(findViewById(R.id.linearLayout_activityMain_listDiary)).setVisibility(View.GONE);
			(findViewById(R.id.linearLayout_activityMain_GlobalStats)).setVisibility(View.GONE);
			(findViewById(R.id.linearLayout_activityMain_CurrStats)).setVisibility(View.VISIBLE);
			refreshCurrent();			
		} else {
			((Button) findViewById(R.id.startStopBTN)).setText("START NEW RUN");
			(findViewById(R.id.linearLayout_activityMain_listDiary)).setVisibility(View.VISIBLE);
			(findViewById(R.id.linearLayout_activityMain_GlobalStats)).setVisibility(View.VISIBLE);
			(findViewById(R.id.linearLayout_activityMain_CurrStats)).setVisibility(View.GONE);
			refreshGlobalDiary();
		}
		super.onResume();
		myReceiver = new MyReceiver();
		myReceiverCurr = new MyReceiverCurr();
	    LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(myReceiver, new IntentFilter("COM_STIBLA_THREEDSKITRACKER_TRACK_REFRESH"));
	    LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(myReceiverCurr, new IntentFilter("COM_STIBLA_THREEDSKITRACKER_TRACK_REFRESH_CURR"));
	}

	/*@Override
    protected void onRestart() {
        super.onStart();
        android.util.Log.w("TrackDbAdapter", "ActivityMain.onRestart");
    }
    
    @Override
    protected void onStart() {
        super.onStart();
        android.util.Log.w("TrackDbAdapter", "ActivityMain.onStart");
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        android.util.Log.w("TrackDbAdapter", "ActivityMain.onStop");
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        android.util.Log.w("TrackDbAdapter", "ActivityMain.onDestroy");
    }*/
	
	private boolean isMyServiceRunning(Class<?> serviceClass) {
		ActivityManager manager = (ActivityManager) getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
			if (serviceClass.getName().equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}

	private void buildAlertMessageNoGps() {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setMessage("GPS is disabled in your device. Would you like to enable it?").setCancelable(false).setPositiveButton("Goto Settings Page To Enable GPS", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				Intent callGPSSettingIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
				startActivity(callGPSSettingIntent);
			}
		}).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				dialog.cancel();
			}
		});
		AlertDialog alert = builder.create();
		alert.show();
	}

	public boolean testGPS() {
		LocationManager manager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
		return manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
	}

	public void refreshDiary() {
		SimpleCursorAdapter dataAdapter;
		final DatabaseAdapter dbHelper = new DatabaseAdapter(getApplicationContext());
		dbHelper.open();
		
		//dbHelper.cleanDB();
		
		Cursor cursor = dbHelper.fetchAllDiary();
		String[] columns = new String[] { "_id", "maxSpeed", "avgSpeed", "distance", "startTime" };
		int[] to = new int[] { R.id.textView_diaryListDetail_ID, R.id.textView_diaryListDetail_maxSpeed, R.id.textView_diaryListDetail_avgSpeed, R.id.textView_diaryListDetail_distance, R.id.textView_diaryListDetail_startTime };
		dataAdapter = new SimpleCursorAdapter(getApplicationContext(), R.layout.detail_diary_list, cursor, columns, to, 0);
		ListView listView = (ListView) findViewById(R.id.listView_activityMain_Diary);
		listView.setAdapter(dataAdapter);
		
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
				Intent detailIntent = new Intent(getApplicationContext(), ActivityTrackDetail.class);
				
				SharedPreferences prefs = getApplicationContext().getSharedPreferences("com.stibla.threedskitracker", Context.MODE_PRIVATE);
				prefs.edit().putLong("com.stibla.threedskitracker.idDiary", Long.valueOf(((TextView) view.findViewById(R.id.textView_diaryListDetail_ID)).getText().toString())).apply();
				ActivityMain.idDiary = Long.valueOf(((TextView) view.findViewById(R.id.textView_diaryListDetail_ID)).getText().toString());
				
				startActivity(detailIntent);
			}
		});

		listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, final View view, int position, long id) {
				AlertDialog.Builder builder = new AlertDialog.Builder(context);
				builder.setMessage("Delete ?").setCancelable(false).setPositiveButton("Yes", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dbHelper.open();
						dbHelper.deleteDiary(((TextView) view.findViewById(R.id.textView_diaryListDetail_ID)).getText().toString());
						dbHelper.close();
						refreshDiary();
						refreshGlobalDiary();
					}
				}).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});
				AlertDialog alert = builder.create();
				alert.show();
				return true;
			}
		});
		dbHelper.close();
	}
	
	public void refreshCurrent() {
		((TextView) findViewById(R.id.textView_activityMain_CScurrSpeed)).setText(String.format(Locale.US, "%.1f", ActivityMain.currSpeed) + " km/h");
		((TextView) findViewById(R.id.textView_activityMain_CSavgSpeed)).setText(String.format(Locale.US, "%.1f", ActivityMain.avgSpeed) + " km/h");
		((TextView) findViewById(R.id.textView_activityMain_CSmaxSpeed)).setText(String.format(Locale.US, "%.1f", ActivityMain.maxSpeed) + " km/h");
		((TextView) findViewById(R.id.textView_activityMain_CSdistance)).setText(String.format(Locale.US, "%.3f", ActivityMain.descent+ActivityMain.ascent) + " km");
		((TextView) findViewById(R.id.textView_activityMain_CSdescent)).setText(String.format(Locale.US, "%.3f", ActivityMain.descent) + " km");
		((TextView) findViewById(R.id.textView_activityMain_CSascent)).setText(String.format(Locale.US, "%.3f", ActivityMain.ascent) + " km");
	}
	
	public void refreshGlobalDiary() {
		final DatabaseAdapter dbHelper = new DatabaseAdapter(getApplicationContext());
		dbHelper.open();
		Cursor cursor = dbHelper.fetchGlobalDiary();
		((TextView) findViewById(R.id.textView_activityMain_GSdistance)).setText(String.format(Locale.US, "%.3f", cursor.getFloat(cursor.getColumnIndex("distance")))+ " km");
		((TextView) findViewById(R.id.textView_activityMain_GSruns)).setText(String.format(Locale.US, "%d", cursor.getInt(cursor.getColumnIndex("runs"))));
		((TextView) findViewById(R.id.textView_activityMain_GSdescent)).setText(String.format(Locale.US, "%.3f", cursor.getFloat(cursor.getColumnIndex("descent"))) + " km");
		((TextView) findViewById(R.id.textView_activityMain_GSavgSpeed)).setText(String.format(Locale.US, "%.1f", cursor.getFloat(cursor.getColumnIndex("avgSpeed"))) + " km/h");
		((TextView) findViewById(R.id.textView_activityMain_GSmaxSpeed)).setText(String.format(Locale.US, "%.1f", cursor.getFloat(cursor.getColumnIndex("maxSpeed"))) + " km/h");
		cursor.close();
		dbHelper.close();
	}
	
}
/*
 * private void setDimButtons(boolean dimButtons) { android.view.Window window =
 * getWindow(); android.view.WindowManager.LayoutParams layoutParams =
 * window.getAttributes(); float val = dimButtons ? 0 : -1; try {
 * java.lang.reflect.Field buttonBrightness =
 * layoutParams.getClass().getField("buttonBrightness");
 * buttonBrightness.set(layoutParams, val); } catch (Exception e) {
 * e.printStackTrace(); } window.setAttributes(layoutParams); }
 * 
 * android.util.Log.w("TrackDbAdapter", "" + tab.getPosition());
 */