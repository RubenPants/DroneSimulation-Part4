package interfaces;

import java.util.ArrayList;
import java.util.List;

import autopilot_gui.AutopilotGUI;
import autopilot_physics.AutopilotPhysics;
import autopilot_planning.AutopilotMotionPlanner;
import autopilot_planning.Distance;
import autopilot_planning.DroneStage;
import autopilot_planning.OneCube;
import autopilot_utilities.Path3D;
import autopilot_utilities.Point3D;
import autopilot_utilities.Vector3f;

/**
 * A class of autopilots for steering a drone. Autopilots translate a drone's
 * configuration and constantly updated situation to output commands, such that
 * that very drone can reach its desired destination.
 * 
 * @author Team Saffier
 * @version 1.0
 */
public class DroneAutopilot implements Autopilot {	

	// Package delivery
		public Package assignedPackage = null;
		public boolean hasPickedupPackage = false;
		public float getSpeed() {
			return physics.getSSquared();
		}
	
	// Communication
	public boolean free_enabled = false;	// Priority 1
	public boolean airportStack_enabled = false;	// Priority 2
	public boolean dubins_enabled = true;	// Priority 3
	public float requestedSpeedFactor = 1;
	public boolean ENABLE_LOGGING = false;
	
	private AutopilotMotionPlanner motionPlanner = new AutopilotMotionPlanner();
	private OneCube oneCube = new OneCube();
	public List<Point3D> targetCoordinates = new ArrayList<>();

	private float reachWidthLong = 55;
	private float reachWidthShort = 40;
	private float takeOffHeight = 30f;
	
	/**
	 * Initialise this autopilot.
	 * 
	 * @effect This autopilot is initialised with a new image analyser.
	 */
	public DroneAutopilot() {
		this.planner = new AutopilotMotionPlanner();
		this.physics = new AutopilotPhysics();
		this.planner.ENABLE_LOGGING = false;
		this.physics.ENABLE_LOGGING = false;
	}

	/**
	 * Variable referencing a motion planner.
	 */
	private AutopilotMotionPlanner planner;

	/**
	 * This autopilot's physics engine.
	 */
	private AutopilotPhysics physics;

	/**
	 * Variable referencing a gui for this autopilot.
	 */
	@SuppressWarnings("unused")
	private AutopilotGUI gui;

	/**
	 * Set this autopilot's configuration to the given one.
	 * 
	 * @param configuration
	 *            The new configuration for this autopilot.
	 */
	public void setConfiguration(AutopilotConfig configuration) {
		if (configuration == null)
			return;
		this.configuration = configuration;
		if (this.physics != null)
			this.physics.setConfiguration(configuration);
	}

	/**
	 * Returns this autopilot's current configuration.
	 */
	public AutopilotConfig getConfiguration() {
		return this.configuration;
	}

	/**
	 * This autopilot's current configuration.
	 */
	private AutopilotConfig configuration;

	/**
	 * Returns whether or not this autopilot uses motion planning.
	 */
	public boolean getUsesPlanner() {
		return usesPlanner;
	}

	/**
	 * Set whether or not this autopilot uses motion planning.
	 * 
	 * @param usesPlanner
	 *            True if and only if the planner is to be used.
	 */
	public void setUsesPlanner(boolean usePlanner) {
		this.usesPlanner = usePlanner;
	}

	/**
	 * Variable registering whether or not motion planning is applied.
	 */
	private boolean usesPlanner = false;
	
	// Time variables
	private float previousTimeElapsed = -0.02f;
	private float timeElapsed = 0;
	private float deltaTimeElapsed = 0.02f;	
	
	/**
	 * Last coordinates that were registered.
	 */
	public Point3D location = new Point3D(0.0f, 0.0f, 0.0f);
	
