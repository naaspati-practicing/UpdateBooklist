package sam.books;
import static sam.books.BooksDBMinimal.BACKUP_FOLDER;
import static sam.books.BooksDBMinimal.DB_PATH;
import static sam.books.BooksDBMinimal.ROOT;
import static sam.books.BooksDBMinimal.getStatusFromDir;
import static sam.books.BooksDBMinimal.getStatusFromFile;
import static sam.books.BooksMeta.AUTHOR;
import static sam.books.BooksMeta.BOOK_ID;
import static sam.books.BooksMeta.BOOK_TABLE_NAME;
import static sam.books.BooksMeta.CREATED_ON;
import static sam.books.BooksMeta.FILE_NAME;
import static sam.books.BooksMeta.ISBN;
import static sam.books.BooksMeta.MARKER;
import static sam.books.BooksMeta.NAME;
import static sam.books.BooksMeta.PAGE_COUNT;
import static sam.books.BooksMeta.PATH;
import static sam.books.BooksMeta.PATH_ID;
import static sam.books.BooksMeta.PATH_TABLE_NAME;
import static sam.books.BooksMeta.STATUS;
import static sam.books.BooksMeta.YEAR;
import static sam.console.ANSI.createBanner;
import static sam.console.ANSI.cyan;
import static sam.console.ANSI.green;
import static sam.console.ANSI.red;
import static sam.console.ANSI.yellow;
import static sam.sql.querymaker.QueryMaker.qm;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.swing.JOptionPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sam.collection.OneOrMany;
import sam.collection.Pair;
import sam.config.MyConfig;
import sam.console.ANSI;
import sam.myutils.Checker;
import sam.myutils.System2;
import sam.sql.JDBCHelper;
import sam.sql.querymaker.InserterBatch;
import sam.string.StringBuilder2;
import sam.string.StringWriter2;
import sam.tsv.Tsv; 

public class UpdateDB implements Callable<Boolean> {
	private Logger logger = LoggerFactory.getLogger(UpdateDB.class);
	public static final Path SELF_DIR = Paths.get(System2.lookup("SELF_DIR"));

