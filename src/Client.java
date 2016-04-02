import java.net.*;
import java.io.*;
import java.nio.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
///Users/chaitanya/documents/workspace/quotes/src/quotes/one-liners.txt
///home/ashwin/Academics/Distributed-Computing/ce4013/TestFile.txt


public class Client {

	//Client-Server Connection parameters
	InetAddress aHost;
	int serverPort;
	
	DatagramSocket aSocket;
	
	// Request parameters
	public String path;
	public String type; // type of operation - 'R'/'W'/'D' etc
	public int offset;
	public int length;
	public int monitorInterval;
	public String data;
	public String destPath;
	
	public static int requestId = -1;
	
	// Freshness interval
	public int t;
	
	public List<Cache> cache = new ArrayList<Cache>();

	public static int currenttimeDiff(Date timestamp){
		Date current = new Date();
		int diff = (int) (current.getTime()-timestamp.getTime());
		return diff/1000;
	}
	
	// Generates a new request id that is unique up to 10,000 requests
	public static String getRequestId(){
		requestId++;
		
		System.out.println(requestId);
		requestId = requestId % 10000;
		System.out.println(requestId);
				
		return String.format("%04d", requestId);
	}
	
	// Reads the cache to see if the file data requested is cached locally.
	// If the cached data is found but the freshness interval has expired,
	// a request is made to the server for the last modified time. If the
	// last modified time of the client is the same as the server, the 
	// cached content is sent to the user and returns 1. If not returns 0.
	public int readCache(String path, int offset, int length)
	{
		
		if(cache.isEmpty())
		{
			System.out.println("Cache is empty");
			return 0;
		}
		
		int indexOfCache = 0;
		boolean stale = false;
		
		// Iterate through cache and find if exists
		for (Cache cache2 : cache) {
			// Check if the path names are the same
			if(cache2.path.compareTo(path) != 0)
			{
				System.out.println("path mismatch");
				continue;
			}
			
			// Check if the requested content is within the bounds of the cached content
			if(cache2.offset > offset || ((cache2.offset + cache2.length) < (offset + length)))
			{
				continue;
			}
			
			// Check the to see if the freshness interval has expired.
			if( currenttimeDiff(cache2.Tc) > t)
			{
				// Freshness interval has expired.
				// Send a request to the server to get the last modified time (Tmserver)
				String con = getRequestId() + String.format("%04d", cache2.path.length()) + cache2.path + "T";
				byte[] clientRequest = con.getBytes();
				
				DatagramPacket request = new DatagramPacket(clientRequest, clientRequest.length, aHost, serverPort);
				
				try{
					aSocket = new DatagramSocket();
					
					aSocket.send(request);		

					byte[] buffer = new byte[1000]; // a buffer for receive

					DatagramPacket reply = new DatagramPacket(buffer, buffer.length); // a different constructor
					
					aSocket.receive(reply);
					String got = Arrays.toString(buffer); // In form [48,34,...]
					String[] byteValues = got.substring(1, got.length() - 1).split(",");
					byte[] bytes = new byte[byteValues.length];

					for (int i = 0, len = bytes.length; i < len; i++) {
						bytes[i] = Byte.parseByte(byteValues[i].trim());
					}

					String answer = new String(bytes);
					Date Tmserver = new Date(Long.parseLong(answer.substring(0, 13)));
					
					// Check to see if cache is stale or not
					if(Tmserver != cache2.Tmclient)
					{
						// Cache is stale
						stale = true;
						indexOfCache = cache.indexOf(cache2); // Mark cache index for eviction
					}
					else{
						// Cache is not stale. Reset the freshness interval start time
						cache2.Tc = new Date();
					}
				}
				catch(IOException e)
				{
					System.out.println(e.getMessage());
				}
			}
			
			if(!stale)
			{
				// Cache is not stale. Display data to the user.
				System.out.println("Returned from client cache: " + cache2.data.substring(offset - cache2.offset, offset - cache2.offset + length));
				
				return 1;
			}
		}
		
		if(stale)
		{
			// Cache is stale. Evict the cache from the list using the recorded index.
			cache.remove(indexOfCache);
		}
		
		return 0;
	}
	
