package sam.books;

import static sam.books.BooksDBMinimal.ROOT;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.json.JSONObject;

import sam.string.StringWriter2;

class FileWrap {
	public static final int ROOT_NAMECOUNT = ROOT.getNameCount();
	
	private Path path;
	private long lastModified;
	private String name;

	public FileWrap(Path p) {
		this.path = p;
		this.name = path.getFileName().toString();
		this.lastModified = readLasmodified();
	}
	private long readLasmodified() {
		return path().toFile().lastModified();
	}
	
	public FileWrap(Path fullpath, Path subpath, long lastModified) {
		this.path = fullpath;
		this.subpath = subpath;
		this.lastModified = lastModified;
	}

	public FileWrap(String subpath, long lastModified) {
		this.subpath = Paths.get(subpath);
		this.lastModified = lastModified;
	}
	public boolean isDir() {
		return false;
	}
	

	public void toString(Writer sw) {
		new JSONObject()
				.put("name",name())
				.put("isDir",isDir())
				.put("lastModified",lastModified())
				.put("subpath",subpath())
				.write(sw, 4, 0);
	}
	
	@Override
	public String toString() {
		StringWriter2 sw = new StringWriter2();
		toString(sw);
		return sw.toString();
	}

	public Path path() {
		if(path == null)
			path = ROOT.resolve(subpath);
		
		return path;
	}
	public String name() {
		if(name == null)
			name = subpath.getFileName().toString();
		
		return name;
	}

	private Path subpath;
	public Path subpath() {
		if(subpath == null)
			subpath = path.subpath(ROOT_NAMECOUNT, path.getNameCount());
		
		return subpath;
	}

	public long lastModified() {
		return lastModified;
	}

	private long new_lastmodified = -1;
	public boolean isModified() {
		if(new_lastmodified == -1)
			new_lastmodified = readLasmodified();
		return lastModified != new_lastmodified;
	}

	private int exists = -1;
	public boolean exists() {
		if(exists == -1)
			exists = Files.exists(path()) ? 1 : 0;
		
		return exists == 1;
	}
	private boolean found = false;
	public void found(boolean found) {
		this.found = found;
	}
	public boolean found() {
		return found;
	}
}