package nismod.transport.network.road;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.tuple.Pair;
import org.geotools.graph.structure.DirectedEdge;
import org.geotools.graph.structure.DirectedNode;
import org.junit.Test;

import nismod.transport.network.road.RoadNetworkAssignment.EnergyType;
import nismod.transport.network.road.RoadNetworkAssignment.EngineType;
import nismod.transport.network.road.RoadNetworkAssignment.VehicleType;
import nismod.transport.network.road.Route.WebTAG;
import nismod.transport.utility.ConfigReader;

public class RouteSetTest {

	@Test
	public void test() throws IOException	{

		final String configFile = "./src/test/config/testConfig.properties";
		Properties props = ConfigReader.getProperties(configFile);
		
		final String areaCodeFileName = props.getProperty("areaCodeFileName");
		final String areaCodeNearestNodeFile = props.getProperty("areaCodeNearestNodeFile");
		final String workplaceZoneFileName = props.getProperty("workplaceZoneFileName");
		final String workplaceZoneNearestNodeFile = props.getProperty("workplaceZoneNearestNodeFile");
		final String freightZoneToLADfile = props.getProperty("freightZoneToLADfile");
		final String freightZoneNearestNodeFile = props.getProperty("freightZoneNearestNodeFile");

		final URL zonesUrl = new URL(props.getProperty("zonesUrl"));
		final URL networkUrl = new URL(props.getProperty("networkUrl"));
		final URL networkUrlFixedEdgeIDs = new URL(props.getProperty("networkUrlFixedEdgeIDs"));
		final URL nodesUrl = new URL(props.getProperty("nodesUrl"));
		final URL AADFurl = new URL(props.getProperty("AADFurl"));

		//create a road network
		RoadNetwork roadNetwork = new RoadNetwork(zonesUrl, networkUrl, nodesUrl, AADFurl, areaCodeFileName, areaCodeNearestNodeFile, workplaceZoneFileName, workplaceZoneNearestNodeFile, freightZoneToLADfile, freightZoneNearestNodeFile, props);
		roadNetwork.replaceNetworkEdgeIDs(networkUrlFixedEdgeIDs);
		
		//create routes
		Route r1 = new Route(roadNetwork);
		Route r2 = new Route(roadNetwork);
		Route r3 = new Route(roadNetwork);
		Route r4 = new Route(roadNetwork);
		
		DirectedNode n1 = (DirectedNode) roadNetwork.getNodeIDtoNode().get(7);
		DirectedNode n2 = (DirectedNode) roadNetwork.getNodeIDtoNode().get(8);
		DirectedNode n3 = (DirectedNode) roadNetwork.getNodeIDtoNode().get(27);
		DirectedNode n4 = (DirectedNode) roadNetwork.getNodeIDtoNode().get(9);
		DirectedNode n5 = (DirectedNode) roadNetwork.getNodeIDtoNode().get(55);
		DirectedNode n6 = (DirectedNode) roadNetwork.getNodeIDtoNode().get(40);
			
		DirectedEdge e1 = (DirectedEdge) n1.getOutEdge(n2);
		DirectedEdge e2 = (DirectedEdge) n2.getOutEdge(n4);
		DirectedEdge e3 = (DirectedEdge) n4.getOutEdge(n6);
		DirectedEdge e4 = (DirectedEdge) n4.getOutEdge(n5);
		DirectedEdge e5 = (DirectedEdge) n5.getOutEdge(n6);
		DirectedEdge e6 = (DirectedEdge) n1.getOutEdge(n3);
		DirectedEdge e7 = (DirectedEdge) n3.getOutEdge(n2);
		
		r1.addEdge(e1);
		r1.addEdge(e2);
		r1.addEdge(e3);
		//r1.calculateUtility(roadNetwork.getFreeFlowTravelTime(), null);
					
		r2.addEdge(e1);
		r2.addEdge(e2);
		r2.addEdge(e4);
		r2.addEdge(e5);
		//r2.calculateUtility(roadNetwork.getFreeFlowTravelTime(), null);
		
		r3.addEdge(e6);
		r3.addEdge(e7);
		r3.addEdge(e2);
		r3.addEdge(e3);
		//r3.calculateUtility(roadNetwork.getFreeFlowTravelTime(), null);
		
		r4.addEdge(e6);
		r4.addEdge(e7);
		r4.addEdge(e2);
		r4.addEdge(e4);
		r4.addEdge(e5);
		//r4.calculateUtility(roadNetwork.getFreeFlowTravelTime(), null);
		
		DirectedNode originNode = (DirectedNode)roadNetwork.getNodeIDtoNode().get(7);
		DirectedNode destinationNode = (DirectedNode)roadNetwork.getNodeIDtoNode().get(40);
		
		RouteSet rs = new RouteSet(roadNetwork);
		
		//set route choice parameters
		Properties params = new Properties();
		params.setProperty("TIME", "-1.5");
		params.setProperty("LENGTH", "-1.0");
		params.setProperty("COST", "-3.6");
		params.setProperty("INTERSECTIONS", "-0.1");
		params.setProperty("AVERAGE_INTERSECTION_DELAY", "0.8");
		
		Map<WebTAG, Double> parameters = new EnumMap<>(WebTAG.class);
		parameters.put(WebTAG.A, 1.11932239320862);
		parameters.put(WebTAG.B, 0.0440047704089497);
		parameters.put(WebTAG.C, -0.0000813834474888197);
		parameters.put(WebTAG.D, 2.44908328418021E-06);
		
		HashMap<Pair<VehicleType, EngineType>, Map<WebTAG, Double>> energyConsumptionParameters = new HashMap<Pair<VehicleType, EngineType>, Map<WebTAG, Double>>();
		energyConsumptionParameters.put(Pair.of(VehicleType.CAR, EngineType.ICE_PETROL), parameters);
		energyConsumptionParameters.put(Pair.of(VehicleType.CAR, EngineType.BEV), parameters);
		
		HashMap<Pair<VehicleType, EngineType>, Double> relativeFuelEfficiency = new HashMap<Pair<VehicleType, EngineType>, Double>();
		relativeFuelEfficiency.put(Pair.of(VehicleType.CAR, EngineType.ICE_PETROL), 0.9);
		relativeFuelEfficiency.put(Pair.of(VehicleType.CAR, EngineType.BEV), 0.9);
		
		Map<EnergyType, Double> energyUnitCosts = new EnumMap<>(EnergyType.class);
		energyUnitCosts.put(EnergyType.PETROL, 1.17);
		energyUnitCosts.put(EnergyType.DIESEL, 1.17);
		energyUnitCosts.put(EnergyType.CNG, 1.17);
		energyUnitCosts.put(EnergyType.LPG, 1.17);
		energyUnitCosts.put(EnergyType.HYDROGEN, 1.17);
		energyUnitCosts.put(EnergyType.ELECTRICITY, 1.17);
		
		//rs.addRoute(r1);
		rs.addRoute(r4);
		rs.addRoute(r2);
		rs.addRoute(r3);
	
		rs.printChoiceSet();
		rs.printStatistics();
		
		rs.calculatePathsizes();
		rs.printPathsizes();
		
		for (double pathsize: rs.getPathsizes()) {
			assertThat(0.0, lessThan(pathsize)); //all pathsizes should be larger than 0 and less than or equal to 1.
			assertThat(1.0, greaterThanOrEqualTo(pathsize)); 
		}
		
		//rs.calculateUtilities(roadNetwork.getFreeFlowTravelTime(), params);
		rs.calculateUtilities(VehicleType.CAR, EngineType.PHEV_PETROL, roadNetwork.getFreeFlowTravelTime(), energyConsumptionParameters, relativeFuelEfficiency, energyUnitCosts, null, params);
		rs.printUtilities();
	
		for (double utility: rs.getUtilities())
			assertThat(0.0, greaterThanOrEqualTo(utility)); //all utilities should be negative (or 0)
		
		rs.calculateProbabilities();
		rs.printProbabilities();
		
		//test that the sum of probabilities is 1.0
		final double EPSILON = 1e-11; //may fail for higher accuracy
		double probabilitySum = 0.0;
		for (double probability: rs.getProbabilities()) 
			probabilitySum += probability;
		
		System.out.printf("The sum of probabilites is: %.12f.%n", probabilitySum);
		assertEquals("The sum of probabilities is 1.0", 1.0, probabilitySum, EPSILON);
		
		rs.printUtilities();
		rs.printProbabilities();

		for (double utility: rs.getUtilities())
			assertThat(0.0, greaterThanOrEqualTo(utility)); //all utilities should be negative (or 0)

		//test that the sum of probabilities is 1.0
		probabilitySum = 0.0;
		for (double probability: rs.getProbabilities()) 
			probabilitySum += probability;
		
		//check that probabilities are also sorted after sorting the utilities
		double[] sorted = rs.getProbabilities();
		Arrays.sort(sorted);
		assertEquals ("Probabilities list is sorted", sorted, rs.getProbabilities());
		
		int[] choiceFrequency = new int[4];
		for (int i=0; i<1000; i++) {
			Route chosenRoute = rs.choose(null);
			int choiceIndex = rs.getIndexOfRoute(chosenRoute);
			choiceFrequency[choiceIndex]++;
		}
		
		System.out.println("Choice frequencies: ");
		System.out.println(Arrays.toString(choiceFrequency));
		
		RoadPath rp = roadNetwork.getFastestPath(n1, n6, null);
		System.out.println("Fastest path: " + rp);
		System.out.println("Edges: " + rp.getEdges());
		for (Object o: rp.getEdges()) {
			DirectedEdge e = (DirectedEdge) o;
			System.out.println(e.getInNode() + "->" + e.getOutNode());
		}
		Route newRoute = new Route(rp, roadNetwork);
		System.out.println(newRoute.isValid());
		
		rs.addRoute(newRoute);
		rs.printChoiceSet();
		rs.printStatistics();
		//all routes need to have re-calculated utility and path size after the new route is added!
		rs.calculatePathsizes();
		rs.calculateUtilities(VehicleType.CAR, EngineType.PHEV_PETROL, roadNetwork.getFreeFlowTravelTime(), energyConsumptionParameters, relativeFuelEfficiency, energyUnitCosts, null, params);
		rs.printUtilities();
		rs.calculateProbabilities();
		rs.printProbabilities();
		rs.printChoiceSet();
		rs.printUtilities();
		rs.printPathsizes();
		rs.printProbabilities();
		Route chosenRoute = rs.choose(params);
		System.out.println("Chosen route: " + chosenRoute.toString());
		
		//create a one-node route
		Route r5 = new Route(roadNetwork);
		rp = new RoadPath();
		rp.add(n1);
		System.out.println(rp.toString());
		System.out.println(rp.buildEdges());

		r5 = new Route(rp, roadNetwork);
		System.out.println("Valid: " + r5.isValid());
		System.out.println("Empty: " + r5.isEmpty());
		System.out.println(r5.toString());
		System.out.println(r5.getFormattedString());
		
		rs = new RouteSet(roadNetwork);
		rs.addRoute(r5);
		rs.printChoiceSet();
		rs.printStatistics();
		rs.calculatePathsizes();
		rs.calculateUtilities(VehicleType.CAR, EngineType.PHEV_PETROL, roadNetwork.getFreeFlowTravelTime(), energyConsumptionParameters, relativeFuelEfficiency, energyUnitCosts, null, params);
		rs.printUtilities();
		rs.calculateProbabilities();
		rs.printPathsizes();
		rs.printProbabilities();
	}
}
