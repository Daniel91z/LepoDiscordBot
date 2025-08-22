package lepo.bot.slashcommand.play;


import lepo.bot.audio.AudioPlayerLoadResultHandler;
import lepo.bot.audio.AudioPlayerSendHandler;
import lepo.bot.event.buttoninteraction.Play;
import lepo.bot.filemanager.FileService;
import lepo.bot.slashcommand.Command;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.internal.interactions.component.ButtonImpl;

import java.io.File;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Play audio after fist 25
 */
@Slf4j
public class Play2 extends Play implements Command {

    public static final String PLAY2_COMMAND = "play2";
    public static final String PLAY2_DESCRIPTION = "Lista audio 2";
    public static final String PLAY2_HELP_DESCRIPTION = "Riproduci un audio da lista 2";

    public Play2(AudioPlayerLoadResultHandler audioPlayerLoadResultHandler, AudioPlayerSendHandler audioPlayerSendHandler, List<File> audioFilesList, FileService fileService) {
        super(audioPlayerLoadResultHandler, audioPlayerSendHandler, audioFilesList, fileService);
    }

    @Override
    public void play(String commandName, SlashCommandInteractionEvent event) {
        if (event.getName().equals(PLAY2_COMMAND)) {
            List<Button> audioButtonList =
                    playAudioFilesList.stream()
                            .skip(FIRST_25_AUDIO)
                            .map(file -> new ButtonImpl(getFileNameWithoutExtension(file.getName()), getFileNameWithoutExtension(file.getName()), ButtonStyle.PRIMARY, false, null))
                            .collect(Collectors.toList());
            event.reply(PLAY2_DESCRIPTION)
                    .addActionRow(audioButtonList.subList(0, 5))
                    .addActionRow(audioButtonList.subList(5, 10))
                    .addActionRow(audioButtonList.subList(10, 15))
                    .addActionRow(audioButtonList.subList(15, 20))
                    .addActionRow(audioButtonList.subList(20, 25))
                    .setEphemeral(true)
                    .queue(interactionHook -> {
                        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
                        scheduler.schedule(() -> interactionHook.deleteOriginal().queue(), DELAY_DELETE_MESSAGE, TimeUnit.HOURS);
                    });
        }
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        log.info("{} starting SlashCommandInteractionEvent {}", getUserName(event), PLAY2_COMMAND);
        this.play(PLAY2_COMMAND, event);
    }

}