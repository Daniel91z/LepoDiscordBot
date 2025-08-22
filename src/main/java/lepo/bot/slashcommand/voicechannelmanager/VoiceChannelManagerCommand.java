package lepo.bot.slashcommand.voicechannelmanager;


import lepo.bot.filemanager.FileService;
import lepo.bot.slashcommand.Command;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.util.Objects;


@Slf4j
public class VoiceChannelManagerCommand extends VoiceChannelManagerListenerAdapter implements Command {

    public static final String VOICE_CHANNEL_MANAGER_COMMAND = "manager";
    public static final String VOICE_CHANNEL_MANAGER_DESCRIPTION = "Gestore canali vocali (solo Admin)";
    public static final String VOICE_CHANNEL_MANAGER_HELP_DESCRIPTION = "Decidi in quali canali il bot puo' collegarsi, non si colleghera' nei canali contrassegnati con " + RED_SIGN;

    public VoiceChannelManagerCommand(FileService fileService, String adminId, String powerUserId) {
        super(fileService, adminId, powerUserId);
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        log.info("{} starting SlashCommandInteractionEvent {}", getUserName(event), VOICE_CHANNEL_MANAGER_COMMAND);
    }

    protected String getUserName(SlashCommandInteractionEvent event) {
        return Objects.nonNull(event.getMember()) ? event.getMember().getUser().getName() : "USER_NOT_FOUND";
    }

}