package sam.books;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Consumer;

public class Dir extends FileWrap {
	public static final FileWrap[] EMPTY = new FileWrap[0];
	
	private int id = -1;
	private FileWrap[] children;
	private int index;
	
	public Dir(String name, Path fullpath, Path subpath, long lastModified, FileWrap[] children) {
		super(name, fullpath, subpath, lastModified);
		this.children = children;
	}
	
	public Dir(String name, Path subpath, long lastModified, FileWrap[] children) {
		super(name, subpath, lastModified);
		this.children = children;
	}
	public void add(FileWrap file) {
		if(children[index] != null)
			throw new IllegalStateException();
		
		children[index++] = file;
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
	
	public void forEach(Consumer<? super FileWrap> action) {
		for (FileWrap f : children) 
			action.accept(f);
	}
	public int deepCount() {
		int n = children.length;
		
		for (FileWrap f : children) {
			if(f.isDir())
				n += ((Dir)f).deepCount();
		}
		
		return n;
	}
	public int count() {
		return children.length;
	}
	public int update(Walker walker) throws IOException {
		int mod = 0;
		
		if(isModified()) {
			this.children = walker.update(this, children);
			mod++;
		} else {
			for (FileWrap f : children) {
				if(f.isDir())
					mod += ((Dir)f).update(walker);
			}
		}
		
		return mod;
	}
}
