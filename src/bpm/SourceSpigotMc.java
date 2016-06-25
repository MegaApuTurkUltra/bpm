package bpm;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Whitelist;
import org.jsoup.select.Elements;

public class SourceSpigotMc implements Source {
	public static final String URL_PLUGINS_SERVER = "https://www.spigotmc.org";
	public static final String URL_PLUGINS_BASE = "https://www.spigotmc.org/resources/";
	public static final String URL_PLUGINS_SEARCH = "https://www.spigotmc.org/search/1/?t=resource_update&o=relevance&c[rescat]=4+14+15+16+17+18+22+23+24+25+26&q=";
	public static final String URL_PLUGINS_SEARCH_NAME = "https://www.spigotmc.org/search/1/?t=resource_update&o=relevance&c[title_only]=1&c[rescat]=2+4+5+6+7+8+14+15+16+17+18+22+23+24+25+26&q=";
	public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.103 Safari/537.36";

	@Override
	public File downloadPlugin(String name) throws Exception {
		String url = findPluginPage(name);
		if (BukkitPackageManager.verbose) {
			System.out.println("[Spigot] " + "Read " + url);
		}
		Document doc = Jsoup.connect(url).userAgent(USER_AGENT).get();
		String download = doc.select(".downloadButton .inner").get(0).absUrl("href");
		if (!BukkitPackageManager.verbose) {
			System.out.print("\r[Connecting...       ] 0%");
		}
		File dest = File.createTempFile("bpm-" + name, null);
		if (BukkitPackageManager.verbose) {
			System.out.println("[Spigot] " + download + " -> " + dest);
		}

		HttpURLConnection conn = (HttpURLConnection) new URL(download).openConnection();
		conn.setRequestProperty("User-Agent", USER_AGENT);
		int totalSize = conn.getHeaderFieldInt("Content-Length", -1), currentSize = 0;
		InputStream in = conn.getInputStream();
		OutputStream out = new FileOutputStream(dest);
		byte[] buffer = new byte[1024 * 1024 * 8];
		int len;
		while ((len = in.read(buffer)) != -1) {
			out.write(buffer, 0, len);
			currentSize += len;
			if (totalSize != -1) {
				int percent = (int) (20.0 * currentSize / totalSize);
				System.out.print("\r[");
				for (int i = 0; i < 20; i++) {
					System.out.print(i < percent ? '|' : ' ');
				}
				System.out.print("] " + ((int) (100.0 * currentSize / totalSize)) + "%");
			} else {
				System.out.print("\r[????????????????????] ?%");
			}
		}
		System.out.println("\r[||||||||||||||||||||] 100%");
		in.close();
		out.close();
		conn.disconnect();
		return dest;
	}

	protected String findPluginPage(String name) throws Exception {
		String url = URL_PLUGINS_SEARCH_NAME + URLEncoder.encode(name, "UTF-8");

		if (BukkitPackageManager.verbose) {
			System.out.println("[Spigot] " + "Read " + url);
		}
		Document doc = Jsoup.connect(url).userAgent(USER_AGENT).get();

		Elements titles = doc.select(".title");
		if (titles.size() < 1) {
			throw new Exception("Plugin " + name + " not found");
		}
		Element first = titles.get(0);
		String text = first.text();
		if (!text.toLowerCase().contains(name.toLowerCase())) {
			throw new Exception("Plugin " + name + " not found");
		}
		return first.select("a").get(0).absUrl("href");
	}

	@Override
	public List<String> searchForPackage(String search) throws Exception {
		List<String> packages = new ArrayList<String>();
		String url = URL_PLUGINS_SEARCH + URLEncoder.encode(search, "UTF-8");
		if (BukkitPackageManager.verbose) {
			System.out.println("\n[Spigot] " + "Read " + url);
		}
		Document doc = Jsoup.connect(url).userAgent(USER_AGENT).get();

		Elements titles = doc.select(".titleText");
		for (Element el : titles) {
			if (el.text().contains("Resource Update"))
				continue;

			String[] pieces = el.select(".title").text().trim().split(" ");
			for (int i = 0; i < pieces.length; i++) {
				char first = pieces[i].charAt(0);
				if (Character.isAlphabetic(first)) {
					packages.add(pieces[i]);
					break;
				}
			}
		}
		return packages;
	}

	@Override
	public String getPackageDetails(String name) throws Exception {
		String url = findPluginPage(name);
		if (BukkitPackageManager.verbose) {
			System.out.println("\n[Spigot] " + "Read " + url);
		}
		Document doc = Jsoup.connect(url).userAgent(USER_AGENT).get();

		if (BukkitPackageManager.fullDetails) {
			String res = doc.select(".tagLine").get(0).text().trim() + "\n";
			Element text = doc.select(".messageText").get(0);
			res += br2nl(text.html());
			return res;
		} else {
			return doc.select(".tagLine").get(0).text().trim();
		}
	}

	protected static String br2nl(String html) {
		if (html == null)
			return html;
		Document document = Jsoup.parse(html);
		document.outputSettings(new Document.OutputSettings().prettyPrint(false));
		document.select("br").append("\\n");
		document.select("p").prepend("\\n\\n");
		String s = document.html().replaceAll("\\\\n", "\n");
		return Jsoup.clean(s, "", Whitelist.none(), new Document.OutputSettings().prettyPrint(false));
	}

	public String getName() {
		return "Spigot";
	}
}
