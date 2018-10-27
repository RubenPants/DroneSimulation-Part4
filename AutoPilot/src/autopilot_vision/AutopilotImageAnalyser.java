package autopilot_vision;

import java.util.ArrayList;
import java.util.List;

import autopilot_utilities.Vector3f;
import autopilot_utilities.Matrix4f;
import autopilot_utilities.Path3D;
import autopilot_utilities.Vector4f;
import autopilot_utilities.Line2D;
import autopilot_utilities.Point2D;
import autopilot_utilities.Point3D;
import interfaces.AutopilotConfig;
import interfaces.AutopilotInputs;
import autopilot_vision.CubeDetectionAlgorithm.CubeDetectionAlgorithmType;

/**
 * A class for autopilot camera image analysers, recognizing objects and locating them.
 * 
 * @author 	Team Saffier
 * @version 	1.0
 */
public class AutopilotImageAnalyser {

	/**
	 * Variable denoting whether or not this object logs information.
	 */
	public static boolean ENABLE_LOGGING = true;
	
	/**
	 * Variable denoting whether or not this analyser uses history (previously seen cubes).
	 *  This used to be a flag set on/off at each analysis, meaning that history would always be kept
	 *  but not included in return values.
	 */
	private final boolean USE_HISTORY = false;
	
	/**
	 * The side length of cubes.
	 */
	public static double SIDE_LENGTH = 5.0;

	/**
	 * Initialise this new image analyser with a basic cube recognition algorithm, a
	 * 	default width & height for input images, and a default horizontal & vertical angle
	 * 	of view of these very images.
	 * 
	 * @effect	This image analyser is initialised with a basic cube recognition algorithm.
	 */
	public AutopilotImageAnalyser() {
		this(CubeDetectionAlgorithmType.ADVANCED, false);
	}

	/**
	 * Initialise this new image analyser with given input image size, input image
	 *  horizontal/vertical angle of view and cube recognition algorithm type.
	 *  
	 * @param 	type
	 * 			The type of cube detection algorithm that is to be used by this new
	 * 			 image analyser.
	 * @param	useHistory
	 * 			Whether or not history is to be used (i.e. previously seen cubes are kept).
	 */
	public AutopilotImageAnalyser(CubeDetectionAlgorithmType type, boolean useHistory) {
		this.cubeDetectionAlgorithm = CubeDetectionAlgorithm.initializeAlgorithm(type);
	}

	/**
	 * Variable registering an algorithm used by this image analyser for detecting and locating cubes.
	 */
	private CubeDetectionAlgorithm cubeDetectionAlgorithm;

	/**
	 * Locate a red unit cube in the image in the given input, keeping in mind the given configuration
	 * 	of the autopilot.
	 * 
	 * @param 	configuration
	 * 			The autopilot's configuration.
	 * @param 	input
	 * 			The autopilot's input that is to be analysed.
	 * @return	An integer array with the center coordinates of the red cube, as well as its
	 * 				approximate distance to the camera, or null if no cube was found.
	 * 				[0]x [1]y [2]vertical angle [3]horizontal angle [4]distance

	 */
	public Cube locateRedCube(AutopilotConfig configuration, AutopilotInputs input) {

		// Read input
		ImageSize inputSize = new ImageSize(configuration.getNbColumns(), configuration.getNbRows());
		Image inputImage = new Image(input.getImage(), inputSize);
		return cubeDetectionAlgorithm.locateUnitCube(inputImage, 0.0f, 1.0f);

	}

