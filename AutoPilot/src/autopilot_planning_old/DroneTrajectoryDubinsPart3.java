package autopilot_planning_old;

import java.util.List;

import autopilot_utilities.Path3D;
import autopilot_utilities.Point3D;
import autopilot_utilities.Vector3f;

public class DroneTrajectoryDubinsPart3 extends DroneTrajectory {
	
	// Print booleans
	private boolean printExtraPoints = false;
	private boolean printPID = false;
	private boolean printDubins = false;
	private boolean printBooleans = false;
	
	// Member variables
	private int stage;
	private DubinsPath2D dubinsPath = null;
	private float endHeading;
	
	private List<Point3D> targetCoordinates;
	private Point3D firstCircleCenter;
	private Point3D secondCircleCenter;
	private Point3D firstCircleKeyPoint;
	private Point3D secondCircleKeyPoint;
	private Point3D target;
	private Point3D nextTarget;

	// PID using position
	private float positionError = 0;
	private float prevPositionError = 0;

	private boolean lastCube = false;
	private Point3D dronePosStart;
	
	private float maxFault = 50f;
	private boolean goDirectlyToNext = false;
	private boolean turnAround = false;
	
	
	@Override
	protected float[] calculateTrajectoryInternal(Path3D path, Vector3f dronePosition, Vector3f rotations, double velocity, double rho, boolean landingTrailSet, float deltaTimeElapsed) {
		float[] motion = new float[2];
		
		targetCoordinates = path.getCoordinates();
		
		if (dronePosition.y < 5)
			resetAll();
		else if (!goDirectlyToNext && targetCoordinates.size() > 1 && dronePosition.y > 50) {
			lastCube = false;

			if (Math.abs(positionError) > maxFault)
				goDirectlyToNext = true;
			
			// Initialization
			if (dubinsPath == null) {
				System.out.println("INIT DUBINS");
				if (!landingTrailSet)
					new PathReorden().reordenPath(path, dronePosition, (float)rotations.x);
				
				targetCoordinates = path.getCoordinates();

				target = targetCoordinates.get(0);
				nextTarget = targetCoordinates.get(1);
				Vector3f cubeVector = new Vector3f(nextTarget.getX() - target.getX(), 0, nextTarget.getZ() - target.getZ());
				endHeading = (float) Math.atan2(-cubeVector.x, -cubeVector.z);
				Vector3f startConfiguration = new Vector3f(-dronePosition.z, -dronePosition.x, rotations.x);
				Vector3f endConfiguration = new Vector3f(-target.getZ(), -target.getX(), endHeading);
		
				dubinsPath = new DubinsPath2D(startConfiguration, endConfiguration, rho);
				
				calculateCircleCenters(dronePosition, rotations, dubinsPath.type.identifier.toString());
				calculateCircleKeyPoints(dronePosition, rotations, dubinsPath.type.toString());
			}
			
			System.out.println();
			System.out.println("-->STAGE: "+stage+"<--");

			if (dubinsPath != null) {
				switch (stage) {
				case 0:	// First circle
					if (isReached(firstCircleKeyPoint, dronePosition, rotations)) stage++;
					break;
				case 1:	// Straight line
					if (isReached(secondCircleKeyPoint, dronePosition, rotations)) stage++;
					break;
				case 2:	// Last circle
					if (isReached(target, dronePosition, rotations)) stage++;
					break;
				case 3:
					if (isReached(nextTarget, dronePosition, rotations)) stage++;
					break;
				default:
					if (nextTarget != targetCoordinates.get(0) || distanceTo(nextTarget, dronePosition) > 100)
						resetAll();
					// Path is null --> Nothing to calculate!
					motion[0] = (float) rotations.y;
					motion[1] = (float) rotations.x;
					return motion;
				}
				
				// Handle the Pitch
				if (stage < 3)
					motion[0] = getPitch(dronePosition, target);
				else
					motion[0] = getPitch(dronePosition, nextTarget);
				motion[1] = getPidPosition(dronePosition, rotations);
				
				// TODO
				// In the case of a bad path --> Go naive!
				if (dubinsPath.type.params[0] > (5.0/4.0)*Math.PI && !landingTrailSet)
					goDirectlyToNext = true;
				
				if (printDubins) {
					System.out.println("DUBINS: " + dubinsPath.type.identifier.toString());
					System.out.println("--- length " + dubinsPath.getLength());
					System.out.println("--- params " + dubinsPath.type.params[0]);
					System.out.println("--- params " + dubinsPath.type.params[1]);
					System.out.println("--- params " + dubinsPath.type.params[2]);
				}

				if (printExtraPoints) {
					System.out.println("EXTRA POINTS");
					System.out.println("---firstCenter: ["+firstCircleCenter.getX()+",0,"+firstCircleCenter.getZ()+"]");
					System.out.println("---secondCenter: ["+secondCircleCenter.getX()+",0,"+secondCircleCenter.getZ()+"]");
					System.out.println("---firstKeyPoint: ["+firstCircleKeyPoint.getX()+",0,"+firstCircleKeyPoint.getZ()+"]");
					System.out.println("---secondKeyPoint: ["+secondCircleKeyPoint.getX()+",0,"+secondCircleKeyPoint.getZ()+"]");
					System.out.println("---target: ["+target.getX()+",0,"+target.getZ()+"]");
					System.out.println("---nextTarget: ["+nextTarget.getX()+",0,"+nextTarget.getZ()+"]");
				}
			}
		} else if (goDirectlyToNext || (targetCoordinates.size() == 1 && dronePosition.y > 15)) {
			if (targetCoordinates != null && targetCoordinates.size() != 0)
				target = targetCoordinates.get(0);
			motion = handleOnlyNextCube(dronePosition, rotations);
		} else {
			lastCube = false;
			// TODO
			System.out.println("ELSE - YET TO PROGRAM!");
			
			motion[0] = (float) (rotations.y + Math.PI/36);
			motion[1] = (float) rotations.x;
		}
		
		if (printBooleans) {
			System.out.println("BOOLEANS");
			System.out.println("---lastCube: "+lastCube);
			System.out.println("---goDirectlyToNext: "+goDirectlyToNext);
			System.out.println("---turnAround: "+turnAround);
			System.out.println("---landing: "+landingTrailSet);
		}
		
		
		return motion;
	}
	
	
	// Error methods
	private float[] handleOnlyNextCube(Vector3f dronePosition, Vector3f rotations) {
		System.out.println("GO DIRECTLY TO NEXT");
		float[] motion = new float[2];
		
		if (!lastCube || Math.abs(positionError) > 10)
			dronePosStart = new Point3D(dronePosition.x, dronePosition.y, dronePosition.z);

		// Check if drone is heading towards its target (delta heading < PI/4), otherwise keep updating dronePosStart
		Vector3f requestedVector = new Vector3f(
				target.getX() - dronePosition.x, 
				0, 
				target.getZ() - dronePosition.z);
		float deltaHeading = (float) (rotations.x - Math.atan2(-requestedVector.x, -requestedVector.z));
		if (deltaHeading > Math.PI) deltaHeading -= 2*Math.PI;
		else if (deltaHeading < -Math.PI) deltaHeading += 2*Math.PI;

		lastCube = true;
		if (Math.abs(deltaHeading) < Math.PI/4) {
			turnAround = false;
		} else if (Math.abs(deltaHeading) > Math.PI * 175 / 180) {
			turnAround = true;	// cube behind drone with 1 degree
			lastCube = false;
		}
		
		motion[0] = getPitch(dronePosition, target);
		if (turnAround) {
			System.out.println("ERROR: TURN AROUND");
			if (distanceTo(target, dronePosition) > 600) {
				if (rotations.z > Math.PI/90) motion[1] = (float) (rotations.x + Math.PI/16);	// roll of 33.75 degrees
				else motion[1] = (float) (rotations.x - Math.PI/16);
			} else {
				motion[1] = (float) rotations.x;
			}
		} else if (distanceTo(target, dronePosition) < 600 && Math.abs(deltaHeading) > Math.PI/2) {
			motion[1] = (float) rotations.x;
		} else if (deltaHeading > Math.PI/4) {
			motion[1] = (float) (rotations.x - Math.PI/16);	// 33.75 degrees
		} else if (deltaHeading < -Math.PI/4) {
			motion[1] = (float) (rotations.x + Math.PI/16);	// 33.75 degrees
		} else {
			motion[1] = getPidPositionOneCube(dronePosition, rotations);
		}
		
		if (printExtraPoints) {
			System.out.println("EXTRA POINTS");
			System.out.println("---dronePosStart: ["+dronePosStart.getX()+",0,"+dronePosStart.getZ()+"]");
			System.out.println("---target: ["+target.getX()+",0,"+target.getZ()+"]");
		}
		
		if (isReached(target, dronePosition, rotations)) 
				resetAll();
		
		return motion;
	}
	
	
	// INITIALISATION METHODS //
	
