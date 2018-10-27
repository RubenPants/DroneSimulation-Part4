package autopilot_utilities;

/**
 * A class of 4-dimensional matrices.
 * 
 * @author	Team Saffier
 * @version	1.0
 */
public class Matrix4f {

	/**
	 * Initiliase this new matrix with zero coordinates.
	 */
	public Matrix4f() {
		setIdentity();
	}
	
	/**
	 * Coordinates of this 4-dimensional vector.
	 */
	public double m00, m01, m02, m03, m10, m11, m12, m13, m20, m21, m22, m23, m30, m31, m32, m33;
	
	/**
	 * Set this matrix to the identity matrix.
	 * 
	 * @return This matrix after it has been set to an identity matrix.
	 */
	public Matrix4f setIdentity() {
		return setIdentity(this);
	}
	
	/**
	 * Set the given matrix to be the identity matrix.
	 * 
	 * @param 	matrix 
	 * 			The matrix to set to the identity matrix.
	 * @return 	The given matrix after it has been set to an identity matrix.
	 */
	public static Matrix4f setIdentity(Matrix4f m) {
		m.m00 = 1.0f;
		m.m01 = 0.0f;
		m.m02 = 0.0f;
		m.m03 = 0.0f;
		m.m10 = 0.0f;
		m.m11 = 1.0f;
		m.m12 = 0.0f;
		m.m13 = 0.0f;
		m.m20 = 0.0f;
		m.m21 = 0.0f;
		m.m22 = 1.0f;
		m.m23 = 0.0f;
		m.m30 = 0.0f;
		m.m31 = 0.0f;
		m.m32 = 0.0f;
		m.m33 = 1.0f;
		return m;
	}
	
	/**
	 * Set the given matrix to be a zero matrix.
	 * 
	 * @param 	matrix 
	 * 			The matrix to set to the zero matrix.
	 * @return 	The given matrix after it has been set to an identity matrix.
	 */
	public static Matrix4f setZero(Matrix4f m) {
		m.m00 = 0.0f;
		m.m01 = 0.0f;
		m.m02 = 0.0f;
		m.m03 = 0.0f;
		m.m10 = 0.0f;
		m.m11 = 0.0f;
		m.m12 = 0.0f;
		m.m13 = 0.0f;
		m.m20 = 0.0f;
		m.m21 = 0.0f;
		m.m22 = 0.0f;
		m.m23 = 0.0f;
		m.m30 = 0.0f;
		m.m31 = 0.0f;
		m.m32 = 0.0f;
		m.m33 = 0.0f;
		return m;
	}
	
	/**
	 * Invert this matrix.
	 * 
	 * @return This matrix if successful, null otherwise
	 */
	public Matrix4f invert() {
		return invert(this, this);
	}

