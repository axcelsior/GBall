package GBallClient;

import java.awt.*;
import java.awt.event.*;

import Shared.Const;

public class cGameWindow extends Frame implements WindowListener {
    
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	//private Image background;
    private Image offScreenImage;
    private Graphics offScreenGraphicsCtx;	// Used for double buffering
    
    //private final static int YOFFSET = 34;
    //private final static int XOFFSET = 4;
    
    public cGameWindow() {
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
	cEntityManager.getInstance().renderAll(offScreenGraphicsCtx);
	cScoreKeeper.getInstance().render(offScreenGraphicsCtx);

	if(Const.SHOW_FPS) {
	    offScreenGraphicsCtx.drawString("FPS: " + (int) cWorld.getInstance().getActualFps(), 10, 50);
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