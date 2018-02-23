package nismod.transport.optimisation;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.collections4.keyvalue.MultiKey;

import nismod.transport.decision.CongestionCharging;
import nismod.transport.demand.ODMatrix;
import nismod.transport.demand.RealODMatrix;
import nismod.transport.network.road.RoadNetworkAssignment;
import nismod.transport.network.road.RouteSetGenerator;
import nismod.transport.utility.RandomSingleton;
import nismod.transport.zone.Zoning;

/**
 * Implements SPSA optimisation algorithm (Simultaneous Perturbation Stochastic Approximation). 
 * http://www.jhuapl.edu/SPSA/
 * This version works on TEMPRO zones.
 * @author Milan Lovric
  */
public class SPSA4 {
	
	private final static Logger LOGGER = Logger.getLogger(SPSA4.class.getName());
	
	//maximum and minimum values of OD matrix flows (i.e. constraints)
	public static final double THETA_MAX = 10000000.0;
	public static final double THETA_MIN = 0.0;
	
	//SPSA parameters
	private double a;
	private double A; 
	private double c; 
	private double alpha;
	private double gamma;
	
	private RealODMatrix thetaEstimate; //contains the final result
	private RealODMatrix deltas;
	private RealODMatrix gradientApproximation;
	
	private List<Double> lossFunctionValues;
	
	private RoadNetworkAssignment rna;
	private Zoning zoning;
	private RouteSetGenerator rsg;

	public SPSA4() {
	}
	
	/**
	 * Initialise the SPSA algorithm with starting values.
	 * @param rna Road network assignment.
	 * @param zoning Zoning system for tempro zones.
	 * @param initialTheta Initial OD matrix.
	 * @param a SPSA parameter.
	 * @param A SPSA parameter.
	 * @param c SPSA parameter.
	 * @param alpha SPSA parameter.
	 * @param gamma SPSA parameter.
	 */
	public void initialise(RoadNetworkAssignment rna, Zoning zoning, RouteSetGenerator rsg, RealODMatrix initialTheta, double a, double A, double c, double alpha, double gamma) {
			
		this.rna = rna;
		this.zoning = zoning;
		this.rsg = rsg;
		this.thetaEstimate = initialTheta.clone();
		this.deltas = new RealODMatrix();
		this.gradientApproximation = new RealODMatrix();
		
		this.lossFunctionValues = new ArrayList<Double>();
		
		this.a = a;
		this.A = A;
		this.c = c;
		this.alpha = alpha;
		this.gamma = gamma;
	}
		
	/**
	 * Run the algorithm.
	 * @param maxIterations Maximum number of iterations.
	 */
	public void runSPSA(int maxIterations) {
		
		int k = 1; //counter

		do {
			//evaluate loss function for the current theta
			double loss = this.lossFunction(thetaEstimate);
			//store
			this.lossFunctionValues.add(loss);
			
			System.out.printf("Iteration %d RMSN = %.2f%% %n", k, loss);
			
			//calculate gain coefficients
			double ak = a / Math.pow(A + k, alpha);
			double ck = c / Math.pow(k, gamma);
			System.out.printf("ak = %.5f, ck = %.5f %n", ak, ck);
						
			//generate deltas	
			this.generateDeltas(thetaEstimate.getKeySet());
			//deltas.printMatrixFormatted("Deltas: ");

			//calculate shifted thetas
			RealODMatrix thetaPlus = this.shiftTheta(thetaEstimate, ck, deltas);
			RealODMatrix thetaMinus = this.shiftTheta(thetaEstimate, -1 * ck, deltas);
			//thetaPlus.printMatrixFormatted("Theta plus: ");
			//thetaMinus.printMatrixFormatted("Theta minus: ");
			
			//evaluate loss function
			double yPlus = this.lossFunction(thetaPlus);
			double yMinus = this.lossFunction(thetaMinus);
			//System.out.printf("yPlus = %.5f, yMinus = %.5f %n", yPlus, yMinus);
						
			//approximate gradient
	//		this.approximateGradient(yPlus, yMinus, ck, deltas);
			this.approximateGradientActualChangeInX(yPlus, yMinus, thetaPlus, thetaMinus);
			//gradientApproximation.printMatrixFormatted("Gradient approximation: ");
			
			//estimate new theta
			this.updateThetaEstimate(thetaEstimate, ak, gradientApproximation);
			//thetaEstimate.printMatrixFormatted("New theta estimate: ");
			
			k++;

		} while (k <= maxIterations);
		
		//evaluate loss function for the final theta
		double loss = this.lossFunction(thetaEstimate);
		//store
		this.lossFunctionValues.add(loss);
		
		System.out.printf("Iteration %d RMSN = %.2f%% %n", k, loss);
		
		System.out.println("SPSA stopped. Maximum number of iterations reached");
	}
	
	/**
	 * @return Loss function evaluations for all iterations.
	 */
	public List<Double> getLossFunctionEvaluations() {
		
		return this.lossFunctionValues;
		
	}
	
