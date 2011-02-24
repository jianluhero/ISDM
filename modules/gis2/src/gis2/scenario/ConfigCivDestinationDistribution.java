package gis2.scenario;

import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.StringTokenizer;

import rescuecore2.log.Logger;

/**
 * configure civ's destination distribution file
 * @author Bing Shi
 *
 */
public class ConfigCivDestinationDistribution extends RegionTool {
	private static final String file="destination";

	protected ConfigCivDestinationDistribution(ScenarioEditor editor) {
		super(editor, file);
	}
	
	@Override
	public String getName() {
		return "Config Des. Dis.";
	}

	@Override
	public void activate() {
		super.activate();
		load();
		if (xLength == 0) {// no distribution exist, create default
							// one
			xLength = X;
			yLength = Y;
		}
		dis = new double[xLength][yLength];
		regionOverlay = new RegionOverlay(editor, xLength, yLength, dis);
		editor.getViewer().addOverlay(regionOverlay);
		editor.updateOverlays();
		editor.setOperation(getName());
		count=0;		
	}
	
	/**
	 * load destination distribution from file
	 */
	public void load() {
		File f = editor.getBaseDir();
		File location = new File(f, file);
		if (location.exists()) {
			try {
				BufferedReader reader = new BufferedReader(new FileReader(
						location));
				String l = reader.readLine();
				if (l != null) {
					StringTokenizer st = new StringTokenizer(l, ",");
					xLength = Integer.valueOf(st.nextToken());
					yLength = Integer.valueOf(st.nextToken());
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	@Override
	protected void processClick(MouseEvent e) {
		// locationDis = new double[xLength][yLength];
		regionOverlay.setDis(dis);
		editor.repaint();
		int index[] = getRegionIndex(e.getPoint());

		Logger.debug(index[0] + " " + index[1]);

		Logger.debug("mouse " + e.getButton());

		if (index[0] != -1) {
			if (haveShapes(index)) {
				if (e.getButton() == 1)// increase probability
				{
					for (int i = 0; i < xLength; i++) {
						for (int j = 0; j < yLength; j++) {
							if (i == index[0] && j == index[1]) {
								double temp = dis[index[0]][index[1]] * count
										+ 1;
								dis[index[0]][index[1]] = temp / (count + 1);
							} else {
								dis[i][j] = dis[i][j] * count / (count + 1);
							}
						}
					}
					count++;
				} else if (e.getButton() == 3)// decrease probability
				{
					for (int i = 0; i < xLength; i++) {
						for (int j = 0; j < yLength; j++) {
							if (i == index[0] && j == index[1]) {
								if (count - 1 <= 0)
									dis[index[0]][index[1]] = 0;
								else {
									double temp = dis[index[0]][index[1]]
											* count - 1 > 0 ? dis[index[0]][index[1]]
											* count - 1
											: 0;
									dis[index[0]][index[1]] = temp
											/ (count - 1);
								}
							} else {
								if (count - 1 <= 0)
									dis[i][j] = 0;
								else
									dis[i][j] = (dis[i][j] * count > 0 ? dis[i][j]
											* count
											: 0)
											/ (count - 1);
							}
						}
					}
					count--;
				}
				regionOverlay.setDis(dis);
				editor.getViewer().repaint();
				save();
			}
		}
	}	
}
