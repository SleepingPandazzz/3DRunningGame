package myGameEngine;

import java.io.File;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import a3.MyGame;
import net.java.games.input.Event;
import ray.input.action.AbstractInputAction;
import ray.rage.scene.Light;
import ray.rage.scene.SceneManager;

public class ColorAction extends AbstractInputAction {
	private SceneManager sm;
	private ScriptEngine jsEngine;
	private File scriptFile1;
	private MyGame game;

	public ColorAction(ScriptEngine se, File f, MyGame game) {
		this.sm = game.getEngine().getSceneManager();
		this.jsEngine = se;
		this.scriptFile1 = f;
		this.game = game;
	}

	public void performAction(float time, Event e) {
		Invocable invocableEngine = (Invocable) jsEngine;

		Light light = sm.getLight("testLamp1");

		try {
			invocableEngine.invokeFunction("updateLightColor", light, game.getLightTransfer());
			System.out.println("color Action here");
		} catch (ScriptException e1) {
			System.out.println("ScriptException in " + scriptFile1 + e1);
		} catch (NoSuchMethodException e2) {
			System.out.println("No such method in " + scriptFile1 + e2);
		} catch (NullPointerException e3) {
			System.out.println("Null ptr exception reading " + scriptFile1 + e3);
		}

		if (game.getLightTransfer()) {
			game.setLightTransfer(false);
		} else {
			game.setLightTransfer(true);
		}
	}

}
