package world;

import jig.engine.physics.BodyLayer;
import jig.engine.util.Vector2D;
import physics.Arbiter;
import physics.CattoPhysicsEngine;

public class PlayerObject extends GameObject {

	private static final double JUMPVEL = -200;
	private static final double RUNVEL = 150;
	private static final double WALKVEL = 100;
	private static final double CROUCHVEL = 50;
	private static final double JETFORCE = -400;
	private static final double FLOATFORCE = 100;
	private static final double NOFORCE = 0;
	private static final double FRICTION = 1.0;
	
	private int keyLeftRight; // left right key
	private int keyJumpCrouch; // jump key
	private boolean keyJet; // jetpack key
	private boolean keyCrouch; // crouch key
	private boolean keyRun; // run toggle
	
	private int jetFuel; 
	private boolean onObject; // are we standing on an object

	public PlayerObject(String rsc) {
		super(rsc);
		keyLeftRight = 0; // left right key
		keyJumpCrouch = 0; // jump key
		keyCrouch = false; // crouch key
		keyJet = false; // jetpack key
		keyRun = false; // run toggle
		jetFuel = 100;
		onObject = false;
	}
	
	public void setKeys(int leftRight, int jumpCrouch, boolean jet, 
					   boolean crouch, boolean run) {
		keyLeftRight = leftRight;
		keyJumpCrouch = jumpCrouch;
		keyCrouch = crouch;
		keyJet = jet;
		if (run) keyRun = !keyRun; // toggle run
	}
	
	// horizontal movement
	public void leftright(int leftRight) {
		keyLeftRight = leftRight;
		if (keyLeftRight != 0) { // pressed button
			friction = 0;
			if (onObject){ // on object
				velocity = new Vector2D( WALKVEL*keyLeftRight, velocity.getY() ); // one speed for now
				force = new Vector2D( NOFORCE, force.getY());
			} else { // in air
				force = new Vector2D( FLOATFORCE*mass*leftRight, force.getY());
			}
		} else { // released button
			friction = FRICTION;
			if (onObject){ // on object
				//System.out.println("On Object");
				velocity = new Vector2D( 0, velocity.getY() );
				force = new Vector2D( NOFORCE, force.getY());
			} else { // in air
				//System.out.println("Off Object");
				force = new Vector2D( NOFORCE, force.getY());
			}
		}
	}
	
	// jump
	public void jumpCrouch(int jumpCrouch) {
		keyJumpCrouch = jumpCrouch;
		if (keyJumpCrouch < 0) { // pressed button
			if (onObject){ // on object
				velocity = new Vector2D( velocity.getX(), JUMPVEL );
			}
		} else {
			// TODO: Crouch
		}
	}
	
	// jetpack
	public void jet(boolean jet) {
		keyJet = jet;
		if (keyJet) { // pressed button
			if (jetFuel > 0) {
				force = new Vector2D( force.getX(), JETFORCE*mass );
			}
		} else { // released button
			force = new Vector2D( force.getX(), NOFORCE);
		}
	}
	
	// walking, running and floating
	public void updatePlayer(int leftRight, int jumpCrouch, boolean jet, 
			   boolean crouch, boolean run, BodyLayer<GameObject> layer) {
		
		// detect if on an object TODO: static only?
		Arbiter arb;
		GameObject otherObject;
		onObject = false;
		for (int i = 0; i < layer.size(); i++) {
			otherObject = layer.get(i);
			if (this.hashCode() == otherObject.hashCode()) continue;
			arb = new Arbiter(this, otherObject);
			//System.out.println("playerObject: "+playerObject.getType());
			//System.out.println("otherObject: "+otherObject.getType());
			//System.out.println("num contacts: "+arb.getNumContacts());
			if ( arb.getNumContacts() > 0 ) {
				onObject = true;
				break;
			}
		}
		
		//setKeys(leftRight, jump, jet, crouch, run);
		if (keyLeftRight != leftRight) leftright(leftRight);
		if (keyJumpCrouch != jumpCrouch) jumpCrouch(jumpCrouch);
		if (keyJet != jet) jet(jet);
	}

}