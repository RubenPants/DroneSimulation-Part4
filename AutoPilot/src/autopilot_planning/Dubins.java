package autopilot_planning;

import java.util.List;

import autopilot_utilities.Point3D;
import autopilot_utilities.Vector3f;
import autopilot_vision.DistanceTests;
import interfaces.AutopilotInputs;

public class Dubins {
	
	// Print booleans
	private boolean ENABLE_LOGGING = false;
	private boolean printExtraPoints = true;
	private boolean printPID = true;
	private boolean printDubins = true;
	
	// Member variables
	private int stage;
	private DubinsPath2D dubinsPath = null;
	private float endHeading;
	
	private Point3D firstCircleCenter;
	private Point3D secondCircleCenter;
	private Point3D firstCircleKeyPoint;
	private Point3D secondCircleKeyPoint;
	private Point3D target;
	private Point3D nextTarget;

	// PID using position
	private float positionError = 0;
	private float prevPositionError = 0;
	private float rho = 400;	
	private Point3D dronePosStart = new Point3D(0,0,0);
	private Point3D lastDronePosition = new Point3D(0,0,0);
	private float maxFault = 3f;
	private float maxFaultReset = 30f;
	
	protected float[] calculateTrajectory(List<Point3D> targetCoordinates, AutopilotInputs inputs, double rho, float deltaTimeElapsed) {
		
		// TODO
		// TODO: NEEM DUBINS UIT LEGACY!
		// TODO
		
		float[] motion = new float[2];
		
		
		// TODO: Reset if new points added and not want to reach nextTarget!
		if (target != targetCoordinates.get(0) && nextTarget != targetCoordinates.get(0))
			resetAll(inputs);

		
		// TODO --> Reset this way? In this case there must not be a new trajetory created each time the drone starts flying?
		if (inputs.getY() < 5 || Math.abs(positionError) > maxFaultReset || Distance.distanceTo3D(lastDronePosition, inputs) > 100)
			resetAll(inputs);
		
		// TODO: is dit geheugen intensief?
		lastDronePosition = new Point3D(inputs.getX(), inputs.getY(), inputs.getZ());
		
		// Initialization
		if (dubinsPath == null) {
			if(ENABLE_LOGGING && printDubins) System.out.println("INIT DUBINS");

			target = targetCoordinates.get(0);
			nextTarget = targetCoordinates.get(1);
			Vector3f cubeVector = new Vector3f(nextTarget.getX() - target.getX(), 0, nextTarget.getZ() - target.getZ());
			endHeading = (float) Math.atan2(-cubeVector.x, -cubeVector.z);
			Vector3f startConfiguration = new Vector3f(-inputs.getZ(), -inputs.getX(), inputs.getHeading());
			Vector3f endConfiguration = new Vector3f(-target.getZ(), -target.getX(), endHeading);
	
			long tic = System.nanoTime();
			dubinsPath = new DubinsPath2D(startConfiguration, endConfiguration, rho);
			if (ENABLE_LOGGING) System.out.println("DUBINS TIMING = " + (System.nanoTime() - tic) / 10E6);
			
			calculateCircleCenters(inputs, dubinsPath.type.identifier.toString());
			calculateCircleKeyPoints(inputs, dubinsPath.type.toString());
		}

		if (dubinsPath != null) {
			switch (stage) {
			case 0:	// First circle
				if (isReached(firstCircleKeyPoint, inputs, false)) {
					stage++;
					dronePosStart = new Point3D(inputs.getX(), inputs.getY(), inputs.getZ());
				}
				break;
			case 1:	// Straight line
				if (Math.abs(positionError) > maxFault)
					dronePosStart = new Point3D(inputs.getX(), inputs.getY(), inputs.getZ());
				
				if (isReached(secondCircleKeyPoint, inputs, false)) stage++;
				break;
			case 2:	// Last circle
				// TODO: Or check if heading drone is good!
				if (isReached(target, inputs, false)) {
					stage++;
					dronePosStart = new Point3D(inputs.getX(), inputs.getY(), inputs.getZ());
				}
				break;
			case 3: // Line between target and nextTarget
				// TODO --> Only landingTrail? Then reachWide!
				if (Math.abs(positionError) > maxFault)
					dronePosStart = new Point3D(inputs.getX(), inputs.getY(), inputs.getZ());
				
				// TODO: Fix last target reachability! + reset ok?
				if (target != targetCoordinates.get(0) && 
						(nextTarget != targetCoordinates.get(0) ||
						(targetCoordinates.size() > 2 && Distance.distanceTo3D(nextTarget, inputs) > 1000)))
					return resetAll(inputs);
				
				//if (isReached(nextTarget, inputs, false)) stage++;
				break;
			default:
				// TODO: Not needed?
				if (nextTarget != targetCoordinates.get(0) || Distance.distanceToHor(nextTarget, inputs) > 100)
					return resetAll(inputs);
				// Path is null --> Nothing to calculate!
				if (ENABLE_LOGGING) System.out.println("Stuck in default!");
				motion[0] = (float) inputs.getPitch();
				motion[1] = (float) inputs.getHeading();
				if (ENABLE_LOGGING) System.out.println("ERROR: FLY STRAIGHT");
				return motion;
			}
			
			// Handle the Pitch
			if (stage == 0)	// TODO: Force pitch
				motion[0] = 0;
			else if (stage < 3)
				motion[0] = Pitch.getPitch(inputs, target);
			else
				motion[0] = Pitch.getPitch(inputs, nextTarget);
			
			// TODO
			updatePositionError(inputs, dubinsPath.type.identifier.toString());
			motion[1] = PdControl.getPdPosition(inputs, deltaTimeElapsed, stage, dubinsPath.type.identifier.toString(), positionError, prevPositionError);
			
			if (ENABLE_LOGGING) print(inputs);
		}
		
		return motion;
	}
	
	
	// INITIALISATION METHODS //
	
