package autopilot_planning_old;

import java.util.List;

import autopilot_utilities.Path3D;
import autopilot_utilities.Point3D;
import autopilot_utilities.Vector3f;

public class DroneTrajectoryDubinsPart2 extends DroneTrajectory {

	private List<Point3D> coordinates;
	private DubinsPath2D dubinsPath = null;
	private double totalDistance = 0;
	private float deltaAlpha= 0;
	private Vector3f prevPosition = null;
	private Point3D extraPosition = null;
	private int stage;
	private boolean gotFirst;
	private boolean goToLast;
	private String direction = "R";

	@Override
	protected float[] calculateTrajectoryInternal(Path3D path, Vector3f position, Vector3f rotations, double velocity,
			double rho, boolean landingTrailSet, float timeElapsed) {
		System.out.println();
		System.out.println("---Stage: " + stage);

		float[] motion = new float[2];
		boolean pitchExtraPoint = false;
		coordinates = path.getCoordinates();
		if (coordinates.size() > 0) {

			// Calculate Dubins path
			Point3D target = coordinates.get(0);
			Point3D nextTarget = coordinates.get(1);
			Vector3f cubeVector = new Vector3f(nextTarget.getX() - target.getX(), 0, nextTarget.getZ() - target.getZ());
			float endHeading = (float) Math.atan2(-cubeVector.x, -cubeVector.z);
			Vector3f startConfiguration = new Vector3f(-position.z, -position.x, rotations.x);
			Vector3f endConfiguration = new Vector3f(-target.getZ(), -target.getX(), endHeading);

			// Init values
			if (dubinsPath == null && distanceTo(target, position) > 15) {
				dubinsPath = new DubinsPath2D(startConfiguration, endConfiguration, rho);
				// TODO
				if (deltaAlpha == 0) deltaAlpha = (float) dubinsPath.type.params[2];
				direction = dubinsPath.type.identifier.toString().substring(2);
			}
			/*
			if (extraPosition == null && distanceTo(target, position) > 15) {
				//String direction = dubinsPath.type.identifier.toString().substring(2);
				extraPosition = calculateEndPosition(target, (float) dubinsPath.type.params[2], endHeading, direction);
			}
			*/

			if (dubinsPath != null) {
				System.out.println("Calculated Dubins " + dubinsPath.type.identifier.toString());
				System.out.println("--- length " + dubinsPath.getLength());
				System.out.println("--- params " + dubinsPath.type.params[0]);
				System.out.println("--- params " + dubinsPath.type.params[1]);
				System.out.println("--- params " + dubinsPath.type.params[2]);
			}

			if (((distanceTo(target, position) < 500) && (distanceTo(target, position) > 15)))// || 
				//(dubinsPath != null && dubinsPath.type.params[2] > Math.PI))
				stage = 2; // Target cube stage

			// Handle the heading
			if (stage == 0 && dubinsPath != null) {
				addDistance(position);
				// TODO
				// Check when to go to next stage
				if ((totalDistance >= dubinsPath.type.params[0] * 300) ||
						dubinsPath.type.params[0] < Math.PI/8) {
					//String direction = dubinsPath.type.identifier.toString().substring(2);
					extraPosition = calculateEndPosition(target, deltaAlpha, endHeading, direction);
					stage++;
				}
				
				switch (dubinsPath.type.identifier) {
				case LSL:
				case LSR:
					motion[1] = (float) (rotations.x + Math.PI / 4);
					break;
				case RSL:
				case RSR:
					motion[1] = (float) (rotations.x - Math.PI / 4);
					break;
				default:
					break;
				}
			} else if (stage == 1) {				
				// Check when to go to next stage;
				// TODO:
				if (deltaAlpha < Math.PI/4) {
					totalDistance = 0;
					prevPosition = null;
					stage++;
				} else if  (distanceTo(extraPosition, position) < 100) {
					deltaAlpha -= (float) Math.PI/6;
					extraPosition = calculateEndPosition(target, deltaAlpha, endHeading, direction);
				}
				pitchExtraPoint = true;
				System.out.println("Requested: [" + extraPosition.getX() + ","+extraPosition.getY()+"," + extraPosition.getZ() + "]");
				Vector3f extraPointVector = new Vector3f(extraPosition.getX() - position.x, 0,
						extraPosition.getZ() - position.z);
				motion[1] = (float) Math.atan2(-extraPointVector.x, -extraPointVector.z);

				double headingDifference = Math.min(Math.abs(rotations.x - motion[1]),
						Math.PI * 2 - Math.abs(rotations.x - motion[1]));
				if (distanceTo(extraPosition, position) < 500 && headingDifference > Math.PI / 2) {
					stage = 2; // Cube not reachable, go to last stage and use naive method
				}
			} else {
				// Go to the first next cube
				Vector3f targetPointVector = new Vector3f(target.getX() - position.x, 0, target.getZ() - position.z);
				motion[1] = (float) Math.atan2(-targetPointVector.x, -targetPointVector.z);

				double headingDifference = Math.min(Math.abs(rotations.x - motion[1]),
						Math.PI * 2 - Math.abs(rotations.x - motion[1]));
				
				// Enlarge heading difference when close to cube!
				if (distanceTo(target, position) < 500) {
					float deltaHeading = (float) (motion[1] - rotations.x);
					if (deltaHeading > Math.PI) deltaHeading -= 2 * Math.PI;
					else if (deltaHeading < -Math.PI) deltaHeading += 2 * Math.PI;

					if (Math.abs(rotations.z) < Math.PI/5) {
						if (deltaHeading < Math.PI/36)
							motion[1] += deltaHeading;
						else if (deltaHeading < Math.PI/18)
							motion[1] += deltaHeading/2;
					} else {
						if (deltaHeading < Math.PI/36)
							motion[1] += deltaHeading/2;
						else if (deltaHeading < Math.PI/18)
							motion[1] += deltaHeading/4;
					}
						
				}
				
				// Cube not reachable, keep going forwards until it is
				if (distanceTo(target, position) < 500 && headingDifference > Math.PI / 2) {
					// TODO: FAILED TO REACH CUBE --> Reorden path!
					motion[1] = (float) rotations.x;
					new PathReorden().reordenPath(path, new Point3D(position.x, position.y, position.z), 0);
					stage = 2;
				}

				if (distanceTo(target, position) < 10)
					gotFirst = true;
				if (gotFirst && distanceTo(target, position) > 15)
					goToLast = true;

				if (goToLast && distanceTo(target, position) < 10) {
					reset(position);
				}
			}

			// Handle the Pitch
			Vector3f requestedVector = new Vector3f(target.getX() - position.x, target.getY() - position.y,
					target.getZ() - position.z);
			
			if (pitchExtraPoint) requestedVector = new Vector3f(extraPosition.getX() - position.x, 
					extraPosition.getY() - position.y, extraPosition.getZ() - position.z);
			motion[0] = (float) Math.atan(requestedVector.y / (Math.sqrt(requestedVector.z * requestedVector.z + requestedVector.x * requestedVector.x)));
			float shortPitch = (float) Math.atan(requestedVector.y / 100.0);
			if (shortPitch > Math.PI / 36) shortPitch = (float) Math.PI / 36;
			if (motion[0] > 0 && motion[0] < shortPitch) motion[0] = shortPitch;

		}
		return motion;
	}

