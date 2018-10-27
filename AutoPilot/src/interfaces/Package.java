package interfaces;

import java.util.Comparator;

/**
 * A class of packages that are to be delivered.
 * 
 * @author	Team Saffier
 * @version	1.0
 */
public class Package {
	// TODO: Remove 'pickedUp' (equal to 'transporter')
	public boolean scheduling, pickedUp;
	public DroneAutopilot transporter, reserver;
	public int fromAirport, fromGate, toAirport, toGate;
	public String toString() {
		return "Package [(" + fromAirport + "/" + fromGate + ") -> (" + toAirport + "/" + toGate + ") - " + transporter + " (" + reserver +") ]";
	}
}

//Package Transport Combination
final class PTC implements Comparable<PTC> {
	public DroneAutopilot autopilot;
	public Package p;
	public double dist;
	public String toString() {
		return "PTC [" + autopilot + " " + p + " (dist = " + dist + ")";
	}
	public int compareTo(PTC other) {
		if (dist > other.dist) return 1;
		if (dist < other.dist) return -1;
		return 0;
	}
}

final class PTCComparator implements Comparator<PTC> {
	public int compare(PTC one, PTC two) {return (one.dist > two.dist ? 1 : (one.dist == two.dist ? 0 : -1));}
}