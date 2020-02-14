import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

public class DnsResponse {
	private byte[] responsePacket;
	private byte[] responseID;
	private boolean qr, aa, tc, rd, ra;
	private boolean recordDoesNotExist = false;
	private int rCode, qdCount, anCount, nsCount, arCount;
	private DnsRecord[] answerSection;
	private DnsRecord[] additionalSection;
	private QType queryType;
	
	public DnsResponse(byte[] response, int packetSize, QType queryType) {
		this.responsePacket = response;
		this.queryType = queryType;
		
		this.validateQuestion();
		this.parseHeader();
		
		this.answerSection = new DnsRecord[anCount];
		int offset = packetSize;
		
		for(int i = 0; i < anCount; i++) {
			answerSection[i] = this.parseAnswer(offset);
			offset += answerSection[i].getRecordLength();
		}
		
		//Only get NSCOUNT length
		for(int j = 0; j < nsCount; j++) {
			offset += this.parseAnswer(offset).getRecordLength();
		}
		
		this.additionalSection = new DnsRecord[arCount];
		for(int k = 0; k < arCount; k++) {
			additionalSection[k] = this.parseAnswer(offset);
			offset += additionalSection[k].getRecordLength();
		}
		
		readRCode(); //Check RCODE for errors
		
		if(!this.qr) { //Verify that received message is a response
			throw new RuntimeException("ERROR \t The message received is not a response.");
		}
		
	}
	
	private void validateQuestion() {
		int index = 12; //DNS_PACKET_HEADER_SIZE
		
		while(this.responsePacket[index] != 0x00) { //Go through QNAME until 0 byte is reached
			index++;
		}
		
		byte[] qType = {this.responsePacket[index+1], this.responsePacket[index+2]};
		
		QType responseType; //Find response query type
		responseType = getQTYPEFromBytes(qType);
		
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
		
		//Get value of QDCOUNT
		byte[] QDCOUNT = {this.responsePacket[4], this.responsePacket[5]};
		ByteBuffer wrap = ByteBuffer.wrap(QDCOUNT);
		this.qdCount = wrap.getShort(); //QDCOUNT is only two bytes long
		
		//Get value of ANCOUNT
		byte[] ANCOUNT = {this.responsePacket[6], this.responsePacket[7]};
		wrap = ByteBuffer.wrap(ANCOUNT);
		this.anCount = wrap.getShort();
		
		//Get value of NSCOUNT
		byte[] NSCOUNT = {this.responsePacket[8], this.responsePacket[9]};
		wrap = ByteBuffer.wrap(NSCOUNT);
		this.nsCount = wrap.getShort();
		
		//Get value of ARCOUNT
		byte[] ARCOUNT = {this.responsePacket[10], this.responsePacket[11]};
		wrap = ByteBuffer.wrap(ARCOUNT);
		this.arCount = wrap.getShort();
	}
	
	private DnsRecord parseAnswer(int index) {
		DnsRecord record = new DnsRecord(this.aa);
		String domainName = "";
		int byteCount = index;
		
		RData domainResult = getDomainNameFromOffset(index);
		byteCount += domainResult.getByteLength();
		domainName = domainResult.getDomainName();
		
		//Store NAME
		record.setName(domainName);
		
		//Store TYPE
		byte[] answerType = {responsePacket[byteCount], responsePacket[byteCount+1]};
		record.setQueryType(getQTYPEFromBytes(answerType));
		byteCount += 2;
		
		//Store and check CLASS value
		byte[] answerClass = {responsePacket[byteCount], responsePacket[byteCount+1]};
		if(answerClass[0] != 0x00 && answerClass[1] != 0x01) {
			throw new RuntimeException("ERROR \t The response class does not equal IN (Internet Address).");
		}
		record.setQClass(answerClass);
		byteCount += 2;
		
		//Store TTL
		byte[] answerTTL = {responsePacket[byteCount], responsePacket[byteCount+1], responsePacket[byteCount+2], responsePacket[byteCount+3]};
		ByteBuffer wrappedBuffer = ByteBuffer.wrap(answerTTL);
		record.setTTL(wrappedBuffer.getInt());
		byteCount += 4;
		
		//Store RDLENGTH
		byte[] answerRDLength = {responsePacket[byteCount], responsePacket[byteCount+1]};
		wrappedBuffer = ByteBuffer.wrap(answerRDLength);
		int rdLength = wrappedBuffer.getShort();
		record.setRDLength(rdLength);
		byteCount += 2;
		
		//Determine RDATA value and perform appropriate operation
		switch(record.getQueryType()) {
			case A:
				record.setResourceType(parseATypeResource(byteCount));
				break;
			case NS:
				record.setResourceType(parseNSTypeResource(byteCount));
				break;
			case MX:
				record.setResourceType(parseMXTypeResource(byteCount, record));
				break;
			case CNAME:
				record.setResourceType(parseCNAMETypeResource(byteCount));
				break;
			case NONE:
				break;
		}
		
		record.setRecordLength(byteCount+rdLength-index);
		return record;
	}
	
