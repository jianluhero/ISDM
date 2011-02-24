package sample;

import static rescuecore2.misc.java.JavaTools.instantiate;

import rescuecore2.messages.control.KVTimestep;
import rescuecore2.view.ViewComponent;
import rescuecore2.view.ViewListener;
import rescuecore2.view.RenderedObject;
import rescuecore2.score.ScoreFunction;
import rescuecore2.Constants;
import rescuecore2.Timestep;

import rescuecore2.standard.view.AnimatedWorldModelViewer;

import java.awt.Dimension;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Color;
import java.awt.Font;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import java.util.List;
import java.util.StringTokenizer;
import java.text.NumberFormat;

import rescuecore2.standard.components.StandardViewer;

/**
 * A simple viewer.
 */
public class SampleViewer extends StandardViewer {
	private static final int DEFAULT_FONT_SIZE = 20;
	private static final int PRECISION = 3;

	private static final String FONT_SIZE_KEY = "viewer.font-size";
	private static final String MAXIMISE_KEY = "viewer.maximise";
	private static final String TEAM_NAME_KEY = "viewer.team-name";

	private ScoreFunction scoreFunction;
	private ViewComponent viewer;
	private JLabel timeLabel;
	private JLabel scoreLabel;
	private JLabel teamLabel;
	private NumberFormat format;

	private static final String INITIAL_TIME = "11:30";
	private static final int TIME_STEP_LENGTH = 5;

	@Override
	protected void postConnect() {
		super.postConnect();
		int fontSize = config.getIntValue(FONT_SIZE_KEY, DEFAULT_FONT_SIZE);
		String teamName = config.getValue(TEAM_NAME_KEY, "");
		scoreFunction = makeScoreFunction();
		format = NumberFormat.getInstance();
		format.setMaximumFractionDigits(PRECISION);
		JFrame frame = new JFrame("Viewer " + getViewerID() + " ("
				+ model.getAllEntities().size() + " entities)");
		viewer = new AnimatedWorldModelViewer();
		viewer.initialise(config);
		viewer.view(model);
		// CHECKSTYLE:OFF:MagicNumber
		viewer.setPreferredSize(new Dimension(500, 500));
		// CHECKSTYLE:ON:MagicNumber
		timeLabel = new JLabel("Time: Not started", JLabel.CENTER);
		teamLabel = new JLabel(teamName, JLabel.LEFT);
		scoreLabel = new JLabel("Score: Unknown", JLabel.RIGHT);
		timeLabel.setBackground(Color.WHITE);
		timeLabel.setOpaque(true);
		timeLabel.setFont(timeLabel.getFont().deriveFont(Font.PLAIN, fontSize));
		teamLabel.setBackground(Color.WHITE);
		teamLabel.setOpaque(true);
		teamLabel.setFont(timeLabel.getFont().deriveFont(Font.PLAIN, fontSize));
		scoreLabel.setBackground(Color.WHITE);
		scoreLabel.setOpaque(true);
		scoreLabel
				.setFont(timeLabel.getFont().deriveFont(Font.PLAIN, fontSize));
		frame.add(viewer, BorderLayout.CENTER);
		// CHECKSTYLE:OFF:MagicNumber
		JPanel labels = new JPanel(new GridLayout(1, 3));
		// CHECKSTYLE:ON:MagicNumber
		labels.add(teamLabel);
		labels.add(timeLabel);
		labels.add(scoreLabel);
		frame.add(labels, BorderLayout.NORTH);
		frame.pack();
		if (config.getBooleanValue(MAXIMISE_KEY, false)) {
			frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
		}
		frame.setVisible(true);

		viewer.addViewListener(new ViewListener() {
			@Override
			public void objectsClicked(ViewComponent view,
					List<RenderedObject> objects) {
				for (RenderedObject next : objects) {
					System.out.println(next.getObject());
				}
			}

			@Override
			public void objectsRollover(ViewComponent view,
					List<RenderedObject> objects) {
			}
		});
	}

	@Override
	protected void handleTimestep(final KVTimestep t) {
		super.handleTimestep(t);
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				timeLabel.setText("Time: " +  t.getTime());
				//timeLabel.setText("Time: " + convertToRealTimeFormat(t));
				scoreLabel.setText("Score: "
						+ format.format(scoreFunction.score(model,
								new Timestep(t.getTime()))));
				viewer.view(model, t.getCommands());
				viewer.repaint();
			}
		});
	}

	/**
	 * show the time information in an actual way bing
	 * 
	 * @param t
	 * @return
	 */
	protected String convertToRealTimeFormat(final KVTimestep t) {
		int step = t.getTime();
		StringTokenizer st = new StringTokenizer(INITIAL_TIME, ":");
		int initHour = Integer.valueOf(st.nextToken());
		int initMinute = Integer.valueOf(st.nextToken());
		int time = step * TIME_STEP_LENGTH + initMinute;
		int h = (time / 60 + initHour) % 24;
		int m = time % 60;
		String hour = "";
		String minute = "";
		if (h == 12) {
			hour += h;
			if (m / 10 == 0)
				minute = minute + 0 + m;
			else {
				minute += m;
			}
			return hour + ":" + minute + " pm";
		} else if (h == 0) {
			hour += (h + 12);
			if (m / 10 == 0)
				minute = minute + 0 + m;
			else {
				minute += m;
			}
			return hour + ":" + minute + " am";
		} else if (h < 12) {
			if (h / 10 == 0)
				hour = hour + 0 + h;
			else {
				hour += h;
			}
			if (m / 10 == 0)
				minute = minute + 0 + m;
			else {
				minute += m;
			}
			return hour + ":" + minute + " am";
		} else {
			hour += (h - 12);
			if (m / 10 == 0)
				minute = minute + 0 + m;
			else {
				minute += m;
			}
			return hour + ":" + minute + " pm";
		}
	}

	@Override
	public String toString() {
		return "Sample viewer";
	}

	private ScoreFunction makeScoreFunction() {
		String className = config.getValue(Constants.SCORE_FUNCTION_KEY);
		ScoreFunction result = instantiate(className, ScoreFunction.class);
		result.initialise(model, config);
		return result;
	}
}
