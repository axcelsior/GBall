package GBall;

import java.awt.event.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class World {

	public static final String SERVERIP = "127.0.0.1"; // 'Within' the emulator!  
	public static final int SERVERPORT = 4444; 
    
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
	initPlayers();
	
	// Marshal the state    
	try {
	    ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DatagramSocket m_socket = new DatagramSocket();
		InetAddress m_serverAddress = InetAddress.getByName("localhost");
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
    
	
	while(true) {
	    if(newFrame()) {
		EntityManager.getInstance().updatePositions();
		EntityManager.getInstance().checkBorderCollisions(Const.DISPLAY_WIDTH, Const.DISPLAY_HEIGHT);
		EntityManager.getInstance().checkShipCollisions();
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
	// Team 1
	EntityManager.getInstance().addShip(new Vector2D(Const.START_TEAM1_SHIP1_X, Const.START_TEAM1_SHIP1_Y),
			      new Vector2D(0.0, 0.0),
			      new Vector2D(1.0, 0.0),
			      Const.TEAM1_COLOR,
			      new KeyConfig(KeyEvent.VK_A, KeyEvent.VK_D, KeyEvent.VK_S, KeyEvent.VK_W)
			      );
	
	EntityManager.getInstance().addShip(new Vector2D(Const.START_TEAM1_SHIP2_X, Const.START_TEAM1_SHIP2_Y),
			      new Vector2D(0.0, 0.0),
			      new Vector2D(1.0, 0.0),
			      Const.TEAM1_COLOR,
			      new KeyConfig(KeyEvent.VK_F, KeyEvent.VK_H, KeyEvent.VK_G, KeyEvent.VK_T)
			      );
	
	// Team 2
	EntityManager.getInstance().addShip(new Vector2D(Const.START_TEAM2_SHIP1_X, Const.START_TEAM2_SHIP1_Y),
			      new Vector2D(0.0, 0.0),
			      new Vector2D(-1.0, 0.0),
			      Const.TEAM2_COLOR,
			      new KeyConfig(KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, KeyEvent.VK_DOWN, KeyEvent.VK_UP)
			      );
	
	EntityManager.getInstance().addShip(new Vector2D(Const.START_TEAM2_SHIP2_X, Const.START_TEAM2_SHIP2_Y),
			      new Vector2D(0.0, 0.0),
			      new Vector2D(-1.0, 0.0),
			      Const.TEAM2_COLOR,
			      new KeyConfig(KeyEvent.VK_J, KeyEvent.VK_L, KeyEvent.VK_K, KeyEvent.VK_I)
			      );
	
	// Ball
	EntityManager.getInstance().addBall(new Vector2D(Const.BALL_X, Const.BALL_Y), new Vector2D(0.0, 0.0));
    }

	public double getActualFps() {
		
		return m_actualFps;
	}
	
    public void addKeyListener(KeyListener k) {
    	m_gameWindow.addKeyListener(k);
    }

}