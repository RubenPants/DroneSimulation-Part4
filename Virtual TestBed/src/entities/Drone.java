package entities;

import static java.lang.Math.atan2;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static tools.Tools.addVectors;
import static tools.Tools.scaleVector;
import static tools.Tools.transformVector;

import java.util.ArrayList;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

import guis.GuiTexture;
import interfaces.AutopilotConfig;
import interfaces.AutopilotOutputs;
import shaders.ModelTexture;
import tools.Tools;
import worldSimulation.DroneStartSettings;

public class Drone{
	
	private AutopilotConfig config;
	private int id;
	private Airport startAirport;
	private int gate = -1;
	
	private boolean hasPackage = false;
	
	private float gravity;
	private float MAX_AOA;
	
	private float propellerRoll=0;
	private float propellerAccel=0;
	private float propellerVelocity=0;
	private float accelTotal;//=new ArrayList<Float>(Collections.nCopies(50, 0));
	
	private static TexturedModel model;
	private static TexturedModel propellerModel;
	private static TexturedModel shadowModel;
	
	private static int guiTexture;
	private static int guiTextureFocus;
	private static int guiTexturePackage;
	private static int guiTexturePackageFocus;
	
	private float totalMass;
	private float engineZ;
	private float wingX;
	private float tailZ;
	
	private float wingSlope;
	private float horStabSlope;
	private float verStabSlope;
	
	private float inX; //traagheidsmoment in kg*m2 as van de vleugels / bij pitch draaiing
	private float inZ; //traagheidsmoment bij roll draaiing / om de as van de romp
	private float inY; //traagheidsmoment bij heading draaiing / om de as loodrecht door vliegtuig (is gwn de som van de andere 2)
	
	private Vector3f position = new Vector3f(0,0,0);	//world coo
	private Vector3f velocity = new Vector3f(0,0,0);	//world coo
	private Vector3f angularVelocity = new Vector3f(0,0,0); //world coo
	
	private float heading = 0; //links rechts gedraaid in radialen
	private float pitch = 0; //voor onder boven gedraaid in radialen
	private float roll = 0;// (float)Math.PI;	//rond eigen as links rechts gedraaid in radialen
	
	private Wheel frontWheel;
	private Wheel leftWheel;
	private Wheel rightWheel;
	
	private float thrust;
	private float leftWingInclination;
	private float rightWingInclination;
	private float horStabInclination = 0;
	private float verStabInclination = 0;

	public Drone(AutopilotConfig config){
		setConfiguration(config);
	}
	
	public void setConfiguration(AutopilotConfig config){
		this.config = config;
		this.totalMass = config.getEngineMass() + 2*config.getWingMass() + config.getTailMass();
		this.wingX = config.getWingX();
		this.tailZ = config.getTailSize();
		this.engineZ = -(config.getTailMass()*tailZ)/config.getEngineMass();
		this.inZ = 2*config.getWingMass()*wingX*wingX;
		this.inX = config.getEngineMass()*engineZ*engineZ + config.getTailMass()*tailZ*tailZ;
		this.inY = this.inZ + this.inX;
		this.wingSlope = config.getWingLiftSlope();
		this.horStabSlope = config.getHorStabLiftSlope();
		this.verStabSlope = config.getVerStabLiftSlope();
		this.MAX_AOA = config.getMaxAOA();
		this.gravity = -config.getGravity();
		this.frontWheel = new Wheel(new Vector3f(0,config.getWheelY(),config.getFrontWheelZ()), config.getTyreRadius(), config.getTyreSlope(), config.getDampSlope(), config.getFcMax());
		this.leftWheel = new Wheel(new Vector3f(-config.getRearWheelX(),config.getWheelY(),config.getRearWheelZ()), config.getTyreRadius(), config.getTyreSlope(), config.getDampSlope(), config.getFcMax());
		this.rightWheel = new Wheel(new Vector3f(config.getRearWheelX(),config.getWheelY(),config.getRearWheelZ()), config.getTyreRadius(), config.getTyreSlope(), config.getDampSlope(), config.getFcMax());

	}
	
