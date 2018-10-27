package interfaces;

/**
 * A class of path writers.
 * 
 * @author	Team Saffier
 * @version	1.0
 */
public class PathWriter {
	
	/**
	 * Write the given byte array to the given stream.
	 * 
	 * @param 	stream
	 * 			The stream to write to.
	 * @param 	array
	 * 			The byte array to write.
	 */
    @SuppressWarnings("unused")
	private static void writeByteArray(java.io.DataOutputStream stream, byte[] array) throws java.io.IOException {
        stream.writeInt(array.length);
        stream.write(array);
    }
    
    /**
	 * Write the given float array to the given stream.
	 * 
	 * @param 	stream
	 * 			The stream to write to.
	 * @param 	array
	 * 			The float array to write.
	 */
    private static void writeFloatArray(java.io.DataOutputStream stream, float[] array) throws java.io.IOException {
        stream.writeInt(array.length);
        for (float f : array) { stream.writeFloat(f); }
    }
    
    /**
	 * Write the given path to the given stream.
	 * 
	 * @param 	stream
	 * 			The stream to write to.
	 * @param 	value
	 * 			The path to write.
	 */
    public static void write(java.io.DataOutputStream stream, Path value) throws java.io.IOException {
        writeFloatArray(stream, value.getX());
        writeFloatArray(stream, value.getY());
        writeFloatArray(stream, value.getZ());
    }
    
}