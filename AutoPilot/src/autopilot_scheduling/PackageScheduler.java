package autopilot_scheduling;

import java.util.ArrayList;
import interfaces.Package;

public class PackageScheduler implements Runnable {

	/**
	 * Add the given package to the list of packages to be delivered.
	 * 
	 * @param 	Package
	 * 			The package to add.
	 */
	public void addPackage(Package p) {
		packages.add(p);
	}
	
	/**
	 * Registers the packages to be delivered.
	 */
	private ArrayList<Package> packages = new ArrayList<Package>();
	
	/**
	 * Reserve the given drone.
	 * 
	 * @param drone The index of the drone to reserve.
	 */
	public void addReservedDrone(int drone) {
		drones.add(drone);
	}
	
	/**
	 * Registers the indices of available/reserved drones.
	 */
	private ArrayList<Integer> drones = new ArrayList<Integer>();
	
	@Override
	public void run() {
		
		// Set up constraints
		// Launch (M)ILP
		// Set time limit
		
	}

	// Classic tests
	public static void main(String[] args) {
		
	}
	
}