	public void setInputs(AutopilotOutputs outputs){
		if(outputs.getThrust()<0) {
			this.thrust = 0;
		} else if(outputs.getThrust() > this.config.getMaxThrust()){
			this.thrust = this.config.getMaxThrust();
		} else {
			this.thrust = outputs.getThrust();
		}
		this.leftWingInclination = (float) outputs.getLeftWingInclination();
		this.rightWingInclination = (float) outputs.getRightWingInclination();
		this.horStabInclination =(float) outputs.getHorStabInclination();
		this.verStabInclination = (float) outputs.getVerStabInclination();		
		this.frontWheel.setBrakeForce(outputs.getFrontBrakeForce());
		this.leftWheel.setBrakeForce(outputs.getLeftBrakeForce());
		this.rightWheel.setBrakeForce(outputs.getRightBrakeForce());
	}
	
	public void reset(DroneStartSettings settings){
		this.position = settings.getPosition();
		this.heading = settings.getHeading();
		this.pitch = settings.getPitch();
		this.roll = settings.getRoll();

		this.angularVelocity = settings.getAngularVelocity();
		this.velocity = settings.getVelocity();
	}
	
	public Vector3f getPosition() {
		return position;
	}
	
	public float getHeading() {
		return heading;
	}
	
	public float getPitch() {
		return pitch;
	}
	
	public float getRoll() {
		return roll;
	}
	
