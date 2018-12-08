package sam.books;
import static sam.books.BooksMeta.*;
import static sam.books.BooksMeta.FILE_NAME;
import static sam.books.BooksMeta.PATH_ID;
import static sam.books.BooksMeta.STATUS;

import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;

import sam.sql.JDBCHelper;


public class Book1 {
    public static final String SELECT_SQL = JDBCHelper.selectSQL(BOOK_TABLE_NAME, BOOK_ID, FILE_NAME, PATH_ID, STATUS).append(";").toString();
    
    final int id; 
    final String file_name;
    final int path_id;
    private int new_path_id;
    final BookStatus status;
    final Path path;
    
    public Book1(ResultSet rs,Map<String, Path> bookFiles) throws SQLException {
        this.id = rs.getInt(BOOK_ID);
        this.file_name = rs.getString(FILE_NAME);
        this.path_id = rs.getInt(PATH_ID);
        this.status = Optional.ofNullable(rs.getString(STATUS)).map(BookStatus::valueOf).orElse(null);
        this.path = bookFiles.get(file_name);
    }
    public void setNewPathId(int idNew) {
        new_path_id = idNew;
    }
    public int getNewPathId() {
        return new_path_id;
    }
    @Override
    public String toString() {
        return "Book1 [id=" + id + ", file_name=" + file_name + ", path_id=" + path_id + ", new_path_id=" + new_path_id
                + "]";
    }
    
    
}
