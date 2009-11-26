package server;

import java.util.Hashtable;
import java.util.Random;

import world.GameObject;
import jig.engine.physics.AbstractBodyLayer;
import jig.engine.physics.BodyLayer;
import net.NetObject;
import net.NetState;

public class ServerGameState {

	private NetState netState;
	
	private Hashtable<Integer, GameObject> boxList = new Hashtable<Integer, GameObject>();
	
	private Random generator = new Random();
	
	public ServerGameState() {
		netState = new NetState();
	}
	
	/**
	 * Adds a 'Box' to the boxList
	 * and create a corresponding NetObject to send to the clients
	 * 
	 * 
	 * @param b - Box
	 * @param type - type of object, this can be a player, a bullet, etc
	 */
	public void add(GameObject box, int type) {
		int id = generator.nextInt(65000);  // this is a hack, IDs are random number
		
		if(boxList.containsKey(id))
			return;
		
		boxList.put(id, box);
		
		// Need to figure out which type of box it is. PLAYER, BULLET, PANEL, etc
		// all objects are PLAYERs right now
		netState.add(new NetObject(id, box.getPosition(), type));
	}
	
	public void update() {
		Hashtable<Integer, NetObject> netList = netState.getHashtable();
		for(Integer i : boxList.keySet()) {
			GameObject b = boxList.get(i);
			NetObject no = netList.get(i);
			no.setPosition(b.getPosition());
			
			//System.out.println(b.getVelocity());
			// Box's velocity vector is way too high for some reason, maybe it should be scaled by DELTA_MS, i dunno
			no.setVelocity(b.getVelocity());
			
			no.setRotation(b.getRotation());
		}
	}
	
	
	/**
	 * Returns a Layer which can be added to rendering layer
	 * @return
	 */
	public BodyLayer<GameObject> getBoxes() {
		BodyLayer<GameObject> boxLayer = new AbstractBodyLayer.IterativeUpdate<GameObject>();
		for(GameObject b : boxList.values())
			boxLayer.add(b);
		return boxLayer;
	}
	
	public NetState getNetState() {
		return netState;
	}
}