	public void timePassed(float timePassed){

		updatePropeller(timePassed);
		//*******TRANSFORMATION MATRICES**********
		Matrix4f droneToWorld = getDroneToWorldTransformationMatrix(heading, pitch, roll);
		Matrix4f worldToDrone = getWorldToDroneTransformationMatrix(heading, pitch, roll);
		
		//*******NEW POSITION*********************
		Vector3f relativeVelocity = transformVector(worldToDrone, velocity);
		Vector3f relativeAngularVelocity = transformVector(worldToDrone, angularVelocity);
		
		Vector3f deltaPos = scaleVector(velocity, timePassed);
		this.position = addVectors(deltaPos, position);
		
		//*******NEW HEADING PITCH AND ROLL*******
		Vector3f deltaAngle = scaleVector(relativeAngularVelocity, timePassed);
		Vector3f newForward = addVectors(new Vector3f(0,0,-1),Vector3f.cross(deltaAngle, new Vector3f(0,0,-1), null));
		Vector3f newRight = addVectors(new Vector3f(1,0,0),Vector3f.cross(deltaAngle, new Vector3f(1,0,0), null));
		Vector3f forwardVector = transformVector(droneToWorld, newForward);
		Vector3f headingVector = (new Vector3f(forwardVector.x,0,forwardVector.z)).normalise(null);
		Vector3f rightVector = transformVector(droneToWorld, newRight);
		Vector3f R0 = Vector3f.cross(headingVector, new Vector3f(0,1,0), null);
		Vector3f U0 = Vector3f.cross(R0, forwardVector, null);
		
		heading = (float) atan2(-headingVector.x, -headingVector.z);
		pitch = (float) atan2(forwardVector.y, Vector3f.dot(forwardVector, headingVector));
		roll = (float) atan2(Vector3f.dot(rightVector, U0), Vector3f.dot(rightVector, R0));
		
		//*******FORCES ON THE WINGS***************
		Vector3f relativeAirSpeed = relativeVelocity;
		
		Vector3f leftWingAV = new Vector3f(0,(float) sin(leftWingInclination),-(float)cos(leftWingInclination));
		Vector3f leftWingLift = calculateLiftForce(relativeAirSpeed, leftWingAV, new Vector3f(1,0,0), wingSlope);
		
		Vector3f rightWingAV = new Vector3f(0,(float) sin(rightWingInclination),-(float)cos(rightWingInclination));
		Vector3f rightWingLift = calculateLiftForce(relativeAirSpeed, rightWingAV, new Vector3f(1,0,0), wingSlope);
		
		Vector3f horStabAV = new Vector3f(0,(float) sin(horStabInclination),-(float)cos(horStabInclination));
		Vector3f horStabLift = calculateLiftForce(relativeAirSpeed, horStabAV, new Vector3f(1,0,0), horStabSlope);
		
		Vector3f verStabAV = new Vector3f(-(float)sin(verStabInclination), 0, -(float)cos(verStabInclination));
		Vector3f verStabLift = calculateLiftForce(relativeAirSpeed, verStabAV, new Vector3f(0,1,0), verStabSlope);
		
		Vector3f leftWingMoment = Vector3f.cross(new Vector3f(-wingX,0,0), leftWingLift, null);
		Vector3f rightWingMoment = Vector3f.cross(new Vector3f(wingX,0,0), rightWingLift, null);
		Vector3f horStabMoment = Vector3f.cross(new Vector3f(0,0,tailZ), horStabLift, null);
		Vector3f verStabMoment = Vector3f.cross(new Vector3f(0,0,tailZ), verStabLift, null);
		
		//*******FORCE DUE TO GRAVITY***************
		Vector3f droneGravity = transformVector(worldToDrone, new Vector3f(0, totalMass*gravity, 0));	

		//*********TOTAL FORCE NORMAL FORCES**********
		Vector3f totalForce1 = addVectors(leftWingLift, rightWingLift, horStabLift, verStabLift, new Vector3f(0,0,-thrust), droneGravity);
		Vector3f velocityAcceleration1 = scaleVector(totalForce1, 1/totalMass);
		
		//*******WHEEL FORCES (WHEN ON THE GROUND)**
		ArrayList<Vector3f> groundForces = calculateGroundLiftForce(timePassed);
		Vector3f groundLiftForce = addVectors(groundForces.get(0), groundForces.get(1),groundForces.get(2));
		Vector3f groundLiftForceRelative = transformVector(worldToDrone, groundLiftForce);
		ArrayList<Vector3f> onGroundBrakefrict = calculateOnGroundBrakeFrictForce(groundForces,timePassed, worldToDrone);
		Vector3f groundBrakeFrictForce = addVectors(onGroundBrakefrict.get(0), onGroundBrakefrict.get(1),onGroundBrakefrict.get(2));
		
		Vector3f wheelMomentNormal1 = calculateWheelMoment(frontWheel, groundForces.get(0), droneToWorld);
		Vector3f wheelMomentNormal2 = calculateWheelMoment(leftWheel, groundForces.get(1), droneToWorld);
		Vector3f wheelMomentNormal3 = calculateWheelMoment(rightWheel, groundForces.get(2), droneToWorld);

		Vector3f totalWorldMoment = addVectors(wheelMomentNormal1, wheelMomentNormal2, wheelMomentNormal3);
		Vector3f totalWorldMomentRelative = transformVector(worldToDrone, totalWorldMoment);
		
		Vector3f wheelMomentFrict1 = calculateWheelBottomMoment(frontWheel, onGroundBrakefrict.get(0), worldToDrone);
		Vector3f wheelMomentFrict2 = calculateWheelBottomMoment(leftWheel, onGroundBrakefrict.get(1), worldToDrone);
		Vector3f wheelMomentFrict3 = calculateWheelBottomMoment(rightWheel, onGroundBrakefrict.get(2), worldToDrone);
		
		Vector3f totalFrictMomentRelative = addVectors(wheelMomentFrict1, wheelMomentFrict2, wheelMomentFrict3);

		//*********TOTAL FORCE WHEELS**********
		Vector3f totalForce2 = addVectors(groundLiftForceRelative, groundBrakeFrictForce);
		Vector3f velocityAcceleration2 = scaleVector(totalForce2, 1/totalMass);
		
		//*********TOTAL MOMENT****************
		Vector3f totalMoment = addVectors(leftWingMoment, rightWingMoment, horStabMoment, verStabMoment, totalWorldMomentRelative, totalFrictMomentRelative);
		
		//*********NEW (ANGULAR) VELOCITY**********

		Vector3f extraVel = ignoreSmallValues(transformVector(droneToWorld, scaleVector(velocityAcceleration1, timePassed)), 0.0001f);
	
		Vector3f normalVel = addVectors(extraVel, velocity);
		Vector3f counterVel = ignoreSmallValues(transformVector(droneToWorld, scaleVector(velocityAcceleration2, timePassed)), 0.0001f);
				
		float xVel = originalPlusCounter(normalVel.x, counterVel.x);
		float yVel = normalVel.y + counterVel.y;
		float zVel = originalPlusCounter(normalVel.z, counterVel.z);
		
		velocity = new Vector3f(xVel,yVel,zVel);

		Vector3f angularAcceleration = new Vector3f(totalMoment.x/inX, totalMoment.y/inY, totalMoment.z/inZ);
		
		relativeAngularVelocity = addVectors(relativeAngularVelocity,scaleVector(angularAcceleration, timePassed));
		if(velocity.lengthSquared()<.1f) {
			angularVelocity = new Vector3f();
		} else {
			angularVelocity = transformVector(droneToWorld, relativeAngularVelocity);
		}
		
		isCrashed();
		
	}
	
