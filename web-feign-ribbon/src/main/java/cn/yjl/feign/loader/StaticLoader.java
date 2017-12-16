package cn.yjl.feign.loader;

import java.util.HashMap;
import java.util.Map;

import cn.yjl.feign.feign.ParentClient;

/**
 * 静态装载器
 * @author yjl
 *
 */
@SuppressWarnings("unchecked")
public class StaticLoader implements ILoader {

	/** 静态存储feignClient实例的map */
	private static Map<Class<? extends ParentClient>, ParentClient> clientMap = new HashMap<Class<? extends ParentClient>, ParentClient>();
	
	/**
	 * 根据feignClient的接口类，获取实例
	 * @param interfaceClazz feignClient的接口类
	 * @return 实现实例
	 */
	public static <T extends ParentClient> T getClient(Class<T> interfaceClazz) {
		if (!clientMap.containsKey(interfaceClazz)) {
			return null;
		} else {
			return (T) clientMap.get(interfaceClazz);
		}
	}

	/**
	 * 载入
	 * @param clazz feignClient接口类
	 * @param client 实现实例
	 */
	@Override
	public void load(Class<? extends ParentClient> clazz, ParentClient client) {
		clientMap.put(clazz, client);
	}

}
