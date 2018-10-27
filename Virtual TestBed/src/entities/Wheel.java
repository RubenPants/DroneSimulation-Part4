package entities;

import org.lwjgl.util.vector.Vector3f;

public class Wheel {

	public static float RMAX = 2500;
	
	private final Vector3f position;
	
	private final float tyreRadius;
	private final float tyreSlope;
	private final float dampSlope;
	private final float fcMax;
	
	private float brakeForce;
	private boolean isPressed;
	private float pressed = 0;
	
	public Wheel(Vector3f position, float tyreRadius, float tyreSlope, float dampSlope, float fcMax){
		this.position = position;
		this.tyreRadius = tyreRadius;
		this.tyreSlope = tyreSlope;
		this.dampSlope = dampSlope;
		this.fcMax = fcMax;
	}

	public Vector3f getPosition() {
		return position;
	}

	public float getRadius() {
		return this.tyreRadius;
	}
	
	public float getTyreSlope(){
		return this.tyreSlope;
	}
	
	public float getDampSlope(){
		return this.dampSlope;
	}
	
	public float getFcMax(){
		return this.fcMax;
	}
	
	public float getBrakeForce() {
		return brakeForce;
	}

	public void setBrakeForce(float brakeForce) {
		if(brakeForce<0){
			this.brakeForce=0;
		}
		if(brakeForce<RMAX){
			this.brakeForce = brakeForce;
		}
	}
	
	public void setWheelPressed(boolean wheelPressed) {
		isPressed = wheelPressed;
	}

	public boolean isPressed() {
		return isPressed;
	}

	public float getPressed() {
		return pressed;
	}

	public void setPressed(float pressed) {
		this.pressed = pressed;
	}
}