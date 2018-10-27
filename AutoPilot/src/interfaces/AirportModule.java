package interfaces;

import java.util.ArrayList;

import autopilot_planning.Distance;
import autopilot_planning.Heading;
import autopilot_utilities.Point3D;

/**
 * A class of airport modules
 * 
 * @author	Team Saffier
 * @version	1.0
 */
public class AirportModule {
	
	public boolean ENABLE_LOGGING = false;

	// The airport associated with this module
	public Airport airport;
	
	// Registers the drones for this airport.
	public ArrayList<DroneAutopilot> activeDrones = new ArrayList<DroneAutopilot>();
	
	// The list of drone in the stack
	private ArrayList<DroneAutopilot> dronesInStack = new ArrayList<DroneAutopilot>();;
	
	// The list depicting which drones are in lower airspace (40m - 70m)
	private ArrayList<DroneAutopilot> dronesInLowerAirspace = new ArrayList<DroneAutopilot>();;
	
	// The drone in the gates
	public Point3D centerAirport;
	public DroneAutopilot droneOnLane0, droneOnLane1;
	public DroneAutopilot droneInGate0, droneInGate1;
	
	// Other variables
	private float rho = 400;
	private float maxStackFault = 40f;
	private float bottomOfStack = 70f;
	private float levelHeight = 30f;
	private float defaultFlyHeight = 100f;
	private float spaceInStackBetweenDrones = 100f;
	
	/**
	 * Registers this airport module with given airport.
	 */
	public AirportModule(Airport airport) {
		this.airport = airport;
		centerAirport = new Point3D(this.airport.centerX, 70f, this.airport.centerZ);
	}
	
