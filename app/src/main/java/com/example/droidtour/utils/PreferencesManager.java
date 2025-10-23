package com.example.droidtour.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferencesManager {
    private static final String PREF_NAME = "DroidTourPreferences";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_PHONE = "user_phone";
    private static final String KEY_USER_TYPE = "user_type"; // GUIDE, CLIENT, ADMIN
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_GUIDE_APPROVED = "guide_approved";
    private static final String KEY_GUIDE_RATING = "guide_rating";
    private static final String KEY_NOTIFICATIONS_ENABLED = "notifications_enabled";

    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;

    public PreferencesManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    // ==================== USER DATA ====================
    
    public void saveUserData(String userId, String name, String email, String phone, String userType) {
        editor.putString(KEY_USER_ID, userId);
        editor.putString(KEY_USER_NAME, name);
        editor.putString(KEY_USER_EMAIL, email);
        editor.putString(KEY_USER_PHONE, phone);
        editor.putString(KEY_USER_TYPE, userType);
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.apply();
    }

    public String getUserId() {
        return prefs.getString(KEY_USER_ID, "");
    }

    public String getUserName() {
        return prefs.getString(KEY_USER_NAME, "Usuario");
    }

    public String getUserEmail() {
        return prefs.getString(KEY_USER_EMAIL, "");
    }

    public String getUserPhone() {
        return prefs.getString(KEY_USER_PHONE, "");
    }

    public String getUserType() {
        return prefs.getString(KEY_USER_TYPE, "");
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public void logout() {
        editor.clear();
        editor.apply();
    }

    // ==================== GUIDE DATA ====================
    
    public void setGuideApproved(boolean approved) {
        editor.putBoolean(KEY_GUIDE_APPROVED, approved);
        editor.apply();
    }

    public boolean isGuideApproved() {
        return prefs.getBoolean(KEY_GUIDE_APPROVED, false);
    }

    public void setGuideRating(float rating) {
        editor.putFloat(KEY_GUIDE_RATING, rating);
        editor.apply();
    }

    public float getGuideRating() {
        return prefs.getFloat(KEY_GUIDE_RATING, 0.0f);
    }

    // ==================== NOTIFICATIONS ====================
    
    public void setNotificationsEnabled(boolean enabled) {
        editor.putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled);
        editor.apply();
    }

    public boolean areNotificationsEnabled() {
        return prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true);
    }
}

