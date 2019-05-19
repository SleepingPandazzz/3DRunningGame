package myGameEngine;

import a3.MyGame;
import a3.ProtocolClient;
import net.java.games.input.Event;
import ray.input.action.AbstractInputAction;
import ray.rage.scene.Camera;
import ray.rage.scene.SceneNode;
import ray.rml.Angle;
import ray.rml.Degreef;
import ray.rml.Vector3;
import ray.rml.Vector3f;

public class RotateRightAction extends AbstractInputAction {
	private MyGame game;
	private ProtocolClient protClient;

	public RotateRightAction(MyGame g, ProtocolClient p) {
		game = g;
		protClient = p;
	}

	@Override
	public void performAction(float arg0, Event arg1) {
		SceneNode dNode = game.getEngine().getSceneManager().getSceneNode("Player1Node");
		Angle rotAmt = Degreef.createFrom(-3.0f);
		
		dNode.yaw(rotAmt);

		protClient.sendRotateGhostAvatar(-3.0f);
		System.out.println("Reuest rotation");
	}
}
