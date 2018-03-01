package nismod.transport.zone;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.geotools.data.CachingFeatureSource;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.graph.structure.Node;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;

import nismod.transport.network.road.RoadNetwork;

/**
 * For mapping zones (e.g. TEMPRO) to the nodes of the road network.
 * @author Milan Lovric
  */
public class Zoning {
	
	private ShapefileDataStore zonesShapefile;
	private ShapefileDataStore nodesShapefile;
	private RoadNetwork rn;
	
	private HashMap<String, Integer> zoneToNearestNodeID;
	private HashMap<String, Double> zoneToNearestNodeDistance;
	
	private HashMap<String, List<Pair<Integer, Double>>> zoneToSortedListOfNodeAndDistancePairs;
	
	private HashMap<Integer, String> nodeToZoneInWhichLocated; //maps node to Tempro zone in which it is located
	private HashMap<String, List<Integer>> zoneToListOfContainedNodes; //maps Tempro zone to a list of nodes within that zone (if they exist)
	private HashMap<String, String> zoneToLAD; //maps Tempro zone to LAD zone
	
	private HashMap<String, Integer> temproCodeToID;
	private HashMap<Integer, String> temproIDToCode;
		
	/**
	 * @param zonesUrl
	 * @param nodesUrl
	 * @param rn
	 * @throws IOException
	 */
	public Zoning(URL zonesUrl, URL nodesUrl, RoadNetwork rn) throws IOException {
	
		this.zonesShapefile = new ShapefileDataStore(zonesUrl);
		this.nodesShapefile = new ShapefileDataStore(nodesUrl);
		this.rn = rn;
		
		CachingFeatureSource cache3 = new CachingFeatureSource(nodesShapefile.getFeatureSource());
		SimpleFeatureCollection nodesFeatureCollection = cache3.getFeatures();
		CachingFeatureSource cache4 = new CachingFeatureSource(zonesShapefile.getFeatureSource());
		SimpleFeatureCollection zonesFeatureCollection = cache4.getFeatures();
		
		//map codes and IDs
		mapCodesAndIDs(zonesFeatureCollection);
		//map zones to nodes
		mapZonesToNodes(zonesFeatureCollection);
		//map nodes to zones
		mapNodesToZones(zonesFeatureCollection);
		
		mapZonesToNodesAndDistances(zonesFeatureCollection);
		mapZonesToContainedNodes();
		
	}
	
	/**
	 * Maps tempro zone IDs to codes.
	 * @param zonesFeatureCollection Feature collection with the zones.
	 */
	private void mapCodesAndIDs(SimpleFeatureCollection zonesFeatureCollection) {

		this.temproCodeToID = new HashMap<String, Integer>();
		this.temproIDToCode = new HashMap<Integer, String>();

		//iterate through the zones and through the nodes
		SimpleFeatureIterator iter = zonesFeatureCollection.features();
		try {
			while (iter.hasNext()) {
				SimpleFeature sf = iter.next();
				Integer zoneID = ((Long) sf.getAttribute("Zone_ID")).intValue();
				String zoneCode = (String) sf.getAttribute("Zone_Code");
				
				this.temproCodeToID.put(zoneCode, zoneID);
				this.temproIDToCode.put(zoneID, zoneCode);
			} 
		} finally {
			//feature iterator is a live connection that must be closed
			iter.close();
		}
	}
		
