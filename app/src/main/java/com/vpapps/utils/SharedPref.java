package com.vpapps.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.vpapps.item.ItemUser;

public class SharedPref {

    private Context context;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private static String TAG_UID = "uid" ,TAG_USERNAME = "name", TAG_EMAIL = "email", TAG_MOBILE = "mobile", TAG_REMEMBER = "rem",
            TAG_PASSWORD = "pass", SHARED_PREF_AUTOLOGIN = "autologin";

    public SharedPref(Context context) {
        this.context = context;
        sharedPreferences = context.getSharedPreferences("setting", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    public void setIsFirst(Boolean flag) {
        editor.putBoolean("firstopen", flag);
        editor.apply();
    }

    public Boolean getIsFirst() {
        return sharedPreferences.getBoolean("firstopen", true);
    }

    public void setLoginDetails(ItemUser itemUser, Boolean isRemember, String password) {
        editor.putBoolean(TAG_REMEMBER, isRemember);
        editor.putString(TAG_UID, Methods.encrypt(itemUser.getId()));
        editor.putString(TAG_USERNAME, Methods.encrypt(itemUser.getName()));
        editor.putString(TAG_MOBILE, Methods.encrypt(itemUser.getMobile()));
        editor.putString(TAG_EMAIL, Methods.encrypt(itemUser.getEmail()));
        editor.putBoolean(TAG_REMEMBER, isRemember);
        editor.putString(TAG_PASSWORD, Methods.encrypt(password));
        editor.apply();
    }

    public void setRemeber(Boolean isRemember) {
        editor.putBoolean(TAG_REMEMBER, isRemember);
        editor.putString(TAG_PASSWORD, "");
        editor.apply();
    }

    public void getUserDetails() {
        Constant.itemUser = new ItemUser(Methods.decrypt(sharedPreferences.getString(TAG_UID,"")), Methods.decrypt(sharedPreferences.getString(TAG_USERNAME,"")), Methods.decrypt(sharedPreferences.getString(TAG_EMAIL,"")), Methods.decrypt(sharedPreferences.getString(TAG_MOBILE,"")));
    }

    public String getEmail() {
        return Methods.decrypt(sharedPreferences.getString(TAG_EMAIL,""));
    }

    public String getPassword() {
        return Methods.decrypt(sharedPreferences.getString(TAG_PASSWORD,""));
    }

    public Boolean isRemember() {
        return sharedPreferences.getBoolean(TAG_REMEMBER, false);
    }

    public Boolean getIsNotification() {
        return sharedPreferences.getBoolean("noti", true);
    }

    public void setIsNotification(Boolean isNotification) {
        editor.putBoolean("noti", isNotification);
        editor.apply();
    }

    public Boolean getIsAutoLogin() {
        return sharedPreferences.getBoolean(SHARED_PREF_AUTOLOGIN, false);
    }

    public void setIsAutoLogin(Boolean isAutoLogin) {
        editor.putBoolean(SHARED_PREF_AUTOLOGIN, isAutoLogin);
        editor.apply();
    }

    public Boolean getIsRemember() {
        return sharedPreferences.getBoolean(TAG_REMEMBER, false);
    }
}
