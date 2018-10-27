package rendering;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector4f;

import control.AppManager;
import entities.Camera;
import entities.Entity;
import entities.TexturedModel;
import shaders.TerrainShader;
import shaders.TextureShader;
import skybox.SkyboxRenderer;
import terrain.Terrain;

public class MasterRenderer {

	private float FOV = 120;
	private static final float NEAR_PLANE = 0.1f;
	private static final float FAR_PLANE = 20000f;
	
	public float RED = 0.7f;
	public float GREEN = 0.9f;
	public float BLUE = 0.9f;
	
	private Matrix4f projectionMatrix;
	
	public Matrix4f getProjectionMatrix() {
		return projectionMatrix;
	}

	private TextureShader shader = new TextureShader();
	private EntityRenderer renderer;
	
	private TerrainRenderer terrainRenderer;
	private TerrainShader terrainShader = new TerrainShader();
	
	private Map<TexturedModel,List<Entity>> entities = new HashMap<TexturedModel,List<Entity>>();
	private Map<TexturedModel,List<Entity>> transparencies = new HashMap<TexturedModel,List<Entity>>();
	private List<Terrain> terrains = new ArrayList<Terrain>();
	
	private SkyboxRenderer skyboxRenderer;
	
	public MasterRenderer(Loader loader) {
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glCullFace(GL11.GL_BACK);
		GL11.glEnable(GL30.GL_CLIP_DISTANCE0);
		createPerspectiveProjectionMatrix();
		renderer = new EntityRenderer(shader, projectionMatrix);
		terrainRenderer = new TerrainRenderer(terrainShader, projectionMatrix);
		skyboxRenderer = new SkyboxRenderer(loader, projectionMatrix);
	}
	
	public static void enableCulling() {
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glCullFace(GL11.GL_BACK);
	}
	
	public static void disableCulling() {
		GL11.glDisable(GL11.GL_CULL_FACE);
	}
	
	public void renderScene(List<Entity> entities, List<Terrain> terrains, Camera camera) {
		for (Terrain terrain: terrains) {
			processTerrain(terrain);
		}
		for(Entity entity: entities) {
			processEntity(entity);
		}
		render(camera);
	}
	
	public void render(Camera camera) {
		prepare();
		
		GL11.glDepthMask(false);
		skyboxRenderer.loadProjectionMatrix(projectionMatrix);
		skyboxRenderer.render(camera,RED, GREEN, BLUE);
		terrainShader.start();
		terrainShader.loadProjectionMatrix(projectionMatrix);
//		terrainShader.loadClipPlane(clipPlane);
//		terrainShader.loadSkyColour(RED, GREEN, BLUE);
//		terrainShader.loadLights(lights);
		terrainShader.loadViewMatrix(camera);
		terrainRenderer.render(terrains);
		GL11.glDepthMask(true);

		terrainShader.stop();
		shader.start();
		shader.loadProjectionMatrix(projectionMatrix);
		shader.loadClipPlane(new Vector4f(0,1,0,2f));
//		shader.loadSkyColour(RED, GREEN, BLUE);
//		shader.loadLights(lights);
		shader.loadViewMatrix(camera);
		renderer.render(entities);
		renderer.render(transparencies);
		shader.stop();

		terrains.clear();
		entities.clear();
		transparencies.clear();

	}
	
	public void processTerrain(Terrain terrain) {
		terrains.add(terrain);
	}
	
	public void processEntity(Entity entity) {
		TexturedModel entityModel = entity.getModel();
		if (!entityModel.getTexture().isHasTransparency()) {
			List<Entity> batch = entities.get(entityModel);
			if (batch != null) {
				batch.add(entity);
			} else {
				List<Entity> newBatch = new ArrayList<Entity>();
				newBatch.add(entity);
				entities.put(entityModel, newBatch);
			} 
		} else {
			List<Entity> batch = transparencies.get(entityModel);
			if (batch != null) {
				batch.add(entity);
			} else {
				List<Entity> newBatch = new ArrayList<Entity>();
				newBatch.add(entity);
				transparencies.put(entityModel, newBatch);
			} 
		}
	}
	
	public void cleanUp() {
		shader.cleanUp();
		terrainShader.cleanUp();
	}
	
	
	public void prepare() {
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glDepthFunc(GL11.GL_LEQUAL);
		GL11.glClear(GL11.GL_COLOR_BUFFER_BIT|GL11.GL_DEPTH_BUFFER_BIT); // | = OR operator
		GL11.glClearColor(RED, GREEN, BLUE, 1);
	}
	
	
	public void createPerspectiveProjectionMatrix(){
        float aspectRatio = (float) Display.getWidth() / (float) Display.getHeight();
        float y_scale = (float) ((1f / Math.tan(Math.toRadians(FOV/2f))) * aspectRatio);
        float x_scale = y_scale / aspectRatio;
        float frustum_length = FAR_PLANE - NEAR_PLANE;
        
        projectionMatrix = new Matrix4f();
        projectionMatrix.m00 = x_scale;
        projectionMatrix.m11 = y_scale;
        projectionMatrix.m22 = -((FAR_PLANE + NEAR_PLANE) / frustum_length);
        projectionMatrix.m23 = -1;
        projectionMatrix.m32 = -((2 * NEAR_PLANE * FAR_PLANE) / frustum_length);
        projectionMatrix.m33 = 0;
	}
	
	public void createOrthogonalProjectionMatrix(float width, float height) {
		float r = width/2;
		float t = height/2;
        float frustum_length = FAR_PLANE - NEAR_PLANE;
		
		projectionMatrix = new Matrix4f();
		projectionMatrix.m00 = 1/r;
		projectionMatrix.m11 = 1/t;
		projectionMatrix.m22 = -2/frustum_length;
		projectionMatrix.m32 = -((FAR_PLANE + NEAR_PLANE) / frustum_length);
		projectionMatrix.m33 = 1;
	}
	
	public void changeSettings(float fov){
		this.FOV = fov;
	}
}
