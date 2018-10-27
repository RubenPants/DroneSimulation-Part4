package autopilot_physics;

import static java.lang.Math.cos;
import static java.lang.Math.sin;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;

import autopilot_planning.DroneStage;
import autopilot_utilities.Vector3f;
import autopilot_utilities.Vector4f;
import autopilot_utilities.Matrix4f;
import interfaces.AutopilotConfig;
import interfaces.AutopilotInputs;
import interfaces.AutopilotOutputs;

/**
 * A class of physical models for autopiloting a drone.
 * 
 * @author Team Saffier
 * @version 3.0
 */
public class AutopilotPhysics {

	float precision = 0.000001f;
	
	/**
	 * Variable denoting whether or not this class logs information.
	 */
	public boolean ENABLE_LOGGING;
	private boolean POSITION_LOGGING = false;
	private boolean DRONE_STAT_LOGGING = true;
	private boolean ERROR_LOGGING = false;
	private boolean ESSENTIAL_LOGGING = true;

	// *** VARIABLES *** //
	private float gravity;
	private float wingX;
	private float tailSize;
	private float engineMass;
	private float wingMass;
	private float tailMass;
	private float maxThrust;
	private float maxAOA;
	private float wingLiftSlope;
	private float horStabSlope;
	private float verStabSlope;
	private float engineZ;

	private Matrix4f toDrone;
	private Matrix4f toWorld;
	private Vector3f projAirSpeed;
	private Vector3f projGravity;

	private float sSquared;
	private float requestedSSquared;
	private float requestedSpeedFactor = 1;
	private float averageWingInclination;
	private float adjustInclination;
	private float thrust;
	private float AOA;
	
	private float maxRoll = (float) (Math.PI / 4); // 45 degrees
	private float maxAdjustInclination = (float) (Math.PI / 90); // 2 degrees
	private int maxFrontBrake;
	private int maxBackBrake;
	private float requestedHeading;
	private float requestedPitch;
	private float frontBrake;
	private float rightBrake;
	private float leftBrake;
	private boolean isMaxRoll;

	private float x = 0;
	private float y = 0;
	private float z = 0;
	private float heading = 0;
	private float pitch = 0;
	private float roll = 0;
	private float previousRoll = 0;
	private float deltaTimeElapsed = 0.012f;
	private String stage = "O";
	private Vector3f airSpeed = new Vector3f(0, 0, (float) -Math.sqrt(requestedSSquared));

	DatagramSocket serverSocket;
	byte[] receiveData;

	/**
	 * Initialize this new physics engine.
	 */
	public AutopilotPhysics() {
		
	}

	/**
	 * Read in the given autopilot configuration.
	 * 
	 * @param 	configuration
	 * 			The configuration that is to be read and used.
	 */
	public void setConfiguration(AutopilotConfig configuration) {
		
		this.gravity = configuration.getGravity();
		this.wingX = configuration.getWingX();
		this.tailSize = configuration.getTailSize();
		this.engineMass = configuration.getEngineMass();
		this.wingMass = configuration.getWingMass();
		this.tailMass = configuration.getTailMass();
		this.maxThrust = configuration.getMaxThrust();
		
		// The most optimal maxAOA = 0.86rad. 0.8f is chosen for certainty.
		if (maxAOA > 0.80f)
			this.maxAOA = 0.80f;
		else
			this.maxAOA = configuration.getMaxAOA();
		this.wingLiftSlope = configuration.getWingLiftSlope();
		this.horStabSlope = configuration.getHorStabLiftSlope();
		this.verStabSlope = configuration.getVerStabLiftSlope();
		this.engineZ = calculateEnginePos(tailMass, tailSize, engineMass);
		this.requestedSSquared = (float) Math.abs((1.2 * gravity * getTotalMass())
				/ (maxAOA * Math.cos(maxAOA / 2) * wingLiftSlope * Math.cos(roll) * Math.cos(pitch)));
		if (configuration.getRMax() > 1500) {
			maxFrontBrake = 1000;
			maxBackBrake = 1500;
		} else {
			maxFrontBrake = (int)configuration.getRMax();
			maxBackBrake = (int)configuration.getRMax();
		}
		
	}
	
	public void getInputsStart(AutopilotInputs inputs) {
		// Not used
	}