	/**
	 * Handle drone.
	 */
	public void handleDrone(DroneAutopilot drone, AutopilotInputs inputs) {
		if (ENABLE_LOGGING) printStats(drone);
		
		// The drone has to do nothing, unless it stands in the way of another drone
		if (drone.getFirstTargetAirport() == null) {
			if (ENABLE_LOGGING) System.out.println("DRONE AT REST");
			
			if (drone.getTargetCoordinatesSize() == 0)
				drone.free_enabled = true;	// No airport requested and no targets --> Let drone wait
						
			// Clear lanes when drone is in the gate
			if (droneInGate0 == drone) {
				if (Distance.distanceToHor(this.airport.pointGate0, inputs) < 100 && droneOnLane0 == drone)
					droneOnLane0 = null;
			} else {
				if (Distance.distanceToHor(this.airport.pointGate1, inputs) < 100 && droneOnLane1 == drone)
					droneOnLane1 = null;
			}
			
			// Check if somebody wants to land at the gate the drone is in
			if (!dronesInStack.isEmpty()) {
				// Determine which gates are requested
				boolean otherDroneWantsGate0 = false;
				boolean otherDroneWantsGate1 = false;
				for (DroneAutopilot otherDrone: dronesInStack) {
					if (otherDrone.getFirstTargetGate() == 0)
						otherDroneWantsGate0 = true;
					else
						otherDroneWantsGate1 = true;
				}

				// If a drone is parked at the other gate, then say it is wanted
				if (droneInGate0 != null && droneInGate0 != drone) otherDroneWantsGate0 = true;
				if (droneInGate1 != null && droneInGate1 != drone) otherDroneWantsGate1 = true;
				
				// Determine the gate the drone is in
				int gateInt = 0;
				if (droneInGate1 == drone) gateInt = 1;
				
				// Decide what the drone must do
				if (otherDroneWantsGate0 && otherDroneWantsGate1) {	// Always leave
					AirportModule otherModule = getBestAirportModuleTarget();
					if (otherModule.droneInGate0 == null) {
						drone.addTargetAirport(otherModule.airport, 0);
					} else {
						drone.addTargetAirport(otherModule.airport, 1);
					}
					
					if (gateInt == 0 && droneOnLane1 == null) {
						letDroneTakeOff(drone);	
					} else if (droneOnLane0 == null) {	// gateInt == 1
						letDroneTakeOff(drone);	
					} // else --> Wait till lane is free (another drone is probably landing)
				}
				
				// Only the gate the drone is in is requested --> Move drone to other gate
				else if (gateInt == 0 && otherDroneWantsGate0 && !otherDroneWantsGate1 && droneOnLane1 == null) {	// otherDroneWantsGate1 == false
					letDroneTaxiToOtherGate(drone, gateInt);
				} else if (gateInt == 1 && otherDroneWantsGate1 && !otherDroneWantsGate0 && droneOnLane0 == null) {	// otherDroneWantsGate0 == false
					letDroneTaxiToOtherGate(drone, gateInt);
				}
				// else --> Do nothing
			}
		}
		
		
		// Drone wants to go to another airport
		else if (drone.getFirstTargetAirport() != this.airport) {
			if (ENABLE_LOGGING) System.out.println("DRONE TAKING OFF");	
			// Drone first has to take off
			if (drone == droneInGate0 || drone == droneInGate1) {
				// Check if another drone is already landing, if not, the drone may take off
				if (!dronesInLowerAirspace.isEmpty()) {
					drone.removeAllTargets();
				} 
				
				// Plane will take off when needed lane is free
				else {	
					if (droneInGate0 == drone) {
						if (droneOnLane1 == null) {
							letDroneTakeOff(drone);
						} else {
							drone.removeAllTargets();
						}
					} else { // droneInGate1 == drone
						if (droneOnLane0 == null) {
							letDroneTakeOff(drone);
						} else {
							drone.removeAllTargets();
						}
					}
				}
			} else {
				// The drone that is flying away from the airport is not in lower airspace any more so remove it
				if (inputs.getY() > 70) {
					dronesInLowerAirspace.remove(drone);
				}
				
				if (Distance.distanceToHor(centerAirport, inputs) > 500) {
					if (droneOnLane0 == drone) droneOnLane0 = null;
					else if (droneOnLane1 == drone) droneOnLane1 = null;
				}
				
				if (Distance.distanceToHor(getCenterAirport(), inputs) < 1000) {
					if (drone.getFirstTargetHeight() > 50)
						drone.setFirstTargetHeight(bottomOfStack);
				} else {
					// Check if it is possible to release the drone
					boolean releaseDrone = true;
					float deltaHeading;
					float lowestHeight = defaultFlyHeight;
					for (DroneAutopilot otherDrones : activeDrones) {
						deltaHeading = Heading.getDeltaHeading(inputs, otherDrones.location);
						if (Math.abs(deltaHeading) < Math.PI/2) {
							releaseDrone = false;
							if (otherDrones.location.getY() < lowestHeight) {
								lowestHeight = (float)otherDrones.location.getY();
							}
						}
					}
					
					if (releaseDrone) {	// Push drone back to master module
						if (droneInGate0 == drone) droneInGate0 = null;
						else if (droneInGate1 == drone) droneInGate1 = null;
						if (droneOnLane0 == drone) droneOnLane0 = null;
						else if (droneOnLane1 == drone) droneOnLane1 = null;
						
						drone.setFirstTargetHeight(defaultFlyHeight);
						activeDrones.remove(drone);
						dronesInLowerAirspace.remove(drone);
						AutopilotModule.defaultModule().airportReleasedDrone(drone);
					} else {	// Fly to higher airspace with at most 30meters under the lowest flying drone
						float flyHeight = lowestHeight - levelHeight;
						if (flyHeight < bottomOfStack) flyHeight = bottomOfStack;
						drone.setFirstTargetHeight(flyHeight);
						
						if (Distance.distanceToHor(getCenterAirport(), inputs) > 1000) {
							if (droneInGate0 == drone) droneInGate0 = null;
							else if (droneInGate1 == drone) droneInGate1 = null;
							if (droneOnLane0 == drone) droneOnLane0 = null;
							else if (droneOnLane1 == drone) droneOnLane1 = null;
						}
					}
				}
			}
		}
		
		// Drone wants to land at this airport
		else {
			if (ENABLE_LOGGING) System.out.println("DRONE LANDING");

			// Drone is landed, remove airport target from list
			if (drone.getTargetCoordinatesSize() == 0 && drone.getFirstTargetAirport() != null &&
					((droneInGate0 == drone && drone.getFirstTargetGate() == 0) || droneInGate1 == drone && drone.getFirstTargetGate() == 1)) {
				drone.removeFirstTargetAirport();
			} 
			
			// Handle landing
			else {
				if (inputs.getY() < 5) {
					// When drone is landed, it is not in lower airspace anymore
					dronesInLowerAirspace.remove(drone);
					
					// Clear lanes when drone is in the gate
					if (drone.getFirstTargetGate() == 0) {
						if (Distance.distanceToHor(this.airport.pointGate0, inputs) < 150 && droneOnLane0 == drone)
							droneOnLane0 = null;
					} else {
						if (Distance.distanceToHor(this.airport.pointGate1, inputs) < 150 && droneOnLane1 == drone)
							droneOnLane1 = null;
					}
					
					// Check if drone needs to taxi to other gate
					if (droneInGate0 == drone && drone.getFirstTargetGate() == 1) {
						if (droneInGate1 == null && droneOnLane1 == null)
							letDroneTaxiToOtherGate(drone, 0);
					} else if (droneInGate1 == drone && drone.getFirstTargetGate() == 0) {
						if (droneInGate0 == null && droneOnLane0 == null)
							letDroneTaxiToOtherGate(drone, 1);
					}
				}
				
				// if (requested gate and lane is free)
				if (droneGateIsStrictFree(drone) && isOnlyDroneInLowerAirspace(drone)) {	// If to close to the airport let stack handle drone, '< 55' --> Drone is already landing
					if (dronesInStack.size() != 0) {	// Handle stack first
						// Add drone to list of stack if it is not already there
						if (!dronesInStack.contains(drone)) dronesInStack.add(drone);
						
						// Check if drone is the first in the stack who wants to land at given gate, otherwise stay on stack
						boolean firstInListWithGate = true;
						for (int i = 0; i < dronesInStack.indexOf(drone) - 1 && firstInListWithGate; i++) {
							if (dronesInStack.get(i).getFirstTargetGate() == drone.getFirstTargetGate()) {
								firstInListWithGate = false;
							}
						}
						
						if (firstInListWithGate) {	// Let the drone land
							if (drone.getFirstTargetHeight() > 45) {	// Assign right coordinates to drone
								handleDroneInStack(drone, inputs);
							} else {	// Drone already on right track, remove from stack (drone was added in begin of method)!
								dronesInStack.remove(drone);
							}
						} else {	// Let drone fly on stack
							handleDroneInStack(drone, inputs);						
						}
					} else {
						if (drone.getFirstTargetHeight() > 45) {
							letDroneLand(drone);
						}
						// else --> Drone already on right track! Leave it like that
					}
				} else {
					if ((!isOnlyDroneInLowerAirspace(drone) || !droneGateIsFree(drone)) && inputs.getY() > 25)
						handleDroneInStack(drone, inputs);
					// else --> Drone is landing
				}
			}
		}
	}
	
