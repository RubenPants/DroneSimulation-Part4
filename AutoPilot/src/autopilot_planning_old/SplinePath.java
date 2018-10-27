package autopilot_planning_old;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import autopilot_utilities.*;
import interfaces.AutopilotInputs;

public class SplinePath {
	
	/*
	 * How to call
	 * input: geef pad die je wilt volgen.
	 * 1: CalculateSpline() {geeft spline terug}
	 * 2: calculateRoute()	{van die spline, zegt die welk pitch en heading er genomen moeten worden}
	 * output pitch en heading.
	 */

	public SplinePath(Path3D path, Point3D position, Vector3f direction) {
		setPath(path);
		setPosition(position);
		setDirection(direction);
	}
	
	private Path3D path;
	private Point3D position;
	private Vector3f direction;
	
	private void setPath(Path3D path) {
		this.path = path;
	}
	
	/**
	 * sets position of drone
	 * @param position
	 */
	private void setPosition(Point3D position) {
		this.position = position;
	}
	
	/**
	 * sets direction of drone. Pitch, heading, roll.
	 * @param direction
	 */
	private void setDirection(Vector3f direction) {
		this.direction = direction;
	}
	
	public Path3D getPath() {
		return this.path;
	}
	
	public Point3D getPosition() {
		return this.position;
	}
	
	public Vector3f getDirection() {
		return this.direction;
	}
	
	private double tangent(double a1, double a2, double b1, double b2) {
		return (a1-a2)/(b1-b2);
	}
	
	private double a(double k1, double z1, double z2, double x1, double x2) {
		return k1*(z2-z1) - (x2-x1);
	}
	
	private double b(double k2, double z1, double z2, double x1, double x2) {
		return -k2*(z2-z1) + (x2-x1);
	}
	
	private ArrayList<Double> t(double z1, double z2){
		double denominator = z2-z1;
		ArrayList<Double> t = new ArrayList<Double>();
		t.add(-z1/denominator);
		t.add(1/denominator);
		return t;
	}
	
	/**
	 * ArrayList gaat van graad 0 naar graad 3
	 * @param cube0
	 * @param direction0
	 * @param cube1
	 * @param cube2
	 * @return
	 */
	private ArrayList<Double> calculateSplineZ(Point3D cube1, Point3D cube2){
		double z1 = getPosition().getZ();
		double z2 = cube1.getZ();
		double x1 = getPosition().getX();
		double x2 = cube1.getX();
		double k1 = tangent(getDirection().x, getPosition().getX(), getDirection().z, getPosition().getZ());
		double k2 = tangent(cube1.getX(), cube2.getX(), cube1.getZ(), cube2.getZ());
		double a = a(k1, z1, z2, x1, x2);
		double b = b(k2, z1, z2, x1, x2);
		ArrayList<Double> t = t(z1, z2);
		ArrayList<Double> t2 = new ArrayList<Double>();
		t2.add(1-t.get(0));
		t2.add(-t.get(1));
		ArrayList<Double> part1 = new ArrayList<Double>();
		part1.add(t2.get(0)*x1);
		part1.add(t2.get(1)*x1);
		ArrayList<Double> part2 = new ArrayList<Double>();
		part2.add(t.get(0)*x2);
		part2.add(t.get(1)*x2);
		ArrayList<Double> part3 = new ArrayList<Double>();
		part3.add(t2.get(0)*a);
		part3.add(t2.get(1)*a);
		ArrayList<Double> part4 = new ArrayList<Double>();
		part4.add(t.get(0)*b);
		part4.add(t.get(1)*b);
		ArrayList<Double> part34 = new ArrayList<Double>();
		part34.add(part3.get(0) + part4.get(0));
		part34.add(part3.get(1) + part4.get(1));
		ArrayList<Double> part5 = new ArrayList<Double>();
		part5.add(t.get(0)*t2.get(0));
		part5.add(t.get(0)*t2.get(1) + t.get(1)*t2.get(0));
		part5.add(t.get(1)*t2.get(1));
		ArrayList<Double> part345 = new ArrayList<Double>();
		part345.add(part5.get(0)*part34.get(0));
		part345.add(part5.get(0)*part34.get(1) + part5.get(1)*part34.get(0));
		part345.add(part5.get(1)*part34.get(1) + part5.get(2)*part34.get(0));
		part345.add(part5.get(2)*part34.get(1));
		ArrayList<Double> spline = new ArrayList<Double>();
		spline.add(part1.get(0) + part2.get(0) + part345.get(0));
		spline.add(part1.get(1) + part2.get(1) + part345.get(1));
		spline.add(part345.get(2));
		spline.add(part345.get(3));
		return spline;
	}
	