	private Vector3f calculateLiftForce(Vector3f airSpeed, Vector3f attackVector, Vector3f axis, float slope) {
		Vector3f projAirSpeed = new Vector3f((1-axis.x)*airSpeed.x, (1-axis.y)*airSpeed.y, (1-axis.z)*airSpeed.z);
		Vector3f normal = Vector3f.cross(axis, attackVector, null);
		float AOA = (float) -Math.atan2(Vector3f.dot(projAirSpeed, normal), Vector3f.dot(projAirSpeed, attackVector));
		float factor = slope*AOA*projAirSpeed.lengthSquared();
		Vector3f result = scaleVector(normal, factor);
		if(Math.abs(AOA) > this.MAX_AOA && result.y > 50){
			throw new RuntimeException(toString() + ": MAXAOA WAS EXCEEDED. Allowed AOA is " + this.MAX_AOA + ". Calculated AOA is " + AOA +".");
		}
		return result;
	}
	
	private Matrix4f getWorldToDroneTransformationMatrix(float heading, float pitch, float roll) {
		return Matrix4f.invert(getDroneToWorldTransformationMatrix(heading, pitch, roll), null);
	}

	private Matrix4f getDroneToWorldTransformationMatrix(float heading, float pitch, float roll) {
		Matrix4f matrix = new Matrix4f();
		matrix.setIdentity();
		Matrix4f.rotate(heading, new Vector3f(0, 1, 0), matrix, matrix);
		Matrix4f.rotate(pitch, new Vector3f(1, 0, 0), matrix, matrix);
		Matrix4f.rotate(roll, new Vector3f(0, 0, 1), matrix, matrix);
		return matrix;
	}
	public static void addModels(TexturedModel model, TexturedModel shadowModel, int guiTexture, int guiTexturePackage, int focus, int packFocus) {
		Drone.model = model;
		Drone.shadowModel = shadowModel;
		Drone.guiTexture = guiTexture;
		Drone.guiTexturePackage = guiTexturePackage;
		Drone.guiTextureFocus = focus;
		Drone.guiTexturePackageFocus = packFocus;
	}
	
	public TexturedModel getPropellerModel() {
		return propellerModel;
	}

	public float getPropellerRoll() {
		return propellerRoll;
	}

	public static void setPropellerModel(TexturedModel propellerModel2) {
		propellerModel = propellerModel2;
	}

	public void setPropellerRoll(float propellerRoll) {
		while(propellerRoll<0){
			propellerRoll=(float) (propellerRoll+2*Math.PI);
		}
		this.propellerRoll = propellerRoll;
	}

	protected void updatePropeller(float time){
		addValueToAccelArray(thrust);
		//addValueToAccelArray(velocity.length());
		float average=calculateAverage();
		/*
		propellerAccel=(thrust-lastThrust)/2000;
		System.out.println("propelaccel: "+ propellerAccel);
		propellerVelocity=propellerVelocity+.2f*propellerAccel*time;
		if(propellerVelocity>thrust/2000+.3f){
			propellerVelocity=thrust/2000+.3f;
		}
		*/
	//	System.out.println(average);
		if(average>1600.3){
			average=1600.3f;
		}
		//setPropellerRoll(propellerRoll-0.008f*(average));
		setPropellerRoll(propellerRoll-0.000032f*(average));
	}
	private void addValueToAccelArray(float thrust) {
		accelTotal=accelTotal-(accelTotal/1000);
		accelTotal=accelTotal+thrust;
		
		
	}
	private float calculateAverage(){
		
		return (accelTotal/1000);
	}
	
