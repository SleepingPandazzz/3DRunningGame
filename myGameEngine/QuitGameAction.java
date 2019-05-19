package myGameEngine;

import a3.MyGame;
import net.java.games.input.Event;
import ray.input.action.AbstractInputAction;
import ray.rage.game.Game;

// An abstractInputAction that quits the game.
// It assumes availability of a method "shutdown" in the game
public class QuitGameAction extends AbstractInputAction {
	private MyGame game;

	public QuitGameAction(MyGame g) {
		game = g;
	}

	@Override
	public void performAction(float time, Event event) {
		System.out.println("Shutdown requested");
		game.setState(Game.State.STOPPING);
	}

}
