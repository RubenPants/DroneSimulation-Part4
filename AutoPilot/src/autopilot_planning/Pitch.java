package autopilot_planning;

import autopilot_utilities.Point3D;
import autopilot_utilities.Vector3f;
import interfaces.AutopilotInputs;

public interface Pitch {
	
	// TODO: Deviate when crossing with another drone? --> Handled by the main planner?
	
	/**
	 * Return the angle of pitch the drone should have at the given moment
	 * 
	 * @param inputs
	 * @param target
	 * 
	 * @return   The pitch (radians)
	 */
	static float getPitch(AutopilotInputs inputs, Point3D target) {
		if (target != null) {
			float pitch;
			Vector3f requestedVector = new Vector3f(
					target.getX() - inputs.getX(), 
					target.getY() - inputs.getY(),
					target.getZ() - inputs.getZ());
			
			pitch = (float) Math.atan(requestedVector.y / 
					(Math.sqrt(requestedVector.z * requestedVector.z + requestedVector.x * requestedVector.x)));
			float shortPitch = (float) Math.atan(requestedVector.y / 100.0);

			// Positive pitch
			if (shortPitch > 0) {
				if (shortPitch > Math.PI / 12 && Math.abs(inputs.getRoll()) < Math.PI/36) shortPitch = (float) (Math.PI/12);
				else if (shortPitch > Math.PI / 18 && Math.abs(inputs.getRoll()) < Math.PI/18) shortPitch = (float) (Math.PI / 18);
				else if (shortPitch > Math.PI / 36) shortPitch = (float) (Math.PI/36);
			} 
			// Negative pitch
			else if (shortPitch < -Math.PI / 30) shortPitch = (float) -Math.PI / 30;
			
			// If needed pitch is greater in absolure value then shortPitch, than use needed pitch!
			if (pitch > 0 && pitch < shortPitch) return shortPitch;
			if (pitch < 0 && pitch > shortPitch) return shortPitch;
			
			else return pitch;
		} else
			return (float) Math.PI/36;
	}

}
