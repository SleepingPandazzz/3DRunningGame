package myGameEngine;

import a3.MyGame;
import a3.ProtocolClient;
import net.java.games.input.Event;
import ray.input.action.AbstractInputAction;
import ray.rage.scene.Camera;
import ray.rage.scene.SceneNode;
import ray.rml.Vector3;
import ray.rml.Vector3f;

public class MoveLeftAction extends AbstractInputAction {
	private Camera camera;
	private MyGame game;
	ProtocolClient protClient;

	public MoveLeftAction(Camera c, MyGame g, ProtocolClient p) {
		camera = c;
		game = g;
		protClient = p;
	}

	@Override
	public void performAction(float time, Event e) {
		SceneNode node = game.getEngine().getSceneManager().getSceneNode("Player1Node");
		Camera camera = game.getEngine().getSceneManager().getCamera("MainCamera");

		float dis = (float) Math.sqrt(
				(camera.getPo().x() - node.getLocalPosition().x()) * (camera.getPo().x() - node.getLocalPosition().x())
						+ (camera.getPo().y() - node.getLocalPosition().y())
								* (camera.getPo().y() - node.getLocalPosition().y())
						+ (camera.getPo().z() - node.getLocalPosition().z())
								* (camera.getPo().z() - node.getLocalPosition().z()));

		float spd = game.getSpeed();

		node.moveRight(spd);

		game.checkCollision();
		game.updateVerticalPosition();
		protClient.sendMoveMessages(node.getWorldPosition());
	}
}
