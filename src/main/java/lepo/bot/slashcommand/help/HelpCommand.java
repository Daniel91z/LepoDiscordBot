package lepo.bot.slashcommand.help;


import lepo.bot.slashcommand.Command;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.util.Map;
import java.util.Objects;

@Slf4j
public class HelpCommand extends HelpAdapter implements Command {

    public static final String HELP_COMMAND = "help";
    public static final String HELP_DESCRIPTION = "Lista dei comandi disponibili";

    public HelpCommand(Map<String, String> helpCommands) {
        super(helpCommands);
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        log.info("{} starting SlashCommandInteractionEvent {}", getUserName(event), HELP_COMMAND);
        this.help(HELP_COMMAND, HELP_DESCRIPTION);
    }

    protected String getUserName(SlashCommandInteractionEvent event) {
        return Objects.nonNull(event.getMember()) ? event.getMember().getUser().getName() : "USER_NOT_FOUND";
    }

}