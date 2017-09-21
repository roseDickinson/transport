/**
 * 
 */
package nismod.transport.decision;

import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.geotools.graph.structure.DirectedEdge;
import org.geotools.graph.structure.DirectedGraph;
import org.geotools.graph.structure.DirectedNode;
import org.opengis.feature.simple.SimpleFeature;

import nismod.transport.demand.DemandModel;
import nismod.transport.network.road.RoadNetwork;

/**
 * Intervention that expands a road link with a number of lanes.
 * @author Milan Lovric
 *
 */
public class RoadExpansion extends Intervention {
	
	/** Constructor.
	 * @param props Properties of the road expansion intervention.
	 */
	public RoadExpansion (Properties props) {
		
		super(props);
	}
	
	/** Constructor.
	 * @param fileName File with the properties.
	 */
	public RoadExpansion (String fileName) {
		
		super(fileName);
	}

	/* (non-Javadoc)
	 * @see nismod.transport.decision.Intervention#install(java.lang.Object)
	 */
	@Override
	public void install(Object o) {
		// TODO Auto-generated method stub
		
		System.out.println("Implementing road expansion.");
		RoadNetwork rn = null;
		if (o instanceof RoadNetwork) {
			rn = (RoadNetwork)o;
		}
		else if (o instanceof DemandModel) {
			rn = ((DemandModel)o).getRoadNetwork();
		}
		else {
			System.err.println("RoadExpansion installation has received an unexpected type.");
			return;
		}
		int number = Integer.parseInt(this.props.getProperty("number"));
		Integer expandedEdgeID = this.getExpandedEdgeID(rn); 
		int numberOfLanes = rn.getNumberOfLanes().get(expandedEdgeID);
		rn.getNumberOfLanes().put(expandedEdgeID, numberOfLanes + number);
		
		this.installed = true;
	}

	/* (non-Javadoc)
	 * @see nismod.transport.decision.Intervention#uninstall(nismod.transport.demand.DemandModel)
	 */
	@Override
	public void uninstall(Object o) {
		// TODO Auto-generated method stub
		
		System.out.println("Removing road expansion.");
		RoadNetwork rn = null;
		if (o instanceof RoadNetwork) {
			rn = (RoadNetwork)o;
		}
		else if (o instanceof DemandModel) {
			rn = ((DemandModel)o).getRoadNetwork();
		}
		else {
			System.err.println("RoadExpansion installation has received an unexpected type.");
			return;
		}
		int number = Integer.parseInt(this.props.getProperty("number"));
		Integer expandedEdgeID = this.getExpandedEdgeID(rn); 
		int numberOfLanes = rn.getNumberOfLanes().get(expandedEdgeID);
		rn.getNumberOfLanes().put(expandedEdgeID, numberOfLanes - number);
		
		this.installed = false;
	}
	
	/**
	 * @param dm
	 * @return Edge ID which should be expanded.
	 */
	public Integer getExpandedEdgeID(RoadNetwork rn) {
		
		DirectedGraph network = rn.getNetwork();
		
		int fromNode = Integer.parseInt(this.props.getProperty("fromNode"));
		int toNode = Integer.parseInt(this.props.getProperty("toNode"));
		long CP = Long.parseLong(this.props.getProperty("CP"));
		
		Iterator iter = network.getNodes().iterator();
		DirectedNode nodeA=null, nodeB=null;
		while (iter.hasNext() && (nodeA == null || nodeB == null)) {
			DirectedNode node = (DirectedNode) iter.next();
			if (node.getID() == fromNode) nodeA = node;
			if (node.getID() == toNode) nodeB = node;
		}
		if (nodeA == null || nodeB == null) {
			System.err.println("Could not find a node where road expansion should be installed!");
			return null;
		}
		
		DirectedEdge edgeToExpand = null;
		List<DirectedEdge> listOfEdges = nodeA.getEdges(nodeB);
		for (DirectedEdge edge: listOfEdges) {
			SimpleFeature sf = (SimpleFeature) edge.getObject(); 
			long CPNumber = (long) sf.getAttribute("CP");
			if (CPNumber == CP) edgeToExpand = edge;
		}
		if (edgeToExpand == null) {
			System.err.println("Could not find the edge for which road expansion should be installed!");
			return null;
		}
		
		return edgeToExpand.getID();
	}
}