	private ArrayList<Double> calculateSplineX(Point3D cube1, Point3D cube2){
		double x1 = getPosition().getX();
		double x2 = cube1.getX();
		double z1 = getPosition().getZ();
		double z2 = cube1.getZ();
		double k1 = tangent(getDirection().z, getPosition().getZ(), getDirection().x, getPosition().getX());
		double k2 = tangent(cube1.getZ(), cube2.getZ(), cube1.getX(), cube2.getX());
		double a = a(k1, x1, x2, z1, z2);
		double b = b(k2, x1, x2, z1, z2);
		ArrayList<Double> t = t(x1, x2);
		ArrayList<Double> t2 = new ArrayList<Double>();
		t2.add(1-t.get(0));
		t2.add(-t.get(1));
		ArrayList<Double> part1 = new ArrayList<Double>();
		part1.add(t2.get(0)*z1);
		part1.add(t2.get(1)*z1);
		ArrayList<Double> part2 = new ArrayList<Double>();
		part2.add(t.get(0)*z2);
		part2.add(t.get(1)*z2);
		ArrayList<Double> part3 = new ArrayList<Double>();
		part3.add(t2.get(0)*a);
		part3.add(t2.get(1)*a);
		ArrayList<Double> part4 = new ArrayList<Double>();
		part4.add(t.get(0)*b);
		part4.add(t.get(1)*b);
		ArrayList<Double> part34 = new ArrayList<Double>();
		part34.add(part3.get(0) + part4.get(0));
		part34.add(part3.get(1) + part4.get(1));
		ArrayList<Double> part5 = new ArrayList<Double>();
		part5.add(t.get(0)*t2.get(0));
		part5.add(t.get(0)*t2.get(1) + t.get(1)*t2.get(0));
		part5.add(t.get(1)*t2.get(1));
		ArrayList<Double> part345 = new ArrayList<Double>();
		part345.add(part5.get(0)*part34.get(0));
		part345.add(part5.get(0)*part34.get(1) + part5.get(1)*part34.get(0));
		part345.add(part5.get(1)*part34.get(1) + part5.get(2)*part34.get(0));
		part345.add(part5.get(2)*part34.get(1));
		ArrayList<Double> spline = new ArrayList<Double>();
		spline.add(part1.get(0) + part2.get(0) + part345.get(0));
		spline.add(part1.get(1) + part2.get(1) + part345.get(1));
		spline.add(part345.get(2));
		spline.add(part345.get(3));
		return spline;
	}
	
	/**
	 * x == true
	 * z == false
	 */
	private boolean xorz = false;
	
	/**
	 * x == true
	 * z == false
	 */
	public boolean getXorz() {
		return xorz;
	}
	
	/**
	 * x == true
	 * z == false
	 */
	private void setXorz(boolean xorz) {
		this.xorz = xorz;
	}
	
	/**
	 * forward = true
	 * backward = false
	 */
	private boolean forb = true;
	
	/**
	 * forward = true
	 * backward = false
	 */
	public boolean getForb() {
		return forb;
	}
	
	/**
	 * forward = true
	 * backward = false
	 */
	private void setForb(boolean forb) {
		this.forb = forb;
	}
	