	/**
	 * Let this autopilot's image analyser analyse the given input.
	 * 
	 * @param inputs
	 *            The autopilot input that is to be analysed.
	 * @return The results of the analysis by the image analyser and the
	 *         physical model.
	 */
	private AutopilotOutputs analyseInputs(AutopilotInputs inputs, boolean firstFrame) {
		
		location = new Point3D(inputs.getX(), inputs.getY(), inputs.getZ());
		
		if (ENABLE_LOGGING) {
			System.out.println("________________________________________________________");
			if (targetCoordinates == null || targetCoordinates.size() == 0) System.out.println("targetCoordinates: []");
			else {
				System.out.print("targetCoordinates: [");
				for (Point3D target: targetCoordinates) {
					System.out.print("["+target.getX()+","+target.getY()+","+target.getZ()+"]");
					System.out.print(",");
				}
				System.out.println("]");
			}
		}
		
		
		
		timeElapsed = inputs.getElapsedTime();
		deltaTimeElapsed = (timeElapsed - previousTimeElapsed);
		previousTimeElapsed = timeElapsed;
		if (deltaTimeElapsed == 0.0) {
			deltaTimeElapsed = 0.017f;
		}

		if (firstFrame)
			physics.getInputsStart(inputs);
		else
			physics.getInputs(inputs);
		
		
		// Remove the first next target when reached
		if (!targetCoordinates.isEmpty() &&
				((targetCoordinates.get(0).getY() >= 0 && isReached(targetCoordinates.get(0), inputs, true)) ||
				(targetCoordinates.get(0).getY() < 0 && isReached(targetCoordinates.get(0), inputs, false))))
			targetCoordinates.remove(0);
		
		
		// In this case the drone will do nothing but brake only on its first wheel (to stop creating tension between its wheels)
		if (targetCoordinates == null || targetCoordinates.isEmpty() || free_enabled) {	
			if (ENABLE_LOGGING) System.out.println("FREE");
			
			return physics.output(inputs.getPitch(), inputs.getHeading(), 0f, DroneStage.FREE, deltaTimeElapsed);
		} 
		
		
		// All targets are beneath the surface --> Taxi
		else if (targetCoordinates.get(0).getY() < 0 && inputs.getY() < 5 && physics.getSSquared() < 300) {
			if (ENABLE_LOGGING) System.out.println("TAXI");
			
			float[] motion = motionPlanner.getPitchAndHeading(configuration, inputs, targetCoordinates, DroneStage.TAXI, deltaTimeElapsed);
			
			float speedFactor = 1;
			if (getTargetCoordinatesSize() == 1) {
				speedFactor = Distance.distanceTo3D(targetCoordinates.get(0), inputs)/100;
			}
			if (speedFactor > 1) speedFactor = 1;
			
			return physics.output(motion[0], motion[1], speedFactor, DroneStage.TAXI, deltaTimeElapsed);
		} 
		
		// All the targets are beneath the surface but drone is still flying --> Land
		else if (targetCoordinates.size() == 1 || targetCoordinates.get(0).getY() < 0) {
			if (inputs.getY() > 5 && physics.getSSquared() > 400) {
				if (ENABLE_LOGGING) System.out.println("LAND");
				
				float[] motion = motionPlanner.getPitchAndHeading(configuration, inputs, targetCoordinates, DroneStage.LAND, deltaTimeElapsed);
				
				return physics.output(motion[0], motion[1], 1f, DroneStage.LAND, deltaTimeElapsed);
			} else {
				if (ENABLE_LOGGING) System.out.println("TAXI");
				
				float distanceToTarget = Distance.distanceToHor(targetCoordinates.get(0), inputs);
				float speedFactor = 1;
				if (distanceToTarget < 100) speedFactor = distanceToTarget/100;
			
				float[] motion = motionPlanner.getPitchAndHeading(configuration, inputs, targetCoordinates, DroneStage.TAXI, deltaTimeElapsed);
				
				return physics.output(motion[0], motion[1], speedFactor, DroneStage.TAXI, deltaTimeElapsed);
			}
		} 
		
		
		// The drone is still on the ground, but the next target is in the air --> Take off
		else if (inputs.getY() <= takeOffHeight) {
			if (ENABLE_LOGGING) System.out.println("TAKE_OFF");
			
			float[] motion = motionPlanner.getPitchAndHeading(configuration, inputs, targetCoordinates, DroneStage.TAKE_OFF, deltaTimeElapsed);
			
			return physics.output(motion[0], motion[1], 1f, DroneStage.FLY, deltaTimeElapsed);
 		} 
		
		
		// The drone is flying, and in stead of trying to reach to first next cube, circle around it (clockwise) --> Stack
		else if (airportStack_enabled) {
			if (ENABLE_LOGGING) System.out.println("STACK");
			
			float[] motion = motionPlanner.getPitchAndHeading(configuration, inputs, targetCoordinates.get(0), DroneStage.WAITING_TO_LAND, deltaTimeElapsed);
			
			return physics.output(motion[0], motion[1], requestedSpeedFactor, DroneStage.WAITING_TO_LAND, deltaTimeElapsed);
		} 
		
		
		// Fly and try to reach the next cubes in the list --> Fly
		else {
			if (ENABLE_LOGGING) System.out.println("FLY");
			
			float[] motion;
			if (dubins_enabled)
				motion = motionPlanner.getPitchAndHeading(configuration, inputs, targetCoordinates, DroneStage.FLY, deltaTimeElapsed);
			else
				motion = oneCube.getMotion(targetCoordinates.get(0), inputs, 400, deltaTimeElapsed);
			
			return physics.output(motion[0], motion[1], requestedSpeedFactor, DroneStage.FLY, deltaTimeElapsed);
 		}		
	}

