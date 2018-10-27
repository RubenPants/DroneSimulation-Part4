package autopilot_planning_old;

import autopilot_utilities.Path3D;
import autopilot_utilities.Vector3f;

/**
 * An abstract class of trajectories to be used by a motion planner for a drone/UAV.
 * 
 * @author	Team Saffier
 * @version 	1.0
 */
public abstract class DroneTrajectory {
	
	/**
	 * An enumeration with types of trajectories followed by the drone.
	 */
	public static enum DroneTrajectoryType { // Needs to be static to be able to access it in other classes.

		NAIVE,				// Naive interpolation
		DUBINS,				// Dubins interpolation
		QUADRATIC,			// Quadratic interpolation
		SPLINE;				// B-spline interpolation

		/**
		 * Returns the type of trajectory matching the given ordinal.
		 */
		public static DroneTrajectoryType typeForOrdinal(int ordinal) {
			if (ordinal < 0 || ordinal > 3)
				return null;
			else
				return DroneTrajectoryType.values()[ordinal];
		}

	}

	/**
	 * Create and initialize a new trajectory of given type.
	 * 
	 * @param 	type
	 * 			The type of drone trajectory to set up.
	 * @return	An initialized trajectory of given type, or null if the given type is not valid.
	 */
	public static DroneTrajectory initializeAlgorithm(DroneTrajectoryType type) {

		if (type == null)
			return null; // null type given

		switch (type) {
		case NAIVE:
			return new DroneTrajectoryNaive2();
		case DUBINS:
			return new DroneTrajectoryDubinsPart3();
		case QUADRATIC:
			return new DroneTrajectoryQuadratic();
		case SPLINE:
			return new DroneTrajectorySpline();
		default:
			return null; // No valid type given
		}

	}

	/**
	 * Calculate a trajectory for a drone for the given 3-dimensional path.
	 * 
	 * @param 	path	
	 * 			A path along 3-dimensional coordinates through which the drone should fly.
	 * 			The path should not be empty.		
	 * @param 	position
	 * 			The position of the drone at the current time.
	 * @param 	rotations
	 * 			The heading/pitch/roll angles of the drone at the current time.
	 * @param	velocity
	 * 			The velocity of the drone at the current time.
	 * @param 	rho
	 * 			Maximum turning radius of the drone.
	 * @return	An array with the pitch and heading that the drone should have 
	 * 			at the start of the calculated trajectory.
	 */
	public float[] calculateTrajectory(Path3D path, Vector3f position, Vector3f rotations, double velocity, double rho, boolean landingTrailSet, float timeElapsed) {
		return this.calculateTrajectoryInternal(path, position, rotations, velocity, rho, landingTrailSet, timeElapsed);
	}

	// Private method to be implemented by subclasses
	protected abstract float[] calculateTrajectoryInternal(Path3D path, Vector3f position, Vector3f rotations, double velocity, double rho, boolean landingTrailSet, float timeElapsed);

}
