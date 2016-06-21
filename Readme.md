# Bukkit Package Manager #

A command-line tool that makes it easy to manage Bukkit (CraftBukkit, Spigot) plugins. Uses `dev.bukkit.org` and `spigotmc.org` to find plugins.

    Usage: bpm [options] <action> [package [package ...]]
    
    
    Options:
    
        -a --all-details        Displays full details with 'details'
        -b --bukkit-only        Don't use Spigot to search for plugins
        -f --force              Overwrite plugins without prompting
        -v --verbose            Verbose output
    
    Actions:
    
        details                 Get details of a package
        install                 Install packages for bukkit
        installServer           Install a version of craftbukkit or spigot
        list                    List installed packages
        listServer              List all installed server versions
        search                  Search for a package
        uninstall               Uninstall packages
        uninstallServer         Uninstall a version of craftbukkit or spigot
        upgrade                 Upgrade packages to the latest version. Uses -f
        upgradeServer           Upgrade the current version of craftbukkit or spigot

## How to use ##

On Windows, copy the contents of `dist/` somewhere onto your `PATH`