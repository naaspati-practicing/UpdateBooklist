import sam.books.UpdateDB;
import sam.config.LoadConfig;
import sam.nopkg.Junk;

public class Main2 {

	public static void main(String[] args) throws Exception {
		LoadConfig.load();
		new UpdateDB().call();
		System.out.println(Junk.systemInfo());
	}

}
