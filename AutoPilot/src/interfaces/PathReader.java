package interfaces;

/**
 * A class of path readers.
 * 
 * @author	Team Saffier
 * @version	1.0
 */
public class PathReader {
	
	/**
	 * Read the given byte array from the given stream.
	 * 
	 * @param 	stream
	 * 			The stream to read from.
	 * @return 	An array of bytes read from the given stream.
	 */
    @SuppressWarnings("unused")
    private static byte[] readByteArray(java.io.DataInputStream stream) throws java.io.IOException {
        int length = stream.readInt();
        byte[] array = new byte[length];
        stream.readFully(array);
        return array;
    }
    
    /**
	 * Read the given float array from the given stream.
	 * 
	 * @param 	stream
	 * 			The stream to read from.
	 * @return 	An array of floats read from the given stream.
	 */
    private static float[] readFloatArray(java.io.DataInputStream stream) throws java.io.IOException {
        int length = stream.readInt();
        float[] array = new float[length];
        for (int i = 0; i < length; i++) { array[i] = stream.readFloat(); }
        return array;
    }
    
    /**
	 * Read the given path from the given stream.
	 * 
	 * @param 	stream
	 * 			The stream to read from.
	 * @return 	A path read from the given stream.
	 */
    public static Path read(java.io.DataInputStream stream) throws java.io.IOException {
        final float[] x = readFloatArray(stream);
        final float[] y = readFloatArray(stream);
        final float[] z = readFloatArray(stream);
        return new Path() {
            public float[] getX() { return x; }
            public float[] getY() { return y; }
            public float[] getZ() { return z; }
        };
    }
    
}