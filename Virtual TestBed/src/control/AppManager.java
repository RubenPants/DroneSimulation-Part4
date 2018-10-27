package control;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import org.lwjgl.Sys;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

import entities.Airport;
import entities.Camera;
import entities.Drone;
import entities.Entity;
import entities.Model;
import entities.Monument;
import entities.PackageKey;
import entities.Symbol;
import entities.TexturedModel;
import guis.GuiRenderer;
import guis.GuiTexture;
import interfaces.AutopilotConfig;
import interfaces.AutopilotModule;
import objConverter.OBJFileLoader;
import rendering.CameraView;
import rendering.Loader;
import rendering.MasterRenderer;
import rendering.TextureFrameBuffer;
import shaders.ModelTexture;
import swing_components.AccuracyFrame;
import swing_components.CloseListener;
import swing_components.MainFrame;
import swing_components.SettingsEvent;
import swing_components.SettingsListener;
import swing_components.SimulationListener;
import terrain.ProceduralTerrainLoader;
import terrain.Terrain;
import terrain.TerrainTexture;
import terrain.TerrainTexturePack;
import worldSimulation.DroneStartSettings;
import worldSimulation.DroneTraject;

public class AppManager {
	
	private static boolean ENABLE_LOGGING = false;
	
	public static final int START_DISPLAY_WIDTH = 800;
	public static final int START_DISPLAY_HEIGHT = 800;
	private static int FPS_CAP = 50;
	private static int autopilotCallsPerSecond = 50;
	private static int iterationsPerFrame = 4;
	private static final int INFO_REFRESH_RATE = 10;
	
	//private static final int DRONE_AMOUNT = 1;
	private static final int AIRPORT_LENGTH = 70;	
	private static final int AIRPORT_WIDTH = 250;
	
	private static float lastFrameTime;
	private static long thisFrameTime;
	private static long lastUpdateTime;
	private static float delta;
	
	private static MasterRenderer masterRenderer;
	private static GuiRenderer guiRenderer;
	private static Loader loader;
	
	private static WorldManager worldManager;
	private static DroneTraject traject;
	
	private static TexturedModel texModel;
	private static TexturedModel shadowTexModel;
	
	private static int[] focussedDrone = new int[]{0, 0, 0, 0};
	private static boolean changingDrone = false;
	private static ProceduralTerrainLoader terrainLoader;
	private static float cameraDistance = 50f;
	private static float timeRelativeToReal = 1f;
	private static boolean maxSpeed = false;
	
	private static TextureFrameBuffer textureTopLeftBuffer;
	private static TextureFrameBuffer textureTopRightBuffer;
	private static TextureFrameBuffer textureBottomLeftBuffer;
	private static TextureFrameBuffer textureBottomRightBuffer;
	private static List<GuiTexture> guis = new ArrayList<GuiTexture>();	
	private static ByteBuffer imageBuffer = ByteBuffer.allocateDirect(START_DISPLAY_WIDTH*START_DISPLAY_HEIGHT*3);
	private static byte[] image;
	private static byte[] imageCopy;
	private static boolean imageCopyRequested = true;
	private static Camera customCamera;
	private static ArrayList<Monument> monuments=new ArrayList<Monument>();
	
	private static MainFrame mainFrame;
	private static JFileChooser fileChooser;
	
	private static float deltaLastInfoUpdate = 0; 
	private static boolean shouldClose = false;
	private static CameraView cameraView = CameraView.Custom;
	private static SimulationStatus simStatus = SimulationStatus.Idle;
	
	private static float simulationTime = 0;
	
	private static AutopilotModule module;
	private static AutopilotConfig configs;
	
