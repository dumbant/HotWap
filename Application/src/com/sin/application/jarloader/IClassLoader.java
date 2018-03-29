package com.sin.application.jarloader;

public interface IClassLoader {

	/**
	 * Create a new class loader for all the jar files in the specified folder or folders
	 * @param parentClassLoader
	 * @param paths
	 * @return
	 */
	public ClassLoader createClassLoader(ClassLoader parentClassLoader, String...paths);
}
