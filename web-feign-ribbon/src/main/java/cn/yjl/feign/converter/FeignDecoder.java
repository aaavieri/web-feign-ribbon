package cn.yjl.feign.converter;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import feign.FeignException;
import feign.Response;
import feign.codec.DecodeException;
import feign.codec.Decoder;
import feign.codec.StringDecoder;
import feign.gson.GsonDecoder;

/**
 * 自定义解码器
 * @author yjl
 *
 */
public class FeignDecoder implements Decoder {

	/** 解码器LIST */
	private List<Decoder> decoderList;
	
	/**
	 * 构造函数初始化解码器，如果以后有新的需要解码的往这里面添加
	 */
	public FeignDecoder() {
		this.decoderList = new ArrayList<Decoder>();
		this.decoderList.add(new StringDecoder());
		this.decoderList.add(new GsonDecoder());
	}
	
	/**
	 * 解码
	 * @param response 响应数据
	 * @param type feignClient定义接口的返回数据类型
	 */
	@Override
	public Object decode(Response response, Type type) throws IOException,
			DecodeException, FeignException {

		Object result = null;
		for (Decoder decoder : this.decoderList) {
			try {
				result = decoder.decode(response, type);				
			} catch (Exception e) {
				// do nothing
			}
			if (result != null) {
				break;
			}
		}
		return result;
	}

}
