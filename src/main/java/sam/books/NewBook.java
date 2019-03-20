package sam.books;
import java.io.Serializable;

import sam.books.pathwrap.Dir;
import sam.books.pathwrap.PathWrap;
import sam.myutils.Checker;

public class NewBook implements Serializable {
	private static final long serialVersionUID = -8062536072287690587L;

	final PathWrap path;
	String name;
    public int id;
    String author;
    String isbn;
    int page_count;
    String year;
    String description;
    String url;

    public NewBook(PathWrap path) {
    	Checker.assertTrue(!path.isDir());
    	this.path = path;
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

	public PathWrap path() {
		return path;
	}
}