	/**
	 * Maps zones to nearest nodes of the network.
	 * @param zonesFeatureCollection Feature collection with the zones.
	 */
	private void mapZonesToNodes(SimpleFeatureCollection zonesFeatureCollection) {

		this.zoneToNearestNodeID = new HashMap<String, Integer>();
		this.zoneToNearestNodeDistance = new HashMap<String, Double>();
		
		this.zoneToLAD = new HashMap<String, String>();

		//iterate through the zones and through the nodes
		SimpleFeatureIterator iter = zonesFeatureCollection.features();
		try {
			while (iter.hasNext()) {
				SimpleFeature sf = iter.next();
				MultiPolygon polygon = (MultiPolygon) sf.getDefaultGeometry();
				String zoneID = (String) sf.getAttribute("Zone_Code");
				
				String ladID = (String) sf.getAttribute("LAD_Code");
				this.zoneToLAD.put(zoneID, ladID);	
				
				Point centroid = polygon.getCentroid();
				double minDistance = Double.MAX_VALUE;
				Integer nearestNodeID = null;

				Iterator nodeIter = (Iterator) this.rn.getNetwork().getNodes().iterator();
				while (nodeIter.hasNext()) {

					Node node = (Node) nodeIter.next();
					
					//if node is blacklisted as either start or end node, do not consider that node
					if (rn.isBlacklistedAsStartNode(node.getID()) || rn.isBlacklistedAsEndNode(node.getID())) continue;
					
					SimpleFeature sfn = (SimpleFeature) node.getObject();
					Point point = (Point) sfn.getDefaultGeometry();

					double distanceToNode = centroid.distance(point);
					if (distanceToNode < minDistance) {
						minDistance = distanceToNode;
						nearestNodeID = node.getID();
					}
				}
				this.zoneToNearestNodeID.put(zoneID, nearestNodeID);
				this.zoneToNearestNodeDistance.put(zoneID, minDistance);
			} 
		} finally {
			//feature iterator is a live connection that must be closed
			iter.close();
		}
	}
	
	/**
	 * Maps zones to all the nodes of the network and distances, sorted by distance.
	 * @param zonesFeatureCollection Feature collection with the zones.
	 */
	private void mapZonesToNodesAndDistances(SimpleFeatureCollection zonesFeatureCollection) {

		this.zoneToSortedListOfNodeAndDistancePairs = new HashMap<String, List<Pair<Integer, Double>>>();

		//iterate through the zones and through the nodes
		SimpleFeatureIterator iter = zonesFeatureCollection.features();
		try {
			while (iter.hasNext()) {
				SimpleFeature sf = iter.next();
				MultiPolygon polygon = (MultiPolygon) sf.getDefaultGeometry();
				String zoneID = (String) sf.getAttribute("Zone_Code");
			
				List<Pair<Integer, Double>> list = new ArrayList<Pair<Integer, Double>>();
				this.zoneToSortedListOfNodeAndDistancePairs.put(zoneID, list);
								
				Point centroid = polygon.getCentroid();

				Iterator nodeIter = (Iterator) this.rn.getNetwork().getNodes().iterator();
				while (nodeIter.hasNext()) {

					Node node = (Node) nodeIter.next();
					
					//if node is blacklisted as either start or end node, do not consider that node
					if (rn.isBlacklistedAsStartNode(node.getID()) || rn.isBlacklistedAsEndNode(node.getID())) continue;
					
					SimpleFeature sfn = (SimpleFeature) node.getObject();
					Point point = (Point) sfn.getDefaultGeometry();
					double distanceToNode = centroid.distance(point);

					Pair<Integer, Double> pair = Pair.of(node.getID(), distanceToNode);
					list.add(pair);
				}
				
				//sort the list of nodes based on the distance
				Comparator<Pair<Integer, Double>> c = new Comparator<Pair<Integer, Double>>() {
				    public int compare(Pair<Integer, Double> p1, Pair<Integer, Double> p2) {
				    	Double distance1 = p1.getValue();
				    	Double distance2 = p2.getValue();
				    	return distance1.compareTo(distance2); //ascending
				    }
				};
				
				Collections.sort(list, c);
					
			} 
		} finally {
			//feature iterator is a live connection that must be closed
			iter.close();
		}
	}
	
