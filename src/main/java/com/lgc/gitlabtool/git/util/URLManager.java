package com.lgc.gitlabtool.git.util;

public class URLManager {

    public static final String URL_SUFFIX = "/api/v4";
    public static final String HTTPS = "https://";
    public static final String HTTP = "http://";

    /**
     * Trims the URL - cuts the "http://" or "https://" prefixes and "/api/v3" suffix
     *
     * @param url - server URL
     * @return trimmed URL (URL main part)
     */
    public static String trimServerURL(String url) {
        if (!url.contains("/")) {
            return url;
        }
        String protocol = url.contains(HTTPS) ? HTTPS : url.contains(HTTP) ? HTTP : null;

        String resultedURL = url;
        if (protocol != null) {
            resultedURL = resultedURL.substring(url.indexOf(protocol) + protocol.length());
        }
        if (url.contains(URL_SUFFIX)) {
            resultedURL = resultedURL.substring(0, resultedURL.indexOf(URL_SUFFIX));
        }
        return resultedURL;
    }

    /**
     * Adds the "https://" prefix and "/api/v3" suffix to URL main part
     *
     * @param urlMainPart - main part of URL
     * @return modified URL
     */
    public static String completeServerURL(String urlMainPart) {
        return HTTPS + urlMainPart + URL_SUFFIX;
    }

    /**
     * Adds the "https://" prefix to URL main part
     *
     * @param urlMainPart - main part of URL
     * @return modified URL
     */
    public static String shortServerURL(String urlMainPart) {
        return HTTPS + urlMainPart;
    }

    /**
     * Checks if URL is valid
     *
     * URL could contain http:// or https:// (or blank) prefix, any host that contains only letters, digits or dots and
     * "/api/v3" suffix (or not)
     *
     * @param url for checking
     * @return <code>true</code> if <code>url</code> matches regexp and <code>false</code> if not
     */
    public static boolean isURLValid(String url) {
        String regexp = "(" + HTTP + "|" + HTTPS + ")?([a-z0-9]*\\.?[a-z0-9]+)+(" + URL_SUFFIX + ")?" + "/?";
        return url.matches(regexp);
    }

}
