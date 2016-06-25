package bpm;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.jsoup.HttpStatusException;

import com.esotericsoftware.yamlbeans.YamlReader;
import com.esotericsoftware.yamlbeans.YamlWriter;

public class Plugin implements Comparable<Plugin> {
	enum PluginType {
		JAR, ZIP
	}

	String name;
	File location;
	String version;
	PluginType type;
	boolean installed;

	public Plugin(String name) {
		this.name = name;
		type = PluginType.JAR;
		installed = false;
	}

	public Plugin(File location) throws Exception {
		this.location = location;
		type = PluginType.JAR;
		getPackageInfoFromArchive(location);
		installed = true;
	}

	public void uninstall() throws Exception {
		if (location != null) {
			System.out.println("Uninstalling: " + name + " " + version);
			if (!location.delete()) {
				throw new IOException("Unable to delete: " + location);
			}

			Map<String, PluginInfo> manifest = readBpmManifest();
			if (manifest == null) {
				manifest = new HashMap<String, PluginInfo>();
			}
			if (manifest.containsKey(name)) {
				List<String> remainingFiles = manifest.get(name).installedFiles;
				for (String file : remainingFiles) {
					File f = new File(file);
					f.delete();
					if (!f.getParentFile().getAbsoluteFile().equals(BukkitPackageManager.plugins.getAbsoluteFile())) {
						f.getParentFile().delete();
					}
				}
				manifest.remove(name);
			}
			writeBpmManifest(manifest);
		}
	}
	
