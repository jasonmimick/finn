package finn;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.io.*;
import java.net.InetAddress;
import javax.xml.ws.*;
import javax.xml.ws.http.*;
import javax.xml.transform.*;
import javax.xml.transform.stream.*;
import java.net.InterfaceAddress;
import javax.activation.DataHandler;
import javax.xml.ws.handler.MessageContext;
import java.util.Map;
import java.util.HashMap;
import javax.annotation.Resource;
//import javax.servlet.http.Part;
@WebServiceProvider
@BindingType(value = HTTPBinding.HTTP_BINDING)
@ServiceMode(value = Service.Mode.MESSAGE)
public class Server implements Provider<Source>,Runnable,Configurable {
	
	public Server() {
	}

   	public void setProperty(String name, Object value) {
		// TODO - react to configuration changes
	}

	@Resource(type = Object.class)
 	protected WebServiceContext wsContext;	

    public Source invoke(Source request) {
		// This needs to get invoked with a POST from HealtShare 
		// to send in the configuration data to send off files.
		//Finn.getLogger().fine("Server got request:"+request.toString());
		MessageContext messageContext = wsContext.getMessageContext();
   		String requestMethod = (String) messageContext.get(MessageContext.HTTP_REQUEST_METHOD);
		Finn.getLogger().fine("requestMethod = " + requestMethod);
   		String query = (String) messageContext.get(MessageContext.QUERY_STRING);
		Finn.getLogger().info("query="+query);
   		String path = (String) messageContext.get(MessageContext.PATH_INFO);
		// TODO: Validate AUTH and APIKEY headers!!!
		@SuppressWarnings("unchecked")
		Map<String,List<String>> rawHeaders = 
			(Map<String,List<String>>) messageContext.get(MessageContext.HTTP_REQUEST_HEADERS);
		Map<String,List<String>> headers = new HashMap<String,List<String>>();
		for(String h : rawHeaders.keySet() ) {	
			String lh = h.toLowerCase();
			headers.put( lh, new ArrayList<String>( rawHeaders.get(h) ) );
			Finn.getLogger().fine(lh+"="+headers.get(lh));
		} 
		// TODO - check headers exist!
		if ( !headers.containsKey(Finn.SERVER_AUTH_HEADER) ) {
			Finn.getLogger().severe("Missing SERVER_AUTH_HEADER");
			return new StreamSource(new StringReader(
				"<finn>" + "ERROR" + "</finn>"
			));
		}
		if ( !headers.containsKey(Finn.SERVER_APIKEY_HEADER) ) {
			Finn.getLogger().severe("Missing SERVER_APIKEY_HEADER");
			return new StreamSource(new StringReader(
				"<finn>" + "ERROR" + "</finn>"
			));
		}
		String response = "";
		if ( !validRequest(headers) ) {
			Finn.getLogger().warning("Invalid auth or apikey found");
			response = "ERROR";
		} else {
			Finn.getLogger().info("path="+path);
			response =  handleQuery(path,query);	
		}
		System.out.println("response="+response);
        return new StreamSource(new StringReader( "<finn>"+response+"</finn>" ));
    }

	private boolean validRequest(Map<String,List<String>> headers) {
		String auth = headers.get(Finn.SERVER_AUTH_HEADER).get(0);
		String apikey = headers.get(Finn.SERVER_APIKEY_HEADER).get(0);
		Finn.getLogger().fine("auth="+auth);
		Finn.getLogger().fine("apikey="+apikey);
		Finn.getFinn().setConfig(Finn.SERVER_AUTH_HEADER,auth);
		Finn.getFinn().setConfig(Finn.SERVER_APIKEY_HEADER,apikey);
		return true;
	}

	public static final String PATH_STATUS = "status";
	public static final String PATH_GET = "get";
	public static final String PATH_SET = "set";
	public static final String PATH_LOG = "log";
	
