package cn.yjl.feign.loader;

import java.util.ArrayList;
import java.util.List;

import cn.yjl.feign.util.ClassUtil;

/**
 * 装载器工厂
 * @author Administrator
 *
 */
public class LoaderFactory {

	/**
	 * 获取装载器List
	 * @return 装载器List
	 */
	public static List<ILoader> getLoaderList() {
		List<ILoader> loaderList = new ArrayList<ILoader>();
		// 如果有spring环境，则加入spring装载器
		if (ClassUtil.haveSpring()) {
			loaderList.add(new SpringLoader());
		}
		// 加入静态装载器
		loaderList.add(new StaticLoader());
		return loaderList;
	}
}