	private RData getDomainNameFromOffset(int index) {
		RData sequence = new RData();
		int labelSize = responsePacket[index]; //Start after the question section
		String domainName = ""; //Store full domain name
		boolean label = true; //Check if we reach the end of the label
		int counter = 0; //Calculate total bytes
		
		while(labelSize != 0) {
			if(!label) {
				domainName += "."; //End of label reached
			}
			if((labelSize & 0xc0) == (int) 0xc0) { //Check if first two bits are 1
				byte[] offset = {(byte) (responsePacket[index] & 0x3f), responsePacket[index+1]}; //Store offset without the 1s
				ByteBuffer wrapped = ByteBuffer.wrap(offset);
				domainName += getDomainNameFromOffset(wrapped.getShort()).getDomainName(); //Use offset to find domain name
				index += 2;
				counter += 2;
				labelSize = 0;
			} else { //When a pointer is not encountered
				domainName += getLabelFromIndex(index);
				index += labelSize + 1;
				counter += labelSize + 1;
				labelSize = responsePacket[index];
			}
			
			label = false;
		}
		
		sequence.setDomainName(domainName);
		sequence.setByteLength(counter);
		return sequence;
	}
	
	private String getLabelFromIndex(int index) {
		String label = "";
		int labelSize = responsePacket[index]; //Get the label length
		
		for(int i = 0; i < labelSize; i++) { //Grab each character of the label
			label += (char) responsePacket[index+i+1];
		}
		
		return label;
	}
	
	private QType getQTYPEFromBytes(byte[] input) {
		if(input[0] == 0x00) {
			switch(input[1]) {
				case 0x01:
					return QType.A;
			case 0x02:
					return QType.NS;
			case 0x05:
					return QType.CNAME;
			case 0x0f:
					return QType.MX;
			default:
					return QType.NONE;
			}
		} else {
			return QType.NONE;
		}
	}
	
	private String parseATypeResource(int byteCount) {
		String ipAddress = "";
		byte[] ipByteAddress = {responsePacket[byteCount], responsePacket[byteCount+1], responsePacket[byteCount+2], responsePacket[byteCount+3]};
		
		try {
			InetAddress inetAddress = InetAddress.getByAddress(ipByteAddress);
			ipAddress = inetAddress.toString().substring(1);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		
		return ipAddress;
	}
	
	private String parseNSTypeResource(int byteCount) {
		RData result = getDomainNameFromOffset(byteCount);
		return result.getDomainName();
	}
	
	private String parseMXTypeResource(int byteCount, DnsRecord record) {
		byte[] mxPreference = {responsePacket[byteCount], responsePacket[byteCount+1]};
		ByteBuffer buffer = ByteBuffer.wrap(mxPreference);
		record.setMXPreference(buffer.getShort());
		
		return getDomainNameFromOffset(byteCount+2).getDomainName();
	}
	
	private String parseCNAMETypeResource(int byteCount) {
		RData result = getDomainNameFromOffset(byteCount);
		return result.getDomainName();
	}
	
	private void readRCode() {
		switch(this.rCode) {
			case 0:
				break; //No error condition
			case 1:
				throw new RuntimeException("ERROR \t Name server could not interpret query.");
			case 2:
				throw new RuntimeException("ERROR \t A problem with the name server prevented it from processing the query.");
			case 3:
				recordDoesNotExist = true;
				break;
			case 4:
				throw new RuntimeException("ERROR \t Name server does not support the requested query.");
			case 5:
				throw new RuntimeException("ERROR \t Name server refused to perform query for policy reasons.");
		}
	}
	
	public void printResponsePacket() {
		System.out.println(); //Start on a new line
		
		if(anCount <= 0 || recordDoesNotExist) {
			System.out.println("NOTFOUND");
			return;
		}
		
		System.out.println("***Answer Section (" + this.anCount + " records)***");
		for(DnsRecord record : answerSection) {
			record.outputRecords();
		}
		
		System.out.println();
		
		if(arCount > 0) {
			System.out.println("***Additional Section (" + this.arCount + " records)***");
			for(DnsRecord record : additionalSection) {
				record.outputRecords();
			}
		}
	}

}