	/**
	 * Let the drone fly on the stack. If the drone is not on the stack then get the drone onto the stack its top.
	 * If the drone is in the stack, try to get it to the bottom. The distance between two drones must always be at
	 * least 100m! It is possible to increase or decrease the speed of the drone.
	 * 
	 * @param drone
	 * @param inputs
	 */
	private void handleDroneInStack(DroneAutopilot drone, AutopilotInputs inputs) {
		// Add drone to list of stack if it is not already there
		if (!dronesInStack.contains(drone)) dronesInStack.add(drone);
		if (dronesInLowerAirspace.contains(drone)) dronesInLowerAirspace.remove(drone);
		
		// Get and stay on the stack
		drone.airportStack_enabled = true;
		if (Distance.distanceToHor(drone.getFirstTarget(),getCenterAirport()) >= 1.0)
			drone.addTargetToFrontOfList(getCenterAirport());
		
		// First check if drone is first in stack with requested gate!
		if (droneGateIsFree(drone)) {
			boolean droneMayLand = false;
			if (dronesInStack.size() > 1) {
				if (isOnlyDroneInLowerAirspace(drone)) droneMayLand = true;
				// Check if drone is first in stack who wants to land to requested gate
				for (int i = 0; i < dronesInStack.indexOf(drone) && droneMayLand; i++) {
					if (dronesInStack.get(i).getFirstTargetGate() == drone.getFirstTargetGate())
						droneMayLand = false;
				}
			} else {
				if (Distance.distanceToHor(getCenterAirport(), inputs) < rho+maxStackFault && isOnlyDroneInLowerAirspace(drone))
					droneMayLand = true;	// Drone is only in stack --> No threat!
			}

			// Let the drone land
			if (droneMayLand) {
				float deltaHeading = 0;
				if (drone.getFirstTargetGate() == 0)
					deltaHeading = (float) (inputs.getHeading() - Math.atan2(-this.airport.centerToRunway0X, -this.airport.centerToRunway0Z));
				else
					deltaHeading = (float) (inputs.getHeading() - Math.atan2(this.airport.centerToRunway0X, this.airport.centerToRunway0Z));
				
				if (deltaHeading > Math.PI) deltaHeading -= 2*Math.PI;
				else if (deltaHeading < -Math.PI) deltaHeading += 2*Math.PI;
				if (Math.abs(deltaHeading) < Math.abs(Math.PI/180)) {	// The drone has the fitted heading to land
					// Release drone from stack and let it land
					letDroneLand(drone);
				}
			}
			else {
				dronesInLowerAirspace.remove(drone);
				flyInStack(drone, inputs);
			}	
		} else {
			flyInStack(drone, inputs);
		}
	}
	
