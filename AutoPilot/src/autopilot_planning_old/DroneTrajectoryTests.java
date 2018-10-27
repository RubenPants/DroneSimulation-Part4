package autopilot_planning_old;

import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;

import autopilot_planning_old.DroneTrajectory.DroneTrajectoryType;
import autopilot_utilities.Path3D;
import autopilot_utilities.Point3D;
import autopilot_utilities.Vector3f;
import autopilot_vision.Cube;

/**
 * A class for testing drone trajectory calculators.
 * 
 * @author	Team Saffier
 * @version	1.0
 */
public class DroneTrajectoryTests {

	/**
	 * Variable with sample paths to be used for testing.
	 */
	ArrayList<Path3D> samplePaths = new ArrayList<Path3D>();
	
	@Before
	public void setUp() throws Exception {
		
		// Simple path
		ArrayList<Point3D> simplePath = new ArrayList<Point3D>();
		simplePath.add(new Point3D(-40.0, 0.0, 0.0));
		simplePath.add(new Point3D(-80.0, 0.0, 0.0)); 
		samplePaths.add(new Path3D(simplePath));
		samplePaths.add(new Path3D(simplePath));
		samplePaths.add(new Path3D(simplePath));
		samplePaths.add(new Path3D(simplePath));
		samplePaths.add(new Path3D(simplePath));
		samplePaths.add(new Path3D(simplePath));
		samplePaths.add(new Path3D(simplePath));
		samplePaths.add(new Path3D(simplePath));
		
	}

	@Test
	public void test() {
		
		// Test particular type of quadratic
		Vector3f dronePosition = new Vector3f(0.0, 0.0, 0.0);
		Vector3f droneRotation = new Vector3f(Math.PI/2, 0.0, 0.0);
		
		DroneTrajectory trajectory = DroneTrajectory.initializeAlgorithm(DroneTrajectoryType.DUBINS);
		
		for (Path3D path : samplePaths) {
			long tic = System.nanoTime();
			float[] motion = trajectory.calculateTrajectory(path, dronePosition, droneRotation, 0.0, 10.0, false, 0.02f);
			System.out.println("Trajectory proposes heading of (" + motion[0] + ") and pitch of (" + motion[1] + ")");
			System.out.println("Performance : " + ((System.nanoTime() - tic) / Math.pow(10, 6)) + "ms");
		}
		
	}

}