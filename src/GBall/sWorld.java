package GBall;

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
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Hashtable;

import Shared.Const;
import Shared.KeyMessageData;
import Shared.MsgData;
import Shared.Vector2D;

public class sWorld {

	public static final String SERVERIP = "127.0.0.1"; // 'Within' the emulator!
														// TODO fix
	public static final int SERVERPORT = 25025;
	private DatagramSocket m_socket;
	private ClientListener m_clientListener;
	private StringListener m_stringListener;

	private static class WorldSingletonHolder {
		public static final sWorld instance = new sWorld();
	}

	public static sWorld getInstance() {
		return WorldSingletonHolder.instance;
	}

	private double m_lastTime = System.currentTimeMillis();
	private double m_actualFps = 0.0;

	private final sGameWindow m_gameWindow = new sGameWindow();

	private sWorld() {

	}

	public void process() {

		try {
			m_socket = new DatagramSocket(SERVERPORT);
		} catch (SocketException e) {
			System.err.println("Error binding server socket");
			e.printStackTrace();
		}

		initPlayers();

		int updateTimer = 0;

		while (true) {
			if (newFrame()) {
				sEntityManager.getInstance().updatePositions();
				sEntityManager.getInstance().checkBorderCollisions(Const.DISPLAY_WIDTH, Const.DISPLAY_HEIGHT);
				sEntityManager.getInstance().checkShipCollisions();

				updateTimer++;

				m_gameWindow.repaint();
			}

			if (updateTimer > 3) {
				//System.out.println("Sending gamestate to clients.");
				m_clientListener.sendState();
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
		return rv;
	}
	private void initPlayers() {

		m_stringListener = new StringListener();

		while (!m_stringListener.isHandshook()) {
			System.out.println("Waiting for handshakes.");
		}
		System.out.println("Handshake done.");
		// Team 1
		sEntityManager.getInstance().addShip(new Vector2D(Const.START_TEAM1_SHIP1_X, Const.START_TEAM1_SHIP1_Y),
				new Vector2D(0.0, 0.0), new Vector2D(1.0, 0.0), Const.TEAM1_COLOR, 0);

		sEntityManager.getInstance().addShip(new Vector2D(Const.START_TEAM1_SHIP2_X, Const.START_TEAM1_SHIP2_Y),
				new Vector2D(0.0, 0.0), new Vector2D(1.0, 0.0), Const.TEAM1_COLOR, 1);

		// Team 2
		sEntityManager.getInstance().addShip(new Vector2D(Const.START_TEAM2_SHIP1_X, Const.START_TEAM2_SHIP1_Y),
				new Vector2D(0.0, 0.0), new Vector2D(-1.0, 0.0), Const.TEAM2_COLOR, 2);

		sEntityManager.getInstance().addShip(new Vector2D(Const.START_TEAM2_SHIP2_X, Const.START_TEAM2_SHIP2_Y),
				new Vector2D(0.0, 0.0), new Vector2D(-1.0, 0.0), Const.TEAM2_COLOR, 3);

		// Ball
		sEntityManager.getInstance().addBall(new Vector2D(Const.BALL_X, Const.BALL_Y), new Vector2D(0.0, 0.0));

		try {
			m_stringListener.join();
		} catch (InterruptedException e) {
			
			e.printStackTrace();
		}

		m_clientListener = new ClientListener(m_stringListener.m_IP, m_stringListener.m_port);
		System.out.println("Initplayers done.");
	}

	public double getActualFps() {

		return m_actualFps;
	}

	public void addKeyListener(KeyListener k) {
		m_gameWindow.addKeyListener(k);
	}

	public class StringListener extends Thread {
		private int[] m_port = new int[4];
		private InetAddress[] m_IP = new InetAddress[4];
		private Hashtable<String, Integer> m_hash = new Hashtable<String, Integer>();
		private boolean[] m_ready = new boolean[4];
		private int m_handshakes;

		public StringListener() {

			m_handshakes = 0;
			this.start();
		}

		public void run() {

			System.out.println("Waiting for client connections...");

			byte[] buf = new byte[16];
			DatagramPacket p = new DatagramPacket(buf, buf.length);

			// Thread dies after 2 seconds
			int deathRoll = 10;

			try {
				m_socket.setSoTimeout(200);
			} catch (SocketException e1) {
				System.out.println("Error on setting socket timeout");
				e1.printStackTrace();
				System.exit(-1);
			}

			do {
				boolean timeout = false;
				int tmpIndex = m_handshakes - 1;
				boolean newClient = true;
				System.out.println("Waiting for player handshake nr: " + m_handshakes);
				try {
					m_socket.receive(p);
				} catch (SocketTimeoutException e1) {
					timeout = true;
				} catch (IOException e) {
					System.out.println("Error: IOException on recieving handshake");
					e.printStackTrace();
				}
				
				if (isReady())
					deathRoll--;
				
				if (timeout)
					continue;

				String msg = new String(p.getData(), 0, p.getLength());
				String split[] = msg.trim().split(" ");

				// Do the Handshake
				System.out.println("Recieved message: " + msg);
				if (split[0].equals("handshake")) {
					for (int i = 0; i < m_handshakes; i++) {
						if (m_IP[i].equals(p.getAddress())) {
							if (m_port[i] == p.getPort()) {
								tmpIndex = i;
								newClient = false;
								break;
								// Sending old index if already connected client
							}
						}
					}

					if (newClient) {
						// New index if new client

						m_handshakes++;
						tmpIndex++;

					}
					m_IP[tmpIndex] = p.getAddress();
					m_port[tmpIndex] = p.getPort();

					p.setData(("handshake " + tmpIndex).getBytes());
					System.out.println("Sending handshake nr: " + tmpIndex);
					System.out.println("Actually sending: " + new String(p.getData()));

					try {
						m_socket.send(p);
					} catch (IOException e) {
						System.err.println("Error: Cannot send handshake");
						e.printStackTrace();
					}

				} else if (split[0].equals("start")) {
					for (int i = 0; i < m_handshakes; i++) {
						if (m_IP[i].equals(p.getAddress())) {
							if (m_port[i] == p.getPort()) {
								m_ready[i] = true;
								break;
							}
						}
					}

					if (isReady()) {

						p.setData("start".getBytes());
						System.out.println("Sending Start");

						try {
							m_socket.send(p);
						} catch (IOException e) {
							System.err.println("Error: Cannot send start");
							e.printStackTrace();
						}
					}
				}

			} while (deathRoll > 0);

			try {
				m_socket.setSoTimeout(0);
			} catch (SocketException e1) {
				System.out.println("Error on resetting socket timeout");
				e1.printStackTrace();
				System.exit(-1);
			}

			System.out.println("String Listener is shutting down");
		}

		public boolean isHandshook() {
			if (m_handshakes == 4)
				return true;
			else
				return false;
		}

		public boolean isReady() {
			int i = 0;

			for (int j = 0; j < 4; j++)
				if (m_ready[j])
					i++;

			if (i == 4)
				return true;
			else
				return false;

		}
	}

	public class ClientListener extends Thread {

		private InetAddress m_IP[] = new InetAddress[4];
		private int m_port[] = new int[4];
		Hashtable<String, Integer> m_hash = new Hashtable<String, Integer>();

		public ClientListener(InetAddress IP[], int port[]) {
			m_IP[0] = IP[0];
			m_IP[1] = IP[1];
			m_IP[2] = IP[2];
			m_IP[3] = IP[3];

			m_port[0] = port[0];
			m_port[1] = port[1];
			m_port[2] = port[2];
			m_port[3] = port[3];

			for (int i = 0; i < 4; i++) {
				m_hash.put(m_IP[i].getHostAddress() + ":" + m_port[i], i);
			}
			this.start();
		}

		public void sendState() {
			MsgData[] dataList = sEntityManager.getInstance().getData();
			for (int i = 0; i < 4; i++) {
				// create packet on m_IP[i] and port[i]
				byte buf[] = new byte[1024];
				DatagramPacket p = new DatagramPacket(buf, buf.length, m_IP[i], m_port[i]);

				for (int j = 0; j < dataList.length; j++) {
					// Serialize(dataList[j]
					p.setData(Serialize(dataList[j]));
					
					// send packet to client
					try {
						m_socket.send(p);
					} catch (IOException e) {
						System.err.println("Error: Cannot send state update");
						e.printStackTrace();
					}

				}
			}
		}

		public void run() {

			do {
				KeyMessageData data;

				byte buf[] = new byte[1024];
				DatagramPacket p = new DatagramPacket(buf, buf.length);
				System.out.print("Listening for client messages....");
				try {
					m_socket.receive(p);
				} catch (IOException e) {
					System.out.println("Error: IOException on recieving packet");
					e.printStackTrace();
				}
				System.out.print("Received client data.");
				data = Deserialize(p.getData());

				sEntityManager.getInstance().setShipKeys(data,
						m_hash.get(p.getAddress().getHostAddress() + ":" + p.getPort()));

			} while (true);

		}

		private byte[] Serialize(MsgData data) {

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

		private KeyMessageData Deserialize(byte[] byt) {
			KeyMessageData r = null;

			ByteArrayInputStream BaIs /* hehe */ = new ByteArrayInputStream(byt);
			ObjectInputStream ois = null;

			try {
				ois = new ObjectInputStream(BaIs);
			} catch (IOException e) {
				System.err.println("Error: IOException creating inputStream");
				e.printStackTrace();
				System.exit(-1);
			}

			try {
				r = (KeyMessageData) ois.readObject();
			} catch (ClassNotFoundException e) {
				System.err.println("Error: Class not found while deserializing data");
				e.printStackTrace();
				System.exit(-1);
			} catch (IOException e) {
				System.err.println("Error: IOException deserializing data");
				e.printStackTrace();
				System.exit(-1);
			}

			return r;
		}
	};

}