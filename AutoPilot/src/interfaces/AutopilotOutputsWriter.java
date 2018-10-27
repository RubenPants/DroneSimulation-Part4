package interfaces;

/**
 * A class of autopilot outputs writers, to write autopilot outputs to an output stream.
 * 
 * @author	Team Saffier
 * @version	1.0
 */
public class AutopilotOutputsWriter {
	
	/**
	 * Write the given byte array to the given output stream.
	 * 
	 * @param 	stream
	 * 			The stream to write to.
	 * @param 	array
	 * 			The buffer to write to the output stream.
	 * @throws 	IOException
	 * 			If an I/O error occurs.
	 */
    @SuppressWarnings("unused")
	private static void writeByteArray(java.io.DataOutputStream stream, byte[] array) throws java.io.IOException {
        stream.writeInt(array.length);
        stream.write(array);
    }
    
    /**
	 * Write the given float array to the given output stream.
	 * 
	 * @param 	stream
	 * 			The stream to write to.
	 * @param 	array
	 * 			The buffer to write to the output stream.
	 * @throws 	IOException
	 * 			If an I/O error occurs.
	 */
    @SuppressWarnings("unused")
    private static void writeFloatArray(java.io.DataOutputStream stream, float[] array) throws java.io.IOException {
        stream.writeInt(array.length);
        for (float f : array) { stream.writeFloat(f); }
    }
    
    /**
     * Write the given autopilot outputs to the given output stream.
     * 
     * @param 	stream
	 * 			The stream to write to.
	 * @param 	value
	 * 			The autopilot outputs object to write to the output stream.
	 * @throws 	IOException
	 * 			If an I/O error occurs.
     */
    public static void write(java.io.DataOutputStream stream, AutopilotOutputs value) throws java.io.IOException {
    		stream.writeFloat(value.getThrust());
        stream.writeFloat(value.getLeftWingInclination());
        stream.writeFloat(value.getRightWingInclination());
        stream.writeFloat(value.getHorStabInclination());
        stream.writeFloat(value.getVerStabInclination());
        stream.writeFloat(value.getFrontBrakeForce());
        stream.writeFloat(value.getLeftBrakeForce());
        stream.writeFloat(value.getRightBrakeForce());
    }
    
}