	/**
	 * Receive and update the drone inputs
	 * 
	 * @param inputs
	 */
	public void getInputs(AutopilotInputs inputs) {
		float currentX = inputs.getX();
		float currentY = inputs.getY();
		float currentZ = inputs.getZ();

		heading = inputs.getHeading();
		pitch = inputs.getPitch();
		previousRoll = roll;
		roll = inputs.getRoll();
		// TODO
		if (deltaTimeElapsed == 0.0) {
			deltaTimeElapsed = 0.02f;
		}
		airSpeed = autopilot_utilities.Utilities.scaleVector(
				Vector3f.sub(new Vector3f(currentX, currentY, currentZ), new Vector3f(x, y, z), null),
				(float) (1.0 / deltaTimeElapsed));

		x = currentX;
		y = currentY;
		z = currentZ;

		requestedSSquared = (float) Math.abs((1.2 * gravity * getTotalMass())
				/ (maxAOA * Math.cos(maxAOA / 2) * wingLiftSlope * Math.cos(roll) * Math.cos(pitch)));

		// TODO
		if (pitch > -Math.PI/90 || y > 100)
			isMaxRoll = true;
		if (!isMaxRoll || (y < 30 && pitch < 0) || (y < 100 && pitch < -Math.PI/36 && requestedPitch > 0) || 
				pitch < -Math.PI/18) {
			isMaxRoll = false;
			maxRoll = (float) Math.PI/6;	// 30 degrees
		} else {
			maxRoll = (float) Math.PI/4;	// 60 degrees
		}
		
		if (ENABLE_LOGGING) {
			System.out.println("_____________________________________________________");
			System.out.println("");
			switch (stage) {
			case "O":
				System.out.println("STAGE: TAKE_OFF");
				break;
			case "L":
				System.out.println("STAGE: LAND");
				break;
			case "T":
				System.out.println("STAGE: TAXI");
				break;
			case "F":
				System.out.println("STAGE: FLY");
				break;
			default:
				System.out.println("STAGE: FREE");
				break;
			}
		}

		if (ENABLE_LOGGING && POSITION_LOGGING) {
			System.out.println("POSITION: ");
			System.out.println("---X: " + x);
			System.out.println("---Y: " + y);
			System.out.println("---Z: " + z);
			System.out.println("---Time elapsed: " + deltaTimeElapsed);
		}
	}

	/**
	 * The main method determining what the drone outputs are
	 * 
	 * @param reqPitch
	 * @param reqHeading
	 * @param speedFactor
	 * @param stage
	 * @param deltaTimeElapsedGiven
	 * 
	 * @return   The parameters that define the drone's movement in the next frame
	 */
	public AutopilotOutputs output(float reqPitch, float reqHeading, float speedFactor, DroneStage stage, float deltaTimeElapsedGiven) {
		if (ENABLE_LOGGING && DRONE_STAT_LOGGING) {
			System.out.println("DRONE STATS:");
			System.out.println("---deltaElapsedTime: "+deltaTimeElapsedGiven);
		}
		
		toDrone = getWorldToDroneTransformationMatrix(heading, pitch, roll);
		toWorld = getDroneToWorldTransformationMatrix(heading, pitch, roll);

		float gY = getTotalMass() * gravity;
		projGravity = transformVector(toDrone, new Vector3f(0, -gY, 0));
		
		projAirSpeed = transformVector(toDrone, airSpeed);
		sSquared = (float) projAirSpeed.lengthSquared();

		if (Math.abs(reqHeading) < 10 && Math.abs(reqPitch) < 10) {
			requestedHeading = reqHeading;
			requestedPitch = reqPitch;
		}
		
		if (deltaTimeElapsedGiven != 0)
			deltaTimeElapsed = deltaTimeElapsedGiven;

		switch (stage) {
		case FREE:
			configFree();
			break;
		case TAXI:
			configTaxi(speedFactor);
			break;
		case LAND:
			configLand();
			break;
		case TAKE_OFF:
			configTakeOff();
			break;
		case PHONE:
			configPhone();
			break;
		default:
			configFly(speedFactor);
		}

		float[] leftAndRightInclination;
		leftAndRightInclination = getLeftAndRightAdjIncl(averageWingInclination, adjustInclination, wingLiftSlope, sSquared);

		if (ENABLE_LOGGING) print(leftAndRightInclination);

		return new AutopilotOutputs() {
			@Override
			public float getRightWingInclination() {
				return leftAndRightInclination[1];
			}

			@Override
			public float getLeftWingInclination() {
				return leftAndRightInclination[0];
			}

			@Override
			public float getHorStabInclination() {
				return 0;
			}

			@Override
			public float getVerStabInclination() {
				return 0;
			}

			@Override
			public float getThrust() {
				return thrust;
			}

			@Override
			public float getFrontBrakeForce() {
				return frontBrake;
			}

			@Override
			public float getLeftBrakeForce() {
				return leftBrake;
			}

			@Override
			public float getRightBrakeForce() {
				return rightBrake;
			}
		};
	}

