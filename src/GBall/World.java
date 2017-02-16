package GBall;

import java.awt.event.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Hashtable;

public class World {

	public static final String SERVERIP = "127.0.0.1"; // 'Within' the emulator! TODO fix
	public static final int SERVERPORT = 4444;
	private DatagramSocket m_socket;

	private static class WorldSingletonHolder {
		public static final World instance = new World();
	}

	public static World getInstance() {
		return WorldSingletonHolder.instance;
	}

	private double m_lastTime = System.currentTimeMillis();
	private double m_actualFps = 0.0;

	private final GameWindow m_gameWindow = new GameWindow();

	private World() {

	}

	public void process() {
		
		// Marshal the state
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			m_socket = new DatagramSocket();
			InetAddress m_serverAddress = InetAddress.getByName(SERVERIP);
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(new MsgData());
			oos.flush();

			byte[] buf = new byte[1024];

			buf = baos.toByteArray();

			DatagramPacket pack = new DatagramPacket(buf, buf.length, m_serverAddress, SERVERPORT);
			m_socket.send(pack);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		initPlayers();

		while (true) {
			if (newFrame()) {
				// Listen for clients
				EntityManager.getInstance().updatePositions();
				EntityManager.getInstance().checkBorderCollisions(Const.DISPLAY_WIDTH, Const.DISPLAY_HEIGHT);
				EntityManager.getInstance().checkShipCollisions();
				//Send keystrokes
				m_gameWindow.repaint();
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
		byte[] buf = new byte[256];
		DatagramPacket p = new DatagramPacket(buf, buf.length);
		
		for ( int i = 4; i > 0; i--)
		{
			
			try {
				m_socket.receive(p);
			} catch (IOException e) {
				System.out.println("Error: IOException on recieving handshake");
				e.printStackTrace();
			}
			
			String msg = new String(p.getData(), 0, p.getLength());
			
			String split[] = msg.trim().split(" ");
			
			//split[0] TODO: Do the Handshake
			
		}
		
		// Team 1
		EntityManager.getInstance().addShip(new Vector2D(Const.START_TEAM1_SHIP1_X, Const.START_TEAM1_SHIP1_Y),
				new Vector2D(0.0, 0.0), new Vector2D(1.0, 0.0), Const.TEAM1_COLOR,
				new KeyConfig(KeyEvent.VK_A, KeyEvent.VK_D, KeyEvent.VK_S, KeyEvent.VK_W));

		EntityManager.getInstance().addShip(new Vector2D(Const.START_TEAM1_SHIP2_X, Const.START_TEAM1_SHIP2_Y),
				new Vector2D(0.0, 0.0), new Vector2D(1.0, 0.0), Const.TEAM1_COLOR,
				new KeyConfig(KeyEvent.VK_F, KeyEvent.VK_H, KeyEvent.VK_G, KeyEvent.VK_T));

		// Team 2
		EntityManager.getInstance().addShip(new Vector2D(Const.START_TEAM2_SHIP1_X, Const.START_TEAM2_SHIP1_Y),
				new Vector2D(0.0, 0.0), new Vector2D(-1.0, 0.0), Const.TEAM2_COLOR,
				new KeyConfig(KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, KeyEvent.VK_DOWN, KeyEvent.VK_UP));

		EntityManager.getInstance().addShip(new Vector2D(Const.START_TEAM2_SHIP2_X, Const.START_TEAM2_SHIP2_Y),
				new Vector2D(0.0, 0.0), new Vector2D(-1.0, 0.0), Const.TEAM2_COLOR,
				new KeyConfig(KeyEvent.VK_J, KeyEvent.VK_L, KeyEvent.VK_K, KeyEvent.VK_I));

		// Ball
		EntityManager.getInstance().addBall(new Vector2D(Const.BALL_X, Const.BALL_Y), new Vector2D(0.0, 0.0));
	}

	public double getActualFps() {

		return m_actualFps;
	}

	public void addKeyListener(KeyListener k) {
		m_gameWindow.addKeyListener(k);
	}
	
	public class ClientListener extends Thread {
		
		private InetAddress m_IP[] = new InetAddress[4];
		private int m_port[] = new int[4];
		Hashtable<String,Integer> m_hash = new Hashtable<String,Integer>();
		
		public ClientListener(InetAddress IP[], int port[]) {
			m_IP[0] = IP[0];
			m_IP[1] = IP[1];
			m_IP[2] = IP[2];
			m_IP[3] = IP[3];
			
			m_port[0] = port[0];
			m_port[1] = port[1];
			m_port[2] = port[2];
			m_port[3] = port[3];
			
			for (int i = 0; i < 4; i++)
			{
				m_hash.put(m_IP[i].getHostAddress() + ":" + m_port[i], i);
			}
			this.start();
		}
		
		public void run() {
			
			do{
				KeyMessageData data;
				
				byte buf[] = new byte[256];
				DatagramPacket p = new DatagramPacket(buf, buf.length);
				
				
				
				try {
					m_socket.receive(p);
				} catch (IOException e) {
					System.out.println("Error: IOException on recieving packet");
					e.printStackTrace();
				}
				
				
				
				switch(m_hash.get(p.getAddress().getHostAddress() + ":" + p.getPort()))
				{
				case(0):
					//TODO Update this ship with Deserialize(p.getData())
					break;
				case(1):
					break;
				case(2):
					break;
				case(3):
					break;
				default:
					break;
				}
								
				
			}while (true);
			
		}	
		
		private KeyMessageData Deserialize(byte[] byt){
			KeyMessageData r = null;
			
			
			ByteArrayInputStream BaIs /* hehe */ = new ByteArrayInputStream(byt);
			ObjectInputStream ois = null;
			
			try {
				ois = new ObjectInputStream(BaIs);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			try {
				r = (KeyMessageData) ois.readObject();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			return r;
		}
	};

}