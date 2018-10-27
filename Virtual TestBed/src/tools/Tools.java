package tools;

import static java.lang.Math.toRadians;

import java.awt.Color;
import java.math.BigDecimal;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import entities.Camera;
import entities.Drone;
import interfaces.AutopilotInputs;

public class Tools {

	public static Vector3f HSVtoRGB(float hue, float saturation, float value) {
	    int rgb = Color.HSBtoRGB(hue, saturation, value);
	    float red = (float) (((rgb >> 16) & 0xFF)/255.0);
	    float green = (float) (((rgb >> 8) & 0xFF)/255.0);
	    float blue = (float) ((rgb & 0xFF)/255.0);
	    return new Vector3f(red, green, blue);
	}
	
	public static float[] getCubeVertices(float width,float height,float length){
		float[] result={
				-width/2,height/2,length/2,	
				-width/2,-height/2,length/2,	//front positieve Z 70
				width/2,-height/2,length/2,	
				width/2,height/2,length/2,		
				
				width/2,height/2,-length/2,	
				width/2,-height/2,-length/2,	//back negatieve Z 45
				-width/2,-height/2,-length/2,	
				-width/2,height/2,-length/2,
				
				-width/2,height/2,-length/2,	
				-width/2,-height/2,-length/2,	//left	negatieve X 30
				-width/2,-height/2,length/2,	
				-width/2,height/2,length/2,
				
				width/2,height/2,length/2,	
				width/2,-height/2,length/2,		//right	positieve X 85
				width/2,-height/2,-length/2,	
				width/2,height/2,-length/2,
				
				-width/2,height/2,-length/2,
				-width/2,height/2,length/2,		//top positieve Y 100
				width/2,height/2,length/2,
				width/2,height/2,-length/2,
				
				-width/2,-height/2,length/2,
				-width/2,-height/2,-length/2,	//bottom negatieve Y 15
				width/2,-height/2,-length/2,
				width/2,-height/2,length/2	
		};
		return result;
	}
	
	public static float[] calculateCubeColorValues() {
		float[] values = {.7f, .7f, .7f, .7f,
						  .45f, .45f, .45f, .45f,
						  .30f, .30f, .30f, .30f,
						  .85f, .85f, .85f, .85f,
						  1.00f, 1.00f, 1.00f, 1.00f,
						  .15f, .15f, .15f, .15f};
		return values;
	}
	
	public static Matrix4f createViewMatrix(Camera camera){
		  Matrix4f viewMatrix = new Matrix4f();
		  viewMatrix.setIdentity();
		  Matrix4f.rotate((float) -camera.getRoll(), new Vector3f(0,0,1), viewMatrix,
		    viewMatrix);
		  Matrix4f.rotate((float) -camera.getPitch(), new Vector3f(1,0,0), viewMatrix,
		    viewMatrix);
		  Matrix4f.rotate((float) -camera.getHeading(), new Vector3f(0,1,0), viewMatrix,
				    viewMatrix);
		  Vector3f cameraPos = camera.getPosition();
		  Vector3f negativeCameraPos = new Vector3f(-cameraPos.x,-cameraPos.y,-cameraPos.z);
		  Matrix4f.translate(negativeCameraPos, viewMatrix, viewMatrix);
		  return viewMatrix;
	}
	
	public static Matrix4f createViewMatrixNoTranslate(Camera camera){
		  Matrix4f viewMatrix = new Matrix4f();
		  viewMatrix.setIdentity();
		  Matrix4f.rotate((float) -camera.getRoll(), new Vector3f(0,0,1), viewMatrix,
		    viewMatrix);
		  Matrix4f.rotate((float) -camera.getPitch(), new Vector3f(1,0,0), viewMatrix,
		    viewMatrix);
		  Matrix4f.rotate((float) -camera.getHeading(), new Vector3f(0,1,0), viewMatrix,
				    viewMatrix);
		  return viewMatrix;
	}
	
	public static Vector3f transformVector(Matrix4f matrix, Vector3f vector) {
		Vector4f vector4 = new Vector4f(vector.x, vector.y, vector.z, 1);
		Matrix4f.transform(matrix, vector4, vector4);
		return new Vector3f(vector4.x, vector4.y, vector4.z);
	}

	public static Vector3f addVectors(Vector3f... vectors) {
		Vector3f result = new Vector3f();
		for(Vector3f vector: vectors) {
			Vector3f.add(result, vector, result);
		}
		return result;
	}
	
	public static Vector2f addVectors(Vector2f... vectors) {
		Vector2f result = new Vector2f();
		for(Vector2f vector: vectors) {
			Vector2f.add(result, vector, result);
		}
		return result;
	}
	
