package me.naptie.bililottery;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import me.naptie.bililottery.utils.HttpManager;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Main {

	private static final Map<Integer, Integer> uidMap = new HashMap<>();
	private static final List<Integer> unsubscribeList = new ArrayList<>();
	private static final List<Integer> warned = new ArrayList<>();
	private static String cookie = "#";
	private static String dynId;
	private static JSONObject result;

	public static void main(String[] args) throws IOException, InterruptedException {
		draw(getParticipants(args));
	}

	@SuppressWarnings("unused")
	private static void draw(File file) throws IOException, InterruptedException {
		draw(Objects.requireNonNull(JSONArray.parseArray(readJsonFile(file))));
	}

	private static int random(int[] seeds, int range) {
		long result = seeds[0];
		for (int i = 0; i < Math.ceil(Math.random() * seeds[1]) % range; i++) {
			result += Math.ceil(Math.random() * ((i + seeds[2]) % range));
		}
		result %= range;
		return (int) result;
	}

	private static void draw(JSONArray array) throws IOException, InterruptedException {
		System.out.println("名单获取完成。三秒后显示抽奖结果。");
		TimeUnit.SECONDS.sleep(3);
		JSONObject summary = new JSONObject();
		List<Integer> prized = new ArrayList<>();
		JSONObject firstPrize = new JSONObject();
		int firstPrizeIdx = random(new int[]{array.size() / 37, 20191002, 233}, array.size());
		prized.add(firstPrizeIdx);
		firstPrize.put("index", firstPrizeIdx);
		firstPrize.put("user", array.getJSONObject(firstPrizeIdx));
		summary.put("first_prize", firstPrize);
		System.out.println("\n=================================================\n抽奖结果：\n\n一等奖 · 季度大会员 ¥68.00：\n· · " + array.getJSONObject(firstPrizeIdx).getString("name") + "（UID：" + array.getJSONObject(firstPrizeIdx).getIntValue("uid") + "）\n\n二等奖 · 月度大会员 ¥25.00：");
		JSONArray secondPrizes = new JSONArray();
		JSONObject secondPrize = new JSONObject();
		int secondPrizeIdx;
		for (int i = 0; i < 3; i++) {
			do {
				secondPrizeIdx = random(new int[]{array.size() / 23, 114514, 1919810}, array.size());
			} while (prized.contains(secondPrizeIdx));
			prized.add(secondPrizeIdx);
			secondPrize.put("index", secondPrizeIdx);
			secondPrize.put("user", array.getJSONObject(secondPrizeIdx));
			secondPrizes.add(secondPrize);
			System.out.println("· · " + array.getJSONObject(secondPrizeIdx).getString("name") + "（UID：" + array.getJSONObject(secondPrizeIdx).getIntValue("uid") + "）");
			secondPrize = new JSONObject();
		}
		summary.put("second_prizes", secondPrizes);
		JSONArray thirdPrizes = new JSONArray();
		JSONObject thirdPrize = new JSONObject();
		int thirdPrizeIdx;
		System.out.println("\n三等奖\n· bilibili冬头像挂件（30天） ¥5.00：");
		for (int i = 0; i < 10; i++) {
			if (i == 5) {
				System.out.println("· 鸭鸭的冬天头像挂件（30天） ¥5.00：");
			}
			do {
				thirdPrizeIdx = random(new int[]{array.size() / (int) (System.currentTimeMillis() % 17 + 1), (int) System.currentTimeMillis() % 114514, (int) (Math.random() * array.size())}, array.size());
			} while (prized.contains(thirdPrizeIdx));
			prized.add(thirdPrizeIdx);
			thirdPrize.put("index", thirdPrizeIdx);
			thirdPrize.put("user", array.getJSONObject(thirdPrizeIdx));
			thirdPrizes.add(thirdPrize);
			System.out.println("· · " + array.getJSONObject(thirdPrizeIdx).getString("name") + "（UID：" + array.getJSONObject(thirdPrizeIdx).getIntValue("uid") + "）");
			thirdPrize = new JSONObject();
		}
		summary.put("third_prizes", thirdPrizes);
		summary.put("participants", array);
		File outFile = new File("prize" + System.currentTimeMillis() / 1000 + ".json");
		BufferedWriter writer = new BufferedWriter(new FileWriter(outFile));
		writer.write(JSON.toJSONString(summary, SerializerFeature.DisableCircularReferenceDetect));
		writer.close();
		System.out.println("\n=================================================");
	}

	private static JSONArray getParticipants(String[] args) throws IOException {
		cookie = "SESSDATA=" + args[0];
		dynId = args[1];
		String CSRF = args[2];
		String voteId = args[3];
		JSONObject login = HttpManager.readJsonFromUrl("https://api.bilibili.com/x/web-interface/nav", cookie, false, false);
		if (login.getIntValue("code") == 0) {
			if (login.getJSONObject("data").getBoolean("isLogin")) {
				System.out.println("登录成功" + ("\nID：" + login.getJSONObject("data").getString("uname") + "\nUID：" + login.getJSONObject("data").getIntValue("mid")));
			}
		}
		result = HttpManager.readJsonFromUrl("https://api.vc.bilibili.com/dynamic_repost/v1/dynamic_repost/repost_detail?dynamic_id=" + dynId, cookie, false, false);
		JSONArray array = new JSONArray();
		int idx = 0, counter = 0;
		do {
			for (int i = 0; i < result.getJSONObject("data").getJSONArray("items").size(); i++, counter++) {
				JSONObject repost = result.getJSONObject("data").getJSONArray("items").getJSONObject(i);
				int uid = repost.getJSONObject("desc").getJSONObject("user_profile").getJSONObject("info").getIntValue("uid");
				if (uidMap.containsKey(uid)) {
					continue;
				} else {
					uidMap.put(uid, idx++);
				}
				JSONObject obj = new JSONObject();
				obj.put("uid", uid);
				obj.put("name", repost.getJSONObject("desc").getJSONObject("user_profile").getJSONObject("info").getString("uname"));
				obj.put("avatar", repost.getJSONObject("desc").getJSONObject("user_profile").getJSONObject("info").getString("face"));
				array.add(obj);
			}
		} while (hasMore());
		System.out.println("遍历 " + counter + " 条转发之后，最终发现 " + array.size() + " 名粉丝转发了动态");
		counter = 0;
		for (String uids : toString(uidMap.keySet())) {
			counter += 10;
			if (counter > uidMap.size()) {
				counter = uidMap.size();
			}
			System.out.println("正在检查投票结果（" + counter + " / " + uidMap.size() + "）");
			for (String uid : uids.split(",")) {
				if (!unsubscribeList.contains(Integer.parseInt(uid))) {
					unsubscribeList.add(Integer.parseInt(uid));
				}
			}
			String url = "https://api.bilibili.com/x/relation/batch/modify?fids=" + uids + "&act=1&re_src=11&csrf=" + CSRF;
			JSONObject subRes = HttpManager.readJsonFromUrl(url, cookie, true, false);
			if (subRes.getIntValue("code") != 0) {
				System.out.println("无法关注 " + uids.replaceAll(",", "、") + "。代码：" + subRes.getIntValue("code") + "；原因：" + subRes.getString("message"));
			}
			if (!subRes.getJSONObject("data").getJSONArray("failed_fids").isEmpty()) {
				JSONArray failed = subRes.getJSONObject("data").getJSONArray("failed_fids");
				for (int i = 0; i < failed.size(); i++) {
					System.out.println("无法关注 " + failed.getIntValue(i));
					unsubscribeList.remove(failed.getIntValue(i));
				}
			}
			for (Integer uid : unsubscribeList) {
				System.out.println("成功关注 " + array.getJSONObject(uidMap.get(uid)).getString("name"));
			}
			JSONObject voteRes = HttpManager.readJsonFromUrl("https://api.vc.bilibili.com/vote_svr/v1/vote_svr/followee_votes?vote_id=" + voteId, cookie, false, false);
			JSONArray votes = voteRes.getJSONObject("data").getJSONArray("votes");
			for (int i = 0; i < votes.size(); i++) {
				JSONObject vote = votes.getJSONObject(i);
				int uid = vote.getIntValue("uid");
				if (!uidMap.containsKey(uid)) {
					if (!warned.contains(uid)) {
						System.out.println(vote.getString("name") + " 参加了投票但是没有转发动态");
						warned.add(uid);
					}
					continue;
				}
				if (array.getJSONObject(uidMap.get(uid)).containsKey("vote") && array.getJSONObject(uidMap.get(uid)).containsKey("ctime")) {
					continue;
				}
				JSONObject obj = array.getJSONObject(uidMap.get(uid));
				array.remove(uidMap.get(uid));
				obj.put("vote", vote.get("votes"));
				obj.put("ctime", vote.getIntValue("ctime"));
				array.add(obj);
			}
			for (int uid : unsubscribeList) {
				JSONObject unsubscribeResult = HttpManager.readJsonFromUrl("https://api.bilibili.com/x/relation/modify?fid=" + uid + "&act=2&re_src=11&csrf=" + CSRF, cookie, true, false);
				if (unsubscribeResult.getIntValue("code") == 0) {
					System.out.println("成功取关 " + array.getJSONObject(uidMap.get(uid)).getString("name"));
				}
			}
			unsubscribeList.clear();
		}
		for (int i = 0; i < array.size(); i++) {
			if (!array.getJSONObject(i).containsKey("vote") || !array.getJSONObject(i).containsKey("ctime")) {
				System.out.println("由于未参与投票，已移除 " + array.getJSONObject(i).getString("name"));
				//noinspection SuspiciousListRemoveInLoop
				array.remove(i);
			}
		}
		File file = new File("list" + System.currentTimeMillis() / 1000 + ".json");
		BufferedWriter writer = new BufferedWriter(new FileWriter(file));
		writer.write(JSON.toJSONString(array, SerializerFeature.DisableCircularReferenceDetect));
		writer.close();
		return array;
	}

	private static boolean hasMore() throws IOException {
		if (result.getJSONObject("data").getIntValue("has_more") == 1) {
			result = HttpManager.readJsonFromUrl("https://api.vc.bilibili.com/dynamic_repost/v1/dynamic_repost/repost_detail?dynamic_id=" + dynId + "&offset=" + result.getJSONObject("data").getString("offset"), cookie, false, false);
			return true;
		} else {
			return false;
		}
	}

	private static List<String> toString(Set<Integer> set) {
		List<String> result = new ArrayList<>();
		int idx = 0, count = 0;
		for (int i : set) {
			if (count == 0) {
				result.add(i + "");
			} else {
				result.set(idx, result.get(idx) + "," + i);
			}
			count++;
			if (count == 10) {
				idx++;
				count = 0;
			}
		}
		return result;
	}

	public static String readJsonFile(File jsonFile) {
		String jsonStr;
		try {
			FileReader fileReader = new FileReader(jsonFile);
			Reader reader = new InputStreamReader(new FileInputStream(jsonFile), StandardCharsets.UTF_8);
			int ch;
			StringBuilder builder = new StringBuilder();
			while ((ch = reader.read()) != -1) {
				builder.append((char) ch);
			}
			fileReader.close();
			reader.close();
			jsonStr = builder.toString();
			return jsonStr;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

}