	private void calculateCircleCenters(Vector3f dronePosition, Vector3f rotations, String dubinsString) {
		String directionFirst = dubinsString.substring(0, 1);
		String directionSecond = dubinsString.substring(2, 3);
		float sigma;
		
		// Calculate first center
		if (directionFirst.equals("R"))
			sigma = (float) (rotations.x - Math.PI / 2);
		else // "L"
			sigma = (float) (rotations.x + Math.PI / 2);
		
		firstCircleCenter = new Point3D(
				dronePosition.x - 400 * Math.sin(sigma), 
				0,
				dronePosition.z - 400 * Math.cos(sigma));
		
		// Calculate second center
		if (directionSecond.equals("R"))
			sigma = (float) (endHeading - Math.PI / 2);
		else // "L"
			sigma = (float) (endHeading + Math.PI / 2);
		
		secondCircleCenter = new Point3D(
				target.getX() - 400 * Math.sin(sigma), 
				0,
				target.getZ() - 400 * Math.cos(sigma));
	}
	
	private void calculateCircleKeyPoints(Vector3f dronePosition, Vector3f rotations, String dubinsString) {
		// Get first key point
		double deltaAngle = dubinsPath.type.params[0];
		if (dubinsString.substring(0, 1).equals("L")) deltaAngle *= -1;
		Vector3f targetCubeVector = new Vector3f(
				dronePosition.x - firstCircleCenter.getX(), 
				0,
				dronePosition.z - firstCircleCenter.getZ());
		float centerTargetAngle = (float) Math.atan2(-targetCubeVector.x, -targetCubeVector.z);
		float totalAngle;
		if (dubinsPath.type.identifier.toString().substring(0, 1).equals("R"))
			totalAngle = (float) (centerTargetAngle - deltaAngle);
		else 
			totalAngle = (float) (centerTargetAngle + deltaAngle);
		firstCircleKeyPoint = new Point3D(
				firstCircleCenter.getX() - 400 * Math.sin(totalAngle), 
				0,
				firstCircleCenter.getZ() - 400 * Math.cos(totalAngle));
		
		// Get second key point
		deltaAngle = dubinsPath.type.params[2];
		if (dubinsString.substring(2, 3).equals("L")) deltaAngle *= -1;
		targetCubeVector = new Vector3f(
				target.getX() - secondCircleCenter.getX(), 
				0,
				target.getZ() - secondCircleCenter.getZ());
		centerTargetAngle = (float) Math.atan2(-targetCubeVector.x, -targetCubeVector.z);
		if (dubinsPath.type.identifier.toString().substring(2, 3).equals("L"))
			totalAngle = (float) (centerTargetAngle - deltaAngle);
		else 
			totalAngle = (float) (centerTargetAngle + deltaAngle);
		secondCircleKeyPoint = new Point3D(
				secondCircleCenter.getX() - 400 * Math.sin(totalAngle), 
				0,
				secondCircleCenter.getZ() - 400 * Math.cos(totalAngle));
	}
	
	
	// PID //
	
