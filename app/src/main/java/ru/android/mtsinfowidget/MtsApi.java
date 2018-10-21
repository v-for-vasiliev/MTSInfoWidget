package ru.android.mtsinfowidget;

import android.content.Context;
import android.util.Log;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.SingleClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import ru.android.mtsinfowidget.objects.H2OProfile;
import ru.android.mtsinfowidget.objects.LkHeader;

/**
 * Created by Vasiliev on 30.09.2015.
 */
public class MtsApi {
    private static final String TAG = MtsApi.class.getSimpleName();

    private static final boolean DBG = true;

    private static List<String> sPermittedCookies = new ArrayList<String>(7);

    static {
        sPermittedCookies.add("ARRAffinity");
        sPermittedCookies.add("IHLink");
        sPermittedCookies.add("MTSWebSSO");
        sPermittedCookies.add("amlbcookie");
        sPermittedCookies.add("auth-back-url");
        sPermittedCookies.add("login.mts.ru.logout");
        sPermittedCookies.add("_ga");
    }

    private List<Cookie> mCookies = new ArrayList<Cookie>(0);

    // Raw data
    private String mH2OProfileRawData = null;
    private String mLkHeaderRawData = null;

    // Profile objects and cache
    private LkHeader mLkHeader = null;
    private LkHeader mCachedLkHeader = null;
    private H2OProfile mH2OProfile = null;
    private H2OProfile mCachedH2OProfile = null;

    private Date mLastUpdateTime = null;

    private MtsApi() {
        Log.d(TAG, "MtsApi()");
    }

    public static MtsApi getInstance() {
        return SingletonHolder.HOLDER_INSTANCE;
    }

