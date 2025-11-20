package com.example.droidtour.models;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Modelo de Sesión de Usuario para Firebase Firestore
 * Almacena información sobre las sesiones activas de los usuarios
 * para permitir persistencia entre dispositivos y gestión de sesiones
 */
public class UserSession {
    @DocumentId
    private String sessionId;
    
    private String userId;
    private String userEmail;
    private String userName;
    private String userType; // CLIENT, GUIDE, ADMIN, SUPERADMIN
    
    // Información del dispositivo
    private String deviceId;
    private String deviceModel;
    private String deviceOS;
    private String appVersion;
    
    // Tokens de sesión
    private String accessToken;
    private String refreshToken;
    
    // Estado de la sesión
    private Boolean isActive;
    private String lastActivity;
    private String ipAddress;
    
    @ServerTimestamp
    private Date loginTime;
    private Date lastAccessTime;
    private Date logoutTime;

    // Constructor vacío requerido por Firestore
    public UserSession() {}

    // Constructor básico
    public UserSession(String userId, String userEmail, String userName, String userType) {
        this.userId = userId;
        this.userEmail = userEmail;
        this.userName = userName;
        this.userType = userType;
        this.isActive = true;
    }
    
    // Constructor completo
    public UserSession(String userId, String userEmail, String userName, String userType,
                      String deviceId, String deviceModel, String deviceOS, String appVersion) {
        this.userId = userId;
        this.userEmail = userEmail;
        this.userName = userName;
        this.userType = userType;
        this.deviceId = deviceId;
        this.deviceModel = deviceModel;
        this.deviceOS = deviceOS;
        this.appVersion = appVersion;
        this.isActive = true;
    }

    // Convertir a Map para guardar en Firestore
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", userId);
        map.put("userEmail", userEmail);
        map.put("userName", userName);
        map.put("userType", userType);
        map.put("deviceId", deviceId);
        map.put("deviceModel", deviceModel);
        map.put("deviceOS", deviceOS);
        map.put("appVersion", appVersion);
        map.put("accessToken", accessToken);
        map.put("refreshToken", refreshToken);
        map.put("isActive", isActive);
        map.put("lastActivity", lastActivity);
        map.put("ipAddress", ipAddress);
        map.put("logoutTime", logoutTime);
        return map;
    }

    // Getters y Setters
    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceModel() {
        return deviceModel;
    }

    public void setDeviceModel(String deviceModel) {
        this.deviceModel = deviceModel;
    }

    public String getDeviceOS() {
        return deviceOS;
    }

    public void setDeviceOS(String deviceOS) {
        this.deviceOS = deviceOS;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public Boolean getIsActive() {
        return isActive;
    }
    
    public Boolean isActive() {
        return isActive != null && isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }
    
    public void setActive(Boolean active) {
        isActive = active;
    }

    public String getLastActivity() {
        return lastActivity;
    }

    public void setLastActivity(String lastActivity) {
        this.lastActivity = lastActivity;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public Date getLoginTime() {
        return loginTime;
    }

    public void setLoginTime(Date loginTime) {
        this.loginTime = loginTime;
    }

    public Date getLastAccessTime() {
        return lastAccessTime;
    }

    public void setLastAccessTime(Date lastAccessTime) {
        this.lastAccessTime = lastAccessTime;
    }

    public Date getLogoutTime() {
        return logoutTime;
    }

    public void setLogoutTime(Date logoutTime) {
        this.logoutTime = logoutTime;
    }
}

