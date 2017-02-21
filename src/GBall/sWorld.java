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
import java.net.SocketException;
import java.util.Hashtable;

import Shared.Const;
import Shared.KeyMessageData;
import Shared.MsgData;
import Shared.Vector2D;

public class sWorld {

	public static final String SERVERIP = "127.0.0.1"; // 'Within' the emulator! TODO fix
	public static final int SERVERPORT = 25025;
	private DatagramSocket m_socket;
	private ClientListener m_clientListener;

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

		while (true) {
			if (newFrame()) {
				// Listen for clients
				sEntityManager.getInstance().updatePositions();
				sEntityManager.getInstance().checkBorderCollisions(Const.DISPLAY_WIDTH, Const.DISPLAY_HEIGHT);
				sEntityManager.getInstance().checkShipCollisions();
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
		int[] ports = new int[4];
		InetAddress[] IP_list = new InetAddress[4];
		
		for ( int i = 0; i < 4; i++)
		{
			
			System.out.println("Waiting for player handshake nr: " + i);
			try {
				m_socket.receive(p);
			} catch (IOException e) {
				System.out.println("Error: IOException on recieving handshake");
				e.printStackTrace();
			}
			
			
			String msg = new String(p.getData(), 0, p.getLength());
			String split[] = msg.trim().split(" ");
			
			//split[0] TODO: Do the Handshake
			if (split[0].equals("handshake")){
				IP_list[i] = p.getAddress();
				ports[i] = p.getPort();
				
				p.setData(("handshake " + i).getBytes());
				System.out.println("Sending handshake nr: " + i);
				System.out.println("Actually sending: " + new String(p.getData()));
				
				try {
					m_socket.send(p);
				} catch (IOException e) {
					System.err.println("Error: Cannot send handshake");
					e.printStackTrace();
				}
				
			}
			
		}
		m_clientListener = new ClientListener(IP_list,ports);
		
		// Team 1
		sEntityManager.getInstance().addShip(new Vector2D(Const.START_TEAM1_SHIP1_X, Const.START_TEAM1_SHIP1_Y),
				new Vector2D(0.0, 0.0), new Vector2D(1.0, 0.0), Const.TEAM1_COLOR,0);

		sEntityManager.getInstance().addShip(new Vector2D(Const.START_TEAM1_SHIP2_X, Const.START_TEAM1_SHIP2_Y),
				new Vector2D(0.0, 0.0), new Vector2D(1.0, 0.0), Const.TEAM1_COLOR,1);

		// Team 2
		sEntityManager.getInstance().addShip(new Vector2D(Const.START_TEAM2_SHIP1_X, Const.START_TEAM2_SHIP1_Y),
				new Vector2D(0.0, 0.0), new Vector2D(-1.0, 0.0), Const.TEAM2_COLOR,2);

		sEntityManager.getInstance().addShip(new Vector2D(Const.START_TEAM2_SHIP2_X, Const.START_TEAM2_SHIP2_Y),
				new Vector2D(0.0, 0.0), new Vector2D(-1.0, 0.0), Const.TEAM2_COLOR,3);

		// Ball
		sEntityManager.getInstance().addBall(new Vector2D(Const.BALL_X, Const.BALL_Y), new Vector2D(0.0, 0.0));
		
		//TODO: Send start message!
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