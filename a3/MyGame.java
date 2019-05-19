package a3;

import static ray.rage.scene.SkeletalEntity.EndType.LOOP;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.*;
import java.util.Iterator;
import java.util.Random;
import java.util.UUID;
import java.util.Vector;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import myGameEngine.*;
import ray.audio.AudioManagerFactory;
import ray.audio.AudioResource;
import ray.audio.AudioResourceType;
import ray.audio.IAudioManager;
import ray.audio.Sound;
import ray.audio.SoundType;
import ray.input.GenericInputManager;
import ray.input.InputManager;
import ray.input.action.AbstractInputAction;
import ray.input.action.Action;
import ray.networking.IGameConnection.ProtocolType;
import ray.physics.PhysicsEngine;
import ray.physics.PhysicsEngineFactory;
import ray.physics.PhysicsObject;
import ray.rage.Engine;
import ray.rage.asset.animation.Animation;
import ray.rage.asset.material.Material;
import ray.rage.asset.texture.Texture;
import ray.rage.asset.texture.TextureManager;
import ray.rage.game.Game;
import ray.rage.game.VariableFrameRateGame;
import ray.rage.rendersystem.*;
import ray.rage.rendersystem.Renderable.*;
import ray.rage.rendersystem.gl4.GL4RenderSystem;
import ray.rage.rendersystem.shader.GpuShaderProgram;
import ray.rage.rendersystem.states.*;
import ray.rage.rendersystem.states.TextureState.WrapMode;
import ray.rage.scene.Camera.Frustum.Projection;
import ray.rage.scene.SkeletalEntity.EndType;
import ray.rage.scene.*;
import ray.rage.scene.controllers.RotationController;
import ray.rage.util.BufferUtil;
import ray.rage.util.Configuration;
import ray.rml.Angle;
import ray.rml.Degreef;
import ray.rml.Matrix4;
import ray.rml.Matrix4f;
import ray.rml.Vector3;
import ray.rml.Vector3f;

public class MyGame extends VariableFrameRateGame {
	private String serverAddress;
	private int serverPort;
	private ProtocolType serverProtocol;
	private ProtocolClient protClient;
	private boolean isClientConnected;
	private Vector<UUID> gameObjectsToRemove;
	private boolean lightTransfer = true;

	private SceneNode player1N, water1N, tessN;
	private boolean checkWater = true;

	private final static String GROUND_E = "TessE";
	private final static String GROUND_N = "TessN";

	private Animation WalkAnimation;
	private SceneNode ballNode;
	private PhysicsEngine physicsEng;
	private PhysicsObject ballPhysObj, gndPlaneP;
	private boolean running = true;

	// to minimize variable allocation in update()
	GL4RenderSystem rs;
	float elapsTime = 0.0f;
	float dis = 0.0f;
	String elapsTimeStr, scoreStr, dispStr, numStr, meterStr;
	int elapsTimeSec, score = 0, planetNum = 3, foundNum = 0;

	private InputManager im;
	static Game game;
	private Action moveForwardAction, moveLeftAction, moveRightAction, moveBackAction;
	private Action moveAction;
	private Action leftRightRotateAction, upDownRotateAction;
	private Action moveLeftRightAction, moveForwardBackAction;
	private Action rotateLeftAction, rotateRightAction, rotateUpAction, rotateDownAction;
	private Action quitGameAction;
	private Action toggleModeAction;
	private static final String SKYBOX_NAME = "SkyBox";
	 private float speed = 0.02f;
//	private float speed = 0.5f;

	private int currentAction;
	private SkeletalEntity npc, player1SE, ghost1SE;
	private int test = 0;

	private ScriptEngine jsEngine;
	private Action colorAction;
	private File scriptFile1;

	// Value 1 for character 1 and value 2 for character 2
	private int playerModel = -1;
	private int ghostModel = -1;

	private boolean playerRun = false;
	private boolean ghostRun = false;
	private boolean hasGhost = false;

	private float meters = 0.0f;

	Tessellation tessE;

	private float movemt = 0.01f;

	private boolean bigger = false;

	IAudioManager audioMgr;
	Sound oceanSound, hereSound, wolfSound;

	public MyGame(String serverAddr, int sPort, String protocol) {
		super();
		this.serverAddress = serverAddr;
		this.serverPort = sPort;
		if (protocol.equals("UDP")) {
			this.serverProtocol = ProtocolType.UDP;
		}
		if (protocol.equals("TCP")) {
			this.serverProtocol = ProtocolType.TCP;
		}

		// this.serverProtocol = ProtocolType.TCP;

		this.initNetwork();
	}

	public static void main(String[] args) {
		game = new MyGame(args[0], Integer.parseInt(args[1]), args[2]);

		try {
			game.startup();
			game.run();
		} catch (Exception e) {
			e.printStackTrace(System.err);
		} finally {
			game.shutdown();
			game.exit();
		}
	}

	public void setLightTransfer(boolean newT) {
		this.lightTransfer = newT;
	}

	public boolean getLightTransfer() {
		return this.lightTransfer;
	}

	public void setPlayerModel(int model) {
		this.playerModel = model;
		System.out.println("----------------------------setting-----");
	}

	public int getPlayerModel() {
		return this.playerModel;
	}

	public void setGhostModel(int model) {
		this.ghostModel = model;
	}

	public int getGhostModel() {
		return this.ghostModel;
	}

