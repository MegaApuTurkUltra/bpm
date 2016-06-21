package bpm;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public interface Source {
	public static final List<Source> SOURCES = new ArrayList<Source>();

	public File downloadPlugin(String name) throws Exception;

	public List<String> searchForPackage(String search) throws Exception;

	public String getPackageDetails(String name) throws Exception;
	
	public String getName();
}
