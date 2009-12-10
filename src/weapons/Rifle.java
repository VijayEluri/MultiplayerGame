package weapons;

import server.ServerGameState;
import world.GameObject;
import world.PlayerObject;
import jig.engine.util.Vector2D;

public class Rifle extends Weapon {
	
	protected static int VEL_MAG = 1000;
	protected static int WEAPON_DELAY = 250;
	
	public Rifle(PlayerObject p) {
		super(p);
	}

	@Override
	public void shoot(Vector2D cursor, long deltaMs) {
		//System.out.println("rifle.shoot totalMs: " + ServerGameState.getGameState().totalMs + " delayMs " + delayMs);
		if (ServerGameState.getGameState().totalMs - delayMs < WEAPON_DELAY) {
			//System.out.println("rifle.shoot skipped shot");
			return;
		}
			
		delayMs = ServerGameState.getGameState().totalMs;
		
		// get the oldest bullet
		GameObject bullet = bullets.remove(0);// get from oldest one.
		bullet.setActivation(true);
		
		// set starting location
		Vector2D shootLoc = null;
		if (player.getCenterPosition().getX() > cursor.getX()) {
			shootLoc = new Vector2D(player.getCenterPosition().getX()-player.getWidth()*.7, // bullet size is 5 pixels thats why .7
					player.getCenterPosition().getY()-player.getHeight()*.25);
			//System.out.println("rifle.shoot player: " + player.getCenterPosition().toString() + " shoot left: " + shootLoc.toString());
		} else {
			shootLoc = new Vector2D(player.getCenterPosition().getX()+player.getWidth()*.6, // bullet size is 5 pixels that's why .6
					player.getCenterPosition().getY()-player.getHeight()*.25);
			//System.out.println("rifle.shoot player: " + player.getCenterPosition().toString() + " shoot right: " + shootLoc.toString());
		}
		bullet.setPosition(shootLoc);
		
		// set velocity
		Vector2D shootVec = new Vector2D(cursor.getX()
				- player.getCenterPosition().getX(), 
				cursor.getY() - player.getCenterPosition().getY()).unitVector();
		bullet.setVelocity(shootVec.scale(VEL_MAG));
		
		// add it to the start of the list
		bullets.add(bullet);
	}
}