	/**
	 * Locate all the cubes in an input image.
	 * 
	 * @param 	configuration
	 * 			The configuration of the autopilot.
	 * @param 	input
	 * 			The latest input handed to the autopilot.
	 * @param	path
	 * 			The input path of the autopilot.
	 * @return	An array of cubes - those seen in the input image and (if history is used)
	 * 			those seen previously. 
	 */
	public ArrayList<Cube> locateAllCubes(AutopilotConfig configuration, AutopilotInputs input, Path3D path) {

		// Determine iteration
		currentIteration++;
		if (currentIteration == analysisFrequency)
			currentIteration = 0; // Reset iteration count
		else
			return null; // No analysis yet
		
		/**
		 * If performance is that important, the image analyser could select parts of the image
		 *  that are to be analysed by calculating maximum bounding boxes for every cube in the
		 *  input path, and telling the algorithms that only the pixels in these boxes are to be
		 *  analysed.
		 *  
		 *  This would be especially useful if the resolution becomes rather big.
		 */

		// Detect cubes
		ImageSize inputSize = new ImageSize(configuration.getNbColumns(), configuration.getNbRows());
		Image inputImage = new Image(input.getImage(), inputSize);
		ArrayList<Cube> cubes = cubeDetectionAlgorithm.locateUnitCubes(inputImage, true);

		// Calculate distances/positions
		for (Cube cube : cubes) {
			cube.setVisible(true);
			if (Math.abs(input.getRoll()) < Math.PI/18 && updateCubeLocation(cube, configuration, input)) {
				Point3D approximateLocation = cube.getLocation();
				if (path != null && approximateLocation != null) {
					List<Point3D> coordinates = path.getCoordinates();
					for (int i=0 ; i<coordinates.size() ; i++)
						if (coordinates.get(i).distanceTo(approximateLocation) < 5.0) {
							//path.setCube(i, cube);
							path.updateCoordinate(i, approximateLocation);
							break;
						}
				}
			}
			else {
				cube.setDistance(-1.0);
				// cube.setLocation(null);
			}
		}

		// Reset history (make all previously seen cubes invisible)
		// 	and update it with the cubes that have just been seen
		if (USE_HISTORY) {
			
			updateCubeList(cubes);
			
			// Removal of cubes when close enough
			Point3D currentLocation = new Point3D(input.getX(), input.getY(), input.getZ());
			ArrayList<Cube> toDelete = new ArrayList<Cube>();
			for (Cube cube : history) { 
				if (!cube.isVisible()
					&& cube.getLocation() != null 
					&& cube.getLocation().distanceTo(currentLocation) < 5) {
					toDelete.add(cube);
					if (path != null && cube.getDistance() < 40.0f) { // Update path (code will be run very infrequently)
						List<Point3D> coordinates = path.getCoordinates();
						for (int i=0 ; i<coordinates.size() ; i++)
							if (coordinates.get(i).distanceTo(cube.getLocation()) < 7.0) {
								path.setVisited(i, true);
								break;
							}
					}
				}
			}
			history.removeAll(toDelete);
			
		}

		// Log output
		if (ENABLE_LOGGING) {
			System.out.println("DETECTED CUBES [");
			for (Cube cube : (USE_HISTORY ? history : cubes))
				System.out.println(cube);
			System.out.println("]");
		}

		return (USE_HISTORY ? history : cubes);

	}
	
	/**
	 * The current analysis iteration.
	 */
	private int currentIteration = 0;
	
	/**
	 * Frequency of analysis. Only when we're at this iteration the analysis is done.
	 *  Then the currentIteration variable is reset to zero.
	 *  Must be at least 1 (to analyse at every iteration).
	 *  
	 */
	private int analysisFrequency = 2;

	/**
	 * Remove all history from this analyser (i.e. all previously seen cubes).
	 */
	public void clearHistory() {
		history.clear();
	}

	/**
	 * Update the current list of cubes using the given list of cubes that have just been detected.
	 * 
	 * @param 	cubes
	 * 			The list of cubes that was just detected.
	 */
	protected void updateCubeList(ArrayList<Cube> cubes) {

		// Make old cubes invisible
		for (Cube cube : history)
			cube.setVisible(false);

		// Tag the ones recently detected as visible
		// New cubes are simply added to the list
		boolean added;
		for (Cube cube : cubes) {
			added = false;
			for (Cube oldCube : history) {
				float huediff = Math.abs(cube.getHue() - oldCube.getHue());
				float satdiff = Math.abs(cube.getSaturation() - oldCube.getSaturation());
				if ((huediff < CubeDetectionAlgorithm.THRESHOLD || 1.0 - huediff < CubeDetectionAlgorithm.THRESHOLD)
						&& (satdiff < CubeDetectionAlgorithm.THRESHOLD || 1.0 - satdiff < CubeDetectionAlgorithm.THRESHOLD)) {
					oldCube.setVisible(true);
					oldCube.setBoundingBox(cube.getBoundingBox());
					if (cube.getLocation() != null) {
						oldCube.setDistance(cube.getDistance());
						oldCube.setLocation(cube.getLocation());
					}
					added = true;
					break;
				}
			}
			if (!added) { // New  cube, add it to the list
				cube.setVisible(true);
				history.add(cube);
			}
		}

	}

