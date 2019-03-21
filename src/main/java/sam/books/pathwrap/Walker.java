package sam.books.pathwrap;

import static sam.console.ANSI.cyan;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sam.books.pathwrap.Dir;
import sam.books.pathwrap.PathWrap;
import sam.myutils.Checker;

public class Walker {
	private static final Logger logger = LoggerFactory.getLogger(Walker.class);
	private final Set<Path> skipFiles;

	public Walker(Path skipFilePath) throws IOException {
		this.skipFiles = skipFiles(skipFilePath);
	}

	private int id;
	
	public Dir walkRootDir() throws IOException {
		logger.debug("walking: {}", Dir.ROOT.fullpath());
		this.id = Dir.ROOT.id + 1;
		
		PathWrap[] children = walk(Dir.ROOT);
		Dir.ROOT.setChildren(children);
		return Dir.ROOT;
	}
	
	public static final Dir[] EMPTY_DIRS = new Dir[0]; 

	private PathWrap[] walk(Dir parent) {
		String[] files = parent.fullpathFile().list();

		if(Checker.isEmpty(files))
			return Dir.EMPTY;

		PathWrap[] children = new PathWrap[files.length];

		int n = 0;
		for (String s : files) {
			PathWrap p  = parent.resolve(s, id++);
			
			if(skipFiles.contains(p.subpath()))
				logger.debug("skipped: {}", p.subpath()); 
			else	
				children[n++] = p;
		}
		
		if(n != children.length) {
			logger.debug("array resize: {}, {} -> {}", parent.subpath(), children.length, n);
			children = Arrays.copyOf(children, n);
		}
		
		for (PathWrap p : children) {
			if(p.isDir()) {
				Dir d = (Dir) p;
				d.setChildren(walk(d));
			}
		}
		
		return children;
		
	}
	public Set<Path> getSkipFiles() {
		return skipFiles;
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
		
		if(logger.isDebugEnabled()) {
			StringBuilder sb = new StringBuilder("Skip Files: \n  ");
			skipfiles.forEach(s -> sb.append(s).append("\n  "));
			sb.append('\n');
			logger.debug(sb.toString());
		}
		return skipfiles;
	}
	
	
	public static void walk(Dir dir, Consumer<PathWrap> consumer) {
		dir.forEach(consumer);
		dir.forEach(c -> {
			if(c.isDir())
				walk((Dir)c, consumer);
		});
	}
}
