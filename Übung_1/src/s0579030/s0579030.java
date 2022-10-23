package s0579030;

import java.awt.Color;
import java.awt.Point;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.util.Random;

import lenz.htw.ai4g.ai.AI;
import lenz.htw.ai4g.ai.DivingAction;
import lenz.htw.ai4g.ai.Info;
import lenz.htw.ai4g.ai.PlayerAction;

public class s0579030 extends AI {
	private int pearlsFound;
	Point[] pearls = info.getScene().getPearl(); // Pearls in current level
	int numRays = 5; // Amount of rays
	int obstacleCheckpointAmount = 10;
	int viewFieldAngle = 120; // Angle of view field
	float fleeThreshold; // Distance to obstacle when starting to flee

	public s0579030(Info info) {
		super(info);
		enlistForTournament(579030, 577618);
		pearlsFound = 0;
		
		// Sort pearls by x value with bubble sort
		for(int i = 0; i < pearls.length - 1; i++) {
			for(int j = 0; j < pearls.length - i - 1; j++) {
				if(pearls[j].getX() > pearls[j + 1].getX()) {
					Point temp = pearls[j];
					pearls[j] = pearls[j + 1];
					pearls[j + 1] = temp;
				}
			}
		}
	}

	@Override
	public String getName() {
		return "Rakete";
	}

	@Override
	public Color getPrimaryColor() {
		return Color.YELLOW;
	}

	@Override
	public Color getSecondaryColor() {
		return Color.BLUE;
	}

	@Override
	public PlayerAction update() {
		// Get diver position
		double startX = info.getX();
		double startY = info.getY();
		Vector2D startVector = new Vector2D((float) startX, (float) startY);
		
		// Get pearl position
		double seekZielX = pearls[pearlsFound].getX();
		double seekZielY = pearls[pearlsFound].getY();
		Vector2D seekVector = new Vector2D((float) seekZielX, (float) seekZielY);
		
		float distanceToPearl = (int) Math.sqrt(Math.pow(startY - seekZielY, 2) + Math.pow(startX - seekZielX, 2));
		
		// Check if pearl was found
		if(info.getScore()!= pearlsFound) {
			pearls[pearlsFound] = null;
			pearlsFound++;
		}
		
		// Seek pearl
		Vector2D seekDirection = seekVector.subtractVector(startVector);
		seekDirection = seekDirection.normalize();
		
		// Get obstacles
		Path2D[] obstacles = info.getScene().getObstacles();
		
		// Distance to an obstacle for each ray
		int[] distanceToObstacle = new int[numRays];
		
		// Nearest obstacle point for each ray
		Point2D[] nearestObstaclePoints = new Point2D[numRays];
		
		int angleBetweenRays = viewFieldAngle/numRays;
		
		int distanceOfPoints = 3;
		
		// Check each ray
		for(int ray = 0; ray < numRays; ray++) {
			boolean obstacleFound = false;
			
			// Calculate ray direction and normalize it
			Vector2D rayDirection = new Vector2D();
			float rayRotation = (float) ((viewFieldAngle/2 - angleBetweenRays * ray) * Math.PI / 180);
			
			rayDirection.set(seekDirection.getX(), seekDirection.getY());
			rayDirection = rayDirection.rotate(rayRotation);
			rayDirection = rayDirection.normalize();
			
			// Check each point on ray
			for(int pointOnRay = 0; pointOnRay < obstacleCheckpointAmount && obstacleFound == false; pointOnRay++) {
				//System.out.println("Point: " + pointOnRay);
				// Calculate checkpoint on ray
				Point2D obstacleCheckpoint = startVector.addVector(rayDirection.multiplyVector(pointOnRay * distanceOfPoints)).convertToPoint();
				
				// Check for each obstacle if it contains point
				for(int obstacle = 0; obstacle < obstacles.length && obstacleFound == false; obstacle++) {
					if(obstacles[obstacle].contains(obstacleCheckpoint)) {
						// Store nearest point in obstacle and distance to obstacle
						distanceToObstacle[ray] = pointOnRay * distanceOfPoints;
						nearestObstaclePoints[ray] = obstacleCheckpoint;
						obstacleFound = true;
					}
					else {
						distanceToObstacle[ray] = Integer.MAX_VALUE;
					}
				}
			}
		}
		
		int closestDistance = Integer.MAX_VALUE; // Smallest distance to obstacle
		int closestRay = 0; // Closest ray to obstacle
		
		// Get closest ray to obstacle
		for(int ray = 0; ray < numRays; ray++) {
			if(distanceToObstacle[ray] < closestDistance) {
				closestDistance = distanceToObstacle[ray];
				closestRay = ray;
			}
		}
		
		// Calculate flee direction
		Vector2D fleeDirection;
		float fleeFromObstacleFactor = distanceOfPoints * obstacleCheckpointAmount;
		float seekPearlFactor = 5;
		
		if(closestDistance < fleeThreshold) {
			// Flee until further away
			fleeThreshold = distanceOfPoints * obstacleCheckpointAmount;
			
			// Flee from closest obstacle point
			Vector2D fleeVector = new Vector2D((float)(nearestObstaclePoints[closestRay].getX()), (float)(nearestObstaclePoints[closestRay].getY()));
			
			fleeDirection = startVector.subtractVector(fleeVector);
			
			// Rotate flee vector randomly if its the opposite to flee
			if(fleeDirection == seekDirection.multiplyVector(-1)) {
				int randomAngle = (int) Math.floor(Math.random() * ((5) - (-5) + 1) + (-5));;
				fleeDirection = fleeDirection.rotate((float) (randomAngle * Math.PI/180));
			}
		} else {
			// Seek until close to obstacle
			fleeThreshold = 5;
			
			// Keep seeking if there is no obstacle
			fleeDirection = seekDirection;
		}
		
		fleeDirection = fleeDirection.normalize();
		seekDirection = seekDirection.normalize();
		
		// Combine seek and flee behavior
		Vector2D directionVector = seekDirection.multiplyVector(1f).addVector(fleeDirection.multiplyVector((float) (1 - Math.min(1.0, (closestDistance / fleeThreshold)))));
		
		// Calculate direction radiant value
		float direction = (float) Math.atan2(directionVector.getY(), directionVector.getX());

		return new DivingAction(1, -direction);
	}

}
