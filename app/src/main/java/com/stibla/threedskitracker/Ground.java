package com.stibla.threedskitracker;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;

public class Ground {
	private FloatBuffer vertexBuffer;
	//private Context context;
	
	public Ground(Context context) {
		int index = 0;
		//this.context = context;
		float divisor = ActivityMain.NO_OF_GROUND_LINE / 2.0f; //6.0f;
		float[] vertices = new float[(ActivityMain.NO_OF_GROUND_LINE - 1) * 12];		
		for(int i = -(ActivityMain.NO_OF_GROUND_LINE - 2) / 2;i <= (ActivityMain.NO_OF_GROUND_LINE - 2) / 2;i++) {
			vertices[index++] = i/divisor; //doprava dolava
			vertices[index++] = -1.0f;  //hore dole
			vertices[index++] = -1.0f;  //dopredu dozadu
			vertices[index++] = i/divisor;
			vertices[index++] = -1.0f;
			vertices[index++] = 1.0f;
			
			vertices[index++] = -1.0f;
			vertices[index++] = -1.0f;
			vertices[index++] = i/divisor;
			vertices[index++] = 1.0f;
			vertices[index++] = -1.0f;
			vertices[index++] = i/divisor;
			
			/*vertices[index++] = i/divisor;
			vertices[index++] = 1.0f;
			vertices[index++] = -1.0f;
			vertices[index++] = i/divisor;
			vertices[index++] = 1.0f;
			vertices[index++] = 1.0f;
			
			vertices[index++] = -1.0f;
			vertices[index++] = 1.0f;
			vertices[index++] = i/divisor;
			vertices[index++] = 1.0f;
			vertices[index++] = 1.0f;
			vertices[index++] = i/divisor;*/
		} 
		
		//android.util.Log.w("TrackDbAdapter", "vertices.length:" + vertices.length + Integer.SIZE/Byte.SIZE);
		ByteBuffer vbb = ByteBuffer.allocateDirect(vertices.length * Integer.SIZE/Byte.SIZE);
		vbb.order(ByteOrder.nativeOrder()); // Use native byte order
		vertexBuffer = vbb.asFloatBuffer(); // Convert from byte to float
		vertexBuffer.put(vertices); // Copy data into buffer
		vertexBuffer.position(0);
	}
	
	public void draw(GL10 gl) {
		//gl.glFrontFace(GL10.GL_CCW); // Front face in counter-clockwise
		//gl.glEnable(GL10.GL_CULL_FACE); // Enable cull face
		//gl.glCullFace(GL10.GL_BACK); // Cull the back face (don't display)
		
		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertexBuffer);

		// Set the color for each of the faces
		gl.glColor4f(1f, 1f, 1f, 1f);
		// Draw the primitive from the vertex-array directly
		gl.glLineWidth(ActivityMain.GROUND_LINE_WIDTH);
		gl.glDrawArrays(GL10.GL_LINES, 0, (ActivityMain.NO_OF_GROUND_LINE - 1) * 4);
		gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glDisable(GL10.GL_CULL_FACE);
		
	}
}