	private void mapZonesToContainedNodes () {
		
		this.zoneToListOfContainedNodes = new HashMap<String, List<Integer>>();
		
		for (Integer nodeID: this.nodeToZoneInWhichLocated.keySet()) {
			
			String zoneCode = this.nodeToZoneInWhichLocated.get(nodeID);
			List listOfNodes = this.zoneToListOfContainedNodes.get(zoneCode);
			if (listOfNodes == null) {
				listOfNodes = new ArrayList<Integer>();
				this.zoneToListOfContainedNodes.put(zoneCode, listOfNodes);
			}
			listOfNodes.add(nodeID);
		}
	}
	
	/**
	 * Maps the nodes of the graph to the zone codes.
	 * @param zonesFeatureCollection Feature collection with the zones.
	 */
	private void mapNodesToZones(SimpleFeatureCollection zonesFeatureCollection) {

		this.nodeToZoneInWhichLocated = new HashMap<Integer, String>();

		//iterate through the zones and through the nodes
		SimpleFeatureIterator iter = zonesFeatureCollection.features();
		try {
			while (iter.hasNext()) {
				SimpleFeature sf = iter.next();
				MultiPolygon polygon = (MultiPolygon) sf.getDefaultGeometry();
				String zoneID = (String) sf.getAttribute("Zone_Code");

				Iterator nodeIter = (Iterator) this.rn.getNetwork().getNodes().iterator();
				while (nodeIter.hasNext()) {

					Node node = (Node) nodeIter.next();
					
					//if nodes already assigned to a zone, skip it
					if (this.nodeToZoneInWhichLocated.containsKey(node.getID())) continue;
					
					SimpleFeature sfn = (SimpleFeature) node.getObject();
					Point point = (Point) sfn.getDefaultGeometry();

					//if polygon of the zone contains the node, add it to the map
					if (polygon.contains(point)) this.nodeToZoneInWhichLocated.put(node.getID(), zoneID);
				}
			} 
		} finally {
			//feature iterator is a live connection that must be closed
			iter.close();
		}
	}
		
	/**
	 * Getter for tempro zone code to ID.
	 * @return Tempro zone code to tempro zone ID map.
	 */
	public HashMap<String, Integer> getZoneCodeToIDMap() {
		
		return this.temproCodeToID;
	}
	
	/**
	 * Getter for tempro zone ID to code.
	 * @return Tempro zone ID to tempro zone code.
	 */
	public HashMap<Integer, String> getZoneIDToCodeMap() {
		
		return this.temproIDToCode;
	}
	
	/**
	 * Getter for zone centroid to nearest node ID mapping.
	 * @return Zone to node map.
	 */
	public HashMap<String, Integer> getZoneToNearestNodeIDMap() {
		
		return this.zoneToNearestNodeID;
		
	}
	
	/**
	 * Getter for zone centroid to nearest node distance mapping (in meters).
	 * @return Zone to distance map.
	 */
	public HashMap<String, Double> getZoneToNearestNodeDistanceMap() {
		
		return this.zoneToNearestNodeDistance;
		
	}
	
	/**
	 * Getter for node to zone mapping (for each node gives the zone in which it is located).
	 * @return Node to zone map.
	 */
	public HashMap<Integer, String> getNodeToZoneMap() {
		
		return this.nodeToZoneInWhichLocated;
	}
	
	
	/**
	 * Getter for tempro zone to LAD zone mapping.
	 * @return Tempro zone to LAD zone map.
	 */
	public HashMap<String, String> getZoneToLADMap() {
		
		return this.zoneToLAD;
	}
	
	/**
	 * Getter for tempro zone to sorted node distances mapping (distances to ALL nodes in the network).
	 * @return Zone to sorted list of nodes and distances.
	 */
	public HashMap<String, List<Pair<Integer, Double>>> getZoneToSortedListOfNodeAndDistancePairs() {

		return this.zoneToSortedListOfNodeAndDistancePairs;
	}
	
	/**
	 * Getter for tempro zone to list of contained nodes mapping.
	 * @return Zone to list of contained nodes.
	 */
	public HashMap<String, List<Integer>> getZoneToListOfContaintedNodes() {
		
		return this.zoneToListOfContainedNodes;
	}
}
