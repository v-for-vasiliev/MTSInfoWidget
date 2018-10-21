package ru.android.mtsinfowidget.objects;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Vasiliev on 07.10.2015.
 */
public class H2OProfile {
    private static final String TAG = H2OProfile.class.getSimpleName();

    private static final String H2O = "h2O";
    private static final String H2O_C = "c";
    private static final String H2O_C_MSISDN = "msisdn";
    private static final String H2O_C_REGIONID = "regionId";
    private static final String H2O_P = "p";
    private static final String H2O_P_ROAMING = "roaming";
    private static final String H2O_P_SERVICES = "services";
    private static final String H2O_P_SERVICES_NAME = "name";
    private static final String H2O_P_TRAFFICS = "traffics";
    private static final String H2O_P_TRAFFICS_MUIA = "muia";
    private static final String H2O_P_TRAFFICS_MUIA_SHARED_QUOTA = "sharedQuotaSize";
    private static final String H2O_P_TRAFFICS_VALUE = "value";

    private static final String PROFILE_PATTERN = "_profile: (.*?)\\}\\}\\},";

    private JSONObject mData = null;

    private Long mNumber = null;
    private String mRegionId = null;
    private String mRoaming = null;
    private String mServiceName = null;
    private Long mSharedQuotaSize = 0L;
    private Long mConsumedQuotaSize = 0L;

    private H2OProfile(JSONObject data) {
        mData = data;

        JSONObject H2O = null;
        String H2OData = JSONHelper.extractString(mData, H2OProfile.H2O);
        if (H2OData == null) {
            return;
        }

        try {
            H2O = new JSONObject(H2OData);
        } catch (JSONException e) {
            Log.e(TAG, "", e);
        }

        if (H2O != null) {
            JSONObject H2O_C = JSONHelper.extractObject(H2O, H2OProfile.H2O_C);
            if (H2O_C != null) {
                mNumber = JSONHelper.extractLong(H2O_C, H2O_C_MSISDN);
                mRegionId = JSONHelper.extractString(H2O_C, H2O_C_REGIONID);
            }

            JSONObject H2O_P = JSONHelper.extractObject(H2O, H2OProfile.H2O_P);
            if (H2O_P != null) {
                mRoaming = JSONHelper.extractString(H2O_P, H2O_P_ROAMING);

                JSONArray services = JSONHelper.extractArray(H2O_P, H2O_P_SERVICES);
                if (services != null && services.length() > 0) {
                    JSONObject service = JSONHelper.extractObject(services, 0);
                    if (service != null) {
                        mServiceName = JSONHelper.extractString(service, H2O_P_SERVICES_NAME);
                    }
                }

                JSONArray traffics = JSONHelper.extractArray(H2O_P, H2O_P_TRAFFICS);
                if (traffics != null && traffics.length() > 0) {
                    JSONObject traffic = JSONHelper.extractObject(traffics, 0);
                    if (traffic != null) {
                        JSONObject muia = JSONHelper.extractObject(traffic, H2O_P_TRAFFICS_MUIA);
                        if (muia != null) {
                            mSharedQuotaSize = JSONHelper.extractLong(muia, H2O_P_TRAFFICS_MUIA_SHARED_QUOTA);
                        }
                        mConsumedQuotaSize = JSONHelper.extractLong(traffic, H2O_P_TRAFFICS_VALUE);
                    }
                }
            }
        }
    }

    public static H2OProfile create(String data) {
        if (data == null) {
            Log.d(TAG, "Error creating H2O Profile object: null data");
            return null;
        }

        String jsonSource = null;
        Pattern pattern = Pattern.compile(PROFILE_PATTERN);
        Matcher matcher = pattern.matcher(data);
        if (matcher.find()) {
            jsonSource = matcher.group(1).concat("}}}");
        }

        if (jsonSource == null) {
            Log.d(TAG, "Error, no profile's json was found in profile data");
            return null;
        }

        JSONObject jsonData;
        try {
            jsonData = new JSONObject(jsonSource);
        } catch (JSONException e) {
            Log.d(TAG, "Error creating H2O Profile object");
            return null;
        }

        return new H2OProfile(jsonData);
    }

    public Long getNumber() {
        return mNumber;
    }

    public String getRegionId() {
        return mRegionId;
    }

    public String getRoaming() {
        return mRoaming;
    }

    public String getServiceName() {
        return mServiceName;
    }

    public Long getConsumedQuotaSize() {
        return mConsumedQuotaSize;
    }

    public Long getSharedQuotaSize() {
        return mSharedQuotaSize;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Number = " + mNumber + "\n");
        sb.append("Region Id = " + mRegionId + "\n");
        sb.append("Roaming = " + mRoaming + "\n");
        sb.append("Service name = " + mServiceName + "\n");
        sb.append("Shared quota = " + (mSharedQuotaSize / 1024) + "Mb" + "\n");
        sb.append("Consumed quota = " + (mConsumedQuotaSize / 1024) + "Mb" + "\n");
        return sb.toString();
    }

    private static class JSONHelper {
        public static String extractString(JSONObject o, String name) {
            String value = "n\\a";
            try {
                value = o.getString(name);
            } catch (JSONException e) {
            }
            return value;
        }

        public static Long extractLong(JSONObject o, String name) {
            Long value = -1L;
            try {
                value = o.getLong(name);
            } catch (JSONException e) {
            }
            return value;
        }

        public static JSONObject extractObject(JSONObject o, String name) {
            JSONObject obj = null;
            try {
                obj = o.getJSONObject(name);
            } catch (JSONException e) {
            }
            return obj;
        }

        public static JSONObject extractObject(JSONArray arr, int index) {
            JSONObject obj = null;
            try {
                obj = arr.getJSONObject(index);
            } catch (JSONException e) {
            }
            return obj;
        }

        public static JSONArray extractArray(JSONObject o, String name) {
            JSONArray arr = null;
            try {
                arr = o.getJSONArray(name);
            } catch (JSONException e) {
            }
            return arr;
        }
    }
}
