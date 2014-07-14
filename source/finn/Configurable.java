package finn;
import java.util.Set;
public interface Configurable {
	// Notify this guy of a configuration value
	public void setProperty(String name, Object value);
}
