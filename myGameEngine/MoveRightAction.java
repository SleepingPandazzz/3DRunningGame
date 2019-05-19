package myGameEngine;

import a3.MyGame;
import a3.ProtocolClient;
import net.java.games.input.Event;
import ray.input.action.AbstractInputAction;
import ray.rage.scene.Camera;
import ray.rage.scene.SceneNode;

public class MoveRightAction extends AbstractInputAction {
	private Camera camera;
	private MyGame game;
	private ProtocolClient protClient;

	public MoveRightAction(Camera c, MyGame g, ProtocolClient p) {
		camera = c;
		game = g;
		protClient = p;
	}

	@Override
	public void performAction(float time, Event e) {
		SceneNode node = game.getEngine().getSceneManager().getSceneNode("Player1Node");

		float spd = game.getSpeed();

		node.moveLeft(spd);

		game.checkCollision();
		game.updateVerticalPosition();
		protClient.sendMoveMessages(node.getWorldPosition());
	}
}