	@Override
	public Boolean call() throws Exception {
		logger.info("{}{}", yellow("WORKING_DIR: "), ROOT);

		logger.info(createBanner("Updating "+DB_PATH)+"\n");
		Path updatePath = SELF_DIR.resolve("app_state.dat");

		List<Dir> dirsInRoot = load(updatePath);

		if(Checker.isEmpty(dirsInRoot))
			return false;

		Map<Path, Dir> dirs = new HashMap<>(200);
		List<FileWrap> files = new ArrayList<>(2000);

		Walker.walk(dirsInRoot, w -> {
			if(w.isDir()) {
				if(getStatusFromDir(w.subpath()) == BookStatus.NONE)
					dirs.put(w.subpath(), (Dir)w);
			} else
				files.add(w);
		});

		if(files.stream().collect(Collectors.groupingBy(FileWrap::name, Collectors.counting())).values().stream().anyMatch(e -> e > 1)) {
			StringBuilder2 sb = new StringBuilder2();
			sb.append(createBanner("repeated books")).ln();

			files.stream()
			.collect(Collectors.groupingBy(FileWrap::name, OneOrMany.collector()))
			.forEach((s,t) -> {
				if(t.size() == 1)
					return;

				sb.yellow("-----------------------------------\n")
				.append(s).ln();

				StringWriter2 sw = sb.writer();
				t.forEach(z -> {
					z.toString(sw);
					sb.ln();
				});
			});

			sb.red("db update skipped\n");
			sb.append(separator()).ln();
			logger.info(sb.toString());

			JOptionPane.showMessageDialog(null, "db update skipped");
			return false;
		}

		boolean modified = false;

		try (BooksDBMinimal db = new BooksDBMinimal()) {
			ArrayList<Pair<Integer, Path>> deleted = new ArrayList<>();
			db.iterate("SELECT * FROM "+PATH_TABLE_NAME, rs -> {
				int id = rs.getInt(PATH_ID);
				Path p = Paths.get(rs.getString(PATH));
				Dir dir = dirs.get(p);

				if(dir == null)
					deleted.add(new Pair<Integer, Path>(id, p));
				else
					dir.id(id);
			});

			//checking for dirs not in database
			modified = newDirs(db, dirs) || modified;

			//extra dirs in db
			modified = processDeletedDirs(deleted, db) || modified;

			Map<String, FileWrap> bookFiles = files.stream().collect(Collectors.toMap(FileWrap::name, w -> w, (o, n) -> {throw new IllegalStateException("duplicate: \""+o+"\", \""+n+"\"");}, () -> new HashMap<>(files.size() + 20)));

			//{book_id, file_pame, path_id}
			ArrayList<Book1> dbBooksData = new ArrayList<>(files.size() + 10);
			List<Book1> extras = new ArrayList<>();

			db.iterate(Book1.SELECT_SQL, rs -> {
				Book1 b = new Book1(rs);
				FileWrap f = bookFiles.get(b.file_name);
				if(f == null) {
					extras.add(b);
				} else {
					f.found(true);
					b.file(f);
					dbBooksData.add(b);
				}
			});

			//delete extra books
			modified = deleteExtraBooks(extras, dirs,db) || modified;

			//respect path changes
			modified = lookForPathChanges(db, dbBooksData, bookFiles, dirs) || modified;

			List<NewBook> newBooks = files.stream()
					.filter(f -> !f.found())
					.map(f -> new NewBook(f, dirs.get(getParent(f.subpath()))))
					.collect(Collectors.toList());

			//list non-listed books
			modified = processNewBooks(newBooks, db) || modified;
			db.commit();
		} catch (SQLException e1) {
			if("user refused to proceed".equals(e1.getMessage())) {
				logger.info(red("user refused to proceed"));
				return false;
			}
			e1.printStackTrace();
		}
		logger.info(ANSI.FINISHED_BANNER+"\n\n\n");

		if(modified){
			Path t = BACKUP_FOLDER.resolve(DB_PATH.getFileName()+"_"+LocalDate.now().getDayOfWeek().getValue());
			Files.createDirectories(t.getParent());
			Files.copy(DB_PATH, t, StandardCopyOption.REPLACE_EXISTING);
		}

		return modified;
	}


	/**
	 * returns dirs in root Dir
	 * @param updatePath
	 * @return
	 * @throws IOException
	 */
	private List<Dir> load(Path updatePath) throws IOException {
		List<Dir> result = new Serializer().read(updatePath);

		Walker w = new Walker(skipFilePath());
		if(Checker.isEmpty(result))
			return w.walkRootDir();

		Iterator<Dir> itr = result.iterator();
		int updateCount = 0;
		while (itr.hasNext()) {
			Dir dir = itr.next();
			
			if(!dir.exists()) {
				itr.remove();
				updateCount++;
			} else
				updateCount += dir.update(w);				
		}
		
		if(updateCount == 0) {
			logger.info("no dir modified dirs found");
			return null;
		}
			
		
		return result;
	}

	private Path skipFilePath() {
		return SELF_DIR.resolve("ignore-subpaths.txt");
	}

