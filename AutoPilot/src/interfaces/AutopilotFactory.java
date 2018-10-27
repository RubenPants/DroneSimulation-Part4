package interfaces;

/**
 * A class of autopilot factories for generating autopilots.
 * 
 * @author	Team Saffier
 * @version 	1.0
 */
public class AutopilotFactory {

	/**
	 * Create an autopilot.
	 * 
	 * @return	The newly created autopilot.
	 */
	public static Autopilot createAutopilot() {
		return new DroneAutopilot();
	}
	
}