	private void calculateCircleCenters(AutopilotInputs inputs, String dubinsString) {
		String directionFirst = dubinsString.substring(0, 1);
		String directionSecond = dubinsString.substring(2, 3);
		float sigma;
		
		// Calculate first center
		if (directionFirst.equals("R"))
			sigma = (float) (inputs.getHeading() - Math.PI / 2);
		else // "L"
			sigma = (float) (inputs.getHeading() + Math.PI / 2);
		
		firstCircleCenter = new Point3D(
				inputs.getX() - rho * Math.sin(sigma), 
				0,
				inputs.getZ() - rho * Math.cos(sigma));
		
		// Calculate second center
		if (directionSecond.equals("R"))
			sigma = (float) (endHeading - Math.PI / 2);
		else // "L"
			sigma = (float) (endHeading + Math.PI / 2);
		
		secondCircleCenter = new Point3D(
				target.getX() - rho * Math.sin(sigma), 
				0,
				target.getZ() - rho * Math.cos(sigma));
	}
	
	private void calculateCircleKeyPoints(AutopilotInputs inputs, String dubinsString) {
		// Get first key point
		double deltaAngle = dubinsPath.type.params[0];
		if (dubinsString.substring(0, 1).equals("L")) deltaAngle *= -1;
		Vector3f targetCubeVector = new Vector3f(
				inputs.getX() - firstCircleCenter.getX(), 
				0,
				inputs.getZ() - firstCircleCenter.getZ());
		float centerTargetAngle = (float) Math.atan2(-targetCubeVector.x, -targetCubeVector.z);
		float totalAngle;
		if (dubinsPath.type.identifier.toString().substring(0, 1).equals("R"))
			totalAngle = (float) (centerTargetAngle - deltaAngle);
		else 
			totalAngle = (float) (centerTargetAngle + deltaAngle);
		
		firstCircleKeyPoint = new Point3D(
				firstCircleCenter.getX() - rho * Math.sin(totalAngle), 
				0,
				firstCircleCenter.getZ() - rho * Math.cos(totalAngle));
		
		// Get second key point
		deltaAngle = dubinsPath.type.params[2];
		if (dubinsString.substring(2, 3).equals("L")) deltaAngle *= -1;
		targetCubeVector = new Vector3f(
				target.getX() - secondCircleCenter.getX(), 
				0,
				target.getZ() - secondCircleCenter.getZ());
		centerTargetAngle = (float) Math.atan2(-targetCubeVector.x, -targetCubeVector.z);
		if (dubinsPath.type.identifier.toString().substring(2, 3).equals("L"))
			totalAngle = (float) (centerTargetAngle - deltaAngle);
		else 
			totalAngle = (float) (centerTargetAngle + deltaAngle);
		secondCircleKeyPoint = new Point3D(
				secondCircleCenter.getX() - rho * Math.sin(totalAngle), 
				0,
				secondCircleCenter.getZ() - rho * Math.cos(totalAngle));
	}
	
	
	// HELPER PD DISTANCE
	
