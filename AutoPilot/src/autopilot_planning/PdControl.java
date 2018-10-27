package autopilot_planning;

import autopilot_utilities.Point3D;
import interfaces.AutopilotInputs;

public interface PdControl {
	
	// Print booleans
	static boolean ENABLE_LOGGING = false;
	static boolean printExtraPoints = true;
	static boolean printPD = true;
	static boolean printDubins = true;
	
	/**
	 * The main method using a PD-controller based on position
	 * 
	 * @param inputs   The drone inputs
	 * @param deltaTimeElapsed
	 * @param stage   Defines in which stage the drone is (first circle, straight mid-line, or second circle)
	 * @param dubinsIdentifier   A string saying if the drone flies to the left or to the right
	 * @param positionError
	 * @param prevPositionError
	 * 
	 * @return The amount of deltaHeading the drone must get to result in the requested roll
	 */
	static float getPdPosition(AutopilotInputs inputs, float deltaTimeElapsed, int stage, String dubinsIdentifier, float positionError, float prevPositionError) {

		// TODO --> What is the weight of the dirivative-factor? --> Find correct coefficient
		float derivativeFactor = 3.0f;
		if (stage == 1)
			derivativeFactor = 1.0f;
		
		// Return the drone its own heading with the requested direction
		float pdPositionFault = (float) (proportionalPosition(positionError) + derivativeFactor * derivativePosition(positionError, prevPositionError, deltaTimeElapsed));
		if (pdPositionFault > 20) pdPositionFault = 20;
		else if (pdPositionFault < -20) pdPositionFault = -20;
		String direction = "S";
		switch (stage) {
		case 0:
			direction = dubinsIdentifier.substring(0,1);
			break;
		case 2:
			direction = dubinsIdentifier.substring(2,3);
			break;
		}
		
		if (ENABLE_LOGGING && printPD) {
			System.out.println("PD - POSITION");
			System.out.println("---default pdPositionFault: "+pdPositionFault);
			System.out.println("---proportional: "+proportionalPosition(positionError));
			System.out.println("---derivative: "+ derivativeFactor *derivativePosition(positionError, prevPositionError, deltaTimeElapsed));
		}
			
		if (direction.equals("L"))
			pdPositionFault += 11.5;
		else if (direction.equals("R"))
			pdPositionFault -= 11.5;
		
		return (float) (inputs.getHeading() + pdPositionFault*(Math.PI/180));
	}
	
	/**
	 * Define the position-error from the drone
	 * 
	 * @param positionError
	 * 
	 * @return   The amount of meters the drone is away from its requested path
	 */
	static float proportionalPosition(float positionError) {
		return positionError;
	}
	
	/**
	 * Get the derivative of the last two position-errors
	 * 
	 * @param positionError
	 * @param prevPositionError
	 * 
	 * @return   The rico of the line interpolating the two given error points (distance in between equals deltaElapsedTime)
	 */
	static float derivativePosition(float positionError, float prevPositionError, float deltaElapsedTime) {
		// TODO --> What is the weight of deltaElapsedTime?
		return (float) Math.atan((positionError - prevPositionError)/deltaElapsedTime);
	}
	
	
	// HELPER PD DISTANCE
	
	/**
	 * Get the distance from drone to the requested path depending on which stage the drone is in
	 * 
	 * @param inputs
	 * @param stage
	 * @param dubinsIdentifier
	 * @param firstCircleCenter
	 * @param secondCircleCenter
	 * @param firstCircleKeyPoint
	 * @param secondCircleKeyPoint
	 * 
	 * @return   The distance between the drone and the requested path
	 */
	static float getDistanceFault(AutopilotInputs inputs, int stage, String dubinsIdentifier, Point3D firstCircleCenter, Point3D secondCircleCenter, Point3D firstCircleKeyPoint, Point3D secondCircleKeyPoint, Point3D secondTarget, Point3D dronePosStart) {
		switch (stage) {
		case 0:
			return getDistanceFaultFirstCircle(inputs, dubinsIdentifier, firstCircleCenter);
		case 1:
			return getDistanceFaultOneCube(inputs, secondCircleKeyPoint, dronePosStart);
		case 2:
			return getDistanceFaultSecondCircle(inputs, dubinsIdentifier, secondCircleCenter);	
		case 3:
			return getDistanceFaultOneCube(inputs, secondTarget, dronePosStart);
		default:
			return 0;
		}
	}
	
