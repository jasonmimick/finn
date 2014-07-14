package finn;
import java.io.File;
import java.io.FileWriter;
import java.net.DatagramPacket;

public class UPNPFileWriterSink implements UPNPSearchSink
{
	public String filename;
	private int fileCounter = 0;
	public UPNPFileWriterSink(String filename) {

		this.filename = filename;
	}

	/// Append the data in response to the file
	public void OnDeviceResponse(Object o) {
    	DatagramPacket packet = (DatagramPacket) o;
        String s = new String(packet.getData(),0,packet.getLength());
		System.out.println("UPNPFileWriterSink got data s="+s);
		FileWriter writer = null;
		try {
			String fn = this.filename + "." + (++fileCounter);
			File file = new File(fn);
			file.getParentFile().mkdirs();
			writer = new FileWriter(file, true);
			writer.write(s);
			writer.close();
		} catch (Exception e) {
			System.out.println(e);
		} finally {
		}
	}
}
