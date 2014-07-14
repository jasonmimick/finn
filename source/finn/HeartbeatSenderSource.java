package finn;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;

public class HeartbeatSenderSource implements SenderSource
{

	public HeartbeatSenderSource() {
	}
	public void setProperty(String name,Object value) {
		Finn.getLogger().fine("notify name="+name+" value="+value);
	}

	private Sender sender;
	public void register(Sender sender) {
		this.sender = sender;
		Finn.getLogger().info("HeartbeatSourceSender registered sender="+sender);
	}	

	public static final String HEARTBEAT_RATE_SECONDS = "x-finn-sender-source-heartbeat-rate";
	private boolean needsAResponse = true;
	public boolean needsResponse() { return needsAResponse; }

	public static final int Default_HeartbeatRate_Seconds = 10;
 
	private int getHeartbeatRateMilliseconds() {
		String hbr = Finn.getFinn().getConfigString(HEARTBEAT_RATE_SECONDS);
		if ( !hbr.equals("") ) {
			return Integer.parseInt(hbr)*1000;
		}	
		return Default_HeartbeatRate_Seconds*1000;
	}
	/// Main work here - 	
	/// Send a heartbeat message every HEARTBEAT_RATE seconds
	public void run() {
		Finn.getLogger().info("HeartbeatSenderSource starting.");
		int ExceptionCounter = 0;
		while ( !Thread.currentThread().isInterrupted() ) {
			try {
				//this.needsAResponse = false;
				String hb = "finn.heartbeat."+(System.currentTimeMillis() / 1000L);
				String id = java.util.UUID.randomUUID().toString();
				String response = this.sender.send(this,id,hb);
				Finn.getLogger().info("Sent heartbeat="+hb);
				Thread.sleep(getHeartbeatRateMilliseconds());
			} catch (InterruptedException ie) {
			} catch (Exception e) {
				e.printStackTrace();
				Finn.getLogger().warning(e.getMessage());
				ExceptionCounter++;
				if ( ExceptionCounter > 5 ) {
					Finn.getLogger().severe("HeartbeatSenderSource had " + ExceptionCounter + " exceptions. Sleeping...");
					try { Thread.sleep(20*1000); } catch (InterruptedException iie) {}
					ExceptionCounter = 0;
				}
			}
		}
		Finn.getLogger().info("HeartbeatSenderSource stopping.");
	}

	/// The source will get called back with the object to
	/// send - this allows the source to control the output formatting 
	public byte[] getData(String id, Object o) {
		try {
			String data = o.toString();
			return data.getBytes();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	} 
}
