package autopilot_utilities;

/**
 * A class of 3-dimensional vectors.
 * 
 * @author	Team Saffier
 * @version	1.0
 */
public class Vector3f {

	/**
	 * Initiliase this new vector with zero coordinates.
	 */
	public Vector3f() {
		this(0.0, 0.0, 0.0);
	}
	
	/**
	 * Initiliase this new vector with given coordinates.
	 */
	public Vector3f(double x, double y, double z) {
		set(x, y, z);
	}
	
	/**
	 * Initialize this 3-dimensional vector with given 4-dimensional vector.
	 * 
	 * @param 	vector
	 * 			The 4-dimensional vector to initialize this one with.
	 */
	public Vector3f(Vector4f vector) {
		this(vector.x, vector.y, vector.z);
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
	 */
	public void set(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	/**
	 * Coordinates of this 3-dimensional vector.
	 */
	public double x, y, z;
	
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
	 * 			| x * x + y * y + z * z
	 */
	public double lengthSquared() {
		return x * x + y * y + z * z;
	}
	
	/**
	 * Returns the angle (in radians) between this and the given vector.
	 * 
	 * @param 	other
	 * 			The vector for which the angle is desired.
	 * @return  The angle between this and the given vector.
	 */
	public double angleWith(Vector3f other) {
		Vector3f norm1 = this.normalise(null);
		Vector3f norm2 = other.normalise(null);
		return Math.acos(Vector3f.dot(norm1, norm2));
	}
	
	/**
	 * Negate this vector.
	 * 
	 * @param 	dest
	 * 			The destination vector, or null if a new vector is to be created.
	 * @return	This vector, negated.
	 */
	public Vector3f negate(Vector3f dest) {
		if (dest == null)
			dest = new Vector3f(-x, -y, -z);
		else
			dest.set(-x, -y, -z);
		return dest;
	}
	
	/**
	 * Normalize this vector and place the result in another vector.
	 * 
	 * @param 	dest 
	 * 			The destination vector, or null if a new vector is to be created.
	 * @return 	The normalized vector
	 */
	public Vector3f normalise(Vector3f dest) {
		double l = length();
		if (dest == null)
			dest = new Vector3f(x / l, y / l, z / l);
		else
			dest.set(x / l, y / l, z / l);
		return dest;
	}
	
	/**
	 * The dot product of two vectors is calculated as
	 * 	v1.x * v2.x + v1.y * v2.y + v1.z * v2.z
	 * 
	 * @param 	left 
	 * 			The LHS vector.
	 * @param 	right 
	 * 			The RHS vector.
	 * @return 	Left dot right.
	 */
	public static double dot(Vector3f left, Vector3f right) {
		return left.x * right.x + left.y * right.y + left.z * right.z;
	}
	
	/**
	 * Scale this vector.
	 * 
	 * @param 	scale
	 * 			The scale factor that is to be used.
	 * @return	This vector, scaled by the given factor.
	 */
	public Vector3f scale(float scale) {
		x *= scale;
		y *= scale;
		z *= scale;
		return this;
	}
	
	/**
	 * Subtract a vector from another vector and place the result in a destination
	 * vector.
	 * 
	 * @param 	left 
	 * 			The LHS vector.
	 * @param 	right 
	 * 			The RHS vector.
	 * @param 	dest 
	 * 			The destination vector, or null if a new vector is to be created.
	 * @return 	Left minus right in dest.
	 */
	public static Vector3f sub(Vector3f left, Vector3f right, Vector3f dest) {
		if (dest == null)
			return new Vector3f(left.x - right.x, left.y - right.y, left.z - right.z);
		else {
			dest.set(left.x - right.x, left.y - right.y, left.z - right.z);
			return dest;
		}
	}
	
	/**
	 * The cross product of two vectors.
	 *
	 * @param 	left 
	 * 			The LHS vector.
	 * @param 	right 
	 * 			The RHS vector.
	 * @param 	dest 
	 * 			The destination result, or null if a new vector is to be created.
	 * @return 	Left cross right.
	 */
	public static Vector3f cross(Vector3f left, Vector3f right, Vector3f dest) {

		if (dest == null)
			dest = new Vector3f();

		dest.set(
				left.y * right.z - left.z * right.y,
				right.x * left.z - right.z * left.x,
				left.x * right.y - left.y * right.x
				);

		return dest;
		
	}
	
	/**
	 * Add a vector to another vector and place the result in a destination
	 * vector.
	 * 
	 * @param 	left 
	 * 			The LHS vector.
	 * @param 	right 
	 * 			The RHS vector.
	 * @param 	dest 
	 * 			The destination vector, or null if a new vector is to be created.
	 * @return 	The sum of left and right in dest.
	 */
	public static Vector3f add(Vector3f left, Vector3f right, Vector3f dest) {
		if (dest == null)
			return new Vector3f(left.x + right.x, left.y + right.y, left.z + right.z);
		else {
			dest.set(left.x + right.x, left.y + right.y, left.z + right.z);
			return dest;
		}
	}
	
	@Override
	public String toString() {
		return "[" + this.x + " " + this.y + " " + this.z + "]";
	}
	
}