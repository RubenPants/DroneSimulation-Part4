package autopilot_vision;

import java.io.File;
import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;

import autopilot_vision.CubeDetectionAlgorithm.CubeDetectionAlgorithmType;
import interfaces.AutopilotConfig;
import interfaces.AutopilotInputs;
import junit.framework.TestCase;

/**
 * A class for testing distance calculation.
 * 
 * @author	Team Saffier
 * @version	1.0
 */
public class DistanceTests extends TestCase {

	/**
	 * Variable registering the URIs of test images for this test case.
	 */
	private ArrayList<String> testImages;

	/**
	 * Prepare the unit tests.
	 */
	@Before
	protected void setUp() throws Exception {

		super.setUp();

		AutopilotImageAnalyser.ENABLE_LOGGING = false;

		// Initialise test image paths
		// ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		this.testImages = new ArrayList<String>();
		File folder = new File("Images/full_test/roll_0_pitch_0/");
		File[] listOfFiles = folder.listFiles();
		for (int i = 0; i < listOfFiles.length; i++)
			if (listOfFiles[i].isFile() && listOfFiles[i].getName().startsWith("heading"))
				this.testImages.add("Images/full_test/roll_0_pitch_0/" + listOfFiles[i].getName());

	}

	/**
	 * Test the distance calculations with some basic input images.
	 */
	@Test
	public void testDistanceCalculation_Basic() {

		CubeDetectionAlgorithm algorithm = CubeDetectionAlgorithm.initializeAlgorithm(CubeDetectionAlgorithmType.ADVANCED);
		// AutopilotImageAnalyser.ENABLE_LOGGING = true;

		for (String filePath : this.testImages) {

			System.out.println("--- " + filePath + " ---");

			// long tic = System.nanoTime();
			File imageFile = new File(filePath);
			Image image = Image.createImageUsingFile(imageFile);
			String[] parts = imageFile.getName().replaceFirst("[.][^.]+$", "").split("\\_");
			if (image == null || parts.length != 2)
				continue;
			
			// Get heading from filename
			try {
				
				final double heading = (double)Integer.parseInt(parts[1]);
				// System.out.println((float)Math.toRadians(heading));
				
				// Inputs for test
				AutopilotInputs inputs = new AutopilotInputs(){
					public float getElapsedTime() {return 0.0f;}
					public float getHeading() {return (float)Math.toRadians(heading);}
					public byte[] getImage() {return image.getPixels();}
					public float getPitch() {return (float)Math.toRadians(0.0f);}
					public float getRoll() {return (float)Math.toRadians(0.0f);}
					public float getX() {return 0.0f;}
					public float getY() {return 30.0f;}
					public float getZ() {return 0.0f;}
				};

				long tic = System.nanoTime();
				ArrayList<Cube> cubes = algorithm.locateUnitCubes(image, true);
				for (Cube cube : cubes) {
					if (AutopilotImageAnalyser.updateCubeLocation(cube, configuration, inputs));
						System.out.println("Cube found : " + cube);
					// cube.printVerticalDiagonals();
				}
				System.out.println("Performance locating cubes : " + ((System.nanoTime() - tic) / Math.pow(10, 6)) + "ms");
				
			} catch (Exception e) {}

		}

	}

	// Configuration for test
	AutopilotConfig configuration = new AutopilotConfig() {
		public String getDroneID() {return null;}
		public float getWheelY() {return 0;}
		public float getFrontWheelZ() {return 0;}
		public float getRearWheelZ() {return 0;}
		public float getRearWheelX() {return 0;}
		public float getTyreSlope() {return 0;}
		public float getDampSlope() {return 0;}
		public float getTyreRadius() {return 0;}
		public float getRMax() {return 0;}
		public float getFcMax() {return 0;}
		public float getGravity() {return 0;}
		public float getWingX() {return 0;}
		public float getTailSize() {return 0;}
		public float getEngineMass() {return 0;}
		public float getWingMass() {return 0;}
		public float getTailMass() {return 0;}
		public float getMaxThrust() {return 0;}
		public float getMaxAOA() {return 0;}
		public float getWingLiftSlope() {return 0;}
		public float getHorStabLiftSlope() {return 0;}
		public float getVerStabLiftSlope() {return 0;}
		public float getHorizontalAngleOfView() {return (float)(2*Math.PI/3);}
		public float getVerticalAngleOfView() {return (float)(2*Math.PI/3);}
		public int getNbColumns() {return 200;}
		public int getNbRows() {return 200;}
	};

}