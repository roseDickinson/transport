/**
 * 
 */
package nismod.transport.decision;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Abstract class for policy interventions.
 * @author Milan Lovric
 *
 */
public abstract class Intervention {
	
	private final static Logger LOGGER = LogManager.getLogger(Intervention.class);
	
	protected Properties props;
	protected boolean installed;
	
	//enumerate possible intervention types
	public static enum InterventionType {
		RoadExpansion, RoadDevelopment, CongestionCharging, NewRailStation
	}
	
	protected Intervention (Properties props) {
		
		this.props = props;
		this.installed = false;
	}
	
	protected Intervention (String fileName) {
		
		Properties props = new Properties();
		InputStream input = null;
		try {
			input = new FileInputStream(fileName);
			// load properties file
			props.load(input);
		} catch (IOException e) {
			LOGGER.error(e);
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					LOGGER.error(e);
				}
			}
		}
		this.props = props;
		
		this.installed = false;
	}
	
	public abstract void install(Object o);
	
	public abstract void uninstall(Object o);
	
	/**
	 * @return The year in which intervention is installed.
	 */
	public int getStartYear() {
		
		int startYear = Integer.parseInt(props.getProperty("startYear"));
		return startYear;
	}
	
	/**
	 * @return The last year in which intervention still remains installed.
	 */
	public int getEndYear() {
		
		int endYear = Integer.parseInt(props.getProperty("endYear"));
		return endYear;
	}
	
	public boolean getState() {
		
		return installed;
	}
	
	/**
	 * @param key Name of the property
	 * @return Property
	 */
	public String getProperty(String key) {
		
		return props.getProperty(key);
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		
		return props.toString();
	}
}
