package com.stibla.threedskitracker;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.database.Cursor;

public class TrackLine {
	private FloatBuffer vertexBuffer;
	
	private float speed[];
	private float maxSpeed;
	public float startX;
	public float startY;
	public float startZ;
	public float endX;
	public float endY;
	public float endZ;
	public float maxSpeedX;
	public float maxSpeedY;
	public float maxSpeedZ;

	public TrackLine(Context context) {
		//this.context = context;
		ActivityMain.angleCubeX = 0.0f;    
		ActivityMain.angleCubeY = 0.0f;  
		ActivityMain.increase = -3.2f;
		ActivityMain.translateX = 0.0f;
		ActivityMain.translateY = 0.0f;
		maxSpeed = 0.0f;
		
		DatabaseAdapter dbHelper = new DatabaseAdapter(context);
		dbHelper.open();
		Cursor cursor = dbHelper.fetchTrackMinMax(ActivityMain.idDiary);
						
		double max_lat = cursor.getDouble(cursor.getColumnIndex("max_lat"));
		double min_lat = cursor.getDouble(cursor.getColumnIndex("min_lat"));
		double max_lon = cursor.getDouble(cursor.getColumnIndex("max_lon"));
		double min_lon = cursor.getDouble(cursor.getColumnIndex("min_lon"));
			
		double avgLat = (max_lat + min_lat) / 2;
		double avgLon = (max_lon + min_lon) / 2;
		
		cursor = dbHelper.fetchTrack(ActivityMain.idDiary);
		ActivityMain.GLsizei = cursor.getCount();
		if(ActivityMain.GLsizei==0) return;
		float[] coordsX = new float[ActivityMain.GLsizei];
		float[] coordsY = new float[ActivityMain.GLsizei];
		float[] coordsZ = new float[ActivityMain.GLsizei];
		speed = new float[ActivityMain.GLsizei];
		int index = 0;
		double coordX, coordY, coordZ, coordX1rot, coordY1rot, coordZ1rot, coordX2rot, coordY2rot, coordZ2rot, coordXmax, coordYmax, coordZmax, coordXmin, coordYmin, coordZmin;
		double prevX, prevY, prevZ, prevAlt;
		prevX = prevY = prevZ = prevAlt = 0.0f;
		int prevTime;
		prevTime = 0;
		coordXmax = coordYmax = coordZmax = -1 * Double.MAX_VALUE;
		coordXmin = coordYmin = coordZmin = Double.MAX_VALUE;
		do {
			coordX = ((ActivityMain.RADIUS_OF_EARTH + cursor.getFloat(cursor.getColumnIndex("altitude"))) * java.lang.Math.sin((90 - cursor.getFloat(cursor.getColumnIndex("latitude"))) * java.lang.Math.PI / 180) * java.lang.Math.cos(cursor.getFloat(cursor.getColumnIndex("longitude")) * java.lang.Math.PI / 180));
			coordY = ((ActivityMain.RADIUS_OF_EARTH + cursor.getFloat(cursor.getColumnIndex("altitude"))) * java.lang.Math.sin((90 - cursor.getFloat(cursor.getColumnIndex("latitude"))) * java.lang.Math.PI / 180) * java.lang.Math.sin(cursor.getFloat(cursor.getColumnIndex("longitude")) * java.lang.Math.PI / 180));
			coordZ = ((ActivityMain.RADIUS_OF_EARTH + cursor.getFloat(cursor.getColumnIndex("altitude"))) * java.lang.Math.cos((90 - cursor.getFloat(cursor.getColumnIndex("latitude"))) * java.lang.Math.PI / 180));
			coordX1rot = coordX * java.lang.Math.cos(-1 * avgLon * java.lang.Math.PI / 180) - coordY * java.lang.Math.sin(-1 * avgLon * java.lang.Math.PI / 180);
			coordY1rot = coordX * java.lang.Math.sin(-1 * avgLon * java.lang.Math.PI / 180) + coordY * java.lang.Math.cos(-1 * avgLon * java.lang.Math.PI / 180);
			coordZ1rot = coordZ;
			
			coordX2rot =   coordX1rot * java.lang.Math.cos(-1 * (90 - avgLat) * java.lang.Math.PI / 180) + coordZ1rot * java.lang.Math.sin(-1 * (90 - avgLat) * java.lang.Math.PI / 180);//coordY1rot;
			coordY2rot = coordY1rot;
			coordZ2rot = - coordX1rot * java.lang.Math.sin(-1 * (90 - avgLat) * java.lang.Math.PI / 180) + coordZ1rot * java.lang.Math.cos(-1 * (90 - avgLat) * java.lang.Math.PI / 180);
					
			coordXmin = coordXmin > coordX2rot ? coordX2rot : coordXmin;
			coordYmin = coordYmin > coordY2rot ? coordY2rot : coordYmin;
			coordZmin = coordZmin > coordZ2rot ? coordZ2rot : coordZmin;
			coordXmax = coordXmax < coordX2rot ? coordX2rot : coordXmax;
			coordYmax = coordYmax < coordY2rot ? coordY2rot : coordYmax;
			coordZmax = coordZmax < coordZ2rot ? coordZ2rot : coordZmax;
			
			if(index > 0) {
				double time = cursor.getInt(cursor.getColumnIndex("time")) - prevTime;
				double distance = java.lang.Math.sqrt(java.lang.Math.pow(prevX - coordX2rot,2) + java.lang.Math.pow(prevY - coordY2rot,2) + java.lang.Math.pow(prevZ - coordZ2rot,2)) / 1000.0f;
				
				speed[index] = 0;
				if(prevAlt < cursor.getFloat(cursor.getColumnIndex("altitude"))) {
					
					if (time > 0) {
						speed[index] = (float) -(distance / (time / 3600000.0f));
					}					
				} else {
					if (time > 0) {
						speed[index] = (float) (distance / (time / 3600000.0f));
						if (maxSpeed < speed[index]) maxSpeed = speed[index];
					}
				}				 
			} 
			
			coordsX[index] = (float) coordX2rot;
			coordsY[index] = (float) coordY2rot;
			coordsZ[index++] = (float) coordZ2rot;
			
			prevX = coordX2rot;
			prevY = coordY2rot;
			prevZ = coordZ2rot;
			prevAlt = cursor.getFloat(cursor.getColumnIndex("altitude"));
			prevTime = cursor.getInt(cursor.getColumnIndex("time"));
		} while (cursor.moveToNext());
		
		cursor.close();
		dbHelper.close();
		
		double relation, relationZ;
		relation = coordYmax - coordYmin;
		if(relation < coordXmax - coordXmin) relation = coordXmax - coordXmin;
		if(relation < coordZmax - coordZmin) relation = coordZmax - coordZmin;	
		relationZ = relation;
		if(coordZmax - coordZmin < relationZ / 2) 
			relationZ /= 2;
			else 
				if(coordZmax - coordZmin > relationZ / 2 && coordZmax - coordZmin < relationZ) 
					relationZ = coordZmax - coordZmin;
		
		index = 0;
		
		
		float[] vertices = new float[ActivityMain.GLsizei * 3];		
		for(int i = 0;i < ActivityMain.GLsizei;i++) {
			vertices[index++] = (float) ((coordsY[i] - coordYmin + (relation - (coordYmax - coordYmin)) / 2) / (relation)) * 2 - 1; //(float) ((ActivityMain.RADIUS_OF_EARTH + cursor.getFloat(cursor.getColumnIndex("altitude"))) * java.lang.Math.sin((((cursor.getFloat(cursor.getColumnIndex("latitude")) - avgLat))) * java.lang.Math.PI / 180) * java.lang.Math.sin((cursor.getFloat(cursor.getColumnIndex("longitude")) - avgLon) * java.lang.Math.PI / 180)) / (max_cartY > max_cartX ? max_cartY : max_cartX); // (cursor.getFloat(cursor.getColumnIndex("cartY")) - min_cartY) / (relationship);// * 2 - 1;
			if(i == 0) startX = vertices[index - 1];
			if(i == ActivityMain.GLsizei - 1) endX = vertices[index - 1];
			if(speed[i] == maxSpeed) maxSpeedX = vertices[index - 1];
			vertices[index++] = (float) ((coordsZ[i] - coordZmin) / (relationZ)) * 2 - 1; //(float) ((cursor.getFloat(cursor.getColumnIndex("altitude")) - min_alt) / (max_alt - min_alt)); // (cursor.getFloat(cursor.getColumnIndex("cartZ")) - min_cartZ) / (relationship);// * 2 - 1;
			if(i == 0) startY = vertices[index - 1];
			if(i == ActivityMain.GLsizei - 1) endY = vertices[index - 1];
			if(speed[i] == maxSpeed) maxSpeedY = vertices[index - 1];
			vertices[index++] = (float) ((coordsX[i] - coordXmin + (relation - (coordXmax - coordXmin)) / 2) / (relation)) * 2 - 1; //(float) ((ActivityMain.RADIUS_OF_EARTH + cursor.getFloat(cursor.getColumnIndex("altitude"))) * java.lang.Math.sin((((cursor.getFloat(cursor.getColumnIndex("latitude")) - avgLat))) * java.lang.Math.PI / 180) * java.lang.Math.cos((cursor.getFloat(cursor.getColumnIndex("longitude")) - avgLon) * java.lang.Math.PI / 180)) / (max_cartY > max_cartX ? max_cartY : max_cartX); // (cursor.getFloat(cursor.getColumnIndex("cartX")) - min_cartX) / (relationship);// * 2 - 1;
			if(i == 0) startZ = vertices[index - 1];	
			if(i == ActivityMain.GLsizei - 1) endZ = vertices[index - 1];
			if(speed[i] == maxSpeed) maxSpeedZ = vertices[index - 1];
		} 
		
		/*coordX = coordY = coordZ = 0;
		prevX = prevY = prevZ = 0;
		double prevXplus, prevYplus, prevXminus, prevYminus;
		prevXplus = prevYplus = prevXminus = prevYminus = 0;
		double distanceYX = 0;
		float[] vertices = new float[ActivityMain.GLsizei * 3 * 4];		
		for(int i = 0;i < ActivityMain.GLsizei - 1;i++) {
			coordX = (float) ((coordsX[i + 1] - coordXmin + (relation - (coordXmax - coordXmin)) / 2) / (relation)) * 2 - 1; //(float) ((ActivityMain.RADIUS_OF_EARTH + cursor.getFloat(cursor.getColumnIndex("altitude"))) * java.lang.Math.sin((((cursor.getFloat(cursor.getColumnIndex("latitude")) - avgLat))) * java.lang.Math.PI / 180) * java.lang.Math.cos((cursor.getFloat(cursor.getColumnIndex("longitude")) - avgLon) * java.lang.Math.PI / 180)) / (max_cartY > max_cartX ? max_cartY : max_cartX); // (cursor.getFloat(cursor.getColumnIndex("cartX")) - min_cartX) / (relationship);// * 2 - 1;
			coordY = (float) ((coordsY[i + 1] - coordYmin + (relation - (coordYmax - coordYmin)) / 2) / (relation)) * 2 - 1; //(float) ((ActivityMain.RADIUS_OF_EARTH + cursor.getFloat(cursor.getColumnIndex("altitude"))) * java.lang.Math.sin((((cursor.getFloat(cursor.getColumnIndex("latitude")) - avgLat))) * java.lang.Math.PI / 180) * java.lang.Math.sin((cursor.getFloat(cursor.getColumnIndex("longitude")) - avgLon) * java.lang.Math.PI / 180)) / (max_cartY > max_cartX ? max_cartY : max_cartX); // (cursor.getFloat(cursor.getColumnIndex("cartY")) - min_cartY) / (relationship);// * 2 - 1; 
			coordZ = (float) ((coordsZ[i + 1] - coordZmin) / (relationZ)) * 2 - 1; //(float) ((cursor.getFloat(cursor.getColumnIndex("altitude")) - min_alt) / (max_alt - min_alt)); // (cursor.getFloat(cursor.getColumnIndex("cartZ")) - min_cartZ) / (relationship);// * 2 - 1; 
			
			prevX = (float) ((coordsX[i] - coordXmin + (relation - (coordXmax - coordXmin)) / 2) / (relation)) * 2 - 1;
			prevY = (float) ((coordsY[i] - coordYmin + (relation - (coordYmax - coordYmin)) / 2) / (relation)) * 2 - 1;
			prevZ = (float) ((coordsZ[i] - coordZmin) / (relationZ)) * 2 - 1;			
			
			distanceYX = java.lang.Math.sqrt(java.lang.Math.pow(prevX - coordX,2) + java.lang.Math.pow(prevY - coordY,2));
			
			vertices[index++] = (float) (coordY + ((coordY - prevY) / distanceYX) * ActivityMain.TRACK_TRIANGLE_WIDTH);			
			vertices[index++] = (float) coordZ;			
			vertices[index++] = (float) (coordX + ((coordX - prevX) / distanceYX) * ActivityMain.TRACK_TRIANGLE_WIDTH);

			if(i == 0) {
				vertices[index++] = (float) (prevY + ((coordY - prevY) / distanceYX) * ActivityMain.TRACK_TRIANGLE_WIDTH);			
				vertices[index++] = (float) prevZ;			
				vertices[index++] = (float) (prevX + ((coordX - prevX) / distanceYX) * ActivityMain.TRACK_TRIANGLE_WIDTH);
							
				vertices[index++] = (float) (prevY - ((coordY - prevY) / distanceYX) * ActivityMain.TRACK_TRIANGLE_WIDTH);			
				vertices[index++] = (float) prevZ;			
				vertices[index++] = (float) (prevX - ((coordX - prevX) / distanceYX) * ActivityMain.TRACK_TRIANGLE_WIDTH);
			} else {
				vertices[index++] = (float) prevYplus;			
				vertices[index++] = (float) prevZ;			
				vertices[index++] = (float) prevXplus;
							
				vertices[index++] = (float) prevYminus;			
				vertices[index++] = (float) prevZ;			
				vertices[index++] = (float) prevXminus;

			}
				
			vertices[index++] = (float) (coordY - ((coordY - prevY) / distanceYX) * ActivityMain.TRACK_TRIANGLE_WIDTH);			
			vertices[index++] = (float) coordZ;			
			vertices[index++] = (float) (coordX - ((coordX - prevX) / distanceYX) * ActivityMain.TRACK_TRIANGLE_WIDTH);

			prevYplus = (float) (coordY + ((coordY - prevY) / distanceYX) * ActivityMain.TRACK_TRIANGLE_WIDTH);			
			prevXplus = (float) (coordX + ((coordX - prevX) / distanceYX) * ActivityMain.TRACK_TRIANGLE_WIDTH);
						
			prevYminus = (float) (coordY - ((coordY - prevY) / distanceYX) * ActivityMain.TRACK_TRIANGLE_WIDTH);			
			prevXminus = (float) (coordX - ((coordX - prevX) / distanceYX) * ActivityMain.TRACK_TRIANGLE_WIDTH);
			
			if(i == 0) startX = (float) prevX;
			if(i == ActivityMain.GLsizei - 1) endX = (float) prevX;
			if(speed[i] == maxSpeed) maxSpeedX = (float) prevX;
			
			if(i == 0) startY = (float) prevY;
			if(i == ActivityMain.GLsizei - 1) endY = (float) prevY;
			if(speed[i] == maxSpeed) maxSpeedY = (float) prevY;
			
			if(i == 0) startZ = (float) prevZ;	
			if(i == ActivityMain.GLsizei - 1) endZ = (float) prevZ;
			if(speed[i] == maxSpeed) maxSpeedZ = (float) prevZ;

		} */
		
		ByteBuffer vbb = ByteBuffer.allocateDirect(vertices.length * Integer.SIZE/Byte.SIZE);
		vbb.order(ByteOrder.nativeOrder()); // Use native byte order
		vertexBuffer = vbb.asFloatBuffer(); // Convert from byte to float
		vertexBuffer.put(vertices); // Copy data into buffer
		vertexBuffer.position(0);
	}

