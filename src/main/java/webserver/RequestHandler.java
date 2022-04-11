package webserver;

import model.Database;
import model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.HttpRequestUtils;
import util.IOUtils;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class RequestHandler extends Thread {
    private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);


    private Socket connection;

    public RequestHandler(Socket connectionSocket) {
        this.connection = connectionSocket;
    }

    public void run() {
        log.debug("New Client Connect! Connected IP : {}, Port : {}", connection.getInetAddress(),
                connection.getPort());

        try (InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream()) {

            HttpRequest request = new HttpRequest(in);

            BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            String url = request.getPath();
            log.debug("Content-Length : {}", request.getHeader("Content-Length"));



            DataOutputStream dos = new DataOutputStream(out);
            if (url.startsWith("/user/create")) {
                String requestBody = IOUtils.readData(br, Integer.parseInt(request.getHeader("Content-Length")));
                Map<String, String> params = HttpRequestUtils.parseQueryString(requestBody);

                User user = new User(params.get("userId"), params.get("password"), params.get("name"), params.get("email"));
                Database.addUser(user);
                log.debug("User : {} ", user);
                response302Header(dos);
            } else if (url.equals("/user/login")) {
                String requestBody = IOUtils.readData(br, Integer.parseInt(request.getHeader("Content-Length")));
                Map<String, String> params = HttpRequestUtils.parseQueryString(requestBody);

                log.debug("UserId : {} , password : {} ",params.get("userId"), params.get("password"));
                User user = Database.getUser(params.get("userId"));


                if (user == null) {
                    log.debug("User Not Found");
                    response302HeaderWithCookie(dos, "logined=false", "/user/login_failed.html");
                }
                else if (params.get("password").equals(user.getPassword())) {
                    log.debug("login sucess!!");
                    response302HeaderWithCookie(dos, "logined=true", "/index.html");
                } else {
                    log.debug("Password Mismatch");
                    response302HeaderWithCookie(dos, "logined=false","/user/login_failed.html");
                }

            } else if (url.startsWith("/user/list")) {
                if (!request.getHeader("Cookie").equals("logined=true")) {
                    response302Header(dos);
                }
            } else if (url.endsWith(".css")) {
                byte[] body = Files.readAllBytes(new File("./webapp" + url).toPath());
                response200HeaderWithCss(dos, body.length);
                responseBody(dos, body);
            } else {
                responseResource(out,url);
            }


        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void responseResource(OutputStream out, String url) throws IOException{
        DataOutputStream dos = new DataOutputStream(out);
        byte[] body = Files.readAllBytes(new File("./webapp" + url).toPath());
        response200Header(dos, body.length);
        responseBody(dos, body);
    }


    private void response302HeaderWithCookie(DataOutputStream dos, String cookie, String location) {
        try {
            dos.writeBytes("HTTP/1.1 302 Found \r\n");
            dos.writeBytes("Location: "+location+"\r\n");
            dos.writeBytes("Set-Cookie: " +cookie+"\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void response302Header(DataOutputStream dos) {
        try {
            dos.writeBytes("HTTP/1.1 302 Found \r\n");
            dos.writeBytes("Location: /index.html\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void response200HeaderWithCss(DataOutputStream dos, int lengthOfBodyContent) {
        try {
            dos.writeBytes("HTTP/1.1 200 OK \r\n");
            dos.writeBytes("Content-Type: text/css;charset=utf-8\r\n");
            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
    private void response200Header(DataOutputStream dos, int lengthOfBodyContent) {
        try {
            dos.writeBytes("HTTP/1.1 200 OK \r\n");
            dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }


    private void responseBody(DataOutputStream dos, byte[] body) {
        try {
            dos.write(body, 0, body.length);
            dos.flush();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
}
