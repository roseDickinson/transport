/**
 * 
 */
package nismod.transport.demand;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import org.apache.sanselan.ImageWriteException;
import org.junit.BeforeClass;
import org.junit.Test;

import nismod.transport.network.road.RoadNetwork;
import nismod.transport.network.road.RoadNetworkAssignment;
import nismod.transport.network.road.RouteSetGenerator;
import nismod.transport.utility.ConfigReader;
import nismod.transport.utility.InputFileReader;
import nismod.transport.zone.Zoning;

/**
 * Tests for the RealODMatrixTempro class
 * @author Milan Lovric
 *
 */
public class RealODMatrixTemproTest {
	
	public static void main( String[] args ) throws FileNotFoundException, IOException, ImageWriteException {
		
		final String configFile = "./src/main/full/config/config.properties";
		//final String configFile = "./src/test/config/testConfig.properties";
		//final String configFile = "./src/test/config/miniTestConfig.properties";
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

		final String temproODMatrixFile = props.getProperty("temproODMatrixFile");
		
		final String outputFolder = props.getProperty("outputFolder");
		
		//create output directory
	     File file = new File(outputFolder);
	        if (!file.exists()) {
	            if (file.mkdirs()) {
	                System.out.println("Output directory is created.");
	            } else {
	                System.err.println("Failed to create output directory.");
	            }
	        }

		//create a road network
		RoadNetwork roadNetwork = new RoadNetwork(zonesUrl, networkUrl, nodesUrl, AADFurl, areaCodeFileName, areaCodeNearestNodeFile, workplaceZoneFileName, workplaceZoneNearestNodeFile, freightZoneToLADfile, freightZoneNearestNodeFile, props);
		roadNetwork.replaceNetworkEdgeIDs(networkUrlFixedEdgeIDs);

		final URL temproZonesUrl = new URL(props.getProperty("temproZonesUrl"));
		Zoning zoning = new Zoning(temproZonesUrl, nodesUrl, roadNetwork);
		
		RealODMatrixTempro temproODM = new RealODMatrixTempro(temproODMatrixFile, zoning);
		//temproODM.deleteInterzonalFlows("E02006781");

		//RealODMatrix ladODM = RealODMatrixTempro.createLadMatrixFromTEMProMatrix(temproODM, zoning);
		//ladODM.printMatrixFormatted("LAD matrix:", 2);
		//ladODM.saveMatrixFormatted("ladFromTemproODM.csv");
		
		temproODM.saveMatrixFormatted2("temproMatrixListBased.csv");
		temproODM.saveMatrixFormatted3("temproMatrixListBased2.csv");
		
	}
	
	@BeforeClass
	public static void initialise() {
	
	    File file = new File("./temp");
	    if (!file.exists()) {
	        if (file.mkdir()) {
	            System.out.println("Temp directory is created.");
	        } else {
	            System.err.println("Failed to create temp directory.");
	        }
	    }
	}
	
