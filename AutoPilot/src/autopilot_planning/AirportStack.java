package autopilot_planning;

import autopilot_utilities.Point3D;
import interfaces.AutopilotInputs;

public class AirportStack {
	
	// Print booleans
	private boolean ENABLE_LOGGING = false;
	private boolean printPD = true;
	
	private boolean onStack;
	private float positionError = 0;
	private float prevPositionError = 0;
	private float maxFault = 40f;
	private float defaultRho = 400;
	
	public float[] handleDroneInAirportStack(AutopilotInputs inputs, Point3D target, float deltaElapsedTime, float goToStackY) {
		float rho = defaultRho;
		
		if (ENABLE_LOGGING) System.out.println("DISTANCE TO REQUESTED CENTER: "+Distance.distanceToHor(target, inputs));
		
		if (Distance.distanceToHor(target, inputs) > rho + maxFault) onStack = false;
		else if (Distance.distanceToHor(target, inputs) < rho - 10) return graduallyIncreaseToCircle(inputs);
		else if (Math.abs(Distance.distanceToHor(target, inputs) - rho) < 5) onStack = true;
		
		if (onStack) {
			return flyOnStack(inputs, target, deltaElapsedTime, rho);
		} else {
			//defineHelperTarget(inputs, target, rho, goToStackY);
			
			return getOnStack(inputs, target, deltaElapsedTime, rho);
		}
	}
	
	private float[] graduallyIncreaseToCircle(AutopilotInputs inputs) {
		float[] motion = new float[2];
		motion[0] = 0;	// Fly horizontal
		motion[1] = (float) (inputs.getHeading() - 10*(Math.PI/180));

		if (ENABLE_LOGGING) {
			System.out.println("GRADUALLY INCREASE TO CIRCLE");
			System.out.println("---reqPitch (deg): "+Math.toDegrees(motion[0]));
			System.out.println("---reqHeading (deg): "+Math.toDegrees(motion[1]));
		}
		return motion;
	}
	
	private float[] getOnStack(AutopilotInputs inputs, Point3D target, float deltaElapsedTime, float rho) {
		float[] motion = new float[2];
		
		// pitch
		motion[0] = Pitch.getPitch(inputs, target);	// Fly horizontal
		
		// heading
		float deltaHeading = Heading.getDeltaHeading(inputs, target);
		float distance = Distance.distanceToHor(target, inputs);
		if (distance == 0) distance++;
		float reqHeading = (float)(inputs.getHeading() + deltaHeading + Math.asin(rho/distance));
		if (reqHeading > Math.PI) reqHeading -= 2*Math.PI;
		else if (reqHeading < -Math.PI) reqHeading += 2*Math.PI;
		motion[1] = reqHeading;
		
		if (ENABLE_LOGGING) {
			System.out.println("GET ON STACK");
			System.out.println("---reqPitch (deg): "+Math.toDegrees(motion[0]));
			System.out.println("---reqHeading (deg): "+Math.toDegrees(motion[1]));
		}
		return motion;
	}
	
	private float[] flyOnStack(AutopilotInputs inputs, Point3D target, float deltaElapsedTime, float rho) {		
		float[] motion = new float[2];
		
		// Pitch
		float pitch = Pitch.getPitch(inputs, target);
		if (Math.abs(pitch) < Math.PI/30) motion[0] = pitch;
		else if (pitch <= -Math.PI/30) motion[0] = (float)(-Math.PI/30);
		else motion[0] = (float)(Math.PI/30);
		
		// Heading
		updatePositionErrorCircle(inputs, target, rho);
		motion[1] = PdControl.getPdPositionCircleClockwise(inputs, deltaElapsedTime, positionError, prevPositionError);

		if (ENABLE_LOGGING) {
			System.out.println("FLY ON STACK");
			System.out.println("---reqPitch (deg): "+Math.toDegrees(motion[0]));
			System.out.println("---reqHeading (deg): "+Math.toDegrees(motion[1]));
		}
		return motion;
	}
	
	private void updatePositionErrorCircle(AutopilotInputs inputs, Point3D target, float rho) {
		prevPositionError = positionError;
		positionError = (float) (PdControl.getDistanceFaultCircle(inputs, target, rho));

		if (ENABLE_LOGGING && printPD) {
			System.out.println("---positionError "+positionError);
			System.out.println("---prevPositionError "+prevPositionError);
		}
	}

}
