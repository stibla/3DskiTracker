package com.stibla.threedskitracker;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import javax.microedition.khronos.opengles.GL10;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.opengl.GLUtils;

public class OpenGLText {
	
	private FloatBuffer vertexBuffer;	// buffer holding the vertices
	private FloatBuffer textureBuffer;	// buffer holding the texture coordinates
	private float ratio;
	private int[] textures = new int[1]; /** The texture pointer */
	private float posX;
	private float posY;
	private float posZ;

	public OpenGLText(GL10 gl, Context context, String text, int textColor, int backColor, float x, float y, float z) {
		posX = x; posY = y; posZ = z;
		
		// loading texture
		Bitmap bitmap = textAsBitmap(text, ActivityMain.TEXT_SIZE, textColor, backColor);

		// generate one texture pointer
		gl.glGenTextures(1, textures, 0);
		// ...and bind it to our array
		gl.glBindTexture(GL10.GL_TEXTURE_2D, textures[0]);
		
		// create nearest filtered texture
		gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
		gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);

		//Different possible texture parameters, e.g. GL10.GL_CLAMP_TO_EDGE
		//gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_REPEAT);
		//gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_REPEAT);
		
		// Use Android GLUtils to specify a two-dimensional texture image from our bitmap 
		GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmap, 0);
		
		// Clean up
		bitmap.recycle();
		
		float vertices[] = {
				 posX,  																		posY,  																	posZ,		// V1 - bottom left
				 posX,  																		posY + ActivityMain.TEXT_HEIGHT, 	posZ,  		// V2 - top left
				 posX + ActivityMain.TEXT_HEIGHT * ratio,  posY,  																	posZ,		// V3 - bottom right
				 posX + ActivityMain.TEXT_HEIGHT * ratio,  posY + ActivityMain.TEXT_HEIGHT, 	posZ  	    // V4 - top right
		};
		
		float texture[] = {    		
				// Mapping coordinates for the vertices
				0.0f, 1.0f,		// top left		(V2)
				0.0f, 0.0f,		// bottom left	(V1)
				1.0f, 1.0f,		// top right	(V4)
				1.0f, 0.0f		// bottom right	(V3)
		};
		
		// a float has 4 bytes so we allocate for each coordinate 4 bytes
		ByteBuffer byteBuffer = ByteBuffer.allocateDirect(vertices.length * 4);
		byteBuffer.order(ByteOrder.nativeOrder());
		vertexBuffer = byteBuffer.asFloatBuffer(); // allocates the memory from the byte buffer
		vertexBuffer.put(vertices); // fill the vertexBuffer with the vertices
		vertexBuffer.position(0); // set the cursor position to the beginning of the buffer
		
		byteBuffer = ByteBuffer.allocateDirect(texture.length * 4);
		byteBuffer.order(ByteOrder.nativeOrder());
		textureBuffer = byteBuffer.asFloatBuffer();
		textureBuffer.put(texture);
		textureBuffer.position(0);
	}

	public Bitmap textAsBitmap(String text, float textSize, int textColor, int backColor) {
	    Paint paint = new Paint();
	    paint.setTextSize(textSize);
	    paint.setColor(textColor);
	    paint.setTextAlign(Paint.Align.LEFT);
	    paint.setFlags(Paint.FAKE_BOLD_TEXT_FLAG );
	    int width = (int) (paint.measureText(text) /*+ 0.5f*/); // round
	    float baseline = (int) (-paint.ascent() /*+ 0.5f*/); // ascent() is negative
	    int height = (int) (baseline + paint.descent() /*+ 0.5f*/);
	    if (height == 0) height = 1; 
	    ratio = (float)width / height;
	    Bitmap image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
	    Canvas canvas = new Canvas(image);
	    canvas.drawColor(backColor);
	    canvas.drawText(text, 0, baseline, paint);
	    return image;
	}
		
	/** The draw method for the square with the GL context */
	public void draw(GL10 gl) {
		// bind the previously generated texture
		gl.glBindTexture(GL10.GL_TEXTURE_2D, textures[0]);
		
		// Point to our buffers
		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
		gl.glEnable(GL10.GL_TEXTURE_2D);   //Enable Texture Mapping
        
		// Set the face rotation
		gl.glFrontFace(GL10.GL_CW);
		
		// Point to our vertex buffer
		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertexBuffer);
		gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, textureBuffer);
		
		// Draw the vertices as triangle strip
		gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, 4);

		//Disable the client state before leaving
		gl.glDisable(GL10.GL_TEXTURE_2D);
		gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
	}
}