	private boolean processNewBooks(List<NewBook> newBooks, BooksDBMinimal db) throws SQLException {
		if(Checker.isEmpty(newBooks))
			return false;

		StringBuilder2 sb = new StringBuilder2();
		sb.green("new books : ("+newBooks.size()+")\n");

		newBooks.stream()
		.collect(Collectors.groupingBy(s -> s.dir()))
		.forEach((s,b) -> {
			sb.yellow(s).ln();
			b.forEach(t -> sb.append("  ").append(t.file.name()).ln());
		});
		sb.ln();

		List<NewBook> books =  new AboutBookExtractor(newBooks).getResult();

		if(books == null || books.isEmpty())
			return false;

		Tsv tsv = new Tsv(BOOK_ID,
				NAME,
				FILE_NAME,
				AUTHOR,
				ISBN,
				PAGE_COUNT,
				YEAR);

		int max = db.findFirst("select seq from sqlite_sequence where name='"+BOOK_TABLE_NAME+"'", rs -> rs.getInt(1)) + 1;

		for (NewBook b : books) {
			b.id = max++;
			tsv.addRow(String.valueOf(b.id),
					b.name,
					b.file.name(),
					b.author,
					b.isbn,
					String.valueOf(b.page_count),
					b.year);
		}

		InserterBatch<NewBook> insert = new InserterBatch<>(BOOK_TABLE_NAME);
		insert.setInt(BOOK_ID, b -> b.id);

		for (ColumnNames n : ColumnNames.values())
			insert.setString(n.columnName, b -> n.get(b));

		long time = System.currentTimeMillis(); 
		insert.setLong(CREATED_ON, b -> time);

		sb.yellow("\nexecutes: ").append(insert.execute(db, books)).ln();

		Path p = Paths.get(MyConfig.COMMONS_DIR, "new-books-"+LocalDateTime.now().toString().replace(':', '_') + ".tsv");
		try {
			tsv.save(p);
			sb.yellow("created: ").append(p).ln();
		} catch (IOException e2) {
			sb.red("failed to save: ").append(p).append(", error: ").append(e2).ln();
		}

		sb.append(separator());
		logger.info(sb.toString());
		return true;
	}
	private boolean newDirs(BooksDBMinimal db, Map<Path, Dir> dirs) throws SQLException {
		int[] max = {0};

		List<Dir> missings = dirs
				.values()
				.stream()
				.peek(w -> max[0] = Math.max(max[0], w.id()))
				.filter(w -> w.id() < 0)
				.collect(Collectors.toList());

		if(!missings.isEmpty()){
			logger.info(yellow("new folders"));
			String format = "%-5s%s\n";

			try(PreparedStatement ps = db.prepareStatement(JDBCHelper.insertSQL(PATH_TABLE_NAME, PATH_ID, PATH, MARKER))) {
				StringBuilder2 sb = new StringBuilder2();

				for (Dir w : missings) {
					int n = ++max[0];
					String str = w.subpath().toString();
					sb.format(format, n, str);
					w.id(n);

					ps.setInt(1, w.id());
					ps.setString(2, str);
					ps.setString(3, w.name());

					ps.addBatch();
					n++;
				}

				sb.yellow("\nexecutes: ")
				.append(ps.executeBatch().length)
				.append("\n----------------------------------------------\n");

				logger.info(sb.ln().toString());
			}
			return true;
		}
		return false;
	}
	private boolean lookForPathChanges(BooksDBMinimal db, List<Book1> dbBooksData, Map<String, FileWrap> bookFiles, Map<Path, Dir> dirs) throws SQLException {
		db.prepareStatementBlock(qm().update(BOOK_TABLE_NAME).placeholders(STATUS).where(w -> w.eqPlaceholder(BOOK_ID)).build(), ps -> {
			int n = 0;
			for (Book1 b : dbBooksData) {
				BookStatus s = getStatusFromFile(b.file().subpath());

				if(b.status != s) {
					if(n == 0)
						logger.info(ANSI.yellow("STATUS CHANGES"));
					logger.info(b.file_name+"\t"+b.status +" -> "+ s);
					n++;
					ps.setString(1, s == null ? null : s.toString());
					ps.setInt(2, b.id);
					ps.addBatch();
				}
			}
			if(n != 0)  
				logger.info("status change: "+ps.executeBatch().length+"\n");
			return null;
		});

		ArrayList<Book1> changed = new ArrayList<>();

		for (Book1 o : dbBooksData) {
			Path p2 = bookFiles.get(o.file_name).subpath();
			Dir idNew = dirs.get(getParent(p2));

			if(idNew.id() != o.path_id) {
				o.setNewPathId(idNew.id());
				changed.add(o);
			}
		}
		if(!changed.isEmpty()){
			StringBuilder2 sb = new StringBuilder2();
			sb.red("changed books paths ("+changed.size()+")\n");
			String format = "%-5s%-12s%-12s%s\n";
			sb.append(yellow(String.format(format, "id", "old_path_id", "new_path_id", "file_name")));
			for (Book1 o : changed) 
				sb.format(format, o.id, o.path_id, o.getNewPathId(), o.file_name);

			sb.ln();

			String format2 = "%-10s%s\n";
			sb.ln().yellow(String.format(format2, "path_id", "path"));

			BitSet set = new BitSet();

			changed.stream()
			.map(o -> o.getNewPathId())
			.forEach(e -> set.set(e));

			dirs.forEach((p, dir) -> {
				if(dir.id() >= 0 && set.get(dir.id()))
					sb.format(format2, dir.id(), p);		
			});

			db.prepareStatementBlock(qm().update(BOOK_TABLE_NAME).placeholders(PATH_ID).where(w -> w.eqPlaceholder(BOOK_ID)).build(), 
					ps -> {
						for (Book1 o : changed) {
							ps.setInt(1, o.getNewPathId());
							ps.setInt(2, o.id);
							ps.addBatch();
						}
						sb.yellow("\nexecutes: ").append(ps.executeBatch().length).ln();
						return null;
					});

			logger.info(sb.append(separator()).toString());
			return true;
		}
		return false;
	}

