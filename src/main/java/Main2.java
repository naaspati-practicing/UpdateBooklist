import java.nio.file.Paths;
import java.util.Collections;

import sam.books.AboutBookExtractor;
import sam.books.NewBook;

public class Main2 {

	public static void main(String[] args) {
		new AboutBookExtractor(Collections.singletonList(new NewBook("file_name", Paths.get("path"), 1000))).getResult();
	}

}