	// Stage methods
	/**
	 * Configure the drone when in flying stage
	 * 
	 * @param speedFactor
	 */
	private void configFly(float speedFactor) {
		// TODO: Implement the speedFactor!
		requestedSSquared *= speedFactor;
		
		stage = "F";
		frontBrake = 0;
		rightBrake = 0;
		leftBrake = 0;

		if (Math.abs(roll) > Math.PI/4.5)	// 40 degrees
			requestedPitch += Math.PI / 20;	// 9 degrees
		else if (Math.abs(roll) > Math.PI / 5) // 36 degrees
			requestedPitch += Math.PI / 36; // 5 degrees
		else if (Math.abs(roll) > Math.PI / 9) // 20 degrees
			requestedPitch += Math.PI / 45; // 4 degrees
		else if (Math.abs(roll) > Math.PI / 18) // 10 degrees
			requestedPitch += Math.PI / 90; // 2 degree
		
		// Make sure that the requestedPitch is reasonable
		if (requestedPitch > Math.PI / 12) 	// 15
			requestedPitch = (float) (Math.PI / 12);
		else if (requestedPitch > Math.PI / 18 && sSquared < 3.0 / 4.0 * requestedSSquared) // 10
			requestedPitch = (float) (Math.PI / 18);
		else if (requestedPitch < -Math.PI / 36 && sSquared > 1.5 * requestedSSquared)
			requestedPitch = (float) (-Math.PI/36);
		else if (requestedPitch < -Math.PI / 18) // -10
			requestedPitch = (float) (-Math.PI / 18);

		// Try to make plane crash-proof
		if (y < 20)
			requestedPitch = (float) Math.PI / 36;
		else if (sSquared > 3 * requestedSSquared && requestedPitch < -Math.PI / 90 && y < 50 && !stage.equals("L"))
			requestedPitch = (float) Math.PI / 90;
		else if (sSquared > 2 * requestedSSquared && pitch < -Math.PI / 360
				&& y < 10.0 * (1.0 + sSquared / requestedSSquared))
			requestedPitch = (float) Math.PI / 36;
		else if (y < 20f && pitch < (float) -Math.PI / 90)
			requestedPitch = (float) Math.PI / 90;
		else if (y < 15f && pitch < (float) -Math.PI / 90)
			requestedPitch = (float) Math.PI / 36;
		else if (sSquared > 3 * requestedSSquared && requestedPitch < -Math.PI / 36)
			requestedPitch = (float) -Math.PI / 36;

		if (sSquared < requestedSSquared * (2.0 / 3.0) && requestedPitch > Math.PI / 18 && pitch > Math.PI / 18
				&& requestedPitch > (pitch - Math.PI / 180.0))
			requestedPitch = (float) (pitch - Math.PI / 180.0); // 1 degree

		averageWingInclination = findInclinationZeroForceDroneY(wingLiftSlope, sSquared, projGravity, requestedPitch);
		checkAverageWingInclinationMaxAOA();
		
		thrust = findThrustZeroForceDroneZ(averageWingInclination, wingLiftSlope, sSquared, projGravity);
		adjustInclination = adjustInclinationToMatchHeading(averageWingInclination, requestedHeading, wingLiftSlope,
				sSquared);
	}

	/**
	 * Configure the drone when in the taxi stage
	 * 
	 * @param speedFactor
	 */
	private void configTaxi(float speedFactor) {
		stage = "T";
		if (speedFactor > 1)
			speedFactor = 1;
		else if (speedFactor < 0)
			speedFactor = 0;
		
		// Handle the requestedHeading
		float deltaHeading = (requestedHeading - heading);
		if (deltaHeading > Math.PI)	deltaHeading -= 2 * Math.PI;
		else if (deltaHeading < -Math.PI) deltaHeading += 2 * Math.PI;
		
		if (speedFactor < 0.2) requestedSSquared = 0;
		else requestedSSquared = 200 * speedFactor;	// Max speed of 51km/h
		
		if (ENABLE_LOGGING && DRONE_STAT_LOGGING) {
			System.out.println("---speedFactor: "+speedFactor);
			System.out.println("---adjusted requestedSSquared: "+requestedSSquared);
		}
		
		float brakeFactor = (float) ((sSquared - requestedSSquared) / (0.5 * requestedSSquared));
		if (brakeFactor > 1)
			brakeFactor = 1;
		if (ENABLE_LOGGING && DRONE_STAT_LOGGING)
			System.out.println("---brakeFactor: " + brakeFactor);
		if ((requestedSSquared < 0.01)) {
			frontBrake = maxFrontBrake;
			rightBrake = maxBackBrake;
			leftBrake = maxBackBrake;
		} else if (brakeFactor > 0) {
			frontBrake = brakeFactor * maxFrontBrake;
			rightBrake = brakeFactor * maxBackBrake;
			leftBrake = brakeFactor * maxBackBrake;
		} else {
			frontBrake = 0;
			rightBrake = 0;
			leftBrake = 0;
		}

		if (deltaHeading > 0.01) { // 0.57 degrees
			leftBrake *= 2;
			if (leftBrake < 50 + 5 * sSquared) {
				if (Math.abs(deltaHeading) < Math.PI / 36) // 5 degrees
					leftBrake = (float) (50 + 5 * Math.abs(deltaHeading * 36 / Math.PI) * sSquared);
				else leftBrake = 150 + 5 * sSquared;
			}
		} else if (deltaHeading < -0.01) {
			rightBrake *= 2;
			if (rightBrake < 50 + 5 * sSquared) {
				if (Math.abs(deltaHeading) < Math.PI / 36) // 5 degrees
					rightBrake = (float) (50 + 5 * Math.abs(deltaHeading * 36 / Math.PI) * sSquared);
				else rightBrake = 150 + 5 * sSquared;
			}
		}

		if (frontBrake > maxFrontBrake) frontBrake = maxFrontBrake;
		if (rightBrake > maxBackBrake) rightBrake = maxBackBrake;
		if (leftBrake > maxBackBrake) leftBrake = maxBackBrake;

		requestedPitch = 0;
		averageWingInclination = 0;
		thrust = (float) (findThrustZeroForceDroneZ(averageWingInclination, wingLiftSlope, sSquared, projGravity));
		adjustInclination = 0;
	}

