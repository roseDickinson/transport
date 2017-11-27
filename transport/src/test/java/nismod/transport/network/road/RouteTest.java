package nismod.transport.network.road;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.URL;
import java.util.Properties;

import org.geotools.graph.structure.DirectedEdge;
import org.geotools.graph.structure.DirectedNode;
import org.junit.Test;

public class RouteTest {

	@Test
	public void test() throws IOException {
		
		final String areaCodeFileName = "./src/test/resources/testdata/nomisPopulation.csv";
		final String areaCodeNearestNodeFile = "./src/test/resources/testdata/areaCodeToNearestNode.csv";
		final String workplaceZoneFileName = "./src/test/resources/testdata/workplacePopulation.csv";
		final String workplaceZoneNearestNodeFile = "./src/test/resources/testdata/workplaceZoneToNearestNode.csv";
		final String freightZoneToLADfile = "./src/test/resources/testdata/freightZoneToLAD.csv";
		final String freightZoneNearestNodeFile = "./src/test/resources/testdata/freightZoneToNearestNode.csv";
		
		final URL zonesUrl = new URL("file://src/test/resources/testdata/zones.shp");
		final URL networkUrl = new URL("file://src/test/resources/testdata/network.shp");
		final URL networkUrlNew = new URL("file://src/test/resources/testdata/testOutputNetwork.shp");
		final URL nodesUrl = new URL("file://src/test/resources/testdata/nodes.shp");
		final URL AADFurl = new URL("file://src/test/resources/testdata/AADFdirected.shp");

		//create a road network
		RoadNetwork roadNetwork = new RoadNetwork(zonesUrl, networkUrl, nodesUrl, AADFurl, areaCodeFileName, areaCodeNearestNodeFile, workplaceZoneFileName, workplaceZoneNearestNodeFile, freightZoneToLADfile, freightZoneNearestNodeFile);
		roadNetwork.replaceNetworkEdgeIDs(networkUrlNew);
				
		//create routes
		Route r1 = new Route();
		Route r2 = new Route();
		Route r3 = new Route();
		Route r4 = new Route();
		
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

//		System.out.println("Route " + r1.getID() + " is valid: " + r1.isValid());
//		System.out.println("Route " + r1.getID() + ": " + r1.getEdges());
//		System.out.println("Route " + r1.getID() + ": " + r1.toString());
//		System.out.println("Route " + r1.getID() + ": " + r1.getFormattedString());
				
		//set route choice parameters
		Properties params = new Properties();
		params.setProperty("TIME", "-1.5");
		params.setProperty("LENGTH", "-1.5");
		params.setProperty("COST", "-3.6");
		params.setProperty("INTERSECTIONS", "-1.0");
		
		double consumption = 5.4;
		double unitCost = 1.17;
		
		r1.calculateUtility(roadNetwork.getFreeFlowTravelTime(), consumption, unitCost, params);
		
		double time = r1.getTime();
		double length = r1.getLength();
		double cost = r1.getCost();
		double intersections = r1.getNumberOfIntersections();
		double utility = r1.getUtility();
		
		System.out.println("Time: " + time);
		System.out.println("Length: " + length);
		System.out.println("Cost: " + cost);
		System.out.println("Intersections: " + intersections);
		System.out.println("Utility: " + utility);
		
		double paramTime = Double.parseDouble(params.getProperty("TIME"));
		double paramLength = Double.parseDouble(params.getProperty("LENGTH"));
		double paramCost = Double.parseDouble(params.getProperty("COST"));
		double paramIntersections = Double.parseDouble(params.getProperty("INTERSECTIONS"));
		double calculatedUtility = paramTime * time + paramLength * length + paramCost * cost + paramIntersections * intersections;
		
		final double EPSILON = 1e-11; //may fail for higher accuracy
		
		assertEquals("Utility should be correctly calculated", utility, calculatedUtility, EPSILON);
		
		r2.addEdge(e1);
		r2.addEdge(e2);
		r2.addEdge(e4);
		r2.addEdge(e5);
		
//		System.out.println("Route " + r2.getID() + " is valid: " + r2.isValid());
//		System.out.println("Route " + r2.getID() + ": " + r2.getEdges());
//		System.out.println("Route " + r2.getID() + ": " + r2.toString());
//		System.out.println("Route " + r2.getID() + ": " + r2.getFormattedString());
				
		//set route choice parameters
		params = new Properties();
		params.setProperty("TIME", "-1.5");
		params.setProperty("LENGTH", "-1.5");
		params.setProperty("COST", "-3.6");
		params.setProperty("INTERSECTIONS", "-0.1");
		
		r2.calculateUtility(roadNetwork.getFreeFlowTravelTime(), consumption, unitCost, params);
		
		time = r2.getTime();
		length = r2.getLength();
		cost = r2.getCost();
		intersections = r2.getNumberOfIntersections();
		utility = r2.getUtility();
		
		System.out.println("Time: " + time);
		System.out.println("Length: " + length);
		System.out.println("Intersections: " + intersections);
		System.out.println("Utility: " + utility);
		
		paramTime = Double.parseDouble(params.getProperty("TIME"));
		paramLength = Double.parseDouble(params.getProperty("LENGTH"));
		paramCost = Double.parseDouble(params.getProperty("COST"));
		paramIntersections = Double.parseDouble(params.getProperty("INTERSECTIONS"));
		calculatedUtility = paramTime * time + paramLength * length + paramCost * cost + paramIntersections * intersections;
		
		assertEquals("Utility should be correctly calculated", utility, calculatedUtility, EPSILON);
			
		r3.addEdge(e6);
		r3.addEdge(e7);
		r3.addEdge(e2);
		r3.addEdge(e3);
		r3.calculateUtility(roadNetwork.getFreeFlowTravelTime(), consumption, unitCost, null);
		
//		System.out.println("Route " + r3.getID() + " is valid: " + r3.isValid());
//		System.out.println("Route " + r3.getID() + ": " + r3.getEdges());
//		System.out.println("Route " + r3.getID() + ": " + r3.toString());
//		System.out.println("Route " + r3.getID() + ": " + r3.getFormattedString());
				
		//set route choice parameters
		params = new Properties();
		params.setProperty("TIME", "-2.5");
		params.setProperty("LENGTH", "-1.5");
		params.setProperty("COST", "-3.6");
		params.setProperty("INTERSECTIONS", "-0.1");
		
		r3.calculateUtility(roadNetwork.getFreeFlowTravelTime(), consumption, unitCost, params);
		
		time = r3.getTime();
		length = r3.getLength();
		cost = r3.getCost();
		intersections = r3.getNumberOfIntersections();
		utility = r3.getUtility();
		
		System.out.println("Time: " + time);
		System.out.println("Length: " + length);
		System.out.println("Intersections: " + intersections);
		System.out.println("Utility: " + utility);
		
		paramTime = Double.parseDouble(params.getProperty("TIME"));
		paramLength = Double.parseDouble(params.getProperty("LENGTH"));
		paramCost = Double.parseDouble(params.getProperty("COST"));
		paramIntersections = Double.parseDouble(params.getProperty("INTERSECTIONS"));
		calculatedUtility = paramTime * time + paramLength * length + paramCost * cost + paramIntersections * intersections;
		
		assertEquals("Utility should be correctly calculated", utility, calculatedUtility, EPSILON);
			
		r4.addEdge(e6);
		r4.addEdge(e7);
		r4.addEdge(e2);
		r4.addEdge(e4);
		r4.addEdge(e5);

//		System.out.println("Route " + r4.getID() + " is valid: " + r4.isValid());
//		System.out.println("Route " + r4.getID() + ": " + r4.getEdges());
//		System.out.println("Route " + r4.getID() + ": " + r4.toString());
//		System.out.println("Route " + r4.getID() + ": " + r4.getFormattedString());
				
		//set route choice parameters
		params = new Properties();
		params.setProperty("TIME", "-1.5");
		params.setProperty("LENGTH", "-1.0");
		params.setProperty("COST", "-3.6");
		params.setProperty("INTERSECTIONS", "-0.1");
		
		r4.calculateUtility(roadNetwork.getFreeFlowTravelTime(), consumption, unitCost, params);
		
		time = r4.getTime();
		length = r4.getLength();
		cost = r4.getCost();
		intersections = r4.getNumberOfIntersections();
		utility = r4.getUtility();
		
		System.out.println("Time: " + time);
		System.out.println("Length: " + length);
		System.out.println("Cost: " + cost);
		System.out.println("Intersections: " + intersections);
		System.out.println("Utility: " + utility);
		
		paramTime = Double.parseDouble(params.getProperty("TIME"));
		paramLength = Double.parseDouble(params.getProperty("LENGTH"));
		paramCost = Double.parseDouble(params.getProperty("COST"));
		paramIntersections = Double.parseDouble(params.getProperty("INTERSECTIONS"));
		calculatedUtility = paramTime * time + paramLength * length + paramCost * cost + paramIntersections * intersections;
		
		assertEquals("Utility should be correctly calculated", utility, calculatedUtility, EPSILON);
		
		//create a one-node route
		Route r5 = new Route();
		RoadPath rp = new RoadPath();
		rp.add(n1);
		System.out.println(rp.toString());
		System.out.println(rp.buildEdges());

		r5 = new Route(rp);
		System.out.println("Valid: " + r5.isValid());
		System.out.println("Empty: " + r5.isEmpty());
		System.out.println(r5.toString());
		System.out.println(r5.getFormattedString());
		
		r5.calculateLength();
		r5.calculateTravelTime(roadNetwork.getFreeFlowTravelTime());
		r5.calculateCost(consumption, unitCost);
		System.out.println("Intersections: " + r5.getNumberOfIntersections());
		r5.calculateUtility(roadNetwork.getFreeFlowTravelTime(), consumption, unitCost, params);
		System.out.println("Length: " + r5.getLength());
		System.out.println("Time: " + r5.getTime());
		System.out.println("Cost: " + r5.getCost());
		System.out.println("Utility: " + r5.getUtility());
		
		System.out.println("First node: " + r5.getOriginNode());
		System.out.println("Last node: " + r5.getDestinationNode());
			
	}
}
