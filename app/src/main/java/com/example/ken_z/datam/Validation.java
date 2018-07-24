package com.example.ken_z.datam;

import android.content.Context;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Validation {
    public static String validateIP(String ipAddress) {
        ipAddress = ipAddress.trim();
        String returnMessage = null;
        if (ipAddress.equals("")) {
            returnMessage = "IP Address is blank";
        } else {
            Pattern pattern;
            Matcher matcher;

            String IPADDRESS_PATTERN =
                    "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                            "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                            "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                            "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";
            pattern = Pattern.compile(IPADDRESS_PATTERN);
            matcher = pattern.matcher(ipAddress);
            if (!matcher.matches()) {
                returnMessage = "IP pattern incorrect";
            }
        }
        return returnMessage;
    }

    public static String validatePort(Context context, String port) {
        port = port.trim();
        if (port.equals("")) {
            return context.getString(R.string.port_empty);
        } else {
            try {
                int portInt = Integer.parseInt(port);
                if (portInt >= 0 && portInt <= 65535) {
                    return null;
                } else {
                    return context.getString(R.string.port_value_invalid);
                }
            } catch (NumberFormatException nfe) {
                return context.getString(R.string.port_not_integer);
            }
        }
    }
}
