package GBallClient;

import java.awt.event.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Hashtable;

import javax.xml.crypto.dsig.keyinfo.PGPData;

import GBall.sWorld.ClientListener;
import Shared.Const;
import Shared.KeyMessageData;
import Shared.MsgData;
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
    private InetAddress m_serverAddress;

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
    	} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	
	
	initPlayers();
    
	
	while(true) {
	    if(newFrame()) {
		cEntityManager.getInstance().updatePositions();
		cEntityManager.getInstance().checkBorderCollisions(Const.DISPLAY_WIDTH, Const.DISPLAY_HEIGHT);
		cEntityManager.getInstance().checkShipCollisions();
		m_gameWindow.repaint();
	    }
	}
    }

    private boolean newFrame() {
		double currentTime = System.currentTimeMillis();
		double delta = currentTime - m_lastTime;
		boolean rv = (delta > Const.FRAME_INCREMENT);
		if(rv) {
		    m_lastTime += Const.FRAME_INCREMENT;
		    if(delta > 10 * Const.FRAME_INCREMENT) {
			m_lastTime = currentTime;
		    }
		    m_actualFps = 1000 / delta;
		}
		return rv;
    }

    private void initPlayers() {
    	System.out.println("Sending handshake");
    	int id = -1;
    	
    	//TODO Make handshake resistant to packet loss
    	
    	
    	byte[] buf = new byte[256];
    	buf = "handshake".getBytes();
    	
		DatagramPacket p = new DatagramPacket(buf, buf.length, m_serverAddress, SERVERPORT);
		
		try {
			m_socket.send(p);
		} catch (IOException e) {
			System.err.println("Error: IOException while sending handshake");
			e.printStackTrace();
			System.exit(-1);
		}
		
		byte[] rBuf = new byte[256];
		
		DatagramPacket received = new DatagramPacket(rBuf, rBuf.length);
		
		System.out.println("Waiting for response");
		try {
			m_socket.receive(received);
		} catch (IOException e) {
			System.err.println("Error: IOException while recieving handshake response");
			e.printStackTrace();
			System.exit(-1);
		}
				
		String msg = new String(received.getData());
		String split[] = msg.trim().split(" ");
		
		System.out.println("Received: " + msg);
		if (split[0].equals("handshake"))
			id = Integer.parseInt(split[1]);
		
		
	
		m_serverListener = new ServerListener(m_serverAddress, SERVERPORT);
    	
    	
    	//Ship creating	
    	
		// Team 1
    	if (id == 0){
    		cEntityManager.getInstance().addPlayerShip(new Vector2D(Const.START_TEAM1_SHIP1_X, Const.START_TEAM1_SHIP1_Y),
    				new Vector2D(0.0, 0.0),
				    new Vector2D(1.0, 0.0),
				    Const.TEAM1_COLOR,
				    new cKeyConfig(KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, KeyEvent.VK_DOWN, KeyEvent.VK_UP),
				    0
				    );
    		
    	}else{
		cEntityManager.getInstance().addShip(new Vector2D(Const.START_TEAM1_SHIP1_X, Const.START_TEAM1_SHIP1_Y),
				      new Vector2D(0.0, 0.0),
				      new Vector2D(1.0, 0.0),
				      Const.TEAM1_COLOR,
				      0
				      );
    	}
    	
    	if (id == 1){
    		cEntityManager.getInstance().addPlayerShip(new Vector2D(Const.START_TEAM1_SHIP2_X, Const.START_TEAM1_SHIP2_Y),
				      new Vector2D(0.0, 0.0),
				      new Vector2D(1.0, 0.0),
				      Const.TEAM1_COLOR,
				      new cKeyConfig(KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, KeyEvent.VK_DOWN, KeyEvent.VK_UP),
				      1
				      );
    	}else{
		cEntityManager.getInstance().addShip(new Vector2D(Const.START_TEAM1_SHIP2_X, Const.START_TEAM1_SHIP2_Y),
				      new Vector2D(0.0, 0.0),
				      new Vector2D(1.0, 0.0),
				      Const.TEAM1_COLOR,
				      1
				      );
    	}
		// Team 2
    	if (id == 2){
    		cEntityManager.getInstance().addPlayerShip(new Vector2D(Const.START_TEAM2_SHIP1_X, Const.START_TEAM2_SHIP1_Y),
				      new Vector2D(0.0, 0.0),
				      new Vector2D(-1.0, 0.0),
				      Const.TEAM2_COLOR,
				      new cKeyConfig(KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, KeyEvent.VK_DOWN, KeyEvent.VK_UP),
				      2
				      );
    	}else{
    	
    		cEntityManager.getInstance().addShip(new Vector2D(Const.START_TEAM2_SHIP1_X, Const.START_TEAM2_SHIP1_Y),
				      new Vector2D(0.0, 0.0),
				      new Vector2D(-1.0, 0.0),
				      Const.TEAM2_COLOR,
				      2
				      );
    	}
    	
    	if (id == 3){
    		cEntityManager.getInstance().addPlayerShip(new Vector2D(Const.START_TEAM2_SHIP2_X, Const.START_TEAM2_SHIP2_Y),
				      new Vector2D(0.0, 0.0),
				      new Vector2D(-1.0, 0.0),
				      Const.TEAM2_COLOR,
				      new cKeyConfig(KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, KeyEvent.VK_DOWN, KeyEvent.VK_UP),
				      3
				      );
    	}else{
			cEntityManager.getInstance().addShip(new Vector2D(Const.START_TEAM2_SHIP2_X, Const.START_TEAM2_SHIP2_Y),
					   new Vector2D(0.0, 0.0),
					   new Vector2D(-1.0, 0.0),
					   Const.TEAM2_COLOR,
					   3
					   );
    	}
    	
		// Ball
		cEntityManager.getInstance().addBall(new Vector2D(Const.BALL_X, Const.BALL_Y), new Vector2D(0.0, 0.0));
		
		//TODO Stop here until told to start
		
		//m_socket.recieve
		
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

			this.start();
		}
		
		public void run() {
			
			do{
				MsgData data = null;
				
				byte buf[] = new byte[256];
				DatagramPacket p = new DatagramPacket(buf, buf.length);
				
				
				
				try {
					m_socket.receive(p);
				} catch (IOException e) {
					System.out.println("Error: IOException on recieving packet");
					e.printStackTrace();
				}
				
				
				
				switch(data.m_ID)
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
    }
}