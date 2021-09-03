package me.naptie.bililottery.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class HttpManager {

	public static HttpURLConnection readUrl(String url, String cookie, boolean post, boolean tv) throws IOException {
		return readUrl(url, cookie, null, post, tv);
	}

	public static HttpURLConnection readUrl(String url, String cookie, String userAgent, boolean post, boolean tv) throws IOException {
		long begin = System.currentTimeMillis();
//		if (userAgent == null) {
//			userAgent = tv ? UserAgentManager.getTVUserAgent() : UserAgentManager.getUserAgent();
//		}
		HttpURLConnection request = (HttpURLConnection) (new URL(url)).openConnection();
//		request.setRequestProperty("User-Agent", userAgent);
		String host = url.split("://")[1].split("/")[0];
		request.setRequestProperty("Host", host);
		request.setRequestProperty("Referer", "https://www.bilibili.com");
		if (!cookie.equals("#")) {
			request.setRequestProperty("Cookie", cookie);
		}
//		System.setProperty("http.agent", userAgent);
		request.setRequestProperty("Accept", "*/*");
		if (post) {
			request.setRequestProperty("Content-type", "application/x-www-form-urlencoded");
			request.setRequestMethod("POST");
		}
//		System.out.println("正在访问 " + url);
		while (System.currentTimeMillis() - begin < 800);
		request.connect();
		return request;
	}

	public static JSONObject readJsonFromUrl(String url, String cookie, boolean post, boolean tv) throws IOException {
		return JSON.parseObject(IOUtils.toString((InputStream) readUrl(url, cookie, post, tv).getContent(), StandardCharsets.UTF_8));
	}

	public static JSONObject readJsonFromUrl(String url, String cookie, String userAgent, boolean tv) throws IOException {
		return JSON.parseObject(IOUtils.toString((InputStream) readUrl(url, cookie, userAgent, false, tv).getContent(), StandardCharsets.UTF_8));
	}
}
