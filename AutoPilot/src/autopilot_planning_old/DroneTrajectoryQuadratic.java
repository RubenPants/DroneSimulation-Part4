package autopilot_planning_old;

import java.util.List;

import autopilot_utilities.Path3D;
import autopilot_utilities.Point3D;
import autopilot_utilities.Vector3f;

/**
 * A class of drone trajectories using quadratic interpolation.
 * 
 * @author	Team Saffier
 * @version	1.0
 */
public class DroneTrajectoryQuadratic extends DroneTrajectory {

	@Override
	protected float[] calculateTrajectoryInternal(Path3D path, Vector3f position, Vector3f rotations, double velocity, double rho, boolean landingTrailSet, float timeElapsed) {
		
		float[] motion = new float[2];
		
		// If there are two coordinates, apply quadratic interpolation
		List<Point3D> coordinates = path.getCoordinates();
		if (coordinates.size() > 1) {
						
			// Calculate coordinates
			Point3D firstTarget = coordinates.get(0), secondTarget = coordinates.get(1);
			double x1 = firstTarget.getX(), z1 = firstTarget.getZ(), x2 = secondTarget.getX(), z2 = secondTarget.getZ();
			
			// Calculate quadratic interpolation
			double dx = x2-x1, dz = z2-z1;
			double x1t = x1-position.x, z1t = z1-position.z; // Translation
			double theta = Math.atan2(dz, dx), tcos = Math.cos(theta), tsin = Math.sin(theta); // Rotation
			double x1r = x1t*tcos - z1t*tsin;
			double z1r = x1t*tsin + z1t*tcos;
			/*
			System.out.println(dx);
			System.out.println(Math.toDegrees(theta));
			System.out.println(x1r);
			System.out.println(z1r);
			*/
			
			// Calculate quadratic interpolation
			motion[0] = (float)Math.atan((firstTarget.getY()-position.y)
					/ Math.abs(firstTarget.getZ()-position.z));
			motion[1] = (float)(theta - (Math.PI/2 + Math.atan2(2*z1r/x1r, 1)));
			
		}
		
		return motion;
		
	}
	
}