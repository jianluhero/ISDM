/**
 * 
 */
package iamrescue.util;

import gis2.GMLWorldModelCreator;
import iamrescue.agent.SimulationTimer;
import iamrescue.belief.IAMWorldModel;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javolution.util.FastMap;
import javolution.util.FastSet;
import kernel.KernelException;
import rescuecore2.config.Config;
import rescuecore2.config.ConfigException;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.WorldModel;

/**
 * @author Sebastian
 * 
 */
public class ScenarioGenerator {

	private Map<String, Integer> agents = new FastMap<String, Integer>();
	private Map<String, Integer> buildings = new FastMap<String, Integer>();

	public void setPoliceForces(int number) {
		agents.put("rcr:policeforce", number);
	}

	public void setFireBrigades(int number) {
		agents.put("rcr:firebrigade", number);
	}

	public void setAmbulanceTeams(int number) {
		agents.put("rcr:ambulanceteam", number);
	}

	public void setCivilians(int number) {
		agents.put("rcr:civilian", number);
	}

	public void setFires(int number) {
		buildings.put("rcr:fire", number);
	}

	public void setRefuges(int number) {
		buildings.put("rcr:refuge", number);
	}

	public void setFireStations(int number) {
		buildings.put("rcr:firestation", number);
	}

	public void setAmbulanceCentres(int number) {
		buildings.put("rcr:ambulancecenter", number);
	}

	public void setPoliceOffices(int number) {
		buildings.put("rcr:policeoffice", number);
	}

	public void generateScenario(String outputFile, String mapFile) {
		File f = new File(outputFile);
		Set<Integer> used = new FastSet<Integer>();

		Config cfg;
		try {
			cfg = new Config(new File("boot/config"));
			cfg.setValue("gis.map.dir", mapFile);
			GMLWorldModelCreator creator = new GMLWorldModelCreator();
			WorldModel wm = creator.buildWorldModel(cfg);
			IAMWorldModel iwm = new IAMWorldModel(new SimulationTimer(), cfg);
			iwm.merge(wm.getAllEntities());
			FileWriter writer = new FileWriter(f);

			writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
			writer.write("<rcr:scenario "
					+ "xmlns:rcr=\"urn:roborescue:map:scenario\">\n");

			StandardEntity[] buildingsArray = iwm.getEntitiesOfType(
					StandardEntityURN.BUILDING).toArray(new StandardEntity[0]);
			StandardEntity[] roadsAndBuildings = iwm.getEntitiesOfType(
					StandardEntityURN.BUILDING, StandardEntityURN.ROAD)
					.toArray(new StandardEntity[0]);

			for (Entry<String, Integer> agent : agents.entrySet()) {
				String agentStr = agent.getKey();
				System.out.print("Adding " + agentStr);
				int number = agent.getValue();
				for (int i = 0; i < number; i++) {
					int index = (int) (Math.random() * roadsAndBuildings.length);

					StandardEntity se = roadsAndBuildings[index];
					int location = se.getID().getValue();

					writer.write("  <" + agentStr + " rcr:location=\""
							+ location + "\"/>\n");
					System.out.print(".");
				}
				System.out.println("");
			}

			for (Entry<String, Integer> building : buildings.entrySet()) {
				String buildingStr = building.getKey();
				System.out.print("Adding " + buildingStr);
				int number = building.getValue();
				for (int i = 0; i < number; i++) {
					int index;

					if (used.size() == buildingsArray.length) {
						System.out.println("Cannot add further buildings!");
						break;
					}

					do {
						index = (int) (Math.random() * buildingsArray.length);
					} while (used.contains(index));

					used.add(index);

					StandardEntity se = buildingsArray[index];

					int location = se.getID().getValue();

					writer.write("  <" + buildingStr + " rcr:location=\""
							+ location + "\"/>\n");
					System.out.print(".");
				}
				System.out.println("");
			}

			writer.write("</rcr:scenario>\n");
			writer.flush();
			writer.close();
			
			System.out.println("Done");

		} catch (ConfigException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (KernelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static void main(String[] args) {
		ScenarioGenerator generator = new ScenarioGenerator();
		generator.setCivilians(50);
		generator.setPoliceForces(10);
		generator.setAmbulanceTeams(10);
		generator.setFireBrigades(10);
		generator.setRefuges(5);

		generator.generateScenario(
				"maps/gml/OS/processed/highfield/dummyScenario.xml",
				"maps/gml/OS/processed/highfield");
	}
}
