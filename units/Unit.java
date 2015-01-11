package team158.units;
import team158.Robot;
import team158.units.com.Navigation;
import team158.utils.Broadcast;
import team158.utils.DirectionHelper;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;

public abstract class Unit extends Robot {
	// navigation information
	public boolean isAvoidingObstacle = false; // whether in state of avoiding obstacle
	public boolean isAvoidAllAttack = false;
	
	public MapLocation destination; // desired point to reach
	public Direction origDirection = null; // original direction of collision of robot into obstacle
	
	public MapLocation monitoredObstacle; // obstacle tile to move relative to
	public MapLocation lastLocation = null;
	public int timeSinceLastMove = 0;
	
	// grouping information
	protected int groupID = -1;
	/**
	 * groupID:
	 * -1 = ungrouped
	 * 0 = retreating back
	 * >0 = grouped  
	 */
	
	private double prevHealth = 0;
	
	public void move() {
		try {
			// Transfer supply stage
			int mySupply = (int) rc.getSupplyLevel();
			RobotInfo[] friendlyRobots = rc.senseNearbyRobots(15, rc.getTeam());
			if (friendlyRobots.length > 0) {
				// If predicted to die on this turn
				if (rc.getHealth() <= prevHealth / 2) {
					
					RobotInfo bestFriend = null;
					double maxHealth = 0;
					for (RobotInfo r : friendlyRobots) {
						if (r.health > maxHealth && r.type != RobotType.HQ) {
							maxHealth = r.health;
							bestFriend = r;
						}
					}
					if (maxHealth > 8) {
						rc.transferSupplies(mySupply, bestFriend.location);
					}
				}
				else if (mySupply > rc.getType().supplyUpkeep * 100) {
					for (RobotInfo r : friendlyRobots) {
						if (r.supplyLevel < r.type.supplyUpkeep * 50) {
							rc.transferSupplies(Math.min(mySupply / 2, r.type.supplyUpkeep * 200), r.location);
							break;
						}
					}
				}
			}
			
			// Grouping stage
			if (groupID == -1) {
				int broadcastCh = -1;
				if (rc.getType() == RobotType.SOLDIER) {
					broadcastCh = Broadcast.groupingSoldiersCh;
				}
				else if (rc.getType() == RobotType.DRONE) {
					broadcastCh = Broadcast.groupingDronesCh;
				}
				if (broadcastCh != -1) {
					int group = rc.readBroadcast(broadcastCh);
					if (group > 0) {
						groupID = group;
					}
				}
			}
			else {
				if (rc.readBroadcast(groupID) == -1) {
					groupID = -1;
				}
			}
			// Unit-specific actions
			actions();
			prevHealth = rc.getHealth();
		}
		catch (Exception e) {
			System.out.println(rc.getType());
            e.printStackTrace();
		}
	}

	protected MapLocation selectTarget(RobotInfo[] enemies) {
		MapLocation target = null;
		double maxPriority = 0;
		for (RobotInfo r : enemies) {
			if (1 / r.health > maxPriority && r.type.attackPower > 0) {
				maxPriority = 1 / r.health;
				target = r.location;
			}
		}
		if (target != null) {
			return target;
		}
		else {
			return enemies[0].location;
		}
	}
	
	protected Direction selectMoveDirectionMicro() {
		MapLocation myLocation = rc.getLocation();
		int myRange = rc.getType().attackRadiusSquared;
		Team opponent = rc.getTeam().opponent();
		RobotInfo[] enemies = rc.senseNearbyRobots(24, opponent); // keep max sight range
		
		if (enemies.length == 0) {
			return null;
		}

		RobotInfo[] attackableEnemies = rc.senseNearbyRobots(myRange, opponent);
		// Approach enemy units in range
		if (attackableEnemies.length == 0) {
			for (RobotInfo r : enemies) {
				int distance = myLocation.distanceSquaredTo(r.location);
				if (r.type.attackRadiusSquared >= distance && myRange < distance) {
					Direction enemyDirection = myLocation.directionTo(r.location);
					if (rc.canMove(enemyDirection)) {
						return enemyDirection;
					}
				}
			}
			return null;
		}
		
		// Take less damage
		if (enemies.length < 6) { // Only do computation if it won't take too long
			int[] damages = new int[9]; // 9th slot for current position
			int[] enemyInRange = new int[8];
			
			MapLocation enemyHQ = rc.senseEnemyHQLocation();
			int initDistance = myLocation.distanceSquaredTo(enemyHQ);
			if (initDistance <= 52 && initDistance > 24) {
				int enemyTowers = rc.senseEnemyTowerLocations().length;
				int towerDamage = 0;
				if (enemyTowers == 6) {
					towerDamage = 240;
				}
				else if (enemyTowers >= 3) {
					towerDamage = 36;
				}
				else if (enemyTowers == 2) { // Must have at least 2 towers to be missed with 24 sight range
					towerDamage = 24;
				}

				if (towerDamage > 0) {
					if (initDistance <= 35) {
						damages[8] += towerDamage;
					}
					for (int i = 0; i < 8; i++) {
						if (myLocation.add(DirectionHelper.directions[i]).distanceSquaredTo(enemyHQ) <= 35) {
							damages[i] += towerDamage;
						}
					}
				}
			}
			for (RobotInfo r : enemies) {
				for (int i = 0; i < 8; i++) {
					int newLocationDistance = myLocation.add(DirectionHelper.directions[i]).distanceSquaredTo(r.location);
					if (newLocationDistance <= r.type.attackRadiusSquared) {
						damages[i] += r.type.attackPower / r.type.attackDelay;
					}
					if (newLocationDistance <= myRange) {
						enemyInRange[i] += 1;
					}
				}
				if (myLocation.distanceSquaredTo(r.location) <= r.type.attackRadiusSquared) {
					damages[8] += r.type.attackPower / r.type.attackDelay;
				}
			}
			
			int bestDirection = 8;
			int bestDamage = 999999;
			for (int i = 0; i < 8; i++) {
				if (rc.canMove(DirectionHelper.directions[i]) && damages[i] <= bestDamage && enemyInRange[i] > 0) {
					bestDirection = i;
					bestDamage = damages[i];
				}
			}
			if (bestDamage < damages[8]) {
				return DirectionHelper.directions[bestDirection];
			}
		}
		return null;
	}
	
	protected void moveToTargetByGroup(MapLocation target) {
		try {
			boolean toldToAttack = rc.readBroadcast(groupID) == 1;
			if (toldToAttack) {
				target = rc.senseEnemyHQLocation();
			}
			else {
				// TODO: more robust way of determining when rally point has been reached
				if (target.distanceSquaredTo(rc.getLocation()) <= 24) {
					rc.broadcast(groupID, -1);
					groupID = -1;
				}
			}
			// optimization. stop trying to traverse an obstacle once destination changes
			if (this.destination != null &&  (this.destination.x != target.x || this.destination.y != target.y)) { // then no longer obstacle
				this.isAvoidingObstacle = false;
			}
			this.destination = target;
			Navigation.moveToDestination(rc, this, target, false);

		} 
		catch (GameActionException e) {
			return;
		}
	}
	
}
