package finn;
import java.io.*;
import java.net.*;
import java.util.List;
import java.util.ArrayList;
/// Searchs for UPNP devices
/// Responses are sent to each
/// UPNPSearchSink provided when the searcher
/// is created.
public class UPNPSearcher {

	private List<UPNPSearchSink> sinks;
	public UPNPSearcher(List<UPNPSearchSink> sinks) {
		this.sinks = sinks;
	}
	public UPNPSearcher() {
		this.sinks = new ArrayList<UPNPSearchSink>();
	}

	public void addSink(UPNPSearchSink sink) {
		this.sinks.add(sink);
	}
	public void search() {
		try {
			String sr = "M-SEARCH * HTTP/1.1\n";
			sr += "HOST: 239.255.255.250:1900\n";
			sr += "MAN: ssdp:discover\n";
			sr += "MX: 10\n";
			sr += "ST: ssdp:all\n";		
			String addr = "239.255.255.250";
			int port = 1900;
			final InetAddress iaddr = InetAddress.getByName(addr);		
			byte[] srd = sr.getBytes();

			final MulticastSocket socket = new MulticastSocket(port);
            socket.joinGroup(iaddr);
			NetworkInterface netIf = UPNPUtils.getActiveNetworkInterface();
            DatagramPacket packet = new DatagramPacket(srd, srd.length,
					new InetSocketAddress(addr,port));
			System.out.println("Sending search request");
			socket.send(packet);
			// listen for responses
			Thread response = new Thread() {
				public void run() {
					byte[] buf = new byte[1024];
					DatagramPacket rpack = new DatagramPacket(buf,buf.length);
					System.out.println("Waiting for responses");
					try {
						while ( true ) {
							socket.receive(rpack);
							broadcast(rpack);
						}
					} catch (Exception ie) {
						ie.printStackTrace();
						System.out.println("Stopping");
					} finally {
						try {
							socket.leaveGroup(iaddr);
						} catch (Exception e) {
							throw new RuntimeException(e);
						}
					}
				}
			};
			response.start();
			System.out.println(Thread.currentThread().getId() + " leaving search()");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void broadcast(DatagramPacket packet) {

		for(UPNPSearchSink sink : this.sinks) {
			sink.OnDeviceResponse(packet);
		}
	}
	public static void main(String[] args) {
		List<UPNPSearchSink> sinks = new ArrayList<UPNPSearchSink>();
		sinks.add( new UPNPSearchSink() {
			public void OnDeviceResponse(Object o) {
				System.out.println("Got a response : " + o );
				System.out.println(Thread.currentThread().getId());
				DatagramPacket packet = (DatagramPacket) o;
				String s = new String(packet.getData(),0,packet.getLength());
				System.out.println( s );
			}
		});
		sinks.add( new UPNPFileWriterSink("/tmp/finn/device"));
		UPNPSearcher searcher = new UPNPSearcher(sinks);
		searcher.search();			

		System.out.println("Searching thread working....");
		System.out.println("Press any key to stop.");
		try {
			int i = System.in.read();
			//System.out.println("i="+i);
			System.exit(0);
		} catch (Exception e) {
		}
	}
}
