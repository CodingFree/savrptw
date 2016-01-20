package vrptw;

import java.util.Comparator;

public class VRPTWCustomerEndTimeWindowComparator implements Comparator<VRPTWCustomer> {

	@Override
	public int compare(VRPTWCustomer arg0, VRPTWCustomer arg1) {
		double close_time_window = arg0.getEndTimeWindow() - arg1.getEndTimeWindow();
		return new Long(Math.round(close_time_window)).intValue();
	}

}