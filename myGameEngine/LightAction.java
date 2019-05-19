package myGameEngine;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import a3.MyGame;
import net.java.games.input.Event;
import ray.input.action.AbstractInputAction;
import ray.rage.scene.SceneManager;

public class LightAction extends AbstractInputAction {
	private SceneManager sm;
	private MyGame game;
	
	
	public LightAction(MyGame game) {
		this.game = game;
	}
	
	@Override
	public void performAction(float time, Event e) {
//		ScriptEngineManager factory = new ScriptEngineManager();
//		ScriptEngine jsEngine = .getEngineByName("js");
//		
//		// cast the engine so it supports invoking functions
//		Invocable invocableEngine = (Invocable)jsEngine;
//		
//		
	}

}
