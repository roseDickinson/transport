package nismod.transport.visualisation;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.geotools.brewer.color.BrewerPalette;
import org.geotools.brewer.color.ColorBrewer;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.PieSectionLabelGenerator;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PiePlot3D;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;

import nismod.transport.decision.CongestionCharging;

/**
 * For visualising pie charts using JFreeChart.
 * @author Milan Lovric
  */
public class LineVisualiser extends JFrame {
	
	private final static Logger LOGGER = Logger.getLogger(LineVisualiser.class.getName());
	
	private static DefaultCategoryDataset dataset;
	private static String title;

	public LineVisualiser(DefaultCategoryDataset dataset, String title) throws IOException {

		LineVisualiser.dataset = dataset;
		LineVisualiser.title = title;
				
		initUI();
	}

	private void initUI() throws IOException {
		
		//DefaultPieDataset dataset = createDataset();
		JFreeChart chart = createChart(dataset, title);
		
		chart.setRenderingHints( new RenderingHints( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON ) );
		chart.getRenderingHints().put(JFreeChart.KEY_SUPPRESS_SHADOW_GENERATION, Boolean.TRUE);
		chart.setAntiAlias(true);
		
		ChartPanel chartPanel = new ChartPanel(chart);

		chartPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
		chartPanel.setBackground(Color.white);
		this.add(chartPanel);
		
		//chartPanel.setPreferredSize( new java.awt.Dimension( 560 , 367 ) );
	    //setContentPane( chartPanel );

		this.pack();
		this.setTitle("NISMOD v2");
		this.setLocationRelativeTo(null);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		//Image myImage = Toolkit.getDefaultToolkit().getImage(getClass().getResource("NISMOD-LP.jpg"));
		ImageIcon icon = new ImageIcon("NISMOD-LP.jpg");
		this.setIconImage(icon.getImage());

		//this.setSize(600, 400);
		//ChartUtils.saveChartAsPNG(new File("pie_chart.png"), chart, 600, 400);
	}
	
	public void saveToPNG(String fileName) throws IOException {
		
		BufferedImage img = new BufferedImage(this.getWidth(), this.getHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics2D g2d = img.createGraphics();

		//this.getContentPane().paint(g2d);
		//this.printComponents(g2d);
		this.print(g2d);
		//this.printAll(g2d);
		//this.paint(g2d);
			
		g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		ImageIO.write(img, "png", new File(fileName));
			
		g2d.dispose();
	}

	private JFreeChart createChart(DefaultCategoryDataset dataset, String title) {

		JFreeChart pieChart;
		
		pieChart = ChartFactory.createLineChart(title, "iterations", "RMSN", dataset, PlotOrientation.VERTICAL, false, false, false);

		return pieChart;
	}

	public static void main(String[] args) {

		SwingUtilities.invokeLater(() -> {
			LineVisualiser pc;
			try {
				pc = new LineVisualiser(dataset, title);
				pc.setVisible(true);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		});
	}
}

