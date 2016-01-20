package vrptw;

public class VRPTWUtils {
	public static double distance(double a_x, double a_y, double b_x, double b_y) {
		return Math.sqrt((a_x-b_x)*(a_x-b_x) + (a_y-b_y)*(a_y-b_y));
	}
	public static double distance(VRPTWCustomer a, VRPTWCustomer b) {
		return distance(a.getXPosition(), a.getYPosition(), b.getXPosition(), b.getYPosition());
	}
}
