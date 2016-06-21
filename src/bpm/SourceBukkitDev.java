package bpm;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class SourceBukkitDev implements Source {
	public static final String URL_PLUGINS_SERVER = "http://dev.bukkit.org";
	public static final String URL_PLUGINS_BASE = "http://dev.bukkit.org/bukkit-plugins/";
	public static final String URL_PLUGINS_SEARCH = "http://dev.bukkit.org/search/?scope=projects&search=";

	public File downloadPlugin(String name) throws IOException {
		if(!BukkitPackageManager.verbose){
			System.out.print("\r[Connecting...       ] 0%");
		}
		File dest = File.createTempFile("bpm-" + name, null);
		String download = findDownloadLink(URL_PLUGINS_BASE + name);
		if(BukkitPackageManager.verbose){
			System.out.println("[Bukkit] " + download + " -> " + dest);
		}

		HttpURLConnection conn = (HttpURLConnection) new URL(download).openConnection();
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

	protected String findDownloadLink(String url) throws IOException {
		if(BukkitPackageManager.verbose){
			System.out.println("[Bukkit] " + "Read " + url);
		}
		Document doc = Jsoup.connect(url).get();
		Elements downloadLink = doc.select(".user-action-download a");
		url = downloadLink.get(0).absUrl("href");
		doc = Jsoup.connect(url).get();
		if(BukkitPackageManager.verbose){
			System.out.println("[Bukkit] " + "Read " + url);
		}
		downloadLink = doc.select(".user-action-download a");
		return downloadLink.get(0).absUrl("href");
	}

	public List<String> searchForPackage(String search) throws Exception {
		List<String> results = new LinkedList<String>();
		String url = URL_PLUGINS_SEARCH + URLEncoder.encode(search, "UTF-8");
		if(BukkitPackageManager.verbose){
			System.out.println("\n[Bukkit] " + "Read " + url);
		}
		Document doc = Jsoup.connect(url).get();
		Elements resultElements = doc.select(".col-search-entry:not(.single-col)");
		for (Element el : resultElements) {
			Element link = el.select("a").get(0);
			String name = link.absUrl("href");
			name = name.substring(URL_PLUGINS_BASE.length());
			name = name.substring(0, name.length() - 1);
			results.add(name);
		}

		Collections.sort(results);
		return results;
	}

	public String getPackageDetails(String name) throws Exception {
		String url = URL_PLUGINS_BASE + name;
		if(BukkitPackageManager.verbose){
			System.out.println("\n[Bukkit] " + "Read " + url);
		}
		Document doc = Jsoup.connect(url).get();
		Element contents = doc.select(".content-box-inner").get(0);
		Elements ps = contents.select("p");
		if (!BukkitPackageManager.fullDetails) {
			for (Element p : ps) {
				String content = p.text().trim();
				if (content.length() > 0)
					return content;
			}
		}
		return contents.text();
	}
	
	public String getName(){
		return "Bukkit";
	}
}
