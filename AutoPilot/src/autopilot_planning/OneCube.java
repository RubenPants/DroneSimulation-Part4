package autopilot_planning;

import autopilot_utilities.Point3D;
import autopilot_utilities.Vector3f;
import interfaces.AutopilotInputs;

public class OneCube {

	private boolean ENABLE_LOGGING = false;
	private boolean printPID = true;
	
	// PID using position
	private float positionError = 0;
	private float prevPositionError = 0;
	private float maxFault = 3f;
	private Point3D dronePosStart;
	
	public float[] getMotion(Point3D target, AutopilotInputs inputs, float turningRadius, float deltaTimeElapsed) {
		float[] motion = new float[2];

		// Pitch
		motion[0] = Pitch.getPitch(inputs, target);
		
		// Heading
		if (dronePosStart == null || Math.abs(positionError) > maxFault)
			dronePosStart = new Point3D(inputs.getX(), inputs.getY(), inputs.getZ());
		
		updatePositionErrorOneCube(inputs, target);
		
		// TODO
		Vector3f requestedVector = new Vector3f(
				target.getX() - inputs.getX(), 
				0, 
				target.getZ() - inputs.getZ());
		float deltaHeading = (float) (inputs.getHeading() - Math.atan2(-requestedVector.x, -requestedVector.z));
		if (deltaHeading > Math.PI) deltaHeading -= 2*Math.PI;
		else if (deltaHeading < -Math.PI) deltaHeading += 2*Math.PI;

		if (ENABLE_LOGGING) System.out.println("deltaHeading: "+Math.abs(deltaHeading));
	 	
		if (Math.abs(deltaHeading) > Math.PI/2 && Distance.distanceToHor(target, inputs) < 600) motion[1] = inputs.getHeading();
		else if (deltaHeading > Math.PI/36) motion[1] = (float) (inputs.getHeading() - Math.PI/16);	// Roll of 33.75
		else if (deltaHeading < -Math.PI/36) motion[1] = (float) (inputs.getHeading() + Math.PI/16);	// Roll of 33.75
		else motion[1] = PdControl.getPdPositionOneCube(inputs, target, deltaTimeElapsed, positionError, prevPositionError);
		
		return motion;
	}
	
	private void updatePositionErrorOneCube(AutopilotInputs inputs, Point3D target) {
		prevPositionError = positionError;
		positionError = (float) (PdControl.getDistanceFaultOneCube(inputs, target, dronePosStart));

		if (ENABLE_LOGGING && printPID) {
			System.out.println("---positionError "+positionError);
			System.out.println("---prevPositionError "+prevPositionError);
		}
	}
	
}
