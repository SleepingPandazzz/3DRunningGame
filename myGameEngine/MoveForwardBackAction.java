package myGameEngine;

import a3.MyGame;
import a3.ProtocolClient;
import net.java.games.input.Event;
import ray.input.action.AbstractInputAction;
import ray.rage.scene.Camera;
import ray.rage.scene.SceneNode;
import ray.rml.Vector3;
import ray.rml.Vector3f;

public class MoveForwardBackAction extends AbstractInputAction {
	private MyGame game;
	private ProtocolClient protClient;

	public MoveForwardBackAction(MyGame g, ProtocolClient p) {
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

		// move forward
		if (e.getValue() < -0.3f) {
			node.moveForward(spd);

			game.checkCollision();
		}

		// move backward
		if (e.getValue() > 0.3f) {
			node.moveBackward(0.1f);
		}

		game.updateVerticalPosition();
		protClient.sendMoveMessages(node.getWorldPosition());
	}

}
