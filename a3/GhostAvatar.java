package a3;

import java.util.UUID;

import ray.rml.Vector3;

public class GhostAvatar {
	private UUID id;
	private Vector3 position;

	public GhostAvatar(UUID id, Vector3 position) {
		this.id = id;
		this.position = position;
	}

	public UUID getID() {
		return this.id;
	}

	public void setID(UUID newId) {
		this.id = newId;
	}

	public Vector3 getPosition() {
		return this.position;
	}


	public void setPosition(Vector3 position) {
		this.position=position;
	}

	public void move(Vector3 ghostPosition) {
		setPosition(ghostPosition);
	}
}
