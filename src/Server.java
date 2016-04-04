import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class Server extends Thread {

	public String path; //File Path
	public String type; // type of operation - 'R'/'W'/'D'/'F'/'M'
	public int offset; 
	public int length; // length of path
	public int monitorInterval;
	public String data; //Data to write
	public String destPath; //destination directory for copy
	public static Map monitor = new HashMap(); //Stores path as key and list of clients as value
	
	public static Map responseCache = new HashMap(); //Stores port+requestid as key and response message as value
	
	public String result; //Result of each operation
	private static DatagramSocket aSocket = null;
	public static Random rand = new Random(); //For packet loss
	
	public String requestId;
	
	public static void main(String args[]) throws IOException,
			ClassNotFoundException {

		//Command line argument indicates invocation semantics: 
		//0(at-least one) and 1(at-most once)
		boolean atmostOnce = (Integer.parseInt(args[0]) == 1);
		
		try {
			aSocket = new DatagramSocket(2222); // bound to host and port
			byte[] buffer;
			while (true) {
				buffer = new byte[1000];
				DatagramPacket request = new DatagramPacket(buffer,
						buffer.length); //DatagramPacket for receiving client requests
				aSocket.receive(request); // blocked if no input
				Server ob = unmarshal(buffer); //Unmarshal received byte array
				
				//System.out.println("Inet and Port: "+ request.getAddress() + request.getPort());
				//System.out.println(ob.type);
				
				//Key for response cache
				int crId = Integer.parseInt((String.format("%5d", request.getPort()) + ob.requestId)); 
				
				if(atmostOnce && responseCache.containsKey(crId)) 
					//Retrieve stored response messages for at-most once semantics 
					//if key exists in response cache 
				{
					//Simulation of packet drop
					int n = rand.nextInt(10);
					if( n > 5 )
					{
						aSocket.send((DatagramPacket)responseCache.get(crId)); 
						//Send stored response message
					}
					else
					{
						System.out.println("Simulating response packet drop");
					}
					
					continue;
				}

				//Call respective method for each request type
				switch (ob.type.toUpperCase()) {
				
					case "R":
						ob.result = getFileData(ob) + getLastModifiedTime(ob.path).toString();
						//Append last modified time for client-side cache implementation
						break;
					
					case "W":
						if (writeData(ob))
							ob.result = "T";//Write successful
						else
							ob.result = "F";//Write failed
						break;
					
					case "M":
						if (addMonitorClient(ob,request))
							ob.result = "T"; //Monitor request succeeded
						else
							ob.result = "F"; //Monitor request failed
						break;
					
					case "F":
						ob.result = copy(ob); //Returns name of newly created file
						break;
					
					case "D":
						if (delete(ob))
							ob.result = "T"; //Delete succeeded
						else
							ob.result = "F"; //Delete failed
						break;

					case "T":
						//Retrieve last modified time of file at ob.path
						ob.result = getLastModifiedTime(ob.path);
						break;
					}

				//System.out.println(ob.result);
				byte[] res = (String.format("%04d", ob.result.length())+ob.result).getBytes();
				//Result is preceded by 4-digit result length and marshalled
				
				DatagramPacket reply = new DatagramPacket(res, res.length, request.getAddress(), request.getPort()); 
				
				if(atmostOnce)
					//Add response message to response cache if at-most once semantics
				{
					responseCache.put(Integer.parseInt((String.format("%5d", request.getPort()) + ob.requestId)), reply );
				}
								
				//Simulation of packet drop
				int n = rand.nextInt(10);
				if( n > 8 )
				{
					aSocket.send(reply);
					//Reply sent back to client
				}
				else
				{
					System.out.println("Simulating response packet drop");
				}
			}
		} finally {
			if (aSocket != null)
				aSocket.close();

		}

	}

	public static String getFileData(Server ob) throws IOException {
		//"""Performs the read operation on a file using RandomAccessFile 
		//and returns a String containing content read"""
		
		//System.out.println(ob.path);
		int size = ob.length;
		byte[] bs = new byte[size];


		RandomAccessFile in = null;
		try {
			//Create random access file stream
			in = new RandomAccessFile(ob.path, "rw"); 
			in.seek(ob.offset); //Seek to offset
			in.read(bs); //Read 'length' bytes

			String out = new String(bs); //Bytes to string
			return out;

		}

		catch (Exception e) {
			System.out.println("Error: IOException thrown in getFileData");
			System.out.println(e.getMessage());
		}

		if (in != null) {
			in.close();
		}

		return "";
	}

	public static String getLastModifiedTime(String path)
		//"""Retrieves the last modified time for a file""" 
	
	{
		File file = null;
		
		try{
			file = new File(path);
		}
		catch (Exception e) {

				System.out.println("Error: IOException thrown in getLastModified()");
				System.out.println(e.getMessage());
		}
		//Return last modified time of file
		return Long.toString(file.lastModified()); 
	}
	
	public static boolean writeData(Server ob) throws IOException {
		//"""Performs a write operation of the passed content on a file 
		// starting at a given offset. Returns boolean true if write succeeds 
		// and false otherwise """

		RandomAccessFile out = null;
		try {
			//New random access file stream
			out = new RandomAccessFile(ob.path, "rw");
			//Seek to offset
			out.seek(ob.offset); 
			
			//Read (file size - offset) bytes from offset into byte array bs
			int size = (int) (long) (out.length());
			size = size - ob.offset;
			byte[] bs = new byte[size]; 
			out.read(bs);
			
			//Seek to offset again
			out.seek(ob.offset);
			//Write passed data
			out.write(ob.data.getBytes());
			
			//Write back original content after inserted content
			out.write(bs);
			
			//send updates to all the clients(if any) monitoring this file
			if (monitor.containsKey(ob.path))
				sendUpdates(ob); 
			
			return true;
			
		} catch (Exception e) {

			System.out.println("Error: IOException thrown in writeData");
			System.out.println(e.getMessage());
		}

		if (out != null) {
			out.close();
		}

		return false;
	}

	public static boolean addMonitorClient(Server ob, DatagramPacket request) { 
		// """Adds client entry to the monitor hashmap and returns boolean true if successful"""
												
		try{
		
		//Vector of vectors for client entries for a file path
		Vector<Object> parent;
		
		//Check if entry for given file path already exists
		if (monitor.containsKey(ob.path)){
			//Get all client entries for given file path
		    parent = (Vector<Object>) monitor.get(ob.path);
		    
		    //Iterate through the existing clients for a file path
		    for (int i=0;i<parent.size();i++){
		    	Vector<Object> client = (Vector<Object>)(parent.get(i));
		    	int minterval = (int)client.get(2);
				DateFormat fm = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy"); //Mon Mar 28 19:58:13 SGT 2016
				Date timestamp = fm.parse((String)client.get(3));
				Date date = new Date();
				if (minterval<timeDiff(timestamp,date))
					removeMonitorClient(ob,i);
		    }
		    	
		}
		else 
		 //if entry for given file path does not exist, create a parent vector
			parent = new Vector(5); 
		
		Vector entry = new Vector(4);
		//Add client information
		entry.add(request.getAddress()); //Inet Address of request
		entry.add(request.getPort()); //Port number of request
		entry.add(ob.monitorInterval); //Monitor Interval of client
		Date date = new Date(); //Current date
		entry.add(date.toString()); //Add start of monitoring interval
		parent.add(entry); //Add client vector to parent vector
		
		//Add parent vector to monitor hashmap at filepath key
		monitor.put(ob.path,parent); 
		
		//System.out.println(monitor);
		return true;
		}
		
		catch(Exception e){
			System.out.println(e.getMessage());
			return false;
		}
		
		
	}
	
	public static int timeDiff(Date timestamp, Date current){
		//"""Returns the time difference in seconds between timestamp and current time"""
		
		int diff = (int) (current.getTime()-timestamp.getTime()); 
		//getTime gives seconds passed from an epoch for a Date object 
		return diff/1000; //Conversion from milliseconds to seconds
	}

	public static boolean sendUpdates(Server ob) throws IOException { 
		// """Called every time a change is made to the specified file, sends updates to all clients monitoring this file"""
		
		DatagramPacket reply = null;
		try{
		Vector<Object> clients= (Vector<Object>)monitor.get(ob.path);
		byte[] res;
		
		// Send updates to all monitoring clients
		for (int i=0;i<clients.size();i++){  
			
			Vector<Object> client = (Vector<Object>)(clients.get(i));//Retrieve a client vector
			int minterval = (int)client.get(2);
			DateFormat fm = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy"); //Format: Mon Mar 28 19:58:13 SGT 2016
			Date timestamp = fm.parse((String)client.get(3)); //Parse date object of above format into a string
			
			Date date = new Date();
			System.out.println(timeDiff(timestamp,date));

			//Remove client vector if monitor interval expires
			if (minterval<timeDiff(timestamp,date)) 
				removeMonitorClient(ob,i);
			else{
				if (ob.data!=null){
					//Send message to clients as "<'Data'> inserted at offset 8" for write operation
					String send = ("'"+ob.data+"'"+" inserted at offset "+ ob.offset); 
					
					//Marshal message into bytes
					res = (String.format("%04d",send.length())+send).getBytes();
				}
				else{
					//Send message to clients as "File Deleted!" and 
					//remove file entry from monitor HashMap for delete operation
					res = (String.format("%04d",13)+"File Deleted!").getBytes();
					monitor.remove(ob.path);
				}
				//Prepare datagrampacket for message to clients
				reply = new DatagramPacket(res, res.length, (InetAddress)client.get(0), (int)client.get(1)); 																					
			    
			    // Packet drop simulation
				int n = rand.nextInt(10);
				if( n != 8 )
				{
					aSocket.send(reply);
				}
				else
				{
					System.out.println("Simulating response packet drop");
				}
			}	
		}
		return true;
	}
		catch(Exception e){
			System.out.println("IOException at sendUpdates");
			System.out.println(e.getMessage());
			return false;
		}
	}
	
	
	
	public static boolean removeMonitorClient(Server ob, int i) { 
		// """Removes monitoring client entry at index i and
		//  returns boolean true if successful and false otherwise"""
		try{
		Vector<Object> clients= (Vector<Object>)monitor.get(ob.path);
		clients.removeElementAt(i);
		return true;
		}
		catch(Exception e){
			System.out.println(e.getMessage());
			return false;
		}
	}
	
	
	public static String copy(Server ob) throws IOException { 
		//""" Creates a copy of the requested file at a specified directory location"""
		// Is non-idempotent operation: creates multiple copies in case of duplicate requests

		try {
			File fs = new File(ob.path);			
			Path destDir = Paths.get(ob.destPath); //Path object for destination directory
			String name = fs.toPath().getFileName().toString(); //Get source file name 
			int pos = name.lastIndexOf("."); 
			String ext;
			ext = name.substring(pos); //Get file extension
			name = name.substring(0, pos); //Remove file extension from name

			Path loc = destDir.resolve(name + ext); 
			//Add name and ext to destination directory's path object
			
			File fd = loc.toFile(); //Path object to file descriptor
			int i = 0;
			//System.out.println(loc.toString());
			
			while (fd.exists()) {
				//If file with same path and name exists
				i++;
				//Declare new path object with name+copy+i, where i is copy number
				loc = destDir.resolve(name + "-copy-" + i + ext);
				fd = loc.toFile(); //Path object to file descriptor
				
				if (!fd.exists()) {
					//If file with same path and new name does not exist, create copy
					Files.copy(fs.toPath(), loc);
					return (name + "-copy-" + i + ext); //Return name+ext of newly created file
				}
				
			}
			//If file with same filename does not exist at the path, create copy with same name
			Files.copy(fs.toPath(), loc);
			return (name+ext); //Return name+ext of newly created file
			
			
		} catch (Exception e) {
			System.out.println("File doesn't exist!");
			System.out.println(e.getMessage());
			return null; //return null if file doesn't exist
		}
		
	}

	public static boolean delete(Server ob) throws IOException { 
		//"""Deletes a file if file exists at given path and 
		// returns boolean true if delete succeeds and false otherwise"""
		// Idempotent operation: Duplicate requests have no effect once the method has been executed once

		try {
			File fs = new File(ob.path);
			//Check if file exists
			if (fs.exists()) {
				Path p = Paths.get(ob.path);
				//Delete file if it exists at path p
				Files.deleteIfExists(p);
				
				//send updates to all the clients(if any) monitoring this file
				if (monitor.containsKey(ob.path))
					sendUpdates(ob);
				return true;
			}
		} catch (Exception e) {
			System.out.println("File doesn't exist!");
			System.out.println(e.getMessage());
		}
		return false;
	}

	public static Server unmarshal(byte[] request) {
		//"""Unmarshals each request depending on its type and
		// stores parsed request parameters in a server object and returns this object"""
		
		//Conversion from byte array to string
		String req = Arrays.toString(request); // In the form [48,34,...] of ASCII values
		String[] byteValues = req.substring(1, req.length() - 1).split(","); //Split req string at each ','
		byte[] bytes = new byte[byteValues.length]; 

		//
		for (int i = 0, len = bytes.length; i < len; i++) {
			//Parse each string splitted into a byte
			bytes[i] = Byte.parseByte(byteValues[i].trim());
		}

		//true_request stores actual request from client
		String true_request = new String(bytes);
		System.out.println("True:" + true_request);
		
		
		Server ob = new Server();

		ob.requestId = true_request.substring(0, 4);//Retrieve requestID
		true_request = true_request.substring(4); // Ignore the requestId now

		//Path length is next 4 characters in true_request
		ob.length = Integer.parseInt(true_request.substring(0, 4));
		//Path retrieved for ob.length characters starting from next character
		ob.path = true_request.substring(4, ob.length + 4);
		//System.out.println(ob.path);
		
		//One character request type
		ob.type = true_request.substring(ob.length + 4, ob.length + 5);
		
		
		switch (ob.type.toUpperCase()) {
		//Unmarshal remaining parameters depending on request type
		
			case "R":
				//4 character offset parsed as integer
				ob.offset = Integer.parseInt(true_request.substring(ob.length + 5,
						ob.length + 9));
				//4 character read length parsed as integer
				ob.length = Integer.parseInt(true_request.substring(ob.length + 9,
						ob.length + 13));
				break;
				
			case "W":
				//4 character offset parsed as integer
				ob.offset = Integer.parseInt(true_request.substring(ob.length + 5,
						ob.length + 9));
				//4 character write data length parsed as integer
				int dataLength = Integer.parseInt(true_request.substring(
						ob.length + 9, ob.length + 13));
				//Data retrieved for dataLength characters starting from next character
				ob.data = true_request.substring(ob.length + 13, ob.length + 13
						+ dataLength);
				break;
				
			case "D":
				//No more parameters for delete
				break;
				
			case "M":
				//4 character monitor interval parsed as integer
				ob.monitorInterval = Integer.parseInt(true_request.substring(
						ob.length + 5, ob.length + 9));
				break;
				
			case "F":
				//Get file modification time
				//4 character path length parsed as integer
				int destPathLen = Integer.parseInt(true_request.substring(
						ob.length + 5, ob.length + 9));
				//Path retrieved for destPathLen characters starting from next character
				ob.destPath = true_request.substring(ob.length + 9, ob.length + 9
						+ destPathLen);
				break;

		}

		//Return server object
		return ob;
	}

}