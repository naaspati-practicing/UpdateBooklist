package sam.books;
import java.nio.file.Path;

public class NewBook {
    
    String name;
    public int id;
    String author;
    String isbn;
    int page_count;
    String year;
    String description;
    String url;
    
    final int path_id;
    final String file_name;
    final Path path;

    public NewBook(String file_name, Path path, int path_id) {
        this.file_name = file_name;
        this.path = path;
        this.path_id = path_id;
    }

	@Override
	public String toString() {
		return "NewBook [name=" + name + ", id=" + id + ", author=" + author + ", isbn=" + isbn + ", page_count="
				+ page_count + ", year=" + year + ", description=" + description + ", url=" + url + ", path_id="
				+ path_id + ", file_name=" + file_name + ", path=" + path + "]";
	}
}
