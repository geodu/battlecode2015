package team158.buildings;

import team158.utils.Broadcast;
import team158.utils.DirectionHelper;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;

public class Barracks extends Building {

	@Override
	protected void actions() throws GameActionException {
        // get information broadcasted by the HQ
		MapLocation myLocation = rc.getLocation();
		MapLocation enemyHQ = rc.senseEnemyHQLocation();
		int[] offsets = {0,1,-1,2,-2,3,-3,4};
		int dirint = DirectionHelper.directionToInt(myLocation.directionTo(enemyHQ));
		if (rc.readBroadcast(Broadcast.tankRallyXCh) == 0) {
			MapLocation rally = myLocation;
			// Move 5 squares away
			int rallyDistance = (int)hqDistance / 6;
			for (int i = 0; i < rallyDistance; i++) {
				int offsetIndex = 0;
				while (offsetIndex < 8) {
					MapLocation candidate = rally.add(DirectionHelper.directions[(dirint+offsets[offsetIndex]+8)%8]);
					if (rc.senseTerrainTile(candidate).isTraversable()) {
						rally = candidate;
						break;
					}
					offsetIndex++;
				}
			}
			rc.broadcast(Broadcast.tankRallyXCh, rally.x);
			rc.broadcast(Broadcast.tankRallyYCh, rally.y);
		}
	}
}
