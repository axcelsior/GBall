package GBallClient;

import java.io.Serializable;

public class KeyMessageData implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public boolean forwardKey,rightKey,leftKey,brakeKey;
	
    public KeyMessageData(){
    	
    	forwardKey = false;
    	rightKey = false;
    	leftKey = false;
    	brakeKey = false;
    }

    public KeyMessageData(boolean forwardKey,boolean rightKey,boolean leftKey,boolean brakeKey){
    	this.forwardKey = forwardKey;
    	this.rightKey = rightKey;
    	this.leftKey = leftKey;
    	this.brakeKey = brakeKey;
    }

}