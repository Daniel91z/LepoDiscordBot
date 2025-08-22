package lepo.bot.event.guildvoiceupdate;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@Slf4j
public class SkipBotEvent implements EventListener {

    private final EventListener wrappedListener;

    public SkipBotEvent(EventListener wrappedListener) {
        this.wrappedListener = wrappedListener;
    }

    @Override
    public void onEvent(@NotNull GenericEvent event) {
        Optional.of(event)
                .filter(GuildVoiceUpdateEvent.class::isInstance)
                .map(GuildVoiceUpdateEvent.class::cast)
                .filter(voiceUpdateEvent -> !voiceUpdateEvent.getMember().getUser().isBot())
                .ifPresent(voiceUpdateEvent -> {
                    log.info("Non-Bot GuildVoiceUpdateEvent detected");
                    wrappedListener.onEvent(event);
                });
    }

}