package GBallClient;

import java.io.Serializable;

import Shared.Vector2D;
import Shared.Const;

public abstract class cGameEntity implements Serializable{
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final Vector2D m_position;
    private final Vector2D m_initialPosition;
    private final Vector2D m_initialDirection;
    private final Vector2D m_speed;
    private final Vector2D m_direction;	// Should always be unit vector; determines the object's facing
    protected int m_rotation = 0; // Set to 1 when rotating clockwise, -1 when
    
    private Vector2D m_targetPosition;
    private Vector2D m_targetSpeed;
    private Vector2D m_targetDirection;
    private Vector2D m_deltaPosition;
    private Vector2D m_deltaSpeed;
    private double m_deltaDirection;
    public boolean m_colliding;
    private Vector2D m_collisionPrediction;
    private Vector2D m_collisionDelta;
    private double m_collisionDeadline;
 
    private double m_acceleration;	// Accelerates by multiplying this with m_direction
    private long   m_lastUpdateTime;
    private double m_maxAcceleration;
    private double m_maxSpeed;
    private double m_friction;
    protected int m_ID;
    private long m_interpolationDeadline = 0;

    public abstract void render(java.awt.Graphics g);
    public abstract double getRadius();
    public abstract boolean givesPoints();
    
    public cGameEntity(final Vector2D position, final Vector2D speed, final Vector2D direction, double maxAcceleration, double maxSpeed, double friction) {
	m_position = position;
	m_speed = speed;
	m_direction = direction;
	m_maxAcceleration = maxAcceleration;
	m_friction = friction;
	m_maxSpeed = maxSpeed;
	m_acceleration = 0;
	m_lastUpdateTime = System.currentTimeMillis(); 
	m_initialPosition = new Vector2D(position.getX(), position.getY());
	m_initialDirection = new Vector2D(direction.getX(), direction.getY());
	m_collisionPrediction = new Vector2D(0,0);
	
    }

    public void setAcceleration(double a) {
	if(a > m_maxAcceleration) {
	    m_acceleration = m_maxAcceleration;
	}
	else if(a < (-m_maxAcceleration)) {
	    m_acceleration = -m_maxAcceleration;
	}
	else m_acceleration = a;
    }
    
    public int getID() {
    	return m_ID;
    }

    public void move() {
		// Change to per-frame movement by setting delta to a constant
		// Such as 0.017 for ~60FPS
	
		long currentTime = System.currentTimeMillis();
		double delta = (double) (currentTime - m_lastUpdateTime) / (double) 1000;
		
		//changeSpeed(m_deltaSpeed);
	
		if(m_acceleration > 0) {
		    changeSpeed(m_direction.multiplyOperator(m_acceleration * delta));
		}
		else scaleSpeed(m_friction);
		
		
		//TODO Get this to turn off better. Like a deadline or something
		if (m_colliding) {
			if (m_collisionDeadline >= System.currentTimeMillis())
				m_position.add(m_collisionDelta.multiplyOperator(delta));
			else
				m_colliding = false;
		}
		
			
		if (m_interpolationDeadline >= System.currentTimeMillis()) {
			if (!m_colliding)
				m_position.add(m_deltaPosition.multiplyOperator(delta));
			
			m_direction.rotate(m_deltaDirection);
			
		}
	
		m_position.add(m_speed.multiplyOperator(delta));
		
		
		m_lastUpdateTime = currentTime;
    }
        
