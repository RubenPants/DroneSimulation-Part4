package autopilot_planning_old;

import java.util.ArrayList;

import autopilot_planning_old.DroneTrajectory.DroneTrajectoryType;
import autopilot_utilities.Matrix4f;
import autopilot_utilities.Path3D;
import autopilot_utilities.Point3D;
import autopilot_utilities.Utilities;
import autopilot_utilities.Vector3f;
import interfaces.AutopilotConfig;
import interfaces.AutopilotInputs;

/**
 * A class of motion planners for drone autopilots, for determining what direction
 *  the drone should fly towards.
 *  
 * @author 	Team Saffier
 * @version 	1.0
 */
public class AutopilotMotionPlanner {

	/**
	 * Variable denoting whether or not this object logs information.
	 */
	public boolean ENABLE_LOGGING = true;
	
	// Trajectory
	private DroneTrajectory trajectory;

	// Hard-coded characteristics of the drone when in flight
	private final static double TURNING_RADIUS = 400.0; // Turning radius = forward velocity divided by maximum angular velocity
	// private final static double FLY_SPEED = 46.0; // In m/s

	// Toggle use of spline interpolation
	private static boolean USE_SPLINES = false;

	/**
	 * Initialize this new motion planner.
	 */
	public AutopilotMotionPlanner() {
		loadTrajectory();
	}

	/**
	 * Load trajectory.
	 */
	public void loadTrajectory() {
		trajectory = DroneTrajectory.initializeAlgorithm(DroneTrajectoryType.DUBINS);
	}

	/**
	 * Determine the preferred future motion of the autopilot's drone for the given configuration,
	 *  input and list of detected cubes. 
	 * 
	 * @param 	configuration
	 * 			The current configuration of the autopilot.
	 * @param 	inputs
	 * 			The latest inputs the autopilot received.
	 * @param 	path
	 * 			A 3-dimensional path with locations that are to be visited by the drone.
	 * @return 	An array of 2 floats. 
	 * 				The first float represents the desired pitch. (in radians)
	 * 				The second float represents the desired heading. (in radians)
	 */
	public float[] preferredMotion(AutopilotConfig configuration, AutopilotInputs inputs, Path3D path, boolean landingTrailSet, float deltaTimeElapsed) {

		// First check if splines are used
		if (!USE_SPLINES) {
			
			Vector3f position = new Vector3f(inputs.getX(), inputs.getY(), inputs.getZ());
			Vector3f rotations = new Vector3f(inputs.getHeading(), inputs.getPitch(), inputs.getRoll());
			return trajectory.calculateTrajectory(path, position, rotations, 0.0f, TURNING_RADIUS, landingTrailSet, deltaTimeElapsed);
			
		}
		else { // Use splines

			float[] motion = new float[2];
			Point3D currentPosition = new Point3D(inputs.getX(), inputs.getY(), inputs.getZ());

			Matrix4f toWorld = Utilities.getDroneToWorldTransformationMatrix(
					inputs.getHeading(), 
					inputs.getPitch(), 
					inputs.getRoll());
			Vector3f droneDirection = new Vector3f(0,0,-5);
			Vector3f headingVector = Utilities.transformVector(toWorld, droneDirection);

			SplinePath pathfinder = new SplinePath(path,currentPosition, headingVector);
			if (path.getNbCoordinates() > 1){
				if (ENABLE_LOGGING) System.out.println("Called" + path);

				Point3D firstPoint = path.getCoordinate(0);
				Point3D secondPoint = path.getCoordinate(1);

				ArrayList<Double> splineFunction = pathfinder.calculateSpline(firstPoint, secondPoint);
				if (ENABLE_LOGGING) System.out.println("spline" + splineFunction);
				motion = pathfinder.calculateRoute(splineFunction, inputs, firstPoint);
				
				// getXorz -> true = x
				//			  false = z
				if( (pathfinder.getXorz() && Math.abs(firstPoint.getX() - currentPosition.getX()) < 500 ) || (! pathfinder.getXorz() && Math.abs(firstPoint.getZ() - currentPosition.getZ()) < 500)){
					USE_SPLINES = false;
					motion = preferredMotion(configuration, inputs, path, landingTrailSet, deltaTimeElapsed);
					System.out.println("< 500 meter");
					USE_SPLINES = true;
				}
					
				if (ENABLE_LOGGING) System.out.println("motion0 " + motion[0] + " motion1 " + motion[1]);

			} else {
				if (ENABLE_LOGGING) System.out.println("Last one");
				USE_SPLINES = false;
				motion = preferredMotion(configuration, inputs, path, landingTrailSet, deltaTimeElapsed);
				USE_SPLINES = true;
			}

			return motion;

		}

	}

}