package autopilot_planning_old;

import autopilot_utilities.Vector3f;

/**
 * A class of Dubins paths in 2 dimensions.
 *  Dubins paths are trajectories for given start/end velocity vectors and tangents with a constraint on
 *  maximum turning radius.
 * 
 * @author 	Team Saffier
 * @version 	1.0
 */
public class DubinsPath2D {

	/**
	 * Initialize this new 2-dimensional Dubins path with given start/end configuration and given
	 *  maximum turning angle.
	 *  
	 * @param 	startConfiguration
	 * 			A vector with 2D coordinates and rotation denoting the start configuration.
	 * @param 	endConfiguration
	 * 			A vector with 2D coordinates and rotation denoting the end configuration.
	 * @param 	rho
	 * 			The maximum turning angle for the path.
	 */
	public DubinsPath2D(Vector3f startConfiguration, Vector3f endConfiguration, double rho) {
		
		// Check if the given turning angle is valid
		if (rho < 0.0)
			return;
		
		// Pre-processing
	    double dx = endConfiguration.x - startConfiguration.x, dy = endConfiguration.y - startConfiguration.y;
	    double D = Math.sqrt(dx*dx + dy*dy);
	    double d = D / rho;
	    double theta = mod2pi(Math.atan2( dy, dx ));
	    double alpha = mod2pi(startConfiguration.z - theta);
	    double beta  = mod2pi(endConfiguration.z - theta);
	    
	    // Create path
	    this.startConfiguration = startConfiguration;
	    this.endConfiguration = endConfiguration;
	    this.rho = rho;
	    
	    // Get the best path by trying all 6 possibilities
	    // Logic could be put here so that performance increases a little.
	    for (int ordinal=0 ; ordinal<4 ; ordinal++)
	    		considerType(calculateType(alpha, 
	    				beta, d, DubinsPath2DType.DubinsPath2DTypeIdentifier.values()[ordinal]));
	    
	}
	
	/**
	 * Calculate the 2-dimensional Dubins path of the type with given identifier for given parameters.
	 */
	private DubinsPath2DType calculateType(double alpha, 
			double beta, double d, DubinsPath2DType.DubinsPath2DTypeIdentifier identifier) {
				
		// Pre-processing
		double sa = Math.sin(alpha), sb = Math.sin(beta);
	    double ca = Math.cos(alpha), cb = Math.cos(beta);
	    double c_ab = Math.cos(alpha - beta);
	    double t = 0.0, p = 0.0, q = 0.0, tmp0, tmp1, tmp2, p_squared;
		
	    // Calculate the Dubins path of type with given identifier
	    switch (identifier) {
	    case LSL:
	    		tmp0 = d+sa-sb;
		    p_squared = 2 + (d*d) -(2*c_ab) + (2*d*(sa - sb));
		    if (p_squared < 0) return null;
		    tmp1 = Math.atan2( (cb-ca), tmp0 );
		    t = mod2pi(-alpha + tmp1 );
		    p = Math.sqrt( p_squared );
		    q = mod2pi(beta - tmp1 );
	    		break;
	    case RSR:
	    		tmp0 = d-sa+sb;
	        p_squared = 2 + (d*d) -(2*c_ab) + (2*d*(sb-sa));
		    if (p_squared < 0) return null;
	        tmp1 = Math.atan2( (ca-cb), tmp0 );
	        t = mod2pi( alpha - tmp1 );
	        p = Math.sqrt( p_squared );
	        q = mod2pi( -beta + tmp1 );
	    		break;
	    case LSR:
	    		p_squared = -2 + (d*d) + (2*c_ab) + (2*d*(sa+sb));
			if (p_squared < 0) return null;
	        p = Math.sqrt( p_squared );
	        tmp2 = Math.atan2( (-ca-cb), (d+sa+sb) ) - Math.atan2(-2.0, p);
	        t = mod2pi(-alpha + tmp2);
	        q = mod2pi( -mod2pi(beta) + tmp2);
    			break;
	    case RSL:
	    		p_squared = (d*d) -2 + (2*c_ab) - (2*d*(sa+sb));
			if (p_squared < 0) return null;
	        p = Math.sqrt( p_squared );
	        tmp2 = Math.atan2( (ca+cb), (d-sa-sb) ) - Math.atan2(2.0, p);
	        t = mod2pi(alpha - tmp2);
	        q = mod2pi(beta - tmp2);
    			break;
	    case RLR:
	    		double tmp_rlr = (6. - d*d + 2*c_ab + 2*d*(sa-sb)) / 8.;
	        if (Math.abs(tmp_rlr) > 1) return null;
	        p = mod2pi(2*Math.PI - Math.acos( tmp_rlr ) );
	        t = mod2pi(alpha - Math.atan2( ca-cb, d-sa+sb ) + mod2pi(p/2.));
	        q = mod2pi(alpha - beta - t + mod2pi(p));
    			break;
	    case LRL:
	    		double tmp_lrl = (6. - d*d + 2*c_ab + 2*d*(- sa + sb)) / 8.;
		    if (Math.abs(tmp_lrl) > 1) return null;
	        p = mod2pi(2*Math.PI - Math.acos( tmp_lrl ) );
	        t = mod2pi(-alpha - Math.atan2( ca-cb, d+sa-sb ) + p/2.);
	        q = mod2pi(mod2pi(beta) - alpha -t + mod2pi(p));
    			break;
	    	default:
	    		break;
	    }
	    
	    return new DubinsPath2DType(identifier, t, p, q);
	    
	}

