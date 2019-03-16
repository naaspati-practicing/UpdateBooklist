package sam.books;
import java.io.Serializable;
import java.nio.file.Path;

import sam.myutils.Checker;

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

    transient final FileWrap file;
    transient final Dir parent;

    public NewBook(FileWrap file, Dir parent) {
    	Checker.assertTrue(!file.isDir());
        this.file = file;
        this.parent = parent;
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

	public Dir dir() {
		return parent;
	}
	public FileWrap file() {
		return file;
	}
}
