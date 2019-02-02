package sam.books;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;

public class NewBook implements Serializable {
	private static final long serialVersionUID = -8062536072287690587L;
	
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
    private transient final Path path;
    private final String pathS;

    public NewBook(String file_name, Path path, int path_id) {
        this.file_name = file_name;
        this.path = path;
        this.path_id = path_id;
        this.pathS = path.toString();
    }
    
    public Path path() {
		return path == null ? Paths.get(pathS) : path;
	}
    
	@Override
	public String toString() {
		return "NewBook [name=" + name + ", id=" + id + ", author=" + author + ", isbn=" + isbn + ", page_count="
				+ page_count + ", year=" + year + ", description=" + description + ", url=" + url + ", path_id="
				+ path_id + ", file_name=" + file_name + ", path=" + path + "]";
	}

	public void apply(NewBook existing) {
		if(existing == null)
			return;
		
		this.name = existing.name;
		this.id = existing.id;
		this.author = existing.author;
		this.isbn = existing.isbn;
		this.page_count = existing.page_count;
		this.year = existing.year;
		this.description = existing.description;
		this.url = existing.url;
	}
}