	/*
	 * The main-PID method using position
	 * 
	 * @return	Return the requested heading
	 */
	private float getPidPosition(Vector3f dronePosition, Vector3f rotations) {
		updatePositionError(dronePosition, rotations);

		if (printPID) {
			System.out.println("PID - POSITION");
			System.out.println("---proportional: "+proportionalPosition());
			System.out.println("---derivative: "+ 100 *derivativePosition());
			System.out.println("---integral: "+ 0);//0.5 * integralPosition());
		}
		
		// Return the drone its own heading with the requested direction
		float pidPositionFault = (float) (proportionalPosition() + 100*derivativePosition());// + 0.5 * integralPosition());
		if (pidPositionFault > 20) pidPositionFault = 20;
		else if (pidPositionFault < -20) pidPositionFault = -20;
		String direction = "S";
		switch (stage) {
		case 0:
			direction = dubinsPath.type.identifier.toString().substring(0,1);
			break;
		case 2:
			direction = dubinsPath.type.identifier.toString().substring(2,3);
			break;
		}
		
		if (printPID)
			System.out.println("---default pidPositionFault: "+pidPositionFault);
		
		if (direction.equals("L"))
			pidPositionFault += 11.5;
		else if (direction.equals("R"))
			pidPositionFault -= 11.5;
		
		return (float) (rotations.x + pidPositionFault*(Math.PI/180));
	}
	