	public Entity getEntity() {
		float scalingFactor = 2.90f;
		
		return new Entity(model,position,heading,pitch,roll,scalingFactor);
	}
	

	public Entity getPropellerEntity(){
		float scalingFactor = 2.90f;
		Vector3f propellposition=new Vector3f(0,1.6f,0);
		
		Matrix4f droneToWorld = getDroneToWorldTransformationMatrix(heading, pitch, roll);
		Vector3f toPutPosition= transformVector(droneToWorld, propellposition);
		toPutPosition.translate(position.x,position.y,position.z);
		return new Entity(propellerModel,toPutPosition,heading,pitch,roll+propellerRoll,scalingFactor);
	}
	
	public GuiTexture getGuiTexture(float width, boolean focus){
		if(isHasPackage()) {
			return new GuiTexture(focus? guiTexturePackageFocus: guiTexturePackage, new Vector2f(2*position.x/width-1, 2*-position.z/width-1),
							getHeading(), new Vector2f(focus? 0.045f : 0.03f, focus? 0.045f : 0.03f));
		} else {
			return new GuiTexture(focus? guiTextureFocus: guiTexture, new Vector2f(2*position.x/width-1, 2*-position.z/width-1),
				getHeading(), new Vector2f(focus? 0.045f : 0.03f, focus? 0.045f : 0.03f));
		}
	}
	
	public Entity getShadowEntity(){
		float scale;
		if(position.y > 80){
			scale = 0;
		} else {
			scale = 30/position.y;
		}
		
		return new Entity(shadowModel, new Vector3f(position.x, 0.06f, position.z), 0, 0, 0, scale);
	}
	
	public ModelTexture getShadowTexID(){
		return shadowModel.getTexture();
	}
	
	public Vector3f getVelocity() {
		return velocity;
	}
	
	public Vector3f getRelativeAngularVelocity() {
		Matrix4f worldToDrone = getWorldToDroneTransformationMatrix(heading, pitch, roll);
		return transformVector(worldToDrone, angularVelocity);
	}
	
	public ModelTexture getTextureID(){
		return model.getTexture();
	}
	
	private Vector3f calculateAbsolutePosition(Vector3f todrone){
		Vector3f result=transformVector(getDroneToWorldTransformationMatrix(heading, pitch, roll), todrone);
		result.translate(position.x, position.y, position.z);
		return result;
	}
	
	private ArrayList<Vector3f> calculateOnGroundBrakeFrictForce(ArrayList<Vector3f> groundForces, float timePassed, Matrix4f worldToDrone) {
		checkWheelsPressed();
		Vector3f forceByWheel1 = new Vector3f();
		Vector3f forceByWheel2 = new Vector3f();
		Vector3f forceByWheel3 = new Vector3f();
		ArrayList<Vector3f> wheelForces = new ArrayList<>();
	
		if(frontWheel.isPressed()){
			forceByWheel1 = new Vector3f();// calculateFrictForce(frontWheel,groundForces.get(0),timePassed, worldToDrone);
			forceByWheel1 = Vector3f.add(forceByWheel1,calculateBrakeForce(frontWheel,timePassed,worldToDrone),null);
		}
		wheelForces.add(forceByWheel1);
		
		if(leftWheel.isPressed()){		
			forceByWheel2 = calculateFrictForce(leftWheel,groundForces.get(1),timePassed, worldToDrone);
			forceByWheel2 = Vector3f.add(forceByWheel2,calculateBrakeForce(leftWheel,timePassed,worldToDrone),null);
			
		}
		wheelForces.add(forceByWheel2);

		if(rightWheel.isPressed()){
			forceByWheel3 = calculateFrictForce(rightWheel,groundForces.get(2),timePassed, worldToDrone);	
			forceByWheel3 = Vector3f.add(forceByWheel3,calculateBrakeForce(rightWheel,timePassed,worldToDrone),null);
			
		}
		wheelForces.add(forceByWheel3);
		return wheelForces;
	}

