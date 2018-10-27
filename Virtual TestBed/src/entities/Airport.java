package entities;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

import rendering.Loader;
import shaders.ModelTexture;
import tools.Tools;
import worldSimulation.DroneStartSettings;

public class Airport {
	private static float w = 250;
	private static float l = 70;
	
	private Vector2f position;
	private float rotation;
	
	private Matrix4f toAirport;
	private Matrix4f toWorld;
	
	private static TexturedModel tModel;
	
	private Entity entity;
	
	private Drone droneGate0;
	private Drone droneGate1;
	
	private final float HEIGHT = -4.0f;
	//private static final float HEIGHT = 0.05f;
	
	private int id;
	private String name = "";
	
	public Airport(Vector2f position, float rotation){
		this.position = position;
		this.rotation = rotation;
		this.toWorld = Tools.createTransformationMatrix(getPosition3D(), getRotation(), 0, 0, 1);
		this.toAirport = Matrix4f.invert(toWorld, null);
		this.entity = new Entity(tModel, new Vector3f(position.x, HEIGHT, position.y), rotation, 0, 0, 35);
	}
	
	public static void defineAirportParameters(float length, float width) {
		w = width;
		l = length;
	}
	
	public void modify(Vector2f position, float rotation){
		this.position = position;
		this.rotation = rotation;
		this.toWorld = Tools.createTransformationMatrix(getPosition3D(), getRotation(), 0, 0, 1);
		this.toAirport = Matrix4f.invert(toWorld, null);
		this.entity = new Entity(tModel, new Vector3f(position.x, HEIGHT, position.y), rotation, 0, 0, 35);
		setDroneGate0(droneGate0);
		setDroneGate1(droneGate1);
	}
	
	public Vector2f getPosition2D(){
		return this.position;
	}
	
	public Vector3f getPosition3D(){
		return new Vector3f(position.x, 0, position.y);
	}
	
	public float getRotation(){
		return this.rotation;
	}
	
	public Vector3f getRelativeCoord(Vector3f position){
		return Tools.transformVector(this.toAirport, position);
	}
	
	public Vector3f getAbsolulateCoordinate(Vector3f relativeCoord) {
		return Tools.transformVector(this.toWorld, relativeCoord);
	}
	
	public Vector2f getAbsolulateCoordinate(Vector2f relativeCoord) {
		Vector3f vector3 = Tools.transformVector(this.toWorld, new Vector3f(relativeCoord.x, 0, relativeCoord.y));
		return new Vector2f(vector3.x, vector3.z);
	}
	
	public boolean droneInAirport(Drone drone){
		Vector3f relDronePos = getRelativeCoord(drone.getPosition());
		return Math.abs(relDronePos.x)<w/2 && Math.abs(relDronePos.z)<l/2;
		
	}

	public Entity getEntity(){
		return entity;
	}
	
	public static void setModel(Model model, ModelTexture texture) {
		tModel = new TexturedModel(model.getVaoID(), model.getVertexCount(), texture);
	}
	
	public Vector2f getCenterRunway0Relative() {
		return (new Vector2f(-(float)Math.cos(rotation), (float)Math.sin(rotation))).normalise(null);
	}

	public Vector3f getPlaneSpawnGate0(float droneHeight) {
		Vector2f relSpawn = new Vector2f(0, -w+35);
		Vector2f groundCoord = getAbsolulateCoordinate(relSpawn);
		return new Vector3f(groundCoord.x, droneHeight, groundCoord.y);
	}
	
	public Vector3f getPlaneSpawnGate1(float droneHeight) {
		Vector2f relSpawn = new Vector2f(0, w-35);
		Vector2f groundCoord = getAbsolulateCoordinate(relSpawn);
		return new Vector3f(groundCoord.x, droneHeight, groundCoord.y);
	}

	public boolean isGateZeroOccupied() {
		return droneGate0!=null;
	}

	public void setDroneGate0(Drone drone) {
		droneGate0 = drone;
		if(drone==null) return;
		drone.setAirport(this);
		drone.setGate(0);
		DroneStartSettings settings = new DroneStartSettings();
		settings.setPosition(this.getPlaneSpawnGate0(-drone.getConfigs().getWheelY()+0.2f));
		settings.setHeading(this.getRotation()+(float)Math.PI/2);
		drone.reset(settings);
	}

	public boolean isGateOneOccupied() {
		return droneGate1!=null;
	}
	
	public void setDroneGate1(Drone drone) {
		droneGate1 = drone;
		if(drone==null) return;
		drone.setAirport(this);
		drone.setGate(1);
		DroneStartSettings settings = new DroneStartSettings();
		settings.setPosition(this.getPlaneSpawnGate1(-drone.getConfigs().getWheelY()+0.2f));
		settings.setHeading(this.getRotation()-(float)Math.PI/2);
		drone.reset(settings);
	}
	
	public boolean isAirportOccupied(){
		return isGateOneOccupied()&&isGateZeroOccupied();
	}
	
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
	
	public String toString(){
		return getName();
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getName() {
		if(this.name != "")
			return this.name;
		return ("Airport " + this.id);
	}
	
	public int isInGate(Drone drone) {
		Vector3f position = getRelativeCoord(drone.getPosition());
		if (Math.abs(position.x) < w/2 && Math.abs(position.z) < w) {
			if(position.z > 0) return 1;
			else return 0;
		} else {
			return -1;
		}
	}

	public Vector3f getGate0Mid() {
		// TODO Auto-generated method stub
		return getAbsolulateCoordinate(new Vector3f(10,0,-175));
	}

	public Vector3f getGate1Mid() {
		// TODO Auto-generated method stub
		return  getAbsolulateCoordinate(new Vector3f(10,0,175));
	}
	
}
