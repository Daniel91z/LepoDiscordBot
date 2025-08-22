package lepo.bot.slashcommand;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class SlashCommands extends ListenerAdapter {

    private final Map<String, Command> commandMap;

    public SlashCommands(Map<String, Command> commandMap) {
        this.commandMap = commandMap;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getGuild() == null)
            return;
        Command command = commandMap.get(event.getName());
        if (command != null) {
            command.execute(event);
        } else {
            event.reply("Comando non supportato al momento")
                    .setEphemeral(true)
                    .queue(interactionHook -> {
                        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
                        scheduler.schedule(() -> interactionHook.deleteOriginal().queue(), 3, TimeUnit.SECONDS);
                    });
        }
    }

}