package control;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import javax.management.RuntimeErrorException;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

import entities.Airport;
import entities.Camera;
import entities.Drone;
import entities.Entity;
import entities.Package;
import entities.PackageKey;
import entities.Symbol;
import guis.GuiTexture;
import interfaces.AutopilotModule;
import interfaces.AutopilotOutputs;
import tools.Tools;
import worldSimulation.DroneStartSettings;

public class WorldManager {
	
	private boolean ENABLE_LOGGING = false;

	private ArrayList<Drone> drones = new ArrayList<>();
	private ArrayList<Thread> threads = new ArrayList<>();
	private AutopilotModule module;
	
	private ArrayList<Airport> airports = new ArrayList<>();
	private ArrayList<Airport> occupiedAirports = new ArrayList<>();
	private ArrayList<Airport> freeAirports = new ArrayList<>();

	private ArrayList<Symbol> symbols = new ArrayList<Symbol>();
	public ArrayList<Symbol> getSymbols() {
		return symbols;
	}
	public void addSymbol(Symbol symbol) {
		symbols.add(symbol);
	}
	
	File file = new File("lastRun.txt");
	
	private ArrayList<Package> packages = new ArrayList<>();
	
	private ArrayList<Package> queue = new ArrayList<>();
	
	/**
	 * Map registering packages which is waitig for it's deliver.
	 */
	public Map<PackageKey, Package> waitingPackages = new HashMap<PackageKey, Package>();
	
	/**
	 * Map registering packages which is currently delivered by drones.
	 * Read this map when you want to know about those packages
	 * 
	 * Q. Why is the key of this hashmap "droneID" and not "airport/gate number"?
	 * A. The restriction is "Er zijn nooit 2 pakketten tegelijk beschikbaar aan dezelfde vertrekgate."
	 *    So two packages can have the same PackageKey when delivered.
	 */
	public Map<Integer, Package> deliveringPackages = new HashMap<Integer, Package>();
	
	/**
	 * ArrayList registering packages which are delivered. (Can be used as record)
	 */
	public ArrayList<Package> deliveredPackages = new ArrayList<Package>();
	
	private CrashHandler handler;
	
	public WorldManager(CrashHandler handler) {
		this.handler = handler;
	}
	
	public void setAutopilotModule(AutopilotModule module){
		this.module = module;
	}
	
	public ArrayList<Drone> getDrones(){
		return drones;
	}
	
	public Drone getDrone(int index){
		return drones.get(index);
	}
	
	public void addDrone(Drone drone){
		drone.setID(drones.size());
		drones.add(drone);
	}
	
	public void resetDrone(int drone, DroneStartSettings settings){
		drones.get(drone).reset(settings);
	}
	
	public void defineDrones(){
		for(Drone drone: drones){
			module.defineDrone(drone.getAirport().getId(), drone.getGate(), (drone.getGate()+1)%2, drone.getConfigs());
			//module.defineDrone(0, 0, 0, drone.getConfigs());			
		}
	}
	
	public boolean toggleAutomaticAdder() {
		automaticAdder = !automaticAdder;
		return automaticAdder;
	}
	private boolean automaticAdder = false;
	
	public void allDronesTimePassed(float timePassed, float simulationTime, int iterations){
		if (automaticAdder) {
			if (this.packages.size() - this.deliveredPackages.size() < this.getDrones().size())
				for (int i=0 ; i<ThreadLocalRandom.current().nextInt(1, 10) ; i++)
					this.createRandomRequest();
		}
		Thread.UncaughtExceptionHandler h = new Thread.UncaughtExceptionHandler() {
		    public void uncaughtException(Thread th, Throwable ex) {
		    	handler.handleCrash(ex.getMessage());
		    	ex.printStackTrace();
		    }
		};
//		for(int i=0;i<drones.size();i++){
//			Thread thread = (new Thread(new DroneThread(drones.get(i), module, timePassed, simulationTime, iterations)));
//			threads.add(thread);
//			thread.setUncaughtExceptionHandler(h);
//			thread.start();
//		}
		for(Drone drone: getDrones()) {
			module.startTimeHasPassed(drone.getID(), Tools.getAutopilotInputs(drone, simulationTime, new byte[0]));
			AutopilotOutputs outputs = module.completeTimeHasPassed(drone.getID());
			drone.setInputs(outputs);
			for(int i = 0;i<iterations;i++){
				drone.timePassed(timePassed/iterations);
			}
		}
	}
	
