
public class DnsRecord {
	private int timeToLive, rdLength, mxPreference;
	private int recLength;
	private QType queryType;
	private byte[] qClass;
	private boolean auth;
	private String name, resourceType;
	
	public DnsRecord(boolean authoritative) {
		this.auth = authoritative;
	}
	
	private String isAuth() {
		String result = this.auth ? "auth" : "nonauth";
		return result;
	}
	
	public void outputRecords() {
		switch(this.queryType) {
			case A:
				System.out.println("IP \t" + this.name + "\t" + this.timeToLive + "\t" + isAuth());
				break;
			case NS:
				System.out.println("NS \t" + this.name + "\t" + this.timeToLive + "\t" + isAuth());
				break;
			case MX:
				System.out.println("MX \t" + this.name + "\t" + this.mxPreference + "\t" + this.timeToLive + "\t" + isAuth());
				break;
			case CNAME:
				System.out.println("CNAME \t" + this.name + "\t" + this.timeToLive + "\t" + isAuth());
				break;
			default:
				break;
		}
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public int getRecordLength() {
		return recLength;
	}
	
	public void setRecordLength(int length) {
		this.recLength = length;
	}
	
	public byte[] getQClass() {
		return qClass;
	}
	
	public void setQClass(byte[] queryClass) {
		this.qClass = queryClass;
	}
	
	public int getTTL() {
		return timeToLive;
	}
	
	public void setTTL(int ttl) {
		this.timeToLive = ttl;
	}
	
	public int getRDLength() {
		return rdLength;
	}
	
	public void setRDLength(int rdLength) {
		this.rdLength = rdLength;
	}
	
	public QType getQueryType() {
		return queryType;
	}
	
	public void setQueryType(QType qType) {
		this.queryType = qType;
	}
	
	public String getResourceType() {
		return resourceType;
	}
	
	public void setResourceType(String resourceType) {
		this.resourceType = resourceType;
	}
	
	public int getMXPreference() {
		return mxPreference;
	}
	
	public void setMXPreference(int mxPref) {
		this.mxPreference = mxPref;
	}
}
