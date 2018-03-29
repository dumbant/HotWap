package com.sin.application;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ClassUtil {
	
	public static void main(String[] args) {
		getClasses("com.sin.app");
	}

	/**
	 * 从包package中获取所有的Class
	 * 
	 * @param packageName
	 * @return
	 */
	public static Map<String,Class<?>> getClasses(String packageName) {
		// 第一个class类的集合
		Map<String,Class<?>> classes = new HashMap<String,Class<?>>();
		// 是否循环迭代
		boolean recursive = true;
		// 获取包的名字 并进行替换
		//String packageName = pack.getName();
		String packageDirName = packageName.replace('.', '/');
		if(!packageDirName.endsWith("/")){
			packageDirName += "/";
		}
		// 定义一个枚举的集合 并进行循环来处理这个目录下的things
		Enumeration<URL> dirs;
		try {
			dirs = Thread.currentThread().getContextClassLoader().getResources(packageDirName);
			// 循环迭代下去
			while (dirs.hasMoreElements()) {
				// 获取下一个元素
				URL url = dirs.nextElement();
				
				// 得到协议的名称
				String protocol = url.getProtocol();
				// 如果是以文件的形式保存在服务器上
				if ("file".equals(protocol)) {
					// 获取包的物理路径
					String filePath = URLDecoder.decode(url.getFile(), "UTF-8");
					// 以文件的方式扫描整个包下的文件 并添加到集合中
					findAndAddClassesInPackageByFile(packageName, filePath, recursive, classes);
				} else{
					findClassesByJar(url, packageDirName, classes);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		int count = classes.size();
		System.out.println("class size" + count);
		return classes;
	}
	
	/**
	 * 以文件的形式来获取包下的所有Class
	 * 
	 * @param packageName
	 * @param packagePath
	 * @param recursive
	 * @param classes
	 */
	public static void findAndAddClassesInPackageByFile(String packageName, String packagePath, final boolean recursive, Map<String,Class<?>> classes) {
		// 获取此包的目录 建立一个File
		File dir = new File(packagePath);
		// 如果不存在或者 也不是目录就直接返回
		if (!dir.exists() || !dir.isDirectory()) {
			return;
		}
		// 如果存在 就获取包下的所有文件 包括目录
		File[] dirfiles = dir.listFiles(new FileFilter() {
			// 自定义过滤规则 如果可以循环(包含子目录) 或则是以.class结尾的文件(编译好的java类文件)
			public boolean accept(File file) {
				return (recursive && file.isDirectory()) || (file.getName().endsWith(".class"));
			}
		});

		// 循环所有文件

		for (File file : dirfiles) {
			// 如果是目录 则继续扫描
			if (file.isDirectory()) {
				findAndAddClassesInPackageByFile(packageName + "." + file.getName(), file.getAbsolutePath(), recursive, classes);
			} else {
				// 如果是java类文件 去掉后面的.class 只留下类名
				String className = file.getName().substring(0, file.getName().length() - 6);
				URLClassLoader newLoader = null;
				try {
					if (packagePath.charAt(0) == '/') {
						// 获取后面的字符串
						packagePath = packagePath.substring(1);
					}
					String classPath = Util.joinPath(packagePath,file.getName());
					File classfile = new File(classPath);
					URL[] urls = {classfile.toURI().toURL()};
					
					newLoader = new URLClassLoader(urls, ClassUtil.class.getClassLoader());
					className = packageName + '.' + className;
					Class<?> obj = newLoader.loadClass(className);
					classes.put(className,obj);
					
					// 添加到集合中去
					//classes.add(Class.forName(packageName + '.' + className));

				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				} catch (MalformedURLException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public static void findClassesByJar(File jarFile, String packageDirName,Map<String,Class<?>> classes){
		try {
			URL url = jarFile.toURI().toURL();
			findClassesByJar(url,packageDirName, classes);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * @param url
	 * @param packageDirName
	 * @param classes
	 */
	public static void findClassesByJar(URL url, String packageDirName,Map<String,Class<?>> classes){
		try {
			// 得到协议的名称
			String protocol = url.getProtocol();
			String packageName = null;
			
			String jarPath = url.toString();
			if(jarPath.indexOf(".jar")< 0){
				return;
			}
			if(jarPath.indexOf(".jar!/") > 0){
				jarPath = jarPath.substring(0,jarPath.indexOf(".jar!/")+6);
			}
			
			JarFile jar ;
			if ("jar".equals(protocol)) {
				// 获取jar
				jar = ((JarURLConnection) url.openConnection()).getJarFile();
				if(jarPath.endsWith(".jar")){
					jarPath += "!/";
				}
			}else if ("file".equals(protocol)) {
				String filePath = url.toString();
				filePath = filePath.substring(filePath.indexOf(":")+1);
				if(!filePath.endsWith(".jar")){
					filePath = filePath.substring(0,filePath.indexOf(".jar")+4);
				}
				jar = new JarFile(filePath);
			}else{
				System.out.println("未知类型路径: "+ url);
				return;
			}
			
			// 从此jar包 得到一个枚举类
			Enumeration<JarEntry> entries = jar.entries();
			// 同样的进行循环迭代
			while (entries.hasMoreElements()) {
				// 获取jar里的一个实体 可以是目录 和一些jar包里的其他文件 如META-INF等文件
				JarEntry entry = entries.nextElement();
				String name = entry.getName();
				// 如果是以/开头的
				if (name.charAt(0) == '/') {
					// 获取后面的字符串
					name = name.substring(1);
				}
				
				// 如果前半部分和定义的包名相同
				if (packageDirName!=null && !packageDirName.isEmpty() && !name.startsWith(packageDirName)) {
					continue;
				}
				
				int idx = name.lastIndexOf('/');
				if (idx != -1) {
					// 获取包名 把"/"替换成"."
					packageName = name.substring(0, idx).replace('/', '.');
				}
				// 如果是一个.class文件 而且不是目录
				if (name.endsWith(".class") && !entry.isDirectory()) {
					// 去掉后面的".class" 获取真正的类名
					String className = name.substring(packageName.length() + 1, name.length() - 6);
					URLClassLoader newLoader = null;
					try {
						String classPath = Util.joinPath(jarPath,name);
						URL classURL = new URL(classPath);
						URL[] urls = {classURL};
						
						newLoader = new URLClassLoader(urls, ClassUtil.class.getClassLoader());
						className = packageName + '.' + className;
						Class<?> obj = newLoader.loadClass(className);
						classes.put(className,obj);
						// 添加到classes
						//classes.add(Class.forName(packageName + '.' + className));
					} catch (Exception e) {
						e.printStackTrace();
					}finally{
						if(newLoader != null){
							newLoader.close();
						}
					}
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
