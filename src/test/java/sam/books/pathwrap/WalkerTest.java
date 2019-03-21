package sam.books.pathwrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

class WalkerTest {

	@Test
	void test() throws IOException {
		Walker w = new Walker(Paths.get("D:\\Core Files\\Adobe\\ProgrammingComputer Books\\non-book materials\\Booklist app\\booklist_update\\ignore-subpaths.txt"));
		long time = System.currentTimeMillis();
		Dir dir = w.walkRootDir();
		Set<Path> skipFiles = w.getSkipFiles();
		System.out.println(System.currentTimeMillis() - time);

		Map<Path, PathWrap> actual = new HashMap<>();
		collect(dir, actual);

		final Path root = dir.fullpath();
		int namecount = root.getNameCount();

		time = System.currentTimeMillis();
		ArrayList<Path> expected = new ArrayList<>(actual.size());
		Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
			boolean first = true;

			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				if(first) {
					first = false;
					return FileVisitResult.CONTINUE;
				} else
					return put(dir, true);
			}
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				return put(file, false);
			}

			private FileVisitResult put(Path file, boolean dir) {
				Path subpath = file.subpath(namecount, file.getNameCount());
				if(skipFiles.contains(subpath)) {
					System.out.println("skipped: "+subpath);
					return dir ? FileVisitResult.SKIP_SUBTREE : FileVisitResult.CONTINUE;
				}
				expected.add(file);
				return FileVisitResult.CONTINUE;
			}
		});

		assertEquals(expected.size(), actual.size());

		for (Path full : expected) {
			Path subpath = full.subpath(namecount, full.getNameCount());
			PathWrap p = actual.get(subpath);

			assertNotNull(p, () -> full.toString());

			assertEquals(full, p.fullpath());
			assertEquals(full.toFile(), p.fullpathFile());
			assertEquals(subpath, p.subpath());
			assertEquals(subpath.toFile(), p.subpathFile());
			assertEquals(full.getFileName().toString(), p.name);
		}
		
		System.out.println(System.currentTimeMillis() - time);
		System.out.println("size: "+expected.size());
	}

	private void collect(Dir dir, Map<Path, PathWrap> paths) {
		dir.forEach(e -> {
			paths.put(e.subpath(), e);
			if(e.isDir())
				collect((Dir)e, paths);
		});
	}

	private void print(Dir dir) {
		System.out.println(dir.subpath());

		for (PathWrap p : dir) 
			System.out.println("  "+p.subpath());

		System.out.println();

		for (PathWrap p : dir) {
			if(p.isDir())
				print((Dir)p);
		}

	}

}
