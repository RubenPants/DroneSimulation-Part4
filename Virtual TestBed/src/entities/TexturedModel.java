package entities;

import shaders.ModelTexture;

public class TexturedModel extends Model{

	private ModelTexture texture;
	
	public TexturedModel(int vaoID, int vertexCount, ModelTexture texture) {
		super(vaoID, vertexCount);
		this.texture = texture;
	}

	public ModelTexture getTexture(){
		return texture;
	}
}
