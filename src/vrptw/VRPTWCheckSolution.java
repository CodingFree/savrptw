package vrptw;











public class VRPTWCheckSolution {

	public static void main(String[] args) throws InterruptedException {
	
		VRPTWProblem problem = new VRPTWProblem("C101", 200);
		VRPTWSolution solution = new VRPTWSolution(problem);
		
		solution.addRoute("6 4 8 9 11 12 10 7 5 2 76");
		solution.addRoute("33 34 32 36 38 39 40 37 35");
		solution.addRoute("68 66 64 63 75 73 62 65 69 67 70");
		solution.addRoute("91 88 87 84 83 85 86 89 90 92");
		solution.addRoute("44 43 42 41 45 47 46 49 52 51 53 50 48");
		solution.addRoute("99 97 96 95 93 94 98 100 3");
		solution.addRoute("82 79 77 72 71 74 78 80 81");
		solution.addRoute("14 18 19 20 16 17 15 13");
		solution.addRoute("21 25 26 28 30 31 29 27 24 23 22");
		solution.addRoute("58 56 55 54 57 59 61 60");

		solution.show();
		
		for (VRPTWRoute route : solution.routes) {
			if (!route.check_compactness()) {
				System.err.println("Errore, la rotta non è fattibile");
				System.exit(1);
			}
		}
		
		for (VRPTWCustomer customer : problem.customers) {
			if ( !customer.isWarehouse() && (customer.getArrivalTime() == 0) ) {
				System.err.println("Errore, il cliente " + customer.getID() + " non è stato inserito in alcuna rotta!");
			}
		}
			
	}

}
