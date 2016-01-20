package vrptw;

import java.util.Comparator;

public class VRPTWCustomerNearestToRouteComparator implements Comparator<VRPTWCustomer> {

	VRPTWRoute _route;
	
	public VRPTWCustomerNearestToRouteComparator(VRPTWRoute route) {
		_route = route;
	}
	
	@Override
	public int compare(VRPTWCustomer arg0, VRPTWCustomer arg1) {
		
		double distance = arg0.minimumDistanceToRoute(_route)-arg1.minimumDistanceToRoute(_route);
		return new Long(Math.round(distance)).intValue();
	}

}
