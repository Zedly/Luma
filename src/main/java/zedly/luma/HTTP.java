package zedly.luma;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;

/**
 * A utility class for retrieving HTTP resources.
 *
 * @author Dennis
 */
public class HTTP {

    /**
     * Perform a HTTP GET request for the given URL.
     *
     * @param url the URL of the resource to retrieve
     * @return a structure containing response headers and payload
     * @throws IOException if a network error occurs
     */
    public static HTTPResponse get(URL url) throws IOException {
        URLConnection con = (URLConnection) url.openConnection();
        con.setConnectTimeout(30000);
        if (con instanceof HttpURLConnection) {
            ((HttpURLConnection) con).setRequestMethod("GET");
        }
        con.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.31 (KHTML, like Gecko) Chrome/26.0.1410.64 Safari/537.31");
        con.setDoInput(true);
        Map<String, List<String>> headers = con.getHeaderFields();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            DataInputStream input = new DataInputStream(con.getInputStream());
            for (int c = input.read(); c != -1; c = input.read()) {
                bos.write(c);
            }
            input.close();
        } catch (IOException ignored) {
        }
        byte[] content = bos.toByteArray();
        return new HTTPResponse(headers, content);
    }

    public static class HTTPResponse {

        private final Map<String, List<String>> headers;
        private final byte[] content;

        public HTTPResponse(Map<String, List<String>> headers, byte[] content) {
            this.headers = headers;
            this.content = content;
        }

        public String getCookieString() {
            String httpCookies = "";
            List<String> cookies = headers.get("Set-Cookie");
            for (int i = 0; i < cookies.size() - 1; i++) {
                httpCookies += cookies.get(i).substring(0, cookies.get(i).indexOf(";")) + "; ";
            }
            httpCookies += cookies.get(cookies.size() - 1).substring(0, cookies.get(cookies.size() - 1).indexOf(";"));
            return httpCookies;
        }

        public Map<String, List<String>> getHeaders() {
            return headers;
        }

        public byte[] getContent() {
            return content;
        }
    }
}
