package a3;

import java.io.IOException;
import java.net.InetAddress;
import java.util.UUID;
import java.util.Vector;

import graphicslib3D.Point3D;
import graphicslib3D.Vector3D;
import ray.networking.client.GameConnectionClient;
import ray.rml.Vector3;
import ray.rml.Vector3f;
import ray.rml.Angle;
import ray.rml.Degreef;

public class ProtocolClient extends GameConnectionClient {
	private MyGame game;
	private UUID id;
	// private Vector<GhostAvatar> ghostAvatars;
	private GhostAvatar ghostAvatars;
	private int gModel;
	private Vector<GhostNPC> ghostNPCs;

	public ProtocolClient(InetAddress remAddr, int remPort, ProtocolType pType, MyGame game) throws IOException {
		super(remAddr, remPort, pType);
		this.game = game;
		this.id = UUID.randomUUID();
		ghostNPCs = new Vector<GhostNPC>(10);
	}

	@Override
	protected void processPacket(Object message) {
		String strMessage = (String) message;
		String[] msgTokens = strMessage.split(",");

		if (msgTokens.length > 0) {
			if (msgTokens[0].compareTo("join") == 0) {
				String s = "Joined ";
				if (msgTokens[1].compareTo("success") == 0) {
					game.setIsConnected(true);
					s += "successfully.";
					sendCreateMessage(game.getPlayerPosition(), game.getPlayerModel());
				}
				if (msgTokens[1].compareTo("failure") == 0) {
					game.setIsConnected(false);
					s += "failed";
				}
				System.out.println(s);
			}

			// receive "bye"
			if (msgTokens[0].compareTo("bye") == 0) {
				System.out.println("client bye");
				UUID ghostID = UUID.fromString(msgTokens[1]);
				removeGhostAvatar(ghostID);
			}

			// receive "create"
			if (msgTokens[0].compareTo("create") == 0) {
				UUID ghostID = UUID.fromString(msgTokens[1]);
				Vector3 ghostPosition = Vector3f.createFrom(Float.parseFloat(msgTokens[2]),
						Float.parseFloat(msgTokens[3]), Float.parseFloat(msgTokens[4]));
				int ghostModel = (int) Float.parseFloat(msgTokens[5]);
				
				createGhostAvatar(ghostID, ghostPosition, ghostModel);
				gModel = ghostModel;
				System.out.println("Created a ghost avatar.");
			}

			// receive "dsfr" DETAILSFOR
			// update ghost avatar for sender
			if (msgTokens[0].compareTo("dsfr") == 0) {
				UUID remID = UUID.fromString(msgTokens[1]);
				Vector3 ghostPosition = Vector3f.createFrom(Float.parseFloat(msgTokens[2]),
						Float.parseFloat(msgTokens[3]), Float.parseFloat(msgTokens[4]));
				// sendDetailsForMessage(remID, ghostPosition);
				System.out.println("Get details for the other clients");
			}

			// receive "wsds" WANTSDETAILS
			// get local avatar position/orientation
			if (msgTokens[0].compareTo("wsds") == 0) {
				UUID remID = UUID.fromString(msgTokens[1]);
				Vector3 ghostPosition = Vector3f.createFrom(Float.parseFloat(msgTokens[2]),
						Float.parseFloat(msgTokens[3]), Float.parseFloat(msgTokens[4]));
				sendDetailsForMessage(remID, ghostPosition);
			}

			// receive "move"
			if (msgTokens[0].compareTo("move") == 0) {
				UUID remId = UUID.fromString(msgTokens[1]);
				Vector3 ghostPosition = Vector3f.createFrom(Float.parseFloat(msgTokens[2]),
						Float.parseFloat(msgTokens[3]), Float.parseFloat(msgTokens[4]));
				moveGhostAvatar(remId, ghostPosition);
			}

			// receive "rotate"
			if (msgTokens[0].compareTo("rotate") == 0) {
				UUID remID = UUID.fromString(msgTokens[1]);
				Float rotAngle = Float.parseFloat(msgTokens[2]);
				this.rotateGhostAvatar(remID, rotAngle);
			}

			// receive "needNPC"
			if (msgTokens[0].compareTo("needNPC") == 0) {
				System.out.println("client need npc");
			}

			// receive "collide"
			if (msgTokens[0].compareTo("collide") == 0) {
				System.out.println("client collide");
			}

			// receive "mnpc" - request updates for NPC position
			if (msgTokens[0].compareTo("mnpc") == 0) {
//				System.out.println("client updates for NPC position");
				int npcID = Integer.parseInt(msgTokens[1]);
				Vector3 ghostPosition = Vector3f.createFrom(Float.parseFloat(msgTokens[2]),
						Float.parseFloat(msgTokens[3]), Float.parseFloat(msgTokens[4]));
//				System.out.println("npcID: "+npcID+"    position: "+ghostPosition.toString());
				try {
					updateGhostNPC(npcID, ghostPosition);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public void updateGhostNPC(int ghostID, Vector3 ghostPosition) throws IOException {
		GhostNPC npc = new GhostNPC(ghostID);
		npc.setPosition(ghostPosition);
		ghostNPCs.insertElementAt(npc, ghostID);
		game.updateGhostNPCsToGameWorld(ghostID, ghostPosition);
		if(game.checkNPCAnimation(ghostID, ghostPosition)==1) {
			game.npcDoTheAttack();
		}else {
			game.npcStopTheAttack();
		}
	}

	public void createGhostAvatar(UUID ghostID, Vector3 ghostPosition, int ghostModel) {
		if (ghostAvatars == null) {
			ghostAvatars = new GhostAvatar(ghostID, ghostPosition);
			System.out.println("ghostID: " + ghostAvatars.getID().toString());
			try {
				game.setGhostModel(ghostModel);
				game.addGhostAvatarToGameWorld(ghostAvatars, ghostAvatars.getID(), ghostPosition);
				System.out.println("----------------------------------------here ---------------------------------");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void removeGhostAvatar(UUID ghostID) {
		game.removeGhostAvatarFromGameWorld(ghostAvatars);
	}

	public void moveGhostAvatar(UUID ghostID, Vector3 ghostPosition) throws NullPointerException {
		if (ghostAvatars == null) {
			this.createGhostAvatar(ghostID, ghostPosition, gModel);
		}
		ghostAvatars.move(ghostPosition);
		game.updateGhostPosition(ghostAvatars.getID(), ghostPosition);
		System.out.println("ghost avatar id: " + ghostAvatars.getID().toString());
		System.out.println("position: " + ghostAvatars.getPosition().toString());
	}

	public void rotateGhostAvatar(UUID ghostID, Float ghostRAngle) {
		game.rotateGhostAngle(ghostAvatars.getID(), ghostRAngle);
	}

	public void sendByeMessages() {
		try {
			String message = new String("bye," + id.toString());
			sendPacket(message);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// provides client with updated status of a remote avatar
	public void sendDetailsForMessage(UUID remId, Object pos) {
		try {
			String message = new String("dsfr," + id.toString() + "," + remId.toString());
			message += "," + ((Vector3f) pos).x();
			message += "," + ((Vector3f) pos).y();
			message += "," + ((Vector3f) pos).z();
			sendPacket(message);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendMoveMessages(Object pos) {
		try {
			String message = new String("move," + id.toString());
			message += "," + ((Vector3f) pos).x();
			message += "," + ((Vector3f) pos).y();
			message += "," + ((Vector3f) pos).z();
			System.out.println("Moved character.");
			sendPacket(message); // responsible for sending message over the network
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendRotateGhostAvatar(Float ghostRAngle) {
		try {
			String message = new String("rotate," + id.toString());
			message += "," + ghostRAngle;
			System.out.println("client sending rotate to server 1 ");
			sendPacket(message);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendCreateMessage(Object pos, int model) {
		try {
			String message = new String("create," + id.toString());
			message += "," + ((Vector3f) pos).x() + "," + ((Vector3f) pos).y() + "," + ((Vector3f) pos).z();
			message += "," + model;
			sendPacket(message);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendJoinMessage() {
		try {
			String message = new String("join," + id.toString());
			sendPacket(message);
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Sending join request to the server.");
	}
}
