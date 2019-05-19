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

public class LeftRightRotationAction extends AbstractInputAction {
	private MyGame game;

	public LeftRightRotationAction(MyGame g) {
		game = g;
	}

	@Override
	public void performAction(float time, Event e) {
		Camera c = game.getEngine().getSceneManager().getCamera("MainCamera");
		// left rotation
		if (e.getValue() == -1) {
			SceneNode dNode = game.getEngine().getSceneManager().getSceneNode("Player1Node");
			Angle rotAmt = Degreef.createFrom(3.0f);
			dNode.yaw(rotAmt);
		}

		// right rotation
		if (e.getValue() == 1) {
			SceneNode dNode = game.getEngine().getSceneManager().getSceneNode("Player1Node");
			Angle rotAmt = Degreef.createFrom(-3.0f);
			dNode.yaw(rotAmt);
		}
	}

}
