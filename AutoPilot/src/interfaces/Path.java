package interfaces;

/**
 * A class of paths of cubes in 3D space.
 * 
 * @author	Team Saffier
 * @version	1.0
 */
public interface Path {
	
	/**
	 * X coordinates of the waypoints on this path.
	 * 
	 * @return 	An array of floats, the x coordinates of the waypoints
	 * 			part of this path.
	 */
    float[] getX();
    
    /**
	 * Y coordinates of the waypoints on this path.
	 * 
	 * @return 	An array of floats, the y coordinates of the waypoints
	 * 			part of this path.
	 */
    float[] getY();
    
    /**
	 * Z coordinates of the waypoints on this path.
	 * 
	 * @return 	An array of floats, the z coordinates of the waypoints
	 * 			part of this path.
	 */
    float[] getZ();
    
}