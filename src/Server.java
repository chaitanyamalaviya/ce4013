import java.net.*; 
import java.util.Arrays;
import java.io.*;

public class Server {
	
	public String path;
	public String type; //type of operation - 'R'/'W'/'D' etc
	public String result;
	public int offset;
	public int length;
	
	
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
				Server ob = unmarshal(buffer);
			    System.out.println(ob.type);
				if (ob.type.compareTo("R") == 0)
					ob.result = getFileData(ob);
			    System.out.println(ob.result);
			    
				byte[] res = ob.result.getBytes();
				DatagramPacket reply = new DatagramPacket(res, res.length, request.getAddress(), request.getPort()); //to reply, send back to the same port
			    aSocket.send(reply);	
			    }
			}
		finally 
		{
			if (aSocket != null) 
				aSocket.close();

		}

	 }
			
	
	
	public static String getFileData(Server ob) throws IOException{
		System.out.println(ob.path);
		FileInputStream in = null;
		byte[] bs = new byte[100];
		try {
	 
	         in = new FileInputStream(ob.path);
	         in.read(bs,ob.offset,ob.length);
	         String out = new String(bs);
	         return out;
	         
		}
		catch(Exception e)
		{
			System.out.println("Error: IOException thrown in getFileData");
			System.out.println(e.getMessage());
		}

        if (in != null) {
           in.close();
        }
		
		return "";
	}
	   
	public static boolean writeData(Server ob) throws IOException{
		return true;
		
	}
	
	public static boolean addMonitorClient(){  //Add monitoring client entry to the dictionary
		return true;
	}
	
	public static boolean removeMonitorClient(){  //Remove monitoring client entry upon expiry of its monitor interval
		return true;
	}
	
	
	public static boolean sendUpdates(File fd){  //Called every time a change is made to the specified file
		return true;
	}
	
	public static boolean rename(){ //Non-idempotent
		return true;
	}
	
	public static boolean delete(){ //Idempotent
		return true;
	}
	
	
	public static Server unmarshal(byte[] request)
	{
	    String req = Arrays.toString(request); // In form [48,34,...]
		String[] byteValues = req.substring(1, req.length() - 1).split(",");
		byte[] bytes = new byte[byteValues.length];

		for (int i=0, len=bytes.length; i<len; i++) {
   			bytes[i] = Byte.parseByte(byteValues[i].trim());     
		}

		String true_request = new String(bytes);
		System.out.println("True:"+true_request);
		
	    Server ob =  new Server();

	    ob.length = Integer.parseInt(true_request.substring(0,4));
	    ob.path = true_request.substring(4,ob.length+4);
        System.out.println(ob.path);
	    ob.type = true_request.substring(ob.length+4,ob.length+5);
	    ob.offset = Integer.parseInt(true_request.substring(ob.length+5,ob.length+9));
	    ob.length = Integer.parseInt(true_request.substring(ob.length+9,ob.length+13));
	    System.out.println(ob.length);
	    return ob;
	}	   
	
}