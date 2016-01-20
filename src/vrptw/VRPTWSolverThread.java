package vrptw;

import java.util.Collections;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.Random;


public class VRPTWSolverThread implements Runnable {
	
	static boolean debug = false;
	
	int _id;
	boolean go;
	VRPTWSolution _best_local_solution;
	VRPTWSolution _previous_local_solution;
	VRPTWSolution _coworker_solution;
	VRPTWSolution _old_solution;
	VRPTWProblem _problem;
	
	LinkedList<VRPTWSolution> _solutions;
	CyclicBarrier _start_barrier;
	CyclicBarrier _done_barrier;
	CyclicBarrier _cooperate;
	
	VRPTWSolverThread _coworker_prev, _coworker_next;
	
	double initial_temperature;
	int customers;
	
	public VRPTWSolverThread(int id, VRPTWProblem problem, VRPTWSolution solution, LinkedList<VRPTWSolution> solutions, CyclicBarrier start, CyclicBarrier done, CyclicBarrier cooperate) {
		_id = id;
		_old_solution = solution;
		_best_local_solution = solution;
		_problem = problem;
		_solutions = solutions;
		
		_start_barrier = start;
		_done_barrier = done;
		_cooperate = cooperate;
		
		initial_temperature = VRPTWParameters.gamma * _old_solution.cost();
		customers = problem.getNumberOfCustomers();
		
		_coworker_solution = null;
		go = true;
	}
	
	public void activateDebugMode() {
		debug = true;
	}
	
	public void setCoWorkerPrev(VRPTWSolverThread socio) {
		_coworker_prev = socio;
	}
	
	public void setCoWorkerNext(VRPTWSolverThread socio) {
		_coworker_next = socio;
	}
	
	public void setCoWorkerSolution(VRPTWSolution another_solution) {
		if (another_solution == null)
			throw new IllegalArgumentException("ricevo una soluzione null dal collega!");
		_coworker_solution = another_solution;
	}
	



	
	public void stop() {
		go = false;
	}
	
	public int getID() {
		return _id;
	}

