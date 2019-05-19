package a3;

import ray.ai.behaviortrees.BTCompositeType;
import ray.ai.behaviortrees.BTSequence;
import ray.ai.behaviortrees.BehaviorTree;
import ray.rml.Vector3;
import ray.rml.Vector3f;

public class NPCController {

	private int npcNum = 10;

	private NPC[] npcList = new NPC[npcNum];

	BehaviorTree bt = new BehaviorTree(BTCompositeType.SELECTOR);

	public int getNPCNum() {
		return this.npcNum;
	}

	public NPC getNPC(int i) {
		return npcList[i];
	}

	public void setNPCNum(int newNum) {
		this.npcNum = newNum;
	}

	public void setupNPC(int i) {
		npcList[i] = new NPC();
	}

	public void setupNPCs() {
		System.out.println("Setting up npcs");
		for (int i = 0; i < this.getNPCNum(); i++) {
			setupNPC(i);
		}
	}

	public void updateNPC(int i) {
		System.out.println("updating npc");
//		npcList[i].setZ(npcList[i].getZ() - 0.2f);
		npcList[i].getBigger();
	}

	public void updateNPCs() {
		for (int i = 0; i < this.getNPCNum(); i++) {
			// updateNPC(i);
		}
	}

	public void setupBehaviorTree() {
		bt.insertAtRoot(new BTSequence(10));
		bt.insertAtRoot(new BTSequence(20));
		for (int i = 0; i < this.getNPCNum(); i++) {
			bt.insert(10, new GoForward(i, this));
		}
	}

}
