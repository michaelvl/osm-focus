package dk.network42.osmfocus;

import android.util.SparseArray;

public class SparseMatrix<E> {
	private SparseArray<SparseArray<E>> rows = new SparseArray<SparseArray<E>>();
	
	public SparseMatrix() {
	}
	
	public void put(int x, int y, E elem) {
		SparseArray<E> a = rows.get(y);
		if (a==null) {
			a = new SparseArray<E>();
			rows.put(y, a);
		}
		a.put(x,  elem);
	}

	public E get(int x, int y) {
		SparseArray<E> a = rows.get(y);
		if (a==null)
			return null;
		return a.get(x);
	}

	public void delete(int x, int y) {
		SparseArray<E> a = rows.get(y);
		if (a!=null)
		a.delete(x);
	}
}
