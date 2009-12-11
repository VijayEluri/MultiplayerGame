package clients;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.KeyEvent;
import java.awt.geom.AffineTransform;
import javax.swing.JOptionPane;

import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;
import physics.Box;
import server.NetworkEngine;
import world.GameObject;
import net.Action;
import net.NetObject;
import net.NetStateManager;
import net.SyncState;
import jig.engine.CursorResource;
import jig.engine.FontResource;
import jig.engine.PaintableCanvas;
import jig.engine.RenderingContext;
import jig.engine.ResourceFactory;
import jig.engine.PaintableCanvas.JIGSHAPE;
import jig.engine.hli.ScrollingScreenGame;
import jig.engine.physics.AbstractBodyLayer;
import jig.engine.physics.Body;
import jig.engine.physics.BodyLayer;
import jig.engine.util.Vector2D;

import clients.TcpListener;
import clients.TcpSender;

/**
 * Client
 */
public class Client extends ScrollingScreenGame {

	// used for testing UI elements.
	private class uiItem extends Body {

		public uiItem(String imgrsc) {
			super(imgrsc);
		}

		@Override
		public void update(long deltaMs) {
		}
	}

	private class Explode extends Body {

		int slowdowntest = 0;
		int counter = 0;

		public Explode(String imgrsc) {
			super(imgrsc);
		}

		@Override
		public void update(long deltaMs) {
			if (slowdowntest < 50) {
				slowdowntest += deltaMs;
			} else {
				slowdowntest = 0;
				counter += 1;
				// counter = counter % this.getFrameCount();
				this.setFrame(counter);
			}
			if (counter == getFrameCount()) {
				this.setActivation(false);
			}
		}
	}

	static final String PICTUREBACKGROUND = "res/GameBackground.png";
	static final String UIGFX = "res/ClientUI.png";
	static final String SPRITES = "res/2Destruction-spritesheet.png";
	static final String LEVEL1 = "res/LEVEL1.png";

	public String SERVER_IP = "127.0.0.1";

	public static final int SCREEN_WIDTH = 800, SCREEN_HEIGHT = 600;
	private static final int MAXJETFUEL = 2000;

	boolean keyPressed = false;
	boolean keyReleased = true;

	Action input;

	private NetStateManager netStateMan;
	private Player player;

	private uiItem jetpack, health;

	private BodyLayer<Body> black = new AbstractBodyLayer.NoUpdate<Body>();
	private BodyLayer<Body> background = new AbstractBodyLayer.IterativeUpdate<Body>();
	private BodyLayer<Body> levelmap = new AbstractBodyLayer.IterativeUpdate<Body>();
	private BodyLayer<Body> front = new AbstractBodyLayer.IterativeUpdate<Body>();
	private BodyLayer<uiItem> GUI = new AbstractBodyLayer.IterativeUpdate<uiItem>();

	GameSprites gameSprites;

	private SyncState state;

	private LinkedBlockingQueue<String> msgQueue = new LinkedBlockingQueue<String>();

	// private LinkedList<Box> levelboxes = new LinkedList<Box>();

	private int jetFuel = MAXJETFUEL;

	FontResource fontWhite = ResourceFactory.getFactory().getFontResource(
			new Font("Sans Serif", Font.BOLD, 24), Color.white, null);
	public String gameStatusString = "";

	public Client() {

		super(SCREEN_WIDTH, SCREEN_HEIGHT, false);

		ResourceFactory.getFactory().loadResources("res",
				"2Destruction-Resources.xml");
		// newgame = new Button(SPRITE_SHEET + "#Start");
	}

