package com.hotel.survey.utils;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

public class NetworkUtils {

    public static String getActiveIp(Context context) {
        // 1. Ethernet eth0 — most TV boxes use wired LAN
        String eth = getInterfaceIp("eth0");
        if (eth != null) return eth;

        // 2. Any other non-loopback wired/WiFi interface
        String any = getAnyLanIp();
        if (any != null) return any;

        // 3. WiFi fallback (requires ACCESS_WIFI_STATE)
        return getWifiIp(context);
    }

    private static String getInterfaceIp(String ifaceName) {
        try {
            NetworkInterface ni = NetworkInterface.getByName(ifaceName);
            if (ni == null || !ni.isUp() || ni.isLoopback()) return null;
            Enumeration<InetAddress> addrs = ni.getInetAddresses();
            while (addrs.hasMoreElements()) {
                InetAddress addr = addrs.nextElement();
                String ip = addr.getHostAddress();
                if (ip != null && isLanIpv4(ip)) return ip;
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static String getAnyLanIp() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            if (interfaces == null) return null;
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (!ni.isUp() || ni.isLoopback()) continue;
                Enumeration<InetAddress> addrs = ni.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    String ip = addrs.nextElement().getHostAddress();
                    if (ip != null && isLanIpv4(ip)) return ip;
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static String getWifiIp(Context context) {
        try {
            WifiManager wm = (WifiManager) context.getApplicationContext()
                    .getSystemService(Context.WIFI_SERVICE);
            if (wm == null) return null;
            WifiInfo info = wm.getConnectionInfo();
            int raw = info.getIpAddress();
            if (raw == 0) return null;
            return String.format("%d.%d.%d.%d",
                    raw & 0xff, (raw >> 8) & 0xff, (raw >> 16) & 0xff, (raw >> 24) & 0xff);
        } catch (Exception ignored) {}
        return null;
    }

    private static boolean isLanIpv4(String ip) {
        // IPv4 only, exclude loopback and link-local
        if (!ip.contains(".") || ip.contains(":")) return false;
        return !ip.startsWith("127.") && !ip.startsWith("169.254.");
    }
}
