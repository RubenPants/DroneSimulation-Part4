package entities;

import org.lwjgl.util.vector.Vector3f;

import shaders.ModelTexture;

public class Symbol {

	private Model model;
	private ModelTexture texture;
	private Vector3f position;
	private float scale;
	
	public Symbol(Model model, ModelTexture texture, Vector3f position, float scale) {
		this.model = model;
		this.texture = texture;
		this.position = position;
		this.texture.setReflectivity(0.1f);
		this.texture.setShineDamper(2);
		this.scale = scale;
	}
	
	public void setModel(Model model, ModelTexture texture) {
		this.model = model;
		this.texture = texture;
	}
	
	public Entity getEntity(){
		TexturedModel tModel = new TexturedModel(model.getVaoID(), model.getVertexCount(), texture);
		return new Entity(tModel, position, 0, 0, 0, scale);
	}
	
}