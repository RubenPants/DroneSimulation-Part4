package autopilot_planning;

import java.util.List;

import autopilot_utilities.Point3D;
import interfaces.AutopilotConfig;
import interfaces.AutopilotInputs;

public class AutopilotMotionPlanner {
	
	/*
	 * TODO
	 * Wachtrijen --> Twee cilinders naast luchthaven: 
	 * 1) De stack (start vanaf 100 meter), iedere 50m zal een vliegtuig hier ronddraaien, terwijl deze 	
	 * 		wacht om te kunnen landen
	 * 2) Dalings-cilinder, indien een cilinder klaar is om te landen, maar te hoog (+150m) in de stack 
	 * 		zit, dan zal deze via deze cilinder al cirkelend naar beneden vliegen. Wanneer de 150m grens
	 * 		behaald is zal een nieuwe dubins opgeroepen worden om zo de landing te maken
	 */

	private Dubins dubins = new Dubins();
	private Taxi taxi = new Taxi();
	private OneCube oneCube = new OneCube();
	private AirportStack airportStack = new AirportStack();
	
	/**
	 * Variable denoting whether or not this object logs information.
	 */
	public boolean ENABLE_LOGGING = false;

	// Turning radius = forward velocity divided by maximum angular velocity
	private final static float TURNING_RADIUS = 400.0f; 
	private final static float TAXI_RADIUS = 15.0f; 

	/**
	 * Calculate the requested pitch and heading for a given drone in a given drone stage with a given destination
	 * 
	 * @param configuration
	 * @param inputs
	 * @param targetCoordinates
	 * @param stage
	 * @param deltaTimeElapsed
	 * 
	 * @return   The requested pitch and heading
	 */
	public float[] getPitchAndHeading(AutopilotConfig configuration, AutopilotInputs inputs, List<Point3D> targetCoordinates, 
			DroneStage stage, float deltaTimeElapsed) {		
		switch (stage) {
		case TAKE_OFF:
			if (inputs.getY() < 10)
				return new float[]{(float)Math.PI/36, inputs.getHeading()};
			else 
				return new float[]{(float)Math.PI/18, inputs.getHeading()};
		case FLY:
			if (targetCoordinates.size() >= 2 && targetCoordinates.get(0).getY() > 0)
				return dubins.calculateTrajectory(targetCoordinates, inputs, TURNING_RADIUS, deltaTimeElapsed);
			else
				return oneCube.getMotion(targetCoordinates.get(0), inputs, TAXI_RADIUS, deltaTimeElapsed);
		case TAXI:
			return taxi.getMotion(targetCoordinates, inputs, TAXI_RADIUS, deltaTimeElapsed);
		case WAITING_TO_LAND:
			return airportStack.handleDroneInAirportStack(inputs, targetCoordinates.get(0), deltaTimeElapsed, inputs.getY());
		default:	// LAND, FLY_ONE_CUBE, FREE
			return oneCube.getMotion(targetCoordinates.get(0), inputs, TAXI_RADIUS, deltaTimeElapsed);
		}
	}
	

	public float[] getPitchAndHeading(AutopilotConfig configuration, AutopilotInputs inputs, Point3D target, 
			DroneStage stage, float deltaTimeElapsed) {		
		switch (stage) {
		case TAKE_OFF:
			if (inputs.getY() < 10)
				return new float[]{(float)Math.PI/36, inputs.getHeading()};
			else 
				return new float[]{(float)Math.PI/18, inputs.getHeading()};
		case TAXI:
			return taxi.getMotion(target, inputs, TAXI_RADIUS, deltaTimeElapsed);
		case WAITING_TO_LAND:
			return airportStack.handleDroneInAirportStack(inputs, target, deltaTimeElapsed, inputs.getY());
		default:	// LAND, FLY, FLY_ONE_CUBE, FREE
			return oneCube.getMotion(target, inputs, TAXI_RADIUS, deltaTimeElapsed);
		}
	}
	
}
