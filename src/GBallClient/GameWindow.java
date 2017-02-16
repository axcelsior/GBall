package GBallClient;

import java.awt.*;
import java.awt.event.*;

public class GameWindow extends Frame implements WindowListener {
    
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	//private Image background;
    private Image offScreenImage;
    private Graphics offScreenGraphicsCtx;	// Used for double buffering
    
    //private final static int YOFFSET = 34;
    //private final static int XOFFSET = 4;
    
    public GameWindow() {
        addWindowListener(this);
        	
        setSize(Const.DISPLAY_WIDTH, Const.DISPLAY_HEIGHT);
        setTitle(Const.APP_NAME);
        setVisible(true);
    }

    @Override
    public void update(Graphics g) {
        if (offScreenGraphicsCtx == null) {
            offScreenImage = createImage(getSize().width, getSize().height);
            offScreenGraphicsCtx = offScreenImage.getGraphics();
        }

	offScreenGraphicsCtx.setColor(Const.BG_COLOR);
	offScreenGraphicsCtx.fillRect(0,0,getSize().width,getSize().height);	
	EntityManager.getInstance().renderAll(offScreenGraphicsCtx);
	ScoreKeeper.getInstance().render(offScreenGraphicsCtx);

	if(Const.SHOW_FPS) {
	    offScreenGraphicsCtx.drawString("FPS: " + (int) World.getInstance().getActualFps(), 10, 50);
	}

        // Draw the scene onto the screen
        if(offScreenImage != null){
            g.drawImage(offScreenImage, 0, 0, this);
        }
    }
	
    @Override
    public void paint(Graphics g) {
    }
    
    @Override
    public void windowActivated(WindowEvent e) {}
    @Override
    public void windowClosed(WindowEvent e) {}
    @Override
    public void windowClosing(WindowEvent e) {System.exit(0);}
    @Override
    public void windowDeactivated(WindowEvent e) {}
    @Override
    public void windowDeiconified(WindowEvent e) {}
    @Override
    public void windowIconified(WindowEvent e) {}
    @Override
    public void windowOpened(WindowEvent e) {}
}