/*
 * Copyright (C) 2022 - 2023 Elytrium
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package ru.magicteam.proxy.social.controller.discord;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.LayoutComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.slf4j.Logger;
import ru.magicteam.proxy.social.Settings;
import ru.magicteam.proxy.social.controller.Controller;
import ru.magicteam.proxy.social.controller.discord.register.DiscordCommands;
import ru.magicteam.proxy.social.controller.discord.ui.button.ButtonItem;
import ru.magicteam.proxy.social.controller.discord.ui.button.ButtonVisibility;
import ru.magicteam.proxy.social.controller.discord.ui.embed.EmbedFormJson;
import ru.magicteam.proxy.social.controller.discord.ui.modal.Modals;
import ru.magicteam.proxy.social.model.History;
import ru.magicteam.proxy.social.model.ModelAPI;
import ru.magicteam.proxy.social.model.Player;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class DiscordController extends Controller {

  private JDA jda;

  private List<Role> requiredRoles = new LinkedList<>();

  private HashMap<DType, List<Consumer<DiscordEvent>>> events = new HashMap<>();

  public DiscordController(ModelAPI api, Logger logger) {
    super(api, logger);
    subscribe(DType.PING, e -> {
      if(e.type.equals(DiscordEvent.EventType.SLASH_COMMAND)){
        long time = System.currentTimeMillis();
        SlashCommandInteractionEvent slashCommand = (SlashCommandInteractionEvent) e.event;
        slashCommand.reply("Pong!").setEphemeral(true)
                .flatMap(v -> slashCommand.getHook().editOriginalFormat("Pong: %d ms", System.currentTimeMillis() - time))
                .queue();
      }
    });

    subscribe(DType.BUTTON_ADMIN_ACCEPT_REQUEST, e -> {
      if(!e.type.equals(DiscordEvent.EventType.BUTTON))
        return;

      ButtonInteractionEvent event = (ButtonInteractionEvent) e.event;
      Optional<User> user = Optional.ofNullable(jda.getUserById(e.id));

      MessageEmbed embed;
      if(event.getMessage().getEmbeds().size() == 1)
        embed = event.getMessage().getEmbeds().get(0);
      else throw new IllegalArgumentException();

      EmbedBuilder updatedEmbed = new EmbedBuilder(embed)
              .addBlankField(false)
              .addField(Settings.IMP.MAIN.GOOGLE_FORM.HISTORY_REQUEST_ACCEPTED, "<@" + event.getUser().getIdLong() + ">", false)
              .setFooter(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss")));

      MessageEmbed finalEmbed = updatedEmbed.build();

      Optional<String> newPlayerNickname = embed
              .getFields()
              .stream()
              .filter(field -> Objects.equals(field.getName(), DType.GOOGLE_FORM_NICKNAME_FIELD.value))
              .map(MessageEmbed.Field::getValue)
              .findFirst();

      if (newPlayerNickname.isEmpty())
        throw new IllegalArgumentException("Player access dont have nickname field");

      try {
        api.deleteGoogleFormSesion(e.id);
        ModelAPI.Status historyStatus = api.createHistory(
                e.id,
                event.getUser().getIdLong(),
                History.Action.ACCESS.value,
                History.Status.ACCEPT.value,
                finalEmbed.toData().toString()
        );
        ModelAPI.Status playerStatus = api.createPlayer(e.id, newPlayerNickname.get());

        if(playerStatus == ModelAPI.Status.ALREADY_EXISTS || historyStatus == ModelAPI.Status.ALREADY_EXISTS)
          event.reply("Такой пользователь уже существует").setEphemeral(true).queue();
        else {
          if(user.isEmpty())
            event.reply(Settings.IMP.MAIN.USER_ALREADY_LEAVE_SERVER).setEphemeral(true).queue();
          else
            event.reply(Settings.IMP.MAIN.USER_SUCCESSFULY_ACCEPTED).setEphemeral(true).queue();
        }
      } catch (SQLException ex) {
        throw new RuntimeException(ex);
      }

      TextChannel historyChannel = jda.getTextChannelById(Settings.IMP.MAIN.DISCORD_HISTORY_CHANNEL_ID);
      if(historyChannel == null) throw new RuntimeException("History channel id is null");

      EmbedBuilder historyEmbedBuilder = new EmbedBuilder()
              .addField(Settings.IMP.MAIN.GOOGLE_FORM.HISTORY_REQUEST_ACCEPTED, "<@" + event.getUser().getIdLong() + ">", false)
              .addField(Settings.IMP.MAIN.GOOGLE_FORM.HISTORY_REQUEST_ACCEPTED_SEARCH_KEY,
                      String.join(
                              ", ",
                              user.map(User::getName).orElse("empty"),
                              "<@" + user.map(ISnowflake::getIdLong).orElse(e.id) + ">",
                              user.map(User::getAsTag).orElse("")),
                      false
              )
              .setFooter(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss")));

      historyChannel.sendMessageEmbeds(historyEmbedBuilder.build()).queue();

      event.getMessage().delete().queue();
    });

    subscribe(DType.BUTTON_ADMIN_DENY_REQUEST, e -> {
      if(!e.type.equals(DiscordEvent.EventType.BUTTON))
        return;

      ButtonInteractionEvent event = (ButtonInteractionEvent) e.event;
      event.replyModal(Modals.requestDenyModal(e.id)).queue();
    });

    subscribe(DType.DENY_REASON, e -> {
      if(!e.type.equals(DiscordEvent.EventType.MODAL))
        return;

      ModalInteractionEvent event = (ModalInteractionEvent) e.event;
      Optional<User> user = Optional.ofNullable(jda.getUserById(e.id));

      MessageEmbed embed;
      if(event.getMessage().getEmbeds().size() == 1)
        embed = event.getMessage().getEmbeds().get(0);
      else throw new IllegalArgumentException();

      EmbedBuilder updatedEmbed = new EmbedBuilder(embed)
              .addBlankField(false)
              .addField(Settings.IMP.MAIN.GOOGLE_FORM.HISTORY_REQUEST_DENYED, "<@" + event.getUser().getIdLong() + ">", false)
              .addField(Settings.IMP.MAIN.GOOGLE_FORM.HISTORY_REQUEST_DENY_REASON,
                      event
                              .getMessage()
                              .getEmbeds()
                              .stream()
                              .map(em -> em
                                      .getFields()
                                      .stream()
                                      .map(MessageEmbed.Field::getValue)
                                      .filter(Objects::nonNull)
                                      .reduce(String::concat)
                                      .orElse("")
                              )
                              .reduce(String::concat)
                              .orElse("for fun"),
                      false
              )
              .setFooter(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss")));

      MessageEmbed finalEmbed = updatedEmbed.build();

      try {
        api.deleteGoogleFormSesion(e.id);
        ModelAPI.Status historyStatus = api.createHistory(e.id, event.getUser().getIdLong(), History.Action.ACCESS.value, History.Status.DENY.value, finalEmbed.toData().toString());

        if(historyStatus == ModelAPI.Status.ALREADY_EXISTS)
          event.reply("Такой пользователь уже существует").setEphemeral(true).queue();
        else {
          if(user.isEmpty())
            event.reply(Settings.IMP.MAIN.USER_ALREADY_LEAVE_SERVER).setEphemeral(true).queue();
          else
            event.reply(Settings.IMP.MAIN.USER_DENYED).setEphemeral(true).queue();
        }
      } catch (SQLException ex) {
        throw new RuntimeException(ex);
      }

      TextChannel historyChannel = jda.getTextChannelById(Settings.IMP.MAIN.DISCORD_HISTORY_CHANNEL_ID);
      if(historyChannel == null) throw new RuntimeException("History channel id is null");

      EmbedBuilder historyEmbedBuilder = new EmbedBuilder()
              .addField(Settings.IMP.MAIN.GOOGLE_FORM.HISTORY_REQUEST_DENYED, "<@" + event.getUser().getIdLong() + ">", false)
              .addField(Settings.IMP.MAIN.GOOGLE_FORM.HISTORY_REQUEST_ACCEPTED_SEARCH_KEY,
                      String.join(
                              ", ",
                              user.map(User::getName).orElse("empty"),
                              "<@" + user.map(ISnowflake::getIdLong).orElse(e.id) + ">",
                              user.map(User::getAsTag).orElse("")),
                      false
              )
              .setFooter(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss")));

      historyChannel.sendMessageEmbeds(historyEmbedBuilder.build()).queue();

      event.getMessage().delete().queue();
    });

    subscribe(DType.SEARCH, e -> {
      if(e.type != DiscordEvent.EventType.SLASH_COMMAND)
        return;

      SlashCommandInteractionEvent event = (SlashCommandInteractionEvent) e.event;
      List<OptionMapping> options = event.getOptions();
      
      Collection<History> userHistory = new LinkedList<>();

      switch (DType.fromString(event.getSubcommandName()).orElse(DType.ERROR)){
        case SEARCH_DISCORD_USER, SEARCH_DISCORD_ID -> {
          try {
            userHistory.addAll(api.searchHistory(options.get(0).getAsLong()));
          } catch (SQLException ex) {
            throw new RuntimeException(ex);
          }
        }
        case SEARCH_DISCORD_USERNAME -> {
          try {
            userHistory.addAll(api.searchHistoryByName(options.get(0).getAsString()));
          } catch (SQLException ex) {
            throw new RuntimeException(ex);
          }
        }
      }

      if (userHistory.isEmpty()) {
        event.reply("Не найдено информации в истории по этому пользователю").setEphemeral(true).queue();
        return;
      }

      Collection<MessageEmbed> embedsReply = userHistory.stream().map(h -> EmbedFormJson.parse(h.getInfo())).toList();
      event.replyEmbeds(embedsReply).setEphemeral(true).queue();
    });
  }

  public void subscribe(DType type, Consumer<DiscordEvent> consumer){
    logger.info("Register new handler for type " + type);
    if(events.containsKey(type))
      events.get(type).add(consumer);
    else events.put(type, List.of(consumer));
  }

  public void callEvent(Long id, DType type, DiscordEvent.EventType eventType, Event event){
    if(events.containsKey(type))
      events.get(type).forEach(e -> e.accept(new DiscordEvent(id, eventType, event)));
    else logger.warn("Seems that discord controller dont have handler for type " + type);
  }

  public void start() throws InterruptedException {
    JDABuilder jdaBuilder = JDABuilder.create(Settings.IMP.MAIN.DISCORD.TOKEN, GatewayIntent.DIRECT_MESSAGES, GatewayIntent.GUILD_MEMBERS);
    this.jda = jdaBuilder
            .disableCache(CacheFlag.ACTIVITY, CacheFlag.VOICE_STATE, CacheFlag.EMOJI, CacheFlag.CLIENT_STATUS, CacheFlag.ONLINE_STATUS)
            .setActivity(Settings.IMP.MAIN.DISCORD.ACTIVITY_ENABLED
                    ? Activity.of(Settings.IMP.MAIN.DISCORD.ACTIVITY_TYPE, Settings.IMP.MAIN.DISCORD.ACTIVITY_NAME, Settings.IMP.MAIN.DISCORD.ACTIVITY_URL)
                    : null)
            .build()
            .awaitReady();

    for (Object requiredRole : Settings.IMP.MAIN.DISCORD.REQUIRED_ROLES) {
      Role roleById;
      if (requiredRole instanceof Long)
        roleById = jda.getRoleById((Long) requiredRole);
      else if (requiredRole instanceof String)
        roleById = jda.getRoleById((String) requiredRole);
      else
        throw new IllegalArgumentException("Required-roles entry cannot be of class " + requiredRole.getClass().getName());

      if (roleById != null)
        requiredRoles.add(roleById);
    }

    jda.updateCommands().addCommands(DiscordCommands.commands).queue();
    jda.addEventListener(new DiscordListener(this, logger));
  }

  public void stop() {
    if (this.jda != null) {
      this.jda.shutdown();
    }
  }

  public void sendMessage(Long id, String content, List<List<ButtonItem>> buttons, ButtonVisibility visibility) {
    User user = this.jda.retrieveUserById(id).complete();
    if (user == null) {
      return;
    }

    List<ActionRow> actionRowList = buttons.stream().map(row ->
        ActionRow.of(row.stream().map(e -> {
          ButtonStyle style = switch (e.color()) {
            case RED -> ButtonStyle.DANGER;
            case GREEN -> ButtonStyle.SUCCESS;
            case LINK -> ButtonStyle.LINK;
            case PRIMARY -> ButtonStyle.PRIMARY;
            case SECONDARY-> ButtonStyle.SECONDARY;
          };

          return Button.of(style, e.id(), e.value());
        }).collect(Collectors.toList()))
    ).collect(Collectors.toList());

    user.openPrivateChannel()
        .submit()
        .thenAccept(privateChannel -> privateChannel
            .sendMessage(content)
            .setComponents(actionRowList)
            .submit()
            .exceptionally(e -> {
              if (Settings.IMP.MAIN.DEBUG) {
                e.printStackTrace(); // printStackTrace is necessary there
              }
              return null;
            }))
        .exceptionally(e -> {
          if (Settings.IMP.MAIN.DEBUG) {
            e.printStackTrace(); // printStackTrace is necessary there
          }
          return null;
        });
  }

  public void sendMessage(Player player, String content, List<List<ButtonItem>> buttons, ButtonVisibility visibility) {
    this.sendMessage(player.getDiscordID(), content, buttons, visibility);
  }

  public boolean canSend(Player player) {
    return player.getDiscordID() != null;
  }


  public void broadcastMessage(Player player, String message, List<List<ButtonItem>> item) {
    this.broadcastMessage(player, message, item, ButtonVisibility.DEFAULT);
  }

  public void broadcastMessage(Player player, String message,
                               List<List<ButtonItem>> item, ButtonVisibility visibility) {
    sendMessage(player, message, item, visibility);
  }

  public void broadcastMessage(Long id, String message,
                               List<List<ButtonItem>> item) {
    sendMessage(id, message, item, ButtonVisibility.DEFAULT);
  }

  public void broadcastMessage(Player player, String message) {
    sendMessage(player, message, Collections.emptyList(), ButtonVisibility.DEFAULT);
  }

  public void broadcastMessage(Long id, String message) {
    sendMessage(id, message, Collections.emptyList(), ButtonVisibility.DEFAULT);
  }

  public void sendComponents(Long channelID, MessageEmbed embed, Collection<LayoutComponent> components){
    TextChannel textChannel = jda.getTextChannelById(channelID);
    textChannel
            .sendMessageEmbeds(embed)
            .addComponents(components)
            .queue();
  }

  public void sendModal(Long channelID, MessageEmbed embed){
    TextChannel textChannel = jda.getTextChannelById(channelID);
    textChannel.sendMessageEmbeds(embed).queue();
  }

  public void sendMessage(Long channelID, String content){
    TextChannel textChannel = jda.getTextChannelById(channelID);
    textChannel.sendMessage(content).queue();
  }
}