package vrptw;

import java.util.Comparator;

public class VRPTWRouteCostComparator implements Comparator<VRPTWRoute> {

	@Override
	public int compare(VRPTWRoute arg0, VRPTWRoute arg1) {
		double cost_difference = arg0.travelDistance() - arg1.travelDistance();
		return new Long(Math.round(cost_difference)).intValue();
	}

}
