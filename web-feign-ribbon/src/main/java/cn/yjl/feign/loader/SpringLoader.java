package cn.yjl.feign.loader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.context.ContextLoader;

import cn.yjl.feign.feign.ParentClient;

/**
 * spring装载器
 * @author yjl
 *
 */
public class SpringLoader implements ILoader {

	private static final Logger log = LoggerFactory.getLogger(SpringLoader.class);

	/**
	 * 载入
	 * @param clazz feignClient接口类
	 * @param client 实现实例
	 */
	@Override
	public void load(Class<? extends ParentClient> clazz, ParentClient client) {
		// 获取上下文环境
		ConfigurableApplicationContext webApplicationContext = (ConfigurableApplicationContext)ContextLoader.getCurrentWebApplicationContext();
		if (webApplicationContext == null || webApplicationContext.getBeanFactory() == null) {
			log.warn("找不到spring所在webapp上下文");
			return;
		}
		// 把实现实例注入到spring上下文中
		webApplicationContext.getBeanFactory().registerResolvableDependency(clazz, client);
		webApplicationContext.getBeanFactory().registerSingleton(clazz.getSimpleName(), client);
	}

}
