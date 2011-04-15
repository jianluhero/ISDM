package maps.gml.formats;

public class CoordinateProjection {
	
	private int index;
	private int eastIndex;
	private int northIndex;
	private double se;
	private double sn;
	private double sg;
	public int getIndex() {
		return index;
	}
	public void setIndex(int index) {
		this.index = index;
	}
	public int getEastIndex() {
		return eastIndex;
	}
	public void setEastIndex(int eastIndex) {
		this.eastIndex = eastIndex;
	}
	public int getNorthIndex() {
		return northIndex;
	}
	public void setNorthIndex(int northIndex) {
		this.northIndex = northIndex;
	}
	public double getSe() {
		return se;
	}
	public void setSe(double se) {
		this.se = se;
	}
	public double getSn() {
		return sn;
	}
	public void setSn(double sn) {
		this.sn = sn;
	}
	public double getSg() {
		return sg;
	}
	public void setSg(double sg) {
		this.sg = sg;
	}
}
