package com.stibla.threedskitracker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseAdapter {

	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDb;
	private Context mCtx;

	private static class DatabaseHelper extends SQLiteOpenHelper {

		DatabaseHelper(Context context) {
			super(context, "ThreeDskiTracker", null, 2); // "ThreeDskiTracker" - databasename, 1 - databaseversion
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE if not exists TrackGPS (" + // "TrackGPS" - tablename
					"_id INTEGER PRIMARY KEY autoincrement, altitude REAL, latitude REAL, longitude REAL, speed REAL, accuracy REAL, time INTEGER, " + 
					//"bearing REAL, elapsedRealtimeNanos INTEGER, currentTimeMillis INTEGER, " + 
					"idDiary INTEGER);");
			db.execSQL("CREATE TABLE if not exists Diary (" + // "Diary" - tablename
					"_id INTEGER PRIMARY KEY autoincrement, startTime INTEGER, movingTime INTEGER, restTime INTEGER, descent REAL, " + 
					"ascent REAL, maxSpeed REAL, avgSpeed REAL, maxAltitude REAL, minAltitude REAL, verticalDescent REAL, verticalAscent REAL, " +
					"status INTEGER);");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			//db.execSQL("DROP TABLE IF EXISTS TrackGPS"); // "TrackGPS" - tablename
			//db.execSQL("DROP TABLE IF EXISTS Diary"); // "Diary" - tablename
			//onCreate(db);
			//if (oldVersion == 1) {
			//	db.execSQL("ALTER TABLE `Diary` ADD COLUMN maxAltitude REAL");
			//	db.execSQL("ALTER TABLE `Diary` ADD COLUMN minAltitude REAL");
			//	db.execSQL("ALTER TABLE `Diary` ADD COLUMN verticalDescent REAL");
			//	db.execSQL("ALTER TABLE `Diary` ADD COLUMN verticalAscent REAL");
			 
			//}
		}
	}

	public DatabaseAdapter(Context ctx) {
		this.mCtx = ctx;
	}

	public DatabaseAdapter open() throws SQLException {
		mDbHelper = new DatabaseHelper(mCtx);
		mDb = mDbHelper.getWritableDatabase();
		return this;
	}

	public void close() {
		if (mDbHelper != null) {
			mDbHelper.close();
		}
	}

	public long newDiary() {
		ContentValues initialValues = new ContentValues();
		initialValues.put("startTime", System.currentTimeMillis());
		initialValues.put("movingTime", 0);
		initialValues.put("restTime", 0);
		initialValues.put("descent", 0);
		initialValues.put("ascent", 0);
		initialValues.put("maxSpeed", 0);
		initialValues.put("avgSpeed", 0);
		initialValues.put("status", 1);
		return mDb.insert("Diary", null, initialValues);
	}

	public Cursor fetchAllDiary() {

		Cursor mCursor = mDb
				.rawQuery(
						"SELECT _id, "
						+ "ROUND(maxSpeed,1) || ' km/h' As maxSpeed, "
						+ "ROUND(avgSpeed,1) || ' km/h' As avgSpeed, "
						+ "case when (descent+ascent) = 0.0 or (descent is null and ascent is null) then '0.000' when substr((descent+ascent),1,4) = '0.00' then '0.00' || substr(ROUND((descent+ascent),3) * 1000,1,1) when substr((descent+ascent),1,3) = '0.0' then '0.0' || substr(ROUND((descent+ascent),3) * 1000,length(ROUND((descent+ascent),3) * 1000)-4,3) when substr((descent+ascent),1,1) = '0' then '0.' || substr(ROUND((descent+ascent),3) * 1000,length(ROUND((descent+ascent),3) * 1000)-4,3) else '' || substr(ROUND((descent+ascent),3) * 1000,1,length(ROUND((descent+ascent),3) * 1000)-5) || '.' || substr(ROUND((descent+ascent),3) * 1000,length(ROUND((descent+ascent),3) * 1000)-4,3)  end|| ' km' As distance, "
						+ "strftime('%d.%m.%Y %H:%M:%S', startTime/1000, 'unixepoch', 'localtime') As startTime FROM Diary WHERE status=2 ORDER BY Diary.startTime DESC", null);

		if (mCursor != null) {
			mCursor.moveToFirst();
		}
		return mCursor;
	}

	public Cursor fetchGlobalDiary() {

		Cursor mCursor = mDb.rawQuery(
				"select sum(ascent + descent) as distance, "
				+ "count(1) as runs, "
				+ "sum(descent) as descent, "
				+ "max(maxSpeed) as maxSpeed, "
				+ "sum(descent)/(sum(descent/avgSpeed)) as avgSpeed "
				+ "from diary where status = 2;", null);

		if (mCursor != null) {
			mCursor.moveToFirst();
		}
		return mCursor;
	}

	public Cursor fetchAnalyseDiary(long idDiary) {

		Cursor mCursor = mDb
				.rawQuery(
						"SELECT _id, "
								+ "ROUND(maxSpeed,1) || ' km/h' As maxSpeed, "
								+ "ROUND(avgSpeed,1) || ' km/h' As avgSpeed, "
								+ "case when (descent+ascent) = 0.0 or (descent is null and ascent is null) then '0.000' when substr((descent+ascent),1,4) = '0.00' then '0.00' || substr(ROUND((descent+ascent),3) * 1000,1,1) when substr((descent+ascent),1,3) = '0.0' then '0.0' || substr(ROUND((descent+ascent),3) * 1000,length(ROUND((descent+ascent),3) * 1000)-4,3) when substr((descent+ascent),1,1) = '0' then '0.' || substr(ROUND((descent+ascent),3) * 1000,length(ROUND((descent+ascent),3) * 1000)-4,3) else '' || substr(ROUND((descent+ascent),3) * 1000,1,length(ROUND((descent+ascent),3) * 1000)-5) || '.' || substr(ROUND((descent+ascent),3) * 1000,length(ROUND((descent+ascent),3) * 1000)-4,3)  end|| ' km' As distance, "
								+ "descent, " //"case when descent = '0.0' then '0.000' when substr(descent,1,4) = '0.00' then '0.00' || substr(ROUND(descent,3) * 1000,1,1) when substr(descent,1,3) = '0.0' then '0.0' || substr(ROUND(descent,3) * 1000,length(ROUND(descent,3) * 1000)-4,3) when substr(descent,1,1) = '0' then '0.' || substr(ROUND(descent,3) * 1000,length(ROUND(descent,3) * 1000)-4,3) else '' || substr(ROUND(descent,3) * 1000,1,length(ROUND(descent,3) * 1000)-5) || '.' || substr(ROUND(descent,3) * 1000,length(ROUND(descent,3) * 1000)-4,3) end || ' km' As descent, "
								+ "case when ascent = '0.0' then '0.000' when substr(ascent,1,4) = '0.00' then '0.00' || substr(ROUND(ascent,3) * 1000,1,1) when substr(ascent,1,3) = '0.0' then '0.0' || substr(ROUND(ascent,3) * 1000,length(ROUND(ascent,3) * 1000)-4,3) when substr(ascent,1,1) = '0' then '0.' || substr(ROUND(ascent,3) * 1000,length(ROUND(ascent,3) * 1000)-4,3) else '' || substr(ROUND(ascent,3) * 1000,1,length(ROUND(ascent,3) * 1000)-5) || '.' || substr(ROUND(ascent,3) * 1000,length(ROUND(ascent,3) * 1000)-4,3) end || ' km' As ascent, "
								+ "case when length(movingTime/3600000) = 1 then '0' else '' end || substr(movingTime/3600000,1) || ':' || case when length((movingTime-movingTime/3600000*3600000)/60000) = 1 then '0' else '' end || substr((movingTime-movingTime/3600000*3600000)/60000,1) || ':' || case when length((movingTime-movingTime/3600000*3600000-(movingTime-movingTime/3600000*3600000)/60000*60000)/1000) = 1 then '0' else '' end || substr((movingTime-movingTime/3600000*3600000-(movingTime-movingTime/3600000*3600000)/60000*60000)/1000,1) as movingTime, "
								+ "case when length(restTime/3600000) = 1 then '0' else '' end || substr(restTime/3600000,1) || ':' || case when length((restTime-restTime/3600000*3600000)/60000) = 1 then '0' else '' end || substr((restTime-restTime/3600000*3600000)/60000,1) || ':' || case when length((restTime-restTime/3600000*3600000-(restTime-restTime/3600000*3600000)/60000*60000)/1000) = 1 then '0' else '' end || substr((restTime-restTime/3600000*3600000-(restTime-restTime/3600000*3600000)/60000*60000)/1000,1) as restTime, "
								+ "case when length((restTime+movingTime)/3600000) = 1 then '0' else '' end || substr((restTime+movingTime)/3600000,1) || ':' || case when length(((restTime+movingTime)-(restTime+movingTime)/3600000*3600000)/60000) = 1 then '0' else '' end || substr(((restTime+movingTime)-(restTime+movingTime)/3600000*3600000)/60000,1) || ':' || case when length(((restTime+movingTime)-(restTime+movingTime)/3600000*3600000-((restTime+movingTime)-(restTime+movingTime)/3600000*3600000)/60000*60000)/1000) = 1 then '0' else '' end || substr(((restTime+movingTime)-(restTime+movingTime)/3600000*3600000-((restTime+movingTime)-(restTime+movingTime)/3600000*3600000)/60000*60000)/1000,1) as duration, "
								+ "strftime('%d.%m.%Y %H:%M:%S', startTime/1000, 'unixepoch', 'localtime') As startTime,"
								+ "maxAltitude, "
								+ "minAltitude, "
								+ "verticalDescent, "
								+ "verticalAscent, "
								+ "verticalDescent + verticalAscent As vertical "
								+ "FROM Diary WHERE _id=" + idDiary, null);

		if (mCursor != null) {
			mCursor.moveToFirst();
		}
		return mCursor;
	}

	public int updateDiary(long id, long movingTime, long restTime, float descent, float ascent, float maxSpeed, float avgSpeed, double maxAltitude, double minAltitude, double verticalDescent, double verticalAscent) {
		ContentValues initialValues = new ContentValues();
		initialValues.put("movingTime", movingTime);
		initialValues.put("restTime", restTime);
		initialValues.put("descent", descent);
		initialValues.put("ascent", ascent);
		initialValues.put("maxSpeed", maxSpeed);
		initialValues.put("avgSpeed", avgSpeed);
		initialValues.put("maxAltitude", maxAltitude);
		initialValues.put("minAltitude", minAltitude);
		initialValues.put("verticalDescent", verticalDescent);
		initialValues.put("verticalAscent", verticalAscent);
		return mDb.update("Diary", initialValues, "_id = ?", new String[] { String.valueOf(id) });
	}
	
	public int updateDiaryStatus(long id, int status) {
		ContentValues initialValues = new ContentValues();
		initialValues.put("status", status);
		return mDb.update("Diary", initialValues, "_id = ?", new String[] { String.valueOf(id) });
	}

	public void deleteDiary(String id) {
		mDb.execSQL("DELETE FROM TrackGPS WHERE idDiary=" + id);
		mDb.execSQL("DELETE FROM Diary WHERE _id=" + id);
	}

	public long insertTrack(double altitude, double latitude, double longitude, float speed, float accuracy, long time, // long elapsedRealtimeNanos,
			 //float bearing, long currentTimeMillis, 
			 long idDiary) {
		
		ContentValues initialValues = new ContentValues();
		initialValues.put("altitude", altitude);
		initialValues.put("latitude", latitude);
		initialValues.put("longitude", longitude);
		initialValues.put("speed", speed);
		initialValues.put("accuracy", accuracy);
		initialValues.put("time", time);
		//initialValues.put("bearing", bearing);
		//initialValues.put("currentTimeMillis", currentTimeMillis);
		initialValues.put("idDiary", idDiary);
		try {
			return mDb.insert("TrackGPS", null, initialValues); // "TrackGPS" - tablename
		} catch (NullPointerException ex) {
			android.util.Log.e("TrackDbAdapter", "com.stibla.threedskitracker.DatabaseAdapter.insertTrack:" + ex.getMessage() + " idDiary:" + idDiary);
			return -1;
		}
	}

	public Cursor fetchTrack(long idDiary) {

		Cursor mCursor = mDb.rawQuery("SELECT _id, altitude, latitude, longitude, time " //, speed, accuracy " //cartX, cartY, cartZ "
			+ "FROM TrackGPS WHERE idDiary=" + idDiary + " ORDER BY time", null);
		if (mCursor != null) {
			mCursor.moveToFirst();
		}
		return mCursor;
	}
	
	public Cursor fetchTrackMinMax(long idDiary) {

		Cursor mCursor = mDb.rawQuery("select max(altitude) max_alt, min(altitude) min_alt, "
				+ "max(latitude) max_lat, min(latitude) min_lat, "
				+ "max(longitude) max_lon, min(longitude) min_lon "
				+ "FROM TrackGPS WHERE idDiary=" + idDiary, null);
		if (mCursor != null) {
			mCursor.moveToFirst();
		}
		return mCursor;
	}
	
	public void cleanDB() {
		mDb.execSQL("DELETE FROM Diary WHERE status<>2");
		mDb.execSQL("DELETE FROM TrackGPS WHERE idDiary NOT IN (SELECT _id FROM Diary)");		
	}

}
