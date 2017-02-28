package GBallClient;

import java.awt.event.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Hashtable;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.xml.crypto.dsig.keyinfo.PGPData;

import GBall.sWorld.ClientListener;
import Shared.Const;
import Shared.KeyMessageData;
import Shared.MsgData;
import Shared.ScoreKeeper;
import Shared.Vector2D;

public class cWorld {

	public static final String SERVERIP = "127.0.0.1"; // 'Within' the emulator!
	public static final int SERVERPORT = 25025;

	private static class WorldSingletonHolder {
		public static final cWorld instance = new cWorld();
	}

	public static cWorld getInstance() {
		return WorldSingletonHolder.instance;
	}

	private double m_lastTime = System.currentTimeMillis();
	private double m_actualFps = 0.0;

	private final cGameWindow m_gameWindow = new cGameWindow();

	private DatagramSocket m_socket;
	private ServerListener m_serverListener;
	private LagSender m_lagSender;
	private InetAddress m_serverAddress;
	private int m_ping = 160;

	private cWorld() {

		try {
			m_serverAddress = InetAddress.getByName(SERVERIP);
		} catch (UnknownHostException e) {
			System.err.println("Error: Cannot resolve hostname to IP");
			e.printStackTrace();
			System.exit(-1);
		}

	}

	public void process() {
		try {
			m_socket = new DatagramSocket();
			m_socket.setSoTimeout(200);
		} catch (IOException e) {
			System.err.println("Error while setting socket timeout");
			e.printStackTrace();
			System.exit(-1);
		}

		initPlayers();
		int updateTimer = 0;

		while (true) {
			if (newFrame()) {
				cEntityManager.getInstance().updatePositions();
				cEntityManager.getInstance().checkBorderCollisions(Const.DISPLAY_WIDTH, Const.DISPLAY_HEIGHT);
				cEntityManager.getInstance().checkShipCollisions();
				updateTimer++;
				m_gameWindow.repaint();
			}
			if (updateTimer > 3) {
				// System.out.println("Sending gamestate to clients.");
				m_serverListener.sendPlayerState();
				updateTimer = 0;
			}
		}
	}

	private boolean newFrame() {
		double currentTime = System.currentTimeMillis();
		double delta = currentTime - m_lastTime;
		boolean rv = (delta > Const.FRAME_INCREMENT);
		if (rv) {
			m_lastTime += Const.FRAME_INCREMENT;
			if (delta > 10 * Const.FRAME_INCREMENT) {
				m_lastTime = currentTime;
			}
			m_actualFps = 1000 / delta;
		}
		else //busy wait fixed
		{

			try {
				Thread.sleep((long) (Const.FRAME_INCREMENT - delta));
			} catch (InterruptedException e) {
				System.err.println("Error: overslept");
				e.printStackTrace();
			}
			
			m_lastTime += Const.FRAME_INCREMENT;
			if (delta > 10 * Const.FRAME_INCREMENT) {
				m_lastTime = System.currentTimeMillis();
			}
			
			return true;
		}
		return rv;
	}

