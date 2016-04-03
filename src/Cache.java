import java.util.Date;

public class Cache {
	public String path; //File path
	public String data; //Data read after read request
	public int offset = -1;
	public int length = -1;
	public Date Tc; //Time when client last performed callback for updating cache 
	public Date Tmclient;//Last modification time of file at client

	public Cache(String filePath, String data, int offset, int length) {
		path = filePath;
		this.data = data;
		this.offset = offset;
		this.length = length;
	}
}