    public void delayedUpdate(double ping, Vector2D speed, Vector2D position, Vector2D direction, boolean breaking) {
    	
    	double age = ping / 2.0;
    	
    	//Predict Direction
    	//Future direction is the recieved direction and spin if its spinning for message age + our interpolation time
    	m_targetDirection = direction;
    	if (m_rotation != 0) {
			m_targetDirection.rotate(m_rotation * Const.SHIP_ROTATION * ((age + Const.INTERPOLATION_TIME) / Const.FRAME_INCREMENT));
			scaleSpeed(Const.SHIP_TURN_BRAKE_SCALE);
		}
    	
    	
    	//Predict Speed
		m_targetSpeed = speed;
    	if(m_acceleration > 0) {
		    m_targetSpeed.add(m_targetDirection.multiplyOperator(m_acceleration * (age + Const.INTERPOLATION_TIME) / (double) 1000));	// Adds acceleration in the future direction
		    if(m_targetSpeed.length() > m_maxSpeed) {
			    m_targetSpeed.setLength(m_maxSpeed);
			}
		}
    	else if (breaking) {
			//Friction when breaking
			m_targetSpeed.scale(Math.pow(Const.SHIP_BRAKE_SCALE, (age + Const.INTERPOLATION_TIME) / Const.FRAME_INCREMENT));
		} else {
			//Friction when not accelerating
			m_targetSpeed.scale(Math.pow(Const.BALL_FRICTION, (age + Const.INTERPOLATION_TIME) / Const.FRAME_INCREMENT));
		}
    	
    	//Predict Position
    	m_targetPosition = position;
    	Vector2D avgSpeed = speed;
    	avgSpeed.add(m_targetSpeed);
    	avgSpeed.scale(0.5);
    	
    	Vector2D avgDirection = m_targetDirection;
    	avgDirection.add(direction);
    	avgDirection.scale(0.5);
    	avgDirection.setLength(1);
    	
    	m_targetPosition.add(avgSpeed.multiplyOperator((age + Const.INTERPOLATION_TIME) / (double) 1000));
    	
    	m_targetPosition.add(avgDirection.multiplyOperator(m_acceleration * Math.pow(((age + Const.INTERPOLATION_TIME) / (double) 1000), 2.0) / 2.0));
    	
    	
    	//Predict wall collisions
    	
    	
    	double newX = m_targetPosition.getX();
		double newY = m_targetPosition.getY();
		double radius = getRadius();

		if (newX + radius > (Const.DISPLAY_WIDTH - Const.WINDOW_BORDER_WIDTH)) {
			m_targetPosition.set(Const.DISPLAY_WIDTH - radius - Const.WINDOW_BORDER_WIDTH - ((newX + radius) - (Const.DISPLAY_WIDTH + Const.WINDOW_BORDER_WIDTH)), m_targetPosition.getY());
			if (!m_colliding) {	
				newX = Const.DISPLAY_WIDTH - radius - Const.WINDOW_BORDER_WIDTH;
				m_colliding = true;
				m_collisionDeadline = System.currentTimeMillis() + (Const.INTERPOLATION_TIME );
			}
		} else if ((newX - getRadius()) < Const.WINDOW_BORDER_WIDTH) {
			m_targetPosition.set(- newX + radius + Const.WINDOW_BORDER_WIDTH, m_targetPosition.getY());
			
			if (!m_colliding) {
				newX = radius + Const.WINDOW_BORDER_WIDTH;
				m_colliding = true;
				m_collisionDeadline = System.currentTimeMillis() + (Const.INTERPOLATION_TIME );
			}
		}

		if (newY + radius > (Const.DISPLAY_HEIGHT - Const.WINDOW_BOTTOM_HEIGHT)) {
			m_targetPosition.set(m_targetPosition.getX(), Const.DISPLAY_HEIGHT - radius - Const.WINDOW_BOTTOM_HEIGHT - ((newY + radius) - (Const.DISPLAY_HEIGHT + Const.WINDOW_BOTTOM_HEIGHT)));
			if (!m_colliding) {
				newY = Const.DISPLAY_HEIGHT - radius - Const.WINDOW_BOTTOM_HEIGHT;
				m_colliding = true;
				m_collisionDeadline = System.currentTimeMillis() + (Const.INTERPOLATION_TIME );
			}
		} else if (newY - radius < Const.WINDOW_TOP_HEIGHT) {
			m_targetPosition.set(m_targetPosition.getX(), - newY + radius + Const.WINDOW_BOTTOM_HEIGHT);
			if (!m_colliding) {
				newY = radius + Const.WINDOW_TOP_HEIGHT;
				m_colliding = true;
				m_collisionDeadline = System.currentTimeMillis() + (Const.INTERPOLATION_TIME );
			}
		}
		

		if (!m_colliding) {
			m_collisionPrediction.set(newX, newY);
			
			m_collisionDelta = m_collisionPrediction;
			m_collisionDelta.subtract(m_position);
			m_collisionDelta.scale(1000.0 / Const.INTERPOLATION_TIME);
		}

	
    	
    	m_interpolationDeadline = System.currentTimeMillis() + (long) Const.INTERPOLATION_TIME;
    	
    	
    	//Speed at which to move to get into position after the interpolation time
    	m_deltaPosition = m_targetPosition;
    	m_deltaPosition.subtract(m_position);
    	m_deltaPosition.scale(1000.0 / Const.INTERPOLATION_TIME);
    	
    	
    	//Same for Rotation
    	double rad1;
    	double rad2;

    	rad1 = Math.atan2(m_targetDirection.getY(), m_targetDirection.getX());
    	rad2 = Math.atan2(m_direction.getY(), m_direction.getX());
    	
    	m_deltaDirection = rad1 - rad2;
    	
    	//If the spin is more than half a rotation, spin the other way.
    	if (m_deltaDirection < - Math.PI) 
    		m_deltaDirection += (Math.PI * 2);
    	
    	if (m_deltaDirection > Math.PI)
    		m_deltaDirection -= (Math.PI * 2);
    		
    	
    	m_deltaDirection /= ((Const.INTERPOLATION_TIME) / Const.FRAME_INCREMENT);
    	
    	//TODO Do this with speed *not active*
    	m_deltaSpeed = speed;
    	m_deltaSpeed.add(m_targetSpeed);
    	m_deltaSpeed.scale(0.5);
    	
    	//System.out.println("Predicting\nSpeed: " + m_targetSpeed.length() + "\nDirection x=" + m_targetDirection.getX() + " y=" + m_targetDirection.getY() + "\nPosition x=" + m_targetPosition.getX() + " y=" + m_targetPosition.getY() + "\niSpeed x=" + m_iSpeed.getX() + " y=" + m_iSpeed.getY());
    	
    	
    }

