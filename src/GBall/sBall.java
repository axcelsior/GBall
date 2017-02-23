package GBall;

import java.awt.Color;
import Shared.Vector2D;
import java.awt.Graphics;

import Shared.Const;

public class sBall extends sGameEntity {
    private Color m_color;
    
    public sBall(final Vector2D position, final Vector2D speed) {
	super(position, speed, new Vector2D(0, 0), Const.BALL_MAX_ACCELERATION, Const.BALL_MAX_SPEED, Const.BALL_FRICTION);
	m_color = Const.BALL_COLOR;
	m_ID = 5;
    }

    @Override
    public void render(Graphics g) {
	g.setColor(m_color);
	g.drawOval((int) getPosition().getX() - Const.BALL_RADIUS,
		   (int) getPosition().getY() - Const.BALL_RADIUS,
		   Const.BALL_RADIUS * 2,
		   Const.BALL_RADIUS * 2
		  );
    }

    @Override
    public double getRadius() {
	return Const.BALL_RADIUS;
    }

    @Override
    public boolean givesPoints() {
	return true;
    };
}