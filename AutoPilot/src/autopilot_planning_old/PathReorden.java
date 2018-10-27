package autopilot_planning_old;

import autopilot_utilities.Path3D;
import autopilot_utilities.Point3D;
import autopilot_utilities.Vector3f;

public class PathReorden {
	
	public void reordenPath(Path3D path, Point3D positionDrone, float heading) {
		Point3D minDistPoint = null;
		float minDist = Float.POSITIVE_INFINITY;
		float secondDist = Float.POSITIVE_INFINITY;
		float thirdDist = Float.POSITIVE_INFINITY;
		int bestInt = 0;
		Point3D coordinate;
		float currDist;
		Vector3f requestedVector;
		float reqHeading;
		float reqPitch;
		
		if (path.getCoordinates().size() > 1) {
			for (int i = 0; i < path.getCoordinates().size(); i++) {
				coordinate = path.getCoordinates().get(i);
				
				requestedVector = new Vector3f(coordinate.getX() - positionDrone.getX(), 
						coordinate.getY() - positionDrone.getY(), 
						coordinate.getZ() - positionDrone.getZ());
				reqPitch = (float)Math.atan(requestedVector.y/(Math.sqrt(requestedVector.z*requestedVector.z + requestedVector.x*requestedVector.x)));
				reqHeading = (float)Math.atan2(-requestedVector.x, -requestedVector.z);

				float deltaHeading = (reqHeading - heading);
				if (deltaHeading > Math.PI)	deltaHeading -= 2 * Math.PI;
				else if (deltaHeading < -Math.PI) deltaHeading += 2 * Math.PI;
				currDist = heuristicDistanceTo(coordinate, positionDrone, deltaHeading, reqPitch);
				
				if (currDist < minDist && reqPitch < Math.PI/15) {
					if(!(distanceTo(coordinate, positionDrone) < 200 && Math.abs(deltaHeading) > Math.PI/8)) {
						if (!(distanceTo(coordinate, positionDrone) < 400 && Math.abs(deltaHeading) > Math.PI/4)) {
							if (!(distanceTo(coordinate, positionDrone) < 800 && Math.abs(deltaHeading) > Math.PI/2)) {
								minDist = currDist;
								minDistPoint = coordinate;
								bestInt = i;
							}
						}
					}
				}
			}
			if (bestInt != 0)
				path.moveToStart(bestInt);
			
			// Move second for Dubins
			if (path.getCoordinates().size() > 2) {
				minDistPoint = path.getCoordinates().get(0);
				for (int i = 1; i < path.getCoordinates().size(); i++) {
					coordinate = path.getCoordinates().get(i);
					currDist = distanceTo(coordinate, minDistPoint);
					if (currDist < secondDist) {
						requestedVector = new Vector3f(coordinate.getX() - minDistPoint.getX(), 
								coordinate.getY() - minDistPoint.getY(), 
								coordinate.getZ() - minDistPoint.getZ());
						reqPitch = (float)Math.atan(requestedVector.y/(Math.sqrt(requestedVector.z*requestedVector.z + requestedVector.x*requestedVector.x)));

						// Check if possible to reach
						if (reqPitch < Math.PI/15) {
							secondDist = currDist;
							bestInt = i;
						}
					}
				}
			}
			if (bestInt != 0 && bestInt != 1)
				path.moveToSecond(bestInt);
			
			// Move third for DubinsPart4
			if (path.getCoordinates().size() > 3) {
				minDistPoint = path.getCoordinates().get(1);
				for (int i = 2; i < path.getCoordinates().size(); i++) {
					coordinate = path.getCoordinates().get(i);
					currDist = distanceTo(coordinate, minDistPoint);
					if (currDist < thirdDist) {
						requestedVector = new Vector3f(coordinate.getX() - minDistPoint.getX(), 
								coordinate.getY() - minDistPoint.getY(), 
								coordinate.getZ() - minDistPoint.getZ());
						reqPitch = (float)Math.atan(requestedVector.y/(Math.sqrt(requestedVector.z*requestedVector.z + requestedVector.x*requestedVector.x)));

						// Check if possible to reach
						if (reqPitch < Math.PI/15) {
							thirdDist = currDist;
							bestInt = i;
						}
					}
				}
			}
			if (bestInt != 0 && bestInt != 1 && bestInt != 2)
				path.moveToThird(bestInt);
		}
	}
	
