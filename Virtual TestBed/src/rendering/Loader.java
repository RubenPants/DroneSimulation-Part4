package rendering;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.newdawn.slick.opengl.Texture;
import org.newdawn.slick.opengl.TextureLoader;

import de.matthiasmann.twl.utils.PNGDecoder;
import de.matthiasmann.twl.utils.PNGDecoder.Format;
import entities.Model;
import shaders.TextureData;

public class Loader {
	
	private static final String RES_LOC = "res/";
	
	private List<Integer> vaos = new ArrayList<Integer>();
	private List<Integer> vbos = new ArrayList<Integer>();
	private List<Integer> textures = new ArrayList<Integer>();

	public Model loadToVAO(float[] positions, float[] textureCoords, float[] normals, 
			int[] indices) {
		int vaoID = createVAO();
		bindIndicesBuffer(indices);
		storeDataInAttributeList(0, 3, positions);
		storeDataInAttributeList(1, 2, textureCoords);
		storeDataInAttributeList(2, 3, normals);
		unbindVAO();
		return new Model(vaoID,indices.length);
	}
	
	public Model loadToVAO(float[] positions, float[] colorValues, int[] indices) {
		int vaoID = createVAO();
		storeDataInAttributeList(0, 3, positions);
		storeDataInAttributeList(1, 1, colorValues);
		bindIndicesBuffer(indices);
		unbindVAO();
		return new Model(vaoID,indices.length);
	}
	
	public Model loadToVAO(float[] positions, int dimensions) {
		int vaoID = createVAO();
		this.storeDataInAttributeList(0, dimensions, positions);
		unbindVAO();
		return new Model(vaoID, positions.length/dimensions);
	}
	
