package clients;

import java.util.Collection;
import java.util.Hashtable;

import world.GameObject;

import jig.engine.physics.vpe.VanillaSphere;
import jig.engine.util.Vector2D;

import net.NetStateManager;
import net.NetObject;

public class ClientGameState {
	
	private Hashtable<Integer, VanillaSphere> spriteList = new Hashtable<Integer, VanillaSphere>();
	
	public ClientGameState () { }
	
	public void init(NetStateManager gameState) {
		for(NetObject no : gameState.getState().getNetObjects())
			init(no);
	}
	
	public void init(NetObject no) {
		if (spriteList.containsKey(no.getId()))
			return;
		
		switch(no.getType()) {
		case GameObject.PLAYER:
			spriteList.put(no.getId(), new SpriteObject("player"));
			break;
		case GameObject.PLATFORM:
			spriteList.put(no.getId(), new SpriteObject("platform"));
			break;
		case GameObject.SMALLBOX:
			spriteList.put(no.getId(), new SpriteObject("smallbox"));
			break;
		case GameObject.GROUND:
			spriteList.put(no.getId(), new SpriteObject("ground"));
			break;
		case GameObject.PLAYERSPAWN:
			spriteList.put(no.getId(), new SpriteObject("playerSpawn"));
			break;
		}
		
	}
	
	public void sync(NetStateManager gameState) {
		for (NetObject no : gameState.getState().getNetObjects()) {
			if (spriteList.containsKey(no.getId())) {
				
				// fixing the offset, because in jig, rectangle extending VanillaShere is just a giant sphere
				SpriteObject s = (SpriteObject)spriteList.get(no.getId());
				Vector2D p = no.getPosition();
				Vector2D newPos = new Vector2D(p.getX()-(s.getRadius()-s.getImgWidth()/2), 
											   p.getY()-(s.getRadius()-s.getImgHeight()/2));
				spriteList.get(no.getId()).setPosition(newPos);
				//System.out.println(no.getVelocity());
				spriteList.get(no.getId()).setVelocity(no.getVelocity());
				spriteList.get(no.getId()).setRotation(no.getRotation());
			} else
				init(no);
		}
	}
	
	public void sync(NetStateManager gameState, Vector2D offset) {
		for (NetObject no : gameState.getState().getNetObjects()) {
			if (spriteList.containsKey(no.getId())) {
				
				// fixing the offset, because in jig, rectangle extending VanillaShere is just a giant sphere
				SpriteObject s = (SpriteObject)spriteList.get(no.getId());
				Vector2D p = no.getPosition();
				Vector2D newPos = new Vector2D(p.getX()-(s.getRadius()-s.getImgWidth()/2) - offset.getX(), 
											   p.getY()-(s.getRadius()-s.getImgHeight()/2) - offset.getY());
				spriteList.get(no.getId()).setPosition(newPos);
				//System.out.println(no.getVelocity());
				spriteList.get(no.getId()).setVelocity(no.getVelocity());
				spriteList.get(no.getId()).setRotation(no.getRotation());
			} else
				init(no);
		}
	}
	
	public Collection<VanillaSphere> getSprites() { 
		return spriteList.values(); 
	} 
}
