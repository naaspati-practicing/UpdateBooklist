package sam.books;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.thedeanda.lorem.LoremIpsum;

class WrapSerializerTest {

	@Test
	void test() throws IOException {
		
		Path p = Files.createTempFile("sam-", null);
		try {
			WrapSerializer ws = new WrapSerializer();
			System.out.println(p);
			
			ws.write(Collections.emptyList(), p);
			
			List<FileWrap> wraps = ws.read(p);
			assertTrue(wraps.isEmpty(), "wraps is not empty");

			LoremIpsum lorem = LoremIpsum.getInstance();
			Random r = new Random();
			List<FileWrap> expected = Stream.generate(() -> new FileWrap(lorem.getWords(2, 5), r.nextBoolean(), r.nextLong())).limit(1000).collect(Collectors.toList()); 
			
			ws.write(expected, p);
			List<FileWrap> actual = ws.read(p);
			
			assertEquals(expected.size(), actual.size());
			
			for (int i = 0; i < expected.size(); i++) {
				FileWrap e = expected.get(i);
				FileWrap a = actual.get(i);
				
				assertNotSame(e, a);
				assertEquals(e.subpath(),      a.subpath());
				assertEquals(e.isDir,          a.isDir);
				assertEquals(e.lastModified(), a.lastModified());
				assertEquals(e.name(),         a.name());
				assertEquals(e.path(),         a.path());
			}
			Map<Boolean, Long> counts = actual.stream().collect(Collectors.partitioningBy(d -> d.isDir, Collectors.counting()));
			System.out.println("dirs: "+counts.get(true)+", files: "+counts.get(false));
		} finally {
			Files.deleteIfExists(p);
		}
		
		
	}

}
