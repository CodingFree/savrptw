package vrptw;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import vrptw.VRPTWDrawingSolution.DrawingArea;

public class VRPTWSolver {
	
	public static void main(String[] args) throws InterruptedException {
		VRPTWProblem problem = new VRPTWProblem("C101", 200);
		
		VRPTWSolver solver = new VRPTWSolver();
		
		solver.activateDrawingSolutionsMode();
		System.out.println("Starting optimization.*");
		final VRPTWSolution solution = solver.resolve(problem);
		System.out.println("Finish optimization.");
		solution.show();
		solution.checkBestKnownSolutionImproved();
	}

	private VRPTWSolverThread[] threads;
	private int _processors;
	private boolean debug = false;
	private boolean draw_solution = false;
	String folder = "frames/";
	String basename = "Solution";
	String estensione = ".png";
	int progressivo = 0;
	
	public VRPTWSolver() {
		_processors = VRPTWParameters.threads;
		threads = new VRPTWSolverThread[_processors];
	}
	
	public VRPTWSolution resolve(VRPTWProblem problem) throws InterruptedException {

		System.out.println("Execution parameters: thread="+VRPTWParameters.threads+", tau="+VRPTWParameters.tau+", sigma="+VRPTWParameters.sigma+", gamma="+VRPTWParameters.gamma+", beta="+VRPTWParameters.beta+", delta="+VRPTWParameters.delta);
		progressivo = 0;
		
		
		VRPTWSolution initialSolution = generateFirstSolution(problem);
		VRPTWSolution finalSolution = initialSolution;

		System.out.println("Soluzione di partenza: costo " + finalSolution.cost() + " (km = "+finalSolution.totalTravelDistance()+", mezzi = " + finalSolution.routes.size() + ")");
		LinkedList<VRPTWSolution> solutions = new LinkedList<VRPTWSolution>();

		
		if (draw_solution)
			printSolution(finalSolution, initialSolution , progressivo);
		progressivo++;
		
		
		threads = new VRPTWSolverThread[_processors];
		CyclicBarrier _start_barrier = new CyclicBarrier(_processors+1);
		CyclicBarrier _done_barrier = new CyclicBarrier(_processors+1);
		CyclicBarrier _cooperate_barrier = new CyclicBarrier(_processors);
	     
		for (int i=0; i<_processors; i++) {
			threads[i] = new VRPTWSolverThread(i, problem, finalSolution, solutions, _start_barrier, _done_barrier, _cooperate_barrier);
			if (debug)
				threads[i].activateDebugMode();			
			if (i>0) {
				threads[i-1].setCoWorkerNext(threads[i]);
				threads[i].setCoWorkerPrev(threads[i-1]);
			}
		}
		
		
		for (int i=0; i<_processors; i++) {
			new Thread(threads[i]).start();
		}
		
		int equilibrium = 0;
		while (equilibrium < VRPTWParameters.tau) {
			
			
			
			try {
				_start_barrier.await();
				_start_barrier.reset();
				
				
				_done_barrier.await();
				_done_barrier.reset();
			} catch (InterruptedException e) {
				break;
			} catch (BrokenBarrierException e) {
				break;
			}

			if (solutions.size() != _processors) { 
				System.err.println("QUALCUNO HA MANCATO LA CONSEGNA!");
				System.exit(1);
			}
			
			
			
			VRPTWSolution bestSolution = solutions.remove();
			while (solutions.size() > 0) {
				VRPTWSolution s = solutions.remove();
				if (s.cost()<bestSolution.cost()) {
					bestSolution = s;
				}
				
			}

			
			if (bestSolution.cost() < finalSolution.cost()) {
				finalSolution = bestSolution;
				equilibrium = 0;
				System.out.println("Trovata soluzione migliore ... costo " + finalSolution.cost() + " (km = "+finalSolution.totalTravelDistance()+", mezzi = " + finalSolution.routes.size() + ")");
			} else {
				System.out.println("Nessun miglioramento (" + equilibrium + ")");
				equilibrium ++;
			}
			
			
			if (draw_solution)
				printSolution(finalSolution, initialSolution , progressivo);
			progressivo++;
			
		}

		
		for (int i=0; i<_processors; i++) {
			threads[i].stop();
		}
		try {
			_start_barrier.await();
		} catch (BrokenBarrierException e) { }
		
		System.out.println("Terminato in "+progressivo+" iterazioni.");
		
		return finalSolution;
	}
	
	
	
	
	protected VRPTWSolution generateFirstSolution(VRPTWProblem problem)  {
		
		VRPTWSolution solution = new VRPTWSolution(problem);
		VRPTWCustomer warehouse = problem.getWarehouse();
	
		LinkedList<VRPTWCustomer> customerToServe = new LinkedList<VRPTWCustomer>();	
		for (VRPTWCustomer c : problem.customers) {
			if (!c.isWarehouse())
				customerToServe.add(c);
		}
		
		
		VRPTWRoute route = new VRPTWRoute(warehouse, problem.getVehicleCapacity());
		while (!customerToServe.isEmpty()) {
			
			LinkedList<VRPTWCustomer> candidate_customers = route.candidate_customers(customerToServe);
			LinkedList<VRPTWCandidateCustomerInsertion> candidate_insertions = new LinkedList<VRPTWCandidateCustomerInsertion>();
			
			
			for (VRPTWCustomer c : candidate_customers) {
				
				candidate_insertions.addAll( route.candidate_insertions(c) );
			}
			Collections.sort(candidate_insertions);
			
			
			ListIterator<VRPTWCandidateCustomerInsertion> itr = candidate_insertions.listIterator();
			VRPTWCandidateCustomerInsertion insertion = null;
			boolean inserted = false;
			
			while (itr.hasNext() && !inserted) {
				insertion = itr.next();
				inserted = route.addCustomer(insertion.customer, insertion.prev_customer_idx, insertion.next_customer_idx);
			}
			
			if (inserted) {
				customerToServe.remove(insertion.customer);
				if (debug) System.out.println("Cliente inserito: " + insertion.customer);
			}
			
			
			if ( !inserted || (route._capacity == 0)) {
				solution.addRoute(route);
				route = new VRPTWRoute(warehouse, problem.getVehicleCapacity());
				if (debug) System.out.println("Generazione di una nuova rotta");			
			}
		}
		if (route.customers.size() > 2)
			solution.addRoute(route);
		return solution;
	}
	
