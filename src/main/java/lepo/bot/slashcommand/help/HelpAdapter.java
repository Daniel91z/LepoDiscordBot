package lepo.bot.slashcommand.help;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.internal.interactions.CommandDataImpl;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static lepo.bot.slashcommand.help.HelpCommand.HELP_COMMAND;


@Component
@Slf4j
public abstract class HelpAdapter extends ListenerAdapter {

    private final Map<String, String> helpCommands;

    public HelpAdapter(Map<String, String> helpCommands) {
        this.helpCommands = helpCommands;
    }

    public SlashCommandData help(String commandName, String description) {
        log.info("Loading command {}", commandName);
        return new CommandDataImpl(commandName, description)
                .setGuildOnly(true);
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getName().equals(HELP_COMMAND)) {
            String helpString = helpCommands.entrySet().stream()
                    .filter(entry -> !entry.getKey().equals(HELP_COMMAND))
                    .map(entry -> "- " + entry.getKey().toUpperCase() + ": " + entry.getValue() + "\n")
                    .collect(Collectors.joining());
            event.reply(helpString)
                    .setEphemeral(true)
                    .queue(interactionHook -> {
                        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
                        scheduler.schedule(() -> interactionHook.deleteOriginal().queue(), 1, TimeUnit.MINUTES);
                    });
        }
    }

}