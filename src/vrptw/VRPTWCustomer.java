package vrptw;

public class VRPTWCustomer {

	int _id;
	double _position_x;
	double _position_y;
	double _demand;
	double _ready_time;
	double _due_date;
	double _service_time;
	double _arrival_time;
	
	public VRPTWCustomer(int id, double position_x, double position_y, double demand, double ready_time, double due_date, double service_time) {
		if (id < 1)
			throw new IllegalArgumentException("id wrong");
		if (position_x < 0)
			throw new IllegalArgumentException("position_x negative");
		if (position_y < 0)
			throw new IllegalArgumentException("position_y negative");
		
		_id = id;
		_position_x = position_x;
		_position_y = position_y;
		_demand = demand;
		_ready_time = ready_time;
		_due_date = due_date;
		_arrival_time = 0;
		_service_time = service_time;
	}
	
	public int getID() {
		return _id;
	}
	
	public boolean isWarehouse() {
		return (_id == 1);
	}
	
	public double getXPosition() {
		return _position_x;
	}
	
	public double getYPosition() {
		return _position_y;
	}
	
	public double getStartTimeWindow() {
		return _ready_time;
	}
	
	public double getEndTimeWindow() {
		return _due_date;
	}
	
	public double getArrivalTime() {
		return _arrival_time;
	}
	
	public void setArrivalTime(double actual_arrival_time) {
		if (actual_arrival_time > _due_date) {
			System.err.println("Schedulazione non fattibile! Inserimento al tempo " + actual_arrival_time + " fallito per " + this);
			System.exit(1);
		}
		_arrival_time = actual_arrival_time;
	}
	
	public double getActualStart() {
		if (_arrival_time == 0)
			return 0;
		return Math.max(_ready_time, _arrival_time);
	}
	
	public double getWaiting() {
		if (_arrival_time == 0)
			return 0;
		return Math.max(0, _ready_time - _arrival_time);
	}
	
	public double getServiceTime() {
		return _service_time;
	}
	
	public double getCompletedTime() {
		return getActualStart()+_service_time;
	}
	
	public double minimumDistanceToRoute(VRPTWRoute route) {
		
		VRPTWCustomer nearest_customer = route.customers.get(0);
		double nearest_distance = VRPTWUtils.distance(this.getXPosition(), this.getYPosition(), nearest_customer.getXPosition(), nearest_customer.getYPosition());
		for (VRPTWCustomer c : route.customers) {
			double distance = VRPTWUtils.distance(this.getXPosition(), this.getYPosition(), c.getXPosition(), c.getYPosition());
			if (distance < nearest_distance) {
				nearest_distance = distance;
				nearest_customer = c;
			}
		}
		
		return nearest_distance;
		
	}
	
	public VRPTWCustomer clone() {
		VRPTWCustomer clone = new VRPTWCustomer(_id, _position_x, _position_y, _demand, _ready_time, _due_date, _service_time);
		return clone;
	}
	
	public void show() {
		System.out.println(_id + "\t" + _position_x + "\t" + _position_y + "\t" + _demand + "\t"+ _ready_time + "\t" + _due_date + "\t" + _service_time + "\t" + _arrival_time);
	}
	
	public String toString() {
		return "Customer " + _id + " position=(" + _position_x + "," + _position_y + ")  demand=" + _demand + "  time window=["+ _ready_time + " - " + _due_date + "]  service time=" + _service_time + " actual arrival time=" + _arrival_time + "  waiting=" + getWaiting();
	}
	
}
