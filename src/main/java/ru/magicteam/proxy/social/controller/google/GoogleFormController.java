package ru.magicteam.proxy.social.controller.google;


import com.google.gson.Gson;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.slf4j.Logger;
import ru.magicteam.proxy.social.Settings;
import ru.magicteam.proxy.social.controller.Controller;
import ru.magicteam.proxy.social.controller.discord.DType;
import ru.magicteam.proxy.social.controller.discord.DiscordController;
import ru.magicteam.proxy.social.controller.discord.DiscordEvent;
import ru.magicteam.proxy.social.model.GoogleFormSession;
import ru.magicteam.proxy.social.model.History;
import ru.magicteam.proxy.social.model.ModelAPI;
import ru.magicteam.proxy.social.model.Player;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class GoogleFormController extends Controller{

    private final HttpServer httpServer;
    private final HttpContext httpContext;

    private final DiscordController discordController;

    public GoogleFormController(ModelAPI api, DiscordController discordController, Logger logger){
        super(api, logger);

        this.discordController = discordController;

        try {
            logger.info(String.join(" ",
                    "Creating http server context on ip:",
                    Settings.IMP.MAIN.GOOGLE_FORM.IP, "and port:",
                    Integer.toString(Settings.IMP.MAIN.GOOGLE_FORM.PORT)
            ));
            httpServer = HttpServer.create(new InetSocketAddress(Settings.IMP.MAIN.GOOGLE_FORM.IP, Settings.IMP.MAIN.GOOGLE_FORM.PORT), 10);
            httpContext = httpServer.createContext(Settings.IMP.MAIN.GOOGLE_FORM.API_PATH, this::handle);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        discordController.subscribe(DType.GENERATE_GOOGLE_FORM, e -> {
            if(e.type == DiscordEvent.EventType.SLASH_COMMAND) {
                try {
                    SlashCommandInteractionEvent event = (SlashCommandInteractionEvent) e.event;
                    Long userID = event.getOption(DType.GENERATE_GOOGLE_FORM_USER_OPTION.value).getAsLong();

                    if(!api.requestExist(e.id, "google_form")){
                        event
                                .reply(createGoogleFormSession(userID))
                                .setEphemeral(true)
                                .queue();
                    }
                    else {
                        event
                                .reply(Settings.IMP.MAIN.GOOGLE_FORM.REPLY_ALREADY_CREATE_GOOGLE_FORM_SESSION)
                                .setEphemeral(true)
                                .queue();
                    }

                } catch (SQLException | NoSuchAlgorithmException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
    }

    public void start(){
        logger.info(String.join(" ",
                "Trying to start http server on ip:",
                Settings.IMP.MAIN.GOOGLE_FORM.IP, "and port:",
                Integer.toString(Settings.IMP.MAIN.GOOGLE_FORM.PORT)
        ));
        if(httpServer != null)
            httpServer.start();
        else throw new NullPointerException();
    }

    public void stop() throws InterruptedException {
        logger.info(String.join(" ",
                "Trying to stop http server on ip:",
                Settings.IMP.MAIN.GOOGLE_FORM.IP, "and port:",
                Integer.toString(Settings.IMP.MAIN.GOOGLE_FORM.PORT)
        ));
        if(httpServer != null)
            httpServer.stop(0);
        else throw new NullPointerException();
    }

    public String createGoogleFormSession(Long id) throws NoSuchAlgorithmException {
        try {
            String hash = api.createGoogleFormSession(id);
            return String.join("", Settings.IMP.MAIN.GOOGLE_FORM.URL, hash);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void handle(HttpExchange exchange) throws IOException {
        String requestMethod = exchange.getRequestMethod();

        String data = new BufferedReader(new InputStreamReader(exchange.getRequestBody()))
                .lines()
                .collect(Collectors.joining("\n"));

        exchange.sendResponseHeaders(200, 0);
        exchange.getResponseBody().flush();
        exchange.getResponseBody().close();

        if(requestMethod.equals("POST")) {
            Gson gson = new Gson();
            GoogleFormResponse response = gson.fromJson(data, GoogleFormResponse.class);

            try {
                EmbedBuilder builder = new EmbedBuilder();
                Optional<GoogleFormAnswer> optionalID = response.fields().stream().filter(f -> f.name().equals(Settings.IMP.MAIN.GOOGLE_FORM.REQUEST_ID)).findFirst();

                if(optionalID.isEmpty())
                    throw new NullPointerException("Google form reqeust dont contain id field!");

                GoogleFormAnswer id = optionalID.get();

                if(!api.existGoogleFormSession(id.value()))
                    throw new NullPointerException(String.join(" ", Settings.IMP.MAIN.GOOGLE_FORM.ERROR_NO_HASH_FOR_DISCORD_ID, id.toString()));

                GoogleFormSession session = api.getGoogleFormSesion(id.value());
                Optional<Player> player = api.queryPlayerByID(session.getDiscordID());
                if(player.isPresent()) {
                    discordController.sendMessage(
                            Settings.IMP.MAIN.DISCORD_HISTORY_CHANNEL_ID,
                            "Пользователь <@" + session.getDiscordID() + "> подал заявку на доступ, хотя уже имеет его."
                    );
                    return;
                }
                Collection<History> playerHistory = api.allHistory(session.getDiscordID(), h -> true);
                builder.setAuthor(Settings.IMP.MAIN.GOOGLE_FORM.EMBED_AUTHOR + " №" + playerHistory.size());

                builder.addField(Settings.IMP.MAIN.GOOGLE_FORM.REQUEST_AUTHOR, "<@"+session.getDiscordID()+">", false);

                for(GoogleFormAnswer answer : response.fields())
                    if(!answer.name().equals(Settings.IMP.MAIN.GOOGLE_FORM.REQUEST_ID))
                        builder.addField(answer.name(), answer.value(), false);

                MessageEmbed embed = builder.build();
                Button accept = Button.success(
                        String.join(Settings.IMP.MAIN.GOOGLE_FORM.SEPARATOR, DType.BUTTON_ADMIN_ACCEPT_REQUEST.value, session.getDiscordID().toString()),
                        Settings.IMP.MAIN.GOOGLE_FORM.ACCEPT_PLAYER_REQUEST
                );

                Button deny = Button.danger(
                        String.join(Settings.IMP.MAIN.GOOGLE_FORM.SEPARATOR, DType.BUTTON_ADMIN_DENY_REQUEST.value, session.getDiscordID().toString()),
                        Settings.IMP.MAIN.GOOGLE_FORM.DENY_PLAYER_REQUEST
                );

                ActionRow row = ActionRow.of(accept, deny);
                discordController.sendComponents(Settings.IMP.MAIN.DISCORD_ADMIN_CHANNEL_ID, embed, List.of(row));
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
