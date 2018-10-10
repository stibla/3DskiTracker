package com.stibla.threedskitracker;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.view.MenuItem;
import android.widget.Toast;

public class ActivitySettings extends PreferenceFragment implements OnSharedPreferenceChangeListener {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        addPreferencesFromResource(R.xml.preferences);
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getView().setBackgroundColor(Color.BLACK);

    }
    
    @Override
    public void onStop() {
    	MenuItem item = ActivityMain.menu.findItem(R.id.menu_settings);
        item.setVisible(true);
        super.onStop();
    }


	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if(key.equals("minTimeLocationManager")){
			try {
				ActivityMain.minTimeLocationManager = Long.valueOf(sharedPreferences.getString(key, "1"));				
	        } catch (Exception e) {
	        	Toast.makeText(this.getActivity(),
                        "GPS update time is not valid", Toast.LENGTH_SHORT)
                        .show();
	        }			
		}
		
		if(key.equals("altitudeOffset")){
			try {
				ActivityMain.altitudeOffset = Float.valueOf(sharedPreferences.getString(key, "-45"));				
	        } catch (Exception e) {
	        	Toast.makeText(this.getActivity(),
                        "Altitude offset is not valid", Toast.LENGTH_SHORT)
                        .show();
	        }			
		}
	}
	
	@Override
	public void onResume() {
	    super.onResume();
	    getPreferenceScreen().getSharedPreferences()
	            .registerOnSharedPreferenceChangeListener(this);
	    MenuItem item = ActivityMain.menu.findItem(R.id.menu_settings);
        item.setVisible(false);
	}

	@Override
	public void onPause() {
	    super.onPause();
	    getPreferenceScreen().getSharedPreferences()
	            .unregisterOnSharedPreferenceChangeListener(this);
	}
}