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
				
				if (ob.type == "R" || ob.type == "r")
					ob.result = getFileData(ob);
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
		FileReader in = null;
		char[] bs = new char[ob.length];
		try {
	 
	         in = new FileReader(ob.path);
	         in.read(bs,ob.length,ob.offset);
	         String out = new String(bs);
	         return out;
	         
		}finally {
	         if (in != null) {
	            in.close();
	         }
	      }
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
	    ob.path = true_request.substring(4,ob.length+5);
        System.out.println(ob.path);
	    ob.type = true_request.substring(ob.length+5,ob.length+6);
	    ob.offset = Integer.parseInt(true_request.substring(ob.length+5,ob.length+9));
	    ob.length = Integer.parseInt(true_request.substring(ob.length+10,ob.length+14));
		return ob;
	}	   
	
}