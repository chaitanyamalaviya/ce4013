import java.net.*; 
import java.util.Arrays;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Server {
	
	public String path;
	public String type; //type of operation - 'R'/'W'/'D' etc
	public String content;
	public int offset;
	public int length;
	public boolean writeSucceed;
	public String result;
	public String destPath;
	
	public static void main(String args[]) throws IOException, ClassNotFoundException
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

			    switch(ob.type.toUpperCase()){
			    	case "R": ob.result = getFileData(ob);
			    	break;
			    	case "W": if (writeData(ob))
			    					ob.result = "T";
			    			  else
			    					ob.result = "F";
			    	break;
			    	case "M": addMonitorClient();
			    	break;
			    	case "C": ob.result = String.format("%02d", copy(ob));
			    	break;			
			    	case "D": if (writeData(ob))
    							ob.result = "T";
			    			  else
			    				ob.result = "F";
			    	break;
			    		
			    
			    }

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
		int size = ob.length;
		byte[] bs = new byte[size];
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
		
		FileOutputStream out = null;
		try{
			out = new FileOutputStream(ob.path);
			out.write(ob.content.getBytes(),ob.offset,ob.length);
			return true;
			
		}
		catch(Exception e){
			System.out.println("Error: IOException thrown in writeData");
			System.out.println(e.getMessage());
		}
		
		if (out != null) {
           out.close();      
		}
		
		return false;	
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
	
	public static int copy(Server ob) throws IOException{ //Non-idempotent
		
		try{
			File fs = new File(ob.path);
			Path destDir = Paths.get(ob.destPath);
			String name = fs.toPath().getFileName().toString();
			int pos = name.lastIndexOf(".");
			String ext = null;
			if (pos > 0) {
	    		name = name.substring(0, pos);
	    		ext = name.substring(pos);
			}
			File fd = null;
			int i = 1;
			
			while(!fd.exists()){
				Path loc = destDir.resolve(name+"copy"+i+ext);
				fd = loc.toFile();
				if (!fd.exists()){
					Files.copy(fs.toPath(),loc);
					return i;}
				i++;
			}
			
		}catch(Exception e){
			System.out.println("File doesn't exist!");
			System.out.println(e.getMessage());
		}
		return -1;
	}
	
	public static boolean delete(Server ob) throws IOException{ //Idempotent
		
		try{
		File fs = new File(ob.path);
		if (fs.exists()){
			Path p = Paths.get(ob.path);
			Files.deleteIfExists(p);
		 	return true;}	
		}
		catch(Exception e){
			System.out.println("File doesn't exist!");
			System.out.println(e.getMessage());
		}
		return false;
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

	    if (ob.type.compareTo("W")==0){
	    	int contentLength = Integer.parseInt(true_request.substring(ob.length+13,ob.length+17));
	    	ob.content = true_request.substring(ob.length+17,ob.length+17+contentLength);
	    }

	    System.out.println(ob.length);
	    return ob;
	}	   
	
}