	public static final String PATH_DISCOVERY = "finn";
	/// Valid path's
	/// /status 	Returns finn status information
	/// /get?name=someName&name=someOtherName
	/// /set?name=value&name2=value2&....
	private String handleQuery(String path, String query)
	{
		// parse query
		String normalizedPath = path; //.toLowerCase();
		if ( normalizedPath.equals(PATH_STATUS) ) {
			return handleStatus(query);
		} else if ( normalizedPath.equals(PATH_DISCOVERY) ) {
			return handleDiscovery(query);
		} else if ( normalizedPath.equals(PATH_SET) ) {
			return handleSet(query);
		} else if ( normalizedPath.equals(PATH_GET) ) {
			return handleGet(query);
		} else if ( normalizedPath.equals(PATH_LOG) ) {
			return handleLog(query);
		} else {
			return "ERROR: Invalid path '"+path+"'";
		}
	}	
	private static Map<String,List<String>> parseQuery(String query) {
		Map<String,List<String>> map = new HashMap<String,List<String>>();
		String[] parts = query.split("&");
		for(String part : parts) {
			String[] pp = part.split("=");
			if ( map.containsKey(pp[0]) ) {
				List<String> vals = map.get(pp[0]);
				vals.add(pp[1]);
			} else {
				List<String> vals = new ArrayList<String>();
				vals.add(pp[1]);
				map.put(pp[0],vals);
			}
		}
		return map;
		
	}
	/// This is where we return the meta-data about this finn
	/// back to HS - not sure yet what all needs to be in here
	/// Perhaps we can read this from some external configuration file
	private String handleDiscovery(String query) {
		Map<String,String> map = new HashMap<String,String>();
		map.put("physical-location","One North, Room 3312, Franklin Group Practice");
		map.put("customer-id","19129-w994-1202934j0s9e");
		map.put("install-date","2014-09-12");
		// ....
		return mapToJSON(map);
	}
	private String mapToJSON(Map<String,String> map) {
		Object[] keys = map.keySet().toArray();

		StringBuilder sb = new StringBuilder();
		sb.append("{");
		for (int i=0; i<keys.length; i++) {
			String key = keys[i].toString();
			sb.append(quote(key)).append(":").append(quote(map.get(key)));
			if ( i<(keys.length-1) ) { sb.append(", "); }
		}
		sb.append("}");
		return sb.toString();
	}

	private String quote(String s) { return "\""+s+"\""; }
	private String handleLog(String query) {
		java.util.logging.Handler[] handlers = Finn.getLogger().getHandlers();
		for(java.util.logging.Handler h : handlers) {
			h.flush();
		}
		try {
			java.nio.file.Path path = java.nio.file.FileSystems.getDefault().getPath(".","finn.log");
			return new String(java.nio.file.Files.readAllBytes(path));
		} catch (Exception e) {
			return e.getMessage();
		}
	}

	private String handleStatus(String query) {
	   	StringBuilder sb = new StringBuilder();
		sb.append("STATUS\n");
		Map<String,Object> config = Finn.getFinn().getConfiguration();
		for (String name : config.keySet() ) {
			sb.append(name+"="+config.get(name)+"\n");
		}	
		return sb.toString();
	}

	private String handleGet(String query) {
		try {
			StringBuilder response = new StringBuilder();
			response.append("OK\n");
			Map<String,List<String>> map = parseQuery(query);
			for(String key : map.keySet() ) {
				for(String name : map.get(key) ) {
					Object o = Finn.getFinn().getConfig(name);
					response.append("GET: "+name+"="+o+"\n");
				}
			}
			return response.toString();
		} catch (Exception e) {
			return "ERROR: " + e.getMessage();
		}
	}
	/// look for special names and set them into the Finn runtime
	private String handleSet(String query) {
		try {
			StringBuilder response = new StringBuilder();
			response.append("OK\n");
			Map<String,List<String>> map = parseQuery(query);
			for (String key : map.keySet() ) {
				for(String val : map.get(key) ) {
					if ( key.startsWith("x-finn") ) {
						// TODO - warn if we already have a value of key??
						Finn.getFinn().setConfig(key,val);
						response.append("SET: " +key+"="+val+"\n");
					}
				}
			}
			return response.toString();
		} catch (Exception e) {
			return "ERROR: " + e.getMessage();
		}
	}
	public static String readInputStreamAsString(InputStream in) 
    	throws IOException {

    	BufferedInputStream bis = new BufferedInputStream(in);
    	ByteArrayOutputStream buf = new ByteArrayOutputStream();
    	int result = bis.read();
    	while(result != -1) {
      		byte b = (byte)result;
      		buf.write(b);
      		result = bis.read();
    	}        
   	 	return buf.toString();
	}

	public void run() {
		Finn.getFinn().register("server",this);
		System.out.println("Server run()");
		Finn.getLogger().info("Server run()");
		String ip = "127.0.0.1";
		try {
				ip = InetAddress.getLocalHost().getHostAddress();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		// TODO Inject port from configuration
		// OR - dynamically bind to open port
		String port = Finn.getFinn().getConfigString(Finn.SERVER_PORT_KEY);
        String address = "http://"+ip+":"+port+"/";
		Finn.getLogger().info("Server starting at " + address);
        Endpoint.create(HTTPBinding.HTTP_BINDING, new Server()).publish(address);

        Finn.getLogger().info("Server running at " + address);

		while ( !Thread.currentThread().isInterrupted() ) {
   			try {
				Thread.sleep(1000);
			} catch (InterruptedException ie) {
				Finn.getLogger().info("Server Interrupted");
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		Finn.getLogger().info("Server shutdown");
    }
}
