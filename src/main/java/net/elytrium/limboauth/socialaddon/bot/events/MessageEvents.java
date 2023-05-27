package net.elytrium.limboauth.socialaddon.bot.events;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class MessageEvents extends ListenerAdapter {
    public static long channel = 0;
    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        super.onMessageReceived(event);
        if(event.isWebhookMessage() && event.getChannel().getIdLong() == channel){

        }
    }
}
