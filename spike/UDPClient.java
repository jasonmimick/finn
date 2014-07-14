import java.io.*;
import java.net.*;

class UDPClient
{
   public static void main(String args[]) throws Exception
   {
      BufferedReader inFromUser =
         new BufferedReader(new InputStreamReader(System.in));
	  int port = 1900;
      DatagramSocket clientSocket = new DatagramSocket(port);
      InetAddress IPAddress = InetAddress.getByName("239.255.255.250");
      byte[] sendData = new byte[1024];
      byte[] receiveData = new byte[1024];
	  String sentence="";
	  for (int i=0; i<5; i++) {
      	sentence += inFromUser.readLine() + "\n";
	  }
	  System.out.println(sentence);
      sendData = sentence.getBytes();
      DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, 1900);
      clientSocket.send(sendPacket);
	  for (int i=0; i<100; i++) {
      DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
      clientSocket.receive(receivePacket);
      String modifiedSentence = new String(receivePacket.getData());
      System.out.println("FROM SERVER:" + modifiedSentence);
	  }
      clientSocket.close();
   }
}
