package GBall;

import java.awt.Color;
import java.awt.event.*;

import Shared.Const;
import Shared.KeyMessageData;
import Shared.Vector2D;

public class sShip extends sGameEntity{

	private Color m_color;
	private int m_rotation = 0; // Set to 1 when rotating clockwise, -1 when
	private Vector2D m_targetPosition;
    private Vector2D m_targetSpeed;
    private Vector2D m_targetDirection;
								// rotating counterclockwise

	// Keys
	private boolean m_braking;
	private boolean m_forward;
	private boolean m_left;
	private boolean m_right;
	

	public sShip(final Vector2D position, final Vector2D speed, final Vector2D direction, final Color col,int ID) {
		super(position, speed, direction, Const.SHIP_MAX_ACCELERATION, Const.SHIP_MAX_SPEED, Const.SHIP_FRICTION);
		m_color = col;
		m_ID = ID;
	}

	public void updateKeys() {
		 int tmpRotation = 0;
		
		if (m_right) {
			tmpRotation += 1;
		}
		if (m_left) {
			tmpRotation -= 1;
		}
		if (m_forward) {
			setAcceleration(Const.SHIP_MAX_ACCELERATION);
		}
		if (!m_forward) {
			setAcceleration(0);
		}
		
		m_rotation = tmpRotation;
	}
	
	public void delayedUpdate(int ping) {
    	
    	double age = (double) ping / (double) 2;
    	
    	//Predict Direction
    	//Future direction is the recieved direction and spin if its spinning for message age + our interpolation time
    	m_targetDirection = m_direction;
    	if (m_rotation != 0) {
			m_targetDirection.rotate(m_rotation * Const.SHIP_ROTATION * ((age + Const.INTERPOLATION_TIME) / Const.FRAME_INCREMENT));
			scaleSpeed(Const.SHIP_TURN_BRAKE_SCALE);
		}
    	
    	//Predict Speed
    	m_targetSpeed = m_speed;
    	if(m_acceleration > 0) {
		    m_targetSpeed.add(m_targetDirection.multiplyOperator(m_acceleration * (age + Const.INTERPOLATION_TIME)));	// Adds acceleration in the future direction
		    if(m_targetSpeed.length() > m_maxSpeed) {
			    m_targetSpeed.setLength(m_maxSpeed);
			}
		}
    	else if (m_braking) {
			//Friction when breaking
			m_targetSpeed.multiplyOperator(Math.pow(Const.SHIP_BRAKE_SCALE, (age + Const.INTERPOLATION_TIME) / Const.FRAME_INCREMENT));
		} else {
			//Friction when not accelerating
			m_targetSpeed.multiplyOperator(Math.pow(Const.SHIP_FRICTION, (age + Const.INTERPOLATION_TIME) / Const.FRAME_INCREMENT));
		}
    	
    	//Predict Position
    	m_targetPosition = m_position;
    	Vector2D avgSpeed = m_speed;
    	avgSpeed.add(m_targetSpeed);
    	avgSpeed.multiplyOperator(0.5);
    	
    	m_targetPosition.add(avgSpeed.multiplyOperator(age + Const.INTERPOLATION_TIME));
    	
    	
    	//m_interpolationDeadline = System.currentTimeMillis() + (long) Const.INTERPOLATION_TIME;
    	
    	//Temp test
    	
    	setSpeed(m_targetSpeed);
    	setDirection(m_targetDirection);
    	setPosition(m_targetPosition.getX(), m_targetPosition.getY());
    	
    }

	public void setKeys(KeyMessageData keyState) {
		m_braking = keyState.brakeKey;
		m_forward = keyState.forwardKey;
		m_left = keyState.leftKey;
		m_right = keyState.rightKey;
	}
	@Override
	public KeyMessageData getKeyState(){
		KeyMessageData data = new KeyMessageData(m_forward,m_right,m_left,m_braking);
		return data;
	}


	@Override
	public void move() {
		updateKeys();
		if (m_rotation != 0) {
			rotate(m_rotation * Const.SHIP_ROTATION);
			scaleSpeed(Const.SHIP_TURN_BRAKE_SCALE);
		}
		if (m_braking) {
			scaleSpeed(Const.SHIP_BRAKE_SCALE);
			setAcceleration(0);
		}
		super.move();
	}

	@Override
	public void render(java.awt.Graphics g) {
		g.setColor(m_color);
		g.drawOval((int) getPosition().getX() - Const.SHIP_RADIUS, (int) getPosition().getY() - Const.SHIP_RADIUS,
				Const.SHIP_RADIUS * 2, Const.SHIP_RADIUS * 2);

		g.drawLine((int) getPosition().getX(), (int) getPosition().getY(),
				(int) (getPosition().getX() + getDirection().getX() * Const.SHIP_RADIUS),
				(int) (getPosition().getY() + getDirection().getY() * Const.SHIP_RADIUS));
	}

	@Override
	public boolean givesPoints() {
		return false;
	}

	@Override
	public double getRadius() {
		return Const.SHIP_RADIUS;
	}
}