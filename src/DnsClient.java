import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;

public class DnsClient 
{
	private static int timeout = 5000; //default value in milliseconds
	private static int maxRetries = 3; //default value for retransmitting unanswered query
	private static int port = 53; //default UDP port number
	private static String name = ""; //domain name to query for
	private static byte[] server = new byte[4]; //IPv4 address of the DNS server in a.b.c.d format
	private static QType queryType = QType.A;
	private static int DNS_PACKET_HEADER_SIZE = 12; //Size of DNS header in bytes
	private static int DNS_PACKET_QUESTION_SIZE = 4; //Size of DNS question in bytes (excluding QNAME)
	
	public static void main (String args[]) throws Exception
	{
		
		try {
			readInput(args);
		} catch (Exception e) {
			throw new IllegalArgumentException("ERROR \t Incorrect input syntax: Please input the correct arguments and try again.");
		}
		if (server == null || name == null)
		{
			System.out.println("ERROR \t Incorrect input syntax: IP address and domain name has to be provided.");
			return;
		}
		
		System.out.println("DnsClient sending request for " + name);
		System.out.println("Server: " + server);
		System.out.println("Request type: " + queryType);
		
		transmitQuery(1); //Create request packet and receive response packet
		
	}
			
	private static void readInput(String input[]) {
		List<String> arguments = Arrays.asList(input);
		ListIterator<String> it = arguments.listIterator();
		
		while(it.hasNext()) {
			String arg = it.next();
			switch(arg) {
				case "-t":
					timeout = Integer.parseInt(it.next()) * 1000;
					break;
				case "-r":
					maxRetries = Integer.parseInt(it.next());
					break;
				case "-p":
					port = Integer.parseInt(it.next());
					break;
				case "-mx":
					queryType = QType.MX;
					break;
				case "-ns":
					queryType = QType.NS;
					break;
				default:
					if (arg.contains("@")) {
						String ipAddress = arg.substring(1);
						String[] ipNumbers = ipAddress.split("\\.");
						
						for (int j = 0; j < ipNumbers.length; j++) {
							int octet = Integer.parseInt(ipNumbers[j]);
							if (octet < 0 || octet > 255) {
								throw new NumberFormatException("ERROR \t Incorrect input syntax: IPv4 numbers should be between 0 and 255.");
							}
							
							server[j] = (byte) octet;
							System.out.println(server[j]);
						}
						name = it.next();
					}
					break;
			}
		}
	}
	
	private static int getQNameSize() {
		int byteSize = 0;
		String[] labels = name.split("\\.");
		for (int i = 0; i < labels.length; i++) {
			byteSize = labels[i].length() + 1; //Extra byte included for length of label
		}
		
		return byteSize + 1; //Extra byte with value 0
	}
	
	private static byte[] constructHeader() {
		ByteBuffer header = ByteBuffer.allocate(DNS_PACKET_HEADER_SIZE); //Write values for header
		byte[] dnsID = new byte[2];
		Random rand = new Random();
		rand.nextBytes(dnsID); //Creating random 16-bit ID
		
		header.put(dnsID); //Add bytes for ID, 2nd line and QDCOUNT
		header.put((byte) 0x01);
		header.put((byte) 0x00);
		header.put((byte) 0x00);
		header.put((byte) 0x01);
		
		//Last three lines are all zero so they can be ignored
		return header.array();
	}
	
	private static byte[] constructQuestion(int qName) {
		ByteBuffer question = ByteBuffer.allocate(qName + DNS_PACKET_QUESTION_SIZE); //Write values for question
		String[] domain = name.split("\\.");
		
		for (int j = 0; j < domain.length; j++) {
			question.put((byte) domain[j].length()); //Byte for number of characters in a label
			for (int k = 0; k < domain[j].length(); k++) {
				question.put((byte) ((int) domain[j].charAt(k)));
			}
		}
		question.put((byte) 0x00); //Extra byte with value 0
		
		question.put((byte) getQueryCode(queryType)); //Add byte for QTYPE line
		question.put((byte) 0x00);
		question.put((byte) 0x0001); //Add byte for QCLASS line
		
		return question.array();
	}
	
	private static int getQueryCode(QType type) {
		switch(type) {
		case NS:
			return 0x0002;
		case MX:
			return 0x000f;
		default:
			return 0x0001;
		}
	}
	
	private static void transmitQuery(int retries) {
		if (retries > maxRetries) {
			System.out.println("ERROR \t Maximum number of retries " + maxRetries + " exceeded.");
			return;
		}
		
		try {
			DatagramSocket clientSocket = new DatagramSocket(); //creates the socket
			clientSocket.setSoTimeout(timeout);
			InetAddress ipAddress = InetAddress.getByAddress(server);
			
			int qNameSize = getQNameSize();
			ByteBuffer dnsRequest = ByteBuffer.allocate(DNS_PACKET_HEADER_SIZE + DNS_PACKET_QUESTION_SIZE + qNameSize);
			dnsRequest.put(constructHeader());
			dnsRequest.put(constructQuestion(qNameSize));
			
			byte[] sendData = dnsRequest.array();
			byte[] receiveData = new byte[1024];
			
			//creates datagram with data to send, length, ip address and port
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, ipAddress, port);
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			
			long start = System.currentTimeMillis();
			clientSocket.send(sendPacket); //send datagram to server
			clientSocket.receive(receivePacket); //read datagram from server
			long end = System.currentTimeMillis();
			
			clientSocket.close();
			System.out.println("Response received after " + (end-start)/1000 + " seconds (" + (retries-1) + " retries).");
			
			DnsResponse response = new DnsResponse(receivePacket.getData(), sendData.length, queryType);
			response.printResponsePacket();
			
			
		} catch (SocketException e) {
			System.out.println("ERROR \t The socket could not be created or accessed.");
		} catch (UnknownHostException e) {
			System.out.println("ERROR \t IP address cannot be determined.");
		} catch (SocketTimeoutException e) {
			System.out.println("ERROR \t Socket timeout occurred. Retransmitting query.");
			retries += 1;
			transmitQuery(retries);
		} catch (Exception e) { //Remaining exceptions
			System.out.println(e.getMessage());
		}
	}

}