	public static Vector3f scaleVector(Vector3f vector, float scale) {
		return new Vector3f(scale*vector.x, scale*vector.y, scale*vector.z);
	}
	
	public static Vector2f scaleVector(Vector2f vector, float scale) {
		return new Vector2f(scale*vector.x, scale*vector.y);
	}
	
	public static Vector3f subtract(Vector3f vector1, Vector3f vector2) {
		return Vector3f.sub(vector1, vector2, null);
	}
	
	public static Vector2f subtract(Vector2f vector1, Vector2f vector2) {
		return Vector2f.sub(vector1, vector2, null);
	}
	
	public static void printVector(String name, Vector3f vector) {
		System.out.println(name + "= [ " + vector.x + ", " + vector.y + ", " + vector.z + "]");
	}
	
    public static float round(double number, int decimalPlace) {
        BigDecimal bd;
		try {
			bd = new BigDecimal(number);
			bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
		} catch (Exception e) {
			return Float.NaN;
		}
        return bd.floatValue();
    }
    
	public static Matrix4f createTransformationMatrix(Vector2f translation, Vector2f scale) {
		Matrix4f matrix = new Matrix4f();
		matrix.setIdentity();
		Matrix4f.translate(translation, matrix, matrix);
		Matrix4f.scale(new Vector3f(scale.x, scale.y, 1f), matrix, matrix);
		return matrix;
	}
	
	public static Matrix4f createTransformationMatrix(Vector3f translation, 
			float heading, float pitch, float roll, float scale) {
		Matrix4f matrix = new Matrix4f();
		matrix.setIdentity();
		Matrix4f.translate(translation, matrix, matrix);
		Matrix4f.rotate((float) heading, new Vector3f(0,1,0), matrix, matrix);
		Matrix4f.rotate((float) pitch, new Vector3f(1,0,0), matrix, matrix);
		Matrix4f.rotate((float) roll, new Vector3f(0,0,1), matrix, matrix);
		Matrix4f.scale(new Vector3f(scale,scale,scale), matrix, matrix);
		return matrix;
	}
	
	public static Matrix4f createTransformationMatrix(Vector2f translation, 
			float heading, float pitch, float roll, Vector2f scale) {
		Matrix4f matrix = new Matrix4f();
		matrix.setIdentity();
		Matrix4f.translate(translation, matrix, matrix);
		Matrix4f.rotate((float) heading, new Vector3f(0,1,0), matrix, matrix);
		Matrix4f.rotate((float) pitch, new Vector3f(1,0,0), matrix, matrix);
		Matrix4f.rotate((float) roll, new Vector3f(0,0,1), matrix, matrix);
		Matrix4f.scale(new Vector3f(scale.x, scale.y, 1f), matrix, matrix);
		return matrix;
	}
	
	public static AutopilotInputs getAutopilotInputs(Drone drone, float elapsedTime, byte[] image){
		return new AutopilotInputs() {
			public float getZ() {
				return drone.getPosition().z;
			}
			public float getY() {
				return drone.getPosition().y;
			}
			public float getX() {
				return drone.getPosition().x;
			}
			public float getRoll() {
				return drone.getRoll();
			}
			public float getPitch() {
				return drone.getPitch();
			}
			public byte[] getImage() {
				return image;
			}
			public float getHeading() {
				return drone.getHeading();
			}
			public float getElapsedTime() {
				return elapsedTime;
			}
		};
	}
	
	public static final int[] CUBE_INDICES = {
			0,1,3,	
			3,1,2,	
			4,5,7,
			7,5,6,
			8,9,11,
			11,9,10,
			12,13,15,
			15,13,14,	
			16,17,19,
			19,17,18,
			20,21,23,
			23,21,22
	};
	
	public static final float[] CUBE_COLORVALUES = {
			.35f, .35f, .35f, .35f,
			.3f, .3f, .3f, .3f,
			.25f, .25f, .25f, .25f,
			.40f, .40f, .40f, .40f,
			.45f, .45f, .45f, .45f,
			.2f, .2f, .2f, .2f
	};
	
	public static float getSigmundInterpolationPoint(int number, int max) {
		if(number==0) return 0;
		if(number==max) return 1;
		float point = 20*((float)number/(float)max) - 10;
		return sigmund(point);
		
	}
	
	public static float sigmund(float x ) {
		return ((float)Math.exp(x)/(1+(float)Math.exp(x)));
	}
	
	public static void main(String[] args) {
		for(int i=0;i<11;i++) {
			System.out.println(getSigmundInterpolationPoint(i, 10));
		}
	}
}
