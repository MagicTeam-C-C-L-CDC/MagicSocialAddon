package ru.magicteam.proxy.social.controller.google;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.elytrium.limboauth.model.RegisteredPlayer;
import ru.magicteam.proxy.social.Settings;
import ru.magicteam.proxy.social.controller.Controller;
import ru.magicteam.proxy.social.controller.discord.DType;
import ru.magicteam.proxy.social.controller.discord.DiscordController;
import ru.magicteam.proxy.social.controller.discord.DiscordEvent;
import ru.magicteam.proxy.social.model.ModelAPI;

import javax.sound.sampled.Control;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.stream.Collectors;

public class GoogleFormController extends Controller{

    private final HttpServer httpServer;
    private final HttpContext httpContext;

    private final HashMap<String, Long> googleFormSession = new HashMap<>();

    public GoogleFormController(ModelAPI api, DiscordController discordController){
        super(api);
        try {
            httpServer = HttpServer.create(new InetSocketAddress(Settings.IMP.MAIN.GOOGLE_FORM.IP, Settings.IMP.MAIN.GOOGLE_FORM.PORT), 10);
            httpContext = httpServer.createContext(Settings.IMP.MAIN.GOOGLE_FORM.API_PATH, this::handle);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        discordController.subscribe(DType.GENERATE_GOOGLE_FORM, e -> {
            if(e.type == DiscordEvent.EventType.SLASH_COMMAND)
                try {
                    ((SlashCommandInteractionEvent)e.event)
                            .reply(createGoogleFormSession(e.id))
                            .setEphemeral(true)
                            .queue();
                } catch (NoSuchAlgorithmException ex) {
                    ex.printStackTrace();
                }
        });
    }

    public void start(){
        if(httpServer != null)
            httpServer.start();
        else throw new NullPointerException();
    }

    public String createGoogleFormSession(Long id) throws NoSuchAlgorithmException {
        byte[] longToByte = convert(id);
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(longToByte);
        String getStringValue = new String(hash);
        return String.join("&", Settings.IMP.MAIN.GOOGLE_FORM_URL, getStringValue);
    }

    private byte[] convert(Long value){
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(value);
        return buffer.array();
    }

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
