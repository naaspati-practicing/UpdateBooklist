package sam.books.pathwrap;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.Consumer;

import sam.books.BooksDBMinimal;
import sam.collection.ArrayIterator;
import sam.myutils.Checker;
import sam.myutils.ThrowException;

public class Dir extends PathWrap implements Iterable<PathWrap> {
	public static final PathWrap[] EMPTY = new PathWrap[0];
	private static final Path ROOT_PATH = BooksDBMinimal.ROOT; 
	public static final Dir ROOT = new Dir(0, ROOT_PATH.getFileName().toString(), ROOT_PATH, Paths.get(""), ROOT_PATH.toFile(), new File(""));

	private PathWrap[] children;

	private Dir(int id, Dir parent, String name) {
		super(id, parent, name);
	}
	//root dir
	private Dir(int id, String name, Path fullpath, Path subpath, File fullpathFile, File subpathFile) {
		super(id, null, name, fullpath, subpath, fullpathFile, subpathFile);
	}
	public Dir(int id, Dir parent, String name, File fullpathFile) {
		super(id, parent, name, null, null, fullpathFile, null);
	}
	public PathWrap resolve(String name, int id) {
		File file = new File(fullpathFile(), name);
		boolean isDir = file.isDirectory();
		
		if(isDir)
			return new Dir(id, this, Objects.requireNonNull(name), file);
		else
			return new PathWrap(id, this, Objects.requireNonNull(name));
	}
	
	@Override
	public boolean isDir() {
		return true;
	}
	public void setChildren(PathWrap[] children) {
		if(this.children != null)
			ThrowException.illegalAccessError();
		this.children = children;
	}
	public int count() {
		return children.length;
	}
	
	private int path_id = -1;
	public void path_id(int path_id) {
		if(path_id != -1)
			ThrowException.illegalAccessError();
		this.path_id = path_id;
	}
	public int path_id() {
		return path_id;
	}
	@Override
	public Iterator<PathWrap> iterator() {
		if(Checker.isEmpty(children))
			return Collections.emptyIterator();
		else 
			return new ArrayIterator<>(children);
	}
	@Override
	public void forEach(Consumer<? super PathWrap> action) {
		for (PathWrap c : children) 
			action.accept(c);
	}
	@Override
	public Spliterator<PathWrap> spliterator() {
		return Arrays.spliterator(children);
	}
}
