package autopilot_planning;

import java.util.ArrayList;

import autopilot_utilities.Point3D;
import autopilot_utilities.Vector3f;
import interfaces.AutopilotInputs;
import interfaces.DroneAutopilot;

public interface Distance {

	/**
	 * Get the distance in the horizontal (XZ) plane
	 * 
	 * @param point
	 * @param secondPoint
	 * 
	 * @return   The distance between two points expressed in meters
	 */
	static float distanceToHor(Point3D point, Point3D secondPoint) {
		return (float) Math.sqrt(
				Math.pow(point.getX() - secondPoint.getX(), 2) + 
				Math.pow(point.getZ() - secondPoint.getZ(), 2));
	}

	static float distanceToHor(Point3D point, AutopilotInputs inputs) {
		return distanceToHor(point, new Point3D(inputs.getX(), 0, inputs.getZ()));
	}

	static float distanceToHor(Point3D point, Vector3f secondPoint) {
		return distanceToHor(point, new Point3D(secondPoint.x, 0, secondPoint.z));
	}
	
	/**
	 * Get the real geometric distance between two points
	 * 
	 * @param point
	 * @param secondPoint
	 * 
	 * @return   The distance between two points in the 3D-world expressed in meters
	 */
	static float distanceTo3D(Point3D point, Point3D secondPoint) {
		return (float) Math.sqrt(
				Math.pow(point.getX() - secondPoint.getX(), 2) + 
				Math.pow(point.getY() - secondPoint.getY(), 2) + 
				Math.pow(point.getZ() - secondPoint.getZ(), 2));
	}
	
	static float distanceTo3D(Point3D point, AutopilotInputs inputs) {
		return distanceTo3D(point, new Point3D(inputs.getX(), inputs.getY(), inputs.getZ()));
	}
	
	static Point3D getClosestDronePosition(DroneAutopilot drone, ArrayList<DroneAutopilot> drones) {
		float closestDistance = Float.POSITIVE_INFINITY;
		int closestInt = -1;
		float distance;
		
		for (int i = 0; i < drones.size(); i++) {
			if (drones.get(i) != drone) {
				distance = distanceToHor(drone.location, drones.get(i).location);
				if (distance < closestDistance) {
					closestDistance = distance;
					closestInt = i;
				}
			}
		}
		
		if (closestInt == -1) return null;
		return drones.get(closestInt).location;
	}
	
	static float distanceToClosestDrone(DroneAutopilot drone, ArrayList<DroneAutopilot> drones) {
		Point3D closestDroneLoc = getClosestDronePosition(drone, drones);
		
		if (closestDroneLoc == null) return Float.POSITIVE_INFINITY;
		return distanceToHor(closestDroneLoc, drone.location);
	}
	
	static Point3D getClosestDronePositionBeneathOwn(DroneAutopilot drone, ArrayList<DroneAutopilot> drones) {
		float closestDistance = Float.POSITIVE_INFINITY;
		int closestInt = -1;
		float distance;
		
		for (int i = 0; i < drones.size(); i++) {
			if (drones.get(i) != drone && drones.get(i).location.getY() < drone.location.getY() + 5) {	// '5' is a threshold
				distance = distanceToHor(drone.location, drones.get(i).location);
				if (distance < closestDistance) {
					closestDistance = distance;
					closestInt = i;
				}
			}
		}
		
		if (closestInt == -1) return null;
		return drones.get(closestInt).location;
	}
	
	static float distanceToClosestDroneBeneathOwn(DroneAutopilot drone, ArrayList<DroneAutopilot> drones) {
		Point3D closestDroneLoc = getClosestDronePositionBeneathOwn(drone, drones);
		
		if (closestDroneLoc == null) return Float.POSITIVE_INFINITY;
		return distanceToHor(closestDroneLoc, drone.location);
	}
}
