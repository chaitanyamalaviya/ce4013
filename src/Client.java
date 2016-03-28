import java.net.*;
import java.io.*;
import java.nio.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
///Users/chaitanya/documents/workspace/quotes/src/quotes/one-liners.txt

public class Client {

	InetAddress aHost;
	int serverPort;
	
	DatagramSocket aSocket;
	
	public String path;
	public String type; // type of operation - 'R'/'W'/'D' etc
	public int offset;
	public int length;
	public int monitorInterval;
	public String data;
	public String destPath;
	
	// Freshness interval
	public int t;
	
	public List<Cache> cache = new ArrayList<Cache>();

	public static int currenttimeDiff(Date timestamp){
		Date current = new Date();
		int diff = (int) (current.getTime()-timestamp.getTime());
		return diff/1000;
	}
	
	public int readCache(String path, int offset, int length)
	{
		
		if(cache.isEmpty())
		{
			System.out.println("Cache is empty");
			return 0;
		}
		
		// Iterate through cache and find if exists
		// System.out.println("Returned from client cache:" + cache[i].data);
		for (Cache cache2 : cache) {

			//System.out.println(String.format("%d, %d, %d, %d", cache2.offset, offset, cache2.length, length ));
			
			if(cache2.path.compareTo(path) != 0)
			{
				System.out.println("path mismatch");
				continue;
			}
			
			if(cache2.offset > offset || ((cache2.offset + cache2.length) < (offset + length)))
			{
				continue;
			}
			
			System.out.println(currenttimeDiff(cache2.Tc));
			System.out.println(t);
			
			if( currenttimeDiff(cache2.Tc) > t)
			{
				//getAttr - last modified time
				String con = String.format("%04d", cache2.path.length()) + cache2.path + "T";
				byte[] clientRequest = con.getBytes();
				
				DatagramPacket request = new DatagramPacket(clientRequest, clientRequest.length, aHost, serverPort);
				
				try{
					aSocket.send(request);		

					byte[] buffer = new byte[1000]; // a buffer for receive

					DatagramPacket reply = new DatagramPacket(buffer, buffer.length); // a different constructor
					
					aSocket.receive(reply);
					// System.out.println("File Data: "+ new
					// String(reply.getData()));
					// System.out.println("File Data: "+ buffer);
					String got = Arrays.toString(buffer); // In form [48,34,...]
					String[] byteValues = got.substring(1, got.length() - 1).split(",");
					byte[] bytes = new byte[byteValues.length];

					for (int i = 0, len = bytes.length; i < len; i++) {
						bytes[i] = Byte.parseByte(byteValues[i].trim());
					}

					String answer = new String(bytes);
					Date Tmserver = new Date(Long.parseLong(answer.substring(0, 13)));
					
					System.out.println(Tmserver);
					System.out.println(cache2.Tmclient);
					
					if(Tmserver != cache2.Tmclient)
					{
						if(cache.remove(cache2))
							continue;

						System.out.println("Error: Cache eviction failed");
					}
				}
				catch(IOException e)
				{
					System.out.println(e.getMessage());
				}
			}
			
			cache2.Tc = new Date();
			
			System.out.println("Returned from client cache: " + cache2.data.substring(offset - cache2.offset, offset - cache2.offset + length));
			
			return 1;
		}
		
		return 0;
	}
	
	public void updateCache(String path, String data, int offset, int length)
	{
		System.out.println("Cache Update()");
		int found = 0;
		for (Cache cache2 : cache) {
			if(cache2.path.compareTo(path) == 0)
			{
				cache2.data = data;
				cache2.length = length;
				cache2.offset = offset;
				found = 1;
			}
		}
		
		if(found == 1)
			return;
		
		//getAttr - last modified time
		String con = String.format("%04d", path.length()) + path + "T";
		byte[] clientRequest = con.getBytes();
		
		DatagramPacket request = new DatagramPacket(clientRequest, clientRequest.length, aHost, serverPort);
		
		try{
			aSocket.send(request);		

			byte[] buffer = new byte[1000]; // a buffer for receive

			DatagramPacket reply = new DatagramPacket(buffer, buffer.length); // a different constructor
			
			aSocket.receive(reply);
			// System.out.println("File Data: "+ new
			// String(reply.getData()));
			// System.out.println("File Data: "+ buffer);
			String got = Arrays.toString(buffer); // In form [48,34,...]
			String[] byteValues = got.substring(1, got.length() - 1).split(",");
			byte[] bytes = new byte[byteValues.length];

			for (int i = 0, len = bytes.length; i < len; i++) {
				bytes[i] = Byte.parseByte(byteValues[i].trim());
			}

			String answer = new String(bytes);
			//System.out.println(answer);
			//System.out.println(answer.length());
			
			Date Tmserver = new Date(Long.parseLong(answer.substring(0, 13)));
						
			Cache newCache = new Cache(path, data, offset, length);
			newCache.Tmclient = Tmserver;
			newCache.Tc = new Date();
			
			cache.add(newCache);
			
			System.out.println("Cache Updated Sucessfully");
		
		}
		catch(IOException e)
		{
			System.out.println(e.getMessage());
			System.out.println("Cache Update Failed");
		}
	}
	