	public void endAllDronesTimePassed(){
		for(Thread thread: threads){
			try {
				thread.join();
			} catch (InterruptedException e) {
				System.out.println("Thread interrupted");
			}
		}
		threads.clear();
	}
	
	public void reset(DroneStartSettings settings){
		for(Drone drone: drones){
			drone.reset(settings);
		}
	}
	
	public Camera getDefaultCamera(){
		return new Camera(drones.get(0));
	}

	public ArrayList<Airport> getOccupiedAirports() {
		return occupiedAirports;
	}

	public ArrayList<Airport> getFreeAirports() {
		return freeAirports;
	}
	
	public Airport generateRandomAirport(float minDistance) {
		boolean found = false;
		Vector2f position = new Vector2f();
		while(!found) {
			float x = (float)Math.random()*20000;
			float z = (float)-Math.random()*20000;
			position = new Vector2f(x,z);
			for(Airport airport: getAirports()) {
				Vector2f distance = Tools.subtract(airport.getPosition2D(), position);
				if(distance.lengthSquared() > minDistance*minDistance) {
					found = true;
				} else {
					found = false;
					break;
				}
			}
		}
		return new Airport(position, (float)(Math.random()*2*Math.PI));
	}
	
	public void addAirport(Airport airport){
		airport.setId(airports.size());
		airports.add(airport);
		freeAirports.add(airport);
	}
	
	public Airport getAirport(int i){
		return airports.get(i);
	}
	
	public ArrayList<Airport> getAirports() {
		return airports;
	}

	public void occupyPort(Airport airport){
		if(airport.isAirportOccupied()) {
			freeAirports.remove(airport);
			occupiedAirports.add(airport);
		}
	}
	
	public void unoccupyPort(Airport airport){
		if(!airport.isAirportOccupied()) {
			freeAirports.add(airport);
			occupiedAirports.remove(airport);
		}
	}
	
	public void addDroneToAirport(Drone drone, Airport port, int gate) {
		if(gate==1) {
			port.setDroneGate1(drone);
		} else {
			port.setDroneGate0(drone);
		}
		occupyPort(port);
	}
	
	public void clearAll() {
		drones.clear();
		airports.clear();
		freeAirports.clear();
		occupiedAirports.clear();
	}
	
	public void removeDroneFromAirport(Drone drone) {
		Airport port = drone.getAirport();
		if(port==null) return;
		int gate = drone.getGate();
		if(gate==1) {
			port.setDroneGate1(null);
		} else {
			port.setDroneGate0(null);
		}
		unoccupyPort(port);
	}
	
	public ArrayList<Package> getPackages(){
		return this.packages;
	}
	
	/**
	 * Adds package to packagManager.
	 * @param pack
	 */
	public void addNewPackage(Package pack){
		if(pack.fromA >= airports.size() || pack.toA >= airports.size()) {
			throw new IllegalArgumentException("One of the given airports doesn't exist");
		}
		if (checkNoDuplication(pack) && canHavePackageAt(pack.fromA, pack.fromG)){
			this.waitingPackages.put(new PackageKey(pack.fromA,pack.fromG), pack);
			this.packages.add(pack);
			this.queue.add(pack);			
			
			//Output voor eventuele log, mag weg als niemand het nodig vindt.
	        if(file.exists()){
	        	try {
					FileWriter fw = new FileWriter(file);
					fw.write(pack.fromA + " " + pack.fromG + " " + pack.toA + " " + pack.toG + "\r\n");
				} catch (IOException e) {
					e.printStackTrace();
				}
	        }
		} else
			if (ENABLE_LOGGING) System.out.println("No don't do that");
	}
	
