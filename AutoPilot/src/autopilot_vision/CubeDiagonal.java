package autopilot_vision;

import autopilot_utilities.Line2D;
import autopilot_utilities.Point2D;

/**
 * A class of cube diagonals, diagonals of a cube's side.
 * 
 * @author	Team Saffier
 * @version	1.0
 */
public class CubeDiagonal extends Line2D {

	/**
	 * Initialize this new diagonal with given start- and endpoint, and given RGB values.
	 */
	public CubeDiagonal(Point2D startPoint, Point2D endPoint, float brightness) {
		super(startPoint, endPoint);
		this.brightness = brightness;
	}
	
	/**
	 * Returns the brightness of this cube diagonal.
	 */
	public float getBrightness() {
		return this.brightness;
	}
	
	/**
	 * Returns the rounded brightness of this cube diagonal (to identify what side it is on).
	 */
	public float getBrightnessRounded() {
		return 5 * (Math.round(this.brightness*100/5));
	}
	
	/**
	 * Returns whether or not this diagonal is part of a vertical side of the cube.
	 * 
	 * @return True if and only if this diagonal's brightness matches that of a vertical side.
	 */
	public boolean isFromVerticalSide() {
		return (this.getBrightnessRounded() != 45.0f && this.getBrightnessRounded() != 20.0f);
	}
	
	/**
	 * The brightness of this cube diagonal.
	 */
	private float brightness;
	
	/**
	 * Checks whether this diagonal is pointing downwards if the given diagonal is its complementary diagonal.
	 * 
	 * @param	other
	 * 			This diagonal's complementary diagonal.
	 * @param	roll
	 * 			The roll of the camera looking at the diagonal.
	 * @return	True if and only if this diagonal is the downwards diagonal for the side.
	 */
	public boolean isDownwards(CubeDiagonal other, float roll) {
		Point2D minX = (this.getStartPoint().getX() < this.getEndPoint().getX() ? this.getStartPoint() : this.getEndPoint());
		Point2D minXOther = (other.getStartPoint().getX() < other.getEndPoint().getX() ? other.getStartPoint() : other.getEndPoint());
		if (Math.abs(roll) > 0.1f) {
			if (minX.getX() < 100 && minXOther.getX() < 100)
				roll = -roll;
			//System.out.println(this);
			//System.out.println(other);
			Line2D rotatedLine = CubeDiagonal.rotateAroundCenter(this, roll);
			Line2D rotatedLineOther = CubeDiagonal.rotateAroundCenter(other, roll);
			//System.out.println(rotatedLine);
			//System.out.println(rotatedLineOther);
			minX = (rotatedLine.getStartPoint().getX() < rotatedLine.getEndPoint().getX() ? rotatedLine.getStartPoint() : rotatedLine.getEndPoint());
			minXOther = (rotatedLineOther.getStartPoint().getX() < rotatedLineOther.getEndPoint().getX() ? rotatedLineOther.getStartPoint() : rotatedLineOther.getEndPoint());
		}
		return (minX.getY() > minXOther.getY());
	}
	
}