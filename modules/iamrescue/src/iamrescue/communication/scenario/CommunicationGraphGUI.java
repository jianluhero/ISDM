package iamrescue.communication.scenario;

import iamrescue.communication.messages.MessageChannel;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Paint;

import javax.swing.JFrame;

import org.apache.commons.collections15.Transformer;

import edu.uci.ics.jung.algorithms.layout.FRLayout;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.visualization.VisualizationViewer;

public class CommunicationGraphGUI {

	public static void show(final CommunicationGraph communicationGraph) {
		JFrame jf = new JFrame();
		DirectedSparseGraph<CommunicationGraphVertex, CommunicationGraphEdge> graph = communicationGraph
				.getGraph();

		FRLayout<CommunicationGraphVertex, CommunicationGraphEdge> layout = new FRLayout<CommunicationGraphVertex, CommunicationGraphEdge>(
				graph);

		VisualizationViewer<CommunicationGraphVertex, CommunicationGraphEdge> vv = new VisualizationViewer<CommunicationGraphVertex, CommunicationGraphEdge>(
				layout);

		vv.getRenderContext().setVertexFillPaintTransformer(
				new Transformer<CommunicationGraphVertex, Paint>() {

					@Override
					public Paint transform(CommunicationGraphVertex vertex) {
						if (vertex instanceof MessageChannelVertex) {
							return Color.blue;
						}

						if (vertex instanceof AgentSendingVertex) {
							return Color.green;
						}
						if (vertex instanceof SourceSinkVertex) {
							return Color.black;
						}

						return Color.red;
					}
				});

		vv.getRenderContext().setVertexLabelTransformer(
				new Transformer<CommunicationGraphVertex, String>() {
					@Override
					public String transform(CommunicationGraphVertex vertex) {
						if (vertex instanceof MessageChannelVertex) {
							MessageChannelVertex v = (MessageChannelVertex) vertex;
							MessageChannel channel = v.getChannel();
							int allocatedBandWidth = communicationGraph
									.getAllocatedBandWidth(channel);

							return "CH " + channel.getChannelNumber() + " "
									+ allocatedBandWidth + "/"
									+ channel.getBandwidth();
						}

						if (vertex instanceof AgentVertex) {
							AgentVertex v = (AgentVertex) vertex;
							return v.getType().name();
						}

						return "";
					}
				});

		vv.getRenderContext().setEdgeLabelTransformer(
				new Transformer<CommunicationGraphEdge, String>() {
					@Override
					public String transform(CommunicationGraphEdge assignment) {
						return assignment.getFlow() + "";
					}
				});

		vv.setPreferredSize(new Dimension(800, 800));

		jf.setPreferredSize(new Dimension(800, 800));
		jf.getContentPane().add(vv);
		jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		jf.pack();
		jf.setVisible(true);
	}
}
