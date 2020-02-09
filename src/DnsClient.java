import java.io.*;
import java.net.*;

public class DnsClient 
{
	private static int timeout = 5000; //default value in milliseconds
	private static int maxRetries = 3; //default value for retransmitting unanswered query
	private static int port = 53; //default UDP port number
	private static String name = ""; //domain name to query for
	private static byte[] server = new byte[4]; //IPv4 address of the DNS server in a.b.c.d format
	private static QType queryType = QType.A;
	
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
		
		
		BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in)); //creates input stream
		DatagramSocket clientSocket = new DatagramSocket(); //creates the socket
		clientSocket.setSoTimeout(timeout);
		InetAddress IPAddress = InetAddress.getByAddress(server);
		
		byte[] sendData = new byte[1024];
		byte[] receiveData = new byte[1024];
		
		String sentence = inFromUser.readLine(); 
		sendData = sentence.getBytes();
		
		//creates datagram with data to send, length, ip address and port
		DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, 9876); 
		
		clientSocket.send(sendPacket); //send datagram to server
		
		DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
		clientSocket.receive(receivePacket); //read datagram from server
		
		String modifiedSentence = new String(receivePacket.getData());
		System.out.println("FROM SERVER" + modifiedSentence);
		clientSocket.close();
		
	}
			
	private static void readInput(String input[]) {
		
		for (int i = 0; i < input.length; i++) {
			switch(input[i]) {
				case "-t":
					i += 1; //get argument after flag
					timeout = Integer.parseInt(input[i]) * 1000;
					break;
				case "-r":
					i += 1;
					maxRetries = Integer.parseInt(input[i]);
					break;
				case "-p":
					i += 1;
					port = Integer.parseInt(input[i]);
					break;
				case "-mx":
					queryType = QType.MX;
					break;
				case "-ns":
					queryType = QType.NS;
					break;
				default:
					if (input[i].equals("@")) {
						String ipAddress = input[i].substring(1);
						String ipNumbers[] = ipAddress.split("\\.");
						
						for (int j = 0; j < ipNumbers.length; j++) {
							int octet = Integer.parseInt(ipNumbers[j]);
							if (octet < 0 || octet > 255) {
								throw new NumberFormatException("ERROR \t Incorrect input syntax: IPv4 numbers should be between 0 and 255.");
							}
							
							server[j] = (byte) octet;
						}
						name = input[i+1];
					}
					break;
			}
		}
	}
	

}
