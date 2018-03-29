package com.sin.application.jarloader;

public class HotSwapCL extends ClassLoader{
	
	public HotSwapCL(String className, byte[] raw) {
		super(null);
		try{
			defineClass(className,raw,0,raw.length);
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	protected Class<?> loadClass(String name,boolean resolve) throws ClassNotFoundException{
		Class<?> cls = null ;
		cls = findLoadedClass(name);
		if(cls == null){
			//System.out.println("HotSwapCL.loadClass: " + name);
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
