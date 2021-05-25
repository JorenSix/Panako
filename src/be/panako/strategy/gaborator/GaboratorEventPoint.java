package be.panako.strategy.gaborator;

public class GaboratorEventPoint {
	
	/**
	 * The time expressed using an analysis frame index.
	 */
	public final int t;
	
	/**
	 * The frequency expressed using the bin number in the constant Q transform.
	 */
	public final int f;
	
	/**
	 * The energy value of the element.
	 */
	public final float m;
	
	/**
	 * Create a new event point with a time, frequency and energy and contrast..
	 * @param t The time expressed using an analysis frame index.
	 * @param f The frequency expressed using the bin number in the constant Q transform.
	 * @param m The energy value of the element.
	 */
	public GaboratorEventPoint(int t,int f,float m){
		this.t = t;
		this.f = f;
		this.m = m;
	}
}
