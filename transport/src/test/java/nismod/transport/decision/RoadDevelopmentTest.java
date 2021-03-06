/**
 * 
 */
package nismod.transport.decision;

import static org.junit.Assert.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.geotools.graph.structure.DirectedEdge;
import org.junit.Test;

import nismod.transport.demand.DemandModel;
import nismod.transport.network.road.RoadNetwork;
import nismod.transport.network.road.RouteSetGenerator;
import nismod.transport.utility.ConfigReader;
import nismod.transport.zone.Zoning;

/**
 * @author Milan Lovric
 *
 */
public class RoadDevelopmentTest {
	
	@Test
	public void test() throws IOException {

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

		final String roadDevelopmentFileName = "./src/test/resources/testdata/interventions/roadDevelopment.properties";
		
		//create a road network
		RoadNetwork roadNetwork = new RoadNetwork(zonesUrl, networkUrl, nodesUrl, AADFurl, areaCodeFileName, areaCodeNearestNodeFile, workplaceZoneFileName, workplaceZoneNearestNodeFile, freightZoneToLADfile, freightZoneNearestNodeFile, props);
		roadNetwork.replaceNetworkEdgeIDs(networkUrlFixedEdgeIDs);
		
		List<Intervention> interventions = new ArrayList<Intervention>();
		Properties props2 = new Properties();
		props2.setProperty("type", "RoadDevelopment");
		props2.setProperty("startYear", "2016");
		props2.setProperty("endYear", "2025");
		props2.setProperty("fromNode", "63");
		props2.setProperty("toNode", "23");
		props2.setProperty("biDirectional", "true");
		props2.setProperty("lanesPerDirection", "2");
		props2.setProperty("length", "10.23");
		props2.setProperty("roadClass", "A");
		props2.setProperty("edgeID1", "19003");
		props2.setProperty("edgeID2", "19004");
		RoadDevelopment rd = new RoadDevelopment(props2);
		
		RoadDevelopment rd2 = new RoadDevelopment(roadDevelopmentFileName);
		System.out.println("Road development intervention: " + rd2.toString());
		
		interventions.add(rd);
		
		int currentYear = 2015;
		
		System.out.println("Number of road links/edges before development: " + roadNetwork.getNetwork().getEdges().size());
		assertEquals("The number of road links should be correct", 301, roadNetwork.getNetwork().getEdges().size());
	
		rd.install(roadNetwork);
		
		System.out.println("Number of road links/edges after development: " + roadNetwork.getNetwork().getEdges().size());
		assertEquals("The number of road links in the graph should be correct", 303, roadNetwork.getNetwork().getEdges().size());
		assertEquals("The number of edges in the map should be correct", 303, roadNetwork.getNetwork().getEdges().size());
		assertEquals("The number of road lanes should be correct", (int) Integer.parseInt(rd.getProperty("lanesPerDirection")), (int) roadNetwork.getNumberOfLanes()[rd.getDevelopedEdgeID()]);

		DirectedEdge newEdge = (DirectedEdge) roadNetwork.getEdgeIDtoEdge()[rd.getDevelopedEdgeID()];
		assertEquals("From node ID is correct", newEdge.getNodeA().getID(), Integer.parseInt(props2.getProperty("fromNode")));
		assertEquals("To node ID is correct", newEdge.getNodeB().getID(), Integer.parseInt(props2.getProperty("toNode")));
		
		DirectedEdge newEdge2 = (DirectedEdge) roadNetwork.getEdgeIDtoEdge()[rd.getDevelopedEdgeID2()];
		assertEquals("Edge ID from other direction is correct", newEdge2.getID(), (int)roadNetwork.getEdgeIDtoOtherDirectionEdgeID()[newEdge.getID()]);
		assertEquals("From node ID is correct", newEdge2.getNodeA().getID(), Integer.parseInt(rd.getProperty("toNode")));
		assertEquals("To node ID is correct", newEdge2.getNodeB().getID(), Integer.parseInt(rd.getProperty("fromNode")));
		
		System.out.println(newEdge.getObject());
		
		System.out.println("New edge 1 is mapped to zone: " + roadNetwork.getEdgeToZone().get(newEdge.getID()));
		System.out.println("New edge 2 is mapped to zone: " + roadNetwork.getEdgeToZone().get(newEdge2.getID()));
		assertEquals("New edge 1 is mapped to a correct zone", roadNetwork.getNodeToZone().get(63), roadNetwork.getEdgeToZone().get(newEdge.getID()));
		assertEquals("New edge 2 is mapped to a correct zone", roadNetwork.getNodeToZone().get(23), roadNetwork.getEdgeToZone().get(newEdge2.getID()));
			
		//check length
	
		rd.uninstall(roadNetwork);
		System.out.println("Number of road links/edges after uninstallment: " + roadNetwork.getNetwork().getEdges().size());
		assertEquals("The number of road links should be correct", 301, roadNetwork.getNetwork().getEdges().size());
		
		assertNull("New edge 1 after removal is not mapped to any zone", roadNetwork.getEdgeToZone().get(newEdge.getID()));
		assertNull("New edge 2 after removal is not mapped to any zone", roadNetwork.getEdgeToZone().get(newEdge2.getID()));

		currentYear = 2014;
		//check if correct interventions have been installed
		for (Intervention i: interventions)
			if (i.getStartYear() <= currentYear && i.getEndYear() >= currentYear && !i.getState()) {
				i.install(roadNetwork);
		}
		assertTrue("Intervention should not be installed", !rd.getState());
		
		currentYear = 2026;
		//check if correct interventions have been installed
		for (Intervention i: interventions)
			if (i.getStartYear() <= currentYear && i.getEndYear() >= currentYear && !i.getState()) {
				i.install(roadNetwork);
		}
		assertTrue("Intervention should not be installed", !rd.getState());
		
		currentYear = 2025;
		//check if correct interventions have been installed
		for (Intervention i: interventions)
			if (i.getStartYear() <= currentYear && i.getEndYear() >= currentYear && !i.getState()) {
				i.install(roadNetwork);
		}
		assertTrue("Intervention should be installed", rd.getState());
		
		
		final String roadDevelopmentFileName2 = "./src/test/resources/testdata/interventions/roadDevelopment2.properties";
		RoadDevelopment rd3 = new RoadDevelopment(roadDevelopmentFileName2);
		System.out.println("Road development intervention: " + rd3.toString());
		List<Intervention> interventions2 = new ArrayList<Intervention>();
		interventions2.add(rd3);
		
		final String energyUnitCostsFile = props.getProperty("energyUnitCostsFile");
		final String unitCO2EmissionsFile = props.getProperty("unitCO2EmissionsFile");
		final String engineTypeFractionsFile = props.getProperty("engineTypeFractionsFile");
		final String AVFractionsFile = props.getProperty("autonomousVehiclesFile");

		final String baseYearODMatrixFile = props.getProperty("baseYearODMatrixFile");
		final String freightMatrixFile = props.getProperty("baseYearFreightMatrixFile");
		final String populationFile = props.getProperty("populationFile");
		final String GVAFile = props.getProperty("GVAFile");
		final String elasticitiesFile = props.getProperty("elasticitiesFile");
		final String elasticitiesFreightFile = props.getProperty("elasticitiesFreightFile");

		final String passengerRoutesFile = props.getProperty("passengerRoutesFile");
		final String freightRoutesFile = props.getProperty("freightRoutesFile");
		
		//read routes
		RouteSetGenerator rsg = new RouteSetGenerator(roadNetwork, props);
		rsg.readRoutesBinaryWithoutValidityCheck(passengerRoutesFile);
		rsg.printStatistics();
		rsg.readRoutesBinaryWithoutValidityCheck(freightRoutesFile);
		rsg.printStatistics();
		
		rsg.generateSingleNodeRoutes();
		rsg.calculateAllPathsizes();
		
		final URL temproZonesUrl = new URL(props.getProperty("temproZonesUrl"));
		Zoning zoning = new Zoning(temproZonesUrl, nodesUrl, roadNetwork, props);
		
		//the main demand model
		DemandModel dm = new DemandModel(roadNetwork, baseYearODMatrixFile, freightMatrixFile, populationFile, GVAFile, elasticitiesFile, elasticitiesFreightFile, energyUnitCostsFile, unitCO2EmissionsFile, engineTypeFractionsFile, AVFractionsFile, interventions2, rsg, zoning, props);
		System.out.println(dm.getListsOfLADsForNewRouteGeneration());
		
		rd3.install(dm);
		System.out.println(dm.getListsOfLADsForNewRouteGeneration());

	}
}
