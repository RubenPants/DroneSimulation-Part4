package autopilot_utilities;

/**
 * A class of 4-dimensional vectors.
 * 
 * @author	Team Saffier
 * @version	1.0
 */
public class Vector4f {

	/**
	 * Initiliase this new vector with zero coordinates.
	 */
	public Vector4f() {
		this(0.0, 0.0, 0.0, 0.0);
	}
	
	/**
	 * Initialize this 4-dimensional vector with given 3-dimensional vector.
	 * 
	 * @param 	vector
	 * 			The 3-dimensional vector to initialize this one with.
	 */
	public Vector4f(Vector3f vector) {
		this(vector.x, vector.y, vector.z, 1.0);
	}
	
	/**
	 * Initiliase this new vector with given coordinates.
	 */
	public Vector4f(double x, double y, double z, double w) {
		set(x, y, z, w);
	}
	
	/**
	 * Set the coordinates of this 3-dimensional vector.
	 * 
	 * @param 	x
	 * 			The new x coordinate for this vector.
	 * @param 	y
	 * 			The new y coordinate for this vector.
	 * @param 	z
	 * 			The new z coordinate for this vector.
	 * @param 	w
	 * 			The new w coordinate for this vector.
	 */
	public void set(double x, double y, double z, double w) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.w = w;
	}
	
	/**
	 * Calculate the length of this vector.
	 * 
	 * @return The length of the vector.
	 */
	public final double length() {
		return Math.sqrt(lengthSquared());
	}
	
	/**
	 * Get the squared length of this vector.
	 * 	
	 * @return	The squared length of this vector.
	 * 			| x * x + y * y + z * z + w * w
	 */
	public double lengthSquared() {
		return x * x + y * y + z * z + w * w;
	}
	
	/**
	 * Returns the angle (in radians) between this and the given vector.
	 * 
	 * @param 	other
	 * 			The vector for which the angle is desired.
	 * @return  The angle between this and the given vector.
	 */
	public double angleWith(Vector4f other) {
		Vector4f norm1 = this.normalise(null);
		Vector4f norm2 = other.normalise(null);
		return Math.acos(Vector4f.dot(norm1, norm2));
	}
	
	/**
	 * Normalize this vector and place the result in another vector.
	 * 
	 * @param 	dest 
	 * 			The destination vector, or null if a new vector is to be created.
	 * @return 	The normalized vector
	 */
	public Vector4f normalise(Vector4f dest) {
		double l = length();
		if (dest == null)
			dest = new Vector4f(x / l, y / l, z / l, w / l);
		else
			dest.set(x / l, y / l, z / l, w / l);
		return dest;
	}
	
	/**
	 * Negate this vector.
	 * 
	 * @param 	dest
	 * 			The destination vector, or null if a new vector is to be created.
	 * @return	This vector, negated.
	 */
	public Vector4f negate(Vector4f dest) {
		if (dest == null)
			dest = new Vector4f(-x, -y, -z, -w);
		else
			dest.set(-x, -y, -z, -w);
		return dest;
	}
	
	/**
	 * The dot product of two vectors is calculated as
	 * 	v1.x * v2.x + v1.y * v2.y + v1.z * v2.z + v1 .w * v2.w
	 * 
	 * @param 	left 
	 * 			The LHS vector.
	 * @param 	right 
	 * 			The RHS vector.
	 * @return 	Left dot right.
	 */
	public static double dot(Vector4f left, Vector4f right) {
		return left.x * right.x + left.y * right.y + left.z * right.z + left.w * right.w;
	}
	
	/**
	 * Coordinates of this 4-dimensional vector.
	 */
	public double x, y, z, w;
	
	@Override
	public String toString() {
		return "[" + this.x + " " + this.y + " " + this.z + " " + this.w + "]";
	}
	
}