	public static void main(String args[]) throws IOException, NotSerializableException {
		// ByteArrayOutputStream bos = new ByteArrayOutputStream(); //For
		// serializing byte array-can't use
		// ObjectOutput out = null;
		int op = 0;
		Client ob = new Client();
		Scanner reader = new Scanner(System.in).useDelimiter("\n");
		
		ob.aSocket = null;
				
		Random rand = new Random();
		
		ob.t = Integer.parseInt(args[1]);

		System.out.println("Hello and Welcome to the Remote File System!");
		
		while (op != 6) {			
			System.out.println("1. Read File");
			System.out.println("2. Insert content into the file");
			System.out.println("3. Delete the file ");
			System.out.println("4. Monitor file updates");
			System.out.println("5. Make a copy of the file ");
			System.out.println("6. Exit");
			System.out.println("Please enter your choice (1-6):");
			op = reader.nextInt();
			switch (op) {
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
				// Are you sure you want to delete the file?
				break;
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

			if(ob.type.compareTo("R") == 0 && ob.readCache(ob.path, ob.offset, ob.length) == 1)
			{
				continue;
			}
				
			
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

				// Packet drop simulation
				int n = rand.nextInt(10);
				if( n != 8 )
				{
					ob.aSocket.send(request);
				}
				else
				{
					System.out.println("Simulating request packet drop");
				}
				
				// send packet using socket method
				byte[] buffer = new byte[1000]; // a buffer for receive
				DatagramPacket reply = new DatagramPacket(buffer, buffer.length); // a different constructor
				
				if (ob.type.compareTo("M") == 0) { // Handle Monitor Requests
													// differently, block until
					Date startTime = new Date();
					ob.aSocket.receive(reply);
					System.out.println("Monitoring for updates...");
					while (true) {
						try{					
							int timeout = (ob.monitorInterval*1000)-currenttimeDiff(startTime);
							if (timeout<0)
								break;
							ob.aSocket.setSoTimeout(timeout);					// monitor interval expires
							ob.aSocket.receive(reply); //Blocking command
							String got = Arrays.toString(buffer); // In form [48,34,...]
							String[] byteValues = got
									.substring(1, got.length() - 1).split(",");
							byte[] bytes = new byte[byteValues.length];
	
							for (int i = 0, len = bytes.length; i < len; i++) {
								bytes[i] = Byte.parseByte(byteValues[i].trim());
							}
	
							String monitor = new String(bytes);		
							int length = Integer.parseInt(monitor.substring(0,4));
							System.out.println("Changes Made to "+ ob.path + ": " + monitor.substring(4,length+4));
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
					ob.aSocket.receive(reply);
					// System.out.println("File Data: "+ new
					// String(reply.getData()));
					// System.out.println("File Data: "+ buffer);
					String got = Arrays.toString(buffer); // In form [48,34,...]
					String[] byteValues = got.substring(1, got.length() - 1).split(",");
					byte[] bytes = new byte[byteValues.length];
	
					for (int i = 0, len = bytes.length; i < len; i++) {
						bytes[i] = Byte.parseByte(byteValues[i].trim());
					}
	
					String answer = new String(bytes);
					int length = Integer.parseInt(answer.substring(0,4));
					System.out.println("Reply data:" + answer.substring(4,length+4));
					
					
					if(ob.type.compareTo("R") == 0)
					{
						ob.updateCache(ob.path, answer.substring(4,length+4), ob.offset, ob.length);
					}
				}
			} finally {
				if (ob.aSocket != null)
					ob.aSocket.close();
			}

		}
	}

	public static byte[] marshal(Client ob) {

		String con = "";
		// System.out.println(String.format("%04d", ob.path.length()));
		switch (ob.type.toUpperCase()) {
		case "R":
			con = String.format("%04d", ob.path.length()) + ob.path + ob.type + String.format("%04d", ob.offset)
					+ String.format("%04d", ob.length);
			break;
		case "W":
			con = String.format("%04d", ob.path.length()) + ob.path + ob.type + String.format("%04d", ob.offset)
					+ String.format("%04d", ob.data.length()) + ob.data;
			break;
		case "D":
			con = String.format("%04d", ob.path.length()) + ob.path + ob.type;
			break;
		case "M":
			con = String.format("%04d", ob.path.length()) + ob.path + ob.type
					+ String.format("%04d", ob.monitorInterval);
			break;
		case "F":
			con = String.format("%04d", ob.path.length()) + ob.path + ob.type
					+ String.format("%04d", ob.destPath.length()) + ob.destPath;
			
			break;

		}

		// String con = String.format("%04d", ob.path.length()) + ob.path +
		// ob.type + String.format("%04d", ob.offset)
		// + String.format("%04d", ob.length);
		byte[] req = con.getBytes();

		// byte[] path_ob = ob.path.getBytes();
		// byte[] path_length_ob =
		// ByteBuffer.allocate(4).putInt(ob.path.length()).array();
		// byte[] type_ob = ob.type.getBytes();
		// byte[] offset_ob = ByteBuffer.allocate(4).putInt(ob.offset).array();
		// byte[] length_ob = ByteBuffer.allocate(4).putInt(ob.length).array();
		// byte[] req = new byte[path_ob.length + path_length_ob.length +
		// type_ob.length + offset_ob.length + length_ob.length];
		// //Would be better to do this iteratively
		// System.arraycopy(path_ob, 0, req, 0, path_ob.length);
		// System.arraycopy(path_length_ob, 0, req, path_ob.length,
		// path_length_ob.length);
		// System.arraycopy(type_ob, 0, req,
		// path_length_ob.length+path_ob.length, type_ob.length);
		// System.arraycopy(offset_ob, 0, req,
		// path_length_ob.length+path_ob.length+type_ob.length,
		// offset_ob.length);
		// System.arraycopy(length_ob, 0, req,
		// path_length_ob.length+path_ob.length+type_ob.length+offset_ob.length,
		// length_ob.length);
		return req;
	}
}