    public void scaleSpeed(double scale) {
		m_speed.scale(scale);
		if(m_speed.length() > m_maxSpeed) {
		    m_speed.setLength(m_maxSpeed);
		}
    }

    public void changeSpeed(final Vector2D delta) {
	m_speed.add(delta);
		if(m_speed.length() > m_maxSpeed) {
		    m_speed.setLength(m_maxSpeed);
		}
    }

    public void resetPosition() {
	m_position.set(m_initialPosition.getX(), m_initialPosition.getY());
	m_direction.set(m_initialDirection.getX(), m_initialDirection.getY());
	m_speed.set(0.0, 0.0);
    }

    public void deflectX() {
	m_speed.setX(-m_speed.getX());
    }

    public void deflectY() {
	m_speed.setY(-m_speed.getY());
    }

    public void rotate(double radians) {
	m_direction.rotate(radians);
    }

    public Vector2D getPosition() {
	return m_position;
    }
    
    public void setPosition(double x, double y) {
	m_position.set(x, y);
    }

    public Vector2D getSpeed() {
	return m_speed;
    }
    
    public void setSpeed(Vector2D speed) {
    	m_speed.set(speed.getX(), speed.getY());
    }
    
    public Vector2D getDirection() {
	return m_direction;
    }
    
    public void setDirection(Vector2D direction) {
    	m_direction.set(direction.getX(), direction.getY());
    }

    public void displace(final Vector2D displacement) {
	m_position.add(displacement);
    }
    
}

