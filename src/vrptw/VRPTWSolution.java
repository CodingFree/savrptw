package vrptw;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.text.DecimalFormat;
import java.util.LinkedList;
import java.util.Scanner;

public class VRPTWSolution {

	String _instance_name;
	VRPTWProblem _problem;
	double distance;
	int _max_vehicles;
	LinkedList<VRPTWRoute> routes;

	public VRPTWSolution(VRPTWProblem problem) {
		if (problem == null)
			throw new IllegalArgumentException("Invalid problem instance");

		_problem = problem;
		_instance_name = problem.getInstanceName();
		routes = new LinkedList<VRPTWRoute>();
	}

	public double totalTravelDistance() {
		double _cost = 0;
		for(int r=0; r<routes.size(); r++) {
			_cost += routes.get(r).travelDistance();
		}
		return _cost;
	}

	public double cost() {
		
		double d = totalTravelDistance();
		double c = routes.size();
		double n = _problem.getNumberOfCustomers();
		int e_min = Integer.MAX_VALUE;
		for (VRPTWRoute r : routes) {
			if (r.size() < e_min) e_min = r.size();
		}
		return d + VRPTWParameters.sigma*(c*n + e_min);

	}
	
	public void show() { 
		

		double km = getDistance();
		DecimalFormat twoDForm = new DecimalFormat("#.##");
			
		System.out.print("Problem instance: "+_instance_name + ". Distance: "+twoDForm.format(km)+ ". Vehicles: "+routes.size()+".");
		if (_problem.getOptiumumDistance()>0.001) {
			double optimumDiff = (km-_problem.getOptiumumDistance())/km;
			System.out.print(" Optimus distance: "+twoDForm.format(optimumDiff*100)+"%.");
		}
		System.out.println();
	}

	public void showAll() { 
		show();
		
		for (int i=0; i<routes.size(); i++) {
			System.out.print("route "+(i+1)+": ");
			routes.get(i).show();
		}
	}
	
	public void checkBestKnownSolutionImproved() {
		if (this.getDistance() <= _problem.getCurrentBestDistance() || this.getVehicles() < _problem.getCurrentBestVehicles()){
			System.out.println("* NEW RECORD *");
			try{
				BufferedWriter bw = new BufferedWriter(new FileWriter("problems/records", true));
				bw.write("NEW RECORD");
				bw.newLine();
				bw.write("Parameters: thread="+VRPTWParameters.threads+", tau="+VRPTWParameters.tau+", sigma="+VRPTWParameters.sigma+", gamma="+VRPTWParameters.gamma+", beta="+VRPTWParameters.beta+", delta="+VRPTWParameters.delta);
				bw.newLine();
				bw.write(this.toString());
				bw.newLine();
				bw.newLine();
				bw.close();
			} catch (Exception e) {
				System.err.println("Impossibile aprire file dei record");
			}
		}
	}
	
	public void addRoute(VRPTWRoute route) {
		routes.add(route);
	}
	
	
	
	public void addRoute(String route_description) {
		VRPTWRoute newroute = new VRPTWRoute(_problem.getWarehouse(), _problem.getVehicleCapacity());
		
		Scanner scanner = new Scanner(route_description);
		scanner.useDelimiter(" ");
		
		while ( scanner.hasNext() ) {
		int customerid = Integer.parseInt(scanner.next());
			VRPTWCustomer customer = _problem.getCustomer(customerid);
			if (!customer.isWarehouse()) {
				if (customer.getArrivalTime() != 0) {
					System.err.println("Cliente già inserito");
					System.exit(1);
				}
				newroute.addCustomer(customer);
			}
	    }		
		routes.add(newroute);
	}

	public void removeRoute(VRPTWRoute route) {
		routes.remove(route);
	}
	
	public VRPTWSolution clone() {

		VRPTWSolution clone = new VRPTWSolution(_problem);
		for (VRPTWRoute route : routes) {
			VRPTWRoute newRoute = new VRPTWRoute(_problem.getWarehouse(), _problem.getVehicleCapacity());
			for (VRPTWCustomer customer : route.customers) {
				if (!customer.isWarehouse())
					newRoute.addCustomer(customer.clone());
			}
			clone.addRoute(newRoute);
		}

		return clone;
	}
	
	
	public int customers_size() {
		int tot_cust = 0;
		for (VRPTWRoute r : routes) {
			if (r.customers.size() > 2)
				tot_cust += r.customers.size() - 2;
		}
		return tot_cust;
	}
	
	public double getDistance() {
		double km = 0;
		for (VRPTWRoute r : routes)
			km += r._travel_distance;
		return km;
	}
	
	public int getVehicles(){
		return routes.size();
	}
	
	public String toString() { 
		
		
		
		
		double km = getDistance();
	
		String result = "Problem instance: "+_instance_name + ". Distance: "+km+ ". Vehicles: "+routes.size()+"."+'\n';

		for (int i=0; i<routes.size(); i++) {
			result += "route "+(i+1)+": "+ routes.get(i)+'\n';
		}
		return result;
	}

}