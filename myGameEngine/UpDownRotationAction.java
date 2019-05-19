package myGameEngine;

import a3.MyGame;
import net.java.games.input.Event;
import ray.input.action.AbstractInputAction;
import ray.rage.scene.Camera;
import ray.rage.scene.SceneNode;
import ray.rml.Angle;
import ray.rml.Degreef;
import ray.rml.Vector3;
import ray.rml.Vector3f;

public class UpDownRotationAction extends AbstractInputAction {
	private MyGame game;

	public UpDownRotationAction(MyGame g) {
		game = g;
	}

	@Override
	public void performAction(float time, Event e) {
		Camera c = game.getEngine().getSceneManager().getCamera("MainCamera");

		// move down
		if (e.getValue() == -1) {
			if (c.getMode() == 'c') { // plater is OFF the dolphin now
				Angle rotAmt = Degreef.createFrom(10.0f);
				Vector3 u = c.getRt();
				Vector3 v = c.getUp();
				Vector3 n = c.getFd();
				c.setFd((Vector3f) n.rotate(rotAmt, u));
				c.setUp((Vector3f) v.rotate(rotAmt, u));
			} else { // player is ON the dolphin now
				SceneNode dNode = game.getEngine().getSceneManager().getSceneNode("Player1Node");
				Angle rotAmt = Degreef.createFrom(-10.0f);
				dNode.pitch(rotAmt);
				
				Angle rotAmt1 = Degreef.createFrom(10.0f);
				Vector3 u = c.getRt();
				Vector3 v = c.getUp();
				Vector3 n = c.getFd();
				c.setFd((Vector3f) n.rotate(rotAmt1, u));
				c.setUp((Vector3f) v.rotate(rotAmt1, u));
			}
		}

		// move up
		if (e.getValue() == 1) {
			if (c.getMode() == 'c') { // player is OFF the dolphin now
				Angle rotAmt = Degreef.createFrom(-10.0f);
				Vector3 u = c.getRt();
				Vector3 v = c.getUp();
				Vector3 n = c.getFd();
				c.setFd((Vector3f) n.rotate(rotAmt, u));
				c.setUp((Vector3f) v.rotate(rotAmt, u));
			} else { // player is ON the dolphin now
				SceneNode dNode = game.getEngine().getSceneManager().getSceneNode("Player1Node");
				Angle rotAmt = Degreef.createFrom(10.0f);
				dNode.pitch(rotAmt);
				
				Angle rotAmt1 = Degreef.createFrom(-10.0f);
				Vector3 u = c.getRt();
				Vector3 v = c.getUp();
				Vector3 n = c.getFd();
				c.setFd((Vector3f) n.rotate(rotAmt1, u));
				c.setUp((Vector3f) v.rotate(rotAmt1, u));
			}
		}
	}

}