	/**
	 * A list of cubes that have been seen so far.
	 * 
	 * @note 	This could be implemented using other data structures like hashmaps but 
	 * 			this is not necessary for now ; it'll matter if 100 or more cubes are regularly seen
	 * 			on an image.
	 */
	protected ArrayList<Cube> history = new ArrayList<Cube>();

	/**
	 * Update the location/distance of/to the given cube.
	 * 
	 * @param 	cube
	 * 			The cube whose location is to be approximated.
	 * @param 	config
	 * 			The configuration of the autopilot.
	 * @param 	input
	 * 			The inputs for the autopilot.
	 * @return	True if and only if the location of the cube was updated
	 */
	public static boolean updateCubeLocation(Cube cube, AutopilotConfig config, AutopilotInputs input) {

		// Pre-processing
		float haov = config.getHorizontalAngleOfView(), vaov = config.getVerticalAngleOfView();
		int cols = config.getNbColumns(), rows = config.getNbRows();
		Point3D droneCoordinates = new Point3D(input.getX(), input.getY(), input.getZ());
		Matrix4f droneToWorld = autopilot_utilities.Utilities.getDroneToWorldTransformationMatrix(
				input.getHeading(), 
				input.getPitch(), 
				input.getRoll());
		Matrix4f worldToDrone = autopilot_utilities.Utilities.getWorldToDroneTransformationMatrix(
				input.getHeading(), 
				input.getPitch(), 
				input.getRoll());

		// Get the longest diagonal that is vertical
		CubeDiagonal validDiagonal = null;
		boolean diagonalDownwards = true;
		int diagonalCount = cube.getNbDiagonals();
		double minLength = 10.0;
		for (int i=0 ; i<diagonalCount ; i+=2) {
			CubeDiagonal first = cube.getDiagonalAtIndex(i), second = cube.getDiagonalAtIndex(i+1);
			if (first.isFromVerticalSide()) {
				diagonalDownwards = !first.isDownwards(second, input.getRoll());
				if (first.length() > minLength) {
					validDiagonal = first;
					minLength = first.length();
				} else {
					diagonalDownwards = !diagonalDownwards;
					validDiagonal = second;
					minLength = second.length();
				}
			}
		}
		if (validDiagonal == null)
			return false;
		if (ENABLE_LOGGING)
			System.out.println("Diagonal considered by vision : " + validDiagonal + " (downwards = " + diagonalDownwards + ")");

		// Calculate camera's distance to projection plane (l)
		double delta = (Math.PI/2 - haov);
		double l = Math.tan(delta) * (cols/2);

		// Get angle under which edge is projected (used to be done with law of cosine, now with dot product)
		Point2D firstPoint = validDiagonal.getStartPoint(), lastPoint = validDiagonal.getEndPoint();
		//Point2D midPoint = new Point2D(firstPoint.getX() + (lastPoint.getX() - firstPoint.getX()) / 2,
				//firstPoint.getY() + (lastPoint.getY() - firstPoint.getY()) / 2);
		Vector3f firstVector = new Vector3f(firstPoint.getX()-100, 100-firstPoint.getY(), l);
		//Vector3f midVector = new Vector3f(midPoint.getX()-100, 100-midPoint.getY(), l);
		Vector3f lastVector = new Vector3f(lastPoint.getX()-100, 100-lastPoint.getY(), l);
		//Vector3f firstVector = pixelToVector(firstPoint, cols, rows, haov, vaov, null);
		//Vector3f lastVector = pixelToVector(lastPoint, cols, rows, haov, vaov, null);
		//Vector3f midVector = pixelToVector(midPoint, cols, rows, haov, vaov, null);
		double gamma = firstVector.angleWith(lastVector);
		if (ENABLE_LOGGING) {
			System.out.println("First vector : " + firstVector);
			System.out.println("Last vector : " + lastVector);
			System.out.println("Angle in between (gamma) : " + gamma);
		}

		// Check which cube side we're dealing with, calculate correctional shift
		//  and create artificial vector representing slope of the diagonal
		float diagonalValue = validDiagonal.getBrightnessRounded();
		Vector3f shift = null, vector = null;
		// Positive Y = 45.0f, Negative Y = 20.0f
		if (diagonalValue == 40.0f) { // Positive X
			shift = new Vector3f(-2.5, 0.0, 0.0);
			vector = (diagonalDownwards ? new Vector3f(0,1,1) : new Vector3f(0,1,-1));
		}
		else if (diagonalValue == 25.0f) { // Negative X
			shift = new Vector3f(2.5, 0.0, 0.0);
			vector = (diagonalDownwards ? new Vector3f(0,1,-1) : new Vector3f(0,1,1));
		}
		else if (diagonalValue == 35.0f) { // Positive Z
			shift = new Vector3f(0.0, 0.0, -2.5);
			vector = (diagonalDownwards ? new Vector3f(-1,1,0) : new Vector3f(1,1,0));
		}
		else if (diagonalValue == 30.0f) { // Negative Z
			shift = new Vector3f(0.0, 0.0, 2.5);
			vector = (diagonalDownwards ? new Vector3f(1,1,0) : new Vector3f(-1,1,0));
		}
		else
			return false; // Other sides not considered yet
		if (ENABLE_LOGGING)
			System.out.println("Shift : " + shift);
		if (ENABLE_LOGGING)
			System.out.println("Diagonal vector : " + vector);
		vector = new Vector3f(Matrix4f.transform(worldToDrone, new Vector4f(vector), null));
		if (ENABLE_LOGGING)
			System.out.println("Transformed diagonal vector : " + vector);
		
		// Calculate angle that one projection arm makes with artificial vector (= 'beta')
		double beta1 = vector.angleWith(firstVector);
		double beta2 = vector.angleWith(lastVector);
		// Only sine is necessary so no need to correct
		// if ((firstVector.x < 0.0 && beta > Math.PI/2) || (firstVector.x > 0.0 && beta < Math.PI/2))
			// beta = Math.PI - beta;
		// beta = Math.PI - beta;
		if (ENABLE_LOGGING)
			System.out.println("Beta angles : " + beta1 + " and " + beta2);
		// beta = Math.PI - gamma - test.angleWith(vector);
		
		// Determine length of projection arm to middle of diagonal and use it to calculate location (using law of sine + value of beta/gamma)
		double xFirst = Math.abs(Math.sqrt(2*SIDE_LENGTH*SIDE_LENGTH) * Math.sin(beta2) / Math.sin(gamma));
		double xLast = Math.abs(Math.sqrt(2*SIDE_LENGTH*SIDE_LENGTH) * Math.sin(beta1) / Math.sin(gamma));
		if (ENABLE_LOGGING)
			System.out.println("Distances to end points : " + xFirst + " and " + xLast);
		Vector3f locationLast = lastVector;
		locationLast.normalise(locationLast);
		locationLast.scale((float)(xLast));
		Vector3f locationFirst = firstVector;
		locationFirst.normalise(locationFirst);
		locationFirst.scale((float)(xFirst));
		Vector3f location = new Vector3f(firstVector.x + (lastVector.x-firstVector.x) / 2.0,
				firstVector.y + (lastVector.y-firstVector.y) / 2.0,
				firstVector.z + (lastVector.z-firstVector.z) / 2.0);
		
		// Shift location and transform to world coordinates
		if (ENABLE_LOGGING)
			System.out.println("Cube Location (Drone coords) : " + location);
		Vector4f location4 = Matrix4f.transform(droneToWorld, new Vector4f(location), null);
		Vector3f droneLocation = new Vector3f(input.getX(), input.getY(), input.getZ());
		Vector3f.add(new Vector3f(location4), droneLocation, location);
		Vector3f.add(location, shift, location);
		Point3D cubeCenter = new Point3D(location.x, location.y, location.z);
		if (ENABLE_LOGGING)
			System.out.println("Cube Center : " + cubeCenter);

		// Update location of cube
		cube.setLocation(cubeCenter);
		cube.setDistance(droneCoordinates.distanceTo(cubeCenter));

		return true;

	}