	/**
	 * Submodule of handleDroneInStack. This method will try to get the drone as low as possible in the stack, 
	 * this by increasing or decreasing the speed of the drone.
	 * 
	 * @param drone
	 * @param inputs
	 */
	private void flyInStack(DroneAutopilot drone, AutopilotInputs inputs) {
		if (Distance.distanceToHor(getCenterAirport(), inputs) < rho + maxStackFault) {	// Drone is in the stack
			if (Distance.distanceToClosestDroneBeneathOwn(drone, dronesInStack) > spaceInStackBetweenDrones) {	// Enough space to descend drone
				drone.setFirstTargetHeight(bottomOfStack);

				// Check if drone above own drone that wants to land// Adjust speed to enlarge distance between drone and closest other drone
				Point3D closestDronePos = Distance.getClosestDronePosition(drone, dronesInStack);
				if (closestDronePos == null) {
					drone.requestedSpeedFactor = 1f;
				} else {
					float distanceBetween = Distance.distanceToHor(closestDronePos, inputs);
					if (distanceBetween > spaceInStackBetweenDrones || Math.abs(Heading.getDeltaHeading(inputs, closestDronePos)) < Math.PI/2) {
						drone.requestedSpeedFactor = 1f;
					} else if (Math.abs(Heading.getDeltaHeading(inputs, closestDronePos)) > Math.PI/2) {	// Closest drone behind own drone
						// Speed up
						drone.requestedSpeedFactor = 1.1f;
					}
					// Do not slow down due to poor flying quality!
				}
			} else {
				// Adjust speed to enlarge distance between drone and closest other drone
				Point3D closestDronePos = Distance.getClosestDronePositionBeneathOwn(drone, dronesInStack);
				if (closestDronePos == null) {
					drone.requestedSpeedFactor = 1f;
				} else {
					float distanceBetween = Distance.distanceToHor(closestDronePos, inputs);
					if (distanceBetween > spaceInStackBetweenDrones || Math.abs(Heading.getDeltaHeading(inputs, closestDronePos)) < Math.PI/2) {
						drone.requestedSpeedFactor = 1f;
					} else if (Math.abs(Heading.getDeltaHeading(inputs, closestDronePos)) > Math.PI/2) {	// Closest drone behind own drone
						// Speed up
						drone.requestedSpeedFactor = 1.1f;
					}
					// Do not slow down due to poor flying quality!
				}
				
				if (inputs.getY() - 5 > closestDronePos.getY()) {	// The '5' is a threshold
					drone.setFirstTargetHeight((float) (closestDronePos.getY() + levelHeight));
				} else {
					drone.setFirstTargetHeight((float) (inputs.getY()));	// Stay at the same level
				}
			}
		} else {	// Get on stack
			float heightRequested = getStackHeight();
			float otherDroneHeight;
			for (int i = 0; i < dronesInStack.indexOf(drone) - 1; i++) {
				// Consider only drones that are not in the stack but are (also) getting on the stack
				if (Distance.distanceToHor(getCenterAirport(), dronesInStack.get(i).location) > rho + maxStackFault) {
					if (Distance.distanceToHor(dronesInStack.get(i).location, inputs) < 300) {
						otherDroneHeight = (float)(dronesInStack.get(i).location.getY());
						if (otherDroneHeight + levelHeight > heightRequested + 5) {	// The '5' is a threshold
							heightRequested = otherDroneHeight + levelHeight;
						}			
					}
				}
			}
			
			drone.setFirstTargetHeight(heightRequested);
			drone.requestedSpeedFactor = 1f;
		}
	}
	