	private Object getParent(FileWrap w) {
		return getParent(w.subpath());
	}
	private Object getParent(Path file) {
		if(file == null)
			return null;

		Path parent = file.getParent();
		BookStatus s = getStatusFromDir(parent);
		return s == BookStatus.NONE ? parent : parent.getParent();
	}
	private boolean deleteExtraBooks(List<Book1> extras, Map<Path, Dir> dirs, BooksDBMinimal db) throws SQLException{
		if(Checker.isEmpty(extras))
			return false;

		StringBuilder2 sb = new StringBuilder2();
		sb.red("extra books in DB ("+extras.size()+")\n");
		String format = "%-10s%-10s%s%n";
		sb.append(String.format(format, "id", "path_id", "file_name"));

		for (Book1 o : extras) 
			sb.format(format, o.id, o.path_id, o.file_name);

		sb.ln();

		BitSet set = new BitSet();
		extras.forEach(e -> set.set(e.path_id));
		Map<Integer, Dir> map = dirs.values().stream().filter(w -> w.id() >= 0 && set.get(w.id())).collect(Collectors.toMap(e -> e.id(), e -> e));

		format = "%-10s%s\n";
		sb.yellow(String.format(format, "path_id", "path"));
		for (Book1 o : extras) 
			sb.format(format, o.path_id, map.get(o.path_id));

		sb.ln();
		logger.info(sb.toString());

		int option = JOptionPane.showConfirmDialog(null, "<html>sure?<br>yes : confirm delete book(s)<br>no : continue without deleting<br>cancel : stop app", "delete extra  book(s)", JOptionPane.YES_NO_CANCEL_OPTION);

		if(option == JOptionPane.YES_OPTION){
			logger.info(yellow("\nexecutes: ") +db.executeUpdate(extras.stream().map(o -> String.valueOf(o.id)).collect(Collectors.joining(",", "DELETE FROM Books WHERE _id IN(", ")"))));
			logger.info(separator());
			return true;
		} else if(option != JOptionPane.NO_OPTION)
			throw new SQLException("user refused to proceed");

		logger.info(separator());
		return false;
	}
	private boolean processDeletedDirs(List<Pair<Integer, Path>> notfound, BooksDBMinimal db) throws SQLException {
		if(Checker.isEmpty(notfound))
			return false;

		StringBuilder2 sb = new StringBuilder2();
		sb.red("\nTHESE Dirs WILL BE DELETED\n");

		StringBuilder sql = new StringBuilder("DELETE FROM Paths WHERE path_id IN(");
		notfound.forEach(e -> {
			sb.append(e.key).append(": ").append(e.value).ln();
			sql.append(e.key).append(',');
		});
		sql.setCharAt(sql.length() - 1, ')');

		sb.yellow("\nexecutes: ").append(db.executeUpdate(sql.toString()));
		sb.append(separator());

		logger.info(sb.toString());
		return true;
	}

	private String separator() {
		return "\n----------------------------------------------\n";
	}
}
