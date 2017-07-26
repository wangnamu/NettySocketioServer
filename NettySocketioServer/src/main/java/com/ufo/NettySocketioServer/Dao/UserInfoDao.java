package com.ufo.NettySocketioServer.Dao;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;

import com.ufo.NettySocketioServer.Models.UserInfoBean;

public class UserInfoDao {

	private RedissonClient mRedisson;
	private String mUserID;

	public UserInfoDao(RedissonClient redisson, String userID) {
		mRedisson = redisson;
		mUserID = userID;
	}

	public List<UserInfoBean> getUserInfoBeanList() {
		RMap<String, Map<String, String>> currentUser = mRedisson.getMap(mUserID);
		List<UserInfoBean> list = new ArrayList<>();
		for (String deviceToken : currentUser.keySet()) {
			Map<String, String> map = currentUser.get(deviceToken);
			UserInfoBean userInfoBean = new UserInfoBean();
			userInfoBean.fromMap(map);
			list.add(userInfoBean);
		}
		return list;
	}

	public UserInfoBean getUserInfoBean(String deviceToken) {
		RMap<String, Map<String, String>> currentUser = mRedisson.getMap(mUserID);
		Map<String, String> userMap = currentUser.get(deviceToken);
		if (userMap.isEmpty()) {
			return null;
		}
		UserInfoBean userInfoBean = new UserInfoBean();
		return userInfoBean.fromMap(userMap);
	}

	
	
	public void save(UserInfoBean userInfoBean) {
		RMap<String, Map<String, String>> currentUser = mRedisson.getMap(mUserID);
		currentUser.put(userInfoBean.getDeviceToken(), userInfoBean.toMap());	
	}

	public void del(String deviceToken) {
		RMap<String, Map<String, String>> currentUser = mRedisson.getMap(mUserID);
		currentUser.remove(deviceToken);
	}

}
