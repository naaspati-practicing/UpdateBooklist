package sam.books;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Dir extends FileWrap implements Iterable<FileWrap> {
	private int id = -1;
	private final Map<Path, FileWrap> children;
	
	public Dir(Path fullpath, Path subpath, long lastModified, Map<Path, FileWrap> children) {
		super(fullpath, subpath, lastModified);
		this.children = children;
	}
	public Dir(Path p) {
		super(p);
		this.children = new HashMap<>();
	}
	public Dir(String subpath, long lastModified) {
		super(subpath, lastModified);
		this.children = new HashMap<>();
	}
	public void id(int id) {
		if(id != -1)
			throw new IllegalAccessError();
		this.id = id;
	}
	public int id() {
		return id;
	}
	@Override
	public boolean isDir() {
		return true;
	}
	@Override
	public Iterator<FileWrap> iterator() {
		return children.values().iterator();
	}

}
