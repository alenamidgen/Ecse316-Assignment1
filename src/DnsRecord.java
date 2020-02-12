
public class DnsRecord {
	private int timeToLive, rdLength, mxPreference;
	private int recLength;
	private QType queryType;
	private byte[] qClass;
	private boolean auth;
	private String name;
	
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
}