	public void reordenPath(Path3D path, Vector3f positionDrone, float heading) {
		Point3D minDistPoint = null;
		float minDist = Float.POSITIVE_INFINITY;
		float secondDist = Float.POSITIVE_INFINITY;
		float thirdDist = Float.POSITIVE_INFINITY;
		int bestInt = 0;
		Point3D coordinate;
		float currDist;
		Vector3f requestedVector;
		float reqHeading;
		float reqPitch;
		
		if (path.getCoordinates().size() > 1) {
			for (int i = 0; i < path.getCoordinates().size(); i++) {
				coordinate = path.getCoordinates().get(i);
				
				requestedVector = new Vector3f(coordinate.getX() - positionDrone.x, 
						coordinate.getY() - positionDrone.y, 
						coordinate.getZ() - positionDrone.z);
				reqPitch = (float)Math.atan(requestedVector.y/(Math.sqrt(requestedVector.z*requestedVector.z + requestedVector.x*requestedVector.x)));
				reqHeading = (float)Math.atan2(-requestedVector.x, -requestedVector.z);

				float deltaHeading = (reqHeading - heading);
				if (deltaHeading > Math.PI)	deltaHeading -= 2 * Math.PI;
				else if (deltaHeading < -Math.PI) deltaHeading += 2 * Math.PI;
				currDist = heuristicDistanceTo(coordinate, positionDrone, deltaHeading, reqPitch);
				
				if (currDist < minDist && reqPitch < Math.PI/15) {
					if(!(distanceTo(coordinate, positionDrone) < 400 && Math.abs(deltaHeading) > Math.PI/8)) {
						if (!(distanceTo(coordinate, positionDrone) < 800 && Math.abs(deltaHeading) > Math.PI/4)) {
							if (!(distanceTo(coordinate, positionDrone) < 1000 && Math.abs(deltaHeading) > Math.PI/2)) {
								minDist = currDist;
								minDistPoint = coordinate;
								bestInt = i;
							}
						}
					}
				}
			}
			if (bestInt != 0)
				path.moveToStart(bestInt);
			
			// Move second for Dubins
			if (path.getCoordinates().size() > 2) {
				minDistPoint = path.getCoordinates().get(0);
				for (int i = 1; i < path.getCoordinates().size(); i++) {
					coordinate = path.getCoordinates().get(i);
					currDist = distanceTo(coordinate, minDistPoint);
					if (currDist < secondDist) {
						requestedVector = new Vector3f(coordinate.getX() - minDistPoint.getX(), 
								coordinate.getY() - minDistPoint.getY(), 
								coordinate.getZ() - minDistPoint.getZ());
						reqPitch = (float)Math.atan(requestedVector.y/(Math.sqrt(requestedVector.z*requestedVector.z + requestedVector.x*requestedVector.x)));

						// Check if possible to reach
						if (reqPitch < Math.PI/15) {
							secondDist = currDist;
							bestInt = i;
						}
					}
				}
			}
			if (bestInt != 0 && bestInt != 1)
				path.moveToSecond(bestInt);
			
			// Move third for DubinsPart4
			if (path.getCoordinates().size() > 3) {
				minDistPoint = path.getCoordinates().get(1);
				for (int i = 2; i < path.getCoordinates().size(); i++) {
					coordinate = path.getCoordinates().get(i);
					currDist = distanceTo(coordinate, minDistPoint);
					if (currDist < thirdDist) {
						requestedVector = new Vector3f(coordinate.getX() - minDistPoint.getX(), 
								coordinate.getY() - minDistPoint.getY(), 
								coordinate.getZ() - minDistPoint.getZ());
						reqPitch = (float)Math.atan(requestedVector.y/(Math.sqrt(requestedVector.z*requestedVector.z + requestedVector.x*requestedVector.x)));

						// Check if possible to reach
						if (reqPitch < Math.PI/15) {
							thirdDist = currDist;
							bestInt = i;
						}
					}
				}
			}
			if (bestInt != 0 && bestInt != 1 && bestInt != 2)
				path.moveToThird(bestInt);
		}
	}

	private float distanceTo(Point3D point, Point3D position) {
		return (float) Math.sqrt(Math.pow(point.getX() - position.getX(), 2) + Math.pow(point.getZ() - position.getZ(), 2));
	}

	private float distanceTo(Point3D point, Vector3f position) {
		return (float) Math.sqrt(Math.pow(point.getX() - position.x, 2) + Math.pow(point.getZ() - position.z, 2));
	}
	
	private float heuristicDistanceTo(Point3D point, Point3D position, float deltaHeading, float deltaPitch) {
		return (float)(distanceTo(point, position) * (1 + 0.25*Math.abs(deltaHeading) + 0.1*Math.abs(deltaPitch)));
	}
	
	private float heuristicDistanceTo(Point3D point, Vector3f position, float deltaHeading, float deltaPitch) {
		return (float)(distanceTo(point, position) * (1 + 0.5*Math.abs(deltaHeading) + 0.25*Math.abs(deltaPitch)));
	}

}
