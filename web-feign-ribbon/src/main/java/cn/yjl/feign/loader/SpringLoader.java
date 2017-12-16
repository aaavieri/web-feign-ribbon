package cn.yjl.feign.loader;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.context.ContextLoader;

import cn.yjl.feign.feign.ParentClient;

/**
 * spring装载器
 * @author yjl
 *
 */
public class SpringLoader implements ILoader {

	/**
	 * 载入
	 * @param clazz feignClient接口类
	 * @param client 实现实例
	 */
	@Override
	public void load(Class<? extends ParentClient> clazz, ParentClient client) {
		// 获取上下文环境
		ConfigurableApplicationContext webApplicationContext = (ConfigurableApplicationContext)ContextLoader.getCurrentWebApplicationContext();
		// 把实现实例注入到spring上下文中
		webApplicationContext.getBeanFactory().registerResolvableDependency(clazz, client);
		webApplicationContext.getBeanFactory().registerSingleton(clazz.getSimpleName(), client);
	}

}
