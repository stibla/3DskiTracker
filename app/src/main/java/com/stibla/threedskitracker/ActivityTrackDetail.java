package com.stibla.threedskitracker;

import java.util.Locale;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

public class ActivityTrackDetail extends Activity {
	
	private ProgressBar bar;

   @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //android.util.Log.w("TrackDbAdapter", "ActivityTrackDetail.onCreate");
        setContentView(R.layout.track_detail);
        
        if(ActivityMain.idDiary == 0) {
			SharedPreferences prefs = getApplicationContext().getSharedPreferences("com.stibla.threedskitracker", Context.MODE_PRIVATE);			
			ActivityMain.idDiary = prefs.getLong("com.stibla.threedskitracker.idDiary",0);
			ActivityMain.altitudeOffset = Float.valueOf(PreferenceManager.getDefaultSharedPreferences(this).getString("altitudeOffset", "-45"));
		}
        
        getActionBar().setDisplayHomeAsUpEnabled(true);
        
        bar = (ProgressBar) this.findViewById(R.id.progressbar);
                 
        ImageButton button = (ImageButton) findViewById(R.id.button_trackDetail_motion_rotation);

		button.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (ActivityMain.motionOrRotation == 1) {					
					((ImageButton) findViewById(R.id.button_trackDetail_motion_rotation)).setImageResource(R.drawable.rotation); //("Rotation");
					ActivityMain.motionOrRotation = 2;
				} else {
					((ImageButton) findViewById(R.id.button_trackDetail_motion_rotation)).setImageResource(R.drawable.motion); //("Motion");
					ActivityMain.motionOrRotation = 1;
				}
			}
		});
		
		if (ActivityMain.motionOrRotation == 2) {					
			((ImageButton) findViewById(R.id.button_trackDetail_motion_rotation)).setImageResource(R.drawable.rotation); //("Rotation");
		} else {
			((ImageButton) findViewById(R.id.button_trackDetail_motion_rotation)).setImageResource(R.drawable.motion); //("Motion");			
		}
		
		ImageButton buttonFullScreen = (ImageButton) findViewById(R.id.button_trackDetail_full_screen);

		buttonFullScreen.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent detailIntent = new Intent(getApplicationContext(), ActivityFullScreen.class);
				startActivity(detailIntent);
			}
		});
		
		ImageButton buttonMap = (ImageButton) findViewById(R.id.button_trackDetail_map);

		buttonMap.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				new ProgressTask().execute();							
			}
		});
		
        refreshTrack();
    }
    
    /*@Override
    protected void onRestart() {
        super.onStart();
        android.util.Log.w("TrackDbAdapter", "ActivityTrackDetail.onRestart");
    }
    
    @Override
    protected void onStart() {
        super.onStart();
        android.util.Log.w("TrackDbAdapter", "ActivityTrackDetail.onStart");
    }*/

   @Override
	public void onResume() {
	   ActivityMain.IsFullScreen = false;
	   super.onResume();
   }
   
   @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
        	finish();            
            return true;
        }

        return super.onOptionsItemSelected(item);
    }	
   
   public void refreshTrack() {
	    DatabaseAdapter dbHelper;
		dbHelper = new DatabaseAdapter(this);
		dbHelper.open();
		Cursor cursor = dbHelper.fetchAnalyseDiary(ActivityMain.idDiary);
		((TextView) findViewById(R.id.textView_trackDetail_ASstartTime)).setText(cursor.getString(cursor.getColumnIndex("startTime")));
		((TextView) findViewById(R.id.textView_trackDetail_ASdistance)).setText(cursor.getString(cursor.getColumnIndex("distance")));
		((TextView) findViewById(R.id.textView_trackDetail_ASdescent)).setText(String.format(Locale.US, "%.3f", cursor.getFloat(cursor.getColumnIndex("descent"))) + " km"); //((TextView) findViewById(R.id.textView_trackDetail_ASdescent)).setText(cursor.getString(cursor.getColumnIndex("descent")));
		((TextView) findViewById(R.id.textView_trackDetail_ASascent)).setText(cursor.getString(cursor.getColumnIndex("ascent")));
		((TextView) findViewById(R.id.textView_trackDetail_ASmaxSpeed)).setText(cursor.getString(cursor.getColumnIndex("maxSpeed")));
		((TextView) findViewById(R.id.textView_trackDetail_ASavgSpeed)).setText(cursor.getString(cursor.getColumnIndex("avgSpeed")));
		((TextView) findViewById(R.id.textView_trackDetail_ASduration)).setText(cursor.getString(cursor.getColumnIndex("duration")));
		((TextView) findViewById(R.id.textView_trackDetail_ASmovingTime)).setText(cursor.getString(cursor.getColumnIndex("movingTime")));
		((TextView) findViewById(R.id.textView_trackDetail_ASrestTime)).setText(cursor.getString(cursor.getColumnIndex("restTime")));
		((TextView) findViewById(R.id.textView_trackDetail_ASmaxAltitude)).setText(String.format(Locale.US, "%.0f", cursor.getFloat(cursor.getColumnIndex("maxAltitude")) + ActivityMain.altitudeOffset) + " m");
		((TextView) findViewById(R.id.textView_trackDetail_ASminAltitude)).setText(String.format(Locale.US, "%.0f", cursor.getFloat(cursor.getColumnIndex("minAltitude")) + ActivityMain.altitudeOffset) + " m");
		((TextView) findViewById(R.id.textView_trackDetail_ASvertical)).setText(String.format(Locale.US, "%.0f", cursor.getFloat(cursor.getColumnIndex("vertical"))) + " m");
		((TextView) findViewById(R.id.textView_trackDetail_ASverticalDescent)).setText(String.format(Locale.US, "%.0f", cursor.getFloat(cursor.getColumnIndex("verticalDescent"))) + " m");
		((TextView) findViewById(R.id.textView_trackDetail_ASverticalAscent)).setText(String.format(Locale.US, "%.0f", cursor.getFloat(cursor.getColumnIndex("verticalAscent"))) + " m");
		if(cursor.getFloat(cursor.getColumnIndex("descent")) != 0) {
			((TextView) findViewById(R.id.textView_trackDetail_ASslopePer)).setText(String.format(Locale.US, "%.1f", (cursor.getFloat(cursor.getColumnIndex("verticalDescent"))/(cursor.getFloat(cursor.getColumnIndex("descent")) * 1000)) * 100) + " %");
			((TextView) findViewById(R.id.textView_trackDetail_ASslopeDeg)).setText(String.format(Locale.US, "%.1f", java.lang.Math.asin(cursor.getFloat(cursor.getColumnIndex("verticalDescent"))/(cursor.getFloat(cursor.getColumnIndex("descent")) * 1000)) * (180/java.lang.Math.PI)) + " deg");
		}			
		dbHelper.close();

	}
   
   private class ProgressTask extends AsyncTask <Void,Void,Void>{
       @Override
       protected void onPreExecute(){
           bar.setVisibility(View.VISIBLE);
       }

       @Override
       protected Void doInBackground(Void... arg0) {
    	   Intent detailIntent = new Intent(getApplicationContext(), ActivityMap.class);
    	   startActivity(detailIntent);
			
    	   return null;  
       }

       @Override
       protected void onPostExecute(Void result) {
             bar.setVisibility(View.GONE);
       }
   }
}
