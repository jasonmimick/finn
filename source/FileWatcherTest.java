import static java.nio.file.StandardWatchEventKinds.*;
import java.nio.file.WatchService;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.Path;
import java.nio.file.FileSystems;
import java.nio.file.*;

public class FileWatcherTest {

	public static void main(String[] args) {
		String directory = args[0];
		try {
			WatchService watcher = FileSystems.getDefault().newWatchService();
			Path path = FileSystems.getDefault().getPath(directory);
			WatchKey key = path.register(watcher,
								ENTRY_CREATE,ENTRY_DELETE,
								ENTRY_MODIFY);
			while ( true ) {

				key = watcher.take();
				for(WatchEvent<?> event : key.pollEvents()) {
					WatchEvent.Kind<?> kind = event.kind();

					if ( kind == OVERFLOW ) {
						System.out.println("OVERFLOW event");
						continue;
					}
					WatchEvent<Path> ev = 
						(WatchEvent<Path>)event;
					System.out.println(kind);
					System.out.println(ev.context());
					if ( !key.reset() ) {
						break;
					}
				}
			}
		} catch (Exception e) {
			System.err.println(e);
		}
	}
}
