package finn;
import java.io.*;
import java.net.*;
import java.util.Map;
import java.util.HashMap;
import java.util.Enumeration;
import java.util.Collections;

public class UPNPUtils {

	public static NetworkInterface getActiveNetworkInterface() {
        Enumeration<NetworkInterface> interfaces = null;
        try {
            interfaces = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            return null;
        }

        while (interfaces.hasMoreElements()) {
            NetworkInterface iface = interfaces.nextElement();

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
}
