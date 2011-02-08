package iamrescue.routing.util;

import java.util.Map;
import java.util.Set;

import javolution.util.FastMap;

public class LeastRecentlyUsedMap<K, V> {

	private Map<K, LinkedValue<K, V>> map;

	private LinkedValue<K, V> mostRecentlyAccessed;
	private LinkedValue<K, V> leastRecentlyAccessed;

	private int maxCapacity;

	/**
	 * Creates a new cache with the given maximum capacity and initial capacity.
	 * 
	 * @param initialCapacity
	 *            Initial capacity
	 * @param maxCapacity
	 *            Maximum capacity
	 */
	public LeastRecentlyUsedMap(int initialCapacity, int maxCapacity) {
		map = new FastMap<K, LinkedValue<K, V>>(initialCapacity);
		setMaximumCapacity(maxCapacity);
	}

	/**
	 * Creates a new cache with the given maximum capacity.
	 * 
	 * @param maxCapacity
	 *            Maximum capacity
	 */
	public LeastRecentlyUsedMap(int maxCapacity) {
		this(maxCapacity, maxCapacity);

	}

	/**
	 * Returns the current key set.
	 * 
	 * @return The key set.
	 */
	public Set<K> keySet() {
		return map.keySet();
	}

	/**
	 * The maximum capacity.
	 * 
	 * @return The max capacity.
	 */
	public int getMaxCapacity() {
		return maxCapacity;
	}

	/**
	 * 
	 * @return Size of the cache.
	 */
	public int size() {
		return map.size();
	}

	/**
	 * Adds a new element. If max Capacity is exceeded, the least recently
	 * accessed entries are discarded.
	 * 
	 * @param key
	 *            The key
	 * @param value
	 *            The value
	 */
	public void put(K key, V value) {
		// Safely remove any previous ones.
		LinkedValue<K, V> old = map.remove(key);
		if (old != null) {
			removeFromChain(old);
		}
		LinkedValue<K, V> linkedValue = new LinkedValue<K, V>(key, value);
		map.put(key, linkedValue);
		makeMostRecent(linkedValue);
		checkSize();
	}

	/**
	 * Makes given value the most recent entry, fixing all links.
	 * 
	 * @param value
	 *            The new most recent entry
	 */
	private void makeMostRecent(LinkedValue<K, V> value) {
		if (mostRecentlyAccessed == null) {
			mostRecentlyAccessed = value;
			assert (leastRecentlyAccessed == null);
			leastRecentlyAccessed = value;

		}
		if (!mostRecentlyAccessed.equals(value)) {
			value.previousValue = mostRecentlyAccessed;
			mostRecentlyAccessed.nextValue = value;
			mostRecentlyAccessed = value;
		}
	}

	/**
	 * Removes links to/from neighbours of this value.
	 * 
	 * @param toRemove
	 *            The value to remove.
	 */
	private void removeFromChain(LinkedValue<K, V> toRemove) {

		if (toRemove.equals(mostRecentlyAccessed)) {
			mostRecentlyAccessed = toRemove.previousValue;
		}

		if (toRemove.equals(leastRecentlyAccessed)) {
			leastRecentlyAccessed = toRemove.nextValue;
		}

		LinkedValue<K, V> previous = toRemove.previousValue;
		LinkedValue<K, V> next = toRemove.nextValue;

		if (previous != null) {
			previous.nextValue = next;
		}

		if (next != null) {
			next.previousValue = previous;
		}

		toRemove.nextValue = null;
		toRemove.previousValue = null;
	}

	public V get(K key) {
		LinkedValue<K, V> linkedValue = map.get(key);
		V value;
		if (linkedValue == null) {
			value = null;
		} else {
			value = linkedValue.value;
			if (!mostRecentlyAccessed.equals(linkedValue)) {
				removeFromChain(linkedValue);
				makeMostRecent(linkedValue);
			}
		}

		return value;
	}

	/**
	 * Checks the size of the map and removes the least recently accessed
	 * elements when necessary.
	 */
	private void checkSize() {
		while (map.size() > maxCapacity) {
			K leastRecentlyKey = leastRecentlyAccessed.key;
			leastRecentlyAccessed.nextValue.previousValue = null;
			map.remove(leastRecentlyKey);
			leastRecentlyAccessed = leastRecentlyAccessed.nextValue;
		}
	}

	/**
	 * Sets the maximum capacity for this cache. If more elements than this are
	 * added, the least recently accessed ones are discarded.
	 * 
	 * @param maxCapacity
	 *            The maximum capacity.
	 */
	public void setMaximumCapacity(int maxCapacity) {
		if (maxCapacity <= 0) {
			throw new IllegalArgumentException("Capacity must be at least 1");
		}
		this.maxCapacity = maxCapacity;
		checkSize();
	}

	/**
	 * Removes the entry with the given key.
	 * 
	 * @param key
	 *            The key to remove.
	 * @return The value associated with this key (or null if none).
	 */
	public V remove(K key) {
		LinkedValue<K, V> value = map.remove(key);
		if (value != null) {
			LinkedValue<K, V> next = value.nextValue;
			LinkedValue<K, V> previous = value.previousValue;
			if (next != null) {
				next.previousValue = previous;
			} else {
				assert value.equals(mostRecentlyAccessed);
				mostRecentlyAccessed = previous;
			}
			if (previous != null) {
				previous.nextValue = next;
			} else {
				assert value.equals(leastRecentlyAccessed);
				leastRecentlyAccessed = next;
			}
		} else {
			return null;
		}
		return value.value;

	}

	private static class LinkedValue<K, V> {
		private V value;
		private K key;
		private LinkedValue<K, V> nextValue;
		private LinkedValue<K, V> previousValue;

		public LinkedValue(K key, V value) {
			this.key = key;
			this.value = value;
		}
	}

	/**
	 * Clears the map
	 */
	public void clear() {
		map.clear();
		mostRecentlyAccessed = null;
		leastRecentlyAccessed = null;
	}

}
