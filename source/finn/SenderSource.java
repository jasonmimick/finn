package finn;

public interface SenderSource extends Configurable,Runnable {

	/// Called by the Sender to register itself with
	/// This source - the source then should invoke
	/// send(SenderSource source, Object o) with the data to send off.
	public void register(Sender sender);	

	/// The source will get called back with the object to
	/// send - this allows the source to control the output formatting 
	public byte[] getData(String id, Object o);

	public boolean needsResponse();
}
