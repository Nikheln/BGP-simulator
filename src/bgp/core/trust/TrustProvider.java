package bgp.core.trust;

// A simple interface to provide components with access to just trust values
public interface TrustProvider {
	public byte getTrustFor(int targetId);
}
