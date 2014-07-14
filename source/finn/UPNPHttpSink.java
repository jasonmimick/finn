package finn;
import java.io.File;
import java.io.FileWriter;
import java.net.DatagramPacket;

public class UPNPHttpSink implements UPNPSearchSink,SenderSource
{
	Sender sender;
	public UPNPHttpSink() {
		this.sender = new Sender();
	}
	private String port = "0";

	public void run() {
		Finn.getLogger().warning("UPNPHttpSink run() called - does not do anything");
	}
	public void setProperty(String name, Object value) {
		System.out.println("UPNPHttpString setProperty("+name+","+value+")");
		if ( name.toLowerCase().equals("port") ) {
			this.port = value.toString();
			this.sender.setProperty(Sender.SENDER_ENDPOINT,"http://localhost:"+this.port);
			Finn.getLogger().info("UPNPHttpSink port = "+this.port);
		}
	}


	/// Append the data in response to the file
	public void OnDeviceResponse(Object o) {
    	DatagramPacket packet = (DatagramPacket) o;
        String s = new String(packet.getData(),0,packet.getLength());
		System.out.println("UPNPHttpSink got data s="+s);
		if ( this.port == "0" ) {
			throw new RuntimeException("UPNPHttpSink port not set, unable to send data");
		}
		String response = this.sender.send((SenderSource)this,"1",s);	
		Finn.getLogger().info(response);
	}
	public void register(Sender sender) {
	}	

	/// The source will get called back with the object to
	/// send - this allows the source to control the output formatting 
	public byte[] getData(String id, Object o) {
		Finn.getLogger().fine("getData");
		return o.toString().getBytes();
	}

	public boolean needsResponse() { return true; }

	public static void main(String[] args) {
		UPNPSearcher searcher = new UPNPSearcher();
		UPNPHttpSink sink = new UPNPHttpSink();
		sink.setProperty("port","9981");
		searcher.addSink(sink);
		searcher.search();
		try {
			Thread.sleep(10*1000);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
