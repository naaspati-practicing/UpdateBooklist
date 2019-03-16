package sam.books;

import static sam.books.BooksDBMinimal.ROOT;
import static sam.console.ANSI.cyan;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sam.collection.ArraysUtils;
import sam.myutils.Checker;
import sam.reference.WeakPool;

public class Walker {
	private final Logger logger = LoggerFactory.getLogger(Walker.class);
	private final Set<Path> skipFiles;
	
	public Walker(Path skipFilePath) throws IOException {
		this.skipFiles = skipFiles(skipFilePath);
	}

	public static void walk(Iterable<? extends FileWrap> children, Consumer<FileWrap> consumer) {
		children.forEach(f -> {
			consumer.accept(f);
			if(f.isDir())
				walk((Dir)f, consumer);
		});
	}
	public static void walk(Dir dir, Consumer<FileWrap> consumer) {
		dir.forEach(f -> {
			consumer.accept(f);
			if(f.isDir())
				walk((Dir)f, consumer);
		});
	}

	public List<Dir> walkRootDir() throws IOException {
		logger.info(cyan("walking"));

		File root = ROOT.toFile();
		List<Dir> dirs = new ArrayList<>();

		for (String s : root.list()) {
			Path fullpath = ROOT.resolve(s); 
			Path subpath = Paths.get(s);

			if(skipFiles.contains(subpath) || !Files.isDirectory(fullpath)) 
				logger.info("skip: "+s);
			else {
				FileWrap[] list = walk(fullpath, subpath);
				dirs.add(new Dir(s, fullpath, subpath, Files.getLastModifiedTime(fullpath).toMillis(), list));
			}
		} 
		return dirs;
	}

	private FileWrap[] walk(Path fullpath, Path subpath) throws IOException {
		String[] files = fullpath.toFile().list();
		if(Checker.isEmpty(files))
			return Dir.EMPTY;

		FileWrap[] children = new FileWrap[files.length]; 
		for (int i = 0; i < files.length; i++) 
			children[i] = newInstance(files[i], fullpath, subpath);
		
		return ArraysUtils.removeIf(children, e -> e == null);
	}

	private FileWrap newInstance(String name, Path parent_fullpath, Path parent_subpath) throws IOException {
		final Path p = Paths.get(name);
		final Path sp = parent_subpath.resolve(p);

		if(skipFiles.contains(sp)) {
			logger.debug("skip: "+sp);
			return null;
		}

		final Path fp = parent_fullpath.resolve(p);
		BasicFileAttributes attrs = Files.readAttributes(fp, BasicFileAttributes.class);

		if(attrs.isDirectory()) {
			FileWrap[]  children = walk(fp, sp);
			return new Dir(name, fp, sp, attrs.lastModifiedTime().toMillis(), children);
		} else  {
			return new FileWrap(name, fp, sp, attrs.lastModifiedTime().toMillis());
		}
	}
	public static Set<Path> skipFiles(Path skipFilePath) throws IOException {
		Set<Path> skipfiles = Files.notExists(skipFilePath) ? Collections.emptySet() : Files.lines(skipFilePath).filter(Checker::isNotEmptyTrimmed).filter(s -> s.charAt(0) != '#').map(Paths::get).collect(Collectors.collectingAndThen(Collectors.toList(), s -> {
			if(s.isEmpty())
				return Collections.emptySet();
			else if(s.size() == 1)
				return Collections.singleton(s.get(0));
			else
				return new HashSet<>(s);
		}));
		return skipfiles;
	}

	private final  WeakPool<Map<String, FileWrap>> mapPool = new WeakPool<>(() -> new HashMap<>());

	public FileWrap[] update(Dir dir, FileWrap[] children) throws IOException {
		String[] array = dir.path().toFile().list();
		if(Checker.isEmpty(array)) {
			return Dir.EMPTY;
		} else {
			Map<String, FileWrap> map;
			if(Checker.isEmpty(children)) 
				map = Collections.emptyMap();
			else {
				map = mapPool.poll();
				map.clear();
				for (FileWrap c : children)
					map.put(c.name(), c);
			}
			
			children = children == null || children.length != array.length ? new FileWrap[array.length] : children;
			for (int i = 0; i < array.length; i++) {
				String s = array[i];
				FileWrap file = map.get(s);
				if(file == null) {
					 file = newInstance(s, dir.path(), dir.subpath());
					 logger.debug("new instance: {}", file);
				} else {
					if(file.isDir())
						((Dir)file).update(this);
				}
				children[i] = file;
			}
			
			return ArraysUtils.removeIf(children, e -> e == null); 
		}
	}
}
