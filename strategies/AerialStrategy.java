package team158.strategies;

import team158.buildings.Headquarters;
import team158.utils.Broadcast;
import team158.utils.DirectionHelper;
import team158.utils.GroupController;
import team158.utils.Hashing;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class AerialStrategy extends GameStrategy {
	
	private boolean enemyRush;
	private boolean enemyThreat;
	private int pathDifficulty;
	private int closestBeaver;
	private int scoutBeaver;
	
	public AerialStrategy(RobotController rc, GroupController gc, Headquarters hq) {
		super(rc, gc, hq);
		
		this.enemyRush = false;
		this.enemyThreat = false;
		this.pathDifficulty = 0;	
		this.closestBeaver = 0;
		this.scoutBeaver = 0;
	}
	
	public void executeStrategy() throws GameActionException {
		RobotInfo[] myRobots = rc.senseNearbyRobots(999999, rc.getTeam());
		int numBeavers = 0;
		int numMiners = 0;
		int numMinerFactories = 0;
		int numSupplyDepots = 0;
		int numDrones = 0;
		int numLaunchers = 0;
		int numHelipads = 0;
		int numDronesG1 = 0;
		int numDronesG2 = 0;
		int numAerospaceLabs = 0;
		
		for (RobotInfo r : myRobots) {
			RobotType type = r.type;
			if (type == RobotType.MINER) {
				numMiners++;
			} else if (type == RobotType.DRONE) {
				numDrones++;
				if (Hashing.find(r.ID) == Broadcast.droneGroup1Ch) {
					numDronesG1++;
				}							
				else if (Hashing.find(r.ID)  == Broadcast.droneGroup2Ch) {
					numDronesG2++;
				}		
			} else if (type == RobotType.BEAVER) {
				numBeavers++;
				if (scoutBeaver == 0 && closestBeaver != 0 && r.ID != closestBeaver) {
					scoutBeaver = r.ID;
					rc.broadcast(Broadcast.scoutEnemyHQCh, scoutBeaver);
				}
				else if (r.ID != scoutBeaver) {
					closestBeaver = r.ID;
				}
			} else if (type == RobotType.MINERFACTORY) {
				numMinerFactories++;
			} else if (type == RobotType.SUPPLYDEPOT) {
				numSupplyDepots++;
			} else if (type == RobotType.HELIPAD) {
				numHelipads++;
			} else if (type == RobotType.AEROSPACELAB) {
				numAerospaceLabs++;
			} else if (type == RobotType.LAUNCHER) {
				numLaunchers++;
			} else if (type == RobotType.DRONE) {
				numDrones++;
			}
		}
		
		rc.broadcast(Broadcast.numMinersCh, numMiners);
		rc.broadcast(Broadcast.numDronesCh, numDrones);
		rc.broadcast(Broadcast.numLaunchersCh, numLaunchers);
		rc.broadcast(Broadcast.numBuildingsCh, numMinerFactories + numSupplyDepots + numHelipads + numAerospaceLabs);
		
		if (!enemyRush && Clock.getRoundNum() < 600) {
			RobotInfo[] enemyRobots = rc.senseNearbyRobots(99, rc.getTeam().opponent());
			for (RobotInfo r : enemyRobots) {
				if (r.type == RobotType.DRONE) {
					enemyRush = true;
					// Cancel requested builds
					rc.broadcast(Broadcast.buildHelipadsCh, 0);
					// Tell helipads to yield
					rc.broadcast(Broadcast.yieldToLaunchers, 1);
					break;
				}
			}
		}
		
		if (!enemyThreat && Clock.getRoundNum() < 1200) {
			if (rc.readBroadcast(Broadcast.enemyThreatCh) > 2) {
				enemyThreat = true;
				rc.broadcast(Broadcast.buildHelipadsCh, 0);
				rc.broadcast(Broadcast.yieldToLaunchers, 1);
				gc.unGroup(Broadcast.droneGroup1Ch);
			}
		}
		
		if (pathDifficulty == 0) {
			int possibleDifficulty = rc.readBroadcast(Broadcast.scoutEnemyHQCh);
			if (possibleDifficulty != scoutBeaver) {
				pathDifficulty = possibleDifficulty;
			}
		}

		if (rc.isCoreReady()) {
			double ore = rc.getTeamOre();
			if (numBeavers == 0 || scoutBeaver == 0) {
				int offsetIndex = 0;
				int[] offsets = {0,1,-1,2,-2,3,-3,4};
				int dirint = DirectionHelper.directionToInt(Direction.EAST);
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
			else if (numHelipads == 0) {
				if (ore >= 300) {
					rc.broadcast(Broadcast.buildHelipadsCh, closestBeaver);
				}
			}
			else if (numMinerFactories == 0) {
				if (ore >= 500) {
					rc.broadcast(Broadcast.buildMinerFactoriesCh, closestBeaver);
				}
			}
			else if (numSupplyDepots == 0 && ore >= 100) {
				rc.broadcast(Broadcast.buildSupplyCh, closestBeaver);
			}
			else {
				if (enemyThreat && pathDifficulty < 70) { // Build launchers
					if (numAerospaceLabs == 0) {
						if (ore >= 500) {
							rc.broadcast(Broadcast.slowMinerProductionCh, 0);
							rc.broadcast(Broadcast.stopDroneProductionCh, 0);
							rc.broadcast(Broadcast.buildAerospaceLabsCh, closestBeaver);
						}
						else {
							rc.broadcast(Broadcast.slowMinerProductionCh, 1);
							rc.broadcast(Broadcast.stopDroneProductionCh, 1);
						}
					}
					else if (ore >= 400 + numAerospaceLabs * 300) {
						rc.broadcast(Broadcast.buildAerospaceLabsCh, closestBeaver);
					}
					else if (numSupplyDepots < 3 && ore >= 750) {
						rc.broadcast(Broadcast.buildSupplyCh, closestBeaver);
					}
				}
				else if (enemyRush) { // Build some launchers
					if (numAerospaceLabs == 0) {
						if (ore >= 500) {
							rc.broadcast(Broadcast.slowMinerProductionCh, 0);
							rc.broadcast(Broadcast.stopDroneProductionCh, 0);
							rc.broadcast(Broadcast.buildAerospaceLabsCh, closestBeaver);
						}
						else {
							rc.broadcast(Broadcast.slowMinerProductionCh, 1);
							rc.broadcast(Broadcast.stopDroneProductionCh, 1);
						}
					}
					else if (ore >= 500 + numHelipads * 200) {
						rc.broadcast(Broadcast.buildHelipadsCh, closestBeaver);
					}
					else if (numSupplyDepots < 3 && ore >= 750) {
						rc.broadcast(Broadcast.buildSupplyCh, closestBeaver);
					}
				}
				else {
					if (ore >= 500 + numHelipads * 200) {
						rc.broadcast(Broadcast.buildHelipadsCh, closestBeaver);
					}
					else if (numSupplyDepots < 3 && ore >= 750) {
						rc.broadcast(Broadcast.buildSupplyCh, closestBeaver);
					}
				}
			}
				
			if (numLaunchers > 0) {
				if (numLaunchers > 5) {
					rc.broadcast(Broadcast.launcherGroupCh, 1);
				}
				else {
					rc.broadcast(Broadcast.launcherGroupCh, 0);
					//groupUnits(Broadcast.launcherGroupCh, RobotType.LAUNCHER);
				}
			}
			
			//if they don't build tanks and launchers
			if (!enemyThreat) {
				MapLocation targetTower = Broadcast.readLocation(rc, Broadcast.groupTargetLocationChs);
				if (numDronesG1 < 15 || targetTower == null) {
					rc.broadcast(Broadcast.droneGroup1Ch, 1);
					gc.groupUnits(Broadcast.droneGroup1Ch, RobotType.DRONE);
				}
				else {
					if (numDronesG2 > 20) {
						rc.broadcast(Broadcast.droneGroup2Ch, 1);
						gc.groupUnits(Broadcast.droneGroup1Ch, RobotType.DRONE);
					}
					else if (numDronesG2 > 15 && rc.readBroadcast(Broadcast.droneGroup2Ch) == 1) {
						gc.groupUnits(Broadcast.droneGroup1Ch, RobotType.DRONE);
					}
					else {
						rc.broadcast(Broadcast.droneGroup2Ch, 0);
						gc.groupUnits(Broadcast.droneGroup2Ch, RobotType.DRONE);
					}
				}
			}
			//if enemy builds tanks and launchers
			else {
				MapLocation targetTower = Broadcast.readLocation(rc, Broadcast.groupTargetLocationChs);
				if (numDronesG2 > 20 && targetTower != null) {
					rc.broadcast(Broadcast.droneGroup2Ch, 1);
				}
				else if (numDronesG2 <= 15){
					rc.setIndicatorString(2, String.valueOf(numDronesG2));
					rc.broadcast(Broadcast.droneGroup2Ch, 0);
				}
				gc.groupUnits(Broadcast.droneGroup2Ch, RobotType.DRONE);
			}
		}
		
	}
}