	private void initPlayers() {
		System.out.println("Sending handshake");
		int id = -1;

		// Handshake

		id = sendMessage("handshake");

		// Ship creating

		// Team 1
		if (id == 0) {
			cEntityManager.getInstance().addPlayerShip(
					new Vector2D(Const.START_TEAM1_SHIP1_X, Const.START_TEAM1_SHIP1_Y), new Vector2D(0.0, 0.0),
					new Vector2D(1.0, 0.0), Const.TEAM1_COLOR,
					new cKeyConfig(KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, KeyEvent.VK_DOWN, KeyEvent.VK_UP), 0);

		} else {
			cEntityManager.getInstance().addShip(new Vector2D(Const.START_TEAM1_SHIP1_X, Const.START_TEAM1_SHIP1_Y),
					new Vector2D(0.0, 0.0), new Vector2D(1.0, 0.0), Const.TEAM1_COLOR, 0);
		}

		if (id == 1) {
			cEntityManager.getInstance().addPlayerShip(
					new Vector2D(Const.START_TEAM1_SHIP2_X, Const.START_TEAM1_SHIP2_Y), new Vector2D(0.0, 0.0),
					new Vector2D(1.0, 0.0), Const.TEAM1_COLOR,
					new cKeyConfig(KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, KeyEvent.VK_DOWN, KeyEvent.VK_UP), 1);
		} else {
			cEntityManager.getInstance().addShip(new Vector2D(Const.START_TEAM1_SHIP2_X, Const.START_TEAM1_SHIP2_Y),
					new Vector2D(0.0, 0.0), new Vector2D(1.0, 0.0), Const.TEAM1_COLOR, 1);
		}
		// Team 2
		if (id == 2) {
			cEntityManager.getInstance().addPlayerShip(
					new Vector2D(Const.START_TEAM2_SHIP1_X, Const.START_TEAM2_SHIP1_Y), new Vector2D(0.0, 0.0),
					new Vector2D(-1.0, 0.0), Const.TEAM2_COLOR,
					new cKeyConfig(KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, KeyEvent.VK_DOWN, KeyEvent.VK_UP), 2);
		} else {

			cEntityManager.getInstance().addShip(new Vector2D(Const.START_TEAM2_SHIP1_X, Const.START_TEAM2_SHIP1_Y),
					new Vector2D(0.0, 0.0), new Vector2D(-1.0, 0.0), Const.TEAM2_COLOR, 2);
		}

		if (id == 3) {
			cEntityManager.getInstance().addPlayerShip(
					new Vector2D(Const.START_TEAM2_SHIP2_X, Const.START_TEAM2_SHIP2_Y), new Vector2D(0.0, 0.0),
					new Vector2D(-1.0, 0.0), Const.TEAM2_COLOR,
					new cKeyConfig(KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, KeyEvent.VK_DOWN, KeyEvent.VK_UP), 3);
		} else {
			cEntityManager.getInstance().addShip(new Vector2D(Const.START_TEAM2_SHIP2_X, Const.START_TEAM2_SHIP2_Y),
					new Vector2D(0.0, 0.0), new Vector2D(-1.0, 0.0), Const.TEAM2_COLOR, 3);
		}

		// Ball
		cEntityManager.getInstance().addBall(new Vector2D(Const.BALL_X, Const.BALL_Y), new Vector2D(0.0, 0.0));

		// Send ready, this will pause until all players are started
		sendMessage("start");
		try {
			Thread.sleep(200L);
		} catch (InterruptedException e) {
			System.err.println("Error: Client Overslept");
			e.printStackTrace();
			System.exit(-1);
		}
		
		//Initiate with about 80ms (one way)
		m_lagSender = new LagSender((double) m_ping / (double) 2);
		m_serverListener = new ServerListener(m_serverAddress, SERVERPORT);

	}
	
	public class LagSender extends Thread {
		
		private Double lag;
		ConcurrentLinkedQueue<DatagramPacket> q = new ConcurrentLinkedQueue<DatagramPacket>();
		ConcurrentLinkedQueue<Double> doubleQ = new ConcurrentLinkedQueue<Double>();
		
		public LagSender (Double miliseconds){
			lag = miliseconds;
			this.start();
		}
		
		public void run(){
			while (true) {
				
				if ((doubleQ.peek() == null)) {
					
					try {
						Thread.sleep(5L);
					} catch (InterruptedException e) {
						System.err.println("Lagsender Overslept");
						e.printStackTrace();
						System.exit(-1);
					}
					continue;
				}
				
				if (doubleQ.peek() > System.currentTimeMillis()) {
					try {
						Thread.sleep((long) (doubleQ.peek() - System.currentTimeMillis()));
					} catch (InterruptedException e) {
						System.err.println("Lagsender Overslept");
						e.printStackTrace();
						System.exit(-1);
					}
				}
				
				doubleQ.poll();
				
				try {
					m_socket.send(q.poll());
				} catch (IOException e) {
					System.err.println("Error while sending packet");
					e.printStackTrace();
					System.exit(-1);
				}
				
				
				
			}
		}
		