	public void draw(GL10 gl) {
		if(vertexBuffer == null) return;
		//gl.glFrontFace(GL10.GL_CCW); // Front face in counter-clockwise
		//gl.glEnable(GL10.GL_CULL_FACE); // Enable cull face
		//gl.glCullFace(GL10.GL_BACK); // Cull the back face (don't display)
		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertexBuffer);

		for(int i = 0;i < ActivityMain.GLsizei - 1;i++) {
				if(speed[i] > 0 /*maxSpeed * 0.75f*/) {
					//gl.glColor4f(0.1f + (speed[i] > ActivityMain.MAX_SPEED * 0.9f ? ActivityMain.MAX_SPEED : speed[i])/ActivityMain.MAX_SPEED, 0.0f, 0.0f, 1f);
					gl.glColor4f(1.0f, 1.0f - speed[i]/maxSpeed, 1.0f - speed[i]/maxSpeed, 1.0f);
				} else {
					gl.glColor4f(1.0f - speed[i]/maxSpeed, 1.0f, 1.0f - speed[i]/maxSpeed, 1.0f);
				}
				if(ActivityMain.IsFullScreen) {
					if(i>=ActivityMain.RangeSeekMin && i<=ActivityMain.RangeSeekMax) {
						gl.glLineWidth(ActivityMain.TRACK_LINE_WIDTH_SELECTED);
					} else {
						gl.glLineWidth(ActivityMain.TRACK_LINE_WIDTH_NON_SELECTED);
					}
				} else {
					gl.glLineWidth(ActivityMain.TRACK_LINE_WIDTH);
				}

				//gl.glDrawArrays(GL10.GL_TRIANGLE_FAN, i * 4, 4);
				gl.glDrawArrays(GL10.GL_LINES, i, 2);
			} 
		gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glDisable(GL10.GL_CULL_FACE);
	}
	
}
