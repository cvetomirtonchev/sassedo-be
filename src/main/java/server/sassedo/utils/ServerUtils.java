package server.sassedo.utils;

import server.sassedo.security.jwt.JwtUtils;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

public class ServerUtils {

    public static String getSiteURL(HttpServletRequest request) {
        String siteURL = request.getRequestURL().toString();
        return siteURL.replace(request.getServletPath(), "");
    }

    public static Long getUserId(HttpServletRequest request, JwtUtils jwtUtils) {
        String jwt = jwtUtils.resolveToken(request.getHeader("Authorization"));
        if (jwt == null) {
            return null;
        }
        return jwtUtils.extractUserId(jwt);
    }

    public static int getOffsetInSeconds(String timezone) {
        if (timezone == null) {
            timezone = "Europe/Sofia";
        }
        ZoneId zoneId = ZoneId.of(timezone);
        ZoneOffset zoneOffset = zoneId.getRules().getOffset(Instant.now());
        return zoneOffset.getTotalSeconds();
    }
}
