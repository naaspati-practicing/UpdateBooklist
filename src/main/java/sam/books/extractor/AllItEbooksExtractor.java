package sam.books.extractor;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.UncheckedIOException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import static sam.books.BooksMeta.*;

public class AllItEbooksExtractor implements Function<String, Map<String, String>> {

    @Override
    public Map<String, String> apply(String url) {
        if(!url.toLowerCase().contains("allitebooks"))
            return null;

        try {
            Document doc = Jsoup.parse(new URL(url), 20000);
            Map<String, String> result = new HashMap<>();

            Element elm =  doc.getElementsByClass("entry-content").get(0);
            result.put(DESCRIPTION, "<html>"+elm.html().toString().replaceFirst("<h3>.+</h3>\\s+", "")+"</html>");
            String title = doc.getElementsByClass("single-title").get(0).text();

            elm =  doc.getElementsByClass("book-detail").get(0);
            Map<String, String> map = elm.getElementsByTag("dt").stream().collect(Collectors.toMap(e -> e.text().toLowerCase().startsWith("isbn") ? "ISBN" : e.text(), e -> ((Element)e.nextSibling()).text()));

            result.put(NAME,title);
            result.put(AUTHOR,map.get("Author:"));
            result.put(ISBN,map.get("ISBN"));
            result.put(YEAR,map.get("Year:"));
            result.put(PAGE_COUNT,map.get("Pages:"));

            return result;   
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