    public void updateProfileInfoAsync(final Context context) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (doWork()) {
                    BroadcastManager.broadcastMtsInfoUpdated(context);
                }
            }
        }).start();
    }

    public boolean updateProfileInfo() {
        return doWork();
    }

    private boolean doWork() {
        // Clear object created before
        clearObjects();

        // Do work
        if (!auth()) {
            Log.d(TAG, "Error authorizing in LK");
            return false;
        }
        delay(1000);
        if (!checkAuth()) {
            Log.d(TAG, "Error checking authorization");
            return false;
        }
        delay(1000);
        if (!requestGa()) {
            Log.d(TAG, "Error getting GA");
            return false;
        }
        if ((mH2OProfileRawData = requestH2OProfile()) == null) {
            Log.d(TAG, "Error getting H2O profile data");
            return false;
        }
        if ((mLkHeaderRawData = requestLkHeader()) == null) {
            Log.d(TAG, "Error getting header data");
            return false;
        }

        // Create objects
        LkHeader header = LkHeader.create(mLkHeaderRawData);
        if (header != null) {
            mCachedLkHeader = mLkHeader;
            mLkHeader = header;
        } else {
            Log.d(TAG, "Error creating LK Header object");
            clearObjects();
            return false;
        }
        Log.d(TAG, mLkHeader.toString());

        H2OProfile profile = H2OProfile.create(mH2OProfileRawData);
        if (profile != null) {
            mCachedH2OProfile = mH2OProfile;
            mH2OProfile = profile;
        } else {
            Log.d(TAG, "Error creating H2O Profile object");
            clearObjects();
            return false;
        }
        Log.d(TAG, mH2OProfile.toString());

        mLastUpdateTime = new Date();
        return true;
    }

    private void clearObjects() {
        mLkHeaderRawData = null;
        mLkHeader = null;
        mH2OProfileRawData = null;
        mH2OProfile = null;
    }

    private boolean auth() {
        boolean isAuthorized = false;
        try {
            // SSL and default schemes
            SchemeRegistry schemeRegistry = new SchemeRegistry();
            schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
            schemeRegistry.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));

            // Handle redirects automatically
            HttpParams params = new BasicHttpParams();
            HttpClientParams.setRedirecting(params, true);

            SingleClientConnManager connManager = new SingleClientConnManager(params, schemeRegistry);
            HttpClient httpClient = new DefaultHttpClient(connManager, params);

            String authUri = String.format(Locale.getDefault(), "%s?service=%s&goto=%s",
                    Constants.LOGIN_URI, Constants.LOGIN_URI_SERVICE_PARAM, Constants.LOGIN_URI_GOTO_PARAM);

            // Create HEAD request to suppress data usage
            HttpHead authRequest = new HttpHead(new URI(authUri));
            authRequest.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
            authRequest.setHeader("Accept-Encoding", "gzip, deflate, sdch");
            authRequest.setHeader("Accept-Language", "ru-RU,ru;q=0.8,en-US;q=0.6,en;q=0.4");
            authRequest.setHeader("Connection", "keep-alive");
            authRequest.setHeader("User-Agent", "Mozilla/5.0 (Linux; Android 6.0.1; Nexus 5 Build/MMB29K) " +
                    "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/47.0.2526.83 Mobile Safari/537.36");

            // Clear cookies before auth request
            clearCookieStore();

            // Fill request with cookies
            HttpContext httpContext = new BasicHttpContext();
            CookieStore cookieStore = getCookieStore();
            httpContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);

            // Execute
            HttpResponse response = httpClient.execute(authRequest, httpContext);
            if (DBG) {
                Log.d(TAG, "auth() response code: " + response.getStatusLine().getStatusCode());
            }
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                updateCookies(cookieStore.getCookies());
                isAuthorized = true;
                if (DBG) {
                    Log.d(TAG, "auth() endpoint URL: " + getEndpointUrl(httpContext));
                    printCookies();
                }
            }
        } catch (URISyntaxException e) {
            Log.e(TAG, "", e);
        } catch (ClientProtocolException e) {
            Log.e(TAG, "", e);
        } catch (IOException e) {
            Log.e(TAG, "", e);
        }
        return isAuthorized;
    }

    private boolean checkAuth() {
        boolean isAuthChecked = false;
        try {
            HttpClient httpClient = new DefaultHttpClient();

            // Create request
            String checkUri = String.format(Locale.getDefault(), "%s?_=%s",
                    Constants.CHECK_AUTH_URI, URLEncoder.encode(Long.toString(System.currentTimeMillis()), "UTF-8"));
            HttpGet authCheckRequest = new HttpGet(new URI(checkUri));
            authCheckRequest.setHeader("Accept", "*/*");
            authCheckRequest.setHeader("Accept-Encoding", "gzip, deflate");
            authCheckRequest.setHeader("Accept-Language", "ru-RU,ru;q=0.8,en-US;q=0.6,en;q=0.4");
            authCheckRequest.setHeader("Connection", "keep-alive");
            authCheckRequest.setHeader("User-Agent", "Mozilla/5.0 (Linux; Android 6.0.1; Nexus 5 Build/MMB29K) " +
                    "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/47.0.2526.83 Mobile Safari/537.36");
            authCheckRequest.setHeader("X-Requested-With", "XMLHttpRequest");

            // Fill request with cookies
            HttpContext httpContext = new BasicHttpContext();
            CookieStore cookieStore = getCookieStore();
            httpContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);

            // Execute
            HttpResponse response = httpClient.execute(authCheckRequest, httpContext);
            if (DBG) {
                Log.d(TAG, "checkAuth() response code: " + response.getStatusLine().getStatusCode());
            }
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                updateCookies(cookieStore.getCookies());
                isAuthChecked = true;
                if (DBG) {
                    Log.d(TAG, "checkAuth() endpoint URL: " + getEndpointUrl(httpContext));
                    printCookies();
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "", e);
        } catch (URISyntaxException e) {
            Log.e(TAG, "", e);
        }
        return isAuthChecked;
    }

    private boolean requestGa() {
        boolean isGaObtained = false;
        try {
            HttpClient httpClient = new DefaultHttpClient();

            // Create request
            HttpPost gaRequest = new HttpPost(new URI(Constants.GA_URI));
            gaRequest.setHeader("Accept", "*/*");
            gaRequest.setHeader("Accept-Encoding", "gzip, deflate");
            gaRequest.setHeader("Accept-Language", "ru-RU,ru;q=0.8,en-US;q=0.6,en;q=0.4");
            gaRequest.setHeader("Connection", "keep-alive");
            gaRequest.setHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            gaRequest.setHeader("User-Agent", "Mozilla/5.0 (Linux; Android 6.0.1; Nexus 5 Build/MMB29K) " +
                    "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/47.0.2526.83 Mobile Safari/537.36");

            // Fill POST form
            List<NameValuePair> formData = new ArrayList<NameValuePair>(0);
            formData.add(new BasicNameValuePair("dl", URLEncoder.encode("http://internet.mts.ru/", "UTF-8")));
            formData.add(new BasicNameValuePair("dp", URLEncoder.encode("/", "UTF-8")));
            formData.add(new BasicNameValuePair("dh", URLEncoder.encode("internet.mts.ru", "UTF-8")));
            formData.add(new BasicNameValuePair("dt", URLEncoder.encode("Вся информация о вашем интернете", "UTF-8")));
            formData.add(new BasicNameValuePair("sr", URLEncoder.encode("360x640", "UTF-8")));
            formData.add(new BasicNameValuePair("vp", URLEncoder.encode("360x640", "UTF-8")));
            formData.add(new BasicNameValuePair("sd", URLEncoder.encode("32-bits", "UTF-8")));
            formData.add(new BasicNameValuePair("ul", URLEncoder.encode("ru", "UTF-8")));
            formData.add(new BasicNameValuePair("je", URLEncoder.encode("false", "UTF-8")));
            formData.add(new BasicNameValuePair("fl", URLEncoder.encode("0 0", "UTF-8")));
            formData.add(new BasicNameValuePair("dr", URLEncoder.encode("http://internet.mts.ru/waitauth?goto=http://internet.mts.ru/", "UTF-8")));
            formData.add(new BasicNameValuePair("t", URLEncoder.encode("pageview", "UTF-8")));
            gaRequest.setEntity(new UrlEncodedFormEntity(formData));

            // Fill request with cookies
            HttpContext httpContext = new BasicHttpContext();
            CookieStore cookieStore = getCookieStore();
            httpContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);

            // Execute
            HttpResponse response = httpClient.execute(gaRequest, httpContext);
            if (DBG) {
                Log.d(TAG, "requestGa() response code: " + response.getStatusLine().getStatusCode());
            }
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                updateCookies(cookieStore.getCookies());
                isGaObtained = true;
                if (DBG) {
                    Log.d(TAG, "requestGa() endpoint URL: " + getEndpointUrl(httpContext));
                    printCookies();
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "", e);
        } catch (URISyntaxException e) {
            Log.e(TAG, "", e);
        }
        return isGaObtained;
    }

    private String requestH2OProfile() {
        String rawH2OProfileData = null;

        try {
            HttpClient httpClient = new DefaultHttpClient();

            // Create request
            HttpGet profileRequest = new HttpGet(new URI(Constants.H2O_PROFILE_URI));
            profileRequest.setHeader("Accept", "*/*");
            profileRequest.setHeader("Accept-Encoding", "gzip, deflate, sdch");
            profileRequest.setHeader("Accept-Language", "ru-RU,ru;q=0.8,en-US;q=0.6,en;q=0.4");
            profileRequest.setHeader("Connection", "keep-alive");
            profileRequest.setHeader("User-Agent", "Mozilla/5.0 (Linux; Android 6.0.1; Nexus 5 Build/MMB29K) " +
                    "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/47.0.2526.83 Mobile Safari/537.36");

            // Fill request with cookies
            HttpContext httpContext = new BasicHttpContext();
            CookieStore cookieStore = getCookieStore();
            httpContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);

            // Execute
            HttpResponse response = httpClient.execute(profileRequest, httpContext);
            if (DBG) {
                Log.d(TAG, "requestH2OProfile() response code: " + response.getStatusLine().getStatusCode());
            }
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                HttpEntity responseEntity = response.getEntity();
                if (responseEntity != null) {
                    rawH2OProfileData = EntityUtils.toString(responseEntity, HTTP.UTF_8);
                } else {
                    Log.d(TAG, "Null H2O Profile data response");
                }
            }
        } catch (URISyntaxException e) {
            Log.e(TAG, "", e);
        } catch (IOException e) {
            Log.e(TAG, "", e);
        } catch (ParseException e) {
            Log.e(TAG, "", e);
        }
        return rawH2OProfileData;
    }

    private String requestLkHeader() {
        String rawHeaderData = null;

        try {
            HttpClient httpClient = new DefaultHttpClient();

            // Create request
            HttpGet profileRequest = new HttpGet(new URI(Constants.HEADER_URI));
            profileRequest.setHeader("Accept", "*/*");
            profileRequest.setHeader("Accept-Encoding", "gzip, deflate, sdch");
            profileRequest.setHeader("Accept-Language", "ru-RU,ru;q=0.8,en-US;q=0.6,en;q=0.4");
            profileRequest.setHeader("Connection", "keep-alive");
            profileRequest.setHeader("User-Agent", "Mozilla/5.0 (Linux; Android 6.0.1; Nexus 5 Build/MMB29K) " +
                    "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/47.0.2526.83 Mobile Safari/537.36");

            // Fill request with cookies
            HttpContext httpContext = new BasicHttpContext();
            CookieStore cookieStore = getCookieStore();
            httpContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);

            // Execute
            HttpResponse response = httpClient.execute(profileRequest, httpContext);
            if (DBG) {
                Log.d(TAG, "requestLkHeader() response code: " + response.getStatusLine().getStatusCode());
            }
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                HttpEntity responseEntity = response.getEntity();
                if (responseEntity != null) {
                    Header contentEnc = responseEntity.getContentEncoding();
                    if (contentEnc != null && ("gzip").equalsIgnoreCase(contentEnc.getValue())) {
                        rawHeaderData = IOTools.ungzipStringFromStream(responseEntity.getContent());
                    } else {
                        rawHeaderData = IOTools.readStringFromStream(responseEntity.getContent());
                    }
                } else {
                    Log.d(TAG, "Null header data response");
                }
            }
        } catch (URISyntaxException e) {
            Log.e(TAG, "", e);
        } catch (IOException e) {
            Log.e(TAG, "", e);
        } catch (ParseException e) {
            Log.e(TAG, "", e);
        }
        return rawHeaderData;
    }

    private void clearCookieStore() {
        mCookies.clear();
    }

    private CookieStore getCookieStore() {
        CookieStore store = new BasicCookieStore();
        for (Iterator<Cookie> it = mCookies.iterator(); it.hasNext(); ) {
            store.addCookie(it.next());
        }
        return store;
    }

    private void updateCookies(List<Cookie> cookies) {
        if (cookies == null || cookies.isEmpty()) {
            return;
        }
        mCookies.clear();
        for (Iterator<Cookie> it = cookies.iterator(); it.hasNext(); ) {
            Cookie cookie = it.next();
            if (sPermittedCookies.contains(cookie.getName())) {
                mCookies.add(cookie);
            }
        }
    }

    private void printCookies() {
        Log.d(TAG, "Cookies:");
        for (Iterator<Cookie> it = mCookies.iterator(); it.hasNext(); ) {
            Cookie cookie = it.next();
            Log.d(TAG, cookie.getName() + "=" + cookie.getValue());
        }
    }

    private String getEndpointUrl(HttpContext httpContext) {
        HttpUriRequest endpointReq = (HttpUriRequest) httpContext.getAttribute(ExecutionContext.HTTP_REQUEST);
        HttpHost currentHost = (HttpHost) httpContext.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
        return (endpointReq.getURI().isAbsolute()) ?
                endpointReq.getURI().toString() : (currentHost.toURI() + endpointReq.getURI());
    }

    public String getCurlRequest() {
        StringBuilder curlRequest = new StringBuilder();
        curlRequest.append("curl 'http://internet.mts.ru/sitesettings/H2OProfile' " +
                "-H 'Accept-Encoding: gzip, deflate, sdch' " +
                "-H 'Accept-Language: ru-RU,ru;q=0.8,en-US;q=0.6,en;q=0.4' " +
                "-H 'User-Agent: Mozilla/5.0 (Linux; Android 6.0.1; Nexus 5 Build/MMB29K) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/47.0.2526.83 Mobile Safari/537.36' " +
                "-H 'Accept: */*' " +
                "-H 'Referer: http://internet.mts.ru/' " +
                "-H 'Cookie:");
        for (Cookie cookie : mCookies) {
            curlRequest.append(String.format(Locale.getDefault(), " %s=%s;", cookie.getName(), cookie.getValue()));
        }
        curlRequest.append("' -H 'Connection: keep-alive' --compressed");
        return curlRequest.toString();
    }

    private void delay(int delayMs) {
        if (delayMs > 0) {
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException ignore) {
            }
        }
    }

    public LkHeader getLkHeader() {
        return mLkHeader;
    }

    public LkHeader getCachedLkHeader() {
        return mCachedLkHeader;
    }

    public H2OProfile getH2OProfile() {
        return mH2OProfile;
    }

    public H2OProfile getCachedH2OProfile() {
        return mCachedH2OProfile;
    }

    public Date getLastUpdateTime() {
        return mLastUpdateTime;
    }

    public static class SingletonHolder {
        public static final MtsApi HOLDER_INSTANCE = new MtsApi();
    }
}