	public ArrayList<Double> calculateSpline(Point3D cube1, Point3D cube2){
		Vector3f direction;
		if (getPosition().getZ() == getDirection().z && getPosition().getX() == getDirection().x) {
			direction = new Vector3f(2*getDirection().x, 2*getDirection().y, 2*getDirection().z);
		}
		else {
			direction = getDirection();
		}
		
		if (getPosition().getZ() == direction.z) {
			setForb(true);
			setXorz(true);
			return calculateSplineX(cube1, cube2);
		}
		else if (getPosition().getX() == direction.x) {
			setForb(true);
			setXorz(false);
			return calculateSplineZ(cube1, cube2);
		}
		else if (cube1.getZ() == cube2.getZ()) {
			setForb(true);
			setXorz(true);
			return calculateSplineX(cube1, cube2);
		}
		else if (cube1.getX() == cube2.getX()) {
			setForb(true);
			setXorz(false);
			return calculateSplineZ(cube1, cube2);
		}
		else if (getPosition().getZ() < cube1.getZ() && cube1.getZ() < cube2.getZ() && direction.z >= 0) {
			setForb(true);
			setXorz(false);
			return calculateSplineZ(cube1, cube2);
		}
		else if (getPosition().getX() < cube1.getX() && cube1.getX() < cube2.getX() && direction.x >= 0) {
			setForb(true);
			setXorz(true);
			return calculateSplineX(cube1, cube2);
		}
		else if (getPosition().getZ() > cube1.getZ() && cube1.getZ() > cube2.getZ() && direction.z <= 0) {
			setForb(true);
			setXorz(false);
			return calculateSplineZ(cube1, cube2);
		}
		else if (getPosition().getX() > cube1.getX() && cube1.getX() > cube2.getX() && direction.x <= 0) {
			setForb(true);
			setXorz(true);
			return calculateSplineX(cube1, cube2);
		}
		else if (getPosition().getZ() < cube2.getZ() && cube2.getZ() < cube1.getZ() && direction.z >= 0) {
			setForb(true);
			setXorz(false);
			return calculateSplineZ(cube1, cube2);
		}
		else if (getPosition().getX() < cube2.getX() && cube2.getX() < cube1.getX() && direction.x >= 0) {
			setForb(true);
			setXorz(true);
			return calculateSplineX(cube1, cube2);
		}
		else if (getPosition().getZ() > cube2.getZ() && cube2.getZ() > cube1.getZ() && direction.z <= 0) {
			setForb(true);
			setXorz(false);
			return calculateSplineZ(cube1, cube2);
		}
		else if (getPosition().getX() > cube2.getX() && cube2.getX() > cube1.getX() && direction.x <= 0) {
			setForb(true);
			setXorz(true);
			return calculateSplineX(cube1, cube2);
		}
		else if ((getPosition().getZ() < cube1.getZ() && direction.z <= 0) || (getPosition().getZ() > cube1.getZ() && direction.z >= 0)) {
			setForb(false);
			return new ArrayList<>(Arrays.asList(5.0,5.0,5.0,5.0));
		}
		else if ((getPosition().getX() < cube1.getX() && direction.x <= 0) || (getPosition().getX() > cube1.getX() && direction.x >= 0)) {
			setForb(false);
			return new ArrayList<>(Arrays.asList(5.0,5.0,5.0,5.0));
		}
		else if ((getPosition().getZ() < cube1.getZ() && cube2.getZ() < getPosition().getZ() && direction.z >= 0) ||
				(getPosition().getZ() > cube1.getZ() && cube2.getZ() > getPosition().getZ() && direction.z <= 0)) {
			setForb(true);
			setXorz(false);
			return lineairZ(getPosition(), cube1);
		}
		else if ((getPosition().getX() < cube1.getX() && cube2.getX() < getPosition().getX() && direction.x >= 0) ||
				(getPosition().getX() > cube1.getX() && cube2.getX() > getPosition().getX() && direction.x <= 0)) {
			setForb(true);
			setXorz(true);
			return lineairX(getPosition(), cube1);
		}
		else /*((getPosition().getX() < cube1.getX() && cube1.getX() < cube2.getX()) ||
				(getPosition().getX() > cube1.getX() && cube1.getX() > cube2.getX())) */{
			setForb(true);
			setXorz(false);
			return calculateSplineZ(cube1, cube2);
		}
		//else {
		//	return null;
		//}
	}
	
	/**
	 * van graad 0 naar graad 1
	 * @param cube1
	 * @param cube2
	 * @return
	 */
	public ArrayList<Double> lineairZ(Point3D cube1, Point3D cube2){
		double factor = tangent(cube2.getX(), cube1.getX(), cube2.getZ(), cube1.getZ());
		double degree0 = cube1.getX() - factor*cube1.getZ();
		ArrayList<Double> lineair = new ArrayList<Double>();
		lineair.add(degree0);
		lineair.add(factor);
		return lineair;
	}
	
	public ArrayList<Double> lineairX(Point3D cube1, Point3D cube2){
		double factor = tangent(cube2.getZ(), cube1.getZ(), cube2.getX(), cube1.getX());
		double degree0 = cube1.getZ() - factor*cube1.getX();
		ArrayList<Double> lineair = new ArrayList<Double>();
		lineair.add(degree0);
		lineair.add(factor);
		return lineair;
	}
	