	/**
	 * Invert the source matrix and put the result in the destination.
	 * 
	 * @param 	src 
	 * 			The source matrix
	 * @param 	dest 
	 * 			The destination matrix, or null if a new matrix is to be created.
	 * @return 	The inverted matrix if successful, null otherwise.
	 */
	public static Matrix4f invert(Matrix4f src, Matrix4f dest) {
		double determinant = src.determinant();
		if (determinant != 0) {
			/*
			 * m00 m01 m02 m03
			 * m10 m11 m12 m13
			 * m20 m21 m22 m23
			 * m30 m31 m32 m33
			 */
			if (dest == null)
				dest = new Matrix4f();
			double determinant_inv = 1f/determinant;

			// first row
			double t00 =  determinant3D(src.m11, src.m12, src.m13, src.m21, src.m22, src.m23, src.m31, src.m32, src.m33);
			double t01 = -determinant3D(src.m10, src.m12, src.m13, src.m20, src.m22, src.m23, src.m30, src.m32, src.m33);
			double t02 =  determinant3D(src.m10, src.m11, src.m13, src.m20, src.m21, src.m23, src.m30, src.m31, src.m33);
			double t03 = -determinant3D(src.m10, src.m11, src.m12, src.m20, src.m21, src.m22, src.m30, src.m31, src.m32);
			// second row
			double t10 = -determinant3D(src.m01, src.m02, src.m03, src.m21, src.m22, src.m23, src.m31, src.m32, src.m33);
			double t11 =  determinant3D(src.m00, src.m02, src.m03, src.m20, src.m22, src.m23, src.m30, src.m32, src.m33);
			double t12 = -determinant3D(src.m00, src.m01, src.m03, src.m20, src.m21, src.m23, src.m30, src.m31, src.m33);
			double t13 =  determinant3D(src.m00, src.m01, src.m02, src.m20, src.m21, src.m22, src.m30, src.m31, src.m32);
			// third row
			double t20 =  determinant3D(src.m01, src.m02, src.m03, src.m11, src.m12, src.m13, src.m31, src.m32, src.m33);
			double t21 = -determinant3D(src.m00, src.m02, src.m03, src.m10, src.m12, src.m13, src.m30, src.m32, src.m33);
			double t22 =  determinant3D(src.m00, src.m01, src.m03, src.m10, src.m11, src.m13, src.m30, src.m31, src.m33);
			double t23 = -determinant3D(src.m00, src.m01, src.m02, src.m10, src.m11, src.m12, src.m30, src.m31, src.m32);
			// fourth row
			double t30 = -determinant3D(src.m01, src.m02, src.m03, src.m11, src.m12, src.m13, src.m21, src.m22, src.m23);
			double t31 =  determinant3D(src.m00, src.m02, src.m03, src.m10, src.m12, src.m13, src.m20, src.m22, src.m23);
			double t32 = -determinant3D(src.m00, src.m01, src.m03, src.m10, src.m11, src.m13, src.m20, src.m21, src.m23);
			double t33 =  determinant3D(src.m00, src.m01, src.m02, src.m10, src.m11, src.m12, src.m20, src.m21, src.m22);

			// transpose and divide by the determinant
			dest.m00 = t00*determinant_inv;
			dest.m11 = t11*determinant_inv;
			dest.m22 = t22*determinant_inv;
			dest.m33 = t33*determinant_inv;
			dest.m01 = t10*determinant_inv;
			dest.m10 = t01*determinant_inv;
			dest.m20 = t02*determinant_inv;
			dest.m02 = t20*determinant_inv;
			dest.m12 = t21*determinant_inv;
			dest.m21 = t12*determinant_inv;
			dest.m03 = t30*determinant_inv;
			dest.m30 = t03*determinant_inv;
			dest.m13 = t31*determinant_inv;
			dest.m31 = t13*determinant_inv;
			dest.m32 = t23*determinant_inv;
			dest.m23 = t32*determinant_inv;
			
			return dest;
			
		} else
			return null;
		
	}
	
	/**
	 * Calculate the determinant of this 4D matrix.
	 * 
	 * @return The determinant of this matrix.
	 */
	public double determinant() {
		double f =
			m00
				* ((m11 * m22 * m33 + m12 * m23 * m31 + m13 * m21 * m32)
					- m13 * m22 * m31
					- m11 * m23 * m32
					- m12 * m21 * m33);
		f -= m01
			* ((m10 * m22 * m33 + m12 * m23 * m30 + m13 * m20 * m32)
				- m13 * m22 * m30
				- m10 * m23 * m32
				- m12 * m20 * m33);
		f += m02
			* ((m10 * m21 * m33 + m11 * m23 * m30 + m13 * m20 * m31)
				- m13 * m21 * m30
				- m10 * m23 * m31
				- m11 * m20 * m33);
		f -= m03
			* ((m10 * m21 * m32 + m11 * m22 * m30 + m12 * m20 * m31)
				- m12 * m21 * m30
				- m10 * m22 * m31
				- m11 * m20 * m32);
		return f;
	}

	/**
	 * Calculate the determinant of a 3x3 matrix.
	 */
	private static double determinant3D(double t00, double t01, double t02,
				     double t10, double t11, double t12,
				     double t20, double t21, double t22) {
		return   t00 * (t11 * t22 - t12 * t21)
		       + t01 * (t12 * t20 - t10 * t22)
		       + t02 * (t10 * t21 - t11 * t20);
	}
	
