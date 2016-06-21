package bpm;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public interface Source {
	public static final List<Source> SOURCES = new ArrayList<Source>() {
		private static final long serialVersionUID = -8319448037268263110L;
		{
			add(new SourceBukkitDev());
			// TODO: add Spigot as a source
		}
	};

	public File downloadPlugin(String name) throws Exception;

	public List<String> searchForPackage(String search) throws Exception;

	public String getPackageDetails(String name) throws Exception;
}
