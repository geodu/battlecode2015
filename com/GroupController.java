package team158.com;

import team158.utils.Broadcast;
import team158.utils.Hashing;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class GroupController {
	
	// high-level game information
	private RobotController rc;
	private int strategy;
	
	// two group management code
	int ptA;
	int[] groupA;
	int ptB;
	int[] groupB;
	
	public GroupController(RobotController rc, int strategy) {
		this.rc = rc;
		this.strategy = strategy;
		
		this.groupA = new int[512];
		this.groupB = new int[512];
		this.ptA = 0;
		this.ptB = 0;
	}
	public void setAttackGroup(int ID_Broadcast, RobotType rt) {
		RobotInfo[] myRobots = rc.senseNearbyRobots(999999, rc.getTeam());
		for (RobotInfo r : myRobots) {
			RobotType type = r.type;
			if (type == rt) {
				//if robot is in the hashmap but not in the group
				if (Hashing.find(r.ID) == 0) {
					Hashing.put(r.ID, ID_Broadcast);
					groupA[ptA] = r.ID;
					ptA++;
				}
			}
		}
		
		int broadcastCh;
		if (rt == RobotType.DRONE) {
			broadcastCh = Broadcast.groupingDronesCh;
		}
		else if (rt == RobotType.LAUNCHER) {
			broadcastCh = Broadcast.groupingLaunchersCh;
		}
		else if (rt == RobotType.TANK) {
			broadcastCh = Broadcast.groupingTanksCh;
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
	public void groupUnits(int ID_Broadcast, RobotType rt) {
		RobotInfo[] myRobots = rc.senseNearbyRobots(999999, rc.getTeam());
		if (strategy == 1) {
			for (RobotInfo r : myRobots) {
				RobotType type = r.type;
				if (type == RobotType.TANK) {
					//update hashmap with (id, group id) pair;
					// if tank is in the hashmap but not in a group
					if (Hashing.find(r.ID) == 0) {
						Hashing.put(r.ID, ID_Broadcast);
						//update the corresponding broadcasted group
						if (ID_Broadcast == Broadcast.tankGroupDefenseCh) {
							groupA[ptA] = r.ID;
							ptA++;
						}
						else if (ID_Broadcast == Broadcast.tankGroupAttackCh) {
							groupB[ptB] = r.ID;
							ptB++;
						}
					}
				} 
			}
		} 
		else if (strategy == 2) {
			for (RobotInfo r : myRobots) {
				RobotType type = r.type;
				if (type == RobotType.DRONE) {
					//update hashmap with (id, group id) pair;
					// if tank is in the hashmap but not in a group
					if (Hashing.find(r.ID) == 0) {
						Hashing.put(r.ID, ID_Broadcast);
						//update the corresponding broadcasted group
						if (ID_Broadcast == Broadcast.droneGroup1Ch) {
							groupA[ptA] = r.ID;
							ptA++;
						}
						else if (ID_Broadcast == Broadcast.droneGroup2Ch) {
							groupB[ptB] = r.ID;
							ptB++;
						}
					}
				}
			}
		}
		
		int broadcastCh;
		if (rt == RobotType.DRONE) {
			broadcastCh = Broadcast.groupingDronesCh;
		}
		else if (rt == RobotType.LAUNCHER) {
			broadcastCh = Broadcast.groupingLaunchersCh;
		}
		else if (rt == RobotType.TANK) {
			broadcastCh = Broadcast.groupingTanksCh;
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
	
	public void unGroup(int ID_Broadcast) {
		try {
			rc.broadcast(ID_Broadcast, -1);
			if (ID_Broadcast == Broadcast.droneGroup1Ch) {
				int i = 0;
				while (groupA[i] != 0) {
					Hashing.put(groupA[i], 0);
					groupA[i] = 0;
					i++;
				}
			}
			else if (ID_Broadcast == Broadcast.droneGroup2Ch) {
				int i = 0;
				while (groupB[i] != 0) {
					Hashing.put(groupB[i], 0);
					groupB[i] = 0;
					i++;
				}
			}
			
			if (ID_Broadcast == Broadcast.tankGroupDefenseCh) {
				int i = 0;
				while (groupA[i] != 0) {
					Hashing.put(groupA[i], 0);
					groupA[i] = 0;
					i++;
				}
			}
			else if (ID_Broadcast == Broadcast.tankGroupAttackCh) {
				int i = 0;
				while (groupB[i] != 0) {
					Hashing.put(groupB[i], 0);
					groupB[i] = 0;
					i++;
				}
			}
		}
		catch (GameActionException e) {
			return;
		}
	}
	

	
	public void stopGroup(RobotType rt) {
		int broadcastCh;
		if (rt == RobotType.TANK) {
			broadcastCh = Broadcast.groupingTanksCh;
		}
		else if (rt == RobotType.DRONE) {
			broadcastCh = Broadcast.groupingDronesCh;
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