	@Test
	public void test() throws FileNotFoundException, IOException {
		
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
		
		final URL temproZonesUrl = new URL(props.getProperty("temproZonesUrl"));
		
		Zoning zoning = new Zoning(temproZonesUrl, nodesUrl, roadNetwork);
		
		//E06000045 (E02003552, E02003553)
		//E07000091  (E02004801, E02004800)
		
		RealODMatrixTempro tempro = new RealODMatrixTempro(zoning);
		tempro.setFlow("E02004800", "E02003552", 1.0);
		tempro.setFlow("E02004800", "E02003553", 2.0);
		tempro.setFlow("E02004801", "E02003552", 3.0);
		tempro.setFlow("E02004801", "E02003553", 4.0);
		//tempro.printMatrixFormatted("Tempro matrix:", 2);
		
		RealODMatrix real = new RealODMatrix();
		real.setFlow("E07000091", "E06000045", 10.0);
		real.printMatrixFormatted("LAD matrix", 2);
		
		RealODMatrix ladMatrix = RealODMatrixTempro.createLadMatrixFromTEMProMatrix(tempro, zoning);
		ladMatrix.printMatrixFormatted("Lad matrix:", 2);
		
		RealODMatrixTempro tempro2 = RealODMatrixTempro.createTEMProFromLadMatrix(ladMatrix, tempro, zoning);
		//tempro2.printMatrixFormatted("Tempro matrix 2",  2);
		
		final double DELTA = 0.00001;
		assertEquals("Absolute difference between equal matrices should be zero", 0.0, tempro.getAbsoluteDifference(tempro2), DELTA);

		HashMap<String, Integer> tripEnds = tempro.calculateTripEnds();
		HashMap<String, Integer> tripStarts = tempro.calculateTripStarts();
		int sumTripEnds = 0, sumTripStarts = 0;
		for (int num: tripEnds.values()) sumTripEnds += num;
		for (int num: tripStarts.values()) sumTripStarts += num;
		assertEquals("Sum of trips ends and trip starts is equal", sumTripEnds, sumTripStarts);
				
		RealODMatrixTempro tempro3 = tempro.clone();
		assertEquals("Absolute difference between equal matrices should be zero", 0.0, tempro.getAbsoluteDifference(tempro3), DELTA);
		
		tempro.setFlow("E02004800", "E02003552", 1.1);
		tempro.setFlow("E02004800", "E02003553", 2.4);
		tempro.setFlow("E02004801", "E02003552", 3.5);
		tempro.setFlow("E02004801", "E02003553", 4.9);
		tempro2.setFlow("E02004800", "E02003552", 1.1);
		tempro2.setFlow("E02004800", "E02003553", 2.4);
		tempro2.setFlow("E02004801", "E02003552", 3.5);
		tempro2.setFlow("E02004801", "E02003553", 4.9);
		tempro3.setFlow("E02004800", "E02003552", 1.1);
		tempro3.setFlow("E02004800", "E02003553", 2.4);
		tempro3.setFlow("E02004801", "E02003552", 3.5);
		tempro3.setFlow("E02004801", "E02003553", 4.9);
		
		tempro.ceilMatrixValues(); //2, 3, 4, 5
		assertEquals("Cell value is correct", 2, tempro.getFlow("E02004800", "E02003552"), DELTA);
		assertEquals("Cell value is correct", 3, tempro.getFlow("E02004800", "E02003553"), DELTA);
		assertEquals("Cell value is correct", 4, tempro.getFlow("E02004801", "E02003552"), DELTA);
		assertEquals("Cell value is correct", 5, tempro.getFlow("E02004801", "E02003553"), DELTA);
		
		tempro2.floorMatrixValues(); //1, 2, 3, 4
		assertEquals("Cell value is correct", 1, tempro2.getFlow("E02004800", "E02003552"), DELTA);
		assertEquals("Cell value is correct", 2, tempro2.getFlow("E02004800", "E02003553"), DELTA);
		assertEquals("Cell value is correct", 3, tempro2.getFlow("E02004801", "E02003552"), DELTA);
		assertEquals("Cell value is correct", 4, tempro2.getFlow("E02004801", "E02003553"), DELTA);
		
		tempro3.roundMatrixValues(); //1, 2, 4, 5
		assertEquals("Cell value is correct", 1, tempro3.getFlow("E02004800", "E02003552"), DELTA);
		assertEquals("Cell value is correct", 2, tempro3.getFlow("E02004800", "E02003553"), DELTA);
		assertEquals("Cell value is correct", 4, tempro3.getFlow("E02004801", "E02003552"), DELTA);
		assertEquals("Cell value is correct", 5, tempro3.getFlow("E02004801", "E02003553"), DELTA);
		
		assertEquals("After rounding should be the same", tempro3.getFlow("E02004800", "E02003552"), tempro3.getIntFlow("E02004800", "E02003552"), DELTA);
		assertEquals("After rounding total flows should be the same", tempro3.getTotalIntFlow(), tempro3.getSumOfFlows(), DELTA);

		double before = tempro.getTotalIntFlow();
		tempro.scaleMatrixValue(2.0);
		assertEquals("After scaling total flows should be as expected", before * 2.0, tempro.getTotalIntFlow(), DELTA);
		
		tempro.scaleMatrixValue(tempro2);

		ArrayList<String> origins, destinations;
		origins = new ArrayList<String>();
		origins.add("E02004800");
		destinations = new ArrayList<String>();
		destinations.add("E02003552");
		destinations.add("E02003553");
		
		tempro.sumMatrixSubset(origins, destinations);
		
		int numberOfZones = zoning.getZoneIDToCodeMap().keySet().size();
		System.out.println("Number of zones = " + zoning.getZoneIDToCodeMap().keySet().size());
		RealODMatrixTempro unit1 = RealODMatrixTempro.createUnitMatrix(zoning);
		assertEquals("Total flows for unit matrix should be as expected", numberOfZones*numberOfZones, unit1.getSumOfFlows(), DELTA);
		
		unit1.deleteInterzonalFlows("E02004800");
		//unit1.printMatrixFormatted(2);
		assertEquals("Total flows after interzonal deletion should be as expected", numberOfZones*numberOfZones - 2*(numberOfZones-1), unit1.getSumOfFlows(), DELTA);

		unit1.saveMatrixFormatted("./temp/unitTempro.csv");
		unit1.saveMatrixFormatted2("./temp/unitTempro2.csv");
		unit1.saveMatrixFormatted3("./temp/unitTempro3.csv");
		
		RealODMatrixTempro unit1a = new RealODMatrixTempro("./temp/unitTempro.csv", zoning);
		RealODMatrixTempro unit1b = new RealODMatrixTempro("./temp/unitTempro2.csv", zoning);
		assertEquals("Total flows should be the same for two formats", 	unit1a.getTotalIntFlow(), unit1b.getTotalIntFlow());
	
		RealODMatrixTempro unit2 = RealODMatrixTempro.createUnitMatrix(origins, zoning);
		assertEquals("Total flows for unit matrix should be as expected", 1.0, unit2.getSumOfFlows(), DELTA);

		RealODMatrixTempro unit3 = RealODMatrixTempro.createUnitMatrix(origins, destinations, zoning);
		assertEquals("Total flows for unit matrix should be as expected", 2.0, unit3.getSumOfFlows(), DELTA);
	}
}