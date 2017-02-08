/**
 * 
 */
package nismod.transport.demand;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import org.apache.commons.collections4.keyvalue.MultiKey;
import org.apache.commons.collections4.map.MultiKeyMap;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

/**
 * Skim matrix
 * @author Milan Lovric
 *
 */
public class SkimMatrix {
	
	private MultiKeyMap matrix;
		
	public SkimMatrix() {
		
		matrix = new MultiKeyMap();
	}
	
	/**
	 * Constructor that reads skim matrix from an input csv file
	 * @param filePath Path to the input file
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public SkimMatrix(String fileName) throws FileNotFoundException, IOException {
		
		matrix = new MultiKeyMap();
		CSVParser parser = new CSVParser(new FileReader(fileName), CSVFormat.DEFAULT.withHeader());
		//System.out.println(parser.getHeaderMap().toString());
		Set<String> keySet = parser.getHeaderMap().keySet();
		keySet.remove("origin");
		//System.out.println("keySet = " + keySet);
		double cost;
		for (CSVRecord record : parser) { 
			//System.out.println(record);
			//System.out.println("Origin zone = " + record.get(0));
			for (String destination: keySet) {
				//System.out.println("Destination zone = " + destination);
				cost = Double.parseDouble(record.get(destination));
				matrix.put(record.get(0), destination, cost);			
			}
		} parser.close(); 
	}
	
	/**
	 * Gets cost for a given origin-destination pair.
	 * @param originZone Origin zone.
	 * @param destinationZone Destination zone.
	 * @return Origin-destination cost.
	 */
	public double getCost(String originZone, String destinationZone) {
		
		return (double) matrix.get(originZone, destinationZone);
	}
	
	/**
	 * Sets cost for a given origin-destination pair.
	 * @param originZone Origin zone.
	 * @param destinationZone Destination zone.
	 * @param cost Origin-destination cost.
	 */
	public void setCost(String originZone, String destinationZone, double cost) {
		
		matrix.put(originZone, destinationZone, cost);
	}
	
	/**
	 * Prints the matrix.
	 */
	public void printMatrix() {
		
		System.out.println(matrix.toString());
	}
	
	/**
	 * Prints the matrix as a formatted table.
	 */
	public void printMatrixFormatted() {
		
		Set<String> firstKey = new HashSet<String>();
		Set<String> secondKey = new HashSet<String>();
		
		//extract row and column keysets
		for (Object mk: matrix.keySet()) {
			String origin = (String) ((MultiKey)mk).getKey(0);
			String destination = (String) ((MultiKey)mk).getKey(1);
			firstKey.add(origin);
			secondKey.add(destination);
		}
	
		//put them to a list and sort them
		List<String> firstKeyList = new ArrayList<String>(firstKey);
		List<String> secondKeyList = new ArrayList<String>(secondKey);
		Collections.sort(firstKeyList);
		Collections.sort(secondKeyList);
		//System.out.println(firstKeyList);
		//System.out.println(secondKeyList);
	
		//formatted print
		System.out.print("origin   "); for (String s: secondKeyList) System.out.printf("%10s",s);
		System.out.println();
		for (String o: firstKeyList) {
			System.out.print(o);
			for (String s: secondKeyList) System.out.printf("%10.2f", matrix.get(o,s));
			System.out.println();
		}
	}
		
	/**
	 * Gets the keyset of the multimap.
	 * @return
	 */
	public Set<MultiKey> getKeySet() {
		
		return matrix.keySet();
	}
	
	/**
	 * Gets average OD cost.
	 * @return
	 */
	public double getAverageCost() {
		
		double averageCost = 0.0;
		for (Object cost: matrix.values()) averageCost += (double) cost;
		averageCost /= matrix.size();
		
		return averageCost;
	}
	
	/**
	 * Gets average OD cost weighted by demand.
	 * @param flows The demand as an origin-destination matrix.
	 * @return
	 */
	public double getAverageCost(ODMatrix flows) {
		
		double averageCost = 0.0;
		long totalFlows = 0;
		for (MultiKey mk: flows.getKeySet()) {
			String origin = (String) mk.getKey(0);
			String destination = (String) mk.getKey(1);
			averageCost += flows.getFlow(origin, destination) * (double) matrix.get(origin, destination);
			totalFlows += flows.getFlow(origin, destination);
		}
		averageCost /= totalFlows;
		
		return averageCost;
	}
	
	/**
	 * Gets sum of absolute differences between elements of two matrices.
	 * @param other The other matrix.
	 * @return Sum of absolute differences.
	 */
	public double getAbsoluteDifference(SkimMatrix other) {
		
		double difference = 0.0;
		for (MultiKey mk: other.getKeySet()) {
			String origin = (String) mk.getKey(0);
			String destination = (String) mk.getKey(1);
			difference += Math.abs(this.getCost(origin, destination) - other.getCost(origin, destination));
		}
	
		return difference;
	}
}
