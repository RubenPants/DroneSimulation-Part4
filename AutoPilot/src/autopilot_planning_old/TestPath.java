package autopilot_planning_old;

import java.util.ArrayList;
import org.junit.*;

import autopilot_utilities.Point3D;

public class TestPath {

	@Test
	public void testBla() {
		ArrayList<Point3D> cubes = new ArrayList<Point3D>();
		cubes.add(new Point3D(3,2,-12));
		cubes.add(new Point3D(-7,6,-50));
		cubes.add(new Point3D(4,9,-98));
		//Path path = new Path(new Point3D(0,0,0), cubes);
		//System.out.println(path.cheapestPath(Path.generatePerm(cubes)));
	}
}
