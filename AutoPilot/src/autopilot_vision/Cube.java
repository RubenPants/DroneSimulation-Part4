package autopilot_vision;

import java.util.ArrayList;

import autopilot_utilities.Point2D;
import autopilot_utilities.Point3D;
import autopilot_utilities.Rectangle2D;



/**
 * A class of cubes with at least a 2-dimensional center point and a bounding box,
 * 	both referring to the image in which the cube was detected.
 * 
 * The cube can also have a list of 2-dimensional lines, corresponding to its detected edges.
 * 
 * @author 	Team Saffier
 * @version 	1.0
 */
public class Cube {

	/**
	 * Initialize this new cube with given center point and bounding box.
	 * 
	 * @param	boundingBox
	 * 			The bounding box for this new cube.
	 * @param	hue
	 * 			The hue of the color of this new cube.
	 * @param	saturation
	 * 			The saturation of the color of this new cube.
	 * @post		The new cube's center point equals the given one.
	 * 			| new.getCenter() == center
	 * @post		The new cube's bounding box equals the given one.
	 * 			| new.getBoundingBox() == boundingBox
	 */
	public Cube(Rectangle2D boundingBox, float hue, float saturation) {
		this.boundingBox = boundingBox;
		this.hue = hue;
		this.saturation = saturation;
		this.distance = -1.0;
		this.location = null;
	}
	
	/**
	 * Return the center point of this cube.
	 */
	public Point2D getCenter() {
		return getBoundingBox().getCenter();
	}
	
	/**
	 * Return the bounding box of this cube.
	 */
	public Rectangle2D getBoundingBox() {
		return boundingBox;
	}
	
	/**
	 * Set the bounding box of this cube.
	 */
	public void setBoundingBox(Rectangle2D boundingBox) {
		this.boundingBox = boundingBox;
	}
	
	
	/**
	 * Variable registering the bounding box of the color of this cube.
	 */
	private Rectangle2D boundingBox;
	
	/**
	 * Returns the hue of the color of this cube.
	 */
	public float getHue() {
		return hue;
	}
	
	/**
	 * The hue of this cube.
	 */
	private float hue;
	
	/**
	 * Returns the saturation of the color of this cube.
	 */
	public float getSaturation() {
		return saturation;
	}
	
	/**
	 * The saturation of the color of this cube.
	 */
	private float saturation;
	
	/**
	 * Returns the location of this cube, or null if no location was recorded/approximated.
	 */
	public Point3D getLocation() {
		return location;
	}
	
	/**
	 * Set the location of this cube in 3-dimensional space.
	 * 
	 * @param 	location
	 * 			The new location for this cube.
	 */
	public void setLocation(Point3D location) {
		this.location = location;
	}
	
	/**
	 * Variable registering the location of this cube in 3-dimensional space.
	 * 	This can be an approximation.
	 */
	private Point3D location;
	
	/**
	 * Returns the current distance to this cube, or null if no distance was recorded/approximated.
	 */
	public double getDistance() {
		return distance;
	}
	
	/**
	 * Set the distance to this cube in 3-dimensional space.
	 * 
	 * @param 	distance
	 * 			The new distance for this cube.
	 */
	public void setDistance(double distance) {
		this.distance = distance;
	}
	
	/**
	 * Variable registering the current (approximate) distance to the cube.
	 * 	This can be an approximation.
	 */
	private double distance;
	
	/**
	 * Returns whether or not this cube is currently visible.
	 */
	public boolean isVisible() {
		return visible;
	}
	
	/**
	 * Set the current visibility of this cube.
	 */
	public void setVisible(boolean visible) {
		this.visible = visible;
	}
	
	/**
	 * Variable denoting whether or not this cube is currently visible.
	 */
	private boolean visible;
	
	/**
	 * Get the number of diagonals attributed to this cube.
	 */
	public int getNbDiagonals() {
		return diagonals.size();
	}
	
	/**
	 * Get the diagonal at the given index in this cube's list of attributed diagonals.
	 * 
	 * @param 	index
	 * 			The index of the diagonal that is wanted.
	 * @return	The diagonal at given index or null if the index is invalid.
	 */
	public CubeDiagonal getDiagonalAtIndex(int index) {
		if (index < 0 || index >= diagonals.size())
			return null;
		return diagonals.get(index);
	}
	
	/**
	 * Attribute the given diagonal to this cube.
	 * 
	 * @param	diagonal
	 * 			The diagonal that is to be added.
	 */
	public void addDiagonal(CubeDiagonal diagonal) {
		diagonals.add(diagonal);
	}
	
	/**
	 * Remove all diagonals from this cube's diagonals list.
	 */
	public void removeAllDiagonals() {
		diagonals.clear();
	}
	
	/**
	 * Print all the diagonals of this cube.
	 */
	public void printDiagonals() {
		for (CubeDiagonal diagonal : diagonals)
			System.out.println(diagonal);
	}
	
	/**
	 * Print all the vertical diagonals of this cube.
	 */
	public void printVerticalDiagonals() {
		for (CubeDiagonal diagonal : diagonals)
			if (diagonal.isFromVerticalSide())
				System.out.println(diagonal);
	}
	
	/**
	 * A list of diagonals attributed to this cube.
	 */
	private ArrayList<CubeDiagonal> diagonals = new ArrayList<CubeDiagonal>();
	
	/**
	 * Returns a textual representation of this cube.
	 * 
	 * @return	A string representing this cube.
	 * 			| "[" + getCenter() + " with hue = " + getHue() + " and saturation = " + getSaturation() + "]"
	 */
	public String toString() {
		return "[" + getBoundingBox() + " with hue = " + String.format("%.03f", getHue()) + " and saturation = " +String.format("%.03f", getSaturation()) + " (location = " + getLocation() + ")]";
	}
	
}