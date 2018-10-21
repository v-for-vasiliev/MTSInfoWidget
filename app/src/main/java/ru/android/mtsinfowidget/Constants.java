package ru.android.mtsinfowidget;

/**
 * Created by Vasiliev on 06.10.2015.
 */
public class Constants {
    /**
     * MTS API
     */
    public static final String LOGIN_URI = "https://login.mts.ru/amserver/UI/Login";
    public static final String LOGIN_URI_SERVICE_PARAM = "one";
    public static final String LOGIN_URI_GOTO_PARAM = "http://internet.mts.ru";
    public static final String CHECK_AUTH_URI = "http://internet.mts.ru/WaitAuth/CheckAuth";
    public static final String GA_URI = "http://internet.mts.ru/gaproxy/ga";
    public static final String H2O_PROFILE_URI = "http://internet.mts.ru/sitesettings/H2OProfile";
    public static final String HEADER_URI = "http://login.mts.ru/profile/header?ref=http://internet.mts.ru/&scheme=http&style=2015";

    /**
     * Broadcasts
     */
    public static final String ACTION_MTS_INFO_UPDATED = "ru.android.mtsinfowidget.ACTION_MTS_INFO_UPDATED";

    public static final int DEFAULT_UPDATE_INTERVAL_MS = 300000;
}
