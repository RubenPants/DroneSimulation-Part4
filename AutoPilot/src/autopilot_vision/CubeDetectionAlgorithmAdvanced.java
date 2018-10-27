package autopilot_vision;

import java.awt.Color;
import java.util.ArrayList;

import autopilot_library.AutopilotLibrary;
import autopilot_utilities.Point2D;
import autopilot_utilities.Rectangle2D;

/**
 * A class of cube detection algorithms using more advanced logic.
 * 
 * @author	Team Saffier
 * @version 	1.0
 * @note 	A Java version of the algorithm was made so that comparative speed-up can
 * 			be demonstrated. 
 */
public class CubeDetectionAlgorithmAdvanced extends CubeDetectionAlgorithm {
	
	/**
	 * Flag denoting whether or not the algorithm ignores colors that have already been seen in the past.
	 */
	private final static boolean USE_CACHE = true;
	
	@Override
	public ArrayList<Cube> locateUnitCubes(Image image, boolean hasGround) {
		
		// Pre-processing
		ArrayList<Cube> cubes = new ArrayList<Cube>();
		int width = image.getSize().getWidth(), height = image.getSize().getHeight();
	    	
		// Get 2D array with cube information from JNI
		// Array has [r,g,b - bounding box - edge coordinates] per row, one row for each cube
		byte[] pixels = image.getPixels();
		int[][] cubesArray = AutopilotLibrary.locateUnitCubes(pixels, width, height, USE_CACHE, hasGround);
		float hsv[] = new float[3];
		Rectangle2D boundingBox;
		for (int i=0 ; i<cubesArray.length ; i++) {
						
			if (cubesArray[i].length < 7)
				continue;
			
			// Create new cube
			Color.RGBtoHSB(cubesArray[i][0], cubesArray[i][1], cubesArray[i][2], hsv);
			boundingBox = new Rectangle2D(cubesArray[i][3], cubesArray[i][5], cubesArray[i][4], cubesArray[i][6]);
			Cube cube = new Cube(boundingBox, hsv[0], hsv[1]);
			
			// Register diagonals that were found 
			int idx = 0;
			for (int j=7 ; j+3 < cubesArray[i].length ; j+=4) {
				idx = (cubesArray[i][j] + width * cubesArray[i][j+1]) * 3;
				Color.RGBtoHSB((pixels[idx] & 0xff), (pixels[idx+1] & 0xff), (pixels[idx+2] & 0xff), hsv);
				CubeDiagonal diagonal = new CubeDiagonal(
											new Point2D(cubesArray[i][j], cubesArray[i][j+1]),
											new Point2D(cubesArray[i][j+2], cubesArray[i][j+3]),
											hsv[2]);
				// if (diagonal.isFromVerticalSide())
					// System.out.println(diagonal);
				cube.addDiagonal(diagonal);
			}
			
			// Add cube
			cubes.add(cube);
			
		}
				
		return cubes;
		
	}
		
	@Override
	public Cube locateUnitCube(Image image, float hue, float saturation) {
		
		// TODO : Unnecessary as assignment has gotten more complex than this
		return null;
		
	}
	
}