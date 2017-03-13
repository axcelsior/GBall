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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;

import javax.swing.JOptionPane;

import Shared.Const;
import Shared.KeyMessageData;
import Shared.MsgData;
import Shared.Vector2D;

public class sWorld {

	// Network variables
	public static final String SERVERIP = "127.0.0.1"; // Server always hosts on its own IP, port is selected on startup with the row below.
	public static final int SERVERPORT = Integer.parseInt(JOptionPane.showInputDialog(null, "ServerPort", "Enter ServerPort", JOptionPane.QUESTION_MESSAGE));
	private DatagramSocket m_socket;
	private ClientListener m_clientListener;
	private StringListener m_stringListener;
	private LagSender m_lagSender;
	// This is the roundtime trip for artificial lag, this is set in Const.java
	private int m_ping = (int) Const.PING;
	
	// This replaces busy/wait during handshake phase
	private Semaphore m_hsCheck = new Semaphore(0);

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

		// Initialize server socket
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

			// Send the entire gamestate to all clients, if statement selects frequency. 1 for every frame, 2 for every other frame etc.
			if (updateTimer > 1) {
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
		else // Sleep for the remaining time if not ready for new frame. When this function exits new frame should be processed. This fixed the busy/wait
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

		// Initialize a new thread to listen for handshakes in strings.
		m_stringListener = new StringListener();

		// That thread will release the semaphore once everyone is ready
		try {
			m_hsCheck.acquire();
		} catch (InterruptedException e1) {
			System.err.println("Handshake check never released...");
			e1.printStackTrace();
			System.exit(-1);
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

		
		// We wait for the string listener to die before we start listening for objects.
		try {
			m_stringListener.join();
		} catch (InterruptedException e) {
			
			e.printStackTrace();
		}
		
		// Initate with half the ping (one way)
		m_lagSender = new LagSender(Const.PING / 2.0);
		m_clientListener = new ClientListener(m_stringListener.m_IP, m_stringListener.m_port);	//Start listening for incoming keystrokes from clients
		System.out.println("Initplayers done.");
	}

	public double getActualFps() {

		return m_actualFps;
	}

	public void addKeyListener(KeyListener k) {
		m_gameWindow.addKeyListener(k);
	}
	
	public class LagSender extends Thread {
		
		private Double lag;
		ConcurrentLinkedQueue<DatagramPacket> q = new ConcurrentLinkedQueue<DatagramPacket>();
		ConcurrentLinkedQueue<Double> doubleQ = new ConcurrentLinkedQueue<Double>();
		// The above linked queues should be treated as one, the first packet in q have the timestamp of the first packet in doubleQ etc.
		
		public LagSender (Double miliseconds){
			lag = miliseconds;
			this.start();
		}
		
		public void run(){
			while (true) {
				
				// Sleep if the queues are empty, to avoid busy wait.
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
				
				// If the first packet is not yet ready to be sent we sleep until it is
				if (doubleQ.peek() > System.currentTimeMillis()) {
					try {
						Thread.sleep((long) (doubleQ.peek() - System.currentTimeMillis()));
					} catch (InterruptedException e) {
						System.err.println("Lagsender Overslept");
						e.printStackTrace();
						System.exit(-1);
					}
				}
				
				// Even if we slept or not, remove the timestamp and then send the message.
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
		
		// This is called from another thread (hence concurrent queues) to add packets to the queue and give them a timestamp.
		public void sendMessage(DatagramPacket packet){
			q.add(packet);										//Add the packet first! Otherwise the thread doing run() could see the timestamp and try to send the packet before it has been added.
			doubleQ.add(System.currentTimeMillis() + lag);
		}
	}

	
	// Own thread listening for handshakes in very simplistic strings
	public class StringListener extends Thread {
		private int[] m_port = new int[4];
		private InetAddress[] m_IP = new InetAddress[4];
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

			// Thread dies after 2 seconds if all clients are ready. This to send late acknowledgements and to avoid receiving strings on the object listener
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
				
				// Do not read message if socket timed out
				if (timeout)
					continue;

				String msg = new String(p.getData(), 0, p.getLength());
				String split[] = msg.trim().split(" ");

				// Check for handshake
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
					
					// Send acknowledgement with the correct index found above
					
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

				} else if (split[0].equals("start")) {		// Check for start messages. This is done after clients are done loading to assure no player can move before everyone is ready.
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
						m_hsCheck.release();	// Release the main thread if all players are ready.
						
					}
				}

			} while (deathRoll > 0);	
			// Reset socket timeout for clientListener and shut down the thread.

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

	// Listens for objects containing keyboard input from clients
	public class ClientListener extends Thread {

		private InetAddress m_IP[] = new InetAddress[4];
		private int m_port[] = new int[4];
		Hashtable<String, Integer> m_hash = new Hashtable<String, Integer>();	//Hashtable is used to efficiently locate clients

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
				m_hash.put(m_IP[i].getHostAddress() + ":" + m_port[i], i);	//A string containing "IP:PORT" is bound to easily find client ID
			}
			this.start();
		}

		public void sendState() {
			MsgData[] dataList = sEntityManager.getInstance().getData();
			for (int i = 0; i < 4; i++) {
				// create packet to client with ID i
				byte buf[] = new byte[1024];
				

				for (int j = 0; j < dataList.length; j++) {
					// Serialize data
					DatagramPacket p = new DatagramPacket(buf, buf.length, m_IP[i], m_port[i]);
					p.setData(Serialize(dataList[j]));
					
					// send packet to client with..
					
					// ..artificial lag
					m_lagSender.sendMessage(p);
					
					// ..the socket directly
					/*try {
						m_socket.send(p);
					} catch (IOException e) {
						System.err.println("Error: Cannot send state update");
						e.printStackTrace();
					}*/

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
					m_socket.receive(p);												//Recieve and deserialize
				} catch (IOException e) {
					System.out.println("Error: IOException on recieving packet");
					e.printStackTrace();
				}
				System.out.print("Received client data.");
				data = Deserialize(p.getData());

				// Update keystates with the entity manager, here we use the hashtable to find the correct ID
				sEntityManager.getInstance().setShipKeys(data,
						m_hash.get(p.getAddress().getHostAddress() + ":" + p.getPort()), m_ping);

			} while (true);

		}
		
		// Serialize MsgData to a byte array for outgoing messages
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

		// Deserialize KeyMessageData from a byte array
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