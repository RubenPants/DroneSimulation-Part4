package guis;

import org.lwjgl.util.vector.Vector2f;

public class GuiTexture {

	private int texture;
	private Vector2f position;
	private Vector2f scale;
	private float rotation;
	
	public GuiTexture(int texture, Vector2f position, float rotation, Vector2f scale) {
		super();
		this.texture = texture;
		this.position = position;
		this.rotation = rotation;
		this.scale = scale;
	}
	
	public int getTexture() {
		return texture;
	}
	
	public Vector2f getPosition() {
		return position;
	}
	
	public float getRotation(){
		return rotation;
	}
	
	public Vector2f getScale() {
		return scale;
	}
	
}
