
public class DnsResponse {
	private byte[] responsePacket;
	private byte[] responseID;
	private boolean qr, aa, tc, rd, ra;
	private int rCode, qdCount, anCount, nsCount, arCount;
	private DnsRecord[] answerSection;
	private DnsRecord[] additionalSection;
	private QType queryType;
	
	public DnsResponse(byte[] response, int packetSize, QType queryType) {
		this.responsePacket = response;
		this.queryType = queryType;
		
		this.validateQuestion();
		
	}
	
	private void validateQuestion() {
		int index = 12; //DNS_PACKET_HEADER_SIZE
		
		while(this.responsePacket[index] != 0x00) { //Go through QNAME until 0 byte is reached
			index++;
		}
		
		byte[] qType = {this.responsePacket[index+1], this.responsePacket[index+2]};
		
		QType responseType; //Find response query type
		if(qType[0] == 0x00) {
			switch(qType[1]) {
				case 0x01:
					responseType = QType.A;
					break;
				case 0x02:
					responseType = QType.NS;
					break;
				case 0x05:
					responseType = QType.CNAME;
					break;
				case 0x0f:
					responseType = QType.MX;
					break;
				default:
					responseType = QType.NONE;
					break;
			}
		} else {
			responseType = QType.NONE;
		}
		
		if (responseType != this.queryType) {
			throw new RuntimeException("ERROR \t The response does not match the query.");
		}
	}
	
	private void parseHeader() {
		//Store random header ID
		byte[] ID = {this.responsePacket[0], this.responsePacket[1]};
		this.responseID = ID;
		
		//Check if 1-bit fields are 0 or 1
		this.qr = ((this.responsePacket[2] >> 7) & 1) == 1;
		this.aa = ((this.responsePacket[2] >> 2) & 1) == 1;
		this.tc = ((this.responsePacket[2] >> 1) & 1) == 1;
		this.rd = ((this.responsePacket[2] >> 0) & 1) == 1;
		this.ra = ((this.responsePacket[3] >> 7) & 1) == 1;
		
		//Store RCODE to check for errors
		this.rCode = this.responsePacket[3] & 0x0f;
	}

}
