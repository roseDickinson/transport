package nismod.transport.utility;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Configuration file reader.
 * @author Milan Lovric
 */
public class ConfigReader {
	
	private final static Logger LOGGER = LogManager.getLogger(ConfigReader.class);

	public ConfigReader() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * Reads properties from the configuration file.
	 * @param configFile Path to the configuration file.
	 * @return Loaded properties.
	 */
	public static Properties getProperties(String configFile) {
		
	
		Properties props = new Properties();
		InputStream inputStream = null;
		try {
			inputStream = new FileInputStream(configFile);
			// load properties file
			props.load(inputStream);
		} catch (IOException e) {
			LOGGER.error("Unable to load config properties file!", e);
		} finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException e) {
					LOGGER.error(e);
				}
			}
		}
		
		return props;
		
	}
}