	/**
	 * Transform a Vector by a matrix and return the result in a destination
	 * vector.
	 * 	
	 * @param 	left 
	 * 			The left matrix.
	 * @param 	right 
	 * 			The right vector.
	 * @param 	dest 
	 * 			The destination vector, or null if a new one is to be created.
	 * @return 	The destination vector.
	 */
	public static Vector4f transform(Matrix4f left, Vector4f right, Vector4f dest) {
		
		if (dest == null)
			dest = new Vector4f();

		double x = left.m00 * right.x + left.m10 * right.y + left.m20 * right.z + left.m30 * right.w;
		double y = left.m01 * right.x + left.m11 * right.y + left.m21 * right.z + left.m31 * right.w;
		double z = left.m02 * right.x + left.m12 * right.y + left.m22 * right.z + left.m32 * right.w;
		double w = left.m03 * right.x + left.m13 * right.y + left.m23 * right.z + left.m33 * right.w;

		dest.x = x;
		dest.y = y;
		dest.z = z;
		dest.w = w;

		return dest;
		
	}
	
	/**
	 * Rotates the matrix around the given axis the specified angle
	 * @param angle the angle, in radians.
	 * @param axis The vector representing the rotation axis. Must be normalized.
	 * @return this
	 */
	public Matrix4f rotate(double angle, Vector3f axis) {
		return rotate(angle, axis, this);
	}

	/**
	 * Rotates the matrix around the given axis the specified angle.
	 * 
	 * @param 	angle 
	 * 			The angle, in radians.
	 * @param 	axis 
	 * 			The vector representing the rotation axis. Must be normalized.
	 * @param 	dest 
	 * 			The matrix to put the result, or null if a new matrix is to be created.
	 * @return 	The rotated matrix.
	 */
	public Matrix4f rotate(double angle, Vector3f axis, Matrix4f dest) {
		return rotate(angle, axis, this, dest);
	}

	/**
	 * Rotates the source matrix around the given axis the specified angle and
	 * put the result in the destination matrix.
	 * 
	 * @param 	angle 
	 * 			The angle, in radians.
	 * @param 	axis 
	 * 			The vector representing the rotation axis. Must be normalized.
	 * @param 	src 
	 * 			The matrix to rotate.
	 * @param 	dest 
	 * 			The matrix to put the result, or null if a new matrix is to be created.
	 * @return 	The rotated matrix.
	 */
	public static Matrix4f rotate(double angle, Vector3f axis, Matrix4f src, Matrix4f dest) {
		
		if (dest == null)
			dest = new Matrix4f();
		
		double c = (double) Math.cos(angle);
		double s = (double) Math.sin(angle);
		double oneminusc = 1.0f - c;
		double xy = axis.x*axis.y;
		double yz = axis.y*axis.z;
		double xz = axis.x*axis.z;
		double xs = axis.x*s;
		double ys = axis.y*s;
		double zs = axis.z*s;

		double f00 = axis.x*axis.x*oneminusc+c;
		double f01 = xy*oneminusc+zs;
		double f02 = xz*oneminusc-ys;
		// n[3] not used
		double f10 = xy*oneminusc-zs;
		double f11 = axis.y*axis.y*oneminusc+c;
		double f12 = yz*oneminusc+xs;
		// n[7] not used
		double f20 = xz*oneminusc+ys;
		double f21 = yz*oneminusc-xs;
		double f22 = axis.z*axis.z*oneminusc+c;

		double t00 = src.m00 * f00 + src.m10 * f01 + src.m20 * f02;
		double t01 = src.m01 * f00 + src.m11 * f01 + src.m21 * f02;
		double t02 = src.m02 * f00 + src.m12 * f01 + src.m22 * f02;
		double t03 = src.m03 * f00 + src.m13 * f01 + src.m23 * f02;
		double t10 = src.m00 * f10 + src.m10 * f11 + src.m20 * f12;
		double t11 = src.m01 * f10 + src.m11 * f11 + src.m21 * f12;
		double t12 = src.m02 * f10 + src.m12 * f11 + src.m22 * f12;
		double t13 = src.m03 * f10 + src.m13 * f11 + src.m23 * f12;
		
		dest.m20 = src.m00 * f20 + src.m10 * f21 + src.m20 * f22;
		dest.m21 = src.m01 * f20 + src.m11 * f21 + src.m21 * f22;
		dest.m22 = src.m02 * f20 + src.m12 * f21 + src.m22 * f22;
		dest.m23 = src.m03 * f20 + src.m13 * f21 + src.m23 * f22;
		dest.m00 = t00;
		dest.m01 = t01;
		dest.m02 = t02;
		dest.m03 = t03;
		dest.m10 = t10;
		dest.m11 = t11;
		dest.m12 = t12;
		dest.m13 = t13;
		
		return dest;
		
	}
	
}