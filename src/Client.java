import java.net.*; 
import java.io.*;
import java.nio.*;


public class Client{
	
	public String path;
	public String type; //type of operation - 'R'/'W'/'D' etc
	public int offset;
	public int length;
	

	public static void main(String args[]) throws IOException, NotSerializableException {
		DatagramSocket aSocket = null;
		//ByteArrayOutputStream bos = new ByteArrayOutputStream(); //For serializing byte array-can't use
		//ObjectOutput out = null;
		try {
			aSocket = new DatagramSocket(); 
			Client ob = new Client();
			ob.path = args[0];
			ob.type = args[1];
			ob.offset = Integer.parseInt(args[2]);
			ob.length = Integer.parseInt(args[3]);
			byte[] clientRequest = marshal(ob);
			System.out.println("Req:"+ clientRequest);
			//out = new ObjectOutputStream(bos);   
			//out.writeObject(ob);
			//byte[] clientRequest = bos.toByteArray();

			
			InetAddress aHost = InetAddress.getByName(args[4]);  
			int serverPort = 2222;
	
			DatagramPacket request = new DatagramPacket(clientRequest, clientRequest.length, aHost, serverPort);
			aSocket.send(request);
			//send packet using socket method
			byte[] buffer = new byte[1000]; //a buffer for receive 
			
			DatagramPacket reply = new DatagramPacket(buffer,buffer.length); //a different constructor 
			aSocket.receive(reply);
			System.out.println("File Data: "+ new String(reply.getData()));
            } 
		finally 
		{
			if (aSocket != null) 
				aSocket.close();}

	}
	
	public static byte[] marshal(Client ob){

		//System.out.println(String.format("%04d", ob.path.length()));
		String con = String.format("%04d", ob.path.length()) + ob.path + ob.type + String.format("%04d",ob.offset) + String.format("%04d",ob.length);
		byte[] req = con.getBytes();
		
		
//		byte[] path_ob = ob.path.getBytes();
//		byte[] path_length_ob = ByteBuffer.allocate(4).putInt(ob.path.length()).array();
//		byte[] type_ob = ob.type.getBytes();
//		byte[] offset_ob = ByteBuffer.allocate(4).putInt(ob.offset).array();
//		byte[] length_ob = ByteBuffer.allocate(4).putInt(ob.length).array();
//		byte[] req = new byte[path_ob.length + path_length_ob.length + type_ob.length + offset_ob.length + length_ob.length];
//		//Would be better to do this iteratively
//		System.arraycopy(path_ob, 0, req, 0, path_ob.length);
//		System.arraycopy(path_length_ob, 0, req, path_ob.length, path_length_ob.length);
//		System.arraycopy(type_ob, 0, req, path_length_ob.length+path_ob.length, type_ob.length);
//		System.arraycopy(offset_ob, 0, req, path_length_ob.length+path_ob.length+type_ob.length, offset_ob.length);
//		System.arraycopy(length_ob, 0, req, path_length_ob.length+path_ob.length+type_ob.length+offset_ob.length, length_ob.length);
		return req;
	}
}