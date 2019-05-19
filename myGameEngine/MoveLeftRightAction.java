package myGameEngine;

import a3.MyGame;
import a3.ProtocolClient;
import net.java.games.input.Event;
import ray.input.action.AbstractInputAction;
import ray.rage.scene.Camera;
import ray.rage.scene.SceneNode;
import ray.rml.Vector3;
import ray.rml.Vector3f;

public class MoveLeftRightAction extends AbstractInputAction {
	private MyGame game;
	private ProtocolClient protClient;

	public MoveLeftRightAction(MyGame g, ProtocolClient p) {
		game = g;
		protClient = p;
	}

	@Override
	public void performAction(float time, Event e) {
		SceneNode node = game.getEngine().getSceneManager().getSceneNode("Player1Node");

		float spd = game.getSpeed();

		// move left
		if (e.getValue() < -0.3f) {
			node.moveRight(spd);
		}

		// move right
		if (e.getValue() > 0.3f) {
			node.moveLeft(0.1f);
		}

		game.checkCollision();
		game.updateVerticalPosition();
		protClient.sendMoveMessages(node.getWorldPosition());
	}
}