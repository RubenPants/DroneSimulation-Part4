package interfaces;

import java.util.ArrayList;
import java.util.Collections;

import autopilot_planning.Distance;
import autopilot_planning.Heading;
import autopilot_utilities.Point3D;

/**
 * A class of autopilot modules.
 * 
 * @author	Team Saffier
 * @version	1.0
 */
public class AutopilotModule {

	private static boolean ENABLE_LOGGING = false;
	private static boolean ruben_scheduler = false;

	/**
	 * Instantiate an autopilot module.
	 * 
	 * @note This returns the default module, as this is a singleton class.
	 */
	public AutopilotModule() {
		
		// Create singleton if it hasn't been done already
		if (defaultModule != null)
			throw new IllegalArgumentException("Stop that! Singleton class AutopilotModule already instantiated.");
		defaultModule = this;		
	}

	/**
	 * Get the default autopilot module.
	 *  This is a singleton.
	 */
	public static AutopilotModule defaultModule() {
		if (defaultModule == null) {
			defaultModule = new AutopilotModule();
		}
		return defaultModule;
	}

	/**
	 * The singleton instance.
	 */
	private static AutopilotModule defaultModule = null;

	/**
	 * Define the parameters of airports
	 * 
	 * @param 	length
	 * 			The length of airports.
	 * @param 	width
	 * 			The width of airports.
	 */
	public void defineAirportParams(float length, float width) {
		Airport.setAirportParameters(length, width);
	}

	/**
	 * Define  an airport.
	 * 	(centerToRunway0X, centerToRunway0Z) constitutes a unit vector pointing
	 * 	from the center of the airport towards runway 0.
	 * 
	 * @param 	centerX
	 * 			The x coordinate of the center of this airport.
	 * @param 	centerZ
	 * 			The z coordinate of the center of this airport.
	 */
	public void defineAirport(float centerX, float centerZ, float centerToRunway0X, float centerToRunway0Z) {
		Airport airport = new Airport(centerX, centerZ, centerToRunway0X, centerToRunway0Z);
		airport.ID = airports.size();
		airports.add(airport);
		airportModules.add(new AirportModule(airport));
	}
	
	/**
	 * Returns the list of airport modules for this autopilot module.
	 */
	public ArrayList<AirportModule> getAirportModules() {
		return airportModules;
	}

	/**
	 * Registers the airports associated with this module.
	 */
	private ArrayList<AirportModule> airportModules = new ArrayList<AirportModule>();
	private ArrayList<Airport> airports = new ArrayList<Airport>();
	
	/**
	 * Called by an airport when it releases a drone.
	 */
	public void airportReleasedDrone(DroneAutopilot drone) {
		activeDrones.add(drone);
	}

	/**
	 * Define a new drone at given airport, gate, original orientation and configuration.
	 * 
	 * @param 	airport
	 * 			The airport at which the drone is located.
	 * @param 	gate
	 * 			The gate in which the drone is.
	 * @param 	pointingToRunway
	 * 			The index of the gate towards which the drone is orientated (= 0 or 1).
	 * @param 	config
	 * 			The configuration of the drone/
	 */
	public void defineDrone(int airport, int gate, int pointingToRunway, AutopilotConfig config) {		
		DroneAutopilot drone = new DroneAutopilot();
		drone.setConfiguration(config);
		drone.location = (gate == 0 ? airports.get(airport).pointGate0.copy() : airports.get(airport).pointGate1.copy());
		drone.location.setY(4.7f);
		drones.add(drone);
		airportModules.get(airport).initDroneInAirportModule(drone, gate);
		outputs.add(null);
	}

	// Registers the radius for the area around an airport that it manages
	public static final double AIRPORT_RADIUS = 2000;
	
	public static boolean SCHEDULING = false;
	
	private int iterations = 0;
		
