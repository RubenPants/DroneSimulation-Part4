package interfaces;

/**
 * A class of autopilot outputs readers, to read autopilot outputs from a stream.
 * 
 * @author	Team Saffier
 * @version	1.0
 */
public class AutopilotOutputsReader {
	
	/**
	 * Read floats from an input stream and store them in a buffer array.
	 * 
	 * @param 	stream
	 * 			The input stream to read from.
	 * @return	A buffer with the contents of the given input stream.
	 * @throws 	IOException
	 * 			If an I/O error occurs.
	 */
    @SuppressWarnings("unused")
	private static byte[] readByteArray(java.io.DataInputStream stream) throws java.io.IOException {
        int length = stream.readInt();
        byte[] array = new byte[length];
        stream.readFully(array);
        return array;
    }
    
    /**
	 * Read bytes from an input stream and store them in a buffer array.
	 * 
	 * @param 	stream
	 * 			The input stream to read from.
	 * @return	A buffer with the contents of the given input stream.
	 * @throws 	IOException
	 * 			If an I/O error occurs.
	 */
    @SuppressWarnings("unused")
    private static float[] readFloatArray(java.io.DataInputStream stream) throws java.io.IOException {
        int length = stream.readInt();
        float[] array = new float[length];
        for (int i = 0; i < length; i++) { array[i] = stream.readFloat(); }
        return array;
    }
    
    /**
     * Read bytes from an input stream and convert them to autopilot outputs.
     * 
     * @param 	stream
     * 			The input stream to read from.
     * @return	An autopilot outputs object having values corresponding to the given input stream.
     * @throws 	IOException
     * 			If an I/O error occurs.
     */
    public static AutopilotOutputs read(java.io.DataInputStream stream) throws java.io.IOException {
    	
    		// Read values
    		final float thrust = stream.readFloat();
        final float leftWingInclination = stream.readFloat();
        final float rightWingInclination = stream.readFloat();
        final float horStabInclination = stream.readFloat();
        final float verStabInclination = stream.readFloat();
        final float frontBrakeForce = stream.readFloat();
        final float leftBrakeForce = stream.readFloat();
        final float rightBrakeForce = stream.readFloat();
        
        // Create autopilot outputs object
        return new AutopilotOutputs() {
            public float getThrust() { return thrust; }
            public float getLeftWingInclination() { return leftWingInclination; }
            public float getRightWingInclination() { return rightWingInclination; }
            public float getHorStabInclination() { return horStabInclination; }
            public float getVerStabInclination() { return verStabInclination; }
            public float getFrontBrakeForce() { return frontBrakeForce; }
            public float getLeftBrakeForce() { return leftBrakeForce; }
            public float getRightBrakeForce() { return rightBrakeForce; }
        };

    }
    
}