	private void addDistance(Vector3f position) {
		if (prevPosition == null)
			prevPosition = new Vector3f(position.x, 0, position.z);

		// Update distance and coordinates
		totalDistance += distanceTo(prevPosition, position);
		prevPosition.x = position.x;
		prevPosition.z = position.z;
	}

	private Point3D calculateEndPosition(Point3D targetCube, float deltaAngle, float endHeading, String direction) {
		float sigma;
		if (direction.equals("R")) {
			sigma = (float) (endHeading - Math.PI / 2);
		} else { // "L"
			deltaAngle *= -1;
			sigma = (float) (endHeading + Math.PI / 2);
		}
		// Determine center circle
		Point3D center = new Point3D(targetCube.getX() - 400 * Math.sin(sigma), 0,
				targetCube.getZ() - 400 * Math.cos(sigma));

		// Get angles center to cubes
		Vector3f targetCubeVector = new Vector3f(targetCube.getX() - center.getX(), 0,
				targetCube.getZ() - center.getZ());
		float centerTargetAngle = (float) Math.atan2(-targetCubeVector.x, -targetCubeVector.z);
		float totalAngle = centerTargetAngle + deltaAngle;
		Point3D requestedPoint = new Point3D(center.getX() - 400 * Math.sin(totalAngle), targetCube.getY() + 10 * Math.abs(deltaAngle),
				center.getZ() - 400 * Math.cos(totalAngle));

		return requestedPoint;
	}

	private float distanceTo(Point3D point, Vector3f position) {
		return (float) Math.sqrt(Math.pow(point.getX() - position.x, 2) + Math.pow(point.getZ() - position.z, 2));
	}

	private float distanceTo(Vector3f point, Vector3f position) {
		return (float) Math.sqrt(Math.pow(point.x - position.x, 2) + Math.pow(point.z - position.z, 2));
	}
	
	public void reset(Vector3f position) {
		totalDistance = 0;
		deltaAlpha = 0;
		prevPosition = null;
		dubinsPath = null;
		extraPosition = null;
		gotFirst = false;
		goToLast = false;	
		if (coordinates != null && coordinates.size() > 1 && distanceTo(coordinates.get(1), position) > 800)
			stage = 0;
		else
			stage = 2;
	}

}
