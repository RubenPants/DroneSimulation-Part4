package autopilot_planning;

public enum DroneStage {
	TAKE_OFF, 			// Take off from airport to airspace
	FLY,				// Fly in the airspace
	FLY_ONE_CUBE,		// Fly and force not to use Dubins
	LAND, 				// Land from airspace onto an airport
	TAXI, 				// Taxi within one airport
	PHONE,				// Control drone using the phone
	FREE,				// Drone waits to receive an order whilst resting in an airport
	WAITING_TO_LAND		// Drone waits in airport stack (in airspace) until it got a permission to land
}
