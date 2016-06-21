package bpm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

public class BuildToolsManager {
	protected static void downloadBuildTools() throws Exception {
		String download = "https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/artifact/target/BuildTools.jar";
		File btDir = new File("BuildTools");
		btDir.mkdir();
		File dest = new File(btDir, "BuildTools.jar");

		System.out.print("Downloading latest BuildTools...\n[Connecting...       ] 0%");
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
	}

	protected static void checkConditions() throws Exception {
		Process proc;
		try {
			proc = Runtime.getRuntime().exec("git --version");
		} catch (Exception e) {
			throw new Exception("Git not installed", e);
		}
		proc.waitFor();
		if (proc.exitValue() != 0) {
			throw new Exception("Git not installed");
		}
	}
	
	protected static void clean(File dir) throws Exception {
		if(dir == null)
			dir = new File("BuildTools");
		File[] oldBuilds = dir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".jar") && (name.startsWith("craftbukkit") || name.startsWith("spigot"));
			}
		});
		for(File build: oldBuilds){
			System.out.println("Deleting: " + build);
			build.delete();
		}
	}
	
	protected static void displayProgress(double percent, String msg){
		int percent0 = (int) (percent / 5);
		System.out.print("\r[");
		for (int i = 0; i < 20; i++) {
			System.out.print(i < percent0 ? '|' : ' ');
		}
		System.out.print("] " + ((int) (percent)) + "% " + msg);
	}

	protected static void runBuildTools(String version) throws Exception {
		System.out.println("Running BuildTools...");
		System.out.print("[Starting...         ] 0%");
		File javaLoc = new File(new File(System.getProperty("java.home"), "bin"), "java");
		if (!javaLoc.exists()) {
			javaLoc = new File(javaLoc.getAbsolutePath() + ".exe");
			if (!javaLoc.exists()) {
				throw new Exception("Unable to find Java location");
			}
		}
		ProcessBuilder procBuilder = new ProcessBuilder(javaLoc.getAbsolutePath(), "-jar", "BuildTools.jar", "--rev",
				version);
		procBuilder.directory(new File("BuildTools"));
		procBuilder.redirectErrorStream(true);
		Process proc = procBuilder.start();
		BufferedReader in = new BufferedReader(new InputStreamReader(proc.getInputStream()));
		String s;
		displayProgress(0, "Downloading Minecraft");
		while((s = in.readLine()) != null){
			if(s.contains("Loading mappings")){
				displayProgress(10, "Decompiling vanilla server");
			} else if(s.contains("Applying CraftBukkit Patches")){
				displayProgress(20, "Patching for CraftBukkit  ");
			} else if(s.contains("Compiling Bukkit")){
				displayProgress(30, "Compiling Bukkit          ");
			} else if(s.contains("Compiling CraftBukkit")) {
				displayProgress(40, "Compiling CraftBukkit     ");
			} else if(s.contains("Rebuilding Forked projects....")){
				displayProgress(60, "Patching for Spigot       ");
			} else if(s.contains("Compiling Spigot & Spigot-API")){
				displayProgress(70, "Compiling Spigot          ");
			} else if(s.contains("Success! Everything compiled successfully.")){
				displayProgress(99, "Finishing                 ");
			}
		}
		in.close();
		int exitVal = proc.exitValue();
		System.out.println("\r[||||||||||||||||||||] 100%");
		System.out.println("BuildTools exited with: " + exitVal);
	}
	
	public static List<String> list() {
		File btDir = new File(".");
		File[] jars = btDir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".jar");
			}
		});
		List<String> list = new LinkedList<String>();
		for(File jar: jars){
			String name = jar.getName();
			if(name.contains("-")){
				String[] parts = name.replace(".jar", "").split("\\-");
				list.add(parts[0] + " " + parts[1]);
			}
		}
		return list;
	}
	
	protected static File findJar(String distro) throws Exception {
		File btDir = new File("BuildTools");
		File[] jars = btDir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".jar");
			}
		});
		for(File jar: jars){
			if(jar.getName().startsWith(distro)){
				return jar;
			}
		}
		throw new Exception("Can't find build for: " + distro);
	}
	
	public static void removeVersions() throws Exception {
		clean(new File("."));
	}

	public static void buildServer(String distro, String version) throws Exception {
		checkConditions();
		downloadBuildTools();
		clean(new File("BuildTools"));
		runBuildTools(version);
		
		File jar = findJar(distro);
		File dest = new File(jar.getName());
		if(dest.exists() && !BukkitPackageManager.noPromptOverwrite){
			System.out.print("Overwrite: " + dest.getName() + "? [y/n] ");
			String resp = BukkitPackageManager.consoleIn.readLine();
			if (!resp.toLowerCase().startsWith("y")) {
				return;
			}
		}
		jar.renameTo(dest);
		System.out.println(dest);
	}
}
