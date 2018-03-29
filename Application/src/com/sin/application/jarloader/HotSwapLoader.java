package com.sin.application.jarloader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.sin.application.IApplication;

public class HotSwapLoader extends ClassLoader{

	private String basedir ; //class根目录
	private String[] packageArr ; //需要自定义load的包名
	private HashSet<String> dynaclazns;
	
	public static void main(String[] args) throws Exception {
		String basedir = "E:/work/workspace/workspace/Application/patch/TestApplication/";
		String[] packagearr = {"com.sin.app"};
		HotSwapLoader cl = new HotSwapLoader(basedir, packagearr);
		Class<?> cls = cl.loadClass("com.sin.app.TestApplication");
		IApplication app = (IApplication)cls.newInstance();
		
		System.out.println(app.getClass().getClassLoader());
		app.init();
	}
	
	public HotSwapLoader(String basedir, String... packageNames) {
		super(null);
		basedir = basedir.replace("\\", "/");
		if(!basedir.endsWith("/")) {
			basedir += "/";
		}
		this.basedir = basedir;
		if( packageNames != null) {
			this.packageArr = new String[packageNames.length];
			for (int i=0;i<packageNames.length;i++) {
				String packagename = packageNames[i];
				packagename = packagename.replace(".", "/");
				if(!packagename.endsWith("/")) {
					packagename += "/";
				}
				this.packageArr[i] = packagename;
			}
		}
		dynaclazns = new HashSet<String>();
		
		loadClassByMe(basedir);
	}
	
	private void loadClassByMe(String path) {
		File dir = new File(path);
		// 如果不存在或者 也不是目录就直接返回
		if (!dir.exists() || !dir.isDirectory()) {
			return;
		}
		if(!path.endsWith("/")) {
			path += "/";
		}
		
		String[] subarr = dir.list();
		for (String subname : subarr) {
			if(subname.endsWith(".class")){
				String subpath = path+subname ;
				loadClassByClassFile(subpath);
			}else if(subname.endsWith(".jar")){
				String subpath = path+subname ;
				loadClassesByJar(subpath);
			}else{
				String subpath = path+subname ;
				File subfile = new File(subpath);
				if(subfile.isDirectory()) {
					loadClassByMe(subpath);
				}
			}
		}
	}
	
	private void loadClassesByJar(String jarPath){
		if(jarPath.indexOf(".jar") < 0){
			return;
		}
		if(jarPath.startsWith("file:")) {
			jarPath = jarPath.substring(jarPath.indexOf(":")+1);
		}
		if(!jarPath.endsWith(".jar")){
			jarPath = jarPath.substring(0,jarPath.indexOf(".jar")+4);
		}
		
		JarFile jar ;
		try {
			jar  = new JarFile(jarPath);
			
			ByteBuffer buff = ByteBuffer.allocate(1024*200);
			
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
				boolean b = checkPackage(name);
				if(!b){
					continue;
				}
				
				// 如果是一个.class文件 而且不是目录
				if (name.endsWith(".class") && !entry.isDirectory()) {
					// 去掉后面的".class" 获取真正的类名
					String className = name.substring(0, name.lastIndexOf(".class")).replace("/", ".");
					try{
						InputStream fin = jar.getInputStream(entry);
						
						byte[] buf = new byte[1024];
						int len ;
						while( (len= fin.read(buf)) != -1){
							buff.put(buf,0,len);
						}
						fin.close();
						
						buff.flip();
						byte[] raw = new byte[buff.limit()];
						buff.get(raw);
						
						defineClass(className,raw,0,raw.length);
						dynaclazns.add(className);
						
						buff.clear();
					}catch(Exception e){
						e.printStackTrace();
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private Class<?> loadClassByClassFile(String classFilePath){
		Class<?> cls = null;
		String className = classFilePath.substring(basedir.length(), classFilePath.lastIndexOf(".class"));
		boolean b = checkPackage(className);
		if(!b){
			return cls;
		}
		File classf = new File( classFilePath );
		InputStream in = null;
		try {
			in = new FileInputStream(classf);
			byte[] raw = new byte[(int)classf.length()];
			in.read(raw);
			
			cls = defineClass(className,raw,0,raw.length);
			dynaclazns.add(className);
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			if( in != null) {
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return cls;
	}
	
	private boolean checkPackage(String className){
		boolean b = false;
		if( packageArr != null && packageArr.length > 0) {
			for (String packagename : packageArr) {
				if(className.startsWith(packagename)) {
					b = true;
					break;
				}
			}
		}else{
			b = true;
		}
		return b;
	}
	
	protected Class<?> loadClass(String name,boolean resolve) throws ClassNotFoundException{
		Class<?> cls = null ;
		cls = findLoadedClass(name);
		if(!this.dynaclazns.contains(name) && cls == null) {
			cls = getSystemClassLoader().loadClass(name);
		}
		if(cls == null){
			throw new ClassNotFoundException(name);
		}
		if(resolve){
			resolveClass(cls);
		}
		return cls;
	}
}
