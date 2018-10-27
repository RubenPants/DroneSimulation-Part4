package interfaces;

/**
 * An interface for autopilot output objects having a thrust, left - and right wing
 * 	inclination, and horizontal - and vertical stabilizer inclination.
 * 
 * @author	Team Saffier
 * @version	1.0
 */
public interface AutopilotOutputs {
	
	/**
	 * Return the thrust value for this output object.
	 */
    float getThrust();
    
    /**
	 * Return the left wing inclination value for this output object.
	 */
    float getLeftWingInclination();
    
    /**
	 * Return the right wing inclination value for this output object.
	 */
    float getRightWingInclination();
    
    /**
	 * Return the horizontal stabilizer inclination value for this output object.
	 */
    float getHorStabInclination();
    
    /**
	 * Return the vertical stabilizer inclination value for this output object.
	 */
    float getVerStabInclination();
    
    /**
     * Return the front brake force for this output object.
     */
    float getFrontBrakeForce();
    
    /**
     * Return the left brake force for this output object.
     */
    float getLeftBrakeForce();
   
    /**
     * Return the right brake force for this output object.
     */
    float getRightBrakeForce();
    
}