	private void storeDataInAttributeList(int attributeNumber, int coordinateSize, float[] data) {
		int vboID = GL15.glGenBuffers();
		vbos.add(vboID);
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboID);
		FloatBuffer buffer = storeDataInFloatBuffer(data);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW);
		GL20.glVertexAttribPointer(attributeNumber, coordinateSize, GL11.GL_FLOAT, false, 0, 0);
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
	}
	
	private FloatBuffer storeDataInFloatBuffer(float[] data) {
		FloatBuffer buffer = BufferUtils.createFloatBuffer(data.length);
		buffer.put(data);
		buffer.flip();
		return buffer;
	}
	
	public int loadTexture(String fileName) {
		Texture texture = null;
		try {
			texture = TextureLoader.getTexture("PNG", new FileInputStream("res/" + fileName + ".png"));
			GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR);
			GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL14.GL_TEXTURE_LOD_BIAS, -1.4f);
		} catch (IOException e) {
			e.printStackTrace();
		}
		int textureID = texture.getTextureID();
		textures.add(textureID);
		return textureID;
	}
	
	public int loadCubeMap(String[] textureFiles) {
		int texID = GL11.glGenTextures();
		GL13.glActiveTexture(texID);
		GL11.glBindTexture(GL13.GL_TEXTURE_CUBE_MAP, texID);
		
		for(int i=0;i<textureFiles.length;i++) {
			TextureData data = decodeTextureFile("res/"+textureFiles[i]+".png");
			GL11.glTexImage2D(GL13.GL_TEXTURE_CUBE_MAP_POSITIVE_X+i, 0, GL11.GL_RGBA, data.getWidth(),
					data.getHeight(), 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, data.getBuffer());
		}
		
		GL11.glTexParameteri(GL13.GL_TEXTURE_CUBE_MAP, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
		GL11.glTexParameteri(GL13.GL_TEXTURE_CUBE_MAP, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
		textures.add(texID);
		GL11.glTexParameteri(GL13.GL_TEXTURE_CUBE_MAP, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
		GL11.glTexParameteri(GL13.GL_TEXTURE_CUBE_MAP, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
		return texID;
	}
	
	private TextureData decodeTextureFile(String fileName) {
		int width = 0;
		int height = 0;
		ByteBuffer buffer = null;
		try {
			FileInputStream in = new FileInputStream(fileName);
			PNGDecoder decoder = new PNGDecoder(in);
			width = decoder.getWidth();
			height = decoder.getHeight();
			buffer = ByteBuffer.allocateDirect(4 * width * height);
			decoder.decode(buffer, width * 4, Format.RGBA);
			buffer.flip();
			in.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Tried to load texture " + fileName + ", didn't work");
			System.exit(-1);
		}
		return new TextureData(buffer, width, height);
	}
	
	public void cleanUp() {
		for (int vao: vaos) {
			GL30.glDeleteVertexArrays(vao);
		}
		for (int vbo: vbos) {
			GL15.glDeleteBuffers(vbo);
		}	
		for (int texture: textures) {
			GL11.glDeleteTextures(texture);
		}
	}
	
	private int createVAO() {
		int vaoID = GL30.glGenVertexArrays();
		vaos.add(vaoID);
		GL30.glBindVertexArray(vaoID);
		return vaoID;
	}
	
	private void unbindVAO() {
		GL30.glBindVertexArray(0);
	}
	
	private void bindIndicesBuffer(int[] indices) {
		int vboID = GL15.glGenBuffers();
		vbos.add(vboID);
		GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, vboID);
		IntBuffer buffer = storeDataInIntBuffer(indices);
		GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW);
	}
	
	private IntBuffer storeDataInIntBuffer(int[] data) {
		IntBuffer buffer = BufferUtils.createIntBuffer(data.length);
		buffer.put(data);
		buffer.flip();
		return buffer;	
	}
	
	public Model loadTexturedQuad(){
		float[] positions = {-5, 0, -5,
				 -5, 0, 5,
				 5 , 0, 5,
				 5 , 0, -5};
		float[] textureCoords = { 0, 0,
					  1, 0,
					  1, 1,
					  0, 1};
		float[] normals = {0,1,0,0,1,0,0,1,0,0,1,0};
		int[] indices = {0,1,3,	3,1,2};
		return loadToVAO(positions, textureCoords, normals, indices);
	}
//    public Model loadOBJ(String objFileName) {
//        FileReader isr = null;
//        File objFile = new File(RES_LOC + objFileName + ".obj");
//        try {
//            isr = new FileReader(objFile);
//        } catch (FileNotFoundException e) {
//            System.err.println("File not found in res; don't use any extention");
//        }
//        BufferedReader reader = new BufferedReader(isr);
//        String line;
//        List<Vector3f> vertices = new ArrayList<Vector3f>();
//        List<Vector2f> textures = new ArrayList<Vector2f>();
//        List<Integer> indices = new ArrayList<Integer>();
//        List<Integer> textureIndices = new ArrayList<Integer>();
//        try {
//            while (true) {
//                line = reader.readLine();
//                if(line==null) break;
//                if (line.startsWith("v ")) {
//                    String[] currentLine = line.split(" ");
//                    Vector3f vertex = new Vector3f((float) Float.valueOf(currentLine[1]),
//                            (float) Float.valueOf(currentLine[2]),
//                            (float) Float.valueOf(currentLine[3]));
//                    vertices.add(vertex);
//                    
//                } else if (line.startsWith("vt ")) {
//                    String[] currentLine = line.split(" ");
//                    Vector2f texture = new Vector2f((float) Float.valueOf(currentLine[1]),
//                           (float) Float.valueOf(currentLine[2]));
//                    textures.add(texture);
//                    
//                } else if (line.startsWith("f ")) {
//                    String[] currentLine = line.split(" ");
//                    String[] vertex1 = currentLine[1].split("/");
//                    String[] vertex2 = currentLine[2].split("/");
//                    String[] vertex3 = currentLine[3].split("/");
//                    indices.add(Integer.parseInt(vertex1[0])-1);
//                    indices.add(Integer.parseInt(vertex2[0])-1);
//                    indices.add(Integer.parseInt(vertex3[0])-1);
//                    textureIndices.add(Integer.parseInt(vertex1[1])-1);
//                    textureIndices.add(Integer.parseInt(vertex2[1])-1);
//                    textureIndices.add(Integer.parseInt(vertex3[1])-1);
//                }
//            }
//            reader.close();
//        } catch (IOException e) {
//            System.err.println("Error reading the file");
//        }
//        float[] verticesArray = new float[vertices.size() * 3];
//        float[] texturesArray = new float[vertices.size() * 2];
//        
//        for (int i = 0; i < vertices.size(); i++) {
//	        Vector3f position = vertices.get(i);
//	        verticesArray[i * 3] = position.x;
//	        verticesArray[i * 3 + 1] = position.y;
//	        verticesArray[i * 3 + 2] = position.z;
//        }
//        for(int i = 0; i<indices.size(); i++) {
//        	int currentVertexPointer = indices.get(i);
//        	int currentTexturePointer = textureIndices.get(i);
//	        Vector2f textureCoord = textures.get(currentTexturePointer);
//            texturesArray[currentVertexPointer * 2] = textureCoord.x;
//            texturesArray[currentVertexPointer * 2 + 1] = 1 - textureCoord.y;
//	        
//	    }
//        int[] indicesArray = new int[indices.size()];
//        for (int i = 0; i < indicesArray.length; i++) {
//            indicesArray[i] = indices.get(i);
//        }
//        return loadToVAOTex(verticesArray, texturesArray, indicesArray);
//    }
}

