package vrptw;

import java.util.LinkedList;
import java.util.ListIterator;

public class VRPTWRoute {

	LinkedList<VRPTWCustomer> customers;
	VRPTWCustomer _warehouse;
	double _initial_capacity;
	double _capacity;
	double _travel_distance;
	
	public VRPTWRoute(VRPTWCustomer warehouse, double initial_capacity) {
		customers = new LinkedList<VRPTWCustomer>();
		_warehouse = warehouse;
		_capacity = _initial_capacity = initial_capacity;
		_travel_distance = 0;
		
		customers.add(warehouse.clone());	
		customers.add(warehouse.clone());	
	}
	
	public boolean addCustomer(VRPTWCustomer customer, int prev_customer_idx, int next_customer_idx) {
		if (next_customer_idx - prev_customer_idx != 1)
			return false; 
		
		if (_capacity < customer._demand)
			return false;
		
		VRPTWCustomer prev_customer = customers.get(prev_customer_idx);
		VRPTWCustomer next_customer = customers.get(next_customer_idx);
		
		double distance_prev_next = VRPTWUtils.distance(prev_customer, next_customer);
		double distance_prev_c = VRPTWUtils.distance(prev_customer, customer);
		double distance_c_next = VRPTWUtils.distance(customer, next_customer);		
		
		
		double customer_arrival = prev_customer.getCompletedTime() + distance_prev_c;
		double customer_start = Math.max(customer.getStartTimeWindow(), customer_arrival);
		
		double next_customer_new_arrival = customer_start + customer.getServiceTime() + distance_c_next;
		
		double push_forward_time = next_customer_new_arrival - next_customer.getArrivalTime();

		if ( (customer_arrival <= customer.getEndTimeWindow()) && push_forward_keep_feasibility(next_customer_idx, push_forward_time)) {
			

			customer.setArrivalTime( customer_arrival );
			customers.add(next_customer_idx, customer);

			
			int i = next_customer_idx+1;
			while ( (i<customers.size()) && push_forward_time > 0) {
				double new_arrival = customers.get(i).getArrivalTime() + push_forward_time;
				double old_start_time = customers.get(i).getActualStart();
				customers.get(i).setArrivalTime( new_arrival );
				
				
				push_forward_time = customers.get(i).getActualStart() - old_start_time;
				i++;
			}

			
			_travel_distance -= distance_prev_next;
			
			_travel_distance += distance_prev_c + distance_c_next;
			
			
			_capacity -= customer._demand;
			return true;
		}
		return false;
	}
	

	public boolean addCustomer(VRPTWCustomer customer) {
		
		return addCustomer(customer, customers.size()-2, customers.size()-1);
	}
	
	
	public void removeCustomer(VRPTWCustomer customer) {

		
		int customer_idx = customers.indexOf(customer);
		if (customer_idx == -1)
			return;
		
		VRPTWCustomer prev_customer = customers.get(customer_idx-1);
		VRPTWCustomer next_customer = customers.get(customer_idx+1);
		
		double distance_prev_next = VRPTWUtils.distance(prev_customer, next_customer);
		double distance_prev_c = VRPTWUtils.distance(prev_customer, customer);
		double distance_c_next = VRPTWUtils.distance(customer, next_customer);		
		
		
		double next_customer_arrival = prev_customer.getCompletedTime() + distance_prev_next;
		double push_backward_time = next_customer.getArrivalTime() - next_customer_arrival;

		
		int i = customer_idx+1;
		while ( (i<customers.size()) && push_backward_time > 0) {
			double old_actual_start = customers.get(i).getActualStart();
			double new_actual_arrival = customers.get(i).getArrivalTime() - push_backward_time;
			customers.get(i).setArrivalTime( new_actual_arrival );
			
			
			push_backward_time = old_actual_start - customers.get(i).getActualStart();
			i++;
		}
		
		
		_travel_distance += distance_prev_next - distance_prev_c - distance_c_next;
		_capacity += customer._demand;

		
		customers.remove(customer);
	}
		

	
	
