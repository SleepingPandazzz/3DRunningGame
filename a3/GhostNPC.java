package a3;

import ray.rml.Vector3;

public class GhostNPC {
	private int id;
	private Vector3 position;
	
	public GhostNPC(int id) {
		this.id = id;
	}

	public int getID() {
		return this.id;
	}

	public void setID(int newId) {
		this.id = newId;
	}
	
	public Vector3 getPosition() {
		return this.position;
	}
	
	public void setPosition(Vector3 position) {
		this.position = position;
	}
	
	public void move(Vector3 position) {
		setPosition(position);
	}
}