	/**
	 * Register drone.
	 */
	public void registerDrone(DroneAutopilot drone) {
		activeDrones.add(drone);
	}
	
	/**
	 * Initialize a given drone at the given gate
	 * 
	 * @param drone
	 * @param gate
	 */
	public void initDroneInAirportModule(DroneAutopilot drone, int gate) {
		System.out.println("Drone : " + drone + " in gate " + gate);
		activeDrones.add(drone);
		if (gate == 0) {
			droneInGate0 = drone;
		} else {
			droneInGate1 = drone;
		}
	}
	
	/**
	 * Returns whether or not this module registered the given drone.
	 */
	public boolean registeredDrone(DroneAutopilot drone) {
		return activeDrones.contains(drone);
	}
	
	/**
	 * Check if the requested gate of a given drone is free
	 * 
	 * @param drone
	 * 
	 * @return True if the gate and lane are both free
	 */
	private boolean droneGateIsFree(DroneAutopilot drone) {
		return ((drone.getFirstTargetGate() == 0 && (droneInGate0 == null || droneInGate0 == drone) && (droneOnLane0 == null || droneOnLane0 == drone)) || 	
				(drone.getFirstTargetGate() == 1 && (droneInGate1 == null || droneInGate1 == drone) && (droneOnLane1 == null || droneOnLane1 == drone)));
	}
	
	private boolean droneGateIsStrictFree(DroneAutopilot drone) {
		return ((drone.getFirstTargetGate() == 0 && droneInGate0 == null && droneOnLane0 == null) || 	
				(drone.getFirstTargetGate() == 1 && droneInGate1 == null && droneOnLane1 == null));
	}
	
