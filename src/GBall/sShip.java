package GBall;

import java.awt.Color;
import java.awt.event.*;

import Shared.Const;
import Shared.KeyMessageData;
import Shared.Vector2D;

public class sShip extends sGameEntity{

	private Color m_color;
	private int rotation = 0; // Set to 1 when rotating clockwise, -1 when
								// rotating counterclockwise
	private boolean braking = false;

	// Keys
	boolean m_braking;
	boolean m_forward;
	boolean m_left;
	boolean m_right;
	

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
		
		rotation = tmpRotation;
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
		if (rotation != 0) {
			rotate(rotation * Const.SHIP_ROTATION);
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