	public boolean push_forward_keep_feasibility(int customer_idx, double push_forward_time) {
		if (push_forward_time < 0)
			return true;	
		
		int i = customer_idx;
		while ( (i<customers.size()) && (customers.get(i).getArrivalTime() + push_forward_time < customers.get(i).getEndTimeWindow()) ) {
			
			push_forward_time -= customers.get(i).getWaiting();
			if (push_forward_time <= 0)
				
				return true;	
			i++;
		}
		if ( i==customers.size() )
			return true;

		return false;
	}

	
	public LinkedList<VRPTWCustomer> candidate_customers(LinkedList<VRPTWCustomer> unallocated_pool) {
		
		
		
		
		
		
		
		
		
		LinkedList<VRPTWCustomer> out = new LinkedList<VRPTWCustomer>();
		ListIterator<VRPTWCustomer> itr = null;
		
		
		double min_cost = Double.MAX_VALUE;
		
		VRPTWCustomer smaller_start_time_customer = null;
		itr = unallocated_pool.listIterator();
		while(itr.hasNext()) {
			VRPTWCustomer c = itr.next();
			if (c.getStartTimeWindow() < min_cost) {
				smaller_start_time_customer = c;
				min_cost = c.getStartTimeWindow();
			}
		}
		if (smaller_start_time_customer != null)
			out.add(smaller_start_time_customer);
		

		
		
		min_cost = Double.MAX_VALUE;
		
		VRPTWCustomer tight_window_customer = null;
		itr = unallocated_pool.listIterator();
		while(itr.hasNext()) {
			VRPTWCustomer c = itr.next();
			double cost = 1000 * (c.getEndTimeWindow() - c.getStartTimeWindow()) - VRPTWUtils.distance(c, _warehouse);
			
			if ( (cost < min_cost) && (c != smaller_start_time_customer) ) {
				tight_window_customer = c;
				min_cost = cost;
			}
		}
		if (tight_window_customer != null)
			out.add(tight_window_customer);
		
		
		
		double max_cost = Double.MIN_VALUE;
		
		VRPTWCustomer farthest_customer = null;
		itr = unallocated_pool.listIterator();
		while(itr.hasNext()) {
			VRPTWCustomer c = itr.next();
			double cost = VRPTWUtils.distance(c, _warehouse);
			
			if ( (cost > max_cost)  && (c != smaller_start_time_customer) && (c != tight_window_customer) ) {
				farthest_customer = c;
				max_cost = cost;
			}
		}
		if (farthest_customer != null)
			out.add(farthest_customer);
		
		return out;
	}
	
	
	
	public LinkedList<VRPTWCandidateCustomerInsertion> candidate_insertions(VRPTWCustomer customer) {
		LinkedList<VRPTWCandidateCustomerInsertion> out = new LinkedList<VRPTWCandidateCustomerInsertion>();
		
		if (_capacity < customer._demand)
			return out;	
		
		VRPTWCustomer prev_customer = customers.get(0);
		double distance_c_next, distance_prev_next, distance_prev_c = VRPTWUtils.distance(prev_customer, customer);
		
		
		
		int i = 1;
		while (i < customers.size()) {
			VRPTWCustomer next_customer = customers.get(i);
				
			distance_prev_next = VRPTWUtils.distance(prev_customer, next_customer);
			distance_c_next = VRPTWUtils.distance(customer, next_customer);		
			
			
			double customer_arrival = Math.max(customer.getStartTimeWindow(), prev_customer.getCompletedTime() + distance_prev_c);
			
			double next_customer_new_arrival = Math.max(next_customer.getArrivalTime(), customer_arrival + customer.getServiceTime() + distance_c_next);
			double next_customer_new_arrival_ = customer_arrival + customer.getServiceTime() + distance_c_next;

			
			double push_forward_time = Math.max(0, next_customer_new_arrival - next_customer.getArrivalTime());
			
			
			double distance_increase = distance_prev_c + distance_c_next - distance_prev_next;
			out.add( new VRPTWCandidateCustomerInsertion(customer, i-1, i, distance_increase) );
			
			
			double local_sched_time_increase = push_forward_time - next_customer.getWaiting();
			out.add( new VRPTWCandidateCustomerInsertion(customer, i-1, i, local_sched_time_increase) );
			
			prev_customer = next_customer;
			distance_prev_c = distance_c_next;
			i++;
		}
		return out;		
	}

	
	public double travelDistance() {
		  return _travel_distance;
	}
	
	
	public double travelTime() {
		if (customers.size() == 0)
			return 0;
		return customers.get(customers.size()-1).getCompletedTime();
	}
	
	
	public double getRemainCapacity() {
		return _capacity;
	}
	
	
	public int size() {
		return customers.size();
	}
	
	
	public void show() {
		
		for (int c = 0; c<customers.size()-1; c++) {
			System.out.print(customers.get(c).getID() + " ");
		}
		System.out.println(customers.get(customers.size()-1).getID() + ";");
	}
	
	
	public String toString() {
		String description = "";
		
		for (int c = 0; c<customers.size()-1; c++) {
			description += customers.get(c).getID() + " ";
		}
		description += customers.get(customers.size()-1).getID() + ";";
		
		return description;
	}
	
	
	
	
	public boolean check_compactness() {
		double difference = 1;
		ListIterator<VRPTWCustomer> itr = customers.listIterator();
		VRPTWCustomer prev_customer = itr.next();
		while(itr.hasNext()) {
			VRPTWCustomer c = itr.next();
			double distance = VRPTWUtils.distance(prev_customer, c);
			difference = Math.abs(prev_customer.getCompletedTime()+distance - c.getArrivalTime());
			if (difference > 0.001)
				return false;

			prev_customer = c;
		}
		return true;
	}

	
	public boolean serve(VRPTWCustomer customer) {
		return customers.contains(customer);
	}

}
