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
public class DroneTrajectoryNaive extends DroneTrajectory {

	@Override
	protected float[] calculateTrajectoryInternal(Path3D path, Vector3f position, Vector3f rotations, double velocity, double rho, boolean landingTrailSet, float timeElapsed) {

		// Pre-processing
		float[] motion = new float[2];
		Point3D target = null, currentPosition = new Point3D(position.x, position.y, position.z);
		double minDistance = Double.POSITIVE_INFINITY, currentDistance;
		/*
		Matrix4f toWorld = Utilities.getDroneToWorldTransformationMatrix(
				(float)rotations.x, 
				(float)rotations.y, 
				(float)rotations.z);
				*/
/*
		// Get closest location
		for (Point3D coordinate : path.getCoordinates()) {
			currentDistance = coordinate.distanceTo(currentPosition);
			if (currentDistance < minDistance) {
				target = coordinate;
				minDistance = currentDistance;
			}
		}
*/
		// Calculate pitch/heading
		if (path.getCoordinates().size() > 0) {
			target = path.getCoordinates().get(0);
			Vector3f requestedVector = new Vector3f(target.getX() - position.x, target.getY() - position.y, target.getZ() - position.z);
			motion[0] = (float)Math.atan(requestedVector.y/(Math.sqrt(requestedVector.z*requestedVector.z + requestedVector.x*requestedVector.x)));
			motion[1] = (float)Math.atan2(-requestedVector.x, -requestedVector.z);

			double headingDifference = Math.min(Math.abs(rotations.x - motion[1]),
					Math.PI * 2 - Math.abs(rotations.x - motion[1]));
			
			// Cube not reachable, keep going forwards until it is
			if (distanceTo(target, position) < 500 && headingDifference > Math.PI / 2)
				motion[1] = (float) rotations.x;

			float shortPitch = (float) Math.atan(requestedVector.y / 50.0);
			if (shortPitch > Math.PI/36) shortPitch = (float)Math.PI/36;
			else if (shortPitch < -Math.PI/36) shortPitch = -(float)Math.PI/36;
			if (Math.abs(motion[0]) < Math.abs(shortPitch)) motion[0] = shortPitch;
		}
		else {
			motion[0] = 0.0f;
			motion[1] = (float)rotations.x;
		}			
		return motion;
	}

	private float distanceTo(Point3D point, Vector3f position) {
		return (float) Math.sqrt(Math.pow(point.getX() - position.x, 2) + Math.pow(point.getZ() - position.z, 2));
	}

}