	private float proportionalPosition() {
		return positionError;
	}
	
	private float derivativePosition() {
		return (float) Math.atan(positionError - prevPositionError);
	}
	
	/*
	 * The PID method using position when there is only one cube left
	 * 
	 * @return	Return the requested heading
	 */
	private float getPidPositionOneCube(Vector3f dronePosition, Vector3f rotations) {
		updatePositionErrorOneCube(dronePosition, rotations);

		if (printPID) {
			System.out.println("PID - POSITION ONE CUBE");
			System.out.println("---proportional: "+proportionalPosition());
			System.out.println("---derivative: "+ 100 *derivativePosition());
			System.out.println("---integral: "+ 0);//0.5 * integralPosition());
		}
		
		// Return the drone its own heading with the requested direction
		float pidPositionFault = (float) (proportionalPosition() + 100*derivativePosition());// + 0.5 * integralPosition());
		if (pidPositionFault > 20) pidPositionFault = 20;
		else if (pidPositionFault < -20) pidPositionFault = -20;
		
		if (printPID)
			System.out.println("---default pidPositionFault: "+pidPositionFault);
	
		return (float) (rotations.x + pidPositionFault*(Math.PI/180));	
	}
	
	
	// PITCH AND HEADING //
	
	private float getPitch(Vector3f dronePosition, Point3D target) {
		if (target != null) {
			float pitch;
			Vector3f requestedVector = new Vector3f(
					target.getX() - dronePosition.x, 
					target.getY() - dronePosition.y,
					target.getZ() - dronePosition.z);
			
			pitch = (float) Math.atan(requestedVector.y / 
					(Math.sqrt(requestedVector.z * requestedVector.z + requestedVector.x * requestedVector.x)));
			float shortPitch = (float) Math.atan(requestedVector.y / 100.0);

			if (shortPitch > Math.PI / 36) shortPitch = (float) Math.PI / 36;
			else if (shortPitch < -Math.PI / 36) shortPitch = (float) -Math.PI / 36;
			if (pitch > 0 && pitch < shortPitch) return shortPitch;
			if (pitch < 0 && pitch > shortPitch) return shortPitch;
			
			else return pitch;
		} else
			return (float) Math.PI/36;
	}
	
	
	// HELPER PID DISTANCE
	
	private void updatePositionError(Vector3f dronePosition, Vector3f rotations) {
		prevPositionError = positionError;
		positionError = (float) (getDistanceFault(dronePosition));

		if (printPID) {
			System.out.println("---positionError "+positionError);
			System.out.println("---prevPositionError "+prevPositionError);
		}
	}
	
	private void updatePositionErrorOneCube(Vector3f dronePosition, Vector3f rotations) {
		prevPositionError = positionError;
		positionError = (float) (getDistanceFaultOneCube(dronePosition));

		if (printPID) {
			System.out.println("---positionError "+positionError);
			System.out.println("---prevPositionError "+prevPositionError);
		}
	}
	
	private float getDistanceFault(Vector3f dronePosition) {
		switch (stage) {
		case 0:
			return getDistanceFaultFirstCircle(dronePosition);
		case 1:
			return getDistanceFaultStraightLine(dronePosition);
		case 2:
			return getDistanceFaultSecondCircle(dronePosition);	
		case 3:
			return getDistanceFaultStraightEndLine(dronePosition);
		default:
			return 0;
		}
	}
	
	private float getDistanceFaultFirstCircle(Vector3f dronePosition) {
		String direction = dubinsPath.type.identifier.toString().substring(0, 1);
		if (direction.equals("L"))
			return -(400 - distanceTo(firstCircleCenter, dronePosition));
		else
			return (400 - distanceTo(firstCircleCenter, dronePosition));
	}
	
