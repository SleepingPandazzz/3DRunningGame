package a3;

import java.io.IOException;

public class NetworkingServer {
	private GameServerUDP thisUDPServer;
	private GameServerTCP thisTCPServer;

	private NPCController npcCtrl;

	private long startTime, lastUpdateTime;
	private long thinkStartTime, tickStartTime, lastThinkUpdateTime, lastTickUpdateTime;

	public NetworkingServer(int serverPort, String protocol) {
		npcCtrl = new NPCController();

		// start networking server
		try {
			if (protocol.toUpperCase().compareTo("TCP") == 0) {
				thisTCPServer = new GameServerTCP(serverPort);
			} else {
				thisUDPServer = new GameServerUDP(serverPort);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		// start NPC control loop
		startTime = System.nanoTime();
		lastUpdateTime = startTime;
		thinkStartTime = System.nanoTime();
		tickStartTime = System.nanoTime();
		lastThinkUpdateTime = thinkStartTime;
		lastTickUpdateTime = tickStartTime;
		npcCtrl.setupNPCs();
		npcCtrl.setupBehaviorTree();
		npcLoop();
	}

	public void npcLoop() {
		while (true) {
			long frameStartTime = System.nanoTime();
			long currentTime = System.nanoTime();
			float elapMilSecs = (frameStartTime - lastUpdateTime) / 1000000.0f;
			float elapsedThinkMilliSecs = (currentTime-lastThinkUpdateTime)/1000000.0f;
			float elapsedTickMilliSecs = (currentTime-lastTickUpdateTime)/1000000.0f;
			
			if (elapMilSecs >= 300.0f) {
				lastUpdateTime = frameStartTime;
				lastTickUpdateTime = currentTime;
				npcCtrl.updateNPCs();
				thisUDPServer.sendNPCInfo(npcCtrl);
			}
			
			if(elapsedThinkMilliSecs >= 500.0f) {
				System.out.println("500.0f -------------------------------------");
				lastThinkUpdateTime = currentTime;
				npcCtrl.bt.update(elapsedThinkMilliSecs);
			}
			
			Thread.yield();
		}
	}

	public static void main(String[] args) {
		if (args.length > 1) {
			System.out.println("Hosting a server here.");
			NetworkingServer app = new NetworkingServer(Integer.parseInt(args[0]), args[1]);
		}
	}
}