	/**
	 * Return the height of the upper most drone within the stack, additional with a certain threshold (levelheigt).
	 * 
	 * @return	The height of the stack
	 */
	public float getStackHeight() {	// Define height of stack as highestDrone IN stack + levelHeight		
		// No one in stack, first drone may fly on the bottom of the stack
		if (dronesInStack.size() == 0) return bottomOfStack;
		
		// else: highestDroneInStack + levelHeight
		float highestDrone = bottomOfStack;
		for (DroneAutopilot droneInStack: dronesInStack) {
			if (Distance.distanceToHor(getCenterAirport(), droneInStack.location) < rho + maxStackFault) {	// Check if drone IN stack
				if (droneInStack.location.getY() > highestDrone)
					highestDrone = (float)(droneInStack.location.getY());
			}
		}
		
		return highestDrone+levelHeight;
	}
	
	/**
	 * Prepare the drone for landing to its requested gate
	 * 
	 * @param drone
	 */
	private void letDroneLand(DroneAutopilot drone) {
		// Add drone to lower airspace and remove from stack (if in stack)
		if (!dronesInLowerAirspace.contains(drone)) dronesInLowerAirspace.add(drone);
		dronesInStack.remove(drone);
		
		// Set landing targets and fly to them using Dubins
		drone.removeAllTargets();
		drone.free_enabled = false;
		drone.airportStack_enabled = false;
		drone.dubins_enabled = true;
		drone.requestedSpeedFactor = 1f;
		if (drone.getFirstTargetGate() == 0) {
			drone.addTargetToEndOfList(this.airport.prepareLandingPoint0);
			drone.addTargetToEndOfList(this.airport.endPointLane0);
			drone.addTargetToEndOfList(this.airport.pointGate0);
			
			// Set gate and lanes as busy
			droneInGate0 = drone;
			droneOnLane0 = drone;
		} else {
			drone.addTargetToEndOfList(this.airport.prepareLandingPoint1);
			drone.addTargetToEndOfList(this.airport.endPointLane1);
			drone.addTargetToEndOfList(this.airport.pointGate1);
			
			// Set gate and lanes as busy
			droneInGate1 = drone;
			droneOnLane1 = drone;
		}
	}

	/**
	 * Prepare the drone for take off
	 * 
	 * @param drone
	 */
	private void letDroneTakeOff(DroneAutopilot drone) {
		// Add drone to lower airspace and remove from stack (if in stack)
		if (!dronesInLowerAirspace.contains(drone)) dronesInLowerAirspace.add(drone);
		dronesInStack.remove(drone);	// Normally redundant
		
		// Set landing targets and fly to them using Dubins
		drone.removeAllTargets();
		drone.free_enabled = false;
		drone.airportStack_enabled = false;
		drone.dubins_enabled = true;
		drone.requestedSpeedFactor = 1f;
		
		if (droneInGate0 == drone) {
			drone.addTargetToEndOfList(this.airport.startPointLane1);
			drone.addTargetToEndOfList(this.airport.startSecondPointLane1);
			
			// Set lane 1 as busy
			droneOnLane1 = drone;
			// Set Gate 0 as free
			droneInGate0 = null;
		} else {
			drone.addTargetToEndOfList(this.airport.startPointLane0);
			drone.addTargetToEndOfList(this.airport.startSecondPointLane0);
			
			// Set lane 0 as busy
			droneOnLane0 = drone;
			// Set Gate 1 as free
			droneInGate1 = null;
		}
		
		
		if (drone.getFirstTargetGate() == 0) {
			drone.addTargetToEndOfList(drone.getFirstTargetAirport().prepareLandingPoint0);
			drone.addTargetToEndOfList(drone.getFirstTargetAirport().endPointLane0);
			drone.addTargetToEndOfList(drone.getFirstTargetAirport().pointGate0);
		} else {
			drone.addTargetToEndOfList(drone.getFirstTargetAirport().prepareLandingPoint1);
			drone.addTargetToEndOfList(drone.getFirstTargetAirport().endPointLane1);
			drone.addTargetToEndOfList(drone.getFirstTargetAirport().pointGate1);
		}
	}
	
