# Bukkit Package Manager #

A command-line tool that makes it easy to manage Bukkit (CraftBukkit, Spigot) plugins. Uses `dev.bukkit.org` to find plugins.

    Usage: bpm [options] <action> [package [package ...]]
    
    
    Options:
    
        -f --full-details       Action 'details' displays full details
        -v --verbose            Verbose output
    
    Actions:
    
        details                 Get details of a package from bukkit.org
        install                 Install packages from bukkit.org
        list                    List installed packages
        search                  Search bukkit.org for a package
        uninstall               Uninstall packages

## How to use ##

On Windows, copy the contents of `dist/` somewhere onto your `PATH`