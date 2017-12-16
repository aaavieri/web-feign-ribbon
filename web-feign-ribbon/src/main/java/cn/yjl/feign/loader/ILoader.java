package cn.yjl.feign.loader;

import cn.yjl.feign.feign.ParentClient;

/**
 * 装载器接口
 * @author yjl
 *
 */
public interface ILoader {

	/**
	 * 载入
	 * @param clazz feignClient接口类
	 * @param client 实现实例
	 */
	void load(Class<? extends ParentClient> clazz, ParentClient client);
}