	public void runSetup() {
		ResourceFactory factory = ResourceFactory.getFactory();

		PaintableCanvas.loadDefaultFrames("grenade", 10, 10, 1,
				JIGSHAPE.CIRCLE, Color.GREEN);
		PaintableCanvas.loadDefaultFrames("player", 32, 48, 1,
				JIGSHAPE.RECTANGLE, Color.red);
		PaintableCanvas.loadDefaultFrames("smallbox", 32, 32, 1,
				JIGSHAPE.RECTANGLE, Color.blue);
		PaintableCanvas.loadDefaultFrames("playerSpawn", 10, 10, 1,
				JIGSHAPE.CIRCLE, Color.red);
		PaintableCanvas.loadDefaultFrames("bullet", 5, 5, 1,
				JIGSHAPE.RECTANGLE, Color.WHITE);
		// 1280, SCREEN_HEIGHT = 1024
		PaintableCanvas.loadDefaultFrames("blackback", SCREEN_WIDTH,
				SCREEN_HEIGHT, 1, JIGSHAPE.RECTANGLE, Color.black);

		// will be mostly transparent when done with adding gfx stuff.
		CursorResource cr = factory.makeCursor(UIGFX + "#Cursor", new Vector2D(
				15, 15), 1);
		mouse.setCursor(cr);

		netStateMan = new NetStateManager();
		gameSprites = new GameSprites();
		fre.setActivation(true);

		// stateQueue = new LinkedBlockingQueue<String>(1);
		state = new SyncState();

		/* Start thread to sync gameState with server */
		BroadcastListener bListen = new BroadcastListener(state);
		bListen.start();

		/* Thread with TCP networking for server specific commands */
		TcpListener tcpListen = new TcpListener(NetworkEngine.TCP_CLIENT_PORT,
				msgQueue);
		tcpListen.start();

		/* Send TCP data via this object */
		TcpSender control = new TcpSender(SERVER_IP, NetworkEngine.TCP_PORT);

		player = new Player(0, control);
		input = new Action(0, Action.INPUT);
		player.join(SERVER_IP);

		// Create background object and add to layer, to window.
		gameObjectLayers.clear();
		Box back = new Box("blackback");
		black.add(back);

		// background graphic.
		back = new Box(PICTUREBACKGROUND + "#background");
		background.add(back);
		// ui elements
		jetpack = new uiItem(UIGFX + "#JetFuel");
		jetpack.setPosition(new Vector2D(20, 20));
		GUI.add(jetpack);
		health = new uiItem(UIGFX + "#Health");
		health.setPosition(new Vector2D(20, 31));
		GUI.add(health);

		// still broken
		/*
		for (int z = 0; z <= SCREEN_WIDTH / 425 + 1; z++) {
			for (int w = 0; w <= SCREEN_HEIGHT / 150 + 1; w++) {
				Box level = new Box(LEVEL1 + "#LEVEL1");// 4250
				level.setPosition(new Vector2D(z * 425, w * 150));
				level.setFrame(0);// just set one for now.
				levelmap.add(level);
			}
		}*/

		// Control of layering
		// layers below black is forced render.
		gameObjectLayers.add(levelmap);
		gameObjectLayers.add(GUI);

		gameObjectLayers.add(black);

		gameObjectLayers.add(background);
		gameObjectLayers.add(gameSprites.getLayer());
		gameObjectLayers.add(front);

		gameStatusString = "Connecting to the server...";

	}

	/**
	 * Handle keyboard strokes for movement
	 */
	public void keyboardMovementHandler(long deltaMs) {
		if (!netStateMan.getState().objectList.containsKey(player.getID()))
			return;

		keyboard.poll();

		if (netStateMan.getState().objectList.get(player.getID()).getHealth() > 0) {
			input.crouch = keyboard.isPressed(KeyEvent.VK_DOWN)
					|| keyboard.isPressed(KeyEvent.VK_S);
			// GameObject p = gameSprites.spriteList.get(player.getID());
			if (jetFuel > 0
					&& (keyboard.isPressed(KeyEvent.VK_UP) || keyboard
							.isPressed(KeyEvent.VK_W))) {
				input.jet = true;
				jetFuel -= 2;
			} else {
				input.jet = false;
			}
			if (!(keyboard.isPressed(KeyEvent.VK_UP) || keyboard
					.isPressed(KeyEvent.VK_W))
					&& jetFuel < MAXJETFUEL)
				++jetFuel;

			input.left = keyboard.isPressed(KeyEvent.VK_LEFT)
					|| keyboard.isPressed(KeyEvent.VK_A);
			input.right = keyboard.isPressed(KeyEvent.VK_RIGHT)
					|| keyboard.isPressed(KeyEvent.VK_D);
			input.jump = keyboard.isPressed(KeyEvent.VK_SPACE);
			input.shoot = mouse.isLeftButtonPressed();
			input.arg0 = screenToWorld(new Vector2D(mouse.getLocation().getX(),
					mouse.getLocation().getY()));
			player.move(input);
		} else {
			input.crouch = false;
			input.jet = false;
			input.left = false;
			input.right = false;
			input.jump = false;
			player.move(input);
		}
		if (keyboard.isPressed(KeyEvent.VK_B)) {
			Explode boomy = new Explode(SPRITES + "#Explosion");
			boomy.setCenterPosition(this.screenToWorld(new Vector2D(mouse
					.getLocation().x, mouse.getLocation().y)));
			front.add(boomy);
		}

	}

