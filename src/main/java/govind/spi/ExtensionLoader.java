package govind.spi;

import govind.factory.ExtensionFactory;
import govind.utils.ClassHelper;
import govind.utils.Holder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

/**
 *
 *
 *
 * @see <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jar/jar.html#Service%20Provider">Service Provider in Java 5</a>
 * @see govind.spi.Adaptive
 * @see govind.spi.SPI
 * @see govind.spi.Activate
 */
public class ExtensionLoader<T> {
	private static final Logger logger = Logger.getLogger(ExtensionLoader.class.getName());
	private static final String SERVICES_DIRECTORY = "META-INF/services/";
	private static final String DUBBO_DIRECTORY = "META-INF/dubbo/";
	private static final ConcurrentMap<Class<?>, ExtensionLoader<?>> EXTENSION_LOADERS = new ConcurrentHashMap<>();
	private  final ConcurrentMap<String, Holder<Object>> cachedInstance = new ConcurrentHashMap<>();
	private  final Holder<Map<String, Class<?>>> cachedClasses = new Holder<>();
	private String cachedDefaultName = null;
	private static final ConcurrentMap<Class<?>, Object> EXTENSION_INSTANCES = new ConcurrentHashMap<>();
	
	public static <T> ExtensionLoader getExtensionLoader(Class<T> type) {
		if(type == null) {
			throw new IllegalArgumentException("Extension type == null");
		}

		if(!type.isInterface()) {
			throw new IllegalArgumentException("Extension type ("+ type +") is not an interface");
		}

		if(!withExtentionAnnotation(type)) {
			throw new IllegalArgumentException("Extension type ("+ type +")" +
					"is NOT annotated by @" + SPI.class.getName()+  ", so it can not be extended!");
		}

		ExtensionLoader<T> loader = (ExtensionLoader<T>) EXTENSION_LOADERS.get(type);
		if(loader == null) {
			EXTENSION_LOADERS.putIfAbsent(type, new ExtensionLoader<T>(type));
			loader = (ExtensionLoader<T>) EXTENSION_LOADERS.get(type);
		}
		return loader;
	}

	private static <T> boolean withExtentionAnnotation(Class<T> type) {
		return type.isAnnotationPresent(SPI.class);
	}


	//====  构造方法

	private final Class<?>  type;
//	private final ExtensionFactory objectFactory;

	public ExtensionLoader(Class<?> type) {
		this.type = type;
//		objectFactory =  (type == ExtensionFactory.class) ?
//				null
//				:
//				ExtensionLoader.getExtensionLoader(ExtensionFactory.class).getAdaptiveExtention();
	}

	//TODO
	public T getAdaptiveExtention() {
		return null;
	}


	/**
	 * 获取指定名称的扩展类实例，若不存在则抛出异常
	 * @param name
	 * @return
	 */
	public T getExtension(String name) {
		if(name.isEmpty()) {
			throw new IllegalArgumentException("Extension name == null");
		}

		if("true".equals(name)) {
			//TODO return getDefaultExtension();
			return null;
		}

		Holder<Object> holder = getorCreateHolder(name);
		Object instance = holder.get();
		if (instance == null) {
			synchronized (holder) {
				instance = holder.get();
				if (instance == null) {
					instance = createExtension(name);
					holder.set(instance);
				}
			}
		}
		return (T) instance;
	}

	private Holder<Object> getorCreateHolder(String name) {
		Holder<Object> cached = cachedInstance.get(name);
		if (cached == null) {
			cachedInstance.putIfAbsent(name, new Holder<>());
			cached = cachedInstance.get(name);
		}
		return cached;
	}

	private T createExtension(String name) {
		Class<?> clz = getExtensionClasses().get(name);
		if(clz == null) {
			throw new IllegalArgumentException("未找到"+ name+"对应的类");
		}

		try {
			T instance = (T) EXTENSION_INSTANCES.get(clz);
			if (instance == null) {
				EXTENSION_INSTANCES.putIfAbsent(clz, clz.newInstance());
				instance = (T) EXTENSION_INSTANCES.get(clz);
			}

			//TODO injectExtension

			return instance;
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}

		return null;
	}

	private Class<?> getExtensionClass(String name) {
		if (type == null) {
			throw new IllegalArgumentException("Extension type == null");
		}
		if (name == null) {
			throw new IllegalArgumentException("Extension name == null");
		}
		return getExtensionClasses().get(name);
	}
	private Map<String, Class<?>> getExtensionClasses() {
		Map<String, Class<?>> classes = cachedClasses.get();
		if(classes == null) {
			synchronized (cachedClasses) {
				classes =  cachedClasses.get();
				if(classes == null) {
					classes = loadExtensionClasses();
					cachedClasses.set(classes);
				}
			}
		}
		return classes;
	}

