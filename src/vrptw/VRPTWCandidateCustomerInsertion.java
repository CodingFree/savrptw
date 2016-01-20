package vrptw;











public class VRPTWCandidateCustomerInsertion implements Comparable<VRPTWCandidateCustomerInsertion> {
	VRPTWCustomer customer;
	int prev_customer_idx;
	int next_customer_idx;
	double increase;
	
	public VRPTWCandidateCustomerInsertion(VRPTWCustomer customer, int prev_customer_idx, int next_customer_idx, double increase) {
		this.customer = customer;
		this.prev_customer_idx = prev_customer_idx;
		this.next_customer_idx = next_customer_idx;
		this.increase = increase;		
	}
	
	@Override
	public int compareTo(VRPTWCandidateCustomerInsertion other) {
		double diff = this.increase - other.increase;
		return new Long(Math.round(diff)).intValue();
	}
	
	public String toString() {
		return "Customer " + customer._id + " candidate insertion between " + prev_customer_idx + " and " + next_customer_idx + " with cost increase of " + increase;
	}
}
