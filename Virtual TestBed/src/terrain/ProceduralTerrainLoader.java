package terrain;

import java.util.ArrayList;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

import entities.Drone;
import entities.Model;

public class ProceduralTerrainLoader {
	
	private ArrayList<Terrain> terrains = new ArrayList<>();
	private Model model;
	private TerrainTexturePack texturePack;
	private TerrainTexture blendMap;
	
	private int gridAmount;
	
	private Vector2f lastGridPosition;
	
	public ProceduralTerrainLoader(Model model, int gridAmount,
			TerrainTexturePack texturePack, TerrainTexture blendMap) {
		this.model = model;
		this.gridAmount = gridAmount;
		this.texturePack = texturePack;
		this.blendMap = blendMap;
	}
	
	public ArrayList<Terrain> getTerrains() {
		return this.terrains;
	}

	public void updateTerrain(Vector3f position) {
		Vector2f currentGridPos = getCurrentGridPosition(position);
		if(currentGridPos.equals(lastGridPosition))
			return;
		else {
			terrains.clear();
			int start = (gridAmount - 1)/2;
			for(int i = -start; i < start+1; i++) {
				for(int j = -start; j < start+1; j++) {
					terrains.add(new Terrain((int)currentGridPos.x + i, (int)currentGridPos.y + j, texturePack, blendMap, model));
				}
			}
		}
	}
	
	private Vector2f getCurrentGridPosition(Vector3f position) {
		int gridX = (int) Math.floor(position.x/Terrain.getSize());
		int gridZ = (int) Math.floor((position.z/Terrain.getSize()));
		return new Vector2f(gridX, gridZ);
	}
}