	private Vector3f calculateBrakeForce(Wheel wheel, float timePassed, Matrix4f worldToDrone) {
		Vector3f totalForce;
		Vector3f relativeVelocity = transformVector(worldToDrone, velocity);
		Vector3f relativeAngularVelocity = transformVector(worldToDrone, angularVelocity);
		
		Vector3f rotationVelocity = Vector3f.cross(relativeAngularVelocity, wheel.getPosition(), null);
		Vector3f totalVelocity = addVectors(relativeVelocity, rotationVelocity);
		if(totalVelocity.z<0){
			totalForce = new Vector3f(0.0f, 0.0f, wheel.getBrakeForce());
		}else{
			if(totalVelocity.z>0){
				totalForce = new Vector3f(0.0f, 0.0f, -wheel.getBrakeForce());
			}else{
				totalForce = new Vector3f(0,0,0);
			}
		}
		return totalForce;
	}

	private Vector3f calculateFrictForce(Wheel wheel, Vector3f force, float timePassed, Matrix4f worldToDrone) {
		float forceValue = force.length();
		Vector3f relativeVelocity = transformVector(worldToDrone, velocity);
		Vector3f relativeAngularVelocity = transformVector(worldToDrone, angularVelocity);
		
		Vector3f rotationVelocity = Vector3f.cross(relativeAngularVelocity, wheel.getPosition(), null);
		Vector3f totalVelocity = addVectors(relativeVelocity, rotationVelocity);
		
		Vector3f totalForce = new Vector3f(((float)(-wheel.getFcMax()*totalVelocity.x*forceValue)), 0.0f, 0.0f);
		
		return totalForce;
	}

	public ArrayList<Vector3f> calculateGroundLiftForce(float timepassed){
		checkWheelsPressed();
		ArrayList<Vector3f> wheelLiftForces = new ArrayList<>();
	
		Vector3f forceByWheel1 = calculateGroundForce(frontWheel,timepassed);
		wheelLiftForces.add(forceByWheel1);
		
		Vector3f forceByWheel2 = calculateGroundForce(leftWheel,timepassed);
		wheelLiftForces.add(forceByWheel2);

		Vector3f forceByWheel3 = calculateGroundForce(rightWheel,timepassed);
		
		wheelLiftForces.add(forceByWheel3);
		return wheelLiftForces;
	}

	private Vector3f calculateGroundForce(Wheel wheel, float timePassed){
		float oldPressedDistance = wheel.getPressed();
		calculatePressedDistance(wheel);
		
		float groundForceLength = Math.max(0, wheel.getPressed()*wheel.getTyreSlope() + (wheel.getDampSlope()*((wheel.getPressed()-oldPressedDistance)/timePassed)));
		Vector3f groundForce = new Vector3f(0,groundForceLength,0);
		
		return groundForce;
	}

	public void calculatePressedDistance(Wheel wheel) {
		Vector3f wheelCenterAbsolute = calculateAbsolutePosition(wheel.getPosition());
		if(wheelCenterAbsolute.y >= wheel.getRadius()) {
			wheel.setPressed(0);
		} else {
			float pressedDistance = wheel.getRadius() - wheelCenterAbsolute.y;
			wheel.setPressed(pressedDistance);
		}
	}
	
	private Vector3f calculateWheelMoment(Wheel wheel, Vector3f wheelForce, Matrix4f droneToWorld) {
		Vector3f centerToWheelVector = transformVector(droneToWorld, wheel.getPosition());
		return Vector3f.cross(centerToWheelVector, wheelForce, null);
	}

	private Vector3f calculateWheelBottomMoment(Wheel wheel, Vector3f wheelForce, Matrix4f worldToDrone) {
		Vector3f wheelCenterToBottomVector = transformVector(worldToDrone, new Vector3f(0, -wheel.getRadius(), 0));
		Vector3f centerToWheelBottomVector = addVectors(wheel.getPosition(), wheelCenterToBottomVector);
		return Vector3f.cross(centerToWheelBottomVector, wheelForce, null);
	}
	