	/**
	 * This method checks player's state and returns True if player has joined a
	 * server game and false otherwise
	 * 
	 * @return - boolean
	 */
	public boolean areWeInGame() {
		// When joining the game, request for a player ID is sent and this loop
		// awaits Server's response
		while (msgQueue.size() > 0) {
			// We are only looking for servers response to a JOIN_REQUEST here
			Action a = netStateMan.prot.decodeAction(msgQueue.poll());
			if (a.getType() == Action.JOIN_ACCEPT) {
				Integer newID = Integer.valueOf(a.getMsg()).intValue();
				player.setID(newID);
				input.setID(newID);
				player.state = Player.JOINED;
				gameStatusString = "Connected.";
			}

			// Server has refused JOIN_REQUEST or asked us to leave
			if (a.getType() == Action.LEAVE_SERVER) {
				gameStatusString = "Connection to the server lost.";
				player.state = Player.WAITING;
			}
		}

		// Player has not joined a server game yet, still waiting for server's
		// response
		if (player.state != Player.JOINED) {
			gameStatusString = "Conecting to the server...";
			return false;
		}
		return true;
	}

	int shootlimit = 0;

	public void update(long deltaMs) {
		if (areWeInGame() == false)
			return;

		super.update(deltaMs);

		Vector2D mousePos = screenToWorld(new Vector2D(mouse.getLocation()
				.getX(), mouse.getLocation().getY()));

		// get messages from the server
		String s = state.get();
		if (s != null)
			netStateMan.sync(s);
		else {
			// if no message from the server, update position of objects with
			// local deltaMs
			for (NetObject n : netStateMan.getState().getNetObjects()) {
				n.update(deltaMs);
				GameObject a = gameSprites.spriteList.get(n.getId());
				if (a.getType() == GameObject.PLAYER) {

				}
			}
		}
		gameSprites.sync(netStateMan);

		// Move background to 90% of cursor world coordinate location.
		// as seen from player view
		GameObject p = gameSprites.spriteList.get(player.getID());
		Vector2D bgPos = null;
		if (p != null && p.getCenterPosition() != null) {

			// Adjust background position relative to mouse cursor to create the
			// effect of depth
			double bg_deltaPos = 0.3;
			bgPos = new Vector2D((p.getCenterPosition().getX() + mousePos
					.getX())
					/ 2 * bg_deltaPos, (p.getCenterPosition().getY() + mousePos
					.getY())
					/ 2 * bg_deltaPos);
			background.get(0).setCenterPosition(bgPos);

			centerOnPoint(
					(int) (p.getCenterPosition().getX() + mousePos.getX()) / 2,
					(int) (p.getCenterPosition().getY() + mousePos.getY()) / 2);

			// it is assumed that health is in range [0-2000].
			// System.out.println(hl);
			int hl = netStateMan.getState().objectList.get(player.getID())
					.getHealth();
			if (hl > 0) {
				int hframe = 25 - (int) ((((double) hl) / 2000.0) * 25);
				// System.out.println(hframe + " hframe, client");
				health.setFrame(hframe);
			} else {
				health.setFrame(25);
			}
			if (jetFuel > 0) {
				int jframe = 25 - (int) ((((double) jetFuel) / 2000.0) * 25);
				// System.out.println(jframe + " jframe " + jetFuel +
				// "jetfuel, client");
				jetpack.setFrame(jframe);
			} else {
				jetpack.setFrame(25);
			}
		}

		keyboardMovementHandler(deltaMs);

		if (shootlimit < 250) {
			shootlimit += deltaMs;
		} else if (p != null
				&& mouse.isLeftButtonPressed()
				&& netStateMan.getState().objectList.get(player.getID())
						.getHealth() > 0) {
			if (p.getCenterPosition() != null) {
				shootlimit = 0;
				// Get shoot vector and normalize it.
				Vector2D shot = new Vector2D(mousePos.getX()
						- p.getCenterPosition().getX(), mousePos.getY()
						- p.getCenterPosition().getY());
				shot = shot.unitVector();
				player.shoot(shot);
			}
			// System.out.println("Weapon fire keypress" + mouse.getLocation());
		}

		// better but still broken.
		//updateLevelRender(new Vector2D(
			//	(int) (p.getCenterPosition().getX() + mousePos.getX()) / 2,
				//(int) (p.getCenterPosition().getY() + mousePos.getY()) / 2));
	}