	/**
	 * Let the drone taxi to the other gate
	 * 
	 * @param drone
	 * @param startGateInt
	 */
	public void letDroneTaxiToOtherGate(DroneAutopilot drone, int startGateInt) {
		drone.free_enabled = false;
		drone.airportStack_enabled = false;
		drone.dubins_enabled = false;
		
		if (startGateInt == 0) {
			drone.addTargetToEndOfList(this.airport.startPointLane1);
			drone.addTargetToEndOfList(this.airport.endPointLane1);
			drone.addTargetToEndOfList(this.airport.pointGate1);
			
			// Set lane and gate as busy
			droneOnLane1 = drone;
			droneInGate1 = drone;
			
			// Remove drone from own gate
			droneInGate0 = null;
		} else {
			drone.addTargetToEndOfList(this.airport.startPointLane0);
			drone.addTargetToEndOfList(this.airport.endPointLane0);
			drone.addTargetToEndOfList(this.airport.pointGate0);
			
			// Set lane and gate as busy
			droneOnLane0 = drone;
			droneInGate0 = drone;
			
			// Remove drone from own gate
			droneInGate1 = null;			
		}
	}
	
	/**
	 * Find the closest airport (by its module) from this airport that has no drones at it, if no such airport exists,
	 * 	then return the closest airport its module that only has one operative drone
	 * 
	 * @return The best fitted AirportModule
	 */
	private AirportModule getBestAirportModuleTarget() {
		AirportModule closestAirportModule = null;
		float closestDistance = Float.POSITIVE_INFINITY;
		
		// Find closest airport with no drones
		for (AirportModule otherAirportModule: AutopilotModule.defaultModule().getAirportModules()) {
			if (otherAirportModule != this && otherAirportModule.getAmountOfActiveDrones() == 0) {
				if (Distance.distanceToHor(this.getCenterAirport(), otherAirportModule.getCenterAirport()) < closestDistance) {
					closestDistance = Distance.distanceToHor(this.getCenterAirport(), otherAirportModule.getCenterAirport());
					closestAirportModule = otherAirportModule;
				}
			}
		}
		
		// If requested airport is found, then return this one
		if (closestAirportModule != null) return closestAirportModule;
		
		// Find the closes airport where there is only one drone
		for (AirportModule otherAirportModule: AutopilotModule.defaultModule().getAirportModules()) {
			if (otherAirportModule != this && otherAirportModule.getAmountOfActiveDrones() == 1) {
				if (Distance.distanceToHor(this.getCenterAirport(), otherAirportModule.getCenterAirport()) < closestDistance) {
					closestDistance = Distance.distanceToHor(this.getCenterAirport(), otherAirportModule.getCenterAirport());
					closestAirportModule = otherAirportModule;
				}
			}
		}
		
		// It is impossible that closestAirportModule is still null
		return closestAirportModule;
	}
	
	private boolean isOnlyDroneInLowerAirspace(DroneAutopilot drone) {
		if (dronesInLowerAirspace.size() == 0) return true;
		return isStrictOnlyDroneInLowerAirspace(drone);
	}
	
	private boolean isStrictOnlyDroneInLowerAirspace(DroneAutopilot drone) {
		return (dronesInLowerAirspace.size() == 1 && dronesInLowerAirspace.contains(drone));
	}
	
	private boolean isLanding(DroneAutopilot drone) {
		return ((droneInGate0 == drone && droneOnLane0 == drone) ||
				(droneInGate1 == drone && droneOnLane0 == drone));
	}
	
	/**
	 * @return The amount of active airports
	 */
	private int getAmountOfActiveDrones() {
		return activeDrones.size();
	}
	
	/**
	 * @return centerAirport
	 */
	private Point3D getCenterAirport() {
		return this.centerAirport;
	}
	
