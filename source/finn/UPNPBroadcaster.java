package finn;
import java.util.Set;
import java.io.*;
import java.net.*;
import java.util.Map;
import java.util.HashMap;
import java.util.Enumeration;
import java.util.Collections;
import java.net.InetAddress;
class UPNPBroadcaster implements Configurable,Runnable 
{
   public String Version = "0.0.1";
   public String VersionDate = "Jul-04-2014";
   public int Port = 1900;
   public String BroadcastAddr = "239.255.255.250";

   public DatagramSocket ClientSocket;
   public Map<String,String> DiscoveryResponse;
   public UPNPBroadcaster() {
	String port = Finn.getFinn().getConfigString(Finn.SERVER_PORT_KEY);
	String ip="127.0.0.1";
	try {
		ip = InetAddress.getLocalHost().getHostAddress();
	} catch ( java.net.UnknownHostException uhe ) {
		Finn.getLogger().warning(uhe.getMessage());
		Finn.getLogger().warning("UPNPBroadcast using "+ip+" for it's ip");
	}
	String serviceURL = "http://"+ip+":"+port+"/finn";
	Finn.getLogger().info("UPNPBroadcast location:"+serviceURL);
	this.DiscoveryResponse = new HashMap<String,String>();
	this.DiscoveryResponse.put("cache-control","max-age = 60");
	this.DiscoveryResponse.put("location",serviceURL);
    this.DiscoveryResponse.put("server","HealthShare FINN Server UPnP.1.0 " + this.Version + " " + this.VersionDate);
	this.DiscoveryResponse.put("st","upnp:rootdevice");
	this.DiscoveryResponse.put("usn","uuid:InterSystems-HealthShare-Finn-1_0::upnp:rootdevice");	
	
   }

   public void broadcast() throws Exception
   {
		InetAddress addr = InetAddress.getByName(this.BroadcastAddr);
       	//this.ClientSocket = new DatagramSocket(this.Port); 
		byte[] sendData;
		String s = "NOTIFY * HTTP/1.1\n";
		for(String key : this.DiscoveryResponse.keySet()) {
		 s += key.toUpperCase() + ": ";
		 s += this.DiscoveryResponse.get(key) + "\n";
		}
		//System.out.println(s);
		sendData = s.getBytes();
		byte[] receiveData = new byte[1024];
		while ( this.shouldBroadcast ) {
			// wait for a request
      		DatagramPacket dp = new DatagramPacket(receiveData, receiveData.length);
			Finn.getLogger().fine("About to receive()");
			MulticastSocket mSocket = new MulticastSocket(this.Port);
			mSocket.joinGroup(addr);
			mSocket.receive(dp);
      		//this.ClientSocket.receive(receivePacket);
      		String modifiedSentence = new String(dp.getData(),dp.getOffset(), dp.getLength());
      		Finn.getLogger().fine("GOT REQUEST:" + modifiedSentence.replaceAll("\\P{InBasic_Latin}", ""));
	  		Finn.getLogger().fine("Request from: " + dp.getAddress());
			if ( modifiedSentence.contains("M-SEARCH") ) {
			
	   			// respond with our discovery info
				NetworkInterface netIf = getActiveNetworkInterface();
      			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, 
					new InetSocketAddress(this.BroadcastAddr,
									      this.Port));
				//this.ClientSocket = new DatagramSocket(null);
				//this.ClientSocket.setReuseAddress(true);
				//this.ClientSocket.bind(new InetSocketAddress(
				//		getLocalV4Address(netIf)	
				//	,this.Port));
				Finn.getLogger().fine("Sending: " + sendPacket);
      			//this.ClientSocket.send(sendPacket);
      			mSocket.send(sendPacket);
				Thread.sleep(5000);
			}
		}
		this.ClientSocket.close();
   }
   public Boolean shouldBroadcast = true;

	
   	public void setProperty(String name, Object value) {
		Finn.getLogger().fine("notify name="+name+" value="+value);
	}


   public void run() {
		Finn.getFinn().register("upnpbroadcaster",this);
		UPNPBroadcaster ub = new UPNPBroadcaster();
		while ( !Thread.currentThread().isInterrupted()) {
			try {
				Finn.getLogger().info("UPNPBroadcaster starting");
				ub.broadcast();
			} catch (InterruptedException ie) {
				Finn.getLogger().info("UPNPBroadcaster Interrupted");
			} catch (Exception e) {
				Finn.getLogger().severe(e.getMessage());
				throw new RuntimeException(e);
			} finally {
				ub.shouldBroadcast = false;
				System.out.println("UPNPBroadcaster got interuppeted!");
			}
		}
	}
   public static void main(String args[]) throws Exception
   {
	 UPNPBroadcaster ub = new UPNPBroadcaster();
	 try {
		ub.broadcast();
	 } catch (Exception e) {
		System.out.println(e);
	 } finally {	
		ub.shouldBroadcast = false;
	 }
   }

	public static NetworkInterface getActiveNetworkInterface() {
		Enumeration<NetworkInterface> interfaces = null;
        try {
            interfaces = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            return null;
        }

        while (interfaces.hasMoreElements()) {
            NetworkInterface iface = interfaces.nextElement();
			
			//displayInterfaceInformation(iface);
			Enumeration<InetAddress> inetAddresses = iface.getInetAddresses();

            /* Check if we have a non-local address. If so, this is the active
             * interface.
             *
             * This isn't a perfect heuristic: I have devices which this will
             * still detect the wrong interface on, but it will handle the
             * common cases of wifi-only and Ethernet-only.
             */
            while (inetAddresses.hasMoreElements()) {
                InetAddress addr = inetAddresses.nextElement();

                if (!(addr.isLoopbackAddress() || addr.isLinkLocalAddress())) {
                    return iface;
                }
            }
		}	
		return null;
	}

 public static InetAddress getLocalV4Address(NetworkInterface netif)
    {
        Enumeration addrs = netif.getInetAddresses();
        while (addrs.hasMoreElements())
        {
            InetAddress addr = (InetAddress) addrs.nextElement();
            if (addr instanceof Inet4Address && !addr.isLoopbackAddress())
                return addr;
        }
        return null;
    }
static void displayInterfaceInformation(NetworkInterface netint) {
        System.out.printf("Display name: %s\n", netint.getDisplayName());
        System.out.printf("Name: %s\n", netint.getName());
        Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();
        for (InetAddress inetAddress : Collections.list(inetAddresses)) {
            System.out.printf("InetAddress: %s\n", inetAddress);
        }
        System.out.printf("\n");
     }
}