	/**
	 * Notify the given drone of its current situation.
	 * 
	 * @param 	drone
	 * 			The index of the drone (0-N if N drones have been defined).
	 * @param 	inputs
	 * 			The new inputs for the drone.
	 */
	public void startTimeHasPassed(int drone, AutopilotInputs inputs) {
		//synchronized(this) {
			
			// TODO
			if (ruben_scheduler) {
				
				/*
				 *  Ultra naive algorithm - The main idea
				 * 
				 *  If drone picked up packet --> Let it fly towards packet destination
				 *  Drones in rest --> Handle packageQueue
				 *  Reserve packets --> If drone in same airport as packet and reserved, overwrite!
				 *  
				 *   Hierarchy
				 *   1) Got package --> Fly towards airport (do not schedule drone! This already has been done)
				 *   2) In same airport as package (package reserved or not) --> Taxi to package if package is not picked up
				 *   3) In other airport and package not reserved --> Reserve and fly towards package
				 *   4) No packages left --> Rest
				 *   
				 *   
				 *   NOTE: Only handle drones that are standing still! The others are already on 'a mission'
				 *   --> Queue the music "He is a man on a mission!" *guitar solo*
				 */
				
				// System.out.println("Drone speed (autopilot)" + Math.sqrt(drones.get(drone).getSpeed()));
				
				if (drones.get(drone).getSpeed() < 1.0f) {
					DroneAutopilot usedDrone = drones.get(drone);
					
					AirportModule airportModule = getAirportModule(usedDrone);
					
					int droneInGate = 0;
					if (Distance.distanceToHor(airportModule.airport.pointGate1, usedDrone.location) < 100) droneInGate = 1;
					
					
					// 1) Check if drone needs to drop of its package in this airport
					if (usedDrone.hasPickedupPackage && drones.get(drone).getSpeed() < 0.01f) {	// Only drop package down when standing still!
						if (airports.get(usedDrone.assignedPackage.toAirport) == airportModule.airport) {
							// Check first if in right gate --> If correct gate then drop package
							if (usedDrone.assignedPackage.toGate == droneInGate) {
								usedDrone.hasPickedupPackage = false;
								usedDrone.assignedPackage = null;
							} else { // Taxi to other gate
								letDroneTaxiToOtherGate(usedDrone, droneInGate, airportModule);
							}
						} else if (usedDrone.getFirstTargetAirport() == null) {	// Try to fool-proof the code
							if (airports.get(usedDrone.assignedPackage.fromAirport) != airportModule.airport) {
								usedDrone.removeFirstTargetAirport();
								
								Package pack = usedDrone.assignedPackage;
								
								packageQueue.remove(pack);	// Already true
								pack.transporter = usedDrone;	// Already true
								usedDrone.hasPickedupPackage = true;	// Already true
								usedDrone.assignedPackage = pack;	// Already true
								
								// Assign target airport to drone
								usedDrone.removeAllTargetAirports();
								usedDrone.addTargetAirport(airports.get(pack.toAirport), pack.toGate);
							}
						}
					}
					
					
					// --> If the drone has not yet a package -> Give it one if possible <-- //
					
					
					// 2) Check if drone picks up a package at current gate
					if (!usedDrone.hasPickedupPackage && usedDrone.getFirstTargetAirport() == null) {
						Package newPackage = getPackageInAirportGate(airportModule, droneInGate);
						
						// If newPackage != null --> Assign new package to drone and remove package from queue!
						if (newPackage != null) {
							packageQueue.remove(newPackage);
							newPackage.transporter = usedDrone;
							usedDrone.hasPickedupPackage = true;
							usedDrone.assignedPackage = newPackage;
							
							// Assign target airport to drone
							usedDrone.removeAllTargetAirports();
							usedDrone.addTargetAirport(airports.get(newPackage.toAirport), newPackage.toGate);
						}
						

						// 3) Check if package in other side of airport that is not picked up yet --> Taxi
						// Do not reserve or pick up the packet yet! Just taxi to the other gate
						else { // (newPackage == null)
							Package otherGatePackage = getPackageInAirportOtherGate(airportModule, droneInGate);
							
							if (otherGatePackage != null) {
								letDroneTaxiToOtherGate(usedDrone, droneInGate, airportModule);
							}
							
							
							// 4) Fly to nearest packet that is not yet reserved
							else { // (otherGatePackage == null)
								Package bestPackage = getClosestPackageOtherAirport(usedDrone);
								
								if (bestPackage != null) {
									bestPackage.reserver = usedDrone;
									
									// Assign target airport to drone
									usedDrone.removeAllTargetAirports();
									usedDrone.addTargetAirport(airports.get(bestPackage.fromAirport), bestPackage.fromGate);
								}
							}
						}
					}
				}
			} else {
			
				
				
				
				
				
				
				
				
				
				
				
			if (drone == 0) { // Only once assign packages in the queue
				
				// PackageScheduler scheduler = new PackageScheduler();
				
				iterations++;
				
				// Determine # free drones + PTCs
				ArrayList<PTC> combinations = new ArrayList<PTC>();
				int freeDrones = 0, freePackages = 0;
				boolean determinedNbFreeDrones = false;
				for (Package p : packageQueue) {
					if (p.transporter == null) {
						freePackages++;
						Airport targetAirport = airports.get(p.fromAirport);
						Point3D targetLocation = (p.fromGate == 0 ? targetAirport.pointGate0 : targetAirport.pointGate1);
						for (int i=0 ; i<drones.size() ; i++) {
							DroneAutopilot d = drones.get(i);
							if (droneIsFree(d)) { // Free drone
								if (!determinedNbFreeDrones)
									freeDrones++;
								PTC ptc = new PTC();
								ptc.autopilot = d;
								ptc.p = p;
								ptc.dist = d.location.distanceTo(targetLocation);
								combinations.add(ptc);
							}
							else if (iterations == 2500) {
								if (ENABLE_LOGGING) System.out.println("Drone " + d.getTargetCoordinatesSize() + " " + d.getFirstTargetAirport() + " " + d.hasPickedupPackage + " " + d.assignedPackage);
							}
						}
						determinedNbFreeDrones = true;
					}
				}
				Collections.sort(combinations);
				if (iterations == 2500)
					if (ENABLE_LOGGING) System.out.println("Free drones = " + freeDrones);
				
				// Now keep dealing with packages until # min(free drones, free packages) are assigned
				// This is just to send drones to the packages in a way that makes it as fast as possible (naive interpretation)
				int dealtPackages = 0;
				int ptcIdx = 0;
				while (dealtPackages < Math.min(freePackages, freeDrones) && ptcIdx < combinations.size()) {
					PTC ptc = combinations.get(ptcIdx);
					if (ENABLE_LOGGING) System.out.println(ptc);
					if (droneIsFree(ptc.autopilot) && ptc.p.transporter == null) { // Make sure drone/package are still free
						ptc.p.transporter = ptc.autopilot;
						ptc.autopilot.addTargetAirport(airports.get(ptc.p.fromAirport), ptc.p.fromGate);
						ptc.autopilot.assignedPackage = ptc.p;
						if (ENABLE_LOGGING) System.out.println("Sending " + ptc.autopilot + " to " + ptc.p.fromAirport);
						dealtPackages++;
					}
					ptcIdx++;
				}
				
				
				if (ENABLE_LOGGING && iterations == 2500) {
					System.out.println("# packages : " + packageQueue.size());
					for (Package p : packageQueue)
						System.out.println(p);
				}
				
				// Now go over the packages and check if drones are ready to pick them up or deliver them
				for (int i=0 ; i<packageQueue.size() ; i++) {
					Package p = packageQueue.get(i);
					if (!p.scheduling) {
						
						AirportModule fromModule = airportModules.get(p.fromAirport), toModule = airportModules.get(p.toAirport);
						DroneAutopilot dronePickupGate = (p.fromGate == 0 ? fromModule.droneInGate0 : fromModule.droneInGate1);
						DroneAutopilot droneDestinationGate = (p.toGate == 0 ? toModule.droneInGate0 : toModule.droneInGate1);		
						
						if (ENABLE_LOGGING && iterations == 2500) {
							System.out.println("For " + p + " -> " + dronePickupGate + " && <- " + droneDestinationGate);
						}
						
						if (dronePickupGate != null 
							&& !dronePickupGate.hasPickedupPackage 
							&& !p.pickedUp
							&& fromModule.airport.inGate(p.fromGate, dronePickupGate.location)
							&& dronePickupGate.getSpeed() < 1.0f) { // A drone can pickup the package
							//dronePickupGate.removeAllTargets();
							//dronePickupGate.removeFirstTargetAirport();
							if (p.transporter != null)
								p.transporter.assignedPackage = null;
							if (dronePickupGate.assignedPackage != null)
								dronePickupGate.assignedPackage.transporter = null;
							p.pickedUp = true;
							p.transporter = dronePickupGate;
							dronePickupGate.assignedPackage = p;
							dronePickupGate.hasPickedupPackage = true;
							dronePickupGate.addTargetAirport(airports.get(p.toAirport), p.toGate);
							if (ENABLE_LOGGING) System.out.println("Picked up " + p);
						}
										
						if (droneDestinationGate != null 
								&& droneDestinationGate.getSpeed() < 1.0f
								&& toModule.airport.inGate(p.toGate, droneDestinationGate.location)
								&& droneDestinationGate.hasPickedupPackage 
								&& droneDestinationGate.assignedPackage == p) {
							droneDestinationGate.assignedPackage = null;
							droneDestinationGate.hasPickedupPackage = false;
							if (p.transporter != null)
								p.transporter.assignedPackage = null;
							packageQueue.remove(i--);
							if (ENABLE_LOGGING) System.out.println("Delivered " + p);
						}
						
						// TODO - Comment taxi to other side?
						if (p.transporter != null) {
							DroneAutopilot dronePickupLane = (p.fromGate == 0 ? fromModule.droneOnLane0 : fromModule.droneOnLane1);
							DroneAutopilot droneOtherGate = (p.fromGate == 0 ? fromModule.droneInGate1 : fromModule.droneInGate0);
							if (dronePickupGate == null && dronePickupLane == null && droneOtherGate != null && droneOtherGate.getTargetCoordinatesSize() == 0 && droneOtherGate.getFirstTargetAirport() == null)
								droneOtherGate.addTargetAirport(fromModule.airport, p.fromGate);
						}
						
					}
				}

			}
			
			//TODO
			}
			
			
			if (iterations == 2500)
				iterations = 0;
						
			// Deal with the current drone
			DroneAutopilot droneAutopilot = drones.get(drone);		
			if (activeDrones.contains(droneAutopilot)) { // Active drone handled by this 
				Point3D location = new Point3D(inputs.getX(), inputs.getY(), inputs.getZ());
				Airport targetAirport = droneAutopilot.getFirstTargetAirport();
									
					if (targetAirport != null && location.distanceTo(new Point3D(targetAirport.centerX, location.getY(), targetAirport.centerZ)) < AIRPORT_RADIUS) {
						airportModules.get(airports.indexOf(targetAirport)).registerDrone(droneAutopilot);
						activeDrones.remove(droneAutopilot);
						airportModules.get(airports.indexOf(targetAirport)).handleDrone(droneAutopilot, inputs);
					}
					else {
						
						boolean canLower = true;
						for (int j=drone+1 ; j<drones.size() ; j++) { // Check for collisions (real-time)
							DroneAutopilot otherDrone = drones.get(j);
							if (location.distanceTo(otherDrone.location) < 500.0f) {
								if (!droneAutopilot.temporarilyRaised) {
									droneAutopilot.temporaryRaise = droneAutopilot.getFirstTargetHeight();
									droneAutopilot.setFirstTargetHeight(droneAutopilot.temporaryRaise + 30.0f);
									droneAutopilot.temporarilyRaised = true;
									if (ENABLE_LOGGING) System.out.println("Rise to : " + (droneAutopilot.temporaryRaise + 30.0f) + " from : " + droneAutopilot.getFirstTargetHeight());
								}
								canLower = false;
							}
						}
												
						// Only change height of drone coordinates if drone is flying
						if (inputs.getY() > 5 && drones.get(drone).getFirstTargetHeight() > 0) {
							// Code checks if drone flies over (non-target) airport and if so makes sure it is above its stack
							// If it isn't flying above any airport it makes sure it's altitude is returned to baseline
							boolean raisedInIteration = false;
							for (int i=0 ; i<airports.size() ; i++) {
								Airport airport = airports.get(i);
								Point3D center = new Point3D(airport.centerX, location.getY(), airport.centerZ);
								double dist = location.distanceTo(center);
								if (airport != targetAirport && dist < AIRPORT_RADIUS) {
									
									float deltaHeading = Math.abs(Heading.getDeltaHeading(inputs, center));
									if (deltaHeading < Math.PI/2) { // Flying to airport
										
										if (!droneAutopilot.temporarilyRaised || droneAutopilot.temporaryRaiseAirportIndex != i) {
											if (!droneAutopilot.temporarilyRaised)
												droneAutopilot.temporaryRaise = droneAutopilot.getFirstTargetHeight();
											float safeHeight = airportModules.get(i).getStackHeight() + 30.0f;
											if (droneAutopilot.getFirstTargetHeight() < safeHeight) {
												if (ENABLE_LOGGING) System.out.println("Rise to : " + safeHeight + " from : " + droneAutopilot.getFirstTargetHeight());
												droneAutopilot.setFirstTargetHeight(safeHeight);
												droneAutopilot.temporarilyRaised = true;
												droneAutopilot.temporaryRaiseAirportIndex = i;
											}
										}
										
										raisedInIteration = true;
										canLower = false;
										
									}
									else if (dist > 500.0 && droneAutopilot.temporarilyRaised && !raisedInIteration && canLower) { // Flying away
										// droneAutopilot.temporaryRaise = 0.0; // Not necessary
										canLower = (canLower && true);
									}
									else {
										canLower = false;
									}
																
								}
							}
						}
						
						if (canLower && droneAutopilot.temporarilyRaised) {
							if (ENABLE_LOGGING) System.out.println("Lower to : " + droneAutopilot.temporaryRaise);
							droneAutopilot.temporarilyRaised = false;
							droneAutopilot.setFirstTargetHeight(droneAutopilot.temporaryRaise);
						}
						
					}
			}
			else {
				for (AirportModule module : airportModules)
					if (module.registeredDrone(droneAutopilot)) {
						module.handleDrone(droneAutopilot, inputs);
						break;
					}
			}
			
			outputs.set(drone, droneAutopilot.timePassed(inputs));
		//}
	}	
	
	
	// TODO - Refernce point
	/**
	 * Get the airportModule the drone is in
	 * 
	 * @param drone
	 */
	private AirportModule getAirportModule(DroneAutopilot drone) {
		if (drone.getSpeed() > 2.0f) return null;
		else {
			for (AirportModule module : airportModules) {
				if (Distance.distanceToHor(module.centerAirport, drone.location) < 500) {
					return module;
				}
			}
		}
		return null;
	}
	
