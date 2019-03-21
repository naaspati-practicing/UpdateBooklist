package sam.books.pathwrap;

import java.io.File;
import java.io.Writer;
import java.nio.file.Path;

import org.json.JSONObject;

import sam.string.StringWriter2;

public class PathWrap {
	final int id;
	public final Dir parent; 
	public final String name;

	private Path fullpath, subpath;
	private File fullpathFile, subpathFile;

	protected PathWrap(int id, Dir parent, String name) {
		this.id = id;
		this.parent = parent;
		this.name = name;
	}

	//for root
	protected PathWrap(int id, Dir parent, String name, Path fullpath, Path subpath, File fullpathFile, File subpathFile) {
		this.id = id;
		this.parent = parent;
		this.name = name;
		this.fullpath = fullpath;
		this.subpath = subpath;
		this.fullpathFile = fullpathFile;
		this.subpathFile = subpathFile;
	}

	private File file(File file) {
		return new File(file, name);
	}
	private Path path(Path file) {
		return file.resolve(name);
	}

	public File fullpathFile() {
		if(fullpathFile == null)
			fullpathFile = file(parent.fullpathFile());

		return fullpathFile;
	}
	public File subpathFile() {
		if(subpathFile == null) {
			if(parent == Dir.ROOT)
				subpathFile = new File(name);
			else
				subpathFile = file(parent.subpathFile());
		}
		return subpathFile;
	}
	public Path fullpath() {
		if(fullpath == null)
			fullpath = path(parent.fullpath());

		return fullpath;
	}
	public Path subpath() {
		if(subpath == null)
			subpath = path(parent.subpath());

		return subpath;
	}

	private int isDir = -1;
	public boolean isDir() {
		if(isDir == -1)
			isDir = fullpathFile().isDirectory() ? 1 : 0;

		return isDir == 1;
	}
	private int exists = -1;
	public boolean exists() {
		if(exists == -1)
			exists = fullpathFile().exists() ? 1 : 0;

		return exists == 1;
	}
	public String name() {
		return name;
	}

	public void toString(Writer sw) {
		new JSONObject()
		.put("name",name())
		.put("isDir",isDir())
		.put("lastModified",lastModified())
		.put("subpath",subpath())
		.write(sw, 4, 0);
	}
	
	private long lastmod = -1;

	private long lastModified() {
		if(lastmod == -1)
			lastmod = fullpathFile().lastModified();
		return lastmod;
	}

	@Override
	public String toString() {
		StringWriter2 sw = new StringWriter2();
		toString(sw);
		return sw.toString();
	}
}
