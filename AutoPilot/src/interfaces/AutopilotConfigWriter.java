package interfaces;

/**
 * A class of autopilot configuration writers, to write autopilot configurations to an output stream.
 * 
 * @author	Team Saffier
 * @version	1.0
 */
public class AutopilotConfigWriter {
	
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
     * Write the given autopilot configuration to the given output stream.
     * 
     * @param 	stream
	 * 			The stream to write to.
	 * @param 	value
	 * 			The autopilot configuration to write to the output stream.
	 * @throws 	IOException
	 * 			If an I/O error occurs.
     */
    public static void write(java.io.DataOutputStream stream, AutopilotConfig value) throws java.io.IOException {
    	stream.writeUTF(value.getDroneID());
        stream.writeFloat(value.getGravity());
        stream.writeFloat(value.getWingX());
        stream.writeFloat(value.getTailSize());
        stream.writeFloat(value.getWheelY());
        stream.writeFloat(value.getFrontWheelZ());
        stream.writeFloat(value.getRearWheelZ());
        stream.writeFloat(value.getRearWheelX());
        stream.writeFloat(value.getTyreSlope());
        stream.writeFloat(value.getDampSlope());
        stream.writeFloat(value.getTyreRadius());
        stream.writeFloat(value.getRMax());
        stream.writeFloat(value.getFcMax());
        stream.writeFloat(value.getEngineMass());
        stream.writeFloat(value.getWingMass());
        stream.writeFloat(value.getTailMass());
        stream.writeFloat(value.getMaxThrust());
        stream.writeFloat(value.getMaxAOA());
        stream.writeFloat(value.getWingLiftSlope());
        stream.writeFloat(value.getHorStabLiftSlope());
        stream.writeFloat(value.getVerStabLiftSlope());
        stream.writeFloat(value.getHorizontalAngleOfView());
        stream.writeFloat(value.getVerticalAngleOfView());
        stream.writeInt(value.getNbColumns());
        stream.writeInt(value.getNbRows());
    }
    
}