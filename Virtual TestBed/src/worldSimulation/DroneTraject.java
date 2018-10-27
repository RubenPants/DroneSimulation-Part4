package worldSimulation;

import java.util.ArrayList;

import org.lwjgl.util.vector.Vector2f;

import control.WorldManager;
import entities.Airport;
import entities.Drone;
import entities.Package;
import guis.GuiTexture;
import tools.Tools;

public class DroneTraject {
	
	private WorldManager manager;
	private static int texture;
	
	public DroneTraject(WorldManager manager) {
		this.manager = manager;
	}

	public static void setTexture(int texture) {
		DroneTraject.texture = texture;
	}
	
	public ArrayList<GuiTexture> getTraject(int droneID, float width, float interval){
		ArrayList<GuiTexture> guis = new ArrayList<>();
		Drone drone = manager.getDrone(droneID);
		if(drone.isHasPackage()) {
			Package pack = manager.deliveringPackages.get(drone.getID());
			Airport from = manager.getAirport(pack.getFromA());
			Airport to = manager.getAirport(pack.getToA());
			Vector2f dronePos = new Vector2f(drone.getPosition().x, drone.getPosition().z);
			
			Vector2f diffVector2 = Tools.subtract(to.getPosition2D(), dronePos);
			float d2 = diffVector2.length();
			int points = (int)d2/800;
			
			
			for(float i = 1.0f/points;i<=1;i+=1.0f/points) {
				Vector2f position2 = Tools.addVectors(dronePos, Tools.scaleVector(diffVector2, i));
				guis.add(new GuiTexture(texture, new Vector2f(2*position2.x/width-1, 2*-position2.y/width-1),
						0, new Vector2f(0.007f, 0.007f)));
			}
		}
		
		return guis;
	}
}