	/**
	 * Configure the drone when in landing stage
	 */
	private void configLand() {
		stage = "L";

		if (y > 50f) {
			requestedSSquared = 1500f;
			if (sSquared < requestedSSquared*1.5)
				requestedPitch = (float) -Math.PI / 18;
			else
				requestedPitch = (float) -Math.PI / 36;
		} else {
			if (y > 20) {
				requestedPitch = (float) -Math.PI / 36;
				requestedSSquared = 1500f;
			} else if (y > 15) {
				requestedPitch = (float) -Math.PI / 90;
				requestedSSquared = 1500f;
			} else if (y > 5f) {
				requestedHeading = heading;
				requestedPitch = (float) -Math.PI / 180;
				requestedSSquared = 1500f;
			} else if (y > 4.8f) {
				requestedHeading = heading;
				requestedPitch = 0;
				requestedSSquared = 0;
			} else {
				requestedHeading = heading;
				requestedPitch = 0;
				requestedSSquared = 0;
				frontBrake = maxFrontBrake;
				rightBrake = maxBackBrake;
				leftBrake = maxBackBrake;
			}
		}

		averageWingInclination = findInclinationZeroForceDroneY(wingLiftSlope, sSquared, projGravity, requestedPitch);
		checkAverageWingInclinationMaxAOA();
		
		thrust = findThrustZeroForceDroneZ(averageWingInclination, wingLiftSlope, sSquared, projGravity);
		adjustInclination = adjustInclinationToMatchHeading(averageWingInclination, requestedHeading, wingLiftSlope,
				sSquared);
	}

	/**
	 * Configure the drone when it is taking off
	 */
	private void configTakeOff() {
		stage = "O";

		frontBrake = 0;
		rightBrake = 0;
		leftBrake = 0;

		requestedHeading = heading;
		if (sSquared > 9.0/10.0 * requestedSSquared) {
			requestedPitch = (float) Math.PI / 18; // 10 degrees
			averageWingInclination = findInclinationZeroForceDroneY(wingLiftSlope, sSquared, projGravity, requestedPitch);
			checkAverageWingInclinationMaxAOA();
		} else if (sSquared > 2.0 / 3.0 * requestedSSquared) {
			requestedPitch = (float) Math.PI / 36; // 5 degrees
			averageWingInclination = findInclinationZeroForceDroneY(wingLiftSlope, sSquared, projGravity, requestedPitch);
			checkAverageWingInclinationMaxAOA();
		} else {
			requestedPitch = 0;
			averageWingInclination = 0;
		}
		
		thrust = findThrustZeroForceDroneZ(averageWingInclination, wingLiftSlope, sSquared, projGravity);
		adjustInclination = adjustInclinationToMatchHeading(averageWingInclination, requestedHeading, wingLiftSlope,
				sSquared);
	}
	
	/**
	 * The drone in waiting stage
	 */
	private void configFree() {
		stage = "-";
		
		frontBrake = maxFrontBrake;
		rightBrake = 0;
		leftBrake = 0;
		if (sSquared > 10) {
			rightBrake = maxBackBrake;
			leftBrake = maxBackBrake;
		}
		
		requestedPitch = 0;
		adjustInclination = 0;
		requestedHeading = heading;
		
		requestedSSquared = 0;
		thrust = 0;
	}

