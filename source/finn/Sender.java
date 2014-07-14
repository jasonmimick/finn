package finn;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.*;
///
/// A sender will POST raw data from a device
/// to it's registered endpoint
/// Actually device 'drivers' should embed an instance 
/// of a Sender in their code.
/// Each sender will register itself to the "Server" in the
/// runtime which exposes a management interface over HTTP
public class Sender implements Runnable,Configurable
{
	private String endpoint;

	public void setProperty(String name,Object value) {
		// set endpoint??
		if ( name.equals( SENDER_ENDPOINT ) ) {
			this.setEndpoint((String)value);
		}
		// is this a special config to wire up a SenderSource
		configSenderSource(name,value);
		// delegate config to SenderSource
	}
	public Sender() {
		this.Sources = new HashMap<String,SenderSource>();
		this.SourceThreads = new HashMap<String,Thread>();
		Finn.getFinn().register("sender",this);
	}
	public final static String SENDER_SOURCE_PREFIX = "x-finn-sender-source-";
	public final static String SENDER_SOURCE_ADD = "x-finn-sender-source-add";
	public final static String SENDER_SOURCE_START = "x-finn-sender-source-start";
	public final static String SENDER_SOURCE_STOP = "x-finn-sender-source-stop";
	public final static String SENDER_ENDPOINT = "x-finn-sender-endpoint";
	private Map<String,SenderSource> Sources;
	private Map<String,Thread> SourceThreads;
	private void configSenderSource(String name, Object value) {
		if ( !name.startsWith( SENDER_SOURCE_PREFIX ) ) {
			return;
		}
		if ( name.equals(SENDER_SOURCE_START) ) {
			String sn = value.toString();
			if ( !this.Sources.containsKey( sn ) ) {
				Finn.getLogger().warning("Sender source='"+sn+"' not found. Unable to start");
				return;
			}
			Runnable source = (Runnable)this.Sources.get(sn);
			Thread thread = new Thread(	source );
			this.SourceThreads.put(sn,thread);
			Finn.getLogger().fine("About to start "+sn);
			thread.start();
			return;
		}
		if ( name.equals(SENDER_SOURCE_STOP) ) {
			String sn = value.toString();
			if ( !this.SourceThreads.containsKey( sn ) ) {
				Finn.getLogger().warning("Sender source='"+sn+"' not found. Unable to stop");
				return;
			}
			Thread thread = this.SourceThreads.get( sn );
			thread.interrupt();
			this.SourceThreads.remove( sn );
		}
		if ( name.equals(SENDER_SOURCE_ADD) ) {
			// create the sender ---
			try {
				String className = value.toString();
				Class<?> sourceClass = Class.forName(className);
				java.lang.reflect.Constructor<?> constructor = sourceClass.getConstructor();
				SenderSource object = (SenderSource) constructor.newInstance();
				Finn.getLogger().fine("SENDER_SOURCE_ADD className="+className+" object="+object);
				this.Sources.put(className,object);
				object.register(this);
			} catch (Exception e) {
				Finn.getLogger().warning(e.getMessage());
			}
			return;
		// bind the object to the given name
		}
		// if here, then not an add - but does start with SENDER_SOURCE_PREFIX
		// pull out classname
		String[] parts = name.split(SENDER_SOURCE_PREFIX);
		if ( this.Sources.containsKey( parts[0] ) ) {
			this.Sources.get(parts[0]).setProperty(parts[1],value);
		}
	}
	public void setEndpoint(String ep) {
		this.endpoint = ep;
		Finn.getLogger().fine("setEndpoint ep="+ep);
	}
	public String getEndpoint() {
		return this.endpoint;
	}

	public void run() {
		Finn.getLogger().info("Sender started");
		while (!Thread.currentThread().isInterrupted() ) {
			// do the twain stuff here 
			try {
				Thread.sleep(10*1000);
			} catch (InterruptedException ie) {
				System.out.println("Sender caught interruptedException");
				Finn.getLogger().info("Sender Interrupted");
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		Finn.getLogger().info("Sender stopped");
	}
	/// Post the data in this object to the endpoint
	public String send(SenderSource source, String id, Object o) {
		URL url;
    	HttpURLConnection connection = null;  
    	try {
			
      		//Create connection
			Finn.getLogger().fine("Sending to endpoint="+this.endpoint);
      		url = new URL(this.endpoint);
      		connection = (HttpURLConnection)url.openConnection();
         		connection.setUseCaches (false);
      		connection.setDoInput(true);
      		connection.setDoOutput(true);

			byte[] data = source.getData(id,o);
			connection.setRequestMethod("POST");
      		connection.setRequestProperty("Content-Type", 
           			"application/x-www-form-urlencoded");
      		connection.setRequestProperty("Content-Length", "" + 
               Integer.toString( data.length ));
		    connection.setRequestProperty("Content-Language", "en-US");  
			
			//Send request
			DataOutputStream stream = new DataOutputStream (connection.getOutputStream ());
			stream.write( data );
      		stream.flush ();
      		stream.close ();

      		//Get Response	
			if ( source.needsResponse() ) {
      			InputStream is = connection.getInputStream();
      			BufferedReader rd = new BufferedReader(new InputStreamReader(is));
      			String line;
      			StringBuffer response = new StringBuffer(); 
      			while((line = rd.readLine()) != null) {
        			response.append(line);
        			response.append('\r');
      			}
      			rd.close();
				String r = response.toString();
      			Finn.getLogger().fine("Got response: " + response.toString());
				return r;
			} else {
				return "";
			}

    	} catch (Exception e) {

			throw new RuntimeException(e);
    	} finally {

      		if(connection != null) {
        		connection.disconnect(); 
      		}
    	}
	}
}