	private Map<String, Class<?>> loadExtensionClasses() {
		cacheDefaultExtensionName();
		Map<String, Class<?>> extensionClasses = new HashMap<>();
		loadDirectory(extensionClasses, DUBBO_DIRECTORY, type.getName());
		loadDirectory(extensionClasses, SERVICES_DIRECTORY, type.getName());
		return extensionClasses;
	}

	private void cacheDefaultExtensionName() {
		final SPI defaultAnnotation = type.getAnnotation(SPI.class);
		if(defaultAnnotation != null) {
			String value = defaultAnnotation.value();
			if ((value=value.trim()).length() > 0) {
				String[] names = value.split(",");
				if (names.length > 1) {
					throw new IllegalArgumentException("SPI注解中的名称有多个");
				}
				if (names.length == 1) {
					cachedDefaultName = names[0];
				}

			}
		}
	}

	private void loadDirectory(Map<String, Class<?>> extensionClasses, String dir, String type) {
		String fileName = dir + type;

		try {
			Enumeration<URL> urls;
			ClassLoader classLoader = ClassHelper.getClassLoader(ExtensionLoader.class);
			if (classLoader != null) {
				urls = classLoader.getResources(fileName);
			} else {
				urls = ClassLoader.getSystemResources(fileName);
			}

			if (urls != null) {
				while (urls.hasMoreElements()) {
					URL url = urls.nextElement();
					loadResource(extensionClasses, classLoader, url);
				}
			}
		} catch (Exception e) {
			logger.info("加载SPI文件时失败：" + e.getMessage());
		}
	}

	private void loadResource(Map<String, Class<?>> extensionClasses, ClassLoader classLoader, URL url) throws ClassNotFoundException {
		try {
			try(BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8))) {
				String line;
				while ((line = br.readLine()) != null) {
					final int commentIdx = line.indexOf("#");
					if (commentIdx >= 0) {
						line = line.substring(0, commentIdx);
					}
					line = line.trim();
					if(line.length() > 0) {
						try {
							String name = null;
							int idx = line.indexOf("=");
							if(idx > 0) {
								name = line.substring(0,  idx).trim();
								line = line.substring(idx+1).trim();
							}
							if(line.length() > 0) {
								loadClass(extensionClasses, url, Class.forName(line, true, Thread.currentThread().getContextClassLoader()), name);
							}
						} catch (ClassNotFoundException e) {
							logger.info("加载类"+ line +"失败：" + e.getMessage());
							throw e;
						} catch (NoSuchMethodException e) {
							e.printStackTrace();
						}
					}
				}
			}
		} catch (IOException e) {
			logger.info("读取SPI文件过程中失败：" + e.getMessage());
		}
	}

	private void loadClass(Map<String, Class<?>> extensionClasses, URL url, Class<?> clz, String name) throws NoSuchMethodException {
		if(!type.isAssignableFrom(clz)) {
			throw new IllegalStateException(clz.getName() + "实例不是" + type.getName() + "接口的子类");
		}

		if(clz.isAnnotationPresent(Adaptive.class)) {
			//cacheAdaptiveClass(clz);
		} else if(isWrapperClass(clz)) {
			//cacheWrapperClass(clz);
		} else{
			clz.getConstructor();
			if(name == null || name.isEmpty()) {
				name = findAnnotationName(clz);
				if(name.length() == 0) {
					logger.warning( "SPI文件中没有获取到"+ clz.getName() +"对应key");
					return;
				}
			}

			Class<?> aClass = extensionClasses.get(name);
			if(aClass == null) {
				extensionClasses.put(name, clz);
			} else if (aClass != clz){
				throw new IllegalStateException("Duplicate extension");
			}
		}
	}

	private String findAnnotationName(Class<?> clz) {
		SPI annotation = clz.getAnnotation(SPI.class);
		String name = null;
		if (annotation != null) {
			name = annotation.value();
		} else {
			//说明是Java SPI，直接将类名返回
			//fixme 为了避免同一个扩展在META-INF/dubbo/和META-INF/services下都存在
			//这里检查已存在的cachedClasses中是否有对应的class若有则直接返回，
			// 不重复创建
			name = clz.getName();
			logger.info("Java SPI，返回扩展类名为对应实例的标识：" + name);
		}
		return name;
	}

	private boolean isWrapperClass(Class<?> clz) {
		return false;
	}

	public Map<Object, Object> getSupportedExtensions() {
		return null;
	}
}