	/**
	 * Approximate the 3-dimensional location for the given point of an image
	 *  given a distance.
	 *  
	 *  @param	point
	 *  			The location of the point on the input image.
	 *  @param	distance
	 *  			The distance of the cube to the camera.
	 *  @param	cameraCoordinates
	 *  			The coordinates of the camera in the world.
	 *  @param	cameraToWorldTransformationMatrix
	 *  			The matrix to transform from camera to world coordinates.
	 *  @param 	cols
	 *  			The horizontal resolution of the camera.
	 *  @param 	rows
	 *  			The vertical resolution of the camera.
	 *  @param 	haov
	 *  			The horizontal angle of view of the camera.
	 *  @param 	vaov
	 *  			The vertical resolution of the camera.
	 */
	public static Point3D approximateLocation(Point2D point, 
			double distance,
			Vector3f cameraCoordinates,
			Matrix4f cameraToWorldTransformationMatrix, 
			int cols,
			int rows,
			float haov,
			float vaov) {

		// Calculate pitch and heading of cube relative to the drone
		double horizontal = (point.getX() - cols/2)/(cols/2), vertical = (rows/2 - point.getY())/(rows/2);
		float pitchForCube = (float)(vertical * vaov / 2);
		float headingForCube = - (float)(horizontal * haov / 2);

		// Calculate vector directed from the drone towards the cube
		Vector3f vector = new Vector3f(
				(float)-Math.sin(headingForCube),
				(float)Math.sin(pitchForCube),
				(float)-Math.cos(headingForCube)
				);
		vector.normalise(vector);
		// System.out.println("NORM " + pitchForCube + " --- " + headingForCube + " --- " + cubeVector);
		vector.scale((float)distance);
		// System.out.println("SCALED " + pitchForCube + " --- " + headingForCube + " --- " + cubeVector);

		// Transform cube vector from drone to world
		Vector4f cubeVectorTransform = new Vector4f(
				vector.x, 
				vector.y, 
				vector.z, 
				1);
		cubeVectorTransform = Matrix4f.transform(cameraToWorldTransformationMatrix, cubeVectorTransform, null);
		//		System.out.println("TRANSFORM " + pitchForCube + " --- " + headingForCube + " --- " + cubeVectorTransform);

		return new Point3D(
				cameraCoordinates.x + cubeVectorTransform.x,
				cameraCoordinates.y + cubeVectorTransform.y,
				cameraCoordinates.z + cubeVectorTransform.z
				);

	}

