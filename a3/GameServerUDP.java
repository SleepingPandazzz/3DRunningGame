package a3;

import java.io.IOException;
import java.net.InetAddress;
import java.util.UUID;

import ray.networking.server.GameConnectionServer;
import ray.networking.server.IClientInfo;
import ray.rml.Angle;
import ray.rml.Degreef;

public class GameServerUDP extends GameConnectionServer<UUID> {
	public GameServerUDP(int localPort) throws IOException {
		super(localPort, ProtocolType.UDP);
	}

	@Override
	public void processPacket(Object o, InetAddress senderIP, int senderPort) {
		String message = (String) o;
		String[] msgTokens = message.split(",");

		if (msgTokens.length > 0) {
			// case where server receives a JOIN message
			if (msgTokens[0].compareTo("join") == 0) {
				try {
					IClientInfo ci = getServerSocket().createClientInfo(senderIP, senderPort);
					UUID clientID = UUID.fromString(msgTokens[1]);
					addClient(ci, clientID);
					sendJoinedMessage(clientID, true);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			// case where server receives a CREATE message
			if (msgTokens[0].compareTo("create") == 0) {
				UUID clientID = UUID.fromString(msgTokens[1]);
				String[] pos = { msgTokens[2], msgTokens[3], msgTokens[4] };
				int model = (int) Float.parseFloat(msgTokens[5]);
				sendCreateMessages(clientID, pos, model);
				sendWantsDetailsMessages(clientID, pos);
			}

			// case where server receives a BYE message
			if (msgTokens[0].compareTo("bye") == 0) {
				UUID clientID = UUID.fromString(msgTokens[1]);
				sendByeMessages(clientID);
				removeClient(clientID);
				System.out.println("server UDP bye");
			}

			// case where server receives a DETAILS-FOR message
			if (msgTokens[0].compareTo("dsfr") == 0) {
				UUID clientID = UUID.fromString(msgTokens[1]);
				UUID remID = UUID.fromString(msgTokens[2]);
				String[] pos = { msgTokens[3], msgTokens[4], msgTokens[5] };
				sendDetailsMessages(clientID, remID, pos);
			}

			// receive "wsds"
			if (msgTokens[0].compareTo("wsds") == 0) {
				UUID clientID = UUID.fromString(msgTokens[1]);
				String[] pos = { msgTokens[2], msgTokens[3], msgTokens[4] };
				sendWantsDetailsMessages(clientID, pos);
				System.out.println("server UDP wantsdetails");
			}

			// case where server receives a MOVE message
			if (msgTokens[0].compareTo("move") == 0) {
				UUID clientID = UUID.fromString(msgTokens[1]);
				String[] pos = { msgTokens[2], msgTokens[3], msgTokens[4] };
				sendMoveMessages(clientID, pos);
				String s = "Client " + clientID.toString() + " moved.";
				System.out.println(s);
			}

			// case where server receives a ROTATE message
			if (msgTokens[0].compareTo("rotate") == 0) {
				UUID clientID = UUID.fromString(msgTokens[1]);
				Float rotAngle = Float.parseFloat(msgTokens[2]);
				System.out.println("server receives the rotation");
				sendRotateMessages(clientID, rotAngle);
			}

			if (msgTokens[0].compareTo("needNPC") == 0) {

			}

			if (msgTokens[0].compareTo("collide") == 0) {

			}
		}
	}

	public void sendByeMessages(UUID clientID) {
		System.out.println("send bye message");
		try {
			String message = "bye, " + clientID.toString();
			forwardPacketToAll(message, clientID);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// informs client that a remote client wants a local status update
	public void sendWantsDetailsMessages(UUID clientID, String[] position) {
		try {
			String message = new String("wsds," + clientID.toString());
			message += "," + position[0];
			message += "," + position[1];
			message += "," + position[2];
			forwardPacketToAll(message, clientID);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// provides client with updated status of a remote avatar
	public void sendDetailsMessages(UUID clientID, UUID remoteId, String[] position) {
		try {
			String message = new String("dsfr," + clientID.toString());
			message += "," + position[0];
			message += "," + position[1];
			message += "," + position[2];
			forwardPacketToAll(message, remoteId);
			String s = "Send details to " + remoteId.toString() + " for " + clientID.toString();
			System.out.println(s);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendMoveMessages(UUID clientID, String[] position) {
		try {
			String message = new String("move," + clientID.toString());
			message += "," + position[0];
			message += "," + position[1];
			message += "," + position[2];
			forwardPacketToAll(message, clientID);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendRotateMessages(UUID clientID, Float rotAngle) {
		try {
			System.out.println("rotate method in server");
			String message = new String("rotate," + clientID.toString());
			message += "," + rotAngle;
			forwardPacketToAll(message, clientID);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendCreateMessages(UUID clientID, String[] position, int model) {
		try {
			String message = new String("create," + clientID.toString());
			message += "," + position[0];
			message += "," + position[1];
			message += "," + position[2];
			message += "," + model;
			forwardPacketToAll(message, clientID);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendJoinedMessage(UUID clientID, boolean success) {
		try {
			String message = new String("join,");
			String s = "Client " + clientID.toString();
			if (success) {
				message += "success";
				s += " joined successfully.";
			} else {
				message += "failure";
				s += " joined failed";
			}
			System.out.println(s);
			sendPacket(message, clientID);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// informs clients of new NPC positions
	public void sendNPCInfo(NPCController npcCtrl) {
		System.out.println("server sending npc info");
		for (int i = 0; i < npcCtrl.getNPCNum(); i++) {
			try {
				String message = new String("mnpc," + Integer.toString(i));
				message += "," + (npcCtrl.getNPC(i)).getX();
				message += "," + (npcCtrl.getNPC(i)).getY();
				message += "," + (npcCtrl.getNPC(i)).getZ();
				sendPacketToAll(message);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

}
