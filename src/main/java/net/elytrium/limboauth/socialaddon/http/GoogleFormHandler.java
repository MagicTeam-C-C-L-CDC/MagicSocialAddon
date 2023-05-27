package net.elytrium.limboauth.socialaddon.http;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.stream.Collectors;

public class GoogleFormHandler implements HttpHandler {
    private HttpServer httpServer;
    private HttpContext httpContext;

    public GoogleFormHandler(){
        try {
            httpServer = HttpServer.create(new InetSocketAddress("192.168.1.7", 27015), 10);
            httpContext = httpServer.createContext("/api", new GoogleFormHandler());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void start(){
        if(httpServer != null)
            httpServer.start();
        else throw new NullPointerException();
    }
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if(exchange.getRequestMethod().equals("POST"))
            handleGoogleForm(exchange.getRequestBody());

        exchange.sendResponseHeaders(200, 0);

        exchange.getResponseBody().flush();
        exchange.getResponseBody().close();
    }

    public void handleGoogleForm(InputStream stream){
        String body = new BufferedReader(new InputStreamReader(stream)).lines().collect(Collectors.joining("\n"));
        System.out.println(body);
    }
}
