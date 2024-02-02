package com.seafile.seadroid2.account;

import com.seafile.seadroid2.util.Utils;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * This class used to manage Account information
 */
public class AccountInfo {
    public static final String SPACE_USAGE_SEPERATOR = " / ";
    private long usage;
    private long total;
    private String email;
    private String server;
    private String avatar_url;
    private String name;
    private String space_usage;// "6.0382327%"
//    private String login_id;
//    private String department;
//    private String contact_email;
//    private String institution;
//    private boolean is_staff;
//    private int file_updates_email_interval;
//    private int collaborate_email_interval;

    private AccountInfo() {
    }

    public static AccountInfo fromJson(JSONObject accountInfo, String server) throws JSONException {
        AccountInfo info = new AccountInfo();
        info.server = server;
        info.usage = accountInfo.getLong("usage");
        info.total = accountInfo.getLong("total");
        info.email = accountInfo.getString("email");
        info.name = accountInfo.optString("name");
        info.avatar_url = accountInfo.optString("avatar_url");
        info.space_usage = accountInfo.optString("space_usage");
        return info;
    }

    public long getUsage() {
        return usage;
    }

    public long getTotal() {
        return total;
    }

    public String getEmail() {
        return email;
    }

    public String getServer() {
        return server;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        String server = Utils.stripSlashes(getServer());
        return Utils.assembleUserName(name, email, server);
    }

    public void setServer(String server) {
        this.server = server;
    }

    public String getSpaceUsed() {
        String strUsage = Utils.readableFileSize(usage);
        String strTotal = Utils.readableFileSize(total);
        return strUsage + SPACE_USAGE_SEPERATOR + strTotal;
    }

    public String getAvatarUrl() {
        return avatar_url;
    }
}
