package Shared;

import java.io.Serializable;

public class MsgData implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public Vector2D m_position;
    public Vector2D m_speed;
    public Vector2D m_direction;	// Should always be unit vector; determines the object's facing
    
    public int m_ID;
    public KeyMessageData m_keyState;
	
    public MsgData(){
    	
    	m_position = new Vector2D();
    	m_speed = new Vector2D();
    	m_direction = new Vector2D();
    }

    public MsgData(Vector2D position, Vector2D speed, Vector2D direction, KeyMessageData keyState, int ID){
    	
    	m_position = position;
    	m_speed = speed;
    	m_direction = direction;
    	
    	m_keyState = keyState;
    	m_ID = ID;
    }

}