		public void sendMessage(DatagramPacket packet){
			q.add(packet);
			doubleQ.add(System.currentTimeMillis() + lag);
		}
	}

	private int sendMessage(String message) {
		boolean acknowledged = false;

		while (!acknowledged) {
			byte[] buf = new byte[16];
			buf = message.getBytes();

			DatagramPacket p = new DatagramPacket(buf, buf.length, m_serverAddress, SERVERPORT);

			try {
				m_socket.send(p);
			} catch (IOException e) {
				System.err.println("Error: IOException while sending message: " + message);
				e.printStackTrace();
				System.exit(-1);
			}

			byte[] rdBuf = new byte[16];
			DatagramPacket t = new DatagramPacket(rdBuf, rdBuf.length);

			System.out.println("Waiting for response");
			try {
				m_socket.receive(t);
			} catch (SocketTimeoutException e) {
				continue;
			} catch (IOException e) {
				System.err.println("Error: IOException while recieving response");
				e.printStackTrace();
				System.exit(-1);
			}

			String rmsg = new String(t.getData());
			String rsplit[] = rmsg.trim().split(" ");

			System.out.println("Received: " + rmsg);

			if (rmsg.startsWith(message)) {
				acknowledged = true;
				if (rsplit[0].equals("handshake")) {
					return Integer.parseInt(rsplit[1]);
				}
			}

		}
		System.out.print("No ID received");
		return -1;
	}

	public double getActualFps() {

		return m_actualFps;
	}

	public void addKeyListener(KeyListener k) {
		m_gameWindow.addKeyListener(k);
	}

	public class ServerListener extends Thread {

		private InetAddress m_IP;
		private int m_port;

		public ServerListener(InetAddress IP, int port) {
			m_IP = IP;
			m_port = port;

			try {
				m_socket.setSoTimeout(0);
			} catch (SocketException e) {
				System.err.println("Error when disabling socket timeout");
				e.printStackTrace();
				System.exit(-1);
			}

			this.start();
		}

		public void sendPlayerState() {
			KeyMessageData state = cEntityManager.getInstance().getPlayerState();
			byte[] data = new byte[1024];
			data = Serialize(state);
			//System.out.println("Sending data from client");
			DatagramPacket p = new DatagramPacket(data, data.length, m_IP, m_port);

			
			// Call the LagSender to send it
			m_lagSender.sendMessage(p);
			
			/*try {
				m_socket.send(p);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}*/

		}

		public void run() {

			do {
				MsgData data = null;

				byte buf[] = new byte[1024];
				DatagramPacket p = new DatagramPacket(buf, buf.length);

				try {
					m_socket.receive(p);
				} catch (IOException e) {
					System.out.println("Error: IOException on recieving packet");
					e.printStackTrace();
					System.exit(-1);
				}
				data = Deserialize(p.getData());
				
				//System.out.println("Received: " + data.m_ID + "id. " + data.m_position.getX() + ", " + data.m_position.getY() + "pos.");

				cEntityManager.getInstance().updateShipData(data, m_ping);
				ScoreKeeper.getInstance().setScores(data.m_team1Score, data.m_team2Score);

			} while (true);

		}

		private byte[] Serialize(KeyMessageData data) {

			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ObjectOutput out = null;
			byte[] buf = null;
			try {
				out = new ObjectOutputStream(bos);
				out.writeObject(data);
				out.flush();
				buf = bos.toByteArray();

			} catch (IOException e) {
				System.err.println("Error: IOException serializing data");
				e.printStackTrace();
				System.exit(-1);
			}

			return buf;
		}

		private MsgData Deserialize(byte[] byt) {
			MsgData r = null;
			ByteArrayInputStream BaIs /* hehe */ = new ByteArrayInputStream(byt);
			ObjectInputStream ois = null;

			try {
				ois = new ObjectInputStream(BaIs);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			try {
				r = (MsgData) ois.readObject();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			return r;
		}
	}
}