	/**
	 * Notify the autopilot that the simulation started.
	 * 
	 * @param 	config
	 *          The configuration of the drone this autopilot represents.
	 * @param 	inputs
	 *          The current situation of the drone this autopilot steers.
	 * @return 	Output commands for the drone, based on the given input
	 *         	parameters.
	 * @category API
	 */
	public AutopilotOutputs simulationStarted(AutopilotConfig config, AutopilotInputs inputs) {
		setConfiguration(config);
		return analyseInputs(inputs, true);
	}

	/**
	 * Notify this autopilot of the new situation for the drone it's steering.
	 * 
	 * @param inputs
	 *            The updated situation for the drone.
	 * @return Output commands for steering the drone, based on its most recent
	 *         situation.
	 * @category API
	 */
	public AutopilotOutputs timePassed(AutopilotInputs inputs) {
		return analyseInputs(inputs, false);
	}

	/**
	 * The input path for this autopilot.
	 */
	private Path3D path;

	/**
	 * Set the path for this autopilot.
	 * 
	 * @param path
	 *            The path for this autopilot.
	 * @category API
	 */
	public void setPath(Path path) {
		try {
			this.path = new Path3D(path);
			// new PathReorden().reordenPath(this.path, new Point3D(0.0, 50, -550), 0);
		} catch (Exception exception) {
			System.out.println("Input path was either empty or invalid.");
		}
	}

	@Override
	public void simulationEnded() {		
	}
	
	/**
	 * Check if the drone got the target or not
	 * 
	 * @param requestedPoint
	 * @param inputs
	 * @param wide
	 * 
	 * @return True if target is reached
	 */
	private boolean isReached(Point3D requestedPoint, AutopilotInputs inputs, boolean wide) {
		Vector3f targetPointVector = new Vector3f(
				requestedPoint.getX() - inputs.getX(), 
				0, 
				requestedPoint.getZ() - inputs.getZ());
		float reqHeading = (float) Math.atan2(-targetPointVector.x, -targetPointVector.z);
		double headingDifference = Math.min(Math.abs(inputs.getHeading() - reqHeading),
				Math.PI * 2 - Math.abs(inputs.getHeading() - reqHeading));
		
		// Taxi points can be reached when drone is not on the ground!
		if (requestedPoint.getY() < -1 && inputs.getY() > 10) return false;
		
		if (wide) {
			return (Distance.distanceToHor(requestedPoint, inputs) < reachWidthLong || // Successfully reached
					(headingDifference > Math.PI/2 && Distance.distanceToHor(requestedPoint, inputs) < 200));	// Drone has past it
		} else {
			if (getTargetCoordinatesSize() == 1) {
				if (physics.getSSquared() < 0.01) return true;
				else return false;
				/*return (Distance.distanceToHor(requestedPoint, inputs) < reachWidthShortLastTarget || // Successfully reached
						(headingDifference > Math.PI/2 && Distance.distanceToHor(requestedPoint, inputs) < 200));	// Drone has past it
				*/
			} else {
				return (Distance.distanceToHor(requestedPoint, inputs) < reachWidthShort || // Successfully reached
						(headingDifference > Math.PI/2 && Distance.distanceToHor(requestedPoint, inputs) < 200));	// Drone has past it
			}
		}
	}

