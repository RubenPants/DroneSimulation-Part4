package control;

import entities.Drone;
import interfaces.AutopilotModule;
import interfaces.AutopilotOutputs;
import tools.Tools;

public class DroneThread implements Runnable {
	
	private Drone drone;
	private AutopilotModule module;
	private float timePassed;
	private float totalTime;
	private int iterations;
	private byte[] image = new byte[0];
	
	public DroneThread(Drone drone, AutopilotModule module, float timePassed, float totalTime, int iterations){
		this.drone = drone;
		this.module = module;
		this.timePassed = timePassed;
		this.totalTime = totalTime;
		this.iterations = iterations;
	}

	@Override
	public void run() {
		module.startTimeHasPassed(drone.getID(), Tools.getAutopilotInputs(drone, totalTime, image));
		AutopilotOutputs outputs = module.completeTimeHasPassed(drone.getID());
		drone.setInputs(outputs);
		for(int i = 0;i<iterations;i++){
			drone.timePassed(timePassed/iterations);
		}
	}
}
