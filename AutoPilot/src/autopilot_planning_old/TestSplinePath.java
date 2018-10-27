package autopilot_planning_old;

import autopilot_utilities.*;
import java.util.ArrayList;
import static org.junit.Assert.*;
import org.junit.*;

public class TestSplinePath {

	SplinePath path = new SplinePath(null, new Point3D(0,0,0), new Vector3f(0,0,-1));
	Point3D cube1 = new Point3D(2,0,-3);
	Point3D cube2 = new Point3D(1,0,-5);
	
	@Test
	public void testCalculateSplineZ() {
		ArrayList<Double> solution = path.calculateSpline(cube1, cube2);
		assertTrue(solution.get(0) == 0);
		assertTrue(solution.get(1) == 0);
		assertTrue(solution.get(2) == 5/6);
		assertTrue(solution.get(3) == 11/54);
	}
}