	protected void initNetwork() {
		gameObjectsToRemove = new Vector<UUID>();
		isClientConnected = false;
		try {
			protClient = new ProtocolClient(InetAddress.getByName(serverAddress), serverPort, serverProtocol, this);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (protClient == null) {
			System.out.println("missing protocol host");
		} else {
			protClient.sendJoinMessage();
		}
	}

	@Override
	protected void setupWindow(RenderSystem rs, GraphicsEnvironment ge) {
		rs.createRenderWindow(new DisplayMode(1000, 700, 24, 60), false);
		GraphicsDevice gd = ge.getDefaultScreenDevice();
		DisplayAvatarSettings dsd = new DisplayAvatarSettings(ge.getDefaultScreenDevice(), this);
		dsd.showIt();
		RenderWindow rw = rs.createRenderWindow(dsd.getSelectedDisplayMode(), dsd.isFullScreenModeSelected());
	}

	private void initHeightMap(SceneManager sm) {
		// create image-based height map
		tessE = sm.createTessellation("TessE", 6);

		tessE.setSubdivisions(5000f);

		tessN = sm.getRootSceneNode().createChildSceneNode("TessN");
		tessN.attachObject(tessE);

		tessN.scale(100, 200, 100);
		tessE.setHeightMap(this.getEngine(), "maze.png");
		tessE.setTexture(this.getEngine(), "sand.jpg");

		tessE.getTextureState().setWrapMode(WrapMode.REPEAT_MIRRORED);
		// tessE.setHeightMapTiling(4);
		// tessE.setTextureTiling(4);
		// tessE.setNormalMapTiling(4);
	}

	public void initAudio(SceneManager sm) {
		AudioResource resource1, resource2, resource3;
		audioMgr = AudioManagerFactory.createAudioManager("ray.audio.joal.JOALAudioManager");

		if (!audioMgr.initialize()) {
			System.out.println("Audio Manager failed to initialize!");
			return;
		}

		resource1 = audioMgr.createAudioResource("farm.wav", AudioResourceType.AUDIO_SAMPLE);
		resource2 = audioMgr.createAudioResource("farm.wav", AudioResourceType.AUDIO_SAMPLE);
		resource3 = audioMgr.createAudioResource("wolfBark.wav", AudioResourceType.AUDIO_SAMPLE);

		hereSound = new Sound(resource1, SoundType.SOUND_EFFECT, 100, true);
		oceanSound = new Sound(resource2, SoundType.SOUND_EFFECT, 100, true);
		wolfSound = new Sound(resource3, SoundType.SOUND_EFFECT, 100, true);

		hereSound.initialize(audioMgr);
		oceanSound.initialize(audioMgr);
		wolfSound.initialize(audioMgr);
		hereSound.setMaxDistance(10.0f);
		hereSound.setMinDistance(0.5f);
		hereSound.setRollOff(5.0f);
		oceanSound.setMaxDistance(0.2f);
		oceanSound.setMinDistance(0.05f);
		oceanSound.setRollOff(5.0f);
		wolfSound.setMaxDistance(0.03f);
		wolfSound.setMinDistance(0.01f);
		wolfSound.setRollOff(5.0f);
		// wolfSound.setVolume(1000*wolfSound.getVolume());

		SceneNode leafN = sm.getSceneNode("leaf1Node");
		hereSound.setLocation(leafN.getWorldPosition());
		oceanSound.setLocation(Vector3f.createFrom(0, 0, 0));
		// wolfSound.setLocation(this.getEngine().getSceneManager().getSceneNode("NPC1Node").getWorldPosition());
		setEarParameters(sm);
		hereSound.play();
		oceanSound.play();
		// wolfSound.play();
	}

	public void setEarParameters(SceneManager sm) {
		SceneNode playerNode = sm.getSceneNode("Player1Node");
		Vector3 avDir = playerNode.getWorldForwardAxis();
		audioMgr.getEar().setLocation(playerNode.getWorldPosition());
		audioMgr.getEar().setOrientation(avDir, Vector3f.createFrom(0, 1, 0));
	}

	private void initPhysicsSystem() {
		String engine = "ray.physics.JBullet.JBulletPhysicsEngine";
		float[] gravity = { 0, -9.8f, 0 };

		physicsEng = PhysicsEngineFactory.createPhysicsEngine(engine);
		physicsEng.initSystem();
		physicsEng.setGravity(gravity);

	}

	private void createRagePhysicsWorld() {
		float mass = 1.0f;
		float up[] = { 0, 1, 0 };
		double[] temptf;

		temptf = toDoubleArray(ballNode.getLocalTransform().toFloatArray());
		ballPhysObj = physicsEng.addSphereObject(physicsEng.nextUID(), mass, temptf, 2.0f);

		ballPhysObj.setBounciness(1.0f);
		ballNode.setPhysicsObject(ballPhysObj);

		temptf = toDoubleArray(tessN.getLocalTransform().toFloatArray());
		gndPlaneP = physicsEng.addStaticPlaneObject(physicsEng.nextUID(), temptf, up, 0.0f);

		gndPlaneP.setBounciness(1.0f);
		// tessN.scale(3f,0.05f,3f);
		// tessN.setLocalPosition(0,5,0);
		tessN.setPhysicsObject(gndPlaneP);
	}

	public void playerDoTheRun() {
		player1SE.stopAnimation();
		player1SE.playAnimation("RunAnimation", 0.5f, LOOP, 0);
	}

	@Override
	protected void setupScene(Engine eng, SceneManager sm) throws IOException, NullPointerException {
		im = new GenericInputManager();

		setupInputs();

		initHeightMap(sm);

		SceneNode worldObjectN = sm.getRootSceneNode().createChildSceneNode("WorldObjectNode");

		// Ball
		Entity ballEntity = sm.createEntity("ball", "earth.obj");
		ballNode = worldObjectN.createChildSceneNode("BallNode");
		ballNode.attachObject(ballEntity);
		ballNode.scale(0.1f, 0.1f, 0.1f);
		ballNode.setLocalPosition(0, 100.0f, 0);

		initPhysicsSystem();
		createRagePhysicsWorld();

		// player node
		SceneNode playerN = worldObjectN.createChildSceneNode("PlayerNode");

		player1SE = sm.createSkeletalEntity("player1Av", "wolf.rkm", "wolf.rks");

		Texture tex;
		if (this.getPlayerModel() == 1) {
			tex = sm.getTextureManager().getAssetByPath("water1.jpg");
		} else {
			tex = sm.getTextureManager().getAssetByPath("monster.jpg");
		}
		TextureState tstate = (TextureState) sm.getRenderSystem().createRenderState(RenderState.Type.TEXTURE);
		tstate.setTexture(tex);
		player1SE.setRenderState(tstate);

		player1N = playerN.createChildSceneNode("Player1Node");
		player1N.attachObject(player1SE);
		player1N.scale(0.12f, 0.12f, 0.12f);
		player1N.setLocalPosition(Vector3f.createFrom(-22.13f, 3.525f, 5.747f));

		player1SE.loadAnimation("RunAnimation", "WolfRun.rka");

		if (!this.playerRun) {
			System.out.println("pLAYER RUN-------------------------------");
			this.playerDoTheRun();
			this.playerRun = true;
		}

		Camera camera = sm.getCamera("MainCamera");
		SceneNode cameraNode = sm.getRootSceneNode().createChildSceneNode("MainCameraNode");
		if (this.getPlayerModel() == 1) {
			cameraNode.moveUp(3.5f);
			cameraNode.moveForward(3.0f);
		} else {
			cameraNode.moveUp(3.5f);
			cameraNode.moveForward(3.0f);
		}

		player1N.attachChild(cameraNode);
		cameraNode.attachObject(camera);

		// NPC node
		SceneNode npcN = worldObjectN.createChildSceneNode("NPCNode");
		initNPC(sm, npcN, tessE);

		// nature node
		SceneNode natureN = worldObjectN.createChildSceneNode("NatureNode");

		// Planet 3
		Entity planet3E = sm.createEntity("MyPlanet3", "earth.obj");
		planet3E.setPrimitive(Primitive.TRIANGLES);

		TextureManager tm3 = eng.getTextureManager();
		Texture redTexture = tm3.getAssetByPath("red.jpeg");
		RenderSystem rs3 = sm.getRenderSystem();
		TextureState state3 = (TextureState) rs3.createRenderState(RenderState.Type.TEXTURE);
		state3.setTexture(redTexture);
		planet3E.setRenderState(state3);

		SceneNode planet3N = natureN.createChildSceneNode(planet3E.getName() + "Node");
		planet3N.setLocalPosition(0.0f, 20.0f, 0.0f);
		planet3N.setLocalScale(1.0f, 1.0f, 1.0f);
		planet3N.attachObject(planet3E);

		// Planet 3's rotation
		RotationController rc3 = new RotationController(Vector3f.createUnitVectorY(), 0.05f);
		rc3.addNode(planet3N);
		sm.addController(rc3);

		// Planet 3
		Entity planet33E = sm.createEntity("MyPlanet33", "earth.obj");
		planet33E.setPrimitive(Primitive.TRIANGLES);

		TextureManager tm33 = eng.getTextureManager();
		Texture redTexture3 = tm33.getAssetByPath("red.jpeg");
		RenderSystem rs33 = sm.getRenderSystem();
		TextureState state33 = (TextureState) rs33.createRenderState(RenderState.Type.TEXTURE);
		state33.setTexture(redTexture3);
		planet33E.setRenderState(state33);

		SceneNode planet33N = planet3N.createChildSceneNode(planet3E.getName() + "3Node");
		planet33N.setLocalPosition(5.0f, 20.0f,
				5.0f);
		planet33N.setLocalScale(0.1f, 0.1f, 0.1f);
		planet33N.attachObject(planet33E);

		// water node
		SceneNode waterN = natureN.createChildSceneNode("waterNode");

		// water 1 node
		Entity water1E = sm.createEntity("water1", "water.obj");
		water1E.setPrimitive(Primitive.TRIANGLES);

		water1N = waterN.createChildSceneNode("water1Node");

		water1N.setLocalPosition(Vector3f.createFrom(0, 2.5f, 0));

		TextureManager tm = eng.getTextureManager();
		Texture texture = tm.getAssetByPath("water1.jpg");
		RenderSystem rs = sm.getRenderSystem();
		TextureState state = (TextureState) rs.createRenderState(RenderState.Type.TEXTURE);
		state.setTexture(texture);
		water1E.setRenderState(state);

		water1N.scale(1f, 0.08f, 1f);

		water1N.attachObject(water1E);

		// SceneNode flagN = natureN.createChildSceneNode("flagNode");
		// // water 1 node
		// Entity flag1E = sm.createEntity("flag1", "darkcastle.obj");
		// flag1E.setPrimitive(Primitive.TRIANGLES);
		//
		// SceneNode flag1N = flagN.createChildSceneNode("tower1Node");
		//
		// flag1N.setLocalPosition(Vector3f.createFrom(-8.234f, 4.0f, -36.2575f));
		//
		// // TextureManager tm1 = eng.getTextureManager();
		// // Texture texture1 = tm1.getAssetByPath("Difuse.png");
		// // RenderSystem rs1 = sm.getRenderSystem();
		// // TextureState state1 = (TextureState)
		// // rs1.createRenderState(RenderState.Type.TEXTURE);
		// // state1.setTexture(texture1);
		// // flag1E.setRenderState(state1);
		// //
		// flag1N.scale(0.03f,0.03f, 0.03f);
		//
		// flag1N.attachObject(flag1E);

		SceneNode bambooN = natureN.createChildSceneNode("bambooNode");
		// bamboo 1 node
		Entity bamboo1E = sm.createEntity("bamboo1", "bamboo.obj");
		bamboo1E.setPrimitive(Primitive.TRIANGLES);

		SceneNode bamboo1N = bambooN.createChildSceneNode("bamboo1Node");

		bamboo1N.setLocalPosition(Vector3f.createFrom(16.85f, 2.0f, -10.805f));

		bamboo1N.scale(0.1f, 0.3f, 0.1f);

		bamboo1N.attachObject(bamboo1E);

		initShip(natureN, sm);
		initHouse(natureN, sm);
		initCart(natureN, sm);
		initBridge(natureN, sm);
		initFence(natureN, sm);
		initLeaf(natureN, sm);
		initBarrel(natureN, sm);
		initDoor(natureN, sm);

		initLight();
		initSkybox();

		initAudio(sm);
	}

	private void initDoor(SceneNode natureN, SceneManager sm) throws IOException {
		SceneNode doorN = natureN.createChildSceneNode("doorNode");
		// door 1
		Entity door1E = sm.createEntity("door1", "door1.obj");
		door1E.setPrimitive(Primitive.TRIANGLES);

		SceneNode door1N = doorN.createChildSceneNode("door1Node");

		door1N.setLocalPosition(Vector3f.createFrom(-5.48f, 3.0f, 3.345f));

		TextureManager tm1 = this.getEngine().getTextureManager();
		Texture texture1 = tm1.getAssetByPath("WoodPlanksBare0051_3_M.jpg");
		RenderSystem rs1 = sm.getRenderSystem();
		TextureState state1 = (TextureState) rs1.createRenderState(RenderState.Type.TEXTURE);
		state1.setTexture(texture1);
		door1E.setRenderState(state1);

		door1N.scale(0.08f, 0.08f, 0.08f);

		door1N.attachObject(door1E);

		// door 2
		Entity door2E = sm.createEntity("door2", "door1.obj");
		door2E.setPrimitive(Primitive.TRIANGLES);

		SceneNode door2N = doorN.createChildSceneNode("door2Node");

		door2N.setLocalPosition(Vector3f.createFrom(-13.64f, 3.0f, -0.01f));

		TextureManager tm2 = this.getEngine().getTextureManager();
		Texture texture2 = tm2.getAssetByPath("WoodPlanksBare0051_3_M.jpg");
		RenderSystem rs2 = sm.getRenderSystem();
		TextureState state2 = (TextureState) rs2.createRenderState(RenderState.Type.TEXTURE);
		state2.setTexture(texture2);
		door2E.setRenderState(state2);

		door2N.scale(0.08f, 0.08f, 0.08f);

		door2N.attachObject(door2E);

		// door 3
		Entity door3E = sm.createEntity("door3", "door1.obj");
		door3E.setPrimitive(Primitive.TRIANGLES);

		SceneNode door3N = doorN.createChildSceneNode("door3Node");

		door3N.setLocalPosition(Vector3f.createFrom(-6.35f, 3.0f, 14.59f));

		TextureManager tm3 = this.getEngine().getTextureManager();
		Texture texture3 = tm3.getAssetByPath("WoodPlanksBare0051_3_M.jpg");
		RenderSystem rs3 = sm.getRenderSystem();
		TextureState state3 = (TextureState) rs3.createRenderState(RenderState.Type.TEXTURE);
		state3.setTexture(texture3);
		door3E.setRenderState(state3);

		door3N.scale(0.08f, 0.08f, 0.08f);
		door3N.yaw(Degreef.createFrom(90.0f));
		door3N.attachObject(door3E);

		// door 4
		Entity door4E = sm.createEntity("door4", "door1.obj");
		door4E.setPrimitive(Primitive.TRIANGLES);

		SceneNode door4N = doorN.createChildSceneNode("door4Node");

		door4N.setLocalPosition(Vector3f.createFrom(23.83f, 3.0f, 2.326f));

		TextureManager tm4 = this.getEngine().getTextureManager();
		Texture texture4 = tm4.getAssetByPath("WoodPlanksBare0051_3_M.jpg");
		RenderSystem rs4 = sm.getRenderSystem();
		TextureState state4 = (TextureState) rs4.createRenderState(RenderState.Type.TEXTURE);
		state4.setTexture(texture4);
		door4E.setRenderState(state4);

		door4N.scale(0.08f, 0.08f, 0.08f);
		door4N.attachObject(door4E);
	}

	private void initBarrel(SceneNode natureN, SceneManager sm) throws IOException {
		SceneNode barrelN = natureN.createChildSceneNode("barrelNode");
		// barrel 1 node
		Entity barrel1E = sm.createEntity("barrel1", "Barrel.obj");
		barrel1E.setPrimitive(Primitive.TRIANGLES);

		SceneNode barrel1N = barrelN.createChildSceneNode("barrel1Node");

		barrel1N.setLocalPosition(Vector3f.createFrom(-22.4f, 1.8f, 12.55f));

		TextureManager tm1 = this.getEngine().getTextureManager();
		Texture texture1 = tm1.getAssetByPath("Difuse.png");
		RenderSystem rs1 = sm.getRenderSystem();
		TextureState state1 = (TextureState) rs1.createRenderState(RenderState.Type.TEXTURE);
		state1.setTexture(texture1);
		barrel1E.setRenderState(state1);

		barrel1N.scale(0.3f, 0.3f, 0.3f);
		barrel1N.pitch(Degreef.createFrom(90.0f));

		barrel1N.attachObject(barrel1E);
	}

	private void initLeaf(SceneNode natureN, SceneManager sm) throws IOException {
		SceneNode leafN = natureN.createChildSceneNode("leafNode");

		// leaf 1
		Entity leaf1E = sm.createEntity("leaf1", "leaf.obj");
		leaf1E.setPrimitive(Primitive.TRIANGLES);

		SceneNode leaf1N = leafN.createChildSceneNode("leaf1Node");

		leaf1N.setLocalPosition(Vector3f.createFrom(-0.66f, 3.0f, 0.35f));

		TextureManager tm1 = this.getEngine().getTextureManager();
		Texture texture1 = tm1.getAssetByPath("leaf.png");
		RenderSystem rs1 = sm.getRenderSystem();
		TextureState state1 = (TextureState) rs1.createRenderState(RenderState.Type.TEXTURE);
		state1.setTexture(texture1);
		leaf1E.setRenderState(state1);

		leaf1N.scale(1.0f, 1.0f, 1.0f);

		leaf1N.attachObject(leaf1E);

		// leaf 2
		Entity leaf2E = sm.createEntity("leaf2", "leaf.obj");
		leaf2E.setPrimitive(Primitive.TRIANGLES);

		SceneNode leaf2N = leafN.createChildSceneNode("leaf2Node");

		leaf2N.setLocalPosition(Vector3f.createFrom(-3.22f, 3.0f, 9.15f));

		TextureManager tm2 = this.getEngine().getTextureManager();
		Texture texture2 = tm2.getAssetByPath("leaf.png");
		RenderSystem rs2 = sm.getRenderSystem();
		TextureState state2 = (TextureState) rs2.createRenderState(RenderState.Type.TEXTURE);
		state2.setTexture(texture2);
		leaf2E.setRenderState(state2);

		leaf2N.scale(1.0f, 1.0f, 1.0f);
		leaf2N.yaw(Degreef.createFrom(90.0f));

		leaf2N.attachObject(leaf2E);

		// leaf 3
		Entity leaf3E = sm.createEntity("leaf3", "leaf.obj");
		leaf3E.setPrimitive(Primitive.TRIANGLES);

		SceneNode leaf3N = leafN.createChildSceneNode("leaf3Node");

		leaf3N.setLocalPosition(Vector3f.createFrom(1.55f, 3.0f, 16.18f));

		TextureManager tm3 = this.getEngine().getTextureManager();
		Texture texture3 = tm3.getAssetByPath("leaf.png");
		RenderSystem rs3 = sm.getRenderSystem();
		TextureState state3 = (TextureState) rs3.createRenderState(RenderState.Type.TEXTURE);
		state3.setTexture(texture3);
		leaf3E.setRenderState(state3);

		leaf3N.scale(0.8f, 0.8f, 0.8f);
		leaf3N.yaw(Degreef.createFrom(275.0f));

		leaf3N.attachObject(leaf3E);
	}

	private void initFence(SceneNode natureN, SceneManager sm) throws IOException {
		// fence node
		SceneNode fenceN = natureN.createChildSceneNode("fenceNode");

		// fence 1 node
		Entity fence1E = sm.createEntity("fence1", "fenceFinal.obj");
		fence1E.setPrimitive(Primitive.TRIANGLES);

		SceneNode fence1N = fenceN.createChildSceneNode("fence1Node");

		fence1N.setLocalPosition(10.15f, 3.2f, -4.79f);

		TextureManager tm = this.getEngine().getTextureManager();
		Texture texture = tm.getAssetByPath("textureUV.png");
		RenderSystem rs = sm.getRenderSystem();
		TextureState state = (TextureState) rs.createRenderState(RenderState.Type.TEXTURE);
		state.setTexture(texture);
		fence1E.setRenderState(state);

		fence1N.scale(0.2f, 0.2f, 0.2f);

		fence1N.attachObject(fence1E);
	}

	private void initBridge(SceneNode natureN, SceneManager sm) throws IOException {
		SceneNode bridgeN = natureN.createChildSceneNode("bridgeNode");

		Entity bridge1E = sm.createEntity("bridge1", "bridge.obj");
		bridge1E.setPrimitive(Primitive.TRIANGLES);

		SceneNode bridge1N = bridgeN.createChildSceneNode("bridge1Node");

		bridge1N.setLocalPosition(Vector3f.createFrom(15.47f, 3.0f, -6.5f));

		bridge1N.scale(0.48f, 0.48f, 0.48f);
		bridge1N.yaw(Degreef.createFrom(50.0f));

		bridge1N.attachObject(bridge1E);
	}

	private void initCart(SceneNode natureN, SceneManager sm) throws IOException {
		SceneNode cartN = natureN.createChildSceneNode("cartNode");
		// multiple cart node
		SceneNode cartNode = cartN.createChildSceneNode("CartNode");
		cartNode.setLocalPosition(13.2f, 3.1f, -1.3f);
		cartNode.scale(0.2f, 0.2f, 0.2f);

		cartNode.attachObject(sm.createEntity("Cart", "Wooden.obj"));

	}

	private void initHouse(SceneNode natureN, SceneManager sm) throws IOException {
		SceneNode houseN = natureN.createChildSceneNode("houseNode");

		// multiple house node
		SceneNode houseNode = houseN.createChildSceneNode("HouseNode");
		houseNode.setLocalPosition(11.04f, 3.0f, -1.5f);
		houseNode.scale(0.2f, 0.2f, 0.2f);
		houseNode.attachObject(sm.createEntity("House", "house2.obj"));

	}

	private void initShip(SceneNode natureN, SceneManager sm) throws IOException {
		SceneNode shipN = natureN.createChildSceneNode("shipNode");

		// ship1 Node
		Entity ship1E = sm.createEntity("ship1", "ship1.obj");
		ship1E.setPrimitive(Primitive.TRIANGLES);

		SceneNode ship1N = shipN.createChildSceneNode("ship1Node");

		ship1N.setLocalPosition(Vector3f.createFrom(5.5f, 3.0f, -3.7f));

		TextureManager ship1 = this.getEngine().getTextureManager();
		Texture texture1 = ship1.getAssetByPath("5672.jpg");
		RenderSystem rs1 = sm.getRenderSystem();
		TextureState state1 = (TextureState) rs1.createRenderState(RenderState.Type.TEXTURE);
		state1.setTexture(texture1);
		ship1E.setRenderState(state1);

		ship1N.scale(0.5f, 0.5f, 0.5f);
		ship1N.attachObject(ship1E);

		// ship2 Node
		Entity ship2E = sm.createEntity("ship2", "ship1.obj");
		ship2E.setPrimitive(Primitive.TRIANGLES);

		SceneNode ship2N = shipN.createChildSceneNode("ship2Node");

		ship2N.setLocalPosition(Vector3f.createFrom(16.0f, 3.0f, 29.2f));

		TextureManager ship2 = this.getEngine().getTextureManager();
		Texture texture2 = ship2.getAssetByPath("5672.jpg");
		RenderSystem rs2 = sm.getRenderSystem();
		TextureState state2 = (TextureState) rs2.createRenderState(RenderState.Type.TEXTURE);
		state2.setTexture(texture2);
		ship2E.setRenderState(state2);

		ship2N.scale(0.5f, 0.5f, 0.5f);
		ship2N.attachObject(ship2E);

		// ship3 Node
		Entity ship3E = sm.createEntity("ship3", "ship1.obj");
		ship3E.setPrimitive(Primitive.TRIANGLES);

		SceneNode ship3N = shipN.createChildSceneNode("ship3Node");

		ship3N.setLocalPosition(Vector3f.createFrom(10.15f, 3.0f, 25.93f));

		TextureManager ship3 = this.getEngine().getTextureManager();
		Texture texture3 = ship3.getAssetByPath("5672.jpg");
		RenderSystem rs3 = sm.getRenderSystem();
		TextureState state3 = (TextureState) rs3.createRenderState(RenderState.Type.TEXTURE);
		state3.setTexture(texture3);
		ship3E.setRenderState(state3);

		ship3N.scale(0.5f, 0.5f, 0.5f);
		ship3N.yaw(Degreef.createFrom(30.0f));
		ship3N.attachObject(ship3E);
	}

	private double[] toDoubleArray(float[] arr) {
		if (arr == null) {
			return null;
		}
		int n = arr.length;
		double[] ret = new double[n];
		for (int i = 0; i < n; i++) {
			ret[i] = (double) arr[i];
		}
		return ret;
	}

	private void runScript(File scriptFile) {
		try {
			FileReader fileReader = new FileReader(scriptFile);
			jsEngine.eval(fileReader);
			fileReader.close();
		} catch (FileNotFoundException e1) {
			System.out.println(scriptFile + " not found " + e1);
		} catch (IOException e2) {
			System.out.println("IO problem with " + scriptFile + e2);
		} catch (ScriptException e3) {
			System.out.println("Script Exception in " + scriptFile + e3);
		} catch (NullPointerException e4) {
			System.out.println("Null pointer exception reading " + scriptFile + e4);
		}
	}

	protected void initLight() {
		// light
		this.getEngine().getSceneManager().getAmbientLight().setIntensity(new Color(.1f, .1f, .1f));

		Light plight = this.getEngine().getSceneManager().createLight("testLamp1", Light.Type.POINT);
		plight.setAmbient(java.awt.Color.lightGray);
		// plight.setAmbient(new Color(.3f, .3f, .3f));
		plight.setDiffuse(new Color(.7f, .7f, .7f));
		plight.setSpecular(new Color(1.0f, 1.0f, 1.0f));
		plight.setRange(5f);

		SceneNode plightNode = this.getEngine().getSceneManager().getRootSceneNode().createChildSceneNode("plightNode");
		plightNode.attachObject(plight);
		
		
		Light plight1 = this.getEngine().getSceneManager().createLight("testLamp2", Light.Type.POINT);
		plight1.setAmbient(java.awt.Color.lightGray);
		// plight.setAmbient(new Color(.3f, .3f, .3f));
		plight1.setDiffuse(new Color(.7f, .7f, .7f));
		plight1.setSpecular(new Color(1.0f, 1.0f, 1.0f));
		plight1.setRange(1f);

		SceneNode plight1Node = this.getEngine().getSceneManager().getRootSceneNode().createChildSceneNode("plight1Node");
//		plight1Node.attachObject(plight1);
	}

	protected void initSkybox() throws IOException {
		// set up sky box
		Configuration conf = this.getEngine().getConfiguration();
		TextureManager tmq = getEngine().getTextureManager();
		tmq.setBaseDirectoryPath(conf.valueOf("assets.skyboxes.path"));
		Texture front = tmq.getAssetByPath("front.jpg");
		Texture back = tmq.getAssetByPath("back.jpg");
		Texture left = tmq.getAssetByPath("left.jpg");
		Texture right = tmq.getAssetByPath("right.jpg");
		Texture top = tmq.getAssetByPath("up.jpg");
		Texture bottom = tmq.getAssetByPath("down.jpg");

		tmq.setBaseDirectoryPath(conf.valueOf("assets.textures.path"));

		AffineTransform xform = new AffineTransform();
		xform.translate(0, front.getImage().getHeight());
		xform.scale(1d, -1d);

		front.transform(xform);
		back.transform(xform);
		left.transform(xform);
		right.transform(xform);
		top.transform(xform);
		bottom.transform(xform);

		SkyBox sb = this.getEngine().getSceneManager().createSkyBox(SKYBOX_NAME);
		sb.setTexture(front, SkyBox.Face.FRONT);
		sb.setTexture(back, SkyBox.Face.BACK);
		sb.setTexture(left, SkyBox.Face.LEFT);
		sb.setTexture(right, SkyBox.Face.RIGHT);
		sb.setTexture(top, SkyBox.Face.TOP);
		sb.setTexture(bottom, SkyBox.Face.BOTTOM);
		this.getEngine().getSceneManager().setActiveSkyBox(sb);
	}

	protected void setupInputs() {
		im = new GenericInputManager();
		Camera c = this.getEngine().getSceneManager().getCamera("MainCamera");
		String kbName = im.getKeyboardName();
		String gpName = im.getFirstGamepadName();

		// build some action objects for doing things in response to user input
		quitGameAction = new QuitGameAction(this);
		moveForwardAction = new MoveForwardAction(c, this, protClient);
		moveBackAction = new MoveBackAction(c, this, protClient);
		moveLeftAction = new MoveLeftAction(c, this, protClient);
		moveRightAction = new MoveRightAction(c, this, protClient);

		moveAction = new MoveAction(this, protClient);
		leftRightRotateAction = new LeftRightRotationAction(this);
		upDownRotateAction = new UpDownRotationAction(this);
		moveLeftRightAction = new MoveLeftRightAction(this, protClient);
		moveForwardBackAction = new MoveForwardBackAction(this, protClient);
		rotateLeftAction = new RotateLeftAction(this, protClient);
		rotateRightAction = new RotateRightAction(this, protClient);
		rotateUpAction = new RotateUpAction(this);
		rotateDownAction = new RotateDownAction(this);

		// attach the action objects to keyboard components
//		// use Key ESCAPE to quit game
//		im.associateAction(kbName, net.java.games.input.Component.Identifier.Key.ESCAPE, quitGameAction,
//				InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
//		// use Key W to move camera forward
//		im.associateAction(kbName, net.java.games.input.Component.Identifier.Key.W, moveForwardAction,
//				InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
//		// use Key S to move camera backward
//		im.associateAction(kbName, net.java.games.input.Component.Identifier.Key.S, moveBackAction,
//				InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
//		// use Key A to move camera left
//		im.associateAction(kbName, net.java.games.input.Component.Identifier.Key.A, moveLeftAction,
//				InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
//		// use Key D to move camera right
//		im.associateAction(kbName, net.java.games.input.Component.Identifier.Key.D, moveRightAction,
//				InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
//		// use Key SPACE to toggle between being ON/OFF player
//		im.associateAction(kbName, net.java.games.input.Component.Identifier.Key.SPACE, toggleModeAction,
//				InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
		// use Key LEFT to rotate camera/player around its V vertical axis (left)
		im.associateAction(kbName, net.java.games.input.Component.Identifier.Key.LEFT, rotateLeftAction,
				InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		// use Key RIGHT to rotate camera/player around its V vertical axis (right)
		im.associateAction(kbName, net.java.games.input.Component.Identifier.Key.RIGHT, rotateRightAction,
				InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
//		// use Key UP to rotate camera/player around its U side axis (up)
//		im.associateAction(kbName, net.java.games.input.Component.Identifier.Key.UP, rotateUpAction,
//				InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
//		// use Key DOWN to rotate camera/player around its U side axis (down)
//		im.associateAction(kbName, net.java.games.input.Component.Identifier.Key.DOWN, rotateDownAction,
//				InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		// attach the action objects to gamepad components
//		// use Button _2 to quit game
//		im.associateAction(gpName, net.java.games.input.Component.Identifier.Button._2, quitGameAction,
//				InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
//		// use X to move left/right
//		im.associateAction(gpName, net.java.games.input.Component.Identifier.Axis.X, moveLeftRightAction,
//				InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
//		// use Y to move forward/back
//		im.associateAction(gpName, net.java.games.input.Component.Identifier.Axis.Y, moveForwardBackAction,
//				InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
//		// use Button _1 to toggle mode between camera/player
//		im.associateAction(gpName, net.java.games.input.Component.Identifier.Button._1, toggleModeAction,
//				InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
		// use RX to rotate
		im.associateAction(gpName, net.java.games.input.Component.Identifier.Axis.RX, leftRightRotateAction,
				InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
//		// use RY to rotate
//		im.associateAction(gpName, net.java.games.input.Component.Identifier.Axis.RY, upDownRotateAction,
//				InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		ScriptEngineManager factory = new ScriptEngineManager();
		java.util.List<ScriptEngineFactory> list = factory.getEngineFactories();
		jsEngine = factory.getEngineByName("js");

		scriptFile1 = new File("UpdateLightColor.js");
		this.runScript(scriptFile1);
		colorAction = new ColorAction(jsEngine, scriptFile1, this);
		im.associateAction(kbName, net.java.games.input.Component.Identifier.Key.SPACE, colorAction,
				InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
	}

	@Override
	protected void update(Engine engine) {
		// build and set HUD
		rs = (GL4RenderSystem) engine.getRenderSystem();
		elapsTime += engine.getElapsedTimeMillis();
		meters = this.speed * elapsTime;
		meterStr = Integer.toString(Math.round(meters / 100.0f));
		dispStr = "Passed = " + meterStr;
		rs.setHUD(dispStr, 15, 15);

		// update the animation
		player1SE.update();
		if (this.hasGhost) {
			ghost1SE.update();
		}

		for (int i = 0; i < 10; i++) {
			SkeletalEntity npc1SE = (SkeletalEntity) engine.getSceneManager().getEntity("NPC1Av" + i);
			npc1SE.update();
		}

		// tell the input manager to process the inputs
		im.update(elapsTime);

		processNetworking(elapsTime);

		float time = engine.getElapsedTimeMillis();
		if (running) {
			Matrix4 mat;
			physicsEng.update(time);
			for (SceneNode s : engine.getSceneManager().getSceneNodes()) {
				if (s.getPhysicsObject() != null) {
					mat = Matrix4f.createFrom(toFloatArray(s.getPhysicsObject().getTransform()));
					s.setLocalPosition(mat.value(0, 3), mat.value(1, 3), mat.value(2, 3));
				}
			}

			Vector3 loc = ballNode.getLocalPosition();
			ballNode.setLocalPosition(loc.x() + 0.1f, loc.y(), loc.z());

		}

		player1N.moveForward(speed);
		updateVerticalPosition();
		protClient.sendMoveMessages(player1N.getWorldPosition());
		if (player1N.getWorldPosition().y() < 2.0f) {
			System.out.println("died.........................");

			player1N.setLocalPosition(Vector3f.createFrom(-22.13f, 3.525f, 5.747f));
		}

		SceneManager sm = this.getEngine().getSceneManager();

		oceanSound.setLocation(Vector3f.createFrom(0, 0, 0));
		hereSound.setLocation(Vector3f.createFrom(0, 0, 0));
		// wolfSound.setLocation(this.getEngine().getSceneManager().getSceneNode("NPC1Node").getWorldPosition());

		this.setEarParameters(sm);
	}

	private float[] toFloatArray(double[] arr) {
		if (arr == null) {
			return null;
		}
		int n = arr.length;
		float[] ret = new float[n];
		for (int i = 0; i < n; i++) {
			ret[i] = (float) arr[i];
		}
		return ret;
	}

	protected void processNetworking(float elapsTime) {
		// process packets received by the client from the server
		if (protClient != null) {
			protClient.processPackets();
		}

		// remove ghost avatars for players who have left the game
		if (gameObjectsToRemove != null) {
			Iterator<UUID> it = gameObjectsToRemove.iterator();
			while (it.hasNext()) {
				this.getEngine().getSceneManager().destroySceneNode(it.next().toString());
			}
			gameObjectsToRemove.clear();
		}
	}

	@Override
	protected void setupCameras(SceneManager sm, RenderWindow rw) {
		Camera camera = sm.createCamera("MainCamera", Projection.PERSPECTIVE);

		rw.getViewport(0).setCamera(camera);

		camera.setRt((Vector3f) Vector3f.createFrom(1.0f, 0.0f, 0.0f));
		camera.setUp((Vector3f) Vector3f.createFrom(0.0f, 1.0f, 0.0f));
		camera.setFd((Vector3f) Vector3f.createFrom(0.0f, 0.0f, -1.0f));
		camera.setPo((Vector3f) Vector3f.createFrom(0.0f, 0.0f, 0.0f));

		camera.setMode('n');
		// SceneNode cameraNode =
		// sm.getRootSceneNode().createChildSceneNode(camera.getName() + "Node");
		// cameraNode.attachObject(camera);
	}

	public void incrementScore() {
		score += 5;
	}

	public float getSpeed() {
		return this.speed;
	}

	public void setSpeed(float newSpeed) {
		this.speed = newSpeed;
	}

	public void checkCollision() {

	}

	public void updateVerticalPosition() {
		SceneNode player1N = this.getEngine().getSceneManager().getSceneNode("Player1Node");
		SceneNode tessN = this.getEngine().getSceneManager().getSceneNode("TessN");
		Tessellation tessE = ((Tessellation) tessN.getAttachedObject("TessE"));

		Vector3 worldAvatarPosition = player1N.getWorldPosition();
		Vector3 localAvatarPosition = player1N.getLocalPosition();

		Vector3 newAvatarPosition = Vector3f.createFrom(localAvatarPosition.x(),
				tessE.getWorldHeight(worldAvatarPosition.x(), worldAvatarPosition.z()) + 0.4f, localAvatarPosition.z());

		player1N.setLocalPosition(newAvatarPosition);
	}

	public boolean checkDrop(Vector3 worldAvatarPosition) {
		SceneNode tessN = this.getEngine().getSceneManager().getSceneNode("TessN");
		Tessellation tessE = ((Tessellation) tessN.getAttachedObject("TessE"));

		float groundHeight = tessE.getWorldHeight(worldAvatarPosition.x(), worldAvatarPosition.z());
		if (groundHeight < 1.0f) {
			return true;
		} else {
			return false;
		}

	}

	public boolean getIsConnected() {
		return this.isClientConnected;
	}

	public void setIsConnected(boolean b) {
		this.isClientConnected = b;
	}

	public Object getPlayerPosition() {
		SceneNode playerN = this.getEngine().getSceneManager().getSceneNode("Player1Node");
		return playerN.getWorldPosition();
	}

	public void addGhostAvatarToGameWorld(GhostAvatar ghostAvatars, UUID ghostID, Vector3 pos) throws IOException {
		try {
			SceneManager sm = this.getEngine().getSceneManager();

			ghost1SE = this.getEngine().getSceneManager().createSkeletalEntity("ghost1Av", "wolf.rkm", "wolf.rks");

			Texture tex;
			if (this.getGhostModel() == 1) {
				tex = this.getEngine().getSceneManager().getTextureManager().getAssetByPath("water1.jpg");
			} else {
				tex = this.getEngine().getSceneManager().getTextureManager().getAssetByPath("monster.jpg");
			}

			TextureState tstate = (TextureState) this.getEngine().getSceneManager().getRenderSystem()
					.createRenderState(RenderState.Type.TEXTURE);
			tstate.setTexture(tex);
			ghost1SE.setRenderState(tstate);

			SceneNode ghostN = sm.getRootSceneNode().createChildSceneNode(ghostID.toString());

			ghostN.scale(0.12f, 0.12f, 0.12f);
			ghostN.setLocalPosition(Vector3f.createFrom(pos.x(), pos.y() + 0.4f, pos.z()));

			ghostN.attachObject(ghost1SE);

			ghost1SE.loadAnimation("Run1Animation", "WolfRun.rka");

			this.hasGhost = true;
		} catch (RuntimeException e) {
			e.printStackTrace();
		}
	}

	public void ghostDoTheRun() {
		ghost1SE.stopAnimation();
		ghost1SE.playAnimation("Run1Animation", 0.5f, LOOP, 0);
	}

	private void initNPC(SceneManager sm, SceneNode npcN, Tessellation tessE) throws IOException {
		int len = 10;

		for (int i = 0; i < len; i++) {
			Texture tex = sm.getTextureManager().getAssetByPath("monster.jpg");
			TextureState tstate = (TextureState) sm.getRenderSystem().createRenderState(RenderState.Type.TEXTURE);
			tstate.setTexture(tex);

			SkeletalEntity npc1SE = sm.createSkeletalEntity("NPC1Av" + i, "wolf.rkm", "wolf.rks");

			npc1SE.setRenderState(tstate);

			SceneNode npc1N = npcN.createChildSceneNode("NPC1Node" + i);

			Random r = new Random();

			npc1N.setLocalPosition(Vector3f.createFrom(0,
					tessE.getWorldHeight(npc1N.getWorldPosition().x(), npc1N.getWorldPosition().z()) + 3.0f, 0));

			npc1N.scale(0.8f, 0.8f, 0.8f);
			// npc1N.pitch(Degreef.createFrom(90.0f));

			npc1N.attachObject(npc1SE);

			npc1SE.loadAnimation("NPC1Attack", "WolfRun.rka");
		}
	}

	public void npcDoTheAttack() {
		for (int i = 0; i < 10; i++) {
			SkeletalEntity npc1SE = (SkeletalEntity) this.getEngine().getSceneManager().getEntity("NPC1Av" + i);
			npc1SE.stopAnimation();
			npc1SE.playAnimation("NPC1Attack", 0.5f, LOOP, 0);
		}
	}

	public void npcStopTheAttack() {
		for (int i = 0; i < 10; i++) {
			SkeletalEntity npc1SE = (SkeletalEntity) this.getEngine().getSceneManager().getEntity("NPC1Av" + i);
			npc1SE.stopAnimation();
		}
	}

	// public void GhostNPCsGetBigger() {
	// int len = 10;
	//
	// for (int i = 0; i < len; i++) {
	// SceneNode npc1N = this.getEngine().getSceneManager().getSceneNode("NPC1Node"
	// + i);
	//
	// if (player1N.getWorldPosition().x() - npc1N.getWorldPosition().x() < 0.5f
	// || player1N.getWorldPosition().z() - npc1N.getWorldPosition().z() < 0.5f) {
	// if (!bigger) {
	// npc1N.scale(1.5f, 1.5f, 1.5f);
	// bigger = true;
	// }
	//
	// } else {
	// if (bigger) {
	// npc1N.scale(0.5f, 0.5f, 0.5f);
	// bigger = false;
	// }
	// }
	// }
	// }

	public void updateGhostNPCsToGameWorld(int ghostID, Vector3 ghostPosition) {
		try {
			SceneNode tmp = this.getEngine().getSceneManager().getSceneNode("NPC1Node" + ghostID);
			this.getEngine().getSceneManager().getSceneNode("NPC1Node" + ghostID).setLocalPosition(ghostPosition.x(),
					tmp.getLocalPosition().y(), ghostPosition.z());

			if (!this.ghostRun) {
				// System.out.println("Ghost ----------------------------- Run");
				this.ghostDoTheRun();
				this.ghostRun = true;
			}
		} catch (RuntimeException e) {

		}
	}

	// 0: error 1: walk, 2: rest
	public int checkNPCAnimation(int ghostID, Vector3 ghostPosition) {
		try {
			SceneNode tmp = this.getEngine().getSceneManager().getSceneNode("Player1Node");
			if (calDistance(ghostPosition, tmp.getWorldPosition()) < 7.0f) {
				return 1;
			} else {
				return 2;
			}
		} catch (RuntimeException e) {

		}
		return 0;
	}

	public float calDistance(Vector3 x, Vector3 y) {
		float tmp = (float) Math.sqrt((x.x() - y.x()) * (x.x() - y.x()) + (x.y() - y.y()) * (x.y() - y.y())
				+ (x.z() - y.z()) * (x.z() - y.z()));
		// System.out.println("tmp-----------------------------"+tmp);
		return tmp;
	}

	public void updateGhostPosition(UUID ghostID, Vector3 ghostPosition) {
		try {
			System.out.println("Update Ghost Avatar's position " + ghostID.toString());
			System.out.println("-----------------------------------------------------");
			this.getEngine().getSceneManager().getSceneNode(ghostID.toString()).setLocalPosition(ghostPosition);
		} catch (RuntimeException e) {

		}
	}

	public void rotateGhostAngle(UUID ghostID, Float angle) {
		try {
			Angle ghostRAngle = Degreef.createFrom(angle);
			System.out.println("Rotate the ghost avatar");
			SceneNode ghostN = this.getEngine().getSceneManager().getSceneNode(ghostID.toString());
			ghostN.yaw(ghostRAngle);
			System.out.println("Ghost Node is rotating " + ghostRAngle);
		} catch (RuntimeException e) {

		}
	}

	public void removeGhostAvatarFromGameWorld(GhostAvatar avatar) {
		if (avatar != null) {
			gameObjectsToRemove.add((UUID) avatar.getID());
		}
	}

	private class SendCloseConnectionPacketAction extends AbstractInputAction {
		// for leaving the game
		@Override
		public void performAction(float time, net.java.games.input.Event evt) {
			if (protClient != null && isClientConnected == true) {
				protClient.sendByeMessages();
			}
		}
	}
}