	/**
	 * Check whether the given edge can be used for distance approximation.
	 *  This is true if and only if it does not touch the border of the frame.
	 *  
	 * @param 	edge
	 * 			The edge to check for.
	 * @param 	rows
	 * 			The vertical resolution of the frame.
	 * @param 	cols
	 * 			The horizontal resolution of the frame.
	 */
	private static boolean isValidEdge(Line2D edge, int rows, int cols) {
		Point2D firstPoint = edge.getStartPoint(), lastPoint = edge.getEndPoint();
		if (firstPoint.getX() > 0 && firstPoint.getY() > 0 
				&& firstPoint.getX() < cols-1 && firstPoint.getY() < rows-1
				&& lastPoint.getX() > 0 && lastPoint.getY() > 0 
				&& lastPoint.getX() < cols-1 && lastPoint.getY() < rows-1) {
			return true;
		}
		return false;
	}

	/**
	 * Calculate the vector pointing to the point corresponding to the given pixel.
	 * 
	 * @param 	point
	 * 			The pixel coordinates.
	 * @param 	cols
	 * 			The horizontal resolution of the frame.
	 * @param 	rows
	 * 			The vertical resolution of the frame.
	 * @param 	haov
	 *  			The horizontal angle of view of the camera.
	 * @param 	vaov
	 *  			The vertical resolution of the camera.
	 * @param	cameraToWorldTransformationMatrix
	 * 			The camera to world transformation matrix, or null if no transformation is desired.
	 * @return	A 3D vector pointing to the point corresponding to the given pixel.
	 */
	public static Vector3f pixelToVector(Point2D point, int cols, int rows, float haov, float vaov, Matrix4f cameraToWorldTransformationMatrix) {

		// Calculate pitch and heading of cube relative to the drone
		double horizontal = (point.getX() - cols/2)/(cols/2.0), vertical = (rows/2 - point.getY())/(rows/2.0);
		float pitch = (float)(vertical * vaov / 2.0);
		float heading = - (float)(horizontal * haov / 2.0);

		// Calculate vector directed from the drone towards the cube
		Vector3f vector = new Vector3f((float)-Math.sin(heading), (float)Math.sin(pitch), (float)-Math.cos(heading));
		if (cameraToWorldTransformationMatrix != null) {
			Vector4f cubeVectorTransform = new Vector4f(
					vector.x, 
					vector.y, 
					vector.z, 
					0.0);
			cubeVectorTransform = Matrix4f.transform(cameraToWorldTransformationMatrix, cubeVectorTransform, null);
			vector.x = cubeVectorTransform.x;
			vector.y = cubeVectorTransform.y;
			vector.z = cubeVectorTransform.z;
		}
		return vector;

	}

}