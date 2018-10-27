package interfaces;

import java.util.ArrayList;

import autopilot_utilities.Point3D;

/**
 * A class of airports.
 * 
 * @author	Team Saffier
 * @version	1.0
 */
public class Airport {
	
	private boolean ENABLE_LOGGING = false;
	
	/**
	 * Registers the length/width of an airport.
	 */
	private static float airportLength, airportWidth, distToLane;
	private static final float offset = 35.0f;
	
	/**
	 * Set the airport parameters to the given ones.
	 */
	public static void setAirportParameters(float length, float width) {
		airportLength = length;
		airportWidth = width;
		distToLane = (airportLength/2) + (airportWidth/2);
	}	

	/**
	 * Initialize this new airport with given parameters.
	 * 
	 * @param 	centerX
	 * 			The x center for the new airport.
	 * @param 	centerZ
	 * 			The z center for the new airport.
	 * @param 	centerToRunway0X
	 * @param 	centerToRunway0Z
	 */
	public Airport(float centerX, float centerZ, float centerToRunway0X, float centerToRunway0Z) {
		
		this.centerX = centerX;
		this.centerZ = centerZ;
		// TODO
		this.centerToRunway0X = -centerToRunway0X;
		this.centerToRunway0Z = -centerToRunway0Z;
		
		calculateCheckPoints();
		
	}
	
	/**
	 * Registers landing points;
	 */
	public Point3D pointGate0, pointGate1, 
		startPointLane0, startPointLane1, 
		startSecondPointLane0, startSecondPointLane1,
		endPointLane0, endPointLane1, 
		prepareLandingPoint0, prepareLandingPoint1;
	
	// Registers the ID
	public int ID;
	
	/**
	 * Registers airport characteristics.
	 */
	public float centerX, centerZ, centerToRunway0X, centerToRunway0Z;
	
	public Point3D getCenter() {
		return new Point3D(centerX, 0, centerZ);
	}
	
	/**
	 * Get the landing points for the first gate.
	 */
	public ArrayList<Point3D> getLandingPointsGate0() {
		ArrayList<Point3D> array = new ArrayList<Point3D>();
		array.add(prepareLandingPoint0);
		array.add(endPointLane0);
		return array;
	}
	
	/**
	 * Get the landing points for the second gate.
	 */
	public ArrayList<Point3D> getLandingPointsGate1() {
		ArrayList<Point3D> array = new ArrayList<Point3D>();
		array.add(prepareLandingPoint1);
		array.add(endPointLane1);
		return array;
	}
	
	public boolean inGate(int gate, Point3D location) {
		return (gate == 0 ? inGate0(location) : inGate1(location));
	}
	
	public boolean inGate0(Point3D location) {
		float width2 = airportWidth/2;
		return (location.getX() >= this.pointGate0.getX() - width2 
				&& location.getX() <= this.pointGate0.getX() + width2 
				&& location.getZ() >= this.pointGate0.getZ() - width2 
				&& location.getZ() <= this.pointGate0.getZ() + width2);
	}
	
	public boolean inGate1(Point3D location) {
		float width2 = airportWidth/2;
		return (location.getX() >= this.pointGate1.getX() - width2 
				&& location.getX() <= this.pointGate1.getX() + width2 
				&& location.getZ() >= this.pointGate1.getZ() - width2 
				&& location.getZ() <= this.pointGate1.getZ() + width2);
	}
	
	/**
	 * Calculate the checkpoints for this airport.
	 */
	private void calculateCheckPoints() {
		
		pointGate1 = new Point3D(this.centerX - (airportWidth-offset) * this.centerToRunway0Z, -1.0f, this.centerZ + (airportWidth-offset) * this.centerToRunway0X);
		pointGate0 = new Point3D(this.centerX + (airportWidth-offset) * this.centerToRunway0Z, -1.0f, this.centerZ - (airportWidth-offset) * this.centerToRunway0X);

		startPointLane0 = new Point3D(pointGate1.getX() + this.centerToRunway0X * distToLane, -1.0f, pointGate1.getZ() + this.centerToRunway0Z * distToLane);
		startPointLane1 = new Point3D(pointGate0.getX() - this.centerToRunway0X * distToLane, -1.0f, pointGate0.getZ() - this.centerToRunway0Z * distToLane);
		
		startSecondPointLane0 = new Point3D(
				this.centerX - (airportWidth-150) * this.centerToRunway0Z + this.centerToRunway0X * distToLane, 
				-1.0f, 
				this.centerZ + (airportWidth-150) * this.centerToRunway0X + this.centerToRunway0Z * distToLane);
		startSecondPointLane1 = new Point3D(
				this.centerX + (airportWidth-150) * this.centerToRunway0Z - this.centerToRunway0X * distToLane, 
				-1.0f, 
				this.centerZ - (airportWidth-150) * this.centerToRunway0X - this.centerToRunway0Z * distToLane);
		
		endPointLane0 = new Point3D(
				pointGate0.getX() + this.centerToRunway0X * distToLane, 
				-1.0f, 
				pointGate0.getZ() + this.centerToRunway0Z * distToLane);
		endPointLane1 = new Point3D(
				pointGate1.getX() - this.centerToRunway0X * distToLane, 
				-1.0f, 
				pointGate1.getZ() - this.centerToRunway0Z * distToLane);
		
		prepareLandingPoint0 = new Point3D(
				this.centerX - 700 * this.centerToRunway0Z + this.centerToRunway0X * distToLane, 
				40f, 
				this.centerZ + 700 * this.centerToRunway0X + this.centerToRunway0Z * distToLane);
		prepareLandingPoint1 = new Point3D(
				this.centerX + 700 * this.centerToRunway0Z - this.centerToRunway0X * distToLane, 
				40f, 
				this.centerZ - 700 * this.centerToRunway0X - this.centerToRunway0Z * distToLane);
		
		if (ENABLE_LOGGING) {
			System.out.println("center: ["+this.centerX+" 0.0 "+this.centerZ+"]");
			
			System.out.println("centerToRunway0X: "+this.centerToRunway0X);
			System.out.println("centerToRunway0Z: "+this.centerToRunway0Z);
			
			System.out.println("P gate 0 : " + pointGate0);
			System.out.println("P gate 1 : " + pointGate1);
			
			System.out.println("SP gate 0 : " + startPointLane0);
			System.out.println("SP gate 1 : " + startPointLane1);
			
			System.out.println("SSP gate 0 : " + startSecondPointLane0);
			System.out.println("SSP gate 1 : " + startSecondPointLane1);
			
			System.out.println("EP gate 0 : " + endPointLane0);
			System.out.println("EP gate 1 : " + endPointLane1);
			
			System.out.println("PP gate 0 : " + prepareLandingPoint0);
			System.out.println("PP gate 1 : " + prepareLandingPoint1);
		}
	}
	
}