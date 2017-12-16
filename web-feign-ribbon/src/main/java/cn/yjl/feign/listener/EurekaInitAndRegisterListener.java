package cn.yjl.feign.listener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.yjl.feign.annotaion.LinkService;
import cn.yjl.feign.converter.FeignDecoder;
import cn.yjl.feign.feign.ParentClient;
import cn.yjl.feign.loader.ILoader;
import cn.yjl.feign.loader.LoaderFactory;
import cn.yjl.feign.util.ClassUtil;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.MyDataCenterInstanceConfig;
import com.netflix.config.ConfigurationManager;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.discovery.DefaultEurekaClientConfig;
import com.netflix.discovery.DiscoveryManager;
import com.netflix.discovery.shared.Application;
import com.netflix.discovery.shared.LookupService;

import feign.Feign;
import feign.gson.GsonEncoder;
import feign.ribbon.LoadBalancingTarget;

/**
 * web启动时的context监听，用于连接eureka，以及自动注入feignClient。
 * @author yjl
 *
 */
@SuppressWarnings({ "deprecation" })
public class EurekaInitAndRegisterListener implements ServletContextListener {

	/** config.properties文件内容 */
	private static final DynamicPropertyFactory configInstance = DynamicPropertyFactory
			.getInstance();
	
	private static final Logger log = LoggerFactory.getLogger(EurekaInitAndRegisterListener.class);
	
	/**
	 * * Notification that the web application initialization * process is
	 * starting. * All ServletContextListeners are notified of context *
	 * initialization before any filter or servlet in the web * application is
	 * initialized.
	 *
	 * @param sce
	 */
	@Override
	public void contextInitialized(ServletContextEvent sce) {
		// 设置被读取配置文件名称 默认config.properties，如果有需要可以改成其它名字
		System.setProperty("archaius.configurationSource.defaultFileName",
				"config.properties");
		log.info("开始注册eureka");
		// 注册
		this.registerWithEureka();
		log.info("完成注册eureka");
		log.info("开始初始化feign");
		/** 初始化feign */
		this.initFeignClient();
		log.info("完成初始化feign");
	}

	/**
	 * 注册到eureka
	 */
	private void registerWithEureka() {
		// 加载本地配置文件，初始化并且注册到 Eureka Server
		DiscoveryManager.getInstance().initComponent(new MyDataCenterInstanceConfig() {
		    @Override
		    public String getHostName(boolean refresh) {
				return super.getIpAddress();
		    }
		}, new DefaultEurekaClientConfig());

		// 本台 Application Service 已启动，准备接收其它微服务请求
		ApplicationInfoManager.getInstance().setInstanceStatus(
				InstanceInfo.InstanceStatus.UP);
	}

	/**
	 * 初始化feignClient
	 */
	private void initFeignClient() {
		// 获取config.properties文件中配置的待加载feignClient包名
		String[] feignPakcages = configInstance
				.getStringProperty("feign.client.packages", "").get()
				.split(",");
		
		String[] feignJars = configInstance
				.getStringProperty("feign.client.jars", "").get()
				.split(",");
		List<String> feignJarList = Arrays.asList(feignJars);
		// 获取所有的feignClient加载器
		List<ILoader> loaderList = LoaderFactory.getLoaderList();
		// 所有注册到eureka的service的ID集合
		final List<String> serviceList = new ArrayList<String>();
		// 遍历所有的待加载feignClient包
		for (String feignPackage : feignPakcages) {
			log.info("开始载入feign包：" + feignPackage);
			// 获取该包下面，所有继承ParentClient并有LinkService注解的feignClient
			// 之所以不用FeignClient注解，是因这个注解在feign-cloud的包里，容易把spring-cloud的东西扯到一起
			List<Class<ParentClient>> clientList = ClassUtil.getClasses(feignPackage, ParentClient.class, LinkService.class, feignJarList);
			// 遍历包下面所有的feignClient
			for (Class<ParentClient> clazz : clientList) {
				// 获取linkService注解中的serviceId
				LinkService linkService = clazz.getAnnotation(LinkService.class);
				String serviceId = linkService.value();
				log.info(clazz.getSimpleName() + "对应服务ID：" + serviceId);
				// 添加serviceId
				serviceList.add(serviceId);
				log.info(clazz.getSimpleName() + "开始构建FeignClient");
				// 用feign来构建一个实现了feignClient接口的代理
				// 如果是json以外的数据，目前支持Sax、JAXB、JAX-RS类型，也可以自己实现
				// 参考feign在github上的feign项目：https://github.com/OpenFeign/feign
				ParentClient client = Feign.builder().
						encoder(new GsonEncoder()).
						decoder(new FeignDecoder()).
						target(LoadBalancingTarget.create(clazz, "http://" + serviceId));
				log.info(clazz.getSimpleName() + "完成构建FeignClient");
				log.info(clazz.getSimpleName() + "开始载入FeignClient");
				// 用载入器载入生成的代理类实例
				for (ILoader loader : loaderList) {
					loader.load(clazz, client);
				}
				log.info(clazz.getSimpleName() + "完成载入FeignClient");
			}
		}
		// 用计时器来5分钟检测一次注册到eureka上其它服务的信息
		Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			public void run() {
				EurekaInitAndRegisterListener.this.setServiceInfo(serviceList);
			}
		}, 0L, 300000L);
	}
	
	/**
	 * 设置service连接信息
	 * @param serviceList
	 */
	private void setServiceInfo(List<String> serviceList) {
		// 获取lookup服务
		LookupService<?> lookUpService = DiscoveryManager.getInstance().getLookupService();
		// 遍历所有的serviceId
		for (String serviceId : serviceList) {
			// 用lookup服务，从eureka中查找服务可用的点
			Application app = lookUpService.getApplication(serviceId);
			// 把服务可用点的信息，拼接成字符串
			StringBuilder sb = new StringBuilder();
			if (app != null) {
				for (InstanceInfo instance : app.getInstances()) {
					if (sb.length() > 0) {
						sb.append(",");
					}
					sb.append(instance.getIPAddr() + ":" + instance.getPort());
				}
			}
			log.info(serviceId + "可用地址：" + sb);
			// 
			ConfigurationManager.getConfigInstance().setProperty(serviceId + ".ribbon.listOfServers", sb.toString());
		}
	}

	/**
	 * * Notification that the servlet context is about to be shut down. * All
	 * servlets and filters have been destroy()ed before any *
	 * ServletContextListeners are notified of context * destruction.
	 *
	 * @param sce
	 */
	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		DiscoveryManager.getInstance().shutdownComponent();
	}
}