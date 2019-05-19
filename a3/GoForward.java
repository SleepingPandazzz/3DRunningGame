package a3;

import ray.ai.behaviortrees.BTAction;
import ray.ai.behaviortrees.BTStatus;
import ray.rml.Vector3;
import ray.rml.Vector3f;

public class GoForward extends BTAction{
	private int i;
	private NPCController c;

	public GoForward(int i, NPCController npcController) {
		this.i = i;
		this.c = npcController;
	}

	@Override
	protected BTStatus update(float time) {
		c.updateNPC(i);
		return BTStatus.BH_SUCCESS;
	}
}