	/**
	 * Starts the app by creating and showing the configurations frame. The mainframe is made as well but is still invisible.
	 */
	public static void createApp() {
		//Initialiseer het swing main en tester paneel.
		createMainFrame(START_DISPLAY_WIDTH, START_DISPLAY_HEIGHT);
		
		//Initialiseer de loader en renderers en de buffers om naar te renderen
		loader = new Loader();
		masterRenderer = new MasterRenderer(loader);
		guiRenderer = new GuiRenderer(loader);
		
		textureTopLeftBuffer = new TextureFrameBuffer(256,256);
		textureTopRightBuffer = new TextureFrameBuffer(256,256);
		textureBottomLeftBuffer = new TextureFrameBuffer(256,256);
		textureBottomRightBuffer = new TextureFrameBuffer(256,256);
		
		//Splits het scherm in 2 delen voor de orthogonale projecties
		GuiTexture guiTextureTopLeft = new GuiTexture(textureTopLeftBuffer.getTexture(), new Vector2f(-0.5f,0.5f), 0, new Vector2f(0.5f,-0.5f));
		GuiTexture guiTextureTopRight = new GuiTexture(textureTopRightBuffer.getTexture(), new Vector2f(0.5f,0.5f), 0, new Vector2f(.5f,-.5f));
		GuiTexture guiTextureBottomLeft = new GuiTexture(textureBottomLeftBuffer.getTexture(), new Vector2f(-0.5f,-0.5f), 0, new Vector2f(.5f,-.5f));
		GuiTexture guiTextureBottomRight = new GuiTexture(textureBottomRightBuffer.getTexture(), new Vector2f(0.5f,-0.5f), 0, new Vector2f(.5f,-.5f));
		
		guis.add(guiTextureTopLeft);
		guis.add(guiTextureTopRight);
		guis.add(guiTextureBottomLeft);
		guis.add(guiTextureBottomRight);

		//Initialiseer de filechooser om paden te lezen
		fileChooser = new JFileChooser();
		
		//*****************TERRAIN TEXTURES**********************
		int europeTexture = loader.loadTexture("europe_big");
		TerrainTexture backgroundTexture = new TerrainTexture(europeTexture);
		TerrainTexture rTexture = new TerrainTexture(europeTexture);
		TerrainTexture gTexture = new TerrainTexture(europeTexture);
		TerrainTexture bTexture = new TerrainTexture(europeTexture);
				
		TerrainTexturePack texturePack = new TerrainTexturePack(backgroundTexture,rTexture,gTexture,bTexture);
		TerrainTexture blendMap = new TerrainTexture(loader.loadTexture("blendMap2"));
		
		Model terrainModel = Terrain.generateTerrain(loader);

		
		//*******************DRONE EN AIRPORT MODELS*************************
		Model planeModel = OBJFileLoader.loadOBJ("dronewithoutpropell", loader);
		Model shadowModel = loader.loadTexturedQuad();
		Model propellerModel = OBJFileLoader.loadOBJ("propeller", loader);
		ModelTexture propellerTexture=new ModelTexture(loader.loadTexture("plane2"));
		propellerTexture.setReflectivity(1);
		propellerTexture.setShineDamper(6);
		propellerTexture.setHasTransparency(false);
		TexturedModel propelTex = new TexturedModel(propellerModel.getVaoID(),propellerModel.getVertexCount(),propellerTexture);
		
		Drone.setPropellerModel(propelTex);
		
		ModelTexture planeTexture = new ModelTexture(loader.loadTexture("plane2"));
		planeTexture.setReflectivity(1);
		planeTexture.setShineDamper(6);
		ModelTexture shadowTexture = new ModelTexture(loader.loadTexture("shadowTexture"));
		shadowTexture.setHasTransparency(true);
		
		texModel = new TexturedModel(planeModel.getVaoID(),planeModel.getVertexCount(),planeTexture);
		shadowTexModel = new TexturedModel(shadowModel.getVaoID(), shadowModel.getVertexCount(), shadowTexture);
		Drone.addModels(texModel, shadowTexModel, loader.loadTexture("topPlaneRed"), loader.loadTexture("topPlaneRedPackage"), loader.loadTexture("topPlaneFocus"), loader.loadTexture("topPlanePackageFocus"));
		
		ModelTexture tarmac2 = new ModelTexture(loader.loadTexture("tarmac2"));
		tarmac2.setReflectivity(0.2f);
		tarmac2.setShineDamper(2);
		tarmac2.setDepthMask(false);
		
		Model airportModel = OBJFileLoader.loadOBJ("finalp", loader);
		ModelTexture airportTexture = new ModelTexture(loader.loadTexture("betterbegood"));
		airportTexture.setReflectivity(0.2f);
		airportTexture.setShineDamper(2);
		Airport.setModel(airportModel, airportTexture);
		
		Monument atomium=new Monument(new Vector2f(0,0),0,5);
		Model atomiumModel= OBJFileLoader.loadOBJ("atomiumfull",loader);
		ModelTexture atomiumTexture = new ModelTexture(loader.loadTexture("atomiumimage"));
		atomiumTexture.setReflectivity(0.2f);
		atomiumTexture.setShineDamper(2);
		atomium.setModel(atomiumModel, atomiumTexture);
		monuments.add(atomium);
		
		Monument berlinwall=new Monument(new Vector2f(0,0),0,10);
		Model berlinwallModel= OBJFileLoader.loadOBJ("berlinwallfull",loader);
		ModelTexture berlinwallTexture = new ModelTexture(loader.loadTexture("berlinwallimage"));
		berlinwallTexture.setReflectivity(0.2f);
		berlinwallTexture.setShineDamper(2);
		berlinwall.setModel(berlinwallModel, berlinwallTexture);
		monuments.add(berlinwall);
		
		Monument bigben=new Monument(new Vector2f(0,0),0,2);
		Model bigbenModel= OBJFileLoader.loadOBJ("bigbenfull",loader);
		ModelTexture bigbenTexture = new ModelTexture(loader.loadTexture("bigbenimage"));
		bigbenTexture.setReflectivity(0.2f);
		bigbenTexture.setShineDamper(2);
		bigben.setModel(bigbenModel, bigbenTexture);
		monuments.add(bigben);
		
		Monument collosseum=new Monument(new Vector2f(0,0),180,4);
		Model collosseumModel= OBJFileLoader.loadOBJ("collosseumfull",loader);
		ModelTexture collosseumTexture = new ModelTexture(loader.loadTexture("collosseum"));
		collosseumTexture.setReflectivity(0.2f);
		collosseumTexture.setShineDamper(2);
		collosseum.setModel(collosseumModel, collosseumTexture);
		monuments.add(collosseum);
		
		Monument eifeltower=new Monument(new Vector2f(0,0),0,2);
		Model eifeltowerModel= OBJFileLoader.loadOBJ("eifeltower",loader);
		ModelTexture eifeltowerTexture = new ModelTexture(loader.loadTexture("eifeltowerimage"));
		eifeltowerTexture.setReflectivity(0.2f);
		eifeltowerTexture.setShineDamper(2);
		eifeltower.setModel(eifeltowerModel, eifeltowerTexture);
		monuments.add(eifeltower);
		
		Monument windmill=new Monument(new Vector2f(0,0),90,4);
		Model windmillModel= OBJFileLoader.loadOBJ("windmillfull",loader);
		ModelTexture windmillTexture = new ModelTexture(loader.loadTexture("windmillimage"));
		windmillTexture.setReflectivity(0.2f);
		windmillTexture.setShineDamper(2);
		windmill.setModel(windmillModel, windmillTexture);
		monuments.add(windmill);
		
		entities.Package.setGuiTexture(loader.loadTexture("package"));
		Model packageModel = OBJFileLoader.loadOBJ("Cage_Containerfull", loader);
		ModelTexture packageTexture = new ModelTexture(loader.loadTexture("texturepackage"));
		packageTexture.setReflectivity(0.1f);
		packageTexture.setShineDamper(2);
		entities.Package.setModel(packageModel, packageTexture);
		
		//******************WERELD*******************************
		worldManager = new WorldManager(new CrashHandler() {
			public void handleCrash(String message) {
				simStatus = SimulationStatus.RestartRequested;
				showErrorMessage(message);
			}
		});
		
		traject = new DroneTraject(worldManager);
		DroneTraject.setTexture(loader.loadTexture("circle"));
		
		/*
		Symbol bigben = new Symbol(OBJFileLoader.loadOBJ("bigben", loader), 
				new ModelTexture(loader.loadTexture("texturepackage")), 
				new Vector3f(7800, -4.0f, -15400),
				.1f);
		worldManager.addSymbol(bigben);
		*/
		
		Airport.defineAirportParameters(AIRPORT_LENGTH, AIRPORT_WIDTH);
		Airport Madrid = new Airport(new Vector2f(3000,-2800), 0);
		Madrid.setName("Madrid");
		
		Airport Rome = new Airport(new Vector2f(15700,-3000), 0);
		Rome.setName("Rome");
		collosseum.setLocation(15400, -2600);
		
		Airport London = new Airport(new Vector2f(7800,-15000), 0);
		London.setName("London");
		bigben.setLocation(7450,-14600);
		
		Airport Holland = new Airport(new Vector2f(11300,-15100), 0);
		Holland.setName("Amsterdam");
		atomium.setLocation(10000,-13300);
		windmill.setLocation(11000, -15000);
		
		Airport Berlin = new Airport(new Vector2f(17000,-14900), 0);
		Berlin.setName("Berlin");
		berlinwall.setLocation(16500,-14900);
		
		Airport Copenhagen = new Airport(new Vector2f(15800, -18000), 0);
		Copenhagen.setName("Copenhagen");
		Airport Austria = new Airport(new Vector2f(17000,-8500), 0);
		Austria.setName("Kapfenberg");
		
		Airport Paris = new Airport(new Vector2f(8800,-10800), 0);
		Paris.setName("Paris");
		eifeltower.setLocation(8300, -11300);
		
		Airport westFrance = new Airport(new Vector2f(4400, -11700), 0);
		westFrance.setName("Brest");
		Airport Dublin = new Airport(new Vector2f(2800,-16600), 0);
		Dublin.setName("Dublin");
		Airport Germany = new Airport(new Vector2f(13000, -11500), 0);
		Germany.setName("Frankfurt");
		Airport Switzerland = new Airport(new Vector2f(12000, -8000), 0);
		Switzerland.setName("Bern");
		Airport Marseille = new Airport(new Vector2f(6500, -5300), 0);
		Marseille.setName("Toulouse");
		Airport Prague = new Airport(new Vector2f(17900, -12000), 0);
		Prague.setName("Prague");
		
		worldManager.addAirport(Madrid);
		worldManager.addAirport(Rome);
		worldManager.addAirport(London);
		worldManager.addAirport(Holland);
		worldManager.addAirport(Berlin);
		worldManager.addAirport(Copenhagen);
		worldManager.addAirport(Austria);
		worldManager.addAirport(Paris);
		worldManager.addAirport(westFrance);
		worldManager.addAirport(Dublin);
		worldManager.addAirport(Germany);
		worldManager.addAirport(Switzerland);
		worldManager.addAirport(Marseille);
		worldManager.addAirport(Prague);
		
		configs = mainFrame.getConfigs();
		Drone drone0 = new Drone(configs);
		worldManager.addDroneToAirport(drone0, London, 0);
		worldManager.addDrone(drone0);
		customCamera = new Camera(drone0.getCameraPosition(), 0, (float)-Math.PI/6, 0);
		
		Drone drone1 = new Drone(configs);
		worldManager.addDroneToAirport(drone1, Rome, 0);
		worldManager.addDrone(drone1);
		
		Drone drone2 = new Drone(configs);
		worldManager.addDroneToAirport(drone2, Madrid, 0);
		worldManager.addDrone(drone2);
		
		Drone drone3 = new Drone(configs);
		worldManager.addDroneToAirport(drone3, Holland, 0);
		worldManager.addDrone(drone3);
		
		Drone drone4 = new Drone(configs);
		worldManager.addDroneToAirport(drone4, Berlin, 0);
		worldManager.addDrone(drone4);
		
		Drone drone5 = new Drone(configs);
		worldManager.addDroneToAirport(drone5, Copenhagen, 0);
		worldManager.addDrone(drone5);
		
//		Drone drone6 = new Drone(configs);
//		worldManager.addDroneToAirport(drone6, Austria, 0);
//		worldManager.addDrone(drone6);
//		
//		Drone drone7 = new Drone(configs);
//		worldManager.addDroneToAirport(drone7, Paris, 0);
//		worldManager.addDrone(drone7);
//		
//		Drone drone8 = new Drone(configs);
//		worldManager.addDroneToAirport(drone8, westFrance, 0);
//		worldManager.addDrone(drone8);
//		
//		Drone drone9 = new Drone(configs);
//		worldManager.addDroneToAirport(drone9, Dublin, 0);
//		worldManager.addDrone(drone9);
//		
//		Drone drone10 = new Drone(configs);
//		worldManager.addDroneToAirport(drone10, Germany, 0);
//		worldManager.addDrone(drone10);
//		
//		Drone drone11 = new Drone(configs);
//		worldManager.addDroneToAirport(drone11, Switzerland, 0);
//		worldManager.addDrone(drone11);
//		
//		Drone drone12 = new Drone(configs);
//		worldManager.addDroneToAirport(drone12, Marseille, 0);
//		worldManager.addDrone(drone12);
//		
//		Drone drone13 = new Drone(configs);
//		worldManager.addDroneToAirport(drone13, Prague, 0);
//		worldManager.addDrone(drone13);
		
		
		terrainLoader = new ProceduralTerrainLoader(terrainModel, 1, texturePack, blendMap);
		
		mainFrame.setWorldManager(worldManager);
		mainFrame.fireData(worldManager);
		
		//Initialiseer de frame time en maak mainFrame zichtbaar
		lastFrameTime = getCurrentTime();
		lastUpdateTime = getCurrentTime();
		mainFrame.setVisible(true);
	}
	