	private void updatePositionError(AutopilotInputs inputs, String dubinsIdentifier) {
		prevPositionError = positionError;
		positionError = (float) (PdControl.getDistanceFault(inputs, stage, dubinsIdentifier, firstCircleCenter, secondCircleCenter, firstCircleKeyPoint, secondCircleKeyPoint, nextTarget, dronePosStart));

		if (ENABLE_LOGGING && printPID) {
			System.out.println("---positionError "+positionError);
			System.out.println("---prevPositionError "+prevPositionError);
		}
	}
	
	
	// HELPER METHODS //
	
	private boolean isReached(Point3D requestedPoint, AutopilotInputs inputs, boolean wide) {
		Vector3f targetPointVector = new Vector3f(
				requestedPoint.getX() - inputs.getX(), 
				0, 
				requestedPoint.getZ() - inputs.getZ());
		float reqHeading = (float) Math.atan2(-targetPointVector.x, -targetPointVector.z);
		double headingDifference = Math.min(Math.abs(inputs.getHeading() - reqHeading),
				Math.PI * 2 - Math.abs(inputs.getHeading() - reqHeading));
		
		if (wide) {
			return (Distance.distanceToHor(requestedPoint, inputs) < 100 || // Successfully reached
					(headingDifference > Math.PI/2 && Distance.distanceToHor(requestedPoint, inputs) < 200));	// Drone has past it
		} else {
			return (Distance.distanceToHor(requestedPoint, inputs) < 50 || // Successfully reached
					(headingDifference > Math.PI/2 && Distance.distanceToHor(requestedPoint, inputs) < 200));	// Drone has past it
		}
	}
	
	// TODO
	public float[] resetAll(AutopilotInputs inputs) {
		stage = 0;
		endHeading = 0;
		dubinsPath = null;
		return new float[]{0,inputs.getHeading()};
	}
	
	private void print(AutopilotInputs inputs) {
		if (printDubins) {
			System.out.println("DUBINS: " + dubinsPath.type.identifier.toString());
			System.out.println("---stage: "+stage+"<--");
			System.out.println("---length " + dubinsPath.getLength());
			System.out.println("---params " + dubinsPath.type.params[0]);
			System.out.println("---params " + dubinsPath.type.params[1]);
			System.out.println("---params " + dubinsPath.type.params[2]);
		}

		if (printExtraPoints) {
			System.out.println("EXTRA POINTS");
			System.out.println("---dronePosition: ["+inputs.getX()+","+inputs.getY()+","+inputs.getZ()+"]");
			System.out.println("---firstCenter: ["+firstCircleCenter.getX()+",0,"+firstCircleCenter.getZ()+"]");
			System.out.println("---secondCenter: ["+secondCircleCenter.getX()+",0,"+secondCircleCenter.getZ()+"]");
			System.out.println("---firstKeyPoint: ["+firstCircleKeyPoint.getX()+","+firstCircleKeyPoint.getY()+","+firstCircleKeyPoint.getZ()+"]");
			System.out.println("---secondKeyPoint: ["+secondCircleKeyPoint.getX()+",0,"+secondCircleKeyPoint.getZ()+"]");
			System.out.println("---target: ["+target.getX()+",0,"+target.getZ()+"]");
			System.out.println("---nextTarget: ["+nextTarget.getX()+",0,"+nextTarget.getZ()+"]");
		}
	}

}
