package autopilot_planning_old;

import autopilot_utilities.Path3D;
import autopilot_utilities.Point3D;
import autopilot_utilities.Vector3f;

/**
 * A class of drone trajectories in which the closest target is always flown to, without any kind
 *  of refinement.
 * 
 * @author	Team Saffier
 * @version	1.0
 */
public class DroneTrajectoryNaive2 extends DroneTrajectory {
	
	// Print booleans
	private boolean printExtraPoints = false;
	private boolean printPID = false;

	// PID using position
	private float positionError = 0;
	private float prevPositionError = 0;
	
	private Point3D target;
	private Point3D dronePosStart;
	private boolean turnAround = false;

	@Override
	protected float[] calculateTrajectoryInternal(Path3D path, Vector3f dronePosition, Vector3f rotations, double velocity, double rho, boolean landingTrailSet, float deltaTimeElapsed) {
		
		if (path != null && path.getCoordinates().size() > 0)
			target = path.getCoordinate(0);
		
		/*
		if (path != null && path.getCoordinates().size() > 0 && target != path.getCoordinates().get(0)) {
			if (path.getCoordinates().size() > 0 && !landingTrailSet)
				new PathReorden().reordenPath(path, dronePosition, (float)rotations.x);
			target = path.getCoordinates().get(0);
		}
		*/
		
		float[] motion = new float[2];

		if (Math.abs(positionError) > 10 || dronePosStart == null || dronePosition.y < 20)
			dronePosStart = new Point3D(dronePosition.x, dronePosition.y, dronePosition.z);

		if (target != null) {
			// Check if drone is heading towards its target (delta heading < PI/4), otherwise keep updating dronePosStart
			Vector3f requestedVector = new Vector3f(
					target.getX() - dronePosition.x, 
					0, 
					target.getZ() - dronePosition.z);
			float deltaHeading = (float) (rotations.x - Math.atan2(-requestedVector.x, -requestedVector.z));
			if (deltaHeading > Math.PI) deltaHeading -= 2*Math.PI;
			else if (deltaHeading < -Math.PI) deltaHeading += 2*Math.PI;
			
			// Reorden path if cube failed
			if (Math.abs(deltaHeading) > Math.PI/2 && distanceTo(target, dronePosition) < 100 && 
					path.getCoordinates().size() > 1 && !landingTrailSet)
				new PathReorden().reordenPath(path, dronePosition, (float)rotations.x);

	
			if (Math.abs(deltaHeading) < Math.PI/4) {
				turnAround = false;
			} else if (Math.abs(deltaHeading) > Math.PI * 175 / 180) {
				turnAround = true;	// cube behind drone with 1 degree
			}
		
			motion[0] = getPitch(dronePosition, target);
			if (turnAround) {
				System.out.println("ERROR: TURN AROUND");
				if (distanceTo(target, dronePosition) > 1000) {
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
				motion[1] = getPidPositionOneCube(dronePosition, rotations, deltaTimeElapsed);
			}
			
			if (printExtraPoints) {
				System.out.println("EXTRA POINTS");
				System.out.println("---dronePosStart: ["+dronePosStart.getX()+",0,"+dronePosStart.getZ()+"]");
				System.out.println("---target: ["+target.getX()+",0,"+target.getZ()+"]");
			}
			
			if (isReached(target, dronePosition, rotations)) 
					resetAll();
		}
		
		return motion;
	}
	
	
	// PITCH //
	
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
	
	
	// PID //
	
	/*
	 * The PID method using position when there is only one cube left
	 * 
	 * @return	Return the requested heading
	 */
	private float getPidPositionOneCube(Vector3f dronePosition, Vector3f rotations, float deltaTimeElapsed) {
		updatePositionErrorOneCube(dronePosition, rotations);

		if (printPID) {
			System.out.println("PID - POSITION ONE CUBE (NAIVE)");
			System.out.println("---proportional: "+proportionalPosition());
			System.out.println("---derivative: "+ 2 * 1.0/deltaTimeElapsed *derivativePosition());
			System.out.println("---integral: "+ 0);//0.5 * integralPosition());
		}
		
		// Return the drone its own heading with the requested direction
		float pidPositionFault = (float) (proportionalPosition() + 2 * 1.0/deltaTimeElapsed * derivativePosition());// + 0.5 * integralPosition());
		if (pidPositionFault > 20) pidPositionFault = 20;
		else if (pidPositionFault < -20) pidPositionFault = -20;
		
		if (printPID)
			System.out.println("---default pidPositionFault: "+pidPositionFault);
	
		return (float) (rotations.x + pidPositionFault*(Math.PI/180));	
	}
	
	private float proportionalPosition() {
		return positionError;
	}
	
	private float derivativePosition() {
		return (float) Math.atan(positionError - prevPositionError);
	}
	
	private void updatePositionErrorOneCube(Vector3f dronePosition, Vector3f rotations) {
		prevPositionError = positionError;
		positionError = (float) (getDistanceFaultOneCube(dronePosition));

		if (printPID) {
			System.out.println("---positionError "+positionError);
			System.out.println("---prevPositionError "+prevPositionError);
		}
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
		
		return (distanceTo(requestedPoint, dronePosition) < 30 || // Successfully reached
				(headingDifference > Math.PI/2 && distanceTo(requestedPoint, dronePosition) < 200));	// Drone has past it
	}

	private float distanceTo(Point3D point, Vector3f position) {
		return (float) Math.sqrt(Math.pow(point.getX() - position.x, 2) + Math.pow(point.getZ() - position.z, 2));
	}

	private float distanceTo(Point3D point, Point3D position) {
		return (float) Math.sqrt(Math.pow(point.getX() - position.getX(), 2) + Math.pow(point.getZ() - position.getZ(), 2));
	}
	
	public void resetAll() {
		target = null;
		turnAround = false;
	}

}