	/**
	 * takes a package out of waitingPackages and stores it to deliveringPackages.
	 * 
	 * @param airNum
	 * @param gateNum
	 * @param droneID
	 */
	public boolean startDeliveringPackage(int airNum, int gateNum, Drone drone) {
		if(drone.isHasPackage()) return false;
		Package pack = this.waitingPackages.remove(new PackageKey(airNum,gateNum));
		if(pack == null) return false;
		pack.startCarry(drone);
		this.deliveringPackages.put(drone.getID(), pack);
		if(ENABLE_LOGGING) System.out.println("PICKED UP PACKAGE");
		return true;
	}
	
	/**
	 * Ends delivering if the package's destination is current airport (and gate).
	 * 
	 * @param airNum
	 * @param gateNum
	 * @param droneID
	 */
	public boolean endDeliver(int airNum, int gateNum, Drone drone){
		Package pack = this.deliveringPackages.get(drone.getID());
		if(pack == null) return false;
		if(pack.toA == airNum && pack.toG == gateNum){
			Package endedPackage = this.deliveringPackages.remove(drone.getID());
			this.deliveredPackages.add(endedPackage);
			if(ENABLE_LOGGING) System.out.println("PACKAGE DELIVERED");
			pack.deliver();
			return true;
		} else {
			if (!(pack.fromA == airNum && pack.fromG == gateNum) && ENABLE_LOGGING)
				System.out.println("ERROR: delivered on false position, or a false call of endDeliver");
			return false;
		}
	}
	
	/**
	 * Avoids the existence of two packages at the same airport, same gate.
	 * @param pack
	 * @return
	 */
	public boolean checkNoDuplication(Package pack){
		if (pack == null)
			return false;
		else if (!this.waitingPackages.containsKey(new PackageKey(pack.fromA,pack.fromG)))
			return true;
		else 
			return false;
	}
	

	/**
	 * Avoids the existence of two packages at the same airport, same gate.
	 * @param pack
	 * @return
	 */
	public boolean isDuplicate(int fromA, int fromG){
		return this.waitingPackages.containsKey(new PackageKey(fromA,fromG));
	}

	
	//Extra's
	//Extra's
	public void createRandomRequest(){
		//world.getNumberOfAirport (iets om aantal airports te halen)
		int amountOfAirport = airports.size();
		if (this.waitingPackages.size() == airports.size() * 2)
			return;
		Package pack = null;
		do {
			pack = requestRandomPackage(amountOfAirport);
		}
		while (isDuplicate(pack.fromA, pack.fromG) || (pack.fromA == pack.toA));
		addNewPackage(pack);
	}
	
	public Package requestRandomPackage(int amountOfAirport){
		int departAirport = new Random().nextInt(amountOfAirport);
		int departGate = new Random().nextInt(2);
		
		while(isDuplicate(departAirport, departGate)){
			if(departGate == 0)
				departGate = 1;
			else if (departAirport < amountOfAirport - 1){
				departAirport += 1;
				departGate = 0;
			}
			else{
				departAirport = 0;
				departGate = 0;
			}
		}
		
		int arriveAirport = new Random().nextInt(amountOfAirport);
		int arriveGate = new Random().nextInt(2);
		
		Package packet = new Package(departAirport,departGate,arriveAirport,arriveGate);
		
		return packet;
	}
	
	
	public boolean canHavePackageAt(int airport, int gate){
		for(Drone drone: getDrones()){
			Airport port = getAirport(airport);
			int droneGate = port.isInGate(drone);
			if(droneGate == gate) {
				if(drone.getVelocity().length() > 0.01)
					return false;
			}
			/*
			if(drone.getVelocity().length() > 0.5 && drone.getVelocity().length() < 1.5){
				Airport port = getAirport(airport);
				int droneGate = port.isInGate(drone);
				if(droneGate == gate) return true;
			}
			*/
		}
		return true;
	}
	