	/**
	 * Print out all the parameters
	 */
	private void printStats(DroneAutopilot drone) {
		System.out.println("");
		System.out.println("AirportModule");
		System.out.println("--ENTITIES");
		System.out.println("---drone: "+drone);
		System.out.println("-----position: "+drone.location);
		System.out.println("---airport: "+this.airport);
		System.out.println("---airportModule: "+this);
		System.out.println("--GATES");
		System.out.println("---droneInGate0: "+droneInGate0);
		System.out.println("---droneInGate1: "+droneInGate1);
		System.out.println("--LANES");
		System.out.println("---droneOnLane0: "+droneOnLane0);
		System.out.println("---droneOnLane1: "+droneOnLane1);
		System.out.println("--LISTS");
		System.out.println("---activeDrones: "+activeDrones.size());
		System.out.println("-----Contains our drone? "+activeDrones.contains(drone));
		System.out.println("---dronesInLowerAirspace: "+dronesInLowerAirspace.size());
		System.out.println("-----Contains our drone? "+dronesInLowerAirspace.contains(drone));
		if (dronesInLowerAirspace.size() > 0)
			System.out.println("-----First drone in lower airspace: "+dronesInLowerAirspace.get(0));
		System.out.println("---dronesInStack: "+dronesInStack.size());
		System.out.println("-----Contains our drone? "+dronesInStack.contains(drone));
		System.out.println("-----Position in stack: "+dronesInStack.indexOf(drone));
		System.out.println("--DRONE PARAMETERS");
		System.out.println("---free enabled: "+drone.free_enabled);
		System.out.println("---stack enabled: "+drone.airportStack_enabled);
		System.out.println("---dubins enabled: "+drone.dubins_enabled);
		System.out.println("---speedfactor: "+drone.requestedSpeedFactor);
		System.out.println("--AIRPORT PARAMTERS");
		System.out.println("---stackHeight: "+getStackHeight());
		System.out.println("--ALL DRONES");
		for (DroneAutopilot droneIterater: activeDrones) {
			System.out.println("---active: "+activeDrones.indexOf(droneIterater)+", lower airspace: "+dronesInLowerAirspace.indexOf(droneIterater)+", stack: "+dronesInStack.indexOf(droneIterater)+", ID: "+droneIterater);
		}
	}
}

/*
 * TODO: Default vlieghoogte = 100m --> Aanpassen wanneer drone over airport vliegt maar niet hier moet zijn (getStackHeight)
 * 
 * Krijg controle van de drone binnen een straal van 2000m
 * 2000m - 1000m: 	Krijg controle over de drone en beslis landing of stack
 * 					Controleer eerst of de drone kan dalen (dus niet dat de drone in de buurt zit van 
 * 					een andere drone, anders gaat de besturing terug naar de hoofd-motion planner.
 * 1000: - 0m:	Indien de drone nog steeds niet landing-klaar is (i.e. niet in landing-pad) en te hoog vliegt
 * 				dan zal de drone naar de stack gebracht worden, waar hij al dalend zal draaien rond het centrum
 * 				van de airport. Eenmaal aan de 40m zal dubins geinitialiseerd worden voor de landing. Geef ook door
 * 				aan de rest van de drones dat er een drone aan het landen is!
 * FREE:	Alle drones die in de luchthaven aan het wachten zijn of willen vertrekken mogen pas vertrekken indien
 * 			het luchtruim vrij is, er mag dus geen enkele drone zijn niet of in de stack zit, of actief aan het
 *			landen is (actief landen kan immers pas wanneer bijhorende gate ook vrij is --> Wordt op voorhand
 *			gecontrolleerd).
 */

/* --> PROTOCOL <--
 * Drones vliegen gekwaniseerd op niveaus, met elk 30m er tussen
 * Airport: 40m (only landing/take_off) - 70m - 100m - 130m --> Met Default hoogte 70m
 * Casual vliegen: 140m - 170m - 200m - 230m - 260m - 290m --> Met Default hoogte 100m
 * 
 * --> targetCoordinates <--
 * Airport: y-coordinaat moet kleiner dan nul zijn om aan te geven dat coordinaat zich op de grond bevindt!
 * Landing: 900m van eindpunt op een hoogte van 40m
 */