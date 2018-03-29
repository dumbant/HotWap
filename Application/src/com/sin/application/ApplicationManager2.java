package com.sin.application;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.vfs2.FileListener;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.VFS;
import org.apache.commons.vfs2.impl.DefaultFileMonitor;

import com.sin.application.config.AppConfig;
import com.sin.application.config.AppConfigManager;
import com.sin.application.config.GlobalSetting;
import com.sin.application.jarloader.CLFactory;
import com.sin.application.jarloader.IClassLoader;
import com.sin.application.jarloader.SimpleJarLoader;

public class ApplicationManager2 {
	public static Map<String,Class<?>> classes = new HashMap<String,Class<?>>();
	private static ApplicationManager2 instance;
	
	private CLFactory factory ;
	private AppConfigManager configManager;	
	private FileSystemManager fileManager;
	private DefaultFileMonitor fileMonitor;
	
	private Map<String, IApplication> apps;	//all the applications already loaded
	
	private ApplicationManager2(){
	}
	
	public void init(){
		String basedir = "E:/work/workspace/workspace/Application/patch/TestApplication/";
		String[] packagearr = {"com.sin.app"};
		
		factory = CLFactory.getInstance();
		factory.loadClass(basedir, packagearr);
		
		configManager = new AppConfigManager();
		apps = new HashMap<String, IApplication>();
		
		initAppConfigs();
		
		String basePath = getBasePath();
		//URL basePath = this.getClass().getClassLoader().getResource("");
		
		loadAllApplications(basePath);
		
		initMonitorForChange(basePath);
	}
	
	/**
	 * Load all the app configs to memory
	 */
	public void initAppConfigs(){
		
		try {
			String basePath = getBasePath();
			String configPath = Util.joinPath(basePath,"config",GlobalSetting.APP_CONFIG_NAME);
			File file = new File(configPath);
			if(file.exists()){
				URI path = file.toURI();
				configManager.loadAllApplicationConfigs(path);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} 
	}
	
	/**
	 * Load all the apps specified in the applications.xml file
	 */
	public void loadAllApplications(String basePath){
		
		for(AppConfig config : this.configManager.getConfigs()){
			this.createApplication(basePath, config);
		}
	}
	
	/**
	 * Initial the monitor to listen the change event of the application jar files
	 * Here I used the apache common vfs component to monitor the change event
	 * If you want to learn more about vfs, you can visit:
	 * <url>http://commons.apache.org/proper/commons-vfs/</url>
	 * @param basePath
	 */
	public void initMonitorForChange(String basePath){
		try {
			this.fileManager = VFS.getManager();
			
			File file = new File(Util.joinPath(basePath,GlobalSetting.JAR_FOLDER));
			FileObject monitoredDir = this.fileManager.resolveFile(file.getAbsolutePath());
			FileListener fileMonitorListener = new JarFileChangeListener();
			this.fileMonitor = new DefaultFileMonitor(fileMonitorListener);
			this.fileMonitor.setRecursive(true);
			this.fileMonitor.addFile(monitoredDir);
			this.fileMonitor.start();
			System.out.println("Now to listen " + monitoredDir.getName().getPath());
			
		} catch (FileSystemException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Use a special class loader to load the application specified in the config
	 * @param config
	 * @return
	 */
	public void createApplication(String basePath, AppConfig config){
		try {
			Class<?> appClass = factory.getClass(config.getFile()); //loader.loadClass(config.getFile());
			
//			Method method = appClass.getMethod("init", new Class[]{});
//			method.invoke(appClass.newInstance(), new Object[]{});
			
			IApplication app = (IApplication)appClass.newInstance();
			System.out.println(appClass.getClassLoader()+","+IApplication.class.getClassLoader());
			
			app.init();
			
			this.apps.put(config.getName(), app);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Reload the specified application, first to undeploy it , and then create it.
	 * @param name
	 */
	public void reloadApplication(String name){
		IApplication oldApp = this.apps.remove(name);
		
		if(oldApp == null){
			return;
		}
		
		oldApp.destory();	//call the destroy method in the user's application
		
		AppConfig config = this.configManager.getConfig(name);
		if(config == null){
			return;
		}
		
		createApplication(getBasePath(), config);
	}
	
	public static ApplicationManager2 getInstance(){
		if(instance == null){
			instance = new ApplicationManager2();
		}
		return instance;
	}
	
	/**
	 * Get the application by name
	 * @param name
	 * @return
	 */
	public IApplication getApplication(String name){
		if(this.apps.containsKey(name)){
			return this.apps.get(name);
		}
		return null;
	}
	
	public String getBasePath(){
		String rootPath= System.getProperty("user.dir"); //当前项目根目录
		rootPath = rootPath.replace("\\", "/");
//		String basePath = this.getClass().getClassLoader().getResource("").getPath();
//		System.out.println(basePath);
		
		return rootPath;
	}
}
