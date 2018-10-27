package autopilot_utilities;

/**
 * A class representing a line in 2-dimensional Euclidian space, having a 'begin' and 'end' point.
 * 
 * @author Team Saffier
 * @version 1.0
 */
public class Line2D {

	/**
	 * Initialize a new line with given 'begin' and 'end' points.
	 * 
	 * @param  	startPoint
	 *		   	The start point for this new line.
	 * @param  	endPoint
	 *		   	The end point for this new line.
	 * @post   	The start point for this new line equals the
	 * 			given start point.
	 *       	| new.getStartPoint() == startPoint
	 * @post   	The end point for this new line equals the
	 * 			given end point.
	 *       	| new.getEndPoint() == endPoint
	 */
	public Line2D(Point2D startPoint, Point2D endPoint) {
		this.startPoint = startPoint;
		this.endPoint = endPoint;
		this.length = startPoint.distanceTo(endPoint);
	}
	
	/**
	 * Return the start point of this line.
	 */
	public Point2D getStartPoint() {
		return startPoint;
	}
	
	/**
	 * Variable registering the start point of this line.
	 */
	private Point2D startPoint;

	/**
	 * Return the end point of this line.
	 */
	public Point2D getEndPoint() {
		return endPoint;
	}
	
	/**
	 * Variable registering the end point of this line.
	 */
	private Point2D endPoint;
	
	/**
	 * Returns the length of this new line.
	 * 
	 * @return 	The length of this line.
	 * 			| return this.getStartPoint().distanceTo(this.getEndPoint());
	 */
	public Double length() {
		return length;
	}
	
	/**
	 * Variable registering the length of this line.
	 */
	private final double length;
	
	/**
	 * Rotate the given 2-dimensional line around its center.
	 * 
	 * @param 	line
	 * 			The line that is to be rotated.
	 * @param	theta
	 * 			The angle to rotate the line with.
	 * @return	A line, the given line but rotated.
	 */
	public static Line2D rotateAroundCenter(Line2D line, float theta) {
		
		// Pre-processing
		double cosT = Math.cos(theta), sinT = Math.sin(theta);
		Point2D startPoint = line.getStartPoint(), endPoint = line.getEndPoint();
		
		// Move to center
		float midX = (startPoint.getX() + endPoint.getX()) / 2;
		float midY = (startPoint.getY() + endPoint.getY()) / 2;
		float midSPX = startPoint.getX() - midX, midSPY = startPoint.getY() - midY;
		float midEPX = endPoint.getX() - midX, midEPY = endPoint.getY() - midY;
		
		// Rotate
		double rotatedSPX = cosT * midSPX - sinT * midSPY;
		double rotatedSPY = sinT * midSPX + cosT * midSPY;
		double rotatedEPX = cosT * midEPX - sinT * midEPY;
		double rotatedEPY = sinT * midEPX + cosT * midEPY;
		
		// Move back and return
		Point2D rotatedSP = new Point2D(rotatedSPX + midX, rotatedSPY + midY);
		Point2D rotatedEP = new Point2D(rotatedEPX + midX, rotatedEPY + midY);
		return new Line2D(rotatedSP, rotatedEP);
		
	}
	
	/**
	 * Returns a string representation of this line.
	 * 
	 * @return 	A string representing this line.
	 * 			| result == "[" + this.getStartPoint() + " -> " + this.getEndPoint() + "]"
	 */
	public String toString() {
		return "[" + this.getStartPoint() + " -> " + this.getEndPoint() + "]";
	}

}