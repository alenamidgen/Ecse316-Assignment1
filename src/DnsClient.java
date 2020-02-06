import java.io.*;
import java.net.*;

public class DnsClient 
{
	public static void main (String args[]) throws Exception
	{
		int timeout = 5; //default vlaue in seconds
		int max_retries = 3; //default value for retransmitting unanswered query
		int port = 53; //default UDP port number
		String name = ""; //domain name to query for
		String server = ""; //IPv4 address of the DNS server in a.b.c.d format
		String type = "A";
		
		if (args.length <= 0)
		{
			System.out.println("Invalid argument count");
			return;
		}
		else if (server == null || name == null)
		{
			System.out.println("IP address and domain name has to be provided");
		}
		
		
		
		BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in)); //creates input stream
		DatagramSocket clientSocket = new DatagramSocket(); //creates the socket
		InetAddress IPAddress = InetAddress.getByAddress(server);
		
		byte [] sendData = new byte[1024];
		byte [] receiveData = new byte[1024];
		
		String sentence = inFromUser.readLine(); 
		sendData = sentence.getBytes();
		
		//creates datagram with ata to send, length, ip address and port
		DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress 9876); 
		
		clientSocket.send(sendPacket); //send datagram to server
		
		DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
		clientSocket.receive(receivePacket); //read datagram from server
		
		String modifiedSentence = new String(receivePacket.getData());
		System.out.println("FROM SERVER" + modifiedSentence);
		clientSocket.close();
		
	}
			
		
	

}
