import sam.books.UpdateDBBeta;
import sam.config.LoadConfig;
import sam.nopkg.Junk;

public class Main2 {

	public static void main(String[] args) throws Exception {
		LoadConfig.load();
		new UpdateDBBeta().call();
		System.out.println(Junk.systemInfo());
	}

}
