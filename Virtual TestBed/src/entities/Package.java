package entities;

import org.lwjgl.util.vector.Vector3f;

import shaders.ModelTexture;

public class Package {
	
	private static Model model;
	private static ModelTexture texture;
	
	private static int guiTexture;
	//variable registering the airport-number where this package is initially placed. 
	public int fromA;
	//variable registering the airport-number where this package has to be sent.
	public int toA;
	
	//variable registering the gate-number where this package is initially placed.
	public int fromG;
	//variable registering the gate number where this package has to be sent.
	public int toG;

	
	private boolean delivered = false;

	public Package(int fromA, int fromG, int toA, int toG) {
		this.fromA = fromA;
		this.toA = toA;
		this.fromG = fromG;
		this.toG = toG;
		this.carried = false;
		
	}
	
	
	public static void setModel(Model packageModel, ModelTexture texture) {
		Package.model = packageModel;
		Package.texture = texture;
	}
	
	public static void setGuiTexture(int texture){
		guiTexture = texture;
	}
	
	public int getGuiTexture(){
		return guiTexture;
	}
	
	public Entity getEntity(Vector3f position, Vector3f rotation){
		TexturedModel tModel = new TexturedModel(model.getVaoID(), model.getVertexCount(), texture);
	
		Entity entity = new Entity(tModel, position, rotation.x, rotation.y, rotation.z, 1);

		return entity;
	}
	
	//additional parameters (variables)
	
	//variable registering wether this package is carried by some drone
	public boolean carried;
	
	//variable registering the dorne ID of the drone which is carrying this package
	public Drone carriedBy;

	//
	public void startCarry(Drone drone){
		this.carried = true;
		this.carriedBy = drone;
		drone.setHasPackage(true);
	}
	
	public void deliver() {
		delivered = true;
		carried = false;
		carriedBy.setHasPackage(false);
	}
	
	public boolean isWaiting() {
		if(carried || delivered) return false;
		return true;
	}
	
	public boolean isDelivered() {
		return delivered;
	}

	public int getFromA() {
		return fromA;
	}

	public int getToA() {
		return toA;
	}

	public int getFromG() {
		return fromG;
	}

	public int getToG() {
		return toG;
	}


	public int getCurrentAirportNo() {
		// TODO Auto-generated method stub
		return fromA;
	}
	
	
}