	/**
	 * From given function, determines which angle must be taken.
	 * 
	 * @param	function
	 * 			input function
	 * @param 	relative
	 * 			Dummy variable to determine is function relative to x or z axis?
	 */
	public float[] calculateRoute(ArrayList<Double> function, AutopilotInputs inputs, Point3D nextCube){
		//ArrayList<Double> derivative = derivative(function);
		
		Point3D dronePos = this.getPosition();
		System.out.println(function);
		
		boolean answer = false;
		if(xorz == false){ // als z-as is
			// rico = a + bz + cz^2 (al afgeleid)
			
			double zHelpPos = dronePos.getZ() + 300;
			
			double xHelpPos = function.get(0) + function.get(1) * (zHelpPos) + function.get(2) * Math.pow(zHelpPos, 2) + function.get(3) * Math.pow(zHelpPos, 3);
			
			Point3D PointOnSpline = new Point3D( xHelpPos, nextCube.getY(), zHelpPos);

			
			float[] motion = new float[2];
			double droneX = dronePos.getX(), droneY = dronePos.getY(), droneZ = dronePos.getZ();

			Point3D currentPosition = new Point3D(inputs.getX(), inputs.getY(), inputs.getZ());
			Matrix4f toWorld = Utilities.getDroneToWorldTransformationMatrix(
					inputs.getHeading(), 
					inputs.getPitch(), 
					inputs.getRoll());
			
			// Calculate pitch/heading
			Vector3f requestedVector = new Vector3f(PointOnSpline.getX() - droneX, PointOnSpline.getY() - droneY, PointOnSpline.getZ() - droneZ);
			Vector3f forwardVector = Utilities.transformVector(toWorld, requestedVector);
			Vector3f headingVector = new Vector3f(forwardVector.x, 0, forwardVector.z).normalise(null);
			
			motion[0] = (float)(Math.atan2(forwardVector.y, Vector3f.dot(forwardVector, headingVector))/1.5);
			//motion[1] = (float)(Math.atan2(-headingVector.x, -headingVector.z));
			
			double gamma = Math.atan2(Math.abs(droneX - PointOnSpline.getX()), Math.abs(droneZ - PointOnSpline.getZ()));
				if (droneX > PointOnSpline.getX()){ // links tov eigen z-as 
					double angle = gamma;// - inputs.getHeading();
					motion[1] = (float) (angle);
				}
				else{ // rechts tov eigen vooruitlijn
					double angle = gamma;// + inputs.getHeading();
					motion[1] = (float) (-angle);
				}
			return motion;
			
		}
		else{ // geval x-as
			double xHelpPos = dronePos.getX() + 300;
			
			double zHelpPos = function.get(0) + function.get(1) * (xHelpPos) + function.get(2) * Math.pow(xHelpPos, 2) + function.get(3) * Math.pow(xHelpPos, 3);
			
			Point3D PointOnSpline = new Point3D( xHelpPos, nextCube.getY(), zHelpPos);

			
			float[] motion = new float[2];
			double droneX = dronePos.getX(), droneY = dronePos.getY(), droneZ = dronePos.getZ();

			Point3D currentPosition = new Point3D(inputs.getX(), inputs.getY(), inputs.getZ());
			Matrix4f toWorld = Utilities.getDroneToWorldTransformationMatrix(
					inputs.getHeading(), 
					inputs.getPitch(), 
					inputs.getRoll());
			
			// Calculate pitch/heading
			Vector3f requestedVector = new Vector3f(PointOnSpline.getX() - droneX, PointOnSpline.getY() - droneY, PointOnSpline.getZ() - droneZ);
			Vector3f forwardVector = Utilities.transformVector(toWorld, requestedVector);
			Vector3f headingVector = new Vector3f(forwardVector.x, 0, forwardVector.z).normalise(null);
			
			motion[0] = (float)(Math.atan2(forwardVector.y, Vector3f.dot(forwardVector, headingVector))/1.5);
			//motion[1] = (float)(Math.atan2(-headingVector.x, -headingVector.z));
			
			double gamma = Math.atan2(Math.abs(droneX - PointOnSpline.getX()), Math.abs(droneZ - PointOnSpline.getZ()));
				if (droneX > PointOnSpline.getX()){ // links tov eigen z-as 
					double angle = gamma;// - inputs.getHeading();
					motion[1] = (float) (angle);
				}
				else{ // rechts tov eigen vooruitlijn
					double angle = gamma;// + inputs.getHeading();
					motion[1] = (float) (-angle);
				}
			return motion;
			
		}
		
		
	}
	/**
	/**
	 * Calculates derivative of 3rd grade functions
	 * @param function
	 * @return
	 *
	public ArrayList<Double> derivative (ArrayList<Double> function){
		ArrayList<Double> derivative = new ArrayList<Double>();
		derivative.add(function.get(1));
		derivative.add(2*function.get(2));
		derivative.add(3*function.get(3));
		derivative.add((double) 0);
		return derivative;
	}
	*/
	
}
