package autopilot_utilities;

import java.util.ArrayList;
import java.util.List;

import autopilot_vision.Cube;
import interfaces.Path;

/**
 * A class of 3-dimensional paths. Every path consists of a list of points in
 *  3D space. Each point can be associated with a cube.
 * 
 * @author 	Team Saffier
 * @version	1.0
 */
public class Path3D {

	/**
	 * Initialize this new 3-dimensional path with given input path.
	 * 
	 * @param 	path
	 * 			An input path having a list of coordinates.
	 * @throws	IllegalArgumentException
	 * 			The given path is null, empty or its 
	 */
	public Path3D(Path path) throws IllegalArgumentException {
		boolean ok = false;
		this.coordinates = new ArrayList<PointWrapper>();
		if (path != null) {
			float xCoords[] = path.getX();
			float yCoords[] = path.getY();
			float zCoords[] = path.getZ();
			if (xCoords != null
					&& yCoords != null
					&& zCoords != null
					&& xCoords.length == yCoords.length
					&& yCoords.length == zCoords.length
					&& xCoords.length > 0) {
				for (int i=0 ; i<xCoords.length ; i++) {
					Point3D newPoint = new Point3D(xCoords[i], yCoords[i], zCoords[i]);
					//Point3D newPoint = new Point3D((Math.round(xCoords[i]/10))*10, (Math.round(yCoords[i]/10))*10, (Math.round(zCoords[i]/10))*10);
					PointWrapper wrapper = new PointWrapper();
					wrapper.coordinate = newPoint;
					wrapper.cube = null;
					wrapper.visited = false;
					this.coordinates.add(wrapper);
				}
				ok = true;
			}
		}
		if (!ok)
			throw new IllegalArgumentException("Invalid input path.");
		calculateProperties();
	}

	/**
	 * Initialize this new 3-dimensional path with given input coordinates.
	 * 
	 * @param 	coordinates
	 * 			The coordinates part of this path.
	 */
	public Path3D(ArrayList<Point3D> coordinates) {
		this.setCoordinates(coordinates);
	}

	/**
	 * Get the coordinates of the locations in this path.
	 * 
	 * @return	A list of points in 3D space, the coordinates of
	 * 			the locations in this path.
	 */
	public List<Point3D> getCoordinates() {
		ArrayList<Point3D> coords = new ArrayList<Point3D>();
		for (int i=0 ; i<this.getNbCoordinates() ; i++)
			coords.add(this.getCoordinate(i));
		return coords;
	}

	/**
	 * Returns the number of locations in this path.
	 */
	public int getNbCoordinates() {
		return coordinates.size();
	}

	/**
	 * Returns the original location at the given index.
	 * 
	 * @return	The original location at the given index.
	 */
	public Point3D getCoordinate(int i) {
		return coordinates.get(i).coordinate;
	}

	/**
	 * Move best five points first.
	 * 
	 * @param 	firstIndex
	 * 			The index of the point that has to be on the first position.
	 * @param 	secondIndex
	 * 			The index of the point that has to be on the second position.
	 * @param 	thridIndex
	 * 			The index of the point that has to be on the third position.
	 * @param 	fourthIndex
	 * 			The index of the point that has to be on the fourth position.
	 * @param 	fifthIndex
	 * 			The index of the point that has to be on the fifth position.
	 */
	public void moveBestFive(int firstIndex, int secondIndex, int thirdIndex, int fourthIndex, int fifthIndex) {
		PointWrapper firstPoint = null;
		PointWrapper secondPoint = null;
		PointWrapper thirdPoint = null;
		PointWrapper fourthPoint = null;
		PointWrapper fifthPoint = null;
		
		// Collect points
		firstPoint = this.coordinates.get(firstIndex);
		if (secondIndex >= 0) {
			secondPoint = this.coordinates.get(secondIndex);
			if (thirdIndex >= 0) {
				thirdPoint = this.coordinates.get(thirdIndex);
				if (fourthIndex >= 0) {
					fourthPoint = this.coordinates.get(fourthIndex);
					if (fifthIndex >= 0) {
						fifthPoint = this.coordinates.get(fifthIndex);
					}
				}
			}
		}

		// Remove points
		this.coordinates.remove(firstPoint);
		if (secondIndex >= 0) {
			this.coordinates.remove(secondPoint);
			if (thirdIndex >= 0) {
				this.coordinates.remove(thirdPoint);
				if (fourthIndex >= 0) {
					this.coordinates.remove(fourthPoint);
					if (fifthIndex >= 0) {
						this.coordinates.remove(fifthPoint);
					}
				}
			}
		}
		
		// Add points in order
		if (fifthIndex >= 0)
			this.coordinates.add(0, fifthPoint);
		if (fourthIndex >= 0)
			this.coordinates.add(0, fourthPoint);
		if (thirdIndex >= 0)
			this.coordinates.add(0, thirdPoint);
		if (secondIndex >= 0)
			this.coordinates.add(0, secondPoint);
		this.coordinates.add(0, firstPoint);
	}

