package bpm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class BukkitPackageManager {
	public static final String VERSION = "0.0.2";
	
	public static File plugins;
	public static BufferedReader consoleIn;

	@Switch(longOpt = "verbose", shortOpt = "v", description = "Verbose output")
	public static boolean verbose = false;

	@Switch(longOpt = "all-details", shortOpt = "a", description = "Displays full details with 'details'")
	public static boolean fullDetails = false;

	@Switch(longOpt = "bukkit-only", shortOpt = "b", description = "Don't use Spigot to search for plugins")
	public static boolean bukkitOnly = false;

	@Switch(longOpt = "force", shortOpt = "f", description = "Overwrite plugins without prompting")
	public static boolean noPromptOverwrite = false;

	@Switch(longOpt = "purge", shortOpt = "p", description = "Delete plugins data folders after an uninstall")
	public static boolean purge = false;

	@Action(description = "Install packages for bukkit")
	public static void install(List<String> packages) throws Exception {
		for (String pkg : packages) {
			Plugin p = new Plugin(pkg);
			try {
				p.download(null);
			} catch (Exception e) {
				System.out.print('\r');
				if (verbose) {
					e.printStackTrace();
				} else {
					System.err.println(e.getMessage());
				}
			}
		}
	}
	
	@Action(description = "Install a package by its download link (zip or jar)")
	public static void installByUrl(List<String> packages) throws Exception {
		StringBuilder pkgBuilder = new StringBuilder();
		for (String p : packages) {
			pkgBuilder.append(p);
			pkgBuilder.append(' ');
		}
		pkgBuilder.setLength(Math.max(pkgBuilder.length() - 1, 0));
		String url = pkgBuilder.toString();
		
		Plugin p = new Plugin((String) null);
		try {
			p.download(url);
		} catch (Exception e) {
			System.out.print('\r');
			if (verbose) {
				e.printStackTrace();
			} else {
				System.err.println(e.getMessage());
			}
		}
	}

	@Action(description = "Upgrade packages to the latest version. Uses -f")
	public static void upgrade(List<String> packages) throws Exception {
		noPromptOverwrite = true;
		Set<Plugin> installed = Plugin.installed();
		List<String> names = new ArrayList<String>();
		for (Plugin p : installed) {
			names.add(p.name);
		}
		install(names);
	}

	@Action(description = "Uninstall packages")
	public static void uninstall(List<String> packages) throws Exception {
		Set<Plugin> installed = Plugin.installed();

		for (String pkg : packages) {
			boolean found = false;
			for (Plugin p : installed) {
				if (p.name.equalsIgnoreCase(pkg)) {
					try {
						p.uninstall();
					} catch (Exception e) {
						if (verbose) {
							e.printStackTrace();
						} else {
							System.err.println(e.getMessage());
						}
					}
					found = true;
					break;
				}
			}
			if (!found) {
				System.err.println("No such plugin installed: " + pkg + ", skipping");
			}
		}
	}

	@Action(description = "List installed packages")
	public static void list(List<String> packages) throws Exception {
		Set<Plugin> installed = null;
		try {
			installed = Plugin.installed();
		} catch (Exception e) {
			if (verbose) {
				e.printStackTrace();
			} else {
				System.err.println(e.getMessage());
			}
			return;
		}
		TreeSet<Plugin> installedSort = new TreeSet<Plugin>(installed);
		for (Plugin p : installedSort) {
			System.out.println(p);
		}
		if (installedSort.size() == 0) {
			System.out.println("(No plugins)");
		}
	}

	@Action(description = "Search for a package")
	public static void search(List<String> packages) throws Exception {
		StringBuilder pkgBuilder = new StringBuilder();
		for (String p : packages) {
			pkgBuilder.append(p);
			pkgBuilder.append(' ');
		}
		pkgBuilder.setLength(Math.max(pkgBuilder.length() - 1, 0));
		String pkg = pkgBuilder.toString();
		System.out.println(pkg + ":");
		System.out.print("\rRetrieving...");
		List<String> results = new LinkedList<String>();
		for (Source src : Source.SOURCES) {
			try {
				List<String> sourceResults = src.searchForPackage(pkg);
				results.addAll(sourceResults);
			} catch (Exception e) {
				if (verbose) {
					System.out.print("\r              \n");
					e.printStackTrace();
				}
			}
		}
		Collections.sort(results);
		System.out.print('\r');
		for (String s : results) {
			System.out.println("    " + s);
		}
		if (results.size() == 0) {
			System.out.println("    (No results)");
		}
		System.out.println();
	}

	@Action(description = "Get details of a package")
	public static void details(List<String> packages) throws Exception {
		for (String pkg : packages) {
			System.out.println(pkg + ":");
			System.out.print("\rRetrieving...");
			String desc = null;
			try {
				Exception last = new Exception("No such package: " + pkg);
				for (Source src : Source.SOURCES) {
					try {
						desc = src.getPackageDetails(pkg);
						break;
					} catch (Exception e) {
						last = e;
						if (verbose) {
							System.out.println("[" + src.getName() + "] " + e.getMessage());
						}
					}
				}
				if (desc == null) {
					throw last;
				}
			} catch (Exception e) {
				System.out.print("\r              \n");
				if (verbose) {
					e.printStackTrace();
				} else {
					System.err.println(e.getMessage());
				}
				continue;
			}
			System.out.print("\r\t");
			System.out.println(desc);
			System.out.println();
		}
	}

	@Action(description = "Install a version of craftbukkit or spigot")
	public static void installServer(List<String> args) {
		String distro = null, version = null;
		for (String s : args) {
			char first = s.charAt(0);
			if (s.equals("latest") || (first >= '0' && first <= '9')) {
				version = s;
			} else {
				distro = s;
			}
		}
		if (distro == null || version == null) {
			System.err.println("Usage: bpm installServer <craftbukkit|spigot> <version|latest>");
			System.exit(1);
		}
		try {
			BuildToolsManager.buildServer(distro, version);
		} catch (Exception e) {
			System.out.print("\r              \n");
			if (verbose) {
				e.printStackTrace();
			} else {
				System.err.println(e.getMessage());
			}
		}
	}

	@Action(description = "List all installed server versions")
	public static void listServer(List<String> args) {
		List<String> list = BuildToolsManager.list();
		Collections.sort(list);
		System.out.println("Installed servers:");
		for (String item : list) {
			System.out.println("    " + item);
		}
		if (list.size() == 0) {
			System.out.println("    (No servers)");
		}
	}

	@Action(description = "Uninstall a version of craftbukkit or spigot")
	public static void uninstallServer(List<String> args) {
		String distro = null, version = null;
		for (String s : args) {
			char first = s.charAt(0);
			if (s == "latest" || (first >= '0' && first <= '9')) {
				version = s;
			} else {
				distro = s;
			}
		}
		if (distro == null || version == null) {
			System.err.println("Usage: bpm uninstallServer <craftbukkit|spigot> <version|latest>");
			System.exit(1);
		}
		File jar = new File(distro + "-" + version + ".jar");
		if (jar.delete()) {
			System.out.println("Uninstalled: " + distro + " " + version);
		} else {
			System.err.println("Unable to delete: " + jar);
		}
	}

	@Action(description = "Upgrade the current version of craftbukkit or spigot")
	public static void upgradeServer(List<String> args) {
		String distro = null;
		if (args.size() > 0)
			distro = args.get(0);
		if (distro == null) {
			System.err.println("Usage: bpm upgradeServer <craftbukkit|spigot>");
			System.exit(1);
		}
		try {
			BuildToolsManager.removeVersions();
			BuildToolsManager.buildServer(distro, "latest");
		} catch (Exception e) {
			System.out.print("\r              \n");
			if (verbose) {
				e.printStackTrace();
			} else {
				System.err.println(e.getMessage());
			}
		}
	}

	public static void main(String[] args) throws Exception {
		File pluginsFolder = new File("plugins/");
		if (!pluginsFolder.exists() || !pluginsFolder.isDirectory()) {
			System.out.println("No plugins folder, creating...");
			if (!pluginsFolder.mkdirs()) {
				System.err.println("Unable to create plugins folder!");
				System.exit(-1);
			}
		}
		plugins = pluginsFolder;

		consoleIn = new BufferedReader(new InputStreamReader(System.in));

		String action = null;
		List<String> packages = new ArrayList<String>();
		for (String str : args) {
			for (Field field : BukkitPackageManager.class.getDeclaredFields()) {
				if (field.isAnnotationPresent(Switch.class)) {
					Switch opt = field.getAnnotation(Switch.class);
					if (str.equals("-" + opt.shortOpt()) || str.equals("--" + opt.longOpt())
							|| (str.charAt(0) == '-' && str.charAt(1) != '-' && str.contains(opt.shortOpt()))) {
						field.setBoolean(null, true);
					}
				}
			}

			if (action == null && !str.startsWith("-")) {
				action = str;
			} else if (action != null && !str.startsWith("-")) {
				packages.add(str);
			}
		}

		Source.SOURCES.add(new SourceBukkitDev());
		if (!bukkitOnly) {
			Source.SOURCES.add(new SourceSpigotMc());
		}

		Map<String, Action> actions = new TreeMap<String, Action>();
		for (Method method : BukkitPackageManager.class.getDeclaredMethods()) {
			if (method.isAnnotationPresent(Action.class)) {
				actions.put(method.getName(), method.getAnnotation(Action.class));
				if (action != null && action.equals(method.getName())) {
					method.invoke(null, packages);
					cleanup();
					return;
				}
			}
		}

		Map<String, Switch> options = new TreeMap<String, Switch>();
		for (Field field : BukkitPackageManager.class.getDeclaredFields()) {
			if (field.isAnnotationPresent(Switch.class)) {
				Switch opt = field.getAnnotation(Switch.class);
				options.put("-" + opt.shortOpt() + " --" + opt.longOpt(), opt);
			}
		}

		//@formatter:off
		System.err.println("\nBukkit Package Manager " + VERSION + "\n"
				+ "\n"
				+ "Usage: bpm [options] <action> [package [package ...]]\n"
				+ "\n");
		System.err.println("Options:\n");
		//@formatter:on

		for (String o : options.keySet()) {
			System.err.print("    " + o);
			for (int i = 0; i < 24 - o.length(); i++)
				System.err.print(' ');
			System.err.println(options.get(o).description());
		}

		System.err.println("\nActions:\n");
		for (String a : actions.keySet()) {
			System.err.print("    " + a);
			for (int i = 0; i < 24 - a.length(); i++)
				System.err.print(' ');
			System.err.println(actions.get(a).description());
		}
	}

	protected static void cleanup() throws IOException {
		File[] bpmFiles = File.createTempFile("bpm", null).getParentFile().listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.startsWith("bpm");
			}
		});
		for (File f : bpmFiles) {
			f.deleteOnExit();
		}
	}
}
