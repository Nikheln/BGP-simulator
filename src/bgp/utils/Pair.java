package bgp.utils;

/**
 * A class for storing 2-tuples.
 * 
 * @author Niko
 *
 * @param <T>
 * @param <E>
 */
public class Pair<T,E> {
	private final T left;
	private final E right;
	
	public Pair(T left, E right) {
		this.left = left;
		this.right = right;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Pair) {
			Pair<?,?> other = (Pair<?,?>) obj;
			return this.left.equals(other.left) && this.right.equals(other.right);
		}
		return false;
	}
	
	public T getLeft() {
		return left;
	}
	
	public E getRight() {
		return right;
	}
}