	/**
	 * Calculate the fault distance when in stage 0
	 * 
	 * @param inputs
	 * @param dubinsIdentifier
	 * @param firstCircleCenter
	 * 
	 * @return   The distance to the first circle its circumference
	 */
	static float getDistanceFaultFirstCircle(AutopilotInputs inputs, String dubinsIdentifier, Point3D firstCircleCenter) {
		String direction = dubinsIdentifier.substring(0, 1);
		if (direction.equals("L"))
			return -(400 - Distance.distanceToHor(firstCircleCenter, inputs));
		else
			return (400 - Distance.distanceToHor(firstCircleCenter, inputs));
	}

	/**
	 * Calculate the fault distance when in stage 1
	 * 
	 * @param inputs
	 * @param firstCircleKeyPoint
	 * @param secondCircleKeyPoint
	 * 
	 * @return   The distance to the line interpolated between the two circle key points
	 */
	static float getDistanceFaultStraightLine(AutopilotInputs inputs, Point3D firstCircleKeyPoint, Point3D secondCircleKeyPoint) {
		float numerator = (float) Math.abs((secondCircleKeyPoint.getZ() - firstCircleKeyPoint.getZ())*inputs.getX() - 
				(secondCircleKeyPoint.getX() - firstCircleKeyPoint.getX())*inputs.getZ() + 
				secondCircleKeyPoint.getX()*firstCircleKeyPoint.getZ() - 
				secondCircleKeyPoint.getZ()*firstCircleKeyPoint.getX());
		
		float distance = Distance.distanceToHor(firstCircleKeyPoint, secondCircleKeyPoint);
		if (distance == 0) distance++;
		float difference = (float) (numerator/distance);
		
		float sign = (float) ((float) (inputs.getX() - firstCircleKeyPoint.getX()) * (secondCircleKeyPoint.getZ() - firstCircleKeyPoint.getZ()) - 
				(inputs.getZ() - firstCircleKeyPoint.getZ()) * (secondCircleKeyPoint.getX() - firstCircleKeyPoint.getX()));
		
		if (sign > 0) difference *= -1;	// Drone on the right of the line
		return difference;
	}
	
	/**
	 * Calculate the fault distance when in stage 2
	 * 
	 * @param inputs
	 * @param dubinsIdentifier
	 * @param secondCircleCenter
	 * 
	 * @return   The distance to the second circle its circumference
	 */
	static float getDistanceFaultSecondCircle(AutopilotInputs inputs, String dubinsIdentifier, Point3D secondCircleCenter) {
		String direction = dubinsIdentifier.substring(2, 3);
		if (direction.equals("L"))
			return -(400 - Distance.distanceToHor(secondCircleCenter, inputs));
		else
			return (400 - Distance.distanceToHor(secondCircleCenter, inputs));
	}
	
	/**
	 * Calculate the fault distance when in the last stage
	 * 
	 * @param inputs
	 * @param target
	 * @param nextTarget
	 * 
	 * @return   The distance to the line interpolated between the first and the second target
	 */
	static float getDistanceFaultStraightEndLine(AutopilotInputs inputs, Point3D target, Point3D nextTarget) {
		float numerator = (float) Math.abs((nextTarget.getZ() - target.getZ())*inputs.getX() - 
				(nextTarget.getX() - target.getX())*inputs.getZ() + 
				nextTarget.getX()*target.getZ() - 
				nextTarget.getZ()*target.getX());
		
		float distance = Distance.distanceToHor(target, nextTarget);
		if (distance == 0) distance++;
		float difference = (float) (numerator/distance);
		float sign = (float) ((float) (inputs.getX() - target.getX()) * (nextTarget.getZ() - target.getZ()) - 
				(inputs.getZ() - target.getZ()) * (nextTarget.getX() - target.getX()));
		
		if (sign > 0) return -difference;	// Drone on the right of the line
		else return difference;	// Drone on the left of the line	
	}
	
