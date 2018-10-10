package com.stibla.threedskitracker;

import android.annotation.SuppressLint;
import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.MotionEvent;


public class OpenGLView extends GLSurfaceView {
	
	private static float diffMultiTouch = 0.0f;
	private static float prevTouchX = 0.0f;
	private static float prevTouchY = 0.0f;

    public OpenGLView(Context context)
    {
        this(context, null);
    } 

    public OpenGLView(Context context, AttributeSet attrs)
    {
        this(context, attrs, 0);
    }

    public OpenGLView(Context context, AttributeSet attrs, int defStyle)
    {
    	super(context, attrs);

        // Tell EGL to use a ES 2.0 Context
        //setEGLContextClientVersion(2);

       // Set the renderer
        if (!isInEditMode()) {
        	setRenderer(new OpenGLRenderer(context));
        }
        
    }
    
    @SuppressLint("ClickableViewAccessibility")
   	@Override
	public boolean onTouchEvent(MotionEvent event) {
		try {
			if (event.getPointerCount() > 1) {
				if (event.getPointerCount() == 2) {
					float diffMultiTouchNow = (float) java.lang.Math.sqrt((event.getX(event.getPointerId(0)) - event.getX(event.getPointerId(1))) * (event.getX(event.getPointerId(0)) - event.getX(event.getPointerId(1))) + (event.getY(event.getPointerId(0)) - event.getY(event.getPointerId(1))) * (event.getY(event.getPointerId(0)) - event.getY(event.getPointerId(1))));
					if (diffMultiTouch < diffMultiTouchNow)
						ActivityMain.increase /= ActivityMain.ZOOM_SPEED;
					if (diffMultiTouch > diffMultiTouchNow)
						ActivityMain.increase *= ActivityMain.ZOOM_SPEED;

					diffMultiTouch = diffMultiTouchNow;
				}
			} else {
				switch (event.getAction()) {

				case MotionEvent.ACTION_DOWN:
					prevTouchX = event.getX();
					prevTouchY = event.getY();
					break;
				case MotionEvent.ACTION_UP:
					break;
				case MotionEvent.ACTION_MOVE:
					if (ActivityMain.motionOrRotation == 1) {
						ActivityMain.angleCubeX += (prevTouchX - event.getX()) / ActivityMain.ROTATION_SPEED;
						ActivityMain.angleCubeY += (prevTouchY - event.getY()) / ActivityMain.ROTATION_SPEED; 
					} else {
						ActivityMain.translateX -= (prevTouchX - event.getX()) / ActivityMain.width;
						ActivityMain.translateY += (prevTouchY - event.getY()) / ActivityMain.height;
						// android.util.Log.w("TrackDbAdapter", event.getAction() + ":" + ActivityMain.translateX + "x" + ActivityMain.translateY);
						// android.util.Log.w("TrackDbAdapter", ActivityMain.prevMultiTouchX1 + "x" + ActivityMain.prevMultiTouchY1);
					}
					prevTouchX = event.getX();
					prevTouchY = event.getY();
					break;
				default:
					break;
				}
			}
		} catch (IllegalArgumentException ex) {
			android.util.Log.e("TrackDbAdapter", "com.stibla.threedskitracker.ActivityTrackDetail.onTouchEvent:" + ex.getMessage());
		}

		// return false;
		return true;

	}

}