	private void updateLevelRender(Vector2D offset) {
		if (offset == null)
			return;

		Vector2D wintopleft = screenToWorld(new Vector2D(0,0));		
		int xx = (int) (wintopleft.getX() + (4250/2))/425;
		int yy = (int) (wintopleft.getY() + (1500/2))/150;
		
		
		Vector2D off = new Vector2D((int)(offset.getX() % 425), (int) (offset.getY() % 150));
		
		System.out.println( xx + " " + yy + " " + wintopleft.toString() + " client");

		if (xx < 0)
			xx = 0;
		if (yy < 0)
			yy = 0;
		if (xx > 9)
			xx = 9;
		if (yy > 9)
			yy = 9;
		
		
		for (int z = 0; z <= SCREEN_WIDTH / 425 + 1; z++) {
			for (int w = 0; w <= SCREEN_HEIGHT / 150 + 1; w++) {
				Box level = (Box) levelmap.get(w + z * SCREEN_WIDTH / 425);
				level.setPosition(new Vector2D(z * 425 , w * 150));
				if(xx+z < 10 && yy + w < 10)
					level.setFrame((xx + z) + (yy+w) * 10);
				else
					level.setFrame(0);
			}
		}
	}

	public void render(RenderingContext rc) {
		black.render(rc);// draw at screen coordities.
		super.render(rc);
		levelmap.render(rc);
		GUI.render(rc);
		// background.render(rc);
		fontWhite.render(gameStatusString, rc, AffineTransform
				.getTranslateInstance(180, 7));
	}

	public static void main(String[] vars) {
		Client c = new Client();
		int as = 0;
		// unfinished yet
		while (as == 0) {
			String s = JOptionPane
					.showInputDialog("Enter server IP address or empty if want 127.0.0.1");
			if (s.compareTo("") == 0)
				s = "127.0.0.1";
			String[] a = s.split("\\.");
			if (a.length == 4) {
				int a1 = java.lang.Integer.parseInt(a[0]);
				int a2 = java.lang.Integer.parseInt(a[1]);
				int a3 = java.lang.Integer.parseInt(a[2]);
				int a4 = java.lang.Integer.parseInt(a[3]);
				System.out.println(a1 + "." + a2 + "." + a3 + "." + a4);
				// check formatting if its properly done.
				if (a1 > 0 && a1 < 254 && a2 >= 0 && a2 <= 254 && a3 >= 0
						&& a3 <= 254 && a4 > 0 && a4 <= 254) {
					String res = String.valueOf(a1 + "." + a2 + "." + a3 + "."
							+ a4);
					c.SERVER_IP = res;// definitely correctly formatted.
					as = 1;
				}
			}
			if (as == 0)
				JOptionPane
						.showMessageDialog(
								null,
								"Misformatted IP address "
										+ s
										+ "\n\nNeed to be X.X.X.X with X in range [0-254].");
		}

		c.runSetup();
		c.run();
	}
}