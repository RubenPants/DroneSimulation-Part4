package autopilot_utilities;

/**
 * A class of utility methods for autopilots.
 * 
 * @author	Team Saffier
 * @version	1.0
 */
public class Utilities {

	/**
	 * Apply the given transformation to the given vector.
	 * 
	 * @param 	matrix
	 * 			The transformation matrix.
	 * @param 	vector
	 * 			The vector that is to be transformed.
	 * @return	The given vector after transformation.
	 */
	public static Vector3f transformVector(Matrix4f matrix, Vector3f vector) {
		Vector4f vector4 = new Vector4f(vector.x, vector.y, vector.z, 1.0);
		Matrix4f.transform(matrix, vector4, vector4);
		return new Vector3f(vector4.x, vector4.y, vector4.z);
	}

	/**
	 * Calculate the world-to-drone transformation matrix for given heading, pitch & roll.
	 * 
	 * @param 	heading
	 * 			The heading of the drone.
	 * @param 	pitch
	 * 			The pitch of the drone.
	 * @param 	roll
	 * 			The roll of the drone.
	 * @return	The world-to-drone transformation matrix for given drone configuration.
	 */
	public static Matrix4f getWorldToDroneTransformationMatrix(float heading, float pitch, float roll) {
		return Matrix4f.invert(getDroneToWorldTransformationMatrix(heading, pitch, roll), null);
	}

	/**
	 * Calculate the drone-to-world transformation matrix for given heading, pitch & roll.
	 * 
	 * @param 	heading
	 * 			The heading of the drone.
	 * @param 	pitch
	 * 			The pitch of the drone.
	 * @param 	roll
	 * 			The roll of the drone.
	 * @return	The drone-to-world transformation matrix for given drone configuration.
	 */
	public static Matrix4f getDroneToWorldTransformationMatrix(float heading, float pitch, float roll) {
		
		Matrix4f matrix = new Matrix4f();
		matrix.setIdentity();
		Matrix4f.rotate((float) heading, new Vector3f(0, 1, 0), matrix, matrix);
		Matrix4f.rotate((float) pitch, new Vector3f(1, 0, 0), matrix, matrix);
		Matrix4f.rotate((float) roll, new Vector3f(0, 0, 1), matrix, matrix);
		
		return matrix;
		
	}
	
	/**
	 * Calculate the sum of the given vectors.
	 * 
	 * @param 	vectors
	 * 			The vectors whose sum vector is desired.
	 * @return	The sum of the given vectors.
	 */
	public static Vector3f addVectors(Vector3f... vectors) {
		Vector3f result = new Vector3f();
		for(Vector3f vector: vectors) {
			Vector3f.add(result, vector, result);
		}
		return result;
	}
	
	/**
	 * Print the given 3D vector.
	 * 
	 * @param 	name
	 * 			The name of the vector.
	 * @param 	vector
	 * 			The vector that is to be printed.
	 */
	public static void printVector(String name, Vector3f vector) {
		System.out.println(name + " " + vector.x + " " + vector.y + " " + vector.z);
	}
	
	/**
	 * Scale the given vector by given factor.
	 * 
	 * @param 	vector
	 * 			The vector to scale.
	 * @param 	scale
	 * 			The scale factor.
	 * @return 	The given vector scaled by the given factor.
	 */
	public static Vector3f scaleVector(Vector3f vector, float scale) {
		return new Vector3f(scale*vector.x, scale*vector.y, scale*vector.z);
	}
	
	public static double calculateHorizontalAngle(Point3D pos1, Point3D pos2, Point3D pos3){
		double P1X = pos2.getX();
		double P1Z = pos2.getZ();
		double P2X = pos1.getX();
		double P2Z = pos1.getZ();
		double P3X = pos3.getX();
		double P3Z = pos3.getZ();
        double numerator = P2Z*(P1X-P3X) + P1Z*(P3X-P2X) + P3Z*(P2X-P1X);
        double denominator = (P2X-P1X)*(P1X-P3X) + (P2Z-P1Z)*(P1Z-P3Z);
        double ratio = numerator/denominator;

        double angleRad = Math.atan(ratio);

        return angleRad;
	}
	
	/**
	 * Calculates the distance in x and z from a wanted distance starting from the center of the airport.
	 * The first element of the float is the distance in X and the second is the distance in Z.
	 * 
	 * @param 	centerX
	 * 			The x-value of the center of the airport.
	 * @param 	centerZ
	 * 			The z-value of the center of the airport.
	 * @param 	centerToRunway0X
	 * 			The direction from the x-value of the center of the airport to the x-value of runway 0.
	 * @param 	centerToRunway0Z
	 * 			The direction from the z-value of the center of the airport to the z-value of runway 0.
	 * @param 	wantedDistance
	 * 			The wanted distance straight from the center of the airport.
	 * @param	wantedRunway
	 * 			0 = false, 1 = true
	 * @return	Two values, the first is the wanted distance in X, the second is the wanted distance in Z.
	 */
	public static float[] distanceFromRunway(float centerX, float centerZ, float centerToRunway0X, float centerToRunway0Z, float wantedDistance,
			boolean wantedRunway) {
		float[] distance = new float[2];
		if (!wantedRunway) {
			distance[0] = centerX + wantedDistance*centerToRunway0X;
			distance[1] = centerZ + wantedDistance*centerToRunway0Z;
		}
		else {
			distance[0] = centerX - wantedDistance*centerToRunway0X;
			distance[1] = centerZ - wantedDistance*centerToRunway0Z;
		}
		return distance;
	}
	
