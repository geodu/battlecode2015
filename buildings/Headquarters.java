package team158.buildings;

import battlecode.common.*;

import java.util.*;

import team158.utils.Broadcast;
import team158.utils.DirectionHelper;

public class Headquarters extends Building {
	public HashSet<Integer> groupID = new HashSet<Integer>();
	
	protected void actions() throws GameActionException {
		
		RobotInfo[] myRobots = rc.senseNearbyRobots(999999, rc.getTeam());
		rc.setIndicatorString(1, Integer.toString(myRobots.length));
		MapLocation myLocation = rc.getLocation();
		int numSoldiers = 0;
		int numSoldiers700 = 0;
		int numBashers = 0;
		int numBeavers = 0;
		int numBarracks = 0;
		int numMiners = 0;
		int numMinerFactories = 0;
		int numSupplyDepots = 0;
		
		int minBeaverDistance = 25; // Make sure that the closest beaver is actually close
		int closestBeaver = 0;
		
		for (RobotInfo r : myRobots) {
			RobotType type = r.type;
			if (type == RobotType.SOLDIER) {
				numSoldiers++;
				if (groupID.contains(r.ID)) {
					numSoldiers700++;
				}
			} else if (type == RobotType.BEAVER) {
				numBeavers++;
				int distanceSquared = r.location.distanceSquaredTo(myLocation);
				if (distanceSquared < minBeaverDistance) {
					closestBeaver = r.ID;
					minBeaverDistance = r.location.distanceSquaredTo(myLocation);
				}
			} else if (type == RobotType.BASHER) {
				numBashers++;
			} else if (type == RobotType.BARRACKS) {
				numBarracks++;
			} else if (type == RobotType.MINER) {
				numMiners++;
			} else if (type == RobotType.MINERFACTORY) {
				numMinerFactories++;
			} else if (type == RobotType.SUPPLYDEPOT) {
				numSupplyDepots++;
			}
		}
		
		rc.broadcast(Broadcast.numBeaversCh, numBeavers);
		rc.broadcast(Broadcast.numSoldiersCh, numSoldiers);
		rc.broadcast(Broadcast.numBashersCh, numBashers);
		rc.broadcast(Broadcast.numMinersCh, numMiners);
		rc.broadcast(Broadcast.numBarracksCh, numBarracks);
		rc.broadcast(Broadcast.numMinerFactoriesCh, numMinerFactories);
		rc.broadcast(Broadcast.numSupplyDepotsCh, numSupplyDepots);
		
		if (rc.isWeaponReady()) {
			RobotInfo[] enemies = rc.senseNearbyRobots(
				rc.getType().attackRadiusSquared,
				rc.getTeam().opponent()
			);
			if (enemies.length > 0) {
				rc.attackLocation(enemies[0].location);
			}
		}

		if (rc.isCoreReady()) {
			double ore = rc.getTeamOre();
			// Spawn beavers
			if (numBeavers < 2) {
				int offsetIndex = 0;
				int[] offsets = {0,1,-1,2,-2,3,-3,4};
				int dirint = DirectionHelper.directionToInt(myLocation.directionTo(rc.senseEnemyHQLocation()));
				while (offsetIndex < 8 && !rc.canSpawn(DirectionHelper.directions[(dirint+offsets[offsetIndex]+8)%8], RobotType.BEAVER)) {
					offsetIndex++;
				}
				Direction buildDirection = null;
				if (offsetIndex < 8) {
					buildDirection = DirectionHelper.directions[(dirint+offsets[offsetIndex]+8)%8];
				}
				if (buildDirection != null && ore >= 100) {
					rc.spawn(buildDirection, RobotType.BEAVER);
				}
			}
			// Broadcast to build structures
			else if (numMinerFactories < 2) {
				if (ore >= 500) {
					rc.broadcast(Broadcast.buildMinerFactoriesCh, closestBeaver);
				}
			}
			else if (ore >= 300 * numBarracks) {
				rc.broadcast(Broadcast.buildBarracksCh, closestBeaver);
				// tell closest beaver to build barracks
			}
			else if (numSupplyDepots < 3 && ore >= 500) {
				rc.broadcast(Broadcast.buildSupplyCh, closestBeaver);
			}

			
			if (numSoldiers700 < 5 && numSoldiers > 30) {
				groupUnits(Broadcast.soldierGroupCh, RobotType.SOLDIER);
				rc.broadcast(Broadcast.soldierGroupCh, 1);
			}
			else {
				stopGroup(RobotType.SOLDIER);
			}
			
//			if (numBashers701 > 50) {
//				System.out.println(numBashers701);
//				rc.broadcast(701, 1);
//			} else if (numBashers701 <= 50) {
//				//System.out.println(numBashers701);
//				rc.broadcast(701, 0);
//				groupUnits(701, RobotType.BASHER);
//			}
		}
	}
	
	public void groupUnits(int ID_Broadcast, RobotType rt) {
		RobotInfo[] myRobots = rc.senseNearbyRobots(999999, rc.getTeam());
		for (RobotInfo r : myRobots) {
			RobotType type = r.type;
			if (type == RobotType.SOLDIER) {
				//System.out.println(r.ID);
				groupID.add(r.ID);
			}
//			if (type == RobotType.BASHER) {
//				//System.out.println(r.ID);
//				groupID.add(r.ID);
//			}
		}
		int broadcastCh;
		if (rt == RobotType.SOLDIER) {
			broadcastCh = Broadcast.groupingSoldiersCh;
		}
		else if (rt == RobotType.DRONE) {
			broadcastCh = Broadcast.groupingDronesCh;
		}
		else if (rt == RobotType.BASHER) {
			broadcastCh = Broadcast.groupingBashersCh;
		}
		else {
			broadcastCh = 9999;
		}
		try {
			rc.broadcast(broadcastCh, ID_Broadcast);
		}
		catch (GameActionException e) {
			return;
		}
	}
	
	public void stopGroup(RobotType rt) {
		int broadcastCh;
		if (rt == RobotType.SOLDIER) {
			broadcastCh = Broadcast.groupingSoldiersCh;
		}
		else if (rt == RobotType.DRONE) {
			broadcastCh = Broadcast.groupingDronesCh;
		}
		else if (rt == RobotType.BASHER) {
			broadcastCh = Broadcast.groupingBashersCh;
		}
		else {
			broadcastCh = 9999;
		}
		try {
			rc.broadcast(broadcastCh, 0);
		}
		catch (GameActionException e) {
			return;
		}
	}
}