	/**
	 * The PD-controller used when only one cube is taken into account
	 * 
	 * @param inputs
	 * @param target
	 * @param deltaTimeElapsed
	 * @param positionError
	 * @param prevPositionError
	 * 
	 * @return   The difference in heading needed to introduce the requested roll
	 */
	static float getPdPositionOneCube(AutopilotInputs inputs, Point3D target, float deltaTimeElapsed, float positionError, float prevPositionError) {
		// Return the drone its own heading with the requested direction
		float pidPositionFault = (float) (proportionalPosition(positionError) + 3.0*derivativePosition(positionError, prevPositionError, deltaTimeElapsed));
		if (pidPositionFault > 20) pidPositionFault = 20;
		else if (pidPositionFault < -20) pidPositionFault = -20;

		if (ENABLE_LOGGING && printPD) {
			System.out.println("PID - POSITION ONE CUBE");
			System.out.println("---proportional: "+proportionalPosition(positionError));
			System.out.println("---derivative: "+ 3.0*derivativePosition(positionError, prevPositionError, deltaTimeElapsed));
			System.out.println("---default pidPositionFault: "+pidPositionFault);
		}
	
		return (float) (inputs.getHeading() + pidPositionFault*(Math.PI/180));
	}
	
	/**
	 * Get the fault distance when there is only one cube in focus (not dubins)
	 * 
	 * @param inputs
	 * @param target
	 * @param dronePosStart
	 * 
	 * @return   The distance to the line interpolated between the target and the dronePosStart
	 */
	static float getDistanceFaultOneCube(AutopilotInputs inputs, Point3D target, Point3D dronePosStart) {
		float numerator = (float) Math.abs((target.getZ() - dronePosStart.getZ())*inputs.getX() - 
				(target.getX() - dronePosStart.getX())*inputs.getZ() + 
				target.getX()*dronePosStart.getZ() - 
				target.getZ()*dronePosStart.getX());
		float distance = Distance.distanceToHor(dronePosStart, target);
		if (distance == 0) distance++;
		float difference = (float) (numerator/distance);
		float sign = (float) ((float) (inputs.getX() - dronePosStart.getX()) * (target.getZ() - dronePosStart.getZ()) - 
				(inputs.getZ() - dronePosStart.getZ()) * (target.getX() - dronePosStart.getX()));
		if (sign > 0) return -difference;	// Drone on the right of the line
		else return difference;	// Drone on the left of the line
	}
	
	/**
	 * Use the PD controller to stay on a given circle
	 * 
	 * @param inputs
	 * @param deltaTimeElapsed
	 * @param positionError
	 * @param prevPositionError
	 * 
	 * @return
	 */
	static float getPdPositionCircleClockwise(AutopilotInputs inputs, float deltaTimeElapsed, float positionError, float prevPositionError) {		
		// Return the drone its own heading with the requested direction
		float pdPositionFault = (float) (proportionalPosition(positionError) + 3.0 * derivativePosition(positionError, prevPositionError, deltaTimeElapsed));
		if (pdPositionFault > 20) pdPositionFault = 20;
		else if (pdPositionFault < -20) pdPositionFault = -20;
		
		// The drone will ALWAYS fly clockwise
		pdPositionFault -= 11.5;
		
		if (ENABLE_LOGGING && printPD) {
			System.out.println("PD - POSITION");
			System.out.println("---proportional: "+proportionalPosition(positionError));
			System.out.println("---derivative: "+ 3.0 *derivativePosition(positionError, prevPositionError, deltaTimeElapsed));
			System.out.println("---default pdPositionFault: "+pdPositionFault);
		}
		
		return (float) (inputs.getHeading() + pdPositionFault*(Math.PI/180));
	}
	
	static float getDistanceFaultCircle(AutopilotInputs inputs, Point3D circleCenter, float rho) {
		return (rho - Distance.distanceToHor(circleCenter, inputs));	
	}
}