	/**
	 * Update the whole app. This includes rendering the image, then if the simulation isn't paused updating the drone.
	 * Then the display is updated so the drone can be evolved over the amount of time that it took to do the last frame.
	 */
	public static void updateApp() {
		long timeMilli = getCurrentTime();
		long time = System.nanoTime();
		//Haal de simulation status op. (Wordt ook aangepast door swing thread)
		SimulationStatus status = simStatus;
		
		/*
		 * Kijk naar de simulation status:
		 * ResetRequested: Herstart de drone en verwijder het pad
		 * RestartRequested: Herstart enkel de drone en het pad
		 * PathUpdateRequested: Herstart de drone en zet het nieuwe pad
		 */
		switch (status) {
		case RestartRequested:
			reset();
			break;
		case ConfigRequested:
			configAutopilotModule();
			break;
		case Started:
			try {
				if(worldManager.emptyQueue())
					mainFrame.addPackageToTable();
				worldManager.checkForCollision();
				if(worldManager.checkDronesForPackagePickup()) mainFrame.refreshPackTable();
				worldManager.allDronesTimePassed((float) 1.0/autopilotCallsPerSecond, simulationTime, iterationsPerFrame);
			} catch (Exception e) {
				showErrorMessage(e.getMessage());
			}
			simulationTime += (float) 1.0/autopilotCallsPerSecond;
			break;
		default:
			break;
		}
		
		//Update het scherm (default FrameBuffer)
		long time2 = System.nanoTime();
		if (ENABLE_LOGGING) System.out.println("TOTAL PHYSICS TIME TESTBED:" + System.nanoTime() + "-" + time2 + "=" + (time2-time));
		updateDisplay();

		long time3 = System.nanoTime();
		if (ENABLE_LOGGING) System.out.println("TOTAL RENDER TIME TESTBED:" + System.nanoTime() + "-" + time3 + "=" + (time3-time2));
		
		refreshInfo(focussedDrone[0]);
		long time4 = System.nanoTime();
		if (ENABLE_LOGGING) System.out.println("TOTAL REST TIME TESTBED:" + System.nanoTime() + "-" + time4 + "=" + (time4-time3));
	    checkInputs();
		long time5 = System.nanoTime();
		if (ENABLE_LOGGING) System.out.println("TOTAL REST TIME TESTBED:" + System.nanoTime() + "-" + time5 + "=" + (time5-time4));
		//Synchroniseer de update van de app naar het aantal autopilot calls.
		int loopsPerSecond = (int)(autopilotCallsPerSecond*timeRelativeToReal);
		if(!maxSpeed)
			Display.sync(loopsPerSecond);
		long time6 = System.nanoTime();
		if (ENABLE_LOGGING) System.out.println("TOTAL REST TIME TESTBED:" + System.nanoTime() + "-" + time6 + "=" + (time6-time4));
		//System.out.println("TOTAL UPDATE TIME TESTBED:" + getCurrentTime() + "-" + timeMilli + "=" + (getCurrentTime()-timeMilli));
	}
	
