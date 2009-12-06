package server;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.util.Hashtable;
import java.util.concurrent.LinkedBlockingQueue;

import physics.CattoPhysicsEngine;
import world.GameObject;
import world.LevelMap;
import world.LevelSet;
import world.PlayerObject;
import jig.engine.PaintableCanvas;
import jig.engine.PaintableCanvas.JIGSHAPE;
import jig.engine.hli.ScrollingScreenGame;
import jig.engine.physics.BodyLayer;
import jig.engine.util.Vector2D;
import net.Action;
import net.NetStateManager;
import net.Protocol;

/**
 * Server
 * 
 */

public class Server extends ScrollingScreenGame {

	private static final int SCREEN_WIDTH = 1280;
	private static final int SCREEN_HEIGHT = 1024;

	/*
	 * This is a static, constant time between frames, all clients run as fast
	 * as the server runs
	 */
	// private static int DELTA_MS = 30;
	private int totalMS;

	private NetStateManager netState;
	private NetworkEngine ne;
	private CattoPhysicsEngine pe;
	public ServerGameState gameState;
	private LevelSet levels;
	private LevelMap level;
	private int playerID;
	private PlayerObject playerObject;
	private Action oldInput;

	private static final int maxBullets = 10;

	public LinkedBlockingQueue<String> msgQueue;

	public Server() {
		super(SCREEN_WIDTH, SCREEN_HEIGHT, false);

		netState = new NetStateManager();
		gameState = new ServerGameState();
		ne = new NetworkEngine(this);
		pe = new CattoPhysicsEngine(new Vector2D(0, 300));
		// pe.setDrawArbiters(true);
		fre.setActivation(true);
		msgQueue = new LinkedBlockingQueue<String>();
		totalMS = 0;

		// temp resources
		PaintableCanvas.loadDefaultFrames("player", 32, 48, 1,
				JIGSHAPE.RECTANGLE, Color.red);
		PaintableCanvas.loadDefaultFrames("smallbox", 32, 32, 1,
				JIGSHAPE.RECTANGLE, Color.blue);
		PaintableCanvas.loadDefaultFrames("playerSpawn", 10, 10, 1,
				JIGSHAPE.CIRCLE, Color.red);
		PaintableCanvas.loadDefaultFrames("bullet", 10, 10, 1,
				JIGSHAPE.RECTANGLE, Color.DARK_GRAY);
		PaintableCanvas.loadDefaultFrames("background", 100, 100, 1,
				JIGSHAPE.RECTANGLE, Color.gray);
		PaintableCanvas.loadDefaultFrames("target", 20, 20, 1,
				JIGSHAPE.CIRCLE, Color.red);

		// Load all levels
		levels = new LevelSet("/res/Levelset.txt");
		if (levels.getNumLevels() == 0) {
			System.err.println("Error: Levels loading failed.\n");
			System.exit(1);
		}
		// Get specified level.
		level = levels.getThisLevel(1);
		if (level == null) {
			System.err.println("Error: Level wasn't correctly loaded.\n");
			System.exit(1);
		}
		level.buildLevel(gameState);
		
		// Add a player to test movement, remove when not needed
		playerObject = new PlayerObject("player");
		playerObject.set(100, 1.0, 1.0, 0.0);
		Vector2D a = level.playerInitSpots.get(0);
		playerObject.setPosition(new Vector2D(a.getX(), a.getY()));
		playerID = gameState.add(playerObject, GameObject.PLAYER);
		oldInput = new Action(playerID);

		netState.update(gameState.getNetState());
		
		gameObjectLayers.clear();
		pe.clear();
		gameObjectLayers.add(gameState.getLayer());
		pe.manageViewableSet(gameState.getLayer());
	}

	// this can be removed when the server no longer needs to test player
	// movement
	public void keyboardMovementHandler() {
		keyboard.poll();

		// player alive/dead test code.
		if (keyboard.isPressed(KeyEvent.VK_P) && playerID != -1) {
			gameState.removeByID(playerID);
			playerID = -1;
			gameObjectLayers.clear();
			gameObjectLayers.add(gameState.getLayer());
			pe.clear();
			pe.manageViewableSet(gameState.getLayer());

		} else if (keyboard.isPressed(KeyEvent.VK_O) && playerID == -1) {
			playerObject = new PlayerObject("player");
			playerObject.set(100, 1.0, 1.0, 0.0);
			Vector2D a = level.playerInitSpots.get(0);
			playerObject.setPosition(new Vector2D(a.getX(), a.getY()));
			playerID = gameState.add(playerObject, GameObject.PLAYER);
			oldInput = new Action(playerID);
			gameObjectLayers.clear();
			gameObjectLayers.add(gameState.getLayer());
			pe.clear();
			pe.manageViewableSet(gameState.getLayer());

		}

		// current issues - if jetpack is on or jumping player will fall up
		// forever.
		// otherwise acts like dead body (that can stay standing. go figure.)

		// Bother with new input only when player is alive.
		if (playerID != -1) {
			Action input = new Action(playerID, Action.INPUT);
			input.crouch = keyboard.isPressed(KeyEvent.VK_DOWN)
					|| keyboard.isPressed(KeyEvent.VK_S);
			input.jet = keyboard.isPressed(KeyEvent.VK_UP)
					|| keyboard.isPressed(KeyEvent.VK_W);
			input.left = keyboard.isPressed(KeyEvent.VK_LEFT)
					|| keyboard.isPressed(KeyEvent.VK_A);
			input.right = keyboard.isPressed(KeyEvent.VK_RIGHT)
					|| keyboard.isPressed(KeyEvent.VK_D);
			input.jump = keyboard.isPressed(KeyEvent.VK_SPACE);

			if (oldInput.equals(input))
				return;
			String action = new Protocol().encodeAction(input);
			processAction(action);
			oldInput.copy(input);
		}
	}