	private void checkWheelsPressed() {
		frontWheel.setWheelPressed(isWheelPressed(frontWheel));
		leftWheel.setWheelPressed(isWheelPressed(leftWheel));
		rightWheel.setWheelPressed(isWheelPressed(rightWheel));	
	}
	
	public void isCrashed(){
		if(isWheelCrashed(frontWheel))
			throw new RuntimeException(toString() + ": FRONTWHEEL UNDER GROUND. WHEEL POSITION " + calculateAbsolutePosition(frontWheel.getPosition()));
		if(isWheelCrashed(leftWheel))
			throw new RuntimeException(toString() + ": LEFTWHEEL UNDER GROUND. WHEEL POSITION " + calculateAbsolutePosition(leftWheel.getPosition()));
		if(isWheelCrashed(rightWheel))
			throw new RuntimeException(toString() + ": RIGHTWHEEL UNDER GROUND. WHEEL POSITION " + calculateAbsolutePosition(rightWheel.getPosition()));
		Vector3f tailposition=calculateAbsolutePosition(new Vector3f(0,0,engineZ));
		Vector3f leftwingposition=calculateAbsolutePosition(new Vector3f(wingX,0,0));
		Vector3f rightwingposition=calculateAbsolutePosition(new Vector3f(-wingX,0,0));
		if(position.y<0||tailposition.y<0||leftwingposition.y<0||rightwingposition.y<0){
			throw new RuntimeException(toString() + ": WINGS OR TAIL UNDER GROUND");
		}
	}
	
	private boolean isWheelCrashed(Wheel wheel){
		Vector3f absoluteWheelPosition = calculateAbsolutePosition(wheel.getPosition());
		if(absoluteWheelPosition.y < 0){
			return true;
		}
		return false;
	}

	private boolean isWheelPressed(Wheel wheel) {
		Vector3f absoluteWheelPosition = calculateAbsolutePosition(wheel.getPosition());
		if(absoluteWheelPosition.y < wheel.getRadius()){
			return true;
		}
		return false;
	}
	private float timePassed;
	
	public void setTimePassed(float timePassed){
		this.timePassed = timePassed;
	}
	
	private float originalPlusCounter(float originalForce, float counterForce) {
		//System.out.println("ORIGFORCE: " + originalForce + "COUNTER: " + counterForce);
		float result = originalForce + counterForce;
		if (result*originalForce>0) return result;
		else return 0;
	}
	
	private Vector3f ignoreSmallValues(Vector3f vector, float eps) {
		//float eps = 0.0001f;
		float x = (Math.abs(vector.x)>eps)? vector.x : 0;
		float y = (Math.abs(vector.y)>eps)? vector.y : 0;
		float z = (Math.abs(vector.z)>eps)? vector.z : 0;
		return new Vector3f(x,y,z);
	}
	
	public Vector3f getCameraPosition() {
		Matrix4f toWorld = getDroneToWorldTransformationMatrix(heading, pitch, roll);
		Vector3f cameraPos = new Vector3f(0,200*(float)Math.cos(Math.PI/6),200*(float)Math.sin(Math.PI/6));
		return addVectors(position,Tools.transformVector(toWorld, cameraPos));
	}
	
	public Airport getAirport(){
		return startAirport;
	}
	
	public void setAirport(Airport airport){
		this.startAirport = airport;
	}
	
	public int getGate(){
		return gate;
	}
	
	public void setGate(int gate){
		this.gate = gate;
	}
	
	public AutopilotConfig getConfigs(){
		return config;
	}
	
	public void setID(int id){
		this.id = id;
	}
	
	public int getID(){
		return id;
	}
	
	public String toString(){
		return "Drone " + id;
	}

	public boolean isHasPackage() {
		return hasPackage;
	}

	public void setHasPackage(boolean hasPackage) {
		this.hasPackage = hasPackage;
	}
}