	@Override
	public void run() {
		
		double temperature = initial_temperature;
		
		if (debug) System.out.println("SolverThread "+_id+" avviato!");
		
		
		try {
			_start_barrier.await();
		} catch (BrokenBarrierException e) {
			go = false;
		} catch (InterruptedException e) {
			go = false;
		}
		
		while (go) {
			
			
			
			for (int iteration=0; iteration < customers*customers; iteration++ ) {

				_previous_local_solution = _best_local_solution;
				_best_local_solution = annealing_step(_previous_local_solution);

				
				double cost_new = _best_local_solution.cost();
				double cost_old = _previous_local_solution.cost();
				if (cost_new > cost_old) {
					if (debug) System.out.print("thread: soluzione peggiore di quella di partenza: costo " + Math.round(_best_local_solution.cost()) + " con " + _best_local_solution.routes.size() + " mezzi");
					if (Math.random() < (temperature/(temperature + initial_temperature*VRPTWParameters.delta))) {
						if (debug) System.out.println(" accettata comunque (T=" + Math.round(temperature) + ")");
					} else {
						
						_best_local_solution = _previous_local_solution;
						if (debug) System.out.println(" rifiutata");
					}
				} else
					if (debug) System.out.println("thread: soluzione migliore di quella di partenza: costo " + Math.round(_best_local_solution.cost()) + " con " + _best_local_solution.routes.size() + " mezzi");
				
				
				if ( ( (_coworker_next != null) || (_coworker_prev != null) ) &&  iteration % customers == customers-1) {
					if (_id == 0) {
						
						try {
							_cooperate.await();
							_cooperate.reset();
						} catch (BrokenBarrierException e) {
							go = false;
						} catch (InterruptedException e) {
							go = false;
						}
						
						
						if (debug) System.out.println("thread-"+_id+" consegno al collega "+_coworker_next.getID()+" la mia soluzione");
						synchronized(_coworker_next) {
							_coworker_next.setCoWorkerSolution(_best_local_solution);
							_coworker_next.notify();
						}
					} else {
						
						try {
							_cooperate.await();
						} catch (BrokenBarrierException e) {
							go = false;
						} catch (InterruptedException e) {
							go = false;
						}
						
						
						synchronized(this) {
							while (_coworker_solution == null) {
								try {
									if (debug) System.out.println("thread-"+_id+" attendo il collega "+_coworker_prev.getID());
									this.wait();
								} catch (InterruptedException e) {
									go = false;
									break;
								}
							}
						}
						
						
						
						if (debug) System.out.println("thread-"+_id+" controllo solutione arrivata dal collega "+_coworker_prev.getID());
						if (_coworker_solution.cost() < _best_local_solution.cost()) {
							_best_local_solution = _coworker_solution.clone();	
						}
						
						if (_coworker_next != null) {
							if (debug) System.out.println("thread-"+_id+" consegno al collega "+_coworker_next.getID()+" la soluzione migliore");
							synchronized(_coworker_next) {
								_coworker_next.setCoWorkerSolution(_best_local_solution);
								_coworker_next.notify();
							}
						}
					}
					_coworker_solution = null;
				}
			}
			
			
			if (debug) System.out.println("thread-"+_id+" consegna soluzione best_local al thread supervisore");
			synchronized(_solutions) {
				_solutions.add(_best_local_solution);
			}
			
			try {
				
				_done_barrier.await();
				
				
				if (debug) System.out.println("thread-"+_id+" in attesa di iniziare di nuovo");
				_start_barrier.await();
			} catch (BrokenBarrierException e) {
				go = false;
			} catch (InterruptedException e) {
				go = false;
			}
			
			temperature *= VRPTWParameters.beta;
		}
		
		if (debug) System.out.println("SolverThread "+_id+" termina");
	}
	
	
	protected static VRPTWSolution annealing_step(VRPTWSolution start_solution) {
		VRPTWSolution newSolution = start_solution.clone();
	
		
		int src_route_idx = new Long(Math.round(Math.random()*(newSolution.routes.size()-1))).intValue(); 
		VRPTWRoute src_route = newSolution.routes.get(src_route_idx);
		int indexOfCustomer = new Long(Math.round(Math.random()*( src_route.customers.size()-3 ))).intValue() + 1;
		VRPTWCustomer customer = src_route.customers.get(indexOfCustomer);
		
		
		src_route.removeCustomer(customer);
		if (src_route.travelDistance() < 0.0001) { 
			newSolution.removeRoute(src_route);
			if (debug) System.out.println("Rimozione di rotta");	
		}
		
		
		int dest_route_idx = new Long(Math.round(Math.random()*(newSolution.routes.size()-1))).intValue(); 
		VRPTWRoute dest_route = newSolution.routes.get(dest_route_idx);
		
		
		LinkedList<VRPTWCandidateCustomerInsertion> candidate_insertions = dest_route.candidate_insertions(customer);
		Collections.sort(candidate_insertions);
		
		
		ListIterator<VRPTWCandidateCustomerInsertion> itr = candidate_insertions.listIterator();
		VRPTWCandidateCustomerInsertion insertion = null;
		boolean inserted = false;
		
		while (itr.hasNext() && !inserted) {
			insertion = itr.next();
			inserted = dest_route.addCustomer(insertion.customer, insertion.prev_customer_idx, insertion.next_customer_idx);
		}
		
		if (!inserted)
			newSolution = start_solution;
















		return newSolution;		
	}

	
	protected static VRPTWSolution annealing_step_orig(VRPTWSolution start_solution) {
		VRPTWSolution newSolution = start_solution.clone();

		
		int indexOfFirstRoute = new Long(Math.round(Math.random()*(newSolution.routes.size()-1))).intValue();
		VRPTWRoute r1 = newSolution.routes.get(indexOfFirstRoute);
		
		
		LinkedList<VRPTWCustomer> externalCustomers = new LinkedList<VRPTWCustomer>();
		for (VRPTWRoute r : newSolution.routes) {
			if (r != r1) {
				for (VRPTWCustomer c : r.customers)
					if (!c.isWarehouse())
						externalCustomers.add(c);
				
			}
		}
		Collections.sort(externalCustomers, new VRPTWCustomerNearestToRouteComparator(r1));
		VRPTWCustomer nearestCustomer = externalCustomers.remove();;
		
		
		for (VRPTWRoute r : newSolution.routes) {
			if (r.serve(nearestCustomer)) {
				r.removeCustomer(nearestCustomer);
				if (r.travelDistance() < 0.0001) { 
					newSolution.removeRoute(r);
					if (debug) System.out.println("Rimozione di rotta <------");
				}
				break;
			}
		}

		
		LinkedList<VRPTWCandidateCustomerInsertion> candidate_insertions = r1.candidate_insertions(nearestCustomer);
		Collections.sort(candidate_insertions);
		
		
		ListIterator<VRPTWCandidateCustomerInsertion> itr = candidate_insertions.listIterator();
		VRPTWCandidateCustomerInsertion insertion = null;
		boolean inserted = false;
		
		while (itr.hasNext() && !inserted) {
			insertion = itr.next();
			inserted = r1.addCustomer(insertion.customer, insertion.prev_customer_idx, insertion.next_customer_idx);
		}
		
		
		if (!inserted) {
			VRPTWRoute route = new VRPTWRoute(r1._warehouse, r1._initial_capacity);
			route.addCustomer(nearestCustomer);
			newSolution.addRoute(route);
			if (debug) System.out.println("Generazione di una nuova rotta <------");
		}
		
		













		
		return newSolution;		
	}
	

	
	
	protected static VRPTWSolution annealing_step_exchange(VRPTWSolution start_solution) {
		VRPTWSolution newSolution = start_solution.clone();
		
		Random rnd_gen = new Random();
	
		int r_idx1 = rnd_gen.nextInt( newSolution.routes.size() );
		int c_idx1 = rnd_gen.nextInt( (newSolution.routes.get(r_idx1).customers.size() - 2) ) + 1;
		VRPTWCustomer c1 = newSolution.routes.get(r_idx1).customers.get(c_idx1);
		
		int r_idx2 = rnd_gen.nextInt( newSolution.routes.size() );
		int c_idx2 = rnd_gen.nextInt( (newSolution.routes.get(r_idx2).customers.size() - 2) ) + 1;
		VRPTWCustomer c2 = newSolution.routes.get(r_idx2).customers.get(c_idx2);
	
		newSolution.routes.get(r_idx1).removeCustomer(c1);
		newSolution.routes.get(r_idx2).removeCustomer(c2);
		
		boolean inserted = false;
		
		if ( (r_idx1 == r_idx2) && (c_idx1<c_idx2) ) {
			inserted = newSolution.routes.get(r_idx1).addCustomer(c2, c_idx1-1, c_idx1);
			if (inserted)
				inserted = newSolution.routes.get(r_idx2).addCustomer(c1, c_idx2-1, c_idx2);
		} else {
			inserted = newSolution.routes.get(r_idx2).addCustomer(c1, c_idx2-1, c_idx2);
			if (inserted)
				inserted = newSolution.routes.get(r_idx1).addCustomer(c2, c_idx1-1, c_idx1);
		}			
		
		
		if (!inserted)
			return start_solution;
		
		













	
		return newSolution;
	}
	
}
