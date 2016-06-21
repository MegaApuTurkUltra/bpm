package bpm;

import java.io.Console;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class BukkitPackageManager {
	public static File plugins;
	public static Console console;

	@Switch(longOpt = "verbose", shortOpt = "v", description = "Verbose output")
	public static boolean verbose = false;

	@Switch(longOpt = "full-details", shortOpt = "f", description = "Action 'details' displays full details")
	public static boolean fullDetails = false;

	@Action(description = "Install packages from bukkit.org")
	public static void install(List<String> packages) throws Exception {
		for (String pkg : packages) {
			Plugin p = new Plugin(pkg);
			try {
				p.download();
			} catch (Exception e) {
				System.out.print("\r                          \n");
				if (verbose) {
					e.printStackTrace();
				} else {
					System.err.println(e.getMessage());
				}
			}
		}
	}

	@Action(description = "Uninstall packages")
	public static void uninstall(List<String> packages) throws Exception {
		Set<Plugin> installed = Plugin.installed();

		for (String pkg : packages) {
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
					break;
				}
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
	}

	@Action(description = "Search bukkit.org for a package")
	public static void search(List<String> packages) throws Exception {
		for (String pkg : packages) {
			System.out.println(pkg + ":");
			System.out.print("\rRetrieving...");
			List<String> results = null;
			try {
				results = Source.SOURCES.get(0).searchForPackage(pkg);
			} catch (Exception e) {
				System.out.print("\r              \n");
				if (verbose) {
					e.printStackTrace();
				} else {
					System.err.println(e.getMessage());
				}
				continue;
			}
			System.out.print('\r');
			for (String s : results) {
				System.out.println("\t" + s);
			}
			System.out.println();
		}
	}

	@Action(description = "Get details of a package from bukkit.org")
	public static void details(List<String> packages) throws Exception {
		for (String pkg : packages) {
			System.out.println(pkg + ":");
			System.out.print("\rRetrieving...");
			String desc;
			try {
				desc = Source.SOURCES.get(0).getPackageDetails(pkg);
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

	public static void main(String[] args) throws Exception {
		File pluginsFolder = new File("plugins/");
		if (!pluginsFolder.exists() || !pluginsFolder.isDirectory()) {
			System.err.println("Plugins folder not present");
			System.exit(-1);
		}
		plugins = pluginsFolder;

		console = System.console();
		if (console == null) {
			System.err.println("No usable console present");
			System.exit(-1);
		}

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

		Map<String, Action> actions = new TreeMap<String, Action>();
		for (Method method : BukkitPackageManager.class.getDeclaredMethods()) {
			if (method.isAnnotationPresent(Action.class)) {
				actions.put(method.getName(), method.getAnnotation(Action.class));
				if (action != null && action.equals(method.getName())) {
					method.invoke(null, packages);
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
		System.err.println("\nBukkit Package Manager\n"
				+ "\n"
				+ "Usage: bpm [options] <action> [package [package ...]]\n"
				+ "\n");
		System.err.println("Options:\n");
		//@formatter:on

		for (String o : options.keySet()) {
			System.err.print("    " + o);
			for(int i = 0; i < 24 - o.length(); i++) System.err.print(' ');
			System.err.println(options.get(o).description());
		}

		System.err.println("\nActions:\n");
		for (String a : actions.keySet()) {
			System.err.print("    " + a);
			for(int i = 0; i < 24 - a.length(); i++) System.err.print(' ');
			System.err.println(actions.get(a).description());
		}
	}
}