	/**
	 * Get the package that is in the given airport and gate (if there is one)
	 * 
	 * @param module
	 * @param gate
	 * @return
	 */
	private Package getPackageInAirportGate(AirportModule module, int gate) {
		for (Package pack: packageQueue) {
			if (airports.get(pack.fromAirport) == module.airport &&
					pack.fromGate == gate) {
				return pack;
			}
		}
		return null;
	}
	
	/**
	 * Get the package that is in the given airport and the other gate (if there is one)
	 * 
	 * @param module
	 * @param gate
	 * @return
	 */
	private Package getPackageInAirportOtherGate(AirportModule module, int gate) {
		for (Package pack: packageQueue) {
			if (airports.get(pack.fromAirport) == module.airport &&
					pack.fromGate == (gate+1)%2) {
				return pack;
			}
		}
		return null;
	}
	
	// TODO
	private Package getClosestPackageOtherAirport(DroneAutopilot drone) {
		// Check the closest package (from another airport) that is not yet reserved
		float closestDistance = Float.POSITIVE_INFINITY;
		float distance;
		Package bestPackage = null;
		
		for (Package pack: packageQueue) {
			if (pack.reserver == null && pack.transporter == null) {	// transporter == null is redundant
				distance = Distance.distanceToHor(airports.get(pack.fromAirport).getCenter(), drone.location);
				if (distance < closestDistance && distance > 1000) {
					closestDistance = distance;
					bestPackage = pack;
				}
			}
		}
		
		return bestPackage;
	}
	
