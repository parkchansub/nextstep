package webserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.HttpRequestUtils;
import util.IOUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class HttpRequest {
    private static final Logger log = LoggerFactory.getLogger(HttpRequest.class);

    private Map<String, String> headers = new HashMap<String, String>();
    private Map<String, String> params = new HashMap<String, String>();
    private RequestLine requestLine;

    public HttpRequest(InputStream in) {
        try{
            BufferedReader br = new BufferedReader(new InputStreamReader(in,"UTF-8"));
            String line = br.readLine();
            requestLine = new RequestLine(line);
            if (line == null) {
                return;
            }


            line = br.readLine();
            while (!line.equals("")) {
                log.debug("header : {}", line);
                String[] tokens = line.split(":");
                headers.put(tokens[0].trim(), tokens[1].trim());
                line = br.readLine();
            }

            if (requestLine.getMethod().isPost()) {
                String body = IOUtils.readData(br, Integer.parseInt(headers.get("Content-Length")));
                params = HttpRequestUtils.parseQueryString(body);
            } else {
                params = requestLine.getParams();
            }


        } catch (IOException io) {
            log.error(io.getMessage());
        }
    }



    public String getHeader(String key) {
        return this.headers.get(key);
    }

    public String getParam(String key) {
        return this.params.get(key);
    }

    public HttpMethod getMethod() {
        return this.requestLine.getMethod();
    }

    public String getPath() {
        return this.requestLine.getPath();
    }
}
