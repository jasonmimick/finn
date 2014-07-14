package finn; 

import java.io.File;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

import uk.co.mmscomputing.device.scanner.Scanner;
import uk.co.mmscomputing.device.scanner.ScannerDevice;
import uk.co.mmscomputing.device.scanner.ScannerListener;
import uk.co.mmscomputing.device.scanner.ScannerIOException;
import uk.co.mmscomputing.device.scanner.ScannerIOMetadata;

public class TwainSenderSource implements ScannerListener,SenderSource {


  Scanner scanner;
  Map<UUID,Object> scans;
  public TwainSenderSource() {
		this.scans = new HashMap<UUID,Object>();

  }
  public void setProperty(String name,Object value) {
		Finn.getLogger().fine("notify name="+name+" value="+value);
  }
  private Sender sender;
	public void register(Sender sender) {
		this.sender = sender;
		Finn.getLogger().info("TwainSourceSender registered sender="+sender);
	}
  public void run() {
	// throws ScannerIOException{
	while ( !Thread.currentThread().isInterrupted() ) {

		try {
    		scanner=Scanner.getDevice();
    		scanner.addListener(this);    
    		scanner.acquire();
			Thread.sleep(10*1000);
		} catch (ScannerIOException sioe) {
			//Finn.getLogger().warning(sioe.getMessage());
		} catch (InterruptedException ie) {
			Finn.getLogger().info("TwainSenderSource got interrupted");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
  }

	public boolean needsResponse() { 
		return true;
	}

  public void update(ScannerIOMetadata.Type type, ScannerIOMetadata metadata){
    if(type.equals(ScannerIOMetadata.ACQUIRED)){
	  String id = UUID.randomUUID().toString();
      Finn.getLogger().info("Have an image now!");
	  String response = this.sender.send(this,id,metadata);
	  Finn.getLogger().info("sent - response ="+response);
	  /*
      try{
        ImageIO.write(image, "png", new File("mmsc_image.png"));
      }catch(Exception e){
        e.printStackTrace();
      }
	  */
    } else if(type.equals(ScannerIOMetadata.NEGOTIATE)){
      ScannerDevice device=metadata.getDevice();
      try{
		Finn.getLogger().info("Got scanner device="+device);
        device.setShowUserInterface(true);
        device.setShowProgressBar(true);
        device.setResolution(100);
      }catch(Exception e){
        e.printStackTrace();
      }
    }else if(type.equals(ScannerIOMetadata.STATECHANGE)){
      System.err.println(metadata.getStateStr());
      if(metadata.isFinished()){
        //System.exit(0);
		Finn.getFinn().getLogger().severe("TwainSenderSource - got isFinished");
		throw new RuntimeException(new InterruptedException());
      }
    }else if(type.equals(ScannerIOMetadata.EXCEPTION)){
      metadata.getException().printStackTrace();
		Finn.getLogger().warning(metadata.getException().getMessage());
    }
  }
	
  /// The source will get called back with the object to
  /// send - this allows the source to control the output formatting 
  public byte[] getData(String id, Object o) {
		try {
			ScannerIOMetadata metadata = (ScannerIOMetadata) o;
      		BufferedImage image=metadata.getImage();
			byte[] imageBytes = ((DataBufferByte) image.getData().getDataBuffer()).getData();
			String data = javax.xml.bind.DatatypeConverter.printBase64Binary(imageBytes);
			Finn.getLogger().info("Got image - data.length()="+data.length());
			return data.getBytes();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}