	/**
	 * Add a target (point) at the front of the list
	 * @param target
	 */
	public void addTargetToFrontOfList(Point3D target) {
		targetCoordinates.add(0, target.copy());
	}
	
	/**
	 * Add a target (point) at the end of the list
	 * @param target
	 */
	public void addTargetToEndOfList(Point3D target) {
		targetCoordinates.add(getTargetCoordinatesSize(), target.copy());
	}
	
	/**
	 * Remove the target (point) at the front of the list if there is a target
	 * @param target
	 */
	public void removeFirstTarget() {
		if (targetCoordinates.size() > 0)
			targetCoordinates.remove(0);
	}
	
	/**
	 * Remove the target (point) at the end of the list if there is a target
	 * @param target
	 */
	public void removeLastTarget() {
		if (targetCoordinates.size() > 0)
			targetCoordinates.remove(targetCoordinates.size()-1);
	}
	
	/**
	 * Remove all targets out of the list
	 */
	public void removeAllTargets() {
		if (targetCoordinates != null) targetCoordinates.clear();
	}
	
	/**
	 * @return The amount of targets in the list
	 */
	public int getTargetCoordinatesSize() {
		return targetCoordinates.size();
	}
	
	/**
	 * @return The first target in the list (null if there is no target)
	 */
	public Point3D getFirstTarget() {
		if (getTargetCoordinatesSize() > 0)
			return targetCoordinates.get(0);
		else
			return null;
	}
	
	/**
	 * A list with all the airports the drone has to go to (in order)
	 */
	private ArrayList<Airport> targetAirports = new ArrayList<Airport>();
	
	/**
	 * A list with all the gates the drone has to go to (in order) 
	 */
	private ArrayList<Integer> targetGates = new ArrayList<Integer>();
	
	/**
	 * @return The first next airport the drone has to go to
	 */
	public Airport getFirstTargetAirport() {
		return (targetAirports.isEmpty() ? null : targetAirports.get(0));
	}
	
	/**
	 * @return The first next gate the drone has to go to
	 */
	public Integer getFirstTargetGate() {
		return (targetGates.isEmpty() ? null : targetGates.get(0));
	}
	
	/**
	 * Add a new airport (and corresponding gate) to the list of targetAirports (and targetGates)
	 * @param airport
	 * @param gate
	 */
	public void addTargetAirport(Airport airport, int gate) {
		targetAirports.add(airport);
		targetGates.add(gate);
	}
	
	/**
	 * Delete the first targetAirport (and corresponding gate) from the list of targetAirports (and targetGates)
	 */
	public void removeFirstTargetAirport() {
		if (targetAirports.size() > 0) {
			targetAirports.remove(0);
			targetGates.remove(0);
		}
	}
	
	/**
	 * Remove all targets out of the list
	 */
	public void removeAllTargetAirports() {
		if (targetCoordinates != null) targetCoordinates.clear();
		if (targetAirports != null) targetAirports.clear();
		if (targetGates != null) targetGates.clear();
	}
	
	/**
	 * Set a new y-value for the first target in list. Only no taxi-points can be modified!
	 * 
	 * @param newHeight
	 */
	public void setFirstTargetHeight(float newHeight) {
		if (getTargetCoordinatesSize() > 0 && targetCoordinates.get(0).getY() > 0) {
			targetCoordinates.get(0).setY(newHeight);
		}
	}
	
	public boolean temporarilyRaised = false;
	public float temporaryRaise = 0.0f;
	public int temporaryRaiseAirportIndex;
	
	/**
	 * Get the height of the first target in the list
	 * 
	 * @return The height of the first target in the list
	 */
	public float getFirstTargetHeight() {
		if (getTargetCoordinatesSize() == 0) return -1;
		return (float) targetCoordinates.get(0).getY();
	}

}