	/**
	 * Calculates the distance in x and z from a wanted distance starting from the edge of the given runway, at the side of the given gate.
	 * The first element of the float is the distance in X and the second is the distance in Z.
	 * 
	 * @param 	centerX
	 * 			The x-value of the center of the airport.
	 * @param 	centerZ
	 * 			The z-value of the center of the airport.
	 * @param 	centerToRunway0X
	 * 			The direction from the x-value of the center of the airport to the x-value of runway 0.
	 * @param 	centerToRunway0Z
	 * 			The direction from the z-value of the center of the airport to the z-value of runway 0.
	 * @param 	wantedDistance
	 * 			The wanted distance straight from runway 0.
	 * @param	wantedRunway
	 * 			0 = false, 1 = true
	 * @param	gateSide
	 * 			0 = false, 1 = true
	 * @param	w
	 * 			The width of the gates.
	 * @param	l
	 * 			The length of the runway.
	 * @return	Two values, the first is the wanted distance in X, the second is the wanted distance in Z.
	 */
	public static float[] distanceFromRunway(float centerX, float centerZ, float centerToRunway0X, float centerToRunway0Z, float wantedDistance,
			boolean wantedRunway, boolean gateSide, float w, float l) {
	 	Point3D pos1 = new Point3D(0,0,centerZ);
	 	Point3D pos2 = new Point3D(centerX,0,centerZ);
	 	float runway0X = centerX + (centerToRunway0X*((1/2)*w+(1/2)*l));
	 	float runway0Z = centerZ + (centerToRunway0Z*((1/2)*w+(1/2)*l));
		float runway1X = centerX - (centerToRunway0X*((1/2)*w+(1/2)*l));
		float runway1Z = centerZ - (centerToRunway0Z*((1/2)*w+(1/2)*l));
	 	Point3D pos3 = new Point3D(runway0X,0,runway0Z);
	 	double angle = calculateHorizontalAngle(pos1,pos2,pos3);
	 	if (angle > Math.PI/2) {
	 		angle = Math.PI - angle;
	 	}
	 	float[] distance = new float[2];
	 	float differenceX = (float)Math.cos(angle)*wantedDistance;
		float differenceZ = (float)Math.sin(angle)*wantedDistance;
		if (!wantedRunway && gateSide) {
			if (centerX >= runway0X && centerZ <= runway0Z) {
				distance[0] = runway0X - differenceX;
				distance[1] = runway0Z - differenceZ;
			}
			else if (centerX <= runway0X && centerZ >= runway0Z) {
				distance[0] = runway0X + differenceX;
				distance[1] = runway0Z + differenceZ;
			}
			else if (centerX <= runway0X && centerZ <= runway0Z) {
				distance[0] = runway0X - differenceX;
				distance[1] = runway0Z + differenceZ;
			}
			else {
				distance[0] = runway0X + differenceX;
				distance[1] = runway0Z - differenceZ;
			}
	 	}
		else if (!wantedRunway && !gateSide) {
			if (centerX >= runway0X && centerZ <= runway0Z) {
				distance[0] = runway0X + differenceX;
				distance[1] = runway0Z + differenceZ;
			}
			else if (centerX <= runway0X && centerZ >= runway0Z) {
				distance[0] = runway0X - differenceX;
				distance[1] = runway0Z - differenceZ;
			}
			else if (centerX <= runway0X && centerZ <= runway0Z) {
				distance[0] = runway0X + differenceX;
				distance[1] = runway0Z - differenceZ;
			}
			else {
				distance[0] = runway0X - differenceX;
				distance[1] = runway0Z + differenceZ;
			}
	 	}
		else if (wantedRunway && gateSide) {
			if (centerX >= runway0X && centerZ <= runway0Z) {
				distance[0] = runway1X - differenceX;
				distance[1] = runway1Z - differenceZ;
			}
			else if (centerX <= runway0X && centerZ >= runway0Z) {
				distance[0] = runway1X + differenceX;
				distance[1] = runway1Z + differenceZ;
			}
			else if (centerX <= runway0X && centerZ <= runway0Z) {
				distance[0] = runway1X - differenceX;
				distance[1] = runway1Z + differenceZ;
			}
			else {
				distance[0] = runway1X + differenceX;
				distance[1] = runway1Z - differenceZ;
			}
	 	}
	 	else {
			if (centerX >= runway0X && centerZ <= runway0Z) {
				distance[0] = runway1X + differenceX;
				distance[1] = runway1Z + differenceZ;
			}
			else if (centerX <= runway0X && centerZ >= runway0Z) {
				distance[0] = runway1X - differenceX;
				distance[1] = runway1Z - differenceZ;
			}
			else if (centerX <= runway0X && centerZ <= runway0Z) {
				distance[0] = runway1X + differenceX;
				distance[1] = runway1Z - differenceZ;
			}
			else {
				distance[0] = runway1X - differenceX;
				distance[1] = runway1Z + differenceZ;
			}
	 	}
	 	return distance;
	 }
		
}