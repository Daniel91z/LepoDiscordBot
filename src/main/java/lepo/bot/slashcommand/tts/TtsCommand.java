package lepo.bot.slashcommand.tts;


import lepo.bot.audio.AudioPlayerLoadResultHandler;
import lepo.bot.audio.AudioPlayerSendHandler;
import lepo.bot.filemanager.FileService;
import lepo.bot.slashcommand.Command;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.util.Objects;

@Slf4j
public class TtsCommand extends TtsAdapter implements Command {

    public static final String TTS_COMMAND = "tts";
    public static final String TTS_DESCRIPTION = "Text-to-Speech";

    public TtsCommand(AudioPlayerSendHandler audioPlayerSendHandler, AudioPlayerLoadResultHandler audioPlayerLoadResultHandler, FileService fileService) {
        super(audioPlayerSendHandler, audioPlayerLoadResultHandler, fileService);
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        log.info("{} starting SlashCommandInteractionEvent {}", getUserName(event), TTS_COMMAND);
    }

    protected String getUserName(SlashCommandInteractionEvent event) {
        return Objects.nonNull(event.getMember()) ? event.getMember().getUser().getName() : "USER_NOT_FOUND";
    }

}