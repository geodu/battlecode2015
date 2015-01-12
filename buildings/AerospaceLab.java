package team158.buildings;

import team158.utils.DirectionHelper;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class AerospaceLab extends Building {

	public AerospaceLab(RobotController newRC) {
		super(newRC);
	}

	@Override
	protected void actions() throws GameActionException {
		if (rc.isCoreReady() && rc.getTeamOre() >= 400) {
			int[] offsets = {0,1,-1,2,-2,3,-3,4};
			int dirint = DirectionHelper.directionToInt(myLocation.directionTo(enemyHQ));
			int offsetIndex = 0;
			while (offsetIndex < 8 && !rc.canSpawn(DirectionHelper.directions[(dirint+offsets[offsetIndex]+8)%8], RobotType.LAUNCHER)) {
				offsetIndex++;
			}
			if (offsetIndex < 8) {
				rc.spawn(DirectionHelper.directions[(dirint+offsets[offsetIndex]+8)%8], RobotType.LAUNCHER);
			}
		}
	}

}
