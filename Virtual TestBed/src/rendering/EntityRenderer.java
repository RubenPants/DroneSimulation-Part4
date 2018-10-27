package rendering;

import java.util.List;
import java.util.Map;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import entities.Entity;
import entities.Light;
import entities.TexturedModel;
import shaders.ModelTexture;
import shaders.TextureShader;
import tools.Tools;

public class EntityRenderer {
	
	private TextureShader shader;
	
	
	public EntityRenderer(TextureShader shader, Matrix4f projectionMatrix){
		this.shader = shader;
		shader.start();
		shader.loadLight(new Light(new Vector3f(20000,100000,-30000), new Vector3f(1,1,1)));
		shader.loadProjectionMatrix(projectionMatrix);
		shader.stop();
	}
	
	
	public void render(Map<TexturedModel,List<Entity>> entities) {
		for(TexturedModel model:entities.keySet()) {
			prepareTexturedModel(model);
			List<Entity> batch = entities.get(model);
			for(Entity entity:batch) {
				prepareInstance(entity);
				GL11.glDrawElements(GL11.GL_TRIANGLES, model.getVertexCount(), GL11.GL_UNSIGNED_INT, 0);
			}
			unbindTexturedModel();
		}
	}
	
	private void prepareTexturedModel(TexturedModel model) {
		GL30.glBindVertexArray(model.getVaoID());
		GL20.glEnableVertexAttribArray(0); //posities zitten in attribuut 0
		GL20.glEnableVertexAttribArray(1);
		GL20.glEnableVertexAttribArray(2);
		ModelTexture texture = model.getTexture();
		if(texture.isHasTransparency()) {
			MasterRenderer.disableCulling();		
			GL11.glEnable(GL11.GL_BLEND);
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		}
		GL11.glDepthMask(texture.getDepthMask());
		shader.loadShineVariables(texture.getShineDamper(), texture.getReflectivity());
		GL13.glActiveTexture(GL13.GL_TEXTURE0);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture.getID());
	}
	
	private void unbindTexturedModel() {
		MasterRenderer.enableCulling();
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glDepthMask(true);
		GL20.glDisableVertexAttribArray(0);
		GL20.glDisableVertexAttribArray(1);
		GL20.glDisableVertexAttribArray(2);
		GL30.glBindVertexArray(0);
	}
	
	private void prepareInstance(Entity entity) {
		Matrix4f transformationMatrix = Tools.createTransformationMatrix(
				entity.getPosition(), entity.getHeading(), entity.getPitch(),
				entity.getRoll(), entity.getScale());
		shader.loadTransformationMatrix(transformationMatrix);
	}
}