	protected File downloadByUrl(String download) throws Exception {
		File dest = File.createTempFile("bpm-" + name, null);
		HttpURLConnection conn = (HttpURLConnection) new URL(download).openConnection();
		conn.setRequestProperty("User-Agent", SourceSpigotMc.USER_AGENT);
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

	public void download(String url) throws Exception {
		if (this.location != null)
			return;

		System.out.println("Downloading: " + name);

		File temp = null;
		
		if(url == null){
			for (Source src : Source.SOURCES) {
				try {
					temp = src.downloadPlugin(name);
					break;
				} catch (HttpStatusException e) {
					if (BukkitPackageManager.verbose) {
						e.printStackTrace();
					}
				}
			}
		} else {
			temp = downloadByUrl(url);
		}
		
		System.out.print("\r                          \n");
		if (temp == null) {
			throw new Exception("Plugin not found: " + name);
		}

		String oldName = name;
		getPackageInfoFromArchive(temp);
		if (oldName != null && !name.equalsIgnoreCase(oldName)) {
			System.out.print("Downloaded plugin " + name + " doesn't match " + oldName + ".\n"
					+ "Continue installing? [y/n] ");
			String resp = BukkitPackageManager.consoleIn.readLine();
			if (!resp.toLowerCase().startsWith("y")) {
				return;
			}
		}

		for (Plugin p : Plugin.installed()) {
			if (BukkitPackageManager.verbose) {
				System.out.println("Checking installed plugin " + p.name);
			}
			if (p.name.equals(name)) {
				if (!BukkitPackageManager.noPromptOverwrite) {
					System.out.print("Overwrite: " + p.name + " " + p.version + " with " + version + "? [y/n] ");
					String resp = BukkitPackageManager.consoleIn.readLine();
					if (!resp.toLowerCase().startsWith("y")) {
						return;
					}
				}

				p.uninstall();
				break;
			}
		}

		install(temp);
		installed = true;
		System.out.println("Installed: " + name + " " + version);
	}

	protected void install(File temp) throws Exception {
		List<String> files = new ArrayList<String>();
		if (type == PluginType.JAR) {
			if (BukkitPackageManager.verbose) {
				System.out.println("Install type: jar");
			}
			File dest = new File(BukkitPackageManager.plugins, name + "-" + version + ".jar");
			if (BukkitPackageManager.verbose) {
				System.out.println(temp + " -> " + dest);
			}
			temp.renameTo(dest);
			files.add(dest.getPath());
			location = dest;
		} else {
			if (BukkitPackageManager.verbose) {
				System.out.println("Install type: zip");
			}
			ZipFile source = new ZipFile(temp);
			Enumeration<? extends ZipEntry> entries = source.entries();
			while (entries.hasMoreElements()) {
				ZipEntry zipEnt = entries.nextElement();
				File copy = copyOut(source, zipEnt);
				File dest = new File(BukkitPackageManager.plugins, zipEnt.getName());
				if (BukkitPackageManager.verbose) {
					System.out.println(copy + " -> " + dest);
				}
				dest.getParentFile().mkdirs();
				copy.renameTo(dest);
				files.add(dest.getPath());
				if (zipEnt.getName().endsWith(".jar")) {
					if (BukkitPackageManager.verbose) {
						System.out.println("Main jar: " + zipEnt.getName());
					}
					location = dest;
				}
			}
			source.close();
		}

		Map<String, PluginInfo> manifest = readBpmManifest();
		if (manifest == null) {
			manifest = new HashMap<String, PluginInfo>();
		}
		if (manifest.containsKey(name)) {
			manifest.remove(name);
		}
		manifest.put(name, new PluginInfo(name, version, files));
		writeBpmManifest(manifest);
	}

	protected void getPackageInfoFromArchive(File jar) throws Exception {
		PluginInfo manifest = getManifest(jar);
		name = manifest.name;
		version = manifest.version;
	}

	protected PluginInfo getManifest(File jar) throws Exception {
		ZipFile zip;
		ZipEntry ent;
		try {
			zip = new ZipFile(jar);
			ent = zip.getEntry("plugin.yml");
		} catch(Exception e){
			throw new Exception("Not a valid plugin file, check the download manually", e);
		}
		if (ent == null) {
			type = PluginType.ZIP;
			Enumeration<? extends ZipEntry> entries = zip.entries();
			while (entries.hasMoreElements()) {
				ZipEntry zipEnt = entries.nextElement();
				if (zipEnt.getName().endsWith(".jar")) {
					if (BukkitPackageManager.verbose) {
						System.out.println("Main jar: " + zipEnt.getName());
					}
					File res = copyOut(zip, zipEnt);
					zip.close();
					zip = new ZipFile(res);
					ent = zip.getEntry("plugin.yml");
					break;
				}
			}
		}
		if (BukkitPackageManager.verbose) {
			System.out.println("Reading manifest: " + name + " " + location);
		}
		YamlReader reader = new YamlReader(new InputStreamReader(zip.getInputStream(ent)));
		@SuppressWarnings("unchecked")
		Map<String, ?> props = (Map<String, ?>) reader.read();
		reader.close();
		zip.close();
		return new PluginInfo(props.get("name").toString(), props.get("version").toString(), null);
	}

	protected File copyOut(ZipFile parent, ZipEntry which) throws IOException {
		File dest = File.createTempFile("bpm-" + name, null);
		if (BukkitPackageManager.verbose) {
			System.out.println(parent.getName() + "!" + which.getName() + " -> " + dest);
		}
		InputStream in = parent.getInputStream(which);
		OutputStream out = new FileOutputStream(dest);
		byte[] buffer = new byte[1024 * 1024 * 8];
		int len;
		while ((len = in.read(buffer)) != -1) {
			out.write(buffer, 0, len);
		}
		in.close();
		out.close();
		return dest;
	}

	protected Map<String, PluginInfo> readBpmManifest() throws Exception {
		File manifestFile = new File(BukkitPackageManager.plugins, "bpm.yml");
		if (manifestFile.exists() && manifestFile.isFile()) {
			YamlReader reader = new YamlReader(new FileReader(manifestFile));
			@SuppressWarnings("unchecked")
			Map<String, PluginInfo> props = (Map<String, PluginInfo>) reader.read();
			reader.close();
			return props;
		} else {
			return new HashMap<String, PluginInfo>();
		}
	}

	protected void writeBpmManifest(Map<String, PluginInfo> manifest) throws Exception {
		File manifestFile = new File(BukkitPackageManager.plugins, "bpm.yml");
		YamlWriter writer = new YamlWriter(new FileWriter(manifestFile));
		writer.write(manifest);
		writer.close();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((version == null) ? 0 : version.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof Plugin)) {
			return false;
		}
		Plugin other = (Plugin) obj;
		if (name == null) {
			if (other.name != null) {
				return false;
			}
		} else if (!name.equals(other.name)) {
			return false;
		}
		if (version == null) {
			if (other.version != null) {
				return false;
			}
		} else if (!version.equals(other.version)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return name + " " + version + " at " + location;
	}

	@Override
	public int compareTo(Plugin o) {
		return toString().compareTo(o.toString());
	}

	public static class PluginInfo {
		public String name;
		public String version;
		public List<String> installedFiles;

		public PluginInfo() {
		}

		public PluginInfo(String name, String version, List<String> installedFiles) {
			super();
			this.name = name;
			this.version = version;
			this.installedFiles = installedFiles;
		}
	}

	public static Set<Plugin> installed() throws Exception {
		Set<Plugin> list = new HashSet<>();

		File[] jars = BukkitPackageManager.plugins.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".jar");
			}
		});

		for (File jar : jars) {
			list.add(new Plugin(jar));
		}

		return list;
	}
}
