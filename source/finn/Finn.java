package finn;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.logging.FileHandler;

/// Singleton - finn runtime.
public class Finn implements Runnable {

	/* override default LogManager so that worker threads
	   can log stuff on shutdown
	*/
	public static class FinnLogManager extends java.util.logging.LogManager {
		@Override
		public void reset() throws SecurityException {
			System.out.println("FinnLogManager reset() called");
	         //super.reset();
       	}
    }
	static {
		System.setProperty("java.util.logging.manager", FinnLogManager.class.getName());
	}
	private Map<String,Object> Configuration;
	public static String VERSION = "0.1";
	public static String APIKEY = "123456789abcdefghijklmnopqrstuvwxyz";
	public static String SERVER_AUTH_HEADER = "x-finn-auth";
	public static String SERVER_APIKEY_HEADER = "x-finn-apikey";
	public static String SERVER_PORT_KEY = "x-finn-server-port";
	protected List<Thread> Workers;

 
	public static Logger getLogger() {
		return getFinn().logger;
	}
	protected Logger logger = Logger.getLogger(Finn.class.getName());

	private static Finn FINN = new Finn();
	public static Finn getFinn() { 
		return FINN;
	}

	private Map<String,Configurable> Configurables;
	
	/// Called to register a configurable object to receive
	/// notifications on configuration changes
	public void register(String name,Configurable c) {
		this.Configurables.put(name,c);
	}
	public void setConfig(String k, Object v) {
		this.Configuration.put(k,v);	
		logger.fine("setConfig("+k+","+v+")");
		for(Configurable c : this.Configurables.values()) {
			c.setProperty(k,v);
		}
	}

	/// Returns a copy of finn's configuration
	public Map<String,Object> getConfiguration() {
		return new HashMap<String,Object>(this.Configuration);
	}

	public Object getConfig(String key) {
		String val = "";
		if ( this.Configuration.containsKey(key) ) {
			return this.Configuration.get(key);
		}
		return null;
	}
	public String getConfigString(String key) {
		Object o = getConfig(key);
		if ( o != null ) {
			return o.toString();
		}
		return new String();
	}

	private Finn() {
		this.Configuration = new HashMap<String,Object>();
		this.Configurables = new HashMap<String,Configurable>();
		this.Configuration.put(SERVER_PORT_KEY,"8080");
		setupLogger();
	}

	private static boolean running = false;
	public void run() {
		logger.info("FINN Startup");
		if ( Finn.running ) {
			throw new RuntimeException("Finn already running");
		}
		Finn.running = true;
		this.Workers= new ArrayList<Thread>();
		this.Workers.add( new Thread(new Sender(),"Sender") );
		this.Workers.add( new Thread(new Server(),"Server") );
		this.Workers.add( new Thread(new UPNPBroadcaster(),"UPNPBroadcaster") );

		for(Thread worker : this.Workers) {
			System.out.println("About to start worker = " + worker);
			worker.start();
		}
		/*
		if ( java.awt.SystemTray.isSupported() ) {
			UserInterface.showSystemTray();	
		}
		*/
		while (true) {
			try {
				Thread.sleep(1000*60*60);
				logger.info("finn up");
			} catch (Exception e) {
				logger.fine(e.getMessage());
				Finn.running = false;
				System.exit(0);
			}
		}
	}
	private void setupLogger() {
		try {
			FileHandler fh = new FileHandler("finn.log");
			fh.setFormatter( new java.util.logging.SimpleFormatter() );
			logger.addHandler( fh );
			logger.info("logger setup");
		} catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
        } catch (SecurityException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
		if ( this.debug ) {
			logger.setLevel(Level.FINEST);
		} else {
			logger.setLevel(Level.WARNING);
		}
		logger.info("Logger level="+logger.getLevel());
	}
	private boolean debug = false;
	public boolean getDebug() { return this.debug; }

	static Thread ShutdownHook = new Thread() {
		public void run() {
			Finn.getLogger().warning("Got shutdown workers.count="+Finn.getFinn().Workers.size());	
			for (Thread worker : Finn.getFinn().Workers) {
				Finn.getLogger().info("interrupt to " + worker);
				worker.interrupt();
			}
		}
	};

	private static boolean hasSwitch(String[] args,String value) {
		for(String arg : args) {
			if ( arg.toLowerCase().equals( value ) ) {
				return true;
			}
		}
		return false;
	}



	public static void main(String[] args) {
		if ( hasSwitch(args,"debug") ) {
			Finn.getFinn().setDebug(true);
		}	
		Runtime.getRuntime().addShutdownHook( Finn.getFinn().ShutdownHook );
		// TODO For deployment, since it will be windoze, we should
		// add some system-tray thingy to monitor
		Finn.getFinn().run();
	}
}