	/**
	 * Create the mainframe, including all the openGL stuff. (Renderer en loader initialised as well)
	 */
	private static void createMainFrame(int width, int height) {
		mainFrame = new MainFrame(width, height);
		mainFrame.setCloseListener(new CloseListener() {
			public void requestClose() {
				shouldClose = true;
				mainFrame.dispose();
			}
		});
		mainFrame.setSettingsListener(new SettingsListener() {
			public void changeRenderSettings(SettingsEvent e) {
				masterRenderer.changeSettings(e.getFov());
			}
			@Override
			public void setTime(float time) {
				timeRelativeToReal = time;
			}
			@Override
			public void setDroneForCamera(int id) {
				focussedDrone[0] = id;
			}
			@Override
			public void setMaxSpeed(boolean max) {
				maxSpeed = max;
				
			}
		});
		mainFrame.setSimulationListener(new SimulationListener() {
			public void swapView(CameraView view) {
				cameraView = view;
			}
			public void swapSimulationStatus(SimulationStatus status) {
				simStatus = status;
				imageCopyRequested = true;
			}
		});

		mainFrame.addExportListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(fileChooser.showSaveDialog(mainFrame) == JFileChooser.APPROVE_OPTION){
					File selectedFile = fileChooser.getSelectedFile();
					if(((JMenuItem)e.getSource()).getText() == "Export Drone View"){
						selectedFile = new File(selectedFile + ".bmp");
						saveImage(selectedFile);
					}
				}
			}
		});
		mainFrame.addAccuracyListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			    EventQueue.invokeLater(new Runnable() {
			        public void run() {
			            AccuracyFrame frame = new AccuracyFrame(autopilotCallsPerSecond, iterationsPerFrame);
			            frame.addButtonListener(new ActionListener() {
							public void actionPerformed(ActionEvent e) {
								int[] settings = frame.getSettings();
								AppManager.autopilotCallsPerSecond = settings[0];
								AppManager.iterationsPerFrame = settings[1];
								frame.dispose();
							}
						});
			        }
			    });
			}
		});
	}
	
	/**
	 * Update the virtual drone. (Ask the autopilot for outputs and change the settings of the plane.)
	 * If the drone is not yet configured for the autopilot, then it gets configured. 
	 */
	private static void configAutopilotModule(){
		module.defineAirportParams(AIRPORT_LENGTH, AIRPORT_WIDTH);
		for(Airport port: worldManager.getAirports()){
			module.defineAirport(port.getPosition2D().x, port.getPosition2D().y, port.getCenterRunway0Relative().x, port.getCenterRunway0Relative().y);
		}
		worldManager.defineDrones();
		simStatus = SimulationStatus.Started;
	}
	
	/**
	 * Update the display. The display is synchronised to FPS_CAP frames per second (if it can handle it). 
	 * The amount of time between the last frame and the current frame is saved in delta. 
	 */
	private static void updateDisplay() {
		long currentUpdateTime = getCurrentTime();
		delta = (currentUpdateTime - lastUpdateTime)/1000f;
		lastUpdateTime = currentUpdateTime;
		thisFrameTime += delta*1000;
		if(thisFrameTime >= 1000/FPS_CAP) {
			//System.out.println("RENDERING");
			renderTestbedView();
			lastFrameTime = thisFrameTime/1000f;
			thisFrameTime = 0;
			long time = getCurrentTime();
			Display.update();
			//System.out.println("UPDATE SCREEN TIME:" + AppManager.getCurrentTime() + "-" + time + "=" + (AppManager.getCurrentTime()-time));
		}
	}
	
	/**
	 * Get the last frame's time.
	 */
	public static float getUpdateTimeSeconds() {
		return delta;
	}
	
	public static float getFrameTimeSeconds(){
		return lastFrameTime;
	}
	/**
	 * Close the app. Cleans up all the data stored in the loader, renderer and buffers. Destroys the display.
	 */
	public static void closeApp() {
		loader.cleanUp();
		masterRenderer.cleanUp();
		guiRenderer.cleanUp();
		textureTopLeftBuffer.cleanUp();
		textureTopRightBuffer.cleanUp();
		textureBottomLeftBuffer.cleanUp();
		textureBottomRightBuffer.cleanUp();
		Display.destroy();
	}
	
	public static boolean closeRequested(){
		return shouldClose;
	}
	
	//Current time in milliseconds
	public static long getCurrentTime() {
		return Sys.getTime()*1000/Sys.getTimerResolution();
	}