	/**
	 * Let the drone taxi to the other gate
	 * 
	 * @param drone
	 */
	private void letDroneTaxiToOtherGate(DroneAutopilot drone, int gate, AirportModule airportModule) {
		if (airportModule.droneOnLane0 == null && airportModule.droneOnLane1 == null) {	// Redundant code to make sure that there will be no duplicates!
			if ((gate == 0 && airportModule.droneInGate1 == null) || (gate == 1 && airportModule.droneInGate0 == null)) {
				drone.addTargetAirport(airportModule.airport, (gate+1)%2);
			}
		}
	}
	
	/**
	 * Check if the drone is free
	 * 
	 * @param drone
	 * @return
	 */
	private boolean droneIsFree(DroneAutopilot drone) {
		return drone.getFirstTargetAirport() == null && drone.getTargetCoordinatesSize() == 0
				&& drone.assignedPackage == null && !drone.hasPickedupPackage;
	}

	/**
	 * Notify the given drone that the complete time has passed.
	 * 
	 * @param 	drone
	 * 			The index of the drone.
	 * @return	The autopilot's output.
	 */
	public AutopilotOutputs completeTimeHasPassed(int drone) {
		return outputs.get(drone);
	}

	/**
	 * Deliver the package from the given airport and gate to the given airport and gate.
	 * 
	 * @param 	fromAirport
	 * 			The starting airport.
	 * @param 	fromGate
	 * 			The starting gate.
	 * @param 	toAirport
	 * 			The destination airport.
	 * @param 	toGate
	 * 			The destination gate.
	 */
	public static boolean USE_SCHEDULING = false;
	public void deliverPackage(int fromAirport, int fromGate, int toAirport, int toGate) {
		Package newPackage = new Package();
		newPackage.fromAirport = fromAirport;
		newPackage.fromGate = fromGate;
		newPackage.toAirport = toAirport;
		newPackage.toGate = toGate;
		newPackage.transporter = null;
		newPackage.reserver = null;
		synchronized(this) {packageQueue.add(newPackage);} // Add package to queue
	}

	/**
	 * The queue of packages not assigned to any drone.
	 */
	private ArrayList<Package> packageQueue = new ArrayList<Package>();
	
	/**
	 * End the simulation.
	 */
	public void simulationEnded() {
		for (DroneAutopilot drone : drones)
			drone.simulationEnded();
	}

	/**
	 * Registers the drones held by this module.
	 */
	private ArrayList<DroneAutopilot> drones = new ArrayList<DroneAutopilot>();
	private ArrayList<DroneAutopilot> activeDrones = new ArrayList<DroneAutopilot>();
	private ArrayList<AutopilotOutputs> outputs = new ArrayList<AutopilotOutputs>();
	
	public ArrayList<DroneAutopilot> getAllDrones() {
		return this.drones;
	}
}