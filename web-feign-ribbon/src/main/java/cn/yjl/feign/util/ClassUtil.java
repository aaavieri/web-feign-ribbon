package cn.yjl.feign.util;

import java.io.File;
import java.io.FileFilter;
import java.lang.annotation.Annotation;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 操作class的一个公共类
 * @author yjl
 *
 */
@SuppressWarnings("unchecked")
public class ClassUtil {

	private static final Logger log = LoggerFactory.getLogger(ClassUtil.class);

	/** 判断是否有spring环境的类名 */
	private static String SPRING_FEATURE_CLASS_NAME =  "org.springframework.web.context.ContextLoader";
	

	public static <T> List<Class<T>> getClasses(String packageName) {
		return getClasses(packageName, null);
	}

	public static <T> List<Class<T>> getClasses(String packageName, Class<T> parentClazz) {
		return getClasses(packageName, parentClazz, null);
	}
	
	public static <A extends Annotation, T> List<Class<T>> getClasses(String packageName, Class<T> parentClazz, Class<A> annotation) {
		return getClasses(packageName, parentClazz, annotation, null);
	}

	/**
	 * 获取指定包下面的类
	 * @param packageName 指定包名
	 * @param parentClazz 父类
	 * @param annotation
	 * @return
	 */
	public static <A extends Annotation, T> List<Class<T>> getClasses(String packageName, Class<T> parentClazz, Class<A> annotation, List<String> jarFiles) {
		List<Class<T>> classes = new ArrayList<Class<T>>();
		String packageDirName = packageName.replace('.', '/');
		// 定义一个枚举的集合 并进行循环来处理这个目录下的things
		try {
			Enumeration<URL> dirs = Thread.currentThread().getContextClassLoader().getResources(packageDirName);
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
					findAndAddClassesInPackageByFile(packageName, filePath,
							classes, parentClazz, annotation);
				} else if ("jar".equals(protocol)) {
					// 如果是jar包文件
					// 定义一个JarFile
					// 获取jar
					JarFile jar = ((JarURLConnection) url.openConnection())
							.getJarFile();
					// 如果指定了jar包，则判断是否是指定的jar包
					if (jarFiles != null && !jarFiles.contains(jar.getName())) {
						continue;
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
						if (name.startsWith(packageDirName)) {
							int idx = name.lastIndexOf('/');
							// 如果以"/"结尾 是一个包
							if (idx != -1) {
								// 获取包名 把"/"替换成"."
								packageName = name.substring(0, idx).replace(
										'/', '.');
							}
							// 如果可以迭代下去 并且是一个包
							if ((idx != -1)) {
								// 如果是一个.class文件 而且不是目录
								if (name.endsWith(".class")
										&& !entry.isDirectory()) {
									// 去掉后面的".class" 获取真正的类名
									String className = name.substring(
											packageName.length() + 1,
											name.length() - 6);
									try {
										// 添加到classes
										Class<?> clazz = Class.forName(packageName + '.' + className);
										if ((parentClazz == null || isParent(clazz, parentClazz)) 
												&& (annotation == null || clazz.getAnnotation(annotation) != null)) {
												classes.add((Class<T>) clazz);							
										}
									} catch (ClassNotFoundException e) {
										log.error(e.getMessage(), e);
									}
								}
							}
						}
					}
				}
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return classes;
	}
	
	/**
	 * 判断一个类是不是另外一个类的父类
	 * @param child 待判定子类
	 * @param judgeClass 待判定父类
	 * @return 结果
	 */
	public static boolean isParent(Class<?> child, Class<?> judgeClass) {
		return judgeClass.isAssignableFrom(child);
	}


	/**
	 * 以文件的形式来获取包下的所有Class
	 * 
	 * @param packageName
	 * @param packagePath
	 * @param recursive
	 * @param classes
	 */
	public static <A extends Annotation, T> void findAndAddClassesInPackageByFile(String packageName,
			String packagePath, List<Class<T>> classes, Class<T> parentClazz, Class<A> annotation) {
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
				return (file.isDirectory())
						|| (file.getName().endsWith(".class"));
			}
		});
		// 循环所有文件
		for (File file : dirfiles) {
			// 如果是目录 则继续扫描
			if (file.isDirectory()) {
				findAndAddClassesInPackageByFile(
						packageName + "." + file.getName(),
						file.getAbsolutePath(), classes, parentClazz, annotation);
			} else {
				// 如果是java类文件 去掉后面的.class 只留下类名
				String className = file.getName().substring(0,
						file.getName().length() - 6);
				try {
					// 添加到集合中去
					Class<?> clazz = Class.forName(packageName + '.' + className);
					if ((parentClazz == null || isParent(clazz, parentClazz))
							&& (annotation == null || clazz.getAnnotation(annotation) != null)) {
							classes.add((Class<T>) clazz);
					}
				} catch (ClassNotFoundException e) {
					log.error(e.getMessage(), e);
				}
			}
		}
	}

	/**
	 * 是否有spring环境
	 * @return 结果
	 */
	public static boolean haveSpring() {
		try {
			Class.forName(SPRING_FEATURE_CLASS_NAME);
			return true;
		} catch (Throwable e) {
			return false;
		}
	}
}
