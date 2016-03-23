import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Server {

	public String path;
	public String type; // type of operation - 'R'/'W'/'D' etc
	public int offset;
	public int length; // length of path
	public int monitorInterval;
	public String data;
	public String destPath;

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
						addMonitorClient();
						break;
					case "F":
						ob.result = String.format("%02d", copy(ob));
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

		FileInputStream in = null;
		int size = ob.length;
		byte[] bs = new byte[size];
//		try {

			in = new FileInputStream(ob.path);
			System.out.println(ob.offset);
			System.out.println(ob.length);
			in.read(bs, ob.offset, ob.length);
			String out = new String(bs);
			return out;

//		}

//		catch (Exception e) {
//			System.out.println("Error: IOException thrown in getFileData");
//			System.out.println(e.getMessage());
//		}

//		if (in != null) {
//			in.close();
//		}
//
//		return "";
	}

	public static boolean writeData(Server ob) throws IOException {

		RandomAccessFile out = null;
		;
		try {
			out = new RandomAccessFile(ob.path, "rw");
			out.seek(ob.offset);
			out.write(ob.data.getBytes());
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

	public static boolean addMonitorClient() { // Add monitoring client entry to
												// the dictionary
		return true;
	}

	public static boolean removeMonitorClient() { // Remove monitoring client
													// entry upon expiry of its
													// monitor interval
		return true;
	}

	public static boolean sendUpdates(File fd) { // Called every time a change
													// is made to the specified
													// file

		return true;
	}

	public static int copy(Server ob) throws IOException { // Non-idempotent

//		try {
			File fs = new File(ob.path);
			Path destDir = Paths.get(ob.destPath);
			String name = fs.toPath().getFileName().toString();
			int pos = name.lastIndexOf(".");
			String ext;
			System.out.println(name);
			name = name.substring(0, pos);
			ext = name.substring(pos);
			
			System.out.println(ext);
			Path loc = destDir.resolve(name + ext);
			File fd = loc.toFile();
			int i = 1;

			while (!fd.exists()) {
				loc = destDir.resolve(name + "-copy-" + i + ext);
				fd = loc.toFile();
				if (!fd.exists()) {
					Files.copy(fs.toPath(), loc);
					return i;
				}
				i++;
			}

//		} catch (Exception e) {
//			System.out.println("File doesn't exist!");
//			System.out.println(e.getMessage());
//		}
		return -1;
	}

	public static boolean delete(Server ob) throws IOException { // Idempotent

		try {
			File fs = new File(ob.path);
			if (fs.exists()) {
				Path p = Paths.get(ob.path);
				Files.deleteIfExists(p);
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
			System.out.println(ob.data);
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