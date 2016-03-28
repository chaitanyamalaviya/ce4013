import java.util.Date;

public class Cache {
	public String path;
	public String data;
	public int offset = -1;
	public int length = -1;
	public Date Tc;
	public Date Tmclient;

	public Cache(String filePath, String data, int offset, int length) {
		path = filePath;
		this.data = data;
		this.offset = offset;
		this.length = length;
	}
}