	// 1. Cache of file exists: Updates the cache object with the new offset, length 
	// and data obtained from the server response. 
	// 2. Cache does of file does not exist: Creates a new entry in the list of cache
	// objects.
	public void updateCache(String path, String data, int offset, int length, Date Tmserver)
	{
		// Iterate to find if cache exists and if it does, update it.
		System.out.println("Cache Update()");
		for (Cache cache2 : cache) {
			if(cache2.path.compareTo(path) == 0)
			{
				cache2.data = data;
				cache2.length = length;
				cache2.offset = offset;
				cache2.Tc = new Date();
				cache2.Tmclient = Tmserver;
				return;
			}
		}

		// Cached file does not exist. Create a new file cache and add it to the 
		// list of cached objects.
		Cache newCache = new Cache(path, data, offset, length);
		newCache.Tmclient = Tmserver;
		newCache.Tc = new Date();
		
		cache.add(newCache);
		
		System.out.println("Cache Updated Successfully");
	}
	
	public static void main(String args[]) throws IOException, NotSerializableException {
		int op = 0;
		Client ob = new Client();
		
		// Get user input using new line as the delimiter.
		Scanner reader = new Scanner(System.in).useDelimiter("\n");
		
		// Socket timeout used to enforce retries when server response is not received.
		int timeout;
		
		ob.aSocket = null;
		
		// Init. random number generator for packet drop simulation
		Random rand = new Random();
		
		ob.t = Integer.parseInt(args[1]);

		System.out.println("Hello and Welcome to the Remote File System!");
		
		while (op != 6) {
			timeout = 1500;
		
			// Get user input on request type
			System.out.println("1. Read File");
			System.out.println("2. Insert content into the file");
			System.out.println("3. Delete the file ");
			System.out.println("4. Monitor file updates");
			System.out.println("5. Make a copy of the file ");
			System.out.println("6. Exit");
			System.out.println("Please enter your choice (1-6):");
			op = reader.nextInt();
			switch (op) {
			// Get the other request parameters based on the type
			case 1:
				ob.type = "R";
				System.out.println("Please enter the file path:");
				ob.path = reader.next();
				System.out.println("Please enter the offset:");
				ob.offset = reader.nextInt();
				System.out.println("Please enter the length:");
				ob.length = reader.nextInt();
				break;
			case 2:
				ob.type = "W";
				System.out.println("Please enter the file path:");
				ob.path = reader.next();
				System.out.println("Please enter the offset:");
				ob.offset = reader.nextInt();
				System.out.println("Please enter the data:");
				ob.data = reader.next();
				break;
			case 3:
				ob.type = "D";
				System.out.println("Please enter the file path:");
				ob.path = reader.next();
				System.out.println("Are you sure you want to delete the file?(Y/N)");
				if (reader.next().toUpperCase().equals("Y"))
					break;
				else 
					continue;
			case 4:
				ob.type = "M";
				System.out.println("Please enter the file path:");
				ob.path = reader.next();
				System.out.println("Please enter the monitor interval in seconds:");
				ob.monitorInterval = reader.nextInt();
				break;
			case 5:
				ob.type = "F";
				System.out.println("Please enter the file path:");
				ob.path = reader.next();
				System.out.println("Please enter the destination folder:");
				ob.destPath = reader.next();
				break;

			case 6:
				System.out.println("Goodbye!");
				return;
			}

			// If the request is a read, check cache first.
			if(ob.type.compareTo("R") == 0 && ob.readCache(ob.path, ob.offset, ob.length) == 1)
			{
				continue;
			}
				
			// Marshal and send request to the server (server port assumed to be 2222)
			try {
				ob.aSocket = new DatagramSocket();
				byte[] clientRequest = marshal(ob);
				System.out.println("Req:" + clientRequest);
				// out = new ObjectOutputStream(bos);
				// out.writeObject(ob);
				// byte[] clientRequest = bos.toByteArray();

				ob.aHost = InetAddress.getByName(args[0]);
				ob.serverPort = 2222;
								
				DatagramPacket request = new DatagramPacket(clientRequest, clientRequest.length, ob.aHost, ob.serverPort);
				byte[] buffer = new byte[1000]; // a buffer for reply
				DatagramPacket reply = new DatagramPacket(buffer, buffer.length); // a different constructor
				ob.aSocket.setSoTimeout(timeout);					// monitor interval expires
				
				while(true)
				{
					// Packet drop simulation
					int n = rand.nextInt(10);
					if( n <= 8 )
					{
						ob.aSocket.send(request);
					}
					else
					{
						System.out.println("Simulating request packet drop");
					}
					
					try{
						ob.aSocket.receive(reply);
						break;
					}
					catch(SocketTimeoutException e) {
		                // timeout exception.
		                System.out.println("Client Socket Timeout: Response not arrived");
   					}
				}
				
				
				if (ob.type.compareTo("M") == 0) { // Handle Monitor Requests
													// differently, prepare for receiving
													// server updates.
					Date startTime = new Date();
					System.out.println("Monitoring for updates...");
					while (true) {
						try{
							// Wait for server to send updates till the monitor period is elapsed.
							// Timeout is initially set to the (monitor period - time elapsed) and updated
							// after every update is received;
							// timeout = 	montiorInterval - (currentTime - startTime).
							timeout = (ob.monitorInterval*1000)-currenttimeDiff(startTime)*1000;
							
							// Sanitization of timeout
							if (timeout<0)
								break;
							
							ob.aSocket.setSoTimeout(timeout);					// monitor interval expires
							ob.aSocket.receive(reply); //Blocking command
								
							String monitor = unmarshal(buffer);		
							int length = Integer.parseInt(monitor.substring(0,4));
							System.out.println("Changes Made to "+ ob.path + ": " + monitor.substring(4,length+4));
							if (monitor.contains("File Deleted!")) //Stop monitoring if file is deleted
								break;
									
						  }
						catch(SocketTimeoutException e) {
			                // timeout exception.
			                System.out.println("Monitor Interval Over! ");
			                ob.aSocket.close();
						}
						catch(SocketException e){
							System.out.println("Socket Closed!");
							break;
						}
							
					}
				}
				else{
					// If not monitor request
					String answer = unmarshal(buffer);
					
					int length = Integer.parseInt(answer.substring(0,4));
					if (ob.type.compareTo("R") == 0)
						System.out.println("Reply data:" + answer.substring(4,length-9));
					else
						System.out.println("Reply data:" + answer.substring(4,length+4));

					// If a read request was made to the server, the cache needs to be updated
					// with the response.
					if(ob.type.compareTo("R") == 0)
					{
						Date Tmserver = new Date(Long.parseLong(answer.substring(length-9, length + 4)));
						ob.updateCache(ob.path, answer.substring(4,length-9), ob.offset, ob.length, Tmserver);
					}

				}
			} finally {
				if (ob.aSocket != null)
					ob.aSocket.close();
			}

		}
	}

