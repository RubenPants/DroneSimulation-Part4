package entities;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

import shaders.ModelTexture;
import tools.Tools;

public class Monument {
	private TexturedModel tModel;
	private Vector2f position;
	private float rotation;
	private Matrix4f toAirport;
	private Matrix4f toWorld;
	private int scale=35;
	
	public Monument(Vector2f position, float rotation){
		this.position = position;
		this.rotation = rotation;
		this.toWorld = Tools.createTransformationMatrix(getPosition3D(), getRotation(), 0, 0, 1);
		this.toAirport = Matrix4f.invert(toWorld, null);
		//this.entity = new Entity(tModel, new Vector3f(position.x, HEIGHT, position.y), rotation, 0, 0, 35);
	}
	public Monument(Vector2f position, int rotation, int d) {
		this.position = position;
		this.rotation = rotation;
		this.toWorld = Tools.createTransformationMatrix(getPosition3D(), getRotation(), 0, 0, 1);
		this.toAirport = Matrix4f.invert(toWorld, null);
		scale=d;
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
	public void setModel(Model model, ModelTexture texture) {
		tModel = new TexturedModel(model.getVaoID(), model.getVertexCount(), texture);
	}
	
	public Entity getEntity(){
		return new Entity(tModel, new Vector3f(position.x, 0, position.y), rotation, 0, 0, scale);
	}
	public void setLocation(int i, int j) {
		position=new Vector2f(i,j);
		
	}
	

}