	private float getDistanceFaultStraightLine(Vector3f dronePosition) {
		float numerator = (float) Math.abs((secondCircleKeyPoint.getZ() - firstCircleKeyPoint.getZ())*dronePosition.x - 
				(secondCircleKeyPoint.getX() - firstCircleKeyPoint.getX())*dronePosition.z + 
				secondCircleKeyPoint.getX()*firstCircleKeyPoint.getZ() - 
				secondCircleKeyPoint.getZ()*firstCircleKeyPoint.getX());
		float difference = (float) (numerator/distanceTo(firstCircleKeyPoint, secondCircleKeyPoint));
		
		float sign = (float) ((float) (dronePosition.x - firstCircleKeyPoint.getX()) * (secondCircleKeyPoint.getZ() - firstCircleKeyPoint.getZ()) - 
				(dronePosition.z - firstCircleKeyPoint.getZ()) * (secondCircleKeyPoint.getX() - firstCircleKeyPoint.getX()));
		if (sign > 0) return -difference;	// Drone on the right of the line
		else return difference;	// Drone on the left of the line
	}
	
	private float getDistanceFaultSecondCircle(Vector3f dronePosition) {
		String direction = dubinsPath.type.identifier.toString().substring(2, 3);
		if (direction.equals("L"))
			return -(400 - distanceTo(secondCircleCenter, dronePosition));
		else
			return (400 - distanceTo(secondCircleCenter, dronePosition));
	}
	
	private float getDistanceFaultStraightEndLine(Vector3f dronePosition) {
		float numerator = (float) Math.abs((nextTarget.getZ() - target.getZ())*dronePosition.x - 
				(nextTarget.getX() - target.getX())*dronePosition.z + 
				nextTarget.getX()*target.getZ() - 
				nextTarget.getZ()*target.getX());
		
		float difference = (float) (numerator/distanceTo(target, nextTarget));
		float sign = (float) ((float) (dronePosition.x - target.getX()) * (nextTarget.getZ() - target.getZ()) - 
				(dronePosition.z - target.getZ()) * (nextTarget.getX() - target.getX()));
		
		if (sign > 0) return -difference;	// Drone on the right of the line
		else return difference;	// Drone on the left of the line	
	}
	
	private float getDistanceFaultOneCube(Vector3f dronePosition) {
		float numerator = (float) Math.abs((target.getZ() - dronePosStart.getZ())*dronePosition.x - 
				(target.getX() - dronePosStart.getX())*dronePosition.z + 
				target.getX()*dronePosStart.getZ() - 
				target.getZ()*dronePosStart.getX());
		float difference = (float) (numerator/distanceTo(dronePosStart, target));
		
		float sign = (float) ((float) (dronePosition.x - dronePosStart.getX()) * (target.getZ() - dronePosStart.getZ()) - 
				(dronePosition.z - dronePosStart.getZ()) * (target.getX() - dronePosStart.getX()));
		if (sign > 0) return -difference;	// Drone on the right of the line
		else return difference;	// Drone on the left of the line
	}
	
	
	// HELPER METHODS //
	
	private boolean isReached(Point3D requestedPoint, Vector3f dronePosition, Vector3f rotations) {
		Vector3f targetPointVector = new Vector3f(
				requestedPoint.getX() - dronePosition.x, 
				0, 
				requestedPoint.getZ() - dronePosition.z);
		float reqHeading = (float) Math.atan2(-targetPointVector.x, -targetPointVector.z);
		double headingDifference = Math.min(Math.abs(rotations.x - reqHeading),
				Math.PI * 2 - Math.abs(rotations.x - reqHeading));
		
		return (distanceTo(requestedPoint, dronePosition) < 50 || // Successfully reached
				(headingDifference > Math.PI/2 && distanceTo(requestedPoint, dronePosition) < 200));	// Drone has past it
	}

	private float distanceTo(Point3D point, Point3D position) {
		return (float) Math.sqrt(Math.pow(point.getX() - position.getX(), 2) + Math.pow(point.getZ() - position.getZ(), 2));
	}

	private float distanceTo(Point3D point, Vector3f position) {
		return (float) Math.sqrt(Math.pow(point.getX() - position.x, 2) + Math.pow(point.getZ() - position.z, 2));
	}
	
	public void resetAll() {
		stage = 0;
		endHeading = 0;
		
		dubinsPath = null;
		
		targetCoordinates = null;
		firstCircleCenter = null;
		secondCircleCenter = null;
		firstCircleKeyPoint = null;
		secondCircleKeyPoint = null;
		target = null;
		nextTarget = null;
		
		lastCube = false;
		goDirectlyToNext = false;
		turnAround = false;
	}

}
