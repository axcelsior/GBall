package GBallClient;

import java.awt.Color;
import java.awt.Font;

public final class Const {
    // World-related constants
    final static double		TARGET_FPS = 50;
    final static double		FRAME_INCREMENT = 1000 / TARGET_FPS;
    final static String		APP_NAME = "Geometry Ball Tournament 2014";
    final static int            DISPLAY_WIDTH = 1024;
    final static int		DISPLAY_HEIGHT = 758;
    final static int            WINDOW_TOP_HEIGHT = 30;
    final static int            WINDOW_BORDER_WIDTH = 5;
    final static int            WINDOW_BOTTOM_HEIGHT = 5;
    final static Color	        BG_COLOR = Color.BLACK;
    final static int		FONT_SIZE = 24;
    
    final static boolean	SHOW_FPS = false;
    final static Color          FPS_TEXT_COLOR = Color.RED;
    final static Vector2D	FPS_TEXT_POSITION = new Vector2D(0.0, 0.0);
    
    final static Vector2D	TEAM1_SCORE_TEXT_POSITION = new Vector2D((double) (DISPLAY_WIDTH / 2) - 120, 52.0);
    final static Vector2D	TEAM2_SCORE_TEXT_POSITION = new Vector2D((double) (DISPLAY_WIDTH / 2) + 120, 52.0);
    final static Font           SCORE_FONT = new Font("Times New Roman", Font.BOLD, FONT_SIZE);

    final static Color	        TEAM1_COLOR = Color.RED;
    final static Color	        TEAM2_COLOR = Color.GREEN;
    
    final static double		START_TEAM1_SHIP1_X = 200.0;
    final static double		START_TEAM1_SHIP1_Y = 100.0;
    final static double		START_TEAM1_SHIP2_X = START_TEAM1_SHIP1_X;
    final static double		START_TEAM1_SHIP2_Y = DISPLAY_HEIGHT - START_TEAM1_SHIP1_Y;
    final static double		START_TEAM2_SHIP1_X = DISPLAY_WIDTH - START_TEAM1_SHIP1_X;
    final static double		START_TEAM2_SHIP1_Y = START_TEAM1_SHIP1_Y;
    final static double		START_TEAM2_SHIP2_X = START_TEAM2_SHIP1_X;
    final static double		START_TEAM2_SHIP2_Y = DISPLAY_HEIGHT - START_TEAM2_SHIP1_Y;
    
    final static double		BALL_X = DISPLAY_WIDTH / 2;
    final static double		BALL_Y = DISPLAY_HEIGHT / 2;     

    // Ship-related constants
    final static int	        SHIP_RADIUS = 22;
    final static double	        SHIP_MAX_ACCELERATION = 400.0;
    final static double	        SHIP_MAX_SPEED = 370.0;
    final static double	        SHIP_BRAKE_SCALE = 0.978; // Scale speed by this factor (per frame) when braking
    final static double	        SHIP_TURN_BRAKE_SCALE = 0.99; // Scale speed by this factor (per frame) when turning
    final static double         SHIP_FRICTION = 0.99; // Scale speed by this factor (per frame) when not accelerating
    final static double 	SHIP_ROTATION = 0.067; // Rotate ship by this many radians (per frame) when turning

    // Ball-related constants
    final static int		BALL_RADIUS = 18;
    final static double	        BALL_MAX_ACCELERATION = 400.0;
    final static double	        BALL_MAX_SPEED = 370.0;
    final static double	        BALL_FRICTION = 0.992;
    final static Color    	BALL_COLOR = Color.WHITE;
}