	/**
	 * Move the coordinate at the given index to the start of the path.
	 * 
	 * @param 	index
	 * 			The index of the point that is to be moved to the front of the path.
	 */
	public void moveToStart(int index) {
		PointWrapper point = this.coordinates.remove(index);
		this.coordinates.add(0, point);
	}
	
	/**
	 * Move the coordinate at the given index to the second position of the path.
	 * 
	 * @param 	index
	 * 			The index of the point that is to be moved to the second position of the path.
	 */
	public void moveToSecond(int index) {
		if (this.coordinates.size() < 1)
			return;
		PointWrapper point = this.coordinates.remove(index);
		this.coordinates.add(1, point);
	}
	
	/**
	 * Move the coordinate at the given index to the third position of the path.
	 * 
	 * @param 	index
	 * 			The index of the point that is to be moved to the third position of the path.
	 */
	public void moveToThird(int index) {
		if (this.coordinates.size() < 2)
			return;
		PointWrapper point = this.coordinates.remove(index);
		this.coordinates.add(2, point);
	}

	/**
	 * Set the coordinates of this path to the given ones.
	 * 
	 * @param	coordinates
	 * 			The new coordinates for this path.
	 */
	public void setCoordinates(ArrayList<Point3D> coordinates) {
		this.coordinates = new ArrayList<PointWrapper>();
		for (int i=0 ; i<coordinates.size() ; i++) {
			PointWrapper wrapper = new PointWrapper();
			wrapper.coordinate = coordinates.get(i);
			wrapper.cube = null;
			wrapper.visited = false;
			this.coordinates.add(wrapper);
		}
	}

	/**
	 * Update the coordinate at given index.
	 * 
	 * @param 	index
	 * 			The index of the coordinate that is to be updated.
	 * @param 	newValue
	 * 			The new value for the coordinate.
	 */
	public void updateCoordinate(int index, Point3D newValue) {
		try {
			coordinates.get(index).coordinate = newValue;
			//calculateProperties();
		}
		catch (IndexOutOfBoundsException exception) {
			System.out.println("Invalid coordinate index.");
		}
	}

	/**
	 * Remove the coordinate at the given index.
	 * 
	 * @param 	index
	 * 			The index of the coordinate that is to be removed.
	 */
	public void removeCoordinate(int index) {
		try {
			coordinates.remove(index);
			calculateProperties();
		}
		catch (IndexOutOfBoundsException exception) {
			System.out.println("Invalid coordinate index.");
		}
	}

	/**
	 * Returns whether or not this path is empty.
	 * 
	 * @return True if and only if this path does not contain any coordinates.
	 */
	public boolean isEmpty() {
		return coordinates.size() == 0;
	}

	/**
	 * The list of coordinates of the locations in this path.
	 */
	private List<PointWrapper> coordinates;

	public void setCube(int index, Cube cube) {
		this.coordinates.get(index).cube = cube;
	}
	
	/**
	 * Check whether or not the location at given index has been visited.
	 * 
	 * @param 	index
	 * 			The index of the location.
	 * @return	True if and only if the location at given index has been visited.
	 */
	//public boolean isVisited(int index) {
	//	return (index < visited.length && index >=0 ? visited[index] : false);
	//}

	/**
	 * Set whether or not the location at given index has been visited.
	 * 
	 * @param 	index
	 * 			The index of the location.
	 * @param 	visited
	 * 			True if the location at given index has been visited.
	 */
	public void setVisited(int index, boolean visited) {
		removeCoordinate(index);
		// this.coordinates.get(index).visited = visited;
		//if (index < this.visited.length && index >=0)
		//this.visited[index] = visited;
	}

	/**
	 * Calculate the properties of this path (eg. minimum distance).
	 */
	private void calculateProperties() {

		// Pre-processing
		List<Point3D> coordinates = getCoordinates();
		minimumDistance = Double.POSITIVE_INFINITY;
		averageDistance = -1.0;
		int nbOfCoordinates = coordinates.size();

		// Calculate properties
		for (int i=0 ; i<nbOfCoordinates ; i++)
			for (int j=i+1 ; j<nbOfCoordinates ; j++) {
				double distance = coordinates.get(i).distanceTo(coordinates.get(j));
				if (i==0 && j==0)
					averageDistance = distance;
				else
					averageDistance += distance;
				if (distance < minimumDistance)
					minimumDistance = distance;
			}
		averageDistance /= nbOfCoordinates;

	}

	/**
	 * The minimum distance between the points in this path.
	 */
	private double minimumDistance = Double.POSITIVE_INFINITY;

	/**
	 * The average distance between the points in this path.
	 */
	@SuppressWarnings("unused")
	private double averageDistance = Double.POSITIVE_INFINITY;

	/**
	 * Returns a textual representation of this path.
	 */
	public String toString() {
		String output = "[";
		for (int i=0 ; i<this.getNbCoordinates() ; i++) {
			Point3D coordinate = this.getCoordinate(i);
			output += coordinate.toString();
		}
		return output + "]";
	}
	
	/**
	 * Convenience class.
	 * 
	 * @author 	Team Saffier
	 * @version	1.0
	 */
	@SuppressWarnings("unused")
	private class PointWrapper {
		public Point3D coordinate;
		public Cube cube;
		public boolean visited;
	}

}