	/**
	 * Control the drone with the phone
	 */
	private void configPhone() {
		try {
			if (serverSocket == null) {
				serverSocket = new DatagramSocket(9876);
				receiveData = new byte[128];
				System.out.println("Setup server on port 9876");
			}
			DatagramPacket receivePacket = new DatagramPacket(receiveData, 10);
			serverSocket.setSoTimeout(2);
			try {
				while (true) {
					serverSocket.receive(receivePacket);
					String sentence = new String(receivePacket.getData());
					int receivedPitch = Integer.parseInt(sentence.substring(0, 3));
					requestedPitch = (float) (Math.toRadians(receivedPitch) / 4.0);
					int receivedRoll = Integer.parseInt(sentence.substring(3, 6));
					requestedHeading = (float) (heading + Math.toRadians(receivedRoll) / 4.0);
					requestedSpeedFactor = (float) (Integer.parseInt(sentence.substring(6, 9)) / 10.0);
					stage = sentence.substring(9, 10);
				}
			} catch (SocketTimeoutException ignore) {
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Re-scale the requestedSSquared
		requestedSSquared *= requestedSpeedFactor;

		switch (stage) {
		case "O":
			configTakeOff();
			break;
		case "L":
			configLand();
			break;
		case "T":
			configTaxi(requestedSpeedFactor);
			break;
		default:
			configFly(requestedSpeedFactor);
			break;
		}
	}

	
	// *** HELPER METHODS *** //
	/**
	 * Check if the temporally 'averageWingInclination' exceeds the maximum angle of attack
	 */
	public void checkAverageWingInclinationMaxAOA() {
		Vector3f axis = new Vector3f(1, 0, 0);
		Vector3f attackVector = new Vector3f(0, (float) sin(averageWingInclination + maxAdjustInclination),
				-(float) cos(averageWingInclination + maxAdjustInclination));
		Vector3f normal = Vector3f.cross(axis, attackVector, null);
		AOA = (float) -Math.atan2(Vector3f.dot(projAirSpeed, normal), Vector3f.dot(projAirSpeed, attackVector));
		while (Math.abs(AOA) >= Math.abs(maxAOA) * 0.99f && Math.abs(averageWingInclination) > 0.01) {
			if (AOA > 0)
				averageWingInclination -= 0.01f; // 0.01 rad equals 0.57 degrees
			else
				averageWingInclination += 0.01f;

			if (averageWingInclination > maxAOA)
				averageWingInclination = -maxAOA;
			else if (averageWingInclination < -maxAOA)
				averageWingInclination = maxAOA;

			attackVector = new Vector3f(0, (float) sin(averageWingInclination + maxAdjustInclination),
					-(float) cos(averageWingInclination + maxAdjustInclination));
			AOA = (float) -Math.atan2(Vector3f.dot(projAirSpeed, normal), Vector3f.dot(projAirSpeed, attackVector));
		}
	}

	/**
	 * Get the inclination of the left and the right front wing of the drone to introduce the requested roll
	 * 
	 * @param averageInclination
	 * @param adjustInclination
	 * @param liftSlopeConstant
	 * @param sSquared
	 * 
	 * @return   The inclination of the left and right front wings of the drone
	 */
	float[] getLeftAndRightAdjIncl(float averageInclination, float adjustInclination, float liftSlopeConstant,
			float sSquared) {
		float EPSILON = precision;
		float leftIncl = (averageInclination - adjustInclination);
		float rightIncl = (averageInclination + adjustInclination);
		float initForceWings = 2 * wingLift(averageInclination, liftSlopeConstant, sSquared);

		float maxAdjIncl = Math.abs(maxAOA - Math.abs(averageInclination));
		if (maxAdjIncl > 0.25f)
			maxAdjIncl = 0.25f;

		float a = -maxAdjIncl;
		float fa = wingLift(leftIncl + a, liftSlopeConstant, sSquared)
				+ wingLift(rightIncl + a, liftSlopeConstant, sSquared) - initForceWings;
		float b = maxAdjIncl;
		float fb = wingLift(leftIncl + b, liftSlopeConstant, sSquared)
				+ wingLift(rightIncl + b, liftSlopeConstant, sSquared) - initForceWings;

		if (fa * fb > 0) {
			return new float[] { leftIncl, rightIncl };
		}

		while ((b - a) > EPSILON) {
			if ((wingLift(leftIncl + (b + a) / 2, liftSlopeConstant, sSquared)
					+ wingLift(rightIncl + (b + a) / 2, liftSlopeConstant, sSquared) - initForceWings) > 0)
				b = (b + a) / 2;
			else
				a = (b + a) / 2;
		}

		return new float[] { leftIncl + (b + a) / 2, rightIncl + (b + a) / 2 };
	}

	/**
	 * Restore the average adjustInclination after correcting the left and right inclination
	 * 
	 * @param averageInclination
	 * @param requestedHeading
	 * @param liftSlopeConstant
	 * @param sSquared
	 * 
	 * @return   The adjusted inclination of the front two wings
	 */
	private float adjustInclinationToMatchHeading(float averageInclination, float requestedHeading,
			float liftSlopeConstant, float sSquared) {
		float angularVelocity = (roll - previousRoll) / deltaTimeElapsed;
		float requestedRoll;
		float requestedAngularVelocity;

		// Decide which is the fastest way to go to the requested heading
		float deltaHeading = (requestedHeading - heading);
		if (deltaHeading > Math.PI) deltaHeading -= 2 * Math.PI;
		else if (deltaHeading < -Math.PI) deltaHeading += 2 * Math.PI;

		float deltaHeadingFactor = (float) ((1.0/15.0) * (deltaHeading * 180 / Math.PI));		
		if (deltaHeadingFactor > 1)	deltaHeadingFactor = 1;
		else if (deltaHeadingFactor < -1) deltaHeadingFactor = -1;

		requestedRoll = (float) maxRoll * deltaHeadingFactor;
		if (requestedRoll > maxRoll) requestedRoll = maxRoll;
		else if (requestedRoll < -maxRoll) requestedRoll = maxRoll;

		requestedAngularVelocity = (float) ((requestedRoll - roll) / (20 * deltaTimeElapsed));
		if (requestedAngularVelocity > 0.5) requestedAngularVelocity = (float) (0.5);
		else if (requestedAngularVelocity < -0.5) requestedAngularVelocity = (float) (-0.5);

		if (ENABLE_LOGGING && DRONE_STAT_LOGGING) {
			System.out.println("---requestedAngularVelocity: " + requestedAngularVelocity);
			System.out.println("---requestedRoll: " + Math.toDegrees(requestedRoll));
		}

		// Stabilize towards the requestedAngularVelocity
		return findInclinationAngularVelocity(angularVelocity, requestedAngularVelocity,
				averageInclination, sSquared, (requestedHeading - heading));
	}

	/**
	 * Compute the needed inclination to introduce a requested angular velocity
	 * 
	 * @param angularVelocity
	 * @param requestedAngularVelocity
	 * @param averageInclination
	 * @param sSquared
	 * @param deltaHeading
	 * 
	 * @return   The needed inclination to introduce the requested angular velocity
	 */
	private float findInclinationAngularVelocity(float angularVelocity, float requestedAngularVelocity,
			float averageInclination, float sSquared, float deltaHeading) {
		float EPSILON = precision;

		float a = -(maxAOA - Math.abs(averageInclination));
		float Fa = forceEquationAdjustInclination(averageInclination, a, sSquared, angularVelocity,
				requestedAngularVelocity);
		float b = (maxAOA - Math.abs(averageInclination));
		float Fb = forceEquationAdjustInclination(averageInclination, b, sSquared, angularVelocity,
				requestedAngularVelocity);

		if (Fa * Fb > 0) {
			if (requestedAngularVelocity > angularVelocity)
				return maxAdjustInclination;
			else
				return -maxAdjustInclination;
		}

		while ((b - a) > EPSILON) {
			if (forceEquationAdjustInclination(averageInclination, (b + a) / 2, sSquared, angularVelocity,
					requestedAngularVelocity) > 0)
				b = (b + a) / 2;
			else
				a = (b + a) / 2;
		}

		float result = (b + a) / 2;

		if (result > maxAdjustInclination) return maxAdjustInclination;
		else if (result < -maxAdjustInclination) return -maxAdjustInclination;
		else return result;
	}

	/**
	 * The main force equation used to calculate the wing inclinations
	 * 
	 * @param averageInclination
	 * @param adjustInclination
	 * @param sSquared
	 * @param angularVelocity
	 * @param requestedAngularVelocity
	 * 
	 * @return   The result of the force equation
	 */
	private float forceEquationAdjustInclination(float averageInclination, float adjustInclination, float sSquared,
			float angularVelocity, float requestedAngularVelocity) {
		// The minus sign is because the direction of the forces are opposite
		return (float) wingLift(averageInclination + adjustInclination, wingLiftSlope, sSquared) - 
				wingLift(averageInclination - adjustInclination, wingLiftSlope, sSquared)
				- (requestedAngularVelocity - angularVelocity) * getWingMass() * getWingX() / deltaTimeElapsed;
	}

	/**
	 * Determine which average inclination the drone must have to stay within the horizontal plane
	 * 
	 * @param liftSlopeConstant
	 * @param sSquared
	 * @param projGravity
	 * @param requestedPitch
	 * 
	 * @return   The average inclination the drone must have to stay in a given horizontal plane
	 */
	private float findInclinationZeroForceDroneY(float liftSlopeConstant, float sSquared, Vector3f projGravity,
			float requestedPitch) {
		float EPSILON = precision;

		float elevationFactor = (float) (Math.pow((requestedPitch - pitch) / (Math.PI / 60), 3));

		if (elevationFactor < -1) elevationFactor = -1;
		else if (elevationFactor > 1) elevationFactor = 1;

		if (ENABLE_LOGGING && DRONE_STAT_LOGGING)
			System.out.println("---elevationFactor: " + elevationFactor);

		float a = 0;
		float fa = totalForceDroneY(a, liftSlopeConstant, sSquared);
		float b = maxAOA;
		float fb = totalForceDroneY(b, liftSlopeConstant, sSquared);

		if (fa * fb > 0) {
			a = -maxAOA;
			fa = totalForceDroneY(a, liftSlopeConstant, sSquared);
			b = 0;
			fb = totalForceDroneY(b, liftSlopeConstant, sSquared);

			if (elevationFactor == -1) {
				if (ENABLE_LOGGING && ERROR_LOGGING) System.out.println("ERROR: FREE-FALL");
				return 0;
			} else if (fa * fb > 0) { // i.e. the drone's velocity is to low
				if (ENABLE_LOGGING && ERROR_LOGGING)
					System.out.println("ERROR: NO ZERO FOUND! (inclination)");
				if (requestedPitch == 0) { 
					// In this case, the method is in its second iteration, and still failed
					if (ENABLE_LOGGING && ERROR_LOGGING) System.out.println("ERROR: FREE-FALL");
					return 0;
				} else
					return findInclinationZeroForceDroneY(liftSlopeConstant, sSquared, projGravity, 0);
			}
		}

		while ((b - a) > EPSILON) {
			if (totalForceDroneY((b + a) / 2, liftSlopeConstant, sSquared) > 0)
				b = (b + a) / 2;
			else a = (b + a) / 2;
		}

		return adjustInclinationPitch((a + b) / 2, elevationFactor, liftSlopeConstant, sSquared);
	}

	/**
	 * Adjust the average wing inclination to cope with the requested pitch
	 * 
	 * @param defaultInclination
	 * @param elevationFactor
	 * @param liftSlopeConstant
	 * @param sSquared
	 * 
	 * @return   A perturbation on the previous average wing inclination to hangle the requested pitch
	 */
	private float adjustInclinationPitch(float defaultInclination, float elevationFactor, float liftSlopeConstant,
			float sSquared) {
		float EPSILON = precision;

		float defaultLift = wingLift(defaultInclination, liftSlopeConstant, sSquared);

		float requestedLift;
		if (Math.abs(pitch - requestedPitch) < Math.PI / 36) // 5 degrees
			requestedLift = (float) (defaultLift + getTotalMass() * gravity * elevationFactor / 5.0);
		else
			requestedLift = (float) (defaultLift + getTotalMass() * gravity * elevationFactor / 3.0);

		float a;
		float b;
		if (elevationFactor > 0) {
			a = defaultInclination;
			b = maxAOA;
		} else {
			a = -maxAOA;
			b = defaultInclination;
		}

		float Fa = wingLift(a, liftSlopeConstant, sSquared) - requestedLift;
		float Fb = wingLift(b, liftSlopeConstant, sSquared) - requestedLift;
		if (Fa * Fb > 0) { // The target can't be reached
			return 0;
		}

		while ((b - a) > EPSILON) {
			if (wingLift((b + a) / 2, liftSlopeConstant, sSquared) - requestedLift > 0)
				b = (b + a) / 2;
			else
				a = (b + a) / 2;
		}

		return (b + a) / 2;
	}

	/**
	 * Adjust the thrust to fly with the requested speed
	 * 
	 * @param inclinationWings
	 * @param liftSlopeConstant
	 * @param sSquared
	 * @param projGravity
	 * 
	 * @return   The adjusting-thrust
	 */
	private float findThrustZeroForceDroneZ(float inclinationWings, float liftSlopeConstant, float sSquared,
			Vector3f projGravity) {
		float thrust = (float) (2 * inclinationWings * Math.sin(inclinationWings) * liftSlopeConstant * sSquared
				+ projGravity.z);
		float requestedAcceleration;
		float deltaThrust;

		if (deltaTimeElapsed == 0)
			requestedAcceleration = 0;
		// The factor 3 is there because the requestedAcceleration will be
		// reached in 20 frames assuming 60 FPS (so in 0.33 seconds)
		else
			requestedAcceleration = (requestedSSquared - sSquared) * deltaTimeElapsed * 3;

		deltaThrust = requestedAcceleration * getTotalMass();
		thrust += deltaThrust;

		if (thrust > maxThrust) return maxThrust;
		else if (thrust < 0) return 0;
		else return thrust;
	}
	
	/**
	 * Calculate the lift generated by the left wing
	 * 
	 * @param inclination
	 * @param liftSlopeConstant
	 * @param sSquared
	 * 
	 * @return   The lift generated by the left wing
	 */
	private float wingLift(float inclination, float liftSlopeConstant, float sSquared) {
		return (float) (inclination * Math.cos(inclination) * liftSlopeConstant * sSquared);
	}

	/**
	 * Calculate the total force that works on the drone in the y-direction
	 * 
	 * @param inclination
	 * @param liftSlopeConstant
	 * @param sSquared
	 * 
	 * @return   The total force in the y-direction
	 */
	private float totalForceDroneY(float inclination, float liftSlopeConstant, float sSquared) {
		Matrix4f toDrone = getWorldToDroneTransformationMatrix(heading, pitch, roll);

		float wingLiftY = (float) 2 * wingLift(inclination, liftSlopeConstant, sSquared);

		Vector3f gravityVector = transformVector(toDrone, new Vector3f(0, -getTotalMass() * gravity, 0));
		float gravityY = (float) (gravityVector.y);

		return (wingLiftY + gravityY);
	}

	/**
	 * Get the drone its total mass
	 * 
	 * @return   The drone its total mass
	 */
	private float getTotalMass() {
		return (engineMass + 2 * wingMass + tailMass);
	}

	/**
	 * @return   The drone its total wing mass
	 */
	private float getWingMass() {
		return wingMass;
	}

	/**
	 * @return   The length of the drone its wings
	 */
	private float getWingX() {
		return wingX;
	}

	/**
	 * @param tailMass
	 * @param tailSize
	 * @param engineMass
	 * 
	 * @return   The position of the drone its engine
	 */
	private float calculateEnginePos(float tailMass, float tailSize, float engineMass) {
		return -(tailMass * tailSize) / engineMass;
	}

	/**
	 * Transform the world coordinates to the drone's view
	 * 
	 * @param heading
	 * @param pitch
	 * @param roll
	 * 
	 * @return   The transformed matrix
	 */
	private Matrix4f getWorldToDroneTransformationMatrix(float heading, float pitch, float roll) {
		return Matrix4f.invert(getDroneToWorldTransformationMatrix(heading, pitch, roll), null);
	}

	/**
	 * Transform the drone's coordinates to the world's view
	 * 
	 * @param heading
	 * @param pitch
	 * @param roll
	 * 
	 * @return   The transformed matrix
	 */
	private Matrix4f getDroneToWorldTransformationMatrix(float heading, float pitch, float roll) {
		Matrix4f matrix = new Matrix4f();
		matrix.setIdentity();

		Matrix4f.rotate((float) heading, new Vector3f(0, 1, 0), matrix, matrix);
		Matrix4f.rotate((float) pitch, new Vector3f(1, 0, 0), matrix, matrix);
		Matrix4f.rotate((float) roll, new Vector3f(0, 0, 1), matrix, matrix);

		return matrix;
	}

	/**
	 * Get the vector from the transform-matrix
	 * 
	 * @param matrix
	 * @param vector
	 * 
	 * @return   The transformed vector
	 */
	private Vector3f transformVector(Matrix4f matrix, Vector3f vector) {
		Vector4f vector4 = new Vector4f(vector.x, vector.y, vector.z, 1);
		Matrix4f.transform(matrix, vector4, vector4);

		return new Vector3f(vector4.x, vector4.y, vector4.z);
	}

	/**
	 * Print the vector
	 * 
	 * @param name
	 * @param vector
	 */
	public static void printVector(String name, Vector3f vector) {
		System.out.println(name + ": [" + vector.x + ", " + vector.y + ", " + vector.z + "]");
	}

	/**
	 * @return   The drone's speed
	 */
	public float getSSquared() {
		return sSquared;
	}
	
	/**
	 * Print the used parameters and variables
	 * 
	 * @param leftAndRightInclination
	 */
	private void print(float[] leftAndRightInclination) {
		if (DRONE_STAT_LOGGING) {
			System.out.println("---pitch: " + Math.toDegrees(pitch));
			System.out.println("---requestedPitch: " + Math.toDegrees(requestedPitch));
			System.out.println("---heading: " + Math.toDegrees(heading));
			System.out.println("---requestedHeading: " + Math.toDegrees(requestedHeading));
			System.out.println("---roll: " + Math.toDegrees(roll));
			System.out.println("---inclination: " + Math.toDegrees(averageWingInclination));
			System.out.println("---adjustInclination: " + Math.toDegrees(adjustInclination));
			System.out.println("---leftIncl: " + Math.toDegrees(leftAndRightInclination[0]));
			System.out.println("---rightIncl: " + Math.toDegrees(leftAndRightInclination[1]));
			System.out.println("---sSquared: " + sSquared);
			System.out.println("---requestedSSquared: " + requestedSSquared);
			System.out.println("---thrust: " + thrust);
			if (thrust == maxThrust)
				System.out.println("------maxThrust reached");
			System.out.println("---frontBrake: " + frontBrake);
			System.out.println("---leftBrake: " + leftBrake);
			System.out.println("---rightBrake: " + rightBrake);
		} else if (ESSENTIAL_LOGGING) {
			System.out.println("DRONE STATS - ESSENTIALS:");
			System.out.println("---pitch: " + Math.toDegrees(pitch));
			System.out.println("---requestedPitch: " + Math.toDegrees(requestedPitch));
			System.out.println("---heading: " + Math.toDegrees(heading));
			System.out.println("---requestedHeading: " + Math.toDegrees(requestedHeading));
			System.out.println("---sSquared: " + sSquared);
			System.out.println("---requestedSSquared: " + requestedSSquared);
		}
	}
	

	// *** UNUSED HELPER METHODS *** //
	private float findInclinationZeroForceY(float thrust, float liftSlopeConstant, float sSquared,
			Vector3f projGravity) {
		return findInclinationZeroForceDroneY(liftSlopeConstant, sSquared, projGravity, 0.1f);
	}

	private float totalForceX(float inclination, float thrust, float liftSlopeConstant, float sSquared) {
		Matrix4f toWorld = getDroneToWorldTransformationMatrix(heading, pitch, roll);

		Vector3f thrustUnitVector = transformVector(toWorld, new Vector3f(0, 0, -1));
		float thrustX = (float) (thrust * thrustUnitVector.x);

		Vector3f wingNormalUnitVector = transformVector(toWorld,
				new Vector3f(0, (float) Math.cos(inclination), (float) Math.sin(inclination)));
		float wingLiftX = (float) ((2 * inclination * liftSlopeConstant * sSquared) * wingNormalUnitVector.x);

		return (thrustX + wingLiftX);
	}

	private float totalForceY(float inclination, float thrust, float liftSlopeConstant, float sSquared) {
		Matrix4f toWorld = getDroneToWorldTransformationMatrix(heading, pitch, roll);

		Vector3f thrustUnitVector = transformVector(toWorld, new Vector3f(0, 0, -1));
		float thrustY = (float) (thrust * thrustUnitVector.y);

		Vector3f wingNormalUnitVector = transformVector(toWorld,
				new Vector3f(0, (float) Math.cos(inclination), (float) Math.sin(inclination)));
		float wingLiftY = (float) ((2 * inclination * liftSlopeConstant * sSquared) * wingNormalUnitVector.y);

		float gravityY = (-getTotalMass() * gravity);

		return (thrustY + wingLiftY + gravityY);
	}

	private float totalForceZ(float inclination, float thrust, float liftSlopeConstant, float sSquared) {
		Matrix4f toWorld = getDroneToWorldTransformationMatrix(heading, pitch, roll);

		Vector3f thrustUnitVector = transformVector(toWorld, new Vector3f(0, 0, -1));
		float thrustZ = (float) (thrust * thrustUnitVector.z);

		Vector3f wingNormalUnitVector = transformVector(toWorld,
				new Vector3f(0, (float) Math.cos(inclination), (float) Math.sin(inclination)));
		float wingLiftZ = (float) ((2 * inclination * liftSlopeConstant * sSquared) * wingNormalUnitVector.z);

		return (thrustZ + wingLiftZ);
	}

	private float totalForceDroneX() {
		Matrix4f toDrone = getWorldToDroneTransformationMatrix(heading, pitch, roll);

		Vector3f gravityVector = transformVector(toDrone, new Vector3f(0, -getTotalMass() * gravity, 0));

		return (float) gravityVector.x;
	}

	private float totalForceDroneZ(float inclinationWings, float thrust, float liftSlopeConstant, float sSquared) {
		Matrix4f toDrone = getWorldToDroneTransformationMatrix(heading, pitch, roll);

		float wingLiftZ = (float) (2 * inclinationWings * liftSlopeConstant * sSquared * Math.sin(inclinationWings));

		Vector3f gravityVector = transformVector(toDrone, new Vector3f(0, -getTotalMass() * gravity, 0));
		float gravityZ = (float) gravityVector.z;

		return (-thrust + wingLiftZ + gravityZ);
	}

}
