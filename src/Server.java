import java.net.*; 
import java.io.*;

public class Server {
	public static void main(String args[]) throws IOException, ClassNotFoundException, NotSerializableException
	{ 
		DatagramSocket aSocket = null;
		
		try
			{
			aSocket = new DatagramSocket(2222);  //bound to host and port
			byte[] buffer = new byte[1000]; 
			while(true){
				
				DatagramPacket request= new DatagramPacket(buffer, buffer.length);
				aSocket.receive(request); //blocked if no input
				demarshal(buffer);
//				ByteArrayInputStream bis = new ByteArrayInputStream(buffer);
//				ObjectInput in = null;
//				in = new ObjectInputStream(bis);
//				Object o = (Client) in.readObject();
//				System.out.println(o.path);
				//getFileData (o);
				DatagramPacket reply = new DatagramPacket(request.getData(), request.getLength(), request.getAddress(), request.getPort()); //to reply, send back to the same port
			    aSocket.send(reply);	
			    }
			}
		finally 
		{
			if (aSocket != null) 
				aSocket.close();
//			try {
//				 bis.close();
//				} catch (IOException ex) {
				    // ignore close exception
				  }
//			try {
//				 if (in != null) {
//				      in.close();
//				 }
//				} catch (IOException ex) {
				    // ignore close exception
				  }
			
	
	
	//public static String getFileData(Object o){
		//System.out.println(o.path);
		//FileInputStream in = new FileInputStream(o.path);
	//}
	   
	public static byte[] demarshal(byte[] request)
	{
	
	}	   
	
}