	//Extra
    /**
     * calculates progress for all existing packages.
     * @return
     */
    public Map<Drone, Double> calculateProgress(){
    	Map<Drone, Double> deliverProgress = new HashMap<Drone, Double>();
        double[] result = new double[deliveringPackages.size()];
        for (int i=0; i < deliveringPackages.size(); i++){
            Package pack = deliveringPackages.get(i);
            if(!pack.carried){
                result[i] = 0.0;
            }
            else{//pack is carried by somebody
                Vector2f startPos = airports.get(pack.fromA).getPosition2D();
                Vector2f endPos = airports.get(pack.toA).getPosition2D();
                Vector3f curPos = pack.carriedBy.getPosition();
                double fullDistance = Math.sqrt(Math.pow(startPos.x - endPos.x,2) + Math.pow(startPos.y - endPos.y,2));
                double currentDistance = Math.sqrt(Math.pow(curPos.x - endPos.x,2) + Math.pow(curPos.z - endPos.y,2)); // y-coordinate is z in 3d
                if(fullDistance != 0)
                	deliverProgress.put(pack.carriedBy, 1.0-(currentDistance/fullDistance));
                else{
                	deliverProgress.put(pack.carriedBy, 0.0);
                	if (ENABLE_LOGGING) System.out.println("zero full distance (called in worldManager)");
                }
            }
            
        }
        return deliverProgress;
    }
    
    public int calculateProgressSingle(Package pack){
    	double rest = 0.0;
    	
        Vector2f startPos = airports.get(pack.fromA).getPosition2D();
        Vector2f endPos = airports.get(pack.toA).getPosition2D();
        Vector3f curPos = pack.carriedBy.getPosition();
        double fullDistance = Math.sqrt(Math.pow(startPos.x - endPos.x,2) + Math.pow(startPos.y - endPos.y,2));
        double currentDistance = Math.sqrt(Math.pow(curPos.x - endPos.x,2) + Math.pow(curPos.z - endPos.y,2)); // y-coordinate is z in 3d
        if(fullDistance != 0)
        	rest = 1.0 - (currentDistance/fullDistance);
        else{
        	rest = 0.0;
        	if (ENABLE_LOGGING) System.out.println("zero full distance (called in worldManager)");
        }
        if (rest < 0)
        	rest = 0;
    	rest = rest*100;
    	
    	return (int)rest;
    }
	
	
	public boolean checkDronesForPackagePickup() {
		for(Drone drone: drones) {
			if(drone.getVelocity().length() < 1) {
				// System.out.println(drone.getVelocity());
				for(Airport port: getAirports()) {
					int gate = port.isInGate(drone);
					if(gate != -1) {
						boolean end = endDeliver(port.getId(), gate, drone);
						boolean start = startDeliveringPackage(port.getId(), gate, drone);
						if(end||start) return true;
					}
				}
			}
		}
		return false;
	}
	
	public void checkForCollision(){
		for(Drone drone1: getDrones()){
			for(Drone drone2: getDrones()){
				if(!drone1.equals(drone2)){
					Vector3f distance = Tools.subtract(drone1.getPosition(), drone2.getPosition());
					if(distance.lengthSquared()<25.0){
						throw new RuntimeException(drone1 + " and " + drone2 + " CRASHED!");
					}
				}
			}
		}
	}
	
	public ArrayList<GuiTexture> getPackageGuis(float width){
		ArrayList<GuiTexture> guis = new ArrayList<>();
		for(Package pack: packages){
			if(pack.isWaiting()){
				Vector3f position = airports.get(pack.getFromA()).getPosition3D();
				guis.add(new GuiTexture(pack.getGuiTexture(), new Vector2f(2*position.x/width-1 + 0.05f, 2*-position.z/width-1 + 0.05f),
					0, new Vector2f(0.05f, 0.05f)));
			}	
		}
		return guis;
	}
	
	public boolean emptyQueue(){
		boolean flag = false;
		for(Package pack: queue){
			module.deliverPackage(pack.fromA, pack.fromG, pack.toA, pack.toG);
			flag = true;
		}
		queue.clear();
		return flag;
	}

	public Map<PackageKey, Package> getWaitingPackages() {
		// TODO Auto-generated method stub
		return waitingPackages;
	}
	
	public void reset() {
		for(Drone drone: getDrones()) {
			drone.setHasPackage(false);
			if(drone.getGate()==0) {
				drone.getAirport().setDroneGate0(drone);
			} else {
				drone.getAirport().setDroneGate1(drone);
			}
		} 
		queue.clear();
		packages.clear();
		waitingPackages.clear();
		deliveredPackages.clear();
		deliveringPackages.clear();
		module.simulationEnded();
	}
}