	public static String unmarshal(byte[] buffer)
	{
		// Convert the received byte array to string
		String got = Arrays.toString(buffer); // In form [48,34,...]
		String[] byteValues = got.substring(1, got.length() - 1).split(",");
		byte[] bytes = new byte[byteValues.length];

		for (int i = 0, len = bytes.length; i < len; i++) {
			bytes[i] = Byte.parseByte(byteValues[i].trim());
		}

		return new String(bytes);		
	}
	
	public static byte[] marshal(Client ob) {

		// Begin with the requestId.
		String con = getRequestId();

		// Marshal each request based on the type of request
		switch (ob.type.toUpperCase()) {
		case "R":
			// Request Id + 4 digit path length + path + one digit type of request + 4 digit offset
			// + 4 digit length of data; where + is string concatenation
			con += String.format("%04d", ob.path.length()) + ob.path + ob.type + String.format("%04d", ob.offset)
					+ String.format("%04d", ob.length);
			break;
		case "W":
			// Request Id + 4 digit path length + path + one digit type of request + 4 digit offset
			// + 4 digit length of data + data; where + is string concatenation
			con += String.format("%04d", ob.path.length()) + ob.path + ob.type + String.format("%04d", ob.offset)
					+ String.format("%04d", ob.data.length()) + ob.data;
			break;
		case "D":
			// Request Id + 4 digit path length + path + one digit type of request 
			// where + is string concatenation
			con += String.format("%04d", ob.path.length()) + ob.path + ob.type;
			break;
		case "M":
			// Request Id + 4 digit path length + path + one digit type of request + 4 digit monitorInterval
			// where + is string concatenation
			con += String.format("%04d", ob.path.length()) + ob.path + ob.type
					+ String.format("%04d", ob.monitorInterval);
			break;
		case "F":
			// Request Id + 4 digit path length + path + one digit type of request + 4 digit destPath length
			// + destination path; where + is string concatenation
			con += String.format("%04d", ob.path.length()) + ob.path + ob.type
					+ String.format("%04d", ob.destPath.length()) + ob.destPath;
			
			break;

		}
		
		// Convert final request string to bytes and return it.
		byte[] req = con.getBytes();
		return req;
	}
}