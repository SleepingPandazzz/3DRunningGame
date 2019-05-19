package a3;

import java.io.IOException;
import java.net.InetAddress;
import java.util.UUID;

import ray.networking.IGameConnection;
import ray.networking.server.GameConnectionServer;
import ray.networking.server.IClientInfo;

public class GameServerTCP extends GameConnectionServer<UUID> {
	public GameServerTCP(int localPort) throws IOException {
		super(localPort, ProtocolType.TCP);
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
				sendCreateMessages(clientID, pos);
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

	public void sendCreateMessages(UUID clientID, String[] position) {
		try {
			String message = new String("create," + clientID.toString());
			message += "," + position[0];
			message += "," + position[1];
			message += "," + position[2];
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

}
