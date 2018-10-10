package com.stibla.threedskitracker;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.opengl.GLSurfaceView.Renderer;
import android.opengl.GLU;

public class OpenGLRenderer implements Renderer
{
	private Context context;
	private TrackLine line;
	private Ground ground;
	private OpenGLText textStart;
	private OpenGLText textEnd;
	private OpenGLText textMaxSpeed;
	
    public OpenGLRenderer(Context context)
    {
    	this.context = context;  
    	if(ActivityMain.idDiary == 0) {
			SharedPreferences prefs = context.getApplicationContext().getSharedPreferences("com.stibla.threedskitracker", Context.MODE_PRIVATE);			
			ActivityMain.idDiary = prefs.getLong("com.stibla.threedskitracker.idDiary",0);
		}
    	ground = new Ground(context);
    	line = new TrackLine(context);
    	//android.util.Log.w("TrackDbAdapter", "OpenGLRenderer");
    }
    
    @Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);  // Set color's clear-value to black
		//android.util.Log.w("TrackDbAdapter", "onSurfaceCreated");
		textStart = new OpenGLText(gl, this.context, "START", Color.BLACK, Color.GREEN, line.startX, line.startY, line.startZ);
		textEnd = new OpenGLText(gl, this.context, "END", Color.BLACK, Color.RED, line.endX, line.endY, line.endZ);
		textMaxSpeed = new OpenGLText(gl, this.context, "MAX SPEED", Color.BLACK, Color.CYAN, line.maxSpeedX, line.maxSpeedY, line.maxSpeedZ);
		
	    gl.glClearDepthf(1.0f);            // Set depth's clear-value to farthest
	    gl.glEnable(GL10.GL_DEPTH_TEST);   // Enables depth-buffer for hidden surface removal
	    gl.glDepthFunc(GL10.GL_LEQUAL);    // The type of depth testing to do
		
		gl.glEnable(GL10.GL_BLEND);
	    gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
	    
	    gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_NICEST);  // nice perspective view
	    gl.glShadeModel(GL10.GL_SMOOTH);   // Enable smooth shading of color

	}
    
	@Override
    public void onDrawFrame(GL10 gl) {
		// Clear color and depth buffers using clear-value set earlier
		gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
	    
	    gl.glLoadIdentity();                 // Reset model-view matrix ( NEW )	    
	    gl.glTranslatef(ActivityMain.translateX, ActivityMain.translateY, ActivityMain.increase);	   
		gl.glRotatef(ActivityMain.angleCubeX, 0.0f, -1.0f, 0.0f); 
	    gl.glRotatef(ActivityMain.angleCubeY, -1.0f, 0.0f, 0.0f);

	    line.draw(gl);
	    ground.draw(gl);
 
        textStart.draw(gl);	
        textEnd.draw(gl);	
        textMaxSpeed.draw(gl);

	}

	public void onSurfaceChanged(GL10 gl, int width, int height) {
		//android.util.Log.w("TrackDbAdapter", width + "x" + height);
		ActivityMain.width = width;
		ActivityMain.height = height;
		if (height == 0) height = 1;   // To prevent divide by zero
	    float aspect = (float)width / height;

	    // Set the viewport (display area) to cover the entire window
	    gl.glViewport(0, 0, width, height);
	  
	    // Setup perspective projection, with aspect ratio matches viewport
	    gl.glMatrixMode(GL10.GL_PROJECTION); // Select projection matrix
	    gl.glLoadIdentity();                 // Reset projection matrix
	    // Use perspective projection
	    GLU.gluPerspective(gl, 45, aspect, 0.1f, 100.f);
	  
	    gl.glMatrixMode(GL10.GL_MODELVIEW);  // Select model-view matrix
	    gl.glLoadIdentity();                 // Reset
	    
	}
	
}
