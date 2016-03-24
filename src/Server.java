import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.io.*;


public class Server extends Thread {

	public String path;
	public String type; // type of operation - 'R'/'W'/'D' etc
	public int offset;
	public int length; // length of path
	public int monitorInterval;
	public String data;
	public String destPath;
	public static Map monitor = new HashMap();
	
	public boolean writeSucceed;
	public String result;
	
	
	public static void main(String args[]) throws IOException,
			ClassNotFoundException {

		DatagramSocket aSocket = null;

		try {
			aSocket = new DatagramSocket(2222); // bound to host and port
			byte[] buffer = new byte[1000];
			while (true) {

				DatagramPacket request = new DatagramPacket(buffer,
						buffer.length);
				aSocket.receive(request); // blocked if no input
				Server ob = unmarshal(buffer);

				System.out.println(ob.type);

				switch (ob.type.toUpperCase()) {
					case "R":
						ob.result = getFileData(ob);
						break;
					case "W":
						if (writeData(ob))
							ob.result = "T";
						else
							ob.result = "F";
						break;
					case "M":
						if (addMonitorClient(ob,request))
							ob.result = "T";
						else
							ob.result = "F";
						break;
					case "F":
						ob.result = copy(ob);
						break;
					case "D":
						if (delete(ob))
							ob.result = "T";
						else
							ob.result = "F";
						break;
					}

				System.out.println(ob.result);
				byte[] res = ob.result.getBytes();
				DatagramPacket reply = new DatagramPacket(res, res.length,request.getAddress(), request.getPort()); 
															
				aSocket.send(reply);
			}
		} finally {
			if (aSocket != null)
				aSocket.close();

		}

	}

	public static String getFileData(Server ob) throws IOException {

		System.out.println(ob.path);
		int size = ob.length;
		byte[] bs = new byte[size];


		RandomAccessFile in = null;
		try {
			in = new RandomAccessFile(ob.path, "rw");
			in.seek(ob.offset);
			in.read(bs);

			String out = new String(bs);
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

	public static boolean writeData(Server ob) throws IOException {

		RandomAccessFile out = null;
		try {
			out = new RandomAccessFile(ob.path, "rw");
			out.seek(ob.offset);
			
			int size = (int) (long) (out.length());
			size = size - ob.offset;
			byte[] bs = new byte[size]; 
			out.read(bs);
			
			out.seek(ob.offset);
			out.write(ob.data.getBytes());
			
			out.write(bs);
			sendUpdates(ob); //send updates to all the clients monitoring this file
			
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
		// Add monitoring client entry to the hashmap
												
		try{
		
		Vector<Object> parent;
		if (monitor.containsKey(ob.path))
		    parent = (Vector<Object>) monitor.get(ob.path);
		else 
			parent = new Vector(5);
		
		Vector entry = new Vector(4);
		entry.add(request.getAddress());
		entry.add(request.getPort());
		entry.add(ob.monitorInterval);
		Date date = new Date();
		entry.add(date.toString());
		parent.add(entry);
		
		monitor.put(ob.path,parent);
		
		return true;
		}
		
		catch(Exception e){
			System.out.println(e.getMessage());
			return false;
		}
		
		
	}
	
	public static int timeDiff(Date timestamp, Date current){
		int diff = (int) (timestamp.getTime()-current.getTime());
		return diff/1000;
	}

	public static boolean sendUpdates(Server ob) throws IOException { 
		// Called every time a change is made to the specified file, sends updates to all clients monitoring this file
		DatagramSocket aSocket = null;
		DatagramPacket reply = null;
		try{
		Vector<Object> clients= (Vector<Object>)monitor.get(ob.path);
		byte[] res;
		for (int i=0;i<clients.size();i++){
			Vector<Object> client = (Vector<Object>)(clients.get(i));
			Date date = new Date();
			Date timestamp = (Date) client.get(3);
			int minterval = (int)client.get(2);
			if (minterval<timeDiff(timestamp,date))
				removeMonitorClient(ob,i);
			else{
				aSocket = new DatagramSocket((int)client.get(1));
				if (ob.data!=null)
					res = (ob.data+ob.offset).getBytes();
				else
					res = "Deleted".getBytes();
					
			    reply = new DatagramPacket(res, res.length,(InetAddress) client.get(0), (int)client.get(1)); 											
			    aSocket.send(reply);
			}	
		}
		return true;
	}
		catch(Exception e){
			System.out.println("IOException at sendUpdates");
			System.out.println(e.getMessage());
			return false;
		}
		finally {
			if (aSocket != null)
				aSocket.close();
			}
	}
	
	
	
	public static boolean removeMonitorClient(Server ob, int i) { 
		// Remove monitoring client entry upon expiry of its monitor interval
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
	
	
	public static String copy(Server ob) throws IOException { // Non-idempotent

		try {
			File fs = new File(ob.path);
			Path destDir = Paths.get(ob.destPath);
			String name = fs.toPath().getFileName().toString();
			int pos = name.lastIndexOf(".");
			String ext;
			ext = name.substring(pos);
			name = name.substring(0, pos);

			Path loc = destDir.resolve(name + ext);
			File fd = loc.toFile();
			int i = 0;
			System.out.println(loc.toString());
			while (fd.exists()) {
				
				i++;
				loc = destDir.resolve(name + "-copy-" + i + ext);
				fd = loc.toFile();
				if (!fd.exists()) {
					Files.copy(fs.toPath(), loc);
					return (name + "-copy-" + i + ext);
				}
				
			}
			Files.copy(fs.toPath(), loc);
			return (name+ext);
			
			
		} catch (Exception e) {
			System.out.println("File doesn't exist!");
			System.out.println(e.getMessage());
			return null;
		}
		
	}

	public static boolean delete(Server ob) throws IOException { // Idempotent

		try {
			File fs = new File(ob.path);
			if (fs.exists()) {
				Path p = Paths.get(ob.path);
				Files.deleteIfExists(p);
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
		String req = Arrays.toString(request); // In form [48,34,...]
		String[] byteValues = req.substring(1, req.length() - 1).split(",");
		byte[] bytes = new byte[byteValues.length];

		for (int i = 0, len = bytes.length; i < len; i++) {
			bytes[i] = Byte.parseByte(byteValues[i].trim());
		}

		String true_request = new String(bytes);
		System.out.println("True:" + true_request);

		Server ob = new Server();

		ob.length = Integer.parseInt(true_request.substring(0, 4));
		ob.path = true_request.substring(4, ob.length + 4);
		System.out.println(ob.path);
		ob.type = true_request.substring(ob.length + 4, ob.length + 5);
		switch (ob.type.toUpperCase()) {
		case "R":
			ob.offset = Integer.parseInt(true_request.substring(ob.length + 5,
					ob.length + 9));
			ob.length = Integer.parseInt(true_request.substring(ob.length + 9,
					ob.length + 13));
			break;
		case "W":
			ob.offset = Integer.parseInt(true_request.substring(ob.length + 5,
					ob.length + 9));
			int dataLength = Integer.parseInt(true_request.substring(
					ob.length + 9, ob.length + 13));
			ob.data = true_request.substring(ob.length + 13, ob.length + 13
					+ dataLength);
			break;
		case "D":
			break;
		case "M":
			ob.monitorInterval = Integer.parseInt(true_request.substring(
					ob.length + 5, ob.length + 9));
			break;
		case "F":
			int destPathLen = Integer.parseInt(true_request.substring(
					ob.length + 5, ob.length + 9));
			ob.destPath = true_request.substring(ob.length + 9, ob.length + 9
					+ destPathLen);
			break;

		}

		return ob;
	}

}