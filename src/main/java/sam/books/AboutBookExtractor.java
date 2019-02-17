package sam.books;
import static java.awt.GridBagConstraints.BOTH;
import static java.awt.GridBagConstraints.EAST;
import static java.awt.GridBagConstraints.NONE;
import static java.awt.GridBagConstraints.RELATIVE;
import static java.awt.GridBagConstraints.REMAINDER;
import static java.awt.GridBagConstraints.WEST;
import static javax.swing.BorderFactory.createCompoundBorder;
import static javax.swing.BorderFactory.createEmptyBorder;
import static javax.swing.BorderFactory.createLineBorder;
import static sam.books.ColumnNames.AUTHOR;
import static sam.books.ColumnNames.DESCRIPTION;
import static sam.books.ColumnNames.FILE_NAME;
import static sam.books.ColumnNames.ISBN;
import static sam.books.ColumnNames.NAME;
import static sam.books.ColumnNames.PAGE_COUNT;
import static sam.books.ColumnNames.PATH_ID;
import static sam.books.ColumnNames.URL;
import static sam.books.ColumnNames.YEAR;
import static sam.books.ColumnNames.values;
import static sam.console.ANSI.red;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.text.JTextComponent;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import sam.io.fileutils.FileOpenerNE;
import sam.io.serilizers.ObjectReader;
import sam.io.serilizers.ObjectWriter;
import sam.myutils.Checker;
import sam.myutils.MyUtilsException;
import sam.swing.SwingPopupShop;
import sam.swing.SwingPopupShop.SwingPopupWrapper;
import sam.swing.SwingUtils;

public class AboutBookExtractor extends JDialog {
	private static final long serialVersionUID = 5876027522279145824L;

	private Font font = new Font(null, 1, 17);

	private final JLabel nameLabel = new JLabel("name");
	private final JLabel pathLabel  = new JLabel("path");
	private final JButton descriptionBtn = new JButton("Description");
	private final JLabel urlsLabel = new JLabel("URLS");

	private final JButton nextButton;
	private final String nextFormat ;
	private final JButton urlGo ; 
	private final JCheckBox autoLoadCB; 

	JTextField url = new JTextField(20);
	JTextArea urlsTA = new JTextArea(10, 20);

	private List<NewBook> books;
	private final ListIterator<NewBook> iterator;
	private final EnumMap<ColumnNames, JTextComponent> fields = new EnumMap<>(ColumnNames.class);

	private JTextField nameField;

	private String _description;
	private final Map<Path, NewBook> loaded = new HashMap<>();
	private final Path newbook_backup = DatabaseUpdate.SELF_DIR.resolve("newbook-backup");

