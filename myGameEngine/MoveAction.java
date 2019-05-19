package myGameEngine;

import a3.MyGame;
import a3.ProtocolClient;
import net.java.games.input.Event;
import ray.input.action.AbstractInputAction;
import ray.rage.scene.Camera;
import ray.rage.scene.SceneNode;
import ray.rml.Vector3;
import ray.rml.Vector3f;

public class MoveAction extends AbstractInputAction {
	private MyGame game;
	private ProtocolClient protClient;

	public MoveAction(MyGame g, ProtocolClient p) {
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
		if (e.getValue() == 0.25) {
			// System.out.println("move forward");
			node.moveForward(spd);

			game.checkCollision();
		}

		// move backward
		if (e.getValue() == 0.75) {
			node.moveBackward(spd);
		}

		// move left
		if (e.getValue() == 1.0) {
			// System.out.println("move left");
			node.moveRight(spd);
			game.checkCollision();
		}

		// move right
		if (e.getValue() == 0.5) {
			// System.out.println("move right");
			node.moveLeft(spd);

			game.checkCollision();
		}
		game.updateVerticalPosition();
		protClient.sendMoveMessages(node.getWorldPosition());
	}

}
