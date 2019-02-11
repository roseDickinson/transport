package nismod.transport.rail;

import java.io.IOException;
import java.util.Properties;

import org.junit.Test;

import nismod.transport.rail.RailStation.RailModeType;
import nismod.transport.utility.ConfigReader;

/**
 * Test class for the RailStationDemand and Station class.
 * @author Milan Lovric
  */
public class RailStationDemandTest {
	
	@Test
	public void miniTest() throws IOException {

		final String configFile = "./src/test/config/miniTestConfig.properties";
		Properties props = ConfigReader.getProperties(configFile);
		
		RailStation st1 = new RailStation(0, RailModeType.NRAIL, "London Waterloo", "London Waterloo Rail Station", 12340, 12450, 12450, 15044, 363, "E06000006", "London Camden");
		System.out.println(st1);
		
		final String railStationDemandFileName = props.getProperty("baseYearRailStationUsageFile");
		RailStationDemand rsd = new RailStationDemand(railStationDemandFileName);
		
		rsd.printRailDemand("This is rail demand:");
		rsd.printRailDemandNLCSorted("This is rail demand sorted on NLC:");
		rsd.printRailDemandNameSorted("This is rail demand sorted on station name:");
		rsd.printRailDemandUsageSorted("This is rail demand sorted on station usage:");

	}

	@Test
	public void test() throws IOException {

		final String configFile = "./src/test/config/testConfig.properties";
		Properties props = ConfigReader.getProperties(configFile);
		
		RailStation st1 = new RailStation(0, RailModeType.NRAIL, "London Waterloo", "London Waterloo Rail Station", 12340, 12450, 12450, 15044, 363, "E06000006", "London Camden");
		System.out.println(st1);
		
		final String railStationDemandFileName = props.getProperty("baseYearRailStationUsageFile");
		RailStationDemand rsd = new RailStationDemand(railStationDemandFileName);
		
		rsd.printRailDemand("This is rail demand:");
		rsd.printRailDemandNLCSorted("This is rail demand sorted on NLC:");
		rsd.printRailDemandNameSorted("This is rail demand sorted on station name:");
		rsd.printRailDemandUsageSorted("This is rail demand sorted on station usage:");

	}
}
