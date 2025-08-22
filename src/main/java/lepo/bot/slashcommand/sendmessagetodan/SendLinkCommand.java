package lepo.bot.slashcommand.sendmessagetodan;


import lepo.bot.slashcommand.Command;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.util.Objects;

@Slf4j
public class SendLinkCommand extends SendLinkAdapter implements Command {

    public static final String SEND_LINK_COMMAND = "invia";
    public static final String SEND_LINK_DESCRIPTION = "Proponi un audio inviando un link";

    public SendLinkCommand(String adminId) {
        super(adminId);
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        log.info("{} starting SlashCommandInteractionEvent {}", getUserName(event), SEND_LINK_COMMAND);
    }

    protected String getUserName(SlashCommandInteractionEvent event) {
        return Objects.nonNull(event.getMember()) ? event.getMember().getUser().getName() : "USER_NOT_FOUND";
    }

}