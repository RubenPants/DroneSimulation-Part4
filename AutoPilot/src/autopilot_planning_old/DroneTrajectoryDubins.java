package autopilot_planning_old;

import java.util.List;

import autopilot_utilities.Path3D;
import autopilot_utilities.Point3D;
import autopilot_utilities.Vector3f;

/**
 * A class of drone trajectories using Dubins interpolation.
 * 
 * @author	Team Saffier
 * @version	1.0
 */
public class DroneTrajectoryDubins extends DroneTrajectory {

	@Override
	protected float[] calculateTrajectoryInternal(Path3D path, Vector3f position, Vector3f rotations, double velocity, double rho, boolean landingTrailSet, float timeElapsed) {

		float[] motion = new float[2];

		// If there are two coordinates, apply quadratic interpolation
		List<Point3D> coordinates = path.getCoordinates();
		if (coordinates.size() > 0) {

			// Calculate Dubins path
			Point3D target = coordinates.get(0);
			Point3D nextTarget = coordinates.get(1);
			Vector3f cubeVector = new Vector3f(nextTarget.getX() - target.getX(), 0, nextTarget.getZ() - target.getZ());
			float startHeading = (float)Math.toRadians(rotations.x);
			float endHeading = (float)Math.atan2(-cubeVector.x, -cubeVector.z);
			Vector3f startConfiguration = new Vector3f(-position.z, -position.x, startHeading);
			Vector3f endConfiguration = new Vector3f(-target.getZ(), -target.getX(), endHeading);
			DubinsPath2D dubinsPath = new DubinsPath2D(startConfiguration, endConfiguration, rho);			

			// Get heading
			if (dubinsPath.type.params[0] > 0.01) {
				switch (dubinsPath.type.identifier) {
				case LSL:
				case LSR:
				case LRL:
					motion[1] = (float)(rotations.x + Math.PI/4);
					break;
				case RSR:
				case RSL:
				case RLR:
					motion[1] = (float)(rotations.x - Math.PI/4);
					break;
				default:
					break;
				}
			}
			else if (dubinsPath.type.params[1] > 0.1) {
				switch (dubinsPath.type.identifier) {
				case LSL:
				case RSR:
				case LSR:
				case RSL:
					motion[1] = (float)rotations.x;
					break;
				case RLR:
					motion[1] = (float)(rotations.x + Math.PI/4);
					break;
				case LRL:
					motion[1] = (float)(rotations.x - Math.PI/4);
					break;
				default:
					break;
				}
			}
			else {
				switch (dubinsPath.type.identifier) {
				case RLR:
				case RSR:
				case LSR:
					motion[1] = (float)(rotations.x - Math.PI/4);
					break;
				case RSL:
				case LSL:
				case LRL:
					motion[1] = (float)(rotations.x + Math.PI/4);
					break;
				default:
					break;
				}
			}			

			System.out.println("Calculated Dubins " + rotations.x + " " + endHeading + " " + dubinsPath.type.identifier.toString());
			System.out.println("--- length " + dubinsPath.getLength());
			System.out.println("--- params " + dubinsPath.type.params[0]);
			System.out.println("--- params " + dubinsPath.type.params[1]);
			System.out.println("--- params " + dubinsPath.type.params[2]);

			// Pitch			
			Vector3f requestedVector = new Vector3f(target.getX() - position.x, target.getY() - position.y, target.getZ() - position.z);
			motion[0] = (float)Math.atan(requestedVector.y/(Math.sqrt(requestedVector.z*requestedVector.z + requestedVector.x*requestedVector.x)));
			float shortPitch = (float) Math.atan(requestedVector.y / 50.0);
			if (shortPitch > Math.PI/36) shortPitch = (float)Math.PI/36;
			else if (shortPitch < -Math.PI/36) shortPitch = -(float)Math.PI/36;
			if (Math.abs(motion[0]) < Math.abs(shortPitch)) 
				motion[0] = shortPitch;

		}

		return motion;

	}

}