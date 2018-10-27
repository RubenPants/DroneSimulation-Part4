package autopilot_planning;

import java.util.List;

import autopilot_utilities.Point3D;
import autopilot_utilities.Vector3f;
import interfaces.AutopilotInputs;

public class Taxi {
	
	private boolean ENABLE_LOGGING = false;
	
	private float positionError = 0;
	private float prevPositionError = 0;
	private Point3D dronePosStart;
	private float maxFault = 2f;
	
	public float[] getMotion(List<Point3D> targetCoordinates, AutopilotInputs inputs, float turningRadius, float deltaTimeElapsed) {
		float[] motion = new float[2];
		
		if (dronePosStart == null || Math.abs(positionError) > maxFault)
			dronePosStart = new Point3D(inputs.getX(), inputs.getY(), inputs.getZ());
		
		updatePositionErrorOneCube(inputs, targetCoordinates.get(0));
		
		Vector3f requestedVector = new Vector3f(
				targetCoordinates.get(0).getX() - inputs.getX(), 
				0, 
				targetCoordinates.get(0).getZ() - inputs.getZ());
		float deltaHeading = (float) (inputs.getHeading() - Math.atan2(-requestedVector.x, -requestedVector.z));
		if (deltaHeading > Math.PI) deltaHeading -= 2*Math.PI;
		else if (deltaHeading < -Math.PI) deltaHeading += 2*Math.PI;

		if (ENABLE_LOGGING) System.out.println("deltaHeading: "+Math.abs(deltaHeading));
	 	
		float motionHeading = PdControl.getPdPositionOneCube(inputs, targetCoordinates.get(0), deltaTimeElapsed, positionError, prevPositionError);
		if (deltaHeading > Math.PI/8) motionHeading -= (float) (Math.PI/16);	// Roll of 33.75
		else if (deltaHeading < -Math.PI/8) motionHeading += (float) (Math.PI/16);	// Roll of 33.75
		motion[1] = motionHeading;// PdControl.getPdPositionOneCube(inputs, targetCoordinates.get(0), deltaTimeElapsed, positionError, prevPositionError);
		
		if (ENABLE_LOGGING) {
			System.out.println("TAXI");
			System.out.println("---requestedPitch: "+Math.toDegrees(motion[0]));
			System.out.println("---requestedHeading: "+Math.toDegrees(motion[1]));
		}
		
		return motion;
	}
	
	public float[] getMotion(Point3D target, AutopilotInputs inputs, float turningRadius, float deltaTimeElapsed) {
		float[] motion = new float[2];
		
		if (dronePosStart == null || Math.abs(positionError) > maxFault)
			dronePosStart = new Point3D(inputs.getX(), inputs.getY(), inputs.getZ());
		
		updatePositionErrorOneCube(inputs, target);
		
		Vector3f requestedVector = new Vector3f(
				target.getX() - inputs.getX(), 
				0, 
				target.getZ() - inputs.getZ());
		float deltaHeading = (float) (inputs.getHeading() - Math.atan2(-requestedVector.x, -requestedVector.z));
		if (deltaHeading > Math.PI) deltaHeading -= 2*Math.PI;
		else if (deltaHeading < -Math.PI) deltaHeading += 2*Math.PI;

		if (ENABLE_LOGGING) System.out.println("deltaHeading: "+Math.abs(deltaHeading));

		float motionHeading = PdControl.getPdPositionOneCube(inputs, target, deltaTimeElapsed, positionError, prevPositionError);
		if (deltaHeading > Math.PI/8) motionHeading -= (float) (Math.PI/16);	// Roll of 33.75
		else if (deltaHeading < -Math.PI/8) motionHeading += (float) (Math.PI/16);	// Roll of 33.75
		motion[1] = motionHeading;// PdControl.getPdPositionOneCube(inputs, targetCoordinates.get(0), deltaTimeElapsed, positionError, prevPositionError);
		
		if (ENABLE_LOGGING) {
			System.out.println("TAXI");
			System.out.println("---requestedPitch: "+Math.toDegrees(motion[0]));
			System.out.println("---requestedHeading: "+Math.toDegrees(motion[1]));
		}
		
		return motion;
	}
	
	private void updatePositionErrorOneCube(AutopilotInputs inputs, Point3D target) {
		prevPositionError = positionError;
		positionError = (float) (PdControl.getDistanceFaultOneCube(inputs, target, dronePosStart));
	}

}