	public RealODMatrix getThetaEstimate() {
		
		return this.thetaEstimate;
	}
	
	/**
	 * Generates deltas using the Rademacher distribution (i.e. random -1 or 1).
	 * @param keySet The set of origin-destination keys for which deltas need to be generated.
	 * @return Origin-destination matrix with random deltas.
	 */
	private void generateDeltas(Set<MultiKey> keySet) {
		
		RandomSingleton rng = RandomSingleton.getInstance();
		
		for (MultiKey mk: keySet) {
			String origin = (String) mk.getKey(0);
			String destination = (String) mk.getKey(1);
							
			double delta = Math.round(rng.nextDouble()) * 2.0 - 1.0;
			deltas.setFlow(origin, destination, delta);
		}
	}

	/**
	 * Calculate new OD matrix with shifted values.
	 * @param theta Current OD matrix.
	 * @param ck Gain.
	 * @param deltas Random deltas.
	 */
	private RealODMatrix shiftTheta(RealODMatrix theta, double ck, RealODMatrix deltas) {
		
		RealODMatrix shiftedTheta = new RealODMatrix();
		
		for (MultiKey mk: theta.getKeySet()) {
			String origin = (String) mk.getKey(0);
			String destination = (String) mk.getKey(1);
		
			double flow = theta.getFlow(origin, destination);
			double delta = deltas.getFlow(origin, destination);
			double newFlow = flow + ck * delta;
			
			//apply constraints
			newFlow = Math.min(newFlow, THETA_MAX);
			newFlow = Math.max(newFlow, THETA_MIN);
			
			shiftedTheta.setFlow(origin, destination, newFlow);
		}
		
		return shiftedTheta;
	}
		
	/**
	 * Calculate the loss function for a given theta (OD matrix).
	 * @param theta OD matrix.
	 * @return RMSN for the difference between volumes and traffic counts.
	 */
	private double lossFunction(RealODMatrix theta) {
		
		//round values
		ODMatrix odm = new ODMatrix(theta);
		
		//reset as we are re-using the same road network assignment
		rna.resetLinkVolumes();
		rna.resetTripStorages();
		
		//assign passenger flows
		rna.assignPassengerFlowsTempro(odm, this.zoning, this.rsg); //routing version with tempro zones
		rna.expandTripList(); //if fractional assignment used
		rna.updateLinkVolumePerVehicleType(); //used in RMSN calculation
		
		//calculate RMSN
		double RMSN = rna.calculateRMSNforSimulatedVolumes();
		
		return RMSN;
	}
	
	/**
	 * Calculate the loss function of the latest theta estimate (OD matrix).
	 * @return RMSN for the difference between volumes and traffic counts.
	 */
	public double lossFunction() {
		
		return lossFunction(this.thetaEstimate);
	}
	
	/**
	 * Approximate the gradient.
	 * @param yPlus Loss for theta plus.
	 * @param yMinus Loss for theta minus.
	 * @param ck Gain.
	 * @param deltas Random deltas.
	 */
	private void approximateGradient(double yPlus, double yMinus, double ck, RealODMatrix deltas) {
		
		for (MultiKey mk: deltas.getKeySet()) {
			String origin = (String) mk.getKey(0);
			String destination = (String) mk.getKey(1);
		
			double delta = deltas.getFlow(origin, destination);
			double grad = (yPlus - yMinus) / (2 * ck * delta);
				
			this.gradientApproximation.setFlow(origin, destination, grad);
		}
	}
	
	/**
	 * Approximate the gradient.
	 * @param yPlus Loss for theta plus.
	 * @param yMinus Loss for theta minus.
	 * @param ck Gain.
	 * @param deltas Random deltas.
	 */
	private void approximateGradientActualChangeInX(double yPlus, double yMinus, RealODMatrix thetaPlus, RealODMatrix thetaMinus) {
		
		for (MultiKey mk: deltas.getKeySet()) {
			String origin = (String) mk.getKey(0);
			String destination = (String) mk.getKey(1);
		
			double xPlus = thetaPlus.getFlow(origin, destination);
			double xMinus = thetaMinus.getFlow(origin, destination);
				
			double grad = (yPlus - yMinus) / (xPlus - xMinus);
				
			this.gradientApproximation.setFlow(origin, destination, grad);
		}
	}
	
	/**
	 * Obtain new theta estimate.
	 * @param theta Old theta (OD matrix).
	 * @param ak Gain.
	 * @param gradient Gradient.
	 */
	private void updateThetaEstimate(RealODMatrix theta, double ak, RealODMatrix gradient) {
		
		for (MultiKey mk: theta.getKeySet()) {
			String origin = (String) mk.getKey(0);
			String destination = (String) mk.getKey(1);
		
			double flow = theta.getFlow(origin, destination);
			double grad = gradient.getFlow(origin, destination);
			double newFlow = flow - ak * grad;
			
			//apply constraints
			newFlow = Math.min(newFlow, THETA_MAX);
			newFlow = Math.max(newFlow, THETA_MIN);
			
			theta.setFlow(origin, destination, newFlow);
		}
	}

}
