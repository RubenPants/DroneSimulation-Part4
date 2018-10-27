package autopilot_planning;

import autopilot_utilities.Point3D;
import autopilot_utilities.Vector3f;
import interfaces.AutopilotInputs;

public interface Heading {
	
	static float getDeltaHeading(AutopilotInputs inputs, Point3D target) {
		Vector3f requestedVector = new Vector3f(
				target.getX() - inputs.getX(), 
				0, 
				target.getZ() - inputs.getZ());
		float deltaHeading = (float) (inputs.getHeading() - Math.atan2(-requestedVector.x, -requestedVector.z));
		if (deltaHeading > Math.PI) deltaHeading -= 2*Math.PI;
		else if (deltaHeading < -Math.PI) deltaHeading += 2*Math.PI;
		
		return -deltaHeading;
	}

}