	private VRPTWSolution generateFirstSolution_old(VRPTWProblem problem)  {
		
		
		VRPTWSolution solution = new VRPTWSolution(problem);
		
		int vehicle = 0;
		VRPTWCustomer warehouse = null;
	
		LinkedList<VRPTWCustomer> customerToServe = new LinkedList<VRPTWCustomer>();	
		for (VRPTWCustomer c : problem.customers) {
			if (c.isWarehouse())
				warehouse = c;
			else
				customerToServe.add(c);
		}	
		Collections.sort(customerToServe, new VRPTWCustomerEndTimeWindowComparator());
		
		VRPTWCustomer prev_customer = warehouse;
		
		VRPTWRoute route = new VRPTWRoute(warehouse, problem.getVehicleCapacity());
		while (!customerToServe.isEmpty()) {
			VRPTWCustomer customer = customerToServe.remove();
			boolean capacity_test = route.getRemainCapacity()-customer._demand > 0; 
			boolean timewindow_test = route.travelTime()+VRPTWUtils.distance(prev_customer, customer) < customer._due_date; 
			if (!timewindow_test || !capacity_test) {
				solution.addRoute(route);
				route = new VRPTWRoute(warehouse, problem.getVehicleCapacity());
				prev_customer = warehouse; 
			}
			route.addCustomer(customer);
			prev_customer = customer;
		}
		if (route.size()>0) {
			solution.addRoute(route);
		}
		
		return solution;
	}

	public void activateDebugMode() {
		debug = true;
	}
	
	public void activateDrawingSolutionsMode() {
		draw_solution = true;
	}
	
	public void printSolution(VRPTWSolution s, VRPTWSolution is, int progressivo) {
		
		final VRPTWSolution solution = s;
		final VRPTWSolution initial_solution = is;
		final String filename = folder + basename + "_" +solution._problem.getInstanceName() + "_"+ (progressivo) + estensione;
		
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				
				DrawingArea drawingArea = new DrawingArea(solution, initial_solution);
				
				JFrame.setDefaultLookAndFeelDecorated(true);
				JFrame frame = new JFrame("Solution map");
				frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
				frame.getContentPane().add(drawingArea);
				frame.setSize(1024, 768);
				frame.setLocationRelativeTo( null );
				frame.setVisible(true);
				
				
				BufferedImage awtImage = new BufferedImage(drawingArea.getWidth(), drawingArea.getHeight(), BufferedImage.TYPE_INT_RGB);
				
				Graphics g = awtImage.getGraphics();
				drawingArea.printAll(g);
				
				File file = new File(filename);
				try {
					ImageIO.write(awtImage, "png", file);
				} catch (IOException e) {
					e.printStackTrace(); 
				}
				
				frame.dispose();
			}
		});	
	}

}