	/**
	 * Variable registering the cost of this 2-dimensional Dubins path.
	 */
	private double cost = Double.POSITIVE_INFINITY;
	
	/**
	 * Variable registering the maximum turning angle of this 2-dimensional Dubins path.
	 */
	private double rho;
	
	/**
	 * Variable registering the start configuration of this 2-dimensional Dubins path.
	 */
	@SuppressWarnings("unused")
	private Vector3f startConfiguration;
	
	/**
	 * Variable registering the end configuration of this 2-dimensional Dubins path.
	 */
	@SuppressWarnings("unused")
	private Vector3f endConfiguration;
	
	/**
	 * Consider the given calculated path type.
	 *  If the current cost of this Dubins path is larger than that of
	 *  the given one, then this path is set to the given type.
	 * 
	 * @param 	type
	 * 			The dubins path type to consider.
	 */
	public void considerType(DubinsPath2DType type) {
		if (type == null)
			return;
		double typeCost = type.getCost();
		if (typeCost < this.cost) {
			this.cost = typeCost;
			this.type = type;
		}
	}
	
	/**
	 * Variable registering the type of this Dubins path.
	 */
	public DubinsPath2DType type = null; // Type information
	
	/**
	 * Returns the length of this path.
	 */
	public double getLength() {
	    double length = this.type.getCost();
	    length = length * rho;
	    return length;
	}
	
	/**
	 * Convenience class for representing types of dubins paths.
	 */
	public static class DubinsPath2DType {
		
		/**
		 * Initialize this new Dubins path type with given identifier and given parameters.
		 */
		public DubinsPath2DType(DubinsPath2DTypeIdentifier identifier, double t, double p, double q) {
			this.identifier = identifier;
			params[0] = t;
			params[1] = p;
			params[2] = q;
		}
		
		/**
		 * Returns the identifier of this 2-dimensional Dubins path type.
		 */
		public DubinsPath2DTypeIdentifier getIdentifier() {
			return identifier;
		}
		
		/**
		 * Variable registering the identifier of this 2-dimensional Dubins path type.
		 */
		public DubinsPath2DTypeIdentifier identifier;
		
		/**
		 * Variable registering the parameters of this 2-dimensional Dubins path type.
		 */
		public double[] params = new double[3];
		
		/**
		 * Get the cost of this 2-dimensional Dubins path.
		 */
		public double getCost() {
			return params[0] + params[1] + params[2];
		}
		
		/**
		 * An enumeration with identifiers for types of 2-dimensional Dubins paths.
		 */
		public enum DubinsPath2DTypeIdentifier {
			LSL,	
			LSR,	
			RSL,	
			RSR,	
			RLR,	
			LRL;
		}
		
	}
	
	/**
	 * Floating point modulus suitable for rings.
	 */
	private double fmodr(double x, double y) {
	    return x - y * Math.floor(x/y);
	}

	/**
	 * Floating point modulus for 2*π.
	 */
	private double mod2pi(double theta) {
	    return fmodr(theta, 2 * Math.PI);
	}
	
	public static void main(String[] args) {
		long tic = System.nanoTime();
		DubinsPath2D path = new DubinsPath2D(new Vector3f(0.0f, 10.0f, 10.0f),
											new Vector3f(10.0f, 0.0f, 20.0f), 400.0f);
		System.out.println(System.nanoTime() - tic);
	}
	
}