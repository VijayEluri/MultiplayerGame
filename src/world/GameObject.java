package world;

import java.awt.event.KeyEvent;

import jig.engine.util.Vector2D;
import net.Action;
import physics.Box;

public class GameObject extends Box {
	
	public static final int PLAYER   = 0;
	public static final int GROUND   = 1;
	public static final int PLATFORM = 2;
	public static final int SMALLBOX = 3;
	public static final int PLAYERSPAWN   = 4;
	
	public int type;
	
	public GameObject(String rsc) {
		super(rsc);
	}
	
	public Double getMass() {
		return mass;
	}
	
	public void setMass(Double m) {
		mass =  m;
	}
	
	public int getType() {
		return type;
	}
	
	public void setType(int t) {
		type =  t;
	}
	
	// Testing Player Movement on Server Remove When Not Needed //
	public void move(int x, int y) {
		this.setVelocity(new Vector2D(x*100,y*100));
	}
	
}
