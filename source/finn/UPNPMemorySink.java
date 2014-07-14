package finn;
import java.io.File;
import java.io.FileWriter;
import java.net.DatagramPacket;

public class UPNPMemorySink implements UPNPSearchSink
{
	public UPNPMemorySink() {
		this.buffer = new StringBuffer();
	}

	private StringBuffer buffer;

	public synchronized StringBuffer getBuffer() {
		return this.buffer;
	}
	
	public String getLastPacket() {
		String packet = this.getBuffer().toString();
		this.getBuffer().setLength(0);
		return packet;
	}

	/// Append the data in response to the file
	public void OnDeviceResponse(Object o) {
    	DatagramPacket packet = (DatagramPacket) o;
        String s = new String(packet.getData(),0,packet.getLength());
		System.out.println("UPNPMemorySink got data s="+s);
		this.getBuffer().append(s).append("\n");
	}
}