	public AboutBookExtractor(List<NewBook> books) {
		super(null, "Details Extracting", ModalityType.APPLICATION_MODAL);
		SwingPopupShop.setPopupsRelativeTo(this);
		descriptionBtn.setEnabled(false);
		descriptionBtn.addActionListener(e -> showDescription());

		if(Files.exists(newbook_backup)) {
			List<NewBook> list = MyUtilsException.noError(() -> ObjectReader.read(newbook_backup), Throwable::printStackTrace);
			if(Checker.isNotEmpty(list)) {
				Set<Path> paths = books.stream().map(NewBook::path).collect(Collectors.toSet());
				list.forEach(b -> {
					if(paths.contains(b.path()))
						this.loaded.put(b.path(), b);
				});
			}
		}

		this.books = books;
		this.iterator = books.listIterator();

		UIManager.put("Label.font", font);
		UIManager.put("TextField.font", font);
		UIManager.put("TextArea.font", font);

		JPanel top = new JPanel(new FlowLayout(15));
		pathLabel.setFont(font.deriveFont(12f));
		top.setBackground(Color.BLACK);
		top.setOpaque(true);
		pathLabel.setForeground(Color.YELLOW);
		top.setBorder(new EmptyBorder(5, 5, 5, 5));
		top.add(pathLabel);
		top.add(btn("open", e -> open(true)));
		top.add(btn("open location", e -> open(false)));

		JPanel leftPanel = new JPanel(new GridBagLayout(), false);
		leftPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
		add(top, BorderLayout.NORTH);

		GridBagConstraints gbc = new GridBagConstraints(0, RELATIVE, 6, 1, 1, 1, WEST, BOTH, new Insets(3, 5, 3, 5), 0, 0);
		gbc.gridwidth = REMAINDER;
		leftPanel.add(nameLabel, gbc);

		Stream.of(values())
		.filter(c -> c != DESCRIPTION)
		.forEach(c -> {
			if(c == PATH_ID || c == FILE_NAME)
				return;

			gbc.gridx = 0;
			gbc.gridwidth = 1;
			JLabel l = new JLabel(c.toString());
			leftPanel.add(l, gbc);

			gbc.gridx = 1;
			gbc.gridwidth = REMAINDER;
			JTextField f = new JTextField(20);
			leftPanel.add(f, gbc);

			if(c == NAME)
				nameField = f;

			fields.put(c, f);
		});

		gbc.gridx = 0;
		gbc.gridwidth = REMAINDER;

		gbc.fill = NONE;
		gbc.anchor = EAST;
		leftPanel.add(descriptionBtn, gbc);

		gbc.anchor = WEST;
		gbc.fill = BOTH;
		JPanel temp1 = new JPanel(new BorderLayout(), false);
		temp1.add(new JLabel("Extract Data From Url  "));
		autoLoadCB = new JCheckBox("auto load url");
		temp1.add(autoLoadCB, BorderLayout.EAST);
		leftPanel.add(temp1, gbc);

		JPanel pTemp = new JPanel(new BorderLayout(5, 5), false);
		pTemp.add(url);

		urlGo = new JButton("GO");
		url.addActionListener(e -> urlGo.doClick());
		pTemp.add(urlGo, BorderLayout.EAST);
		pTemp.add(Box.createHorizontalStrut(10), BorderLayout.WEST);


		leftPanel.add(pTemp, gbc);

		urlsTA.setBorder(createCompoundBorder(new EmptyBorder(5, 5, 5, 5), createCompoundBorder(LineBorder.createGrayLineBorder(), new EmptyBorder(5, 5, 5, 5))));
		JPanel rightPanel = new JPanel(new BorderLayout(5, 5), false);
		rightPanel.add(urlsLabel, BorderLayout.NORTH);
		rightPanel.add(new JScrollPane(urlsTA));
		JButton extractUrl = new JButton("Extract Url");
		rightPanel.add(extractUrl, BorderLayout.SOUTH);

		add(rightPanel, BorderLayout.CENTER);

		extractUrl.addActionListener(e -> setUrl());

		gbc.gridwidth = 6;
		nextFormat = "Next (%s/"+books.size()+")";
		nextButton = new JButton(String.format(nextFormat, "1"));
		nextButton.setFont(new Font("Elephant", Font.BOLD, 20));
		nextButton.setForeground(Color.white);
		nextButton.setBackground(Color.decode("#2E8B57"));
		leftPanel.add(nextButton, gbc);

		urlGo.addActionListener(this::urlGoAction);
		nextButton.addActionListener(this::nextAction);

		add(leftPanel, BorderLayout.WEST);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				if(JOptionPane.showConfirmDialog(null, "<html>Details Extracting Not Completed<br>Nothing will be saved<br>Sure To Exit</html>", "Confirm Action", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.OK_OPTION){
					AboutBookExtractor.this.books = null;
					dispose();
				}
			};
		});
	}

	private void open(boolean openFile) {
		Optional.ofNullable(current)
		.map(c -> c.path())
		.map(BooksDBMinimal.ROOT::resolve)
		.map(Path::toFile)
		.ifPresent(f -> {
			if(openFile)
				FileOpenerNE.openFile(f);
			else
				FileOpenerNE.openFileLocationInExplorer(f);
		});
	}

	private Component btn(String string, ActionListener action) {
		JButton open = new JButton("open");
		open.setBorder(createCompoundBorder(createLineBorder(Color.white), createEmptyBorder(2, 5, 2, 5)));
		open.setBackground(Color.black);
		open.setForeground(Color.white);
		open.setFocusPainted(false);
		if(action != null)
			open.addActionListener(action);
		return open;
	}

	private void showDescription() {
		JDialog dialog = new JDialog(this, "description", true);
		JLabel l = new JLabel(_description);
		l.setBackground(Color.white);
		l.setOpaque(true);
		dialog.add(new JScrollPane(l));
		dialog.setSize(400, 400);
		dialog.setLocationRelativeTo(this);
		dialog.setVisible(true);
	}

	public List<NewBook> getResult(){
		nextButton.doClick();

		pack();
		setLocationRelativeTo(null);
		setVisible(true);

		if(!loaded.isEmpty())
			MyUtilsException.hideError(() -> ObjectWriter.write(newbook_backup, new ArrayList<>(loaded.values())), Throwable::printStackTrace);

		return books;
	}

	private  NewBook current;

	private void nextAction(Object ignore) {
		descriptionBtn.setEnabled(false);

		if(current != null) {
			current.description = _description; 

			fields.forEach((colName, field) -> {
				String s = field.getText();
				if(s != null){
					s = s.replaceAll("[\r\n\t\f]", "").trim();
					s = s.isEmpty() ? null : s;
				}
				colName.set(current, s);
			});

			if(Arrays.equals(new String[] {current.isbn, current.page_count == 0 ? null : String.valueOf(current.page_count), current.year, current.description}, new String[4])){
				if(JOptionPane.showConfirmDialog(null, "<html>Sure to Proceed<br>Fields Are Empty</html>", "Confirm Action", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) != JOptionPane.YES_OPTION)
					return;
			}
			loaded.put(current.path(), current);
		}

		if(!iterator.hasNext()){
			dispose();
			return;
		}
		current = iterator.next();
		current.apply(loaded.get(current.path()));

		nextButton.setText(String.format(nextFormat, iterator.nextIndex()));

		nameLabel.setText("<html>"+current.file_name+"</html>");
		nameLabel.setName(current.file_name);
		System.out.println(current.path());
		pathLabel.setText(current.path_id+"  " + current.path());

		setDescription(current.description);
		fields.forEach((c,f) -> f.setText(c.get(current)));

		if(current.name == null)
			nameField.setText(current.file_name.replaceFirst("\\.pdf$", ""));

		url.setText(null);
		setUrl();
	}

	private void urlGoAction(Object ignore) {
		String urlsString = url.getText();

		if(urlsString.trim().isEmpty())
			return;

		if(!(urlsString.contains("it-ebooks.info") || urlsString.contains("www.allitebooks.com"))){
			url.setText("url not supported: "+url.getText());
			return;
		}

		SwingPopupWrapper popId = SwingPopupShop.showPopup("Wait");
		setEnabled(false);

		new Thread(() -> {
			try {
				Document doc = Jsoup.parse(new URL(urlsString), 20000);

				if(urlsString.contains("it-ebooks.info")){
					setDescription(doc.select("td.justify.link span").html());

					for (Element e : doc.getElementsByTag("h1")) {
						if(e.toString().contains("itemprop=\"name\""))
							setColumnValue(NAME, e);
					}

					for (Element e : doc.select("td.justify.link b")) {
						if(e.toString().contains("itemprop=\"isbn\""))
							setColumnValue(ISBN, e);
						else if(e.toString().contains("itemprop=\"datePublished\""))
							setColumnValue(YEAR, e);
						else if(e.toString().contains("itemprop=\"numberOfPages\""))
							setColumnValue(PAGE_COUNT, e);
						else if(e.toString().contains("itemprop=\"author\""))
							setColumnValue(AUTHOR, e);
					}
				}
				else if(urlsString.contains("www.allitebooks.com")){
					Element elm =  doc.getElementsByClass("entry-content").get(0);
					setDescription("<html>"+elm.html().toString().replaceFirst("<h3>.+</h3>\\s+", "")+"</html>");
					String title = doc.getElementsByClass("single-title").get(0).text();

					elm =  doc.getElementsByClass("book-detail").get(0);
					Map<String, String> map = elm.getElementsByTag("dt").stream().collect(Collectors.toMap(e -> e.text().toLowerCase().startsWith("isbn") ? "ISBN" : e.text(), e -> ((Element)e.nextSibling()).text()));

					fields.get(NAME).setText(title);
					fields.get(AUTHOR).setText(map.get("Author:"));
					fields.get(ISBN).setText(map.get("ISBN"));
					fields.get(YEAR).setText(map.get("Year:"));
					fields.get(PAGE_COUNT).setText(map.get("Pages:"));
					fields.get(URL).setText(url.getText());
				}
				url.setText(null);
			} catch (IOException e2) {
				SwingUtils.showErrorDialog("failed : ", e2);
			}
			SwingPopupShop.hidePopup(popId, 0);
			setEnabled(true);
		}).start();
	}

	private void setDescription(String s) {
		_description = s;
		descriptionBtn.setEnabled(s != null && !s.isEmpty());
	}
	private void setColumnValue(ColumnNames key, Element e) {
		fields.get(key).setText(e.text());
	}

	private void setUrl() {
		String urls = urlsTA.getText();
		if(urls == null)
			return;

		urls = urls.trim();
		Set<String> list = new HashSet<>(Arrays.asList(urls.split("\r?\n")));
		list.removeIf(s -> s.trim().isEmpty());

		if(list.isEmpty())
			return;

		Map<String, String> urlFileName = list.stream().collect(Collectors.toMap(Function.identity(), s -> {
			try {
				return new URL(s).getFile().replace('/', ' ').trim();
			} catch (Exception e1) {
				System.out.println(red(s+"  "+e1));
				return "";
			}
		}, (o, n) -> n, HashMap::new));

		urlFileName.values().removeIf(String::isEmpty);

		String temp = nameLabel.getName().toLowerCase().replaceFirst(".pdf$", "").replaceAll("\\W+|_", "");
		Iterator<String> iterator = urlFileName.keySet().iterator();

		while(iterator.hasNext()) {
			String s = iterator.next();
			if(temp.equals(urlFileName.get(s).replaceAll("\\W+|_", ""))){
				url.setText(s);
				iterator.remove();
				if(autoLoadCB.isSelected())
					urlGo.doClick();
				break;
			}
		}

		urlsTA.setText(String.join("\n", urlFileName.keySet()));
		urlsLabel.setText("URLS  ("+urlFileName.size()+")");
	}
}