//	private static void renderForAutopilot(){
//		buffer.bindFrameBuffer();
//		planeRenderer.renderCubes(world, camera, 1, true);
//		terrainRenderer.render(terrainLoader.getTerrains(), camera);
//		imageBuffer.clear();
//		GL11.glReadPixels(0, 0, RESOLUTION_WIDTH, RESOLUTION_HEIGHT, GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, imageBuffer);
//		byte[] image_flipped = new byte[RESOLUTION_WIDTH*RESOLUTION_HEIGHT*3];
//		imageBuffer.get(image_flipped);
//		image = new byte[RESOLUTION_WIDTH*RESOLUTION_HEIGHT*3];
//		
//		for(int x = 0; x<RESOLUTION_WIDTH;x++){
//			for(int y = 0; y<RESOLUTION_HEIGHT;y++){
//				image[(x+RESOLUTION_WIDTH*y)*3] = image_flipped[(x+(RESOLUTION_HEIGHT-1-y)*RESOLUTION_WIDTH)*3];
//				image[(x+RESOLUTION_WIDTH*y)*3 + 1] = image_flipped[(x+(RESOLUTION_HEIGHT-1-y)*RESOLUTION_WIDTH)*3 + 1];
//				image[(x+RESOLUTION_WIDTH*y)*3 + 2] = image_flipped[(x+(RESOLUTION_HEIGHT-1-y)*RESOLUTION_WIDTH)*3 + 2];
//			}	
//		}
//		if(imageCopyRequested) {
//			imageCopy = image.clone();
//			imageCopyRequested = false;
//		}
//		
//		buffer.unbindCurrentFrameBuffer();
//	}
	
	private static void renderTestbedView() {
        GL11.glViewport(0, 0, Display.getWidth(), Display.getHeight());
        Drone drone = worldManager.getDrone(focussedDrone[0]);
		switch(cameraView) {
		case DroneView:
//			buffer.bindFrameBuffer();
//			GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, 0);
//			GL30.glBlitFramebuffer(
//				0, 0, RESOLUTION_WIDTH, RESOLUTION_HEIGHT,
//				0, 0, Display.getWidth(), Display.getHeight(),
//				GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
//			buffer.unbindCurrentFrameBuffer();
			Camera camera = new Camera(drone);
			masterRenderer.createPerspectiveProjectionMatrix();
			masterRenderer.renderScene(getAllEntities(false), terrainLoader.getTerrains(), camera);
			break;
		case OrthogonalViews:
			//textureTopBuffer.bindFrameBuffer();
			Camera orthoTopCamera = new Camera(new Vector3f(10000,1000,-10000), 0,-(float)Math.PI/2,0);
			masterRenderer.createOrthogonalProjectionMatrix(20000,20000);
			terrainLoader.updateTerrain(orthoTopCamera.getPosition());
			masterRenderer.renderScene(getAllEntities(true), terrainLoader.getTerrains(), orthoTopCamera);	
			//textureTopBuffer.unbindCurrentFrameBuffer();
			
			//textureSideBuffer.bindFrameBuffer();
			//Camera orthoSideCamera = new Camera(new Vector3f(drone.getPosition().x + 100,drone.getPosition().y,drone.getPosition().z), (float)Math.PI/2,0,0);
			//masterRenderer.renderScene(getAllEntities(true), terrainLoader.getTerrains(), orthoSideCamera);	
			//textureSideBuffer.unbindCurrentFrameBuffer();

			guiRenderer.render(getAllGuis());
			
			break;
		case ThirdPerson:
			renderThirdPerson(null, drone);
			break;
		case Custom:
			customCamera.move();
			customCamera.moveAroundDrone(drone);
			terrainLoader.updateTerrain(customCamera.getPosition());
			masterRenderer.createPerspectiveProjectionMatrix();
			masterRenderer.renderScene(getAllEntities(true), terrainLoader.getTerrains(), customCamera);
			break;
		case QuadraView:
			renderThirdPerson(textureTopLeftBuffer, worldManager.getDrone(focussedDrone[0]));
			renderThirdPerson(textureTopRightBuffer, worldManager.getDrone(focussedDrone[1]));
			renderThirdPerson(textureBottomLeftBuffer, worldManager.getDrone(focussedDrone[2]));
			renderThirdPerson(textureBottomRightBuffer, worldManager.getDrone(focussedDrone[3]));
			
			guiRenderer.render(guis);
		}
	}
	
	private static void showMessage(String message) {
		System.out.println();
		System.out.println("GAME OVER!");
	    EventQueue.invokeLater(new Runnable() {
	        @Override
	        public void run() {
	            int i = JOptionPane.showConfirmDialog(null, message + " Press 'Yes' to quit the app, or press 'No' to restart.");
	            if(i == JOptionPane.OK_OPTION) {
		            shouldClose = true;
		            mainFrame.dispose();
	            }
	            if (i == JOptionPane.NO_OPTION) {
	            	simStatus = SimulationStatus.RestartRequested;
	            }
	        }
	    });
	}
	
	private static void showErrorMessage(String message) {
    	simStatus = SimulationStatus.RestartRequested;
		System.out.println();
		System.out.println("GAME OVER!");
	    EventQueue.invokeLater(new Runnable() {
	        @Override
	        public void run() {
	        	JOptionPane.showMessageDialog(mainFrame,
	        		    message,
	        		    "ERROR",
	        		    JOptionPane.ERROR_MESSAGE);
	        }
	    });
	}

	private static void refreshInfo(int foccusedDrone) {
		deltaLastInfoUpdate += getUpdateTimeSeconds();
		Drone drone = worldManager.getDrone(focussedDrone[0]);
		if(deltaLastInfoUpdate >= 1.0/INFO_REFRESH_RATE) {
			mainFrame.updateOrientationLabels(drone.getHeading(), drone.getPitch(), drone.getRoll());
			mainFrame.updateVelocityLabels(drone.getPosition(), drone.getVelocity(), drone.getRelativeAngularVelocity());
			mainFrame.updateTime(getFrameTimeSeconds(), simulationTime);
			mainFrame.refreshPackTable();
			deltaLastInfoUpdate = 0;
		}
	}
	
	private static void reset(){
		worldManager.reset();
		simulationTime = 0;
		simStatus = SimulationStatus.Idle;
		Drone drone = worldManager.getDrone(0);
		mainFrame.updateOrientationLabels(drone.getHeading(), drone.getPitch(), drone.getRoll());
		mainFrame.updateVelocityLabels(drone.getPosition(), drone.getVelocity(), drone.getRelativeAngularVelocity());
		mainFrame.resetRunMenu();
	}
	
	public static void saveImage(File file) {
		int width = 200, height = 200;
		
		// Convert to image
		BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
		for (int i=0 ; i<height ; i++) {
			for (int j=0 ; j<width ; j++) {
				byte r = imageCopy[(i * width + j) * 3 + 0];
				byte g = imageCopy[(i * width + j) * 3 + 1];
				byte b = imageCopy[(i * width + j) * 3 + 2];
				int rgb = ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
				bufferedImage.setRGB(j, i, rgb);
			}
		}
		try {
			ImageIO.write(bufferedImage, "bmp", file);
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
	
	private static ArrayList<Entity> getAllEntities(boolean withDrones){
		ArrayList<Entity> entities = new ArrayList<>();
		for(Drone drone: worldManager.getDrones()){
			if(withDrones) entities.add(drone.getEntity());
			if(withDrones) entities.add(drone.getPropellerEntity());
			entities.add(drone.getShadowEntity());	
		}
		for(Airport airport: worldManager.getAirports()){
			entities.add(airport.getEntity());
		}
		for (Symbol symbol : worldManager.getSymbols()) {
			entities.add(symbol.getEntity());
		}
		monuments.forEach((monument)->entities.add(monument.getEntity()));
		
		for (Entry<PackageKey, entities.Package> entry : worldManager.getWaitingPackages().entrySet())
        {
            entities.Package pack=entry.getValue();
            Airport port=worldManager.getAirport(pack.getFromA());
            Vector3f rotation=new Vector3f(port.getRotation(),0,0);
            Vector3f position=new Vector3f(port.getPosition3D());
            switch(pack.getFromG()){
            case 0:
                position=port.getGate0Mid();
                break;
            case 1:
                position=port.getGate1Mid();
                break;
           
            }
            entities.add(pack.getEntity(position,rotation));
        }
   
		return entities;
	}
	
	private static ArrayList<GuiTexture> getAllGuis(){
		ArrayList<GuiTexture> guis = new ArrayList<>();
		guis.addAll(traject.getTraject(focussedDrone[0], 20000, 1));
		for(Drone drone: worldManager.getDrones()){
			if(drone.getID()==focussedDrone[0])
				guis.add(drone.getGuiTexture(20000, true));
			else {
				guis.add(drone.getGuiTexture(20000, false));
			}
		}
		guis.addAll(worldManager.getPackageGuis(20000));
		return guis;
	}
	
	public static void setModule(AutopilotModule module){
		AppManager.module = module;
		worldManager.setAutopilotModule(module);
	}
	
	public static void checkInputs(){
		if(Keyboard.isKeyDown(Keyboard.KEY_DOWN)){
			if(!changingDrone){
				focussedDrone[0] = Math.floorMod((focussedDrone[0] - 1), worldManager.getDrones().size());
			}
			changingDrone = true;
		} else if (Keyboard.isKeyDown(Keyboard.KEY_UP)){
			if(!changingDrone){
				focussedDrone[0] = Math.floorMod((focussedDrone[0] + 1), worldManager.getDrones().size());
			}
			changingDrone = true;
		} else {
			changingDrone = false;
		}
	}
	
	private static void renderThirdPerson(TextureFrameBuffer buffer, Drone drone){
		if(buffer!=null) buffer.bindFrameBuffer();
		Camera quadraCamera = new Camera(
				Vector3f.add(drone.getPosition(),
				new Vector3f((float) (cameraDistance*Math.sin(drone.getHeading())),
						0,(float) (cameraDistance*Math.cos(drone.getHeading()))),null),
				drone.getHeading(), 0, 0);
		terrainLoader.updateTerrain(customCamera.getPosition());
		masterRenderer.createPerspectiveProjectionMatrix();
		masterRenderer.renderScene(getAllEntities(true), terrainLoader.getTerrains(), quadraCamera);
		if(buffer!=null) buffer.unbindCurrentFrameBuffer();
	}
	
	public static int[] getFocussedDrones(){
		return focussedDrone;
	}
	
	public static void setFocussedDrones(int[] drones) {
		focussedDrone = drones;
	}
}
