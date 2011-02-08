package iamrescue.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class CombinatorialIterator<E> implements Iterator<List<E>>,
		Iterable<List<E>> {

	private int[] indices;
	private final Object[][] elements;

	public CombinatorialIterator(Collection<? extends E> set, int count) {
		this(repeat(set, count));
	}

	private static <E extends Object> Collection<? extends E>[] repeat(
			Collection<? extends E> set, int count) {
		Collection<? extends E>[] sets = new Collection[count];
		for (int i = 0; i < count; i++) {
			sets[i] = set;
		}

		return sets;
	}

	public CombinatorialIterator(Collection<? extends E>... sets) {
		elements = new Object[sets.length][];

		for (int i = 0; i < sets.length; i++)
			elements[i] = sets[i].toArray();

		indices = new int[sets.length];
	}

	public Iterator<List<E>> iterator() {
		return this;
	}

	public boolean hasNext() {
		return indices != null;
	}

	@SuppressWarnings("unchecked")
	private List<E> createFromIndices() {
		List<E> result = new ArrayList<E>(indices.length * 2);
		for (int i = 0; i < indices.length; i++) {
			result.add((E) elements[i][indices[i]]);
		}
		return result;
	}

	private void incrementIndices() {
		// if (indices[0] > elements[0].length) {
		// indices = null;
		// return;
		// }
		for (int i = indices.length - 1; i >= 0; i--) {
			if (indices[i] < elements[i].length - 1) {
				indices[i]++;
				break;
			} else {
				if (i == 0)
					indices = null;
				else
					indices[i] = 0;
			}
		}
	}

	public List<E> next() {
		if (indices == null) {
			throw new NoSuchElementException("End of iterator");
		}
		List<E> result = createFromIndices();
		incrementIndices();
		return result;
	}

	public void remove() {
		throw new UnsupportedOperationException("remove");
	}

	@SuppressWarnings("unchecked")
	public static void main(String[] args) {
		List<String> list = new ArrayList<String>();
		list.add("1");
		list.add("2");

		List<String> list2 = new ArrayList<String>();
		list2.add("a");
		list2.add("b");

		CombinatorialIterator<String> bla = new CombinatorialIterator<String>(
				list, 3);

		while (bla.hasNext()) {
			System.out.println(bla.next());
		}
	}
}