	/**
	 * Just like client has a "keyboardHandler" method that capture key strokes
	 * and acts on them, this method gets action request from clients and
	 * updates the game server state.
	 * 
	 * TcpServer listens on client requests and calls this method
	 * 
	 * @param action
	 *            = encoded action string
	 */
	public void processAction(String action) {

		Action a = netState.prot.decodeAction(action);

		// Get hashtable of all physics objects currently in the game
		Hashtable<Integer, GameObject> objectList = gameState.getHashtable();

		// If there is no such object ID on the server, simply return and do
		// nothing

		if (!objectList.containsKey(a.getId()) && a.getType() != Action.JOIN)
			return;

		switch (a.getType()) {
		// ////////////////////////////////////////////
		// Handling players input, keystrokes
		case Action.INPUT:
			// System.out.println("Player id: "+a.getId());
			// System.out.println("up:"+a.up+" down:"+a.down+" left:"+a.left+
			// " right:"+a.right+" jump:"+a.jump);

			PlayerObject playerObject = (PlayerObject) objectList
					.get(a.getId());

			// Cool idea from Rolf for handling input
			int x = 0,
			y = 0;

			if (a.jump)
				--y;
			if (a.crouch)
				++y;
			if (a.left)
				--x;
			if (a.right)
				++x;

			BodyLayer<GameObject> layer = gameState.getLayer();
			playerObject.updatePlayer(x, y, a.jet, false, false, layer);

			break;

		// ///////////////////////////////////////////
		// Adding a player
		case Action.JOIN:
			System.out.println("Adding player id:" + a.getId());

			ne.addPlayer(a.getId(), a.getMsg());
			PlayerObject player = new PlayerObject("player");
			player.set(100, .2, 1.0, 0.0);
			Vector2D spawn = level.playerInitSpots.get(0);
			player.setPosition(new Vector2D(spawn.getX(), spawn.getY()));
			gameState.add(a.getId(), player, GameObject.PLAYER);

			// Reseting physics/render layers
			gameObjectLayers.clear();
			gameObjectLayers.add(gameState.getLayer());
			pe.clear();
			pe.manageViewableSet(gameState.getLayer());

			break;

		case Action.CHANGE_VELOCITY:
			objectList.get(a.getId()).setVelocity(a.getArg());
			break;
		case Action.CHANGE_POSITION:
			objectList.get(a.getId()).setPosition(a.getArg());
			break;

		case Action.SHOOT:
			System.out.println(a.getId() + " " + a.getArg());

			Vector2D shootloc = a.getArg();
			GameObject b, obj;
			obj = objectList.get(a.getId());

			if (obj.bulletCount >= maxBullets) {// Full. reuse oldest bullet.
				b = obj.listBullets.remove(0);// get from oldest one.
				Vector2D place = objectList.get(a.getId()).getCenterPosition();
				// Put bullet a
				// little bit away from player
				double xx = place.getX() + shootloc.getX() * 40;
				double yy = place.getY() + shootloc.getY() * 40;
				// set V in direction of travel 1000
				b.setVelocity(shootloc.scale(1000));
				b.setPosition(new Vector2D(xx, yy));
				obj.listBullets.add(b); //add as newest object.
			} else {// not full yet. add new bullet.
				// set place at player.
				b = new GameObject("bullet");
				b.set(1, .2, 1.0, 0.0);
				Vector2D place = objectList.get(a.getId()).getCenterPosition();
				// Put bullet a
				// little bit away from player
				double xx = place.getX() + shootloc.getX() * 40;
				double yy = place.getY() + shootloc.getY() * 40;
				// set V in direction of travel 1000
				b.setVelocity(shootloc.scale(1000));
				b.setPosition(new Vector2D(xx, yy));
				gameState.add(b, GameObject.BULLET);

				// Reseting physics/render layers gameObjectLayers.clear();
				gameObjectLayers.add(gameState.getLayer());
				pe.clear();
				pe.manageViewableSet(gameState.getLayer());
				obj.listBullets.add(b); //add as newest object.
				obj.bulletCount++;
			}

			break;
		}
	}

	public void update(final long deltaMs) {
		super.update(deltaMs);
		pe.applyLawsOfPhysics(deltaMs);
		totalMS += deltaMs;
		if (totalMS > 30) {
			ne.update();
			totalMS = 0;
		}
		keyboardMovementHandler();

		while (msgQueue.size() > 0) {
			this.processAction(msgQueue.poll());
		}
		gameState.update();
		//centerOn(p); // centers on player
		playerObject.updatePlayerState();
		
		Vector2D mousePos = screenToWorld(new Vector2D(mouse.getLocation().getX(), mouse.getLocation().getY()));
		//System.out.println("mouse center: " + mousePos.toString());
		//System.out.println("player center: " + p.getCenterPosition().toString());
		//System.out.println("average: " + new Vector2D((int)(pScreenPos.getX()+mouse.getLocation().getX())/2, 
		//		(int)(pScreenPos.getY()+mouse.getLocation().getY())/2).toString());
		//System.out.println("player center: " + p.getCenterPosition());
		centerOnPoint((int)(playerObject.getCenterPosition().getX()+mousePos.getX())/2, (int)(mousePos.getY())/2); // centers on player
	}

	public static void main(String[] vars) {
		Server s = new Server();
		s.run();
	}
}