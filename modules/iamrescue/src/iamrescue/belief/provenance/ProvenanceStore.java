/**
 * 
 */
package iamrescue.belief.provenance;

import iamrescue.agent.ISimulationTimer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import javolution.util.FastMap;
import rescuecore2.worldmodel.EntityID;

/**
 * @author Sebastian
 * 
 */
public class ProvenanceStore implements IProvenanceStore {

	private Map<EntityID, Map<String, IProvenanceInformation>> provenance = new FastMap<EntityID, Map<String, IProvenanceInformation>>();

	public ProvenanceStore() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * belief.provenance.IProvenanceStore#getProvenance(rescuecore2.worldmodel
	 * .EntityID, rescuecore2.standard.entities.StandardPropertyURN)
	 */
	@Override
	public IProvenanceInformation getProvenance(EntityID id, String propertyURN) {
		Map<String, IProvenanceInformation> map = provenance.get(id);

		if (map != null)
			return map.get(propertyURN);
		else
			return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * belief.provenance.IProvenanceStore#storeProvenance(rescuecore2.worldmodel
	 * .EntityID, belief.provenance.ProvenanceLogEntry)
	 */
	@Override
	public void storeProvenance(EntityID id, ProvenanceLogEntry entry) {
		Map<String, IProvenanceInformation> map = provenance.get(id);

		if (map == null) {
			map = new FastMap<String, IProvenanceInformation>();
			provenance.put(id, map);
		}

		String propertyURN = entry.getProperty().getURN();
		IProvenanceInformation provenanceInformation = map.get(propertyURN);
		if (provenanceInformation == null) {
			provenanceInformation = new FastProvenanceInformation();
			map.put(propertyURN, provenanceInformation);
		}

		provenanceInformation.addEntry(entry);
	}

	@Override
	public Collection<String> getKnownProperties(EntityID id) {
		Map<String, IProvenanceInformation> map = provenance.get(id);
		if (map == null) {
			return new ArrayList<String>();
		} else {
			return map.keySet();
		}
	}

}
