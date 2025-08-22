package lepo.bot.slashcommand.play.autocomplete;


import lepo.bot.audio.AudioPlayerLoadResultHandler;
import lepo.bot.audio.AudioPlayerSendHandler;
import lepo.bot.filemanager.FileService;
import lepo.bot.slashcommand.Command;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Play an audio from audioFilesList
 */
@Slf4j
public class PlayAutocompleteCommand extends PlayAutocomplete implements Command {

    public static final String PLAY_AUDIO = "audio";
    public static final String PLAY_AUDIO_DESCRIPTION = "Riproduci un audio";
    private final FileService fileService;

    public PlayAutocompleteCommand(AudioPlayerLoadResultHandler audioPlayerLoadResultHandler, AudioPlayerSendHandler audioPlayerSendHandler, List<File> audioFilesList, FileService fileService) {
        super(audioPlayerLoadResultHandler, audioPlayerSendHandler, audioFilesList);
        this.fileService = fileService;
    }

    /**
     * Plays an audio file based on the provided command and event.
     *
     * <p>This method is triggered when a slash command interaction event occurs. It first checks if the event's
     * name matches the provided command name. If the names match, it attempts to join a voice channel if not
     * already connected. Then it retrieves the audio request from the event and performs several checks:
     * - Ensures the audio request is not empty.
     * - Verifies that the requested audio file exists.
     * - Confirms that the bot has successfully joined the voice channel and that playing the file is allowed.
     *
     * <p>If all conditions are met, the audio file is played. Otherwise, a message is sent indicating that the
     * requested audio could not be found.</p>
     *
     * @param commandName the name of the command that should trigger the audio playback
     * @param event       the slash command interaction event
     */
    @Override
    public void playAuto(String commandName, SlashCommandInteractionEvent event) {
        Optional.of(event)
                .filter(e -> e.getName().equals(commandName))
                .ifPresent(e -> {
                    boolean joined = joinVoiceChannelIfNotConnected(e);
                    getAudioRequest(e)
                            .filter(audioRequest -> !audioRequest.isEmpty())
                            .filter(this::doesFileExist)
                            .filter(audioRequest -> joined && isFileAllowedToPlay(e))
                            .ifPresentOrElse(
                                    audioRequest -> playAudio(audioRequest, e),
                                    () -> replyAudioNotFound(e)
                            );
                });
    }

    private Optional<String> getAudioRequest(SlashCommandInteractionEvent event) {
        return event.getOptions().stream()
                .findFirst()
                .map(OptionMapping::getAsString);
    }

    private boolean doesFileExist(String audioRequest) {
        return playAudioFilesList.stream()
                .anyMatch(file -> getFileNameWithoutExtension(file.getName()).equals(audioRequest));
    }

    private boolean isFileAllowedToPlay(SlashCommandInteractionEvent event) {
        return fileService.notContainedInFile(fileService.getChannelNameWithoutEmoji(event.getChannel().getName()));
    }

    private void playAudio(String audioRequest, SlashCommandInteractionEvent event) {
        log.info("Playing audio request: {}", audioRequest);
        String audioFilePath = playAudioFilesList.stream()
                .filter(file -> getFileNameWithoutExtension(file.getName()).equals(audioRequest))
                .map(File::getAbsolutePath)
                .findFirst()
                .orElse("AUDIO_NOT_FOUND");
        audioPlayerSendHandler.getTrackScheduler().nextTrack();
        audioPlayerSendHandler.getAudioPlayerManager().loadItem(audioFilePath, audioPlayerLoadResultHandler);
        audioPlayerSendHandler.getTrackScheduler().nextTrack();
        event.deferReply().setEphemeral(true).setContent("\uD83D\uDD0A " + audioRequest)
                .queue(setInteractionTimeout());
    }

    private void replyAudioNotFound(SlashCommandInteractionEvent event) {
        log.info("Can't play audio");
        event.reply("\u2620\uFE0F Questo audio non esiste \u2620\uFE0F")
                .setEphemeral(true)
                .queue(setInteractionTimeout());
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        log.info("{} starting SlashCommandInteractionEvent {}", getUserName(event), PLAY_AUDIO);
        this.playAuto(PLAY_AUDIO, event);
    }

    private Consumer<InteractionHook> setInteractionTimeout() {
        return interactionHook -> {
            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
            scheduler.schedule(() -> interactionHook.deleteOriginal().queue(), 6, TimeUnit.SECONDS);
        };
    }

    private boolean joinVoiceChannelIfNotConnected(SlashCommandInteractionEvent event) {
        AtomicBoolean joined = new AtomicBoolean(false);
        Optional.ofNullable(event.getGuild())
                .map(Guild::getAudioManager)
                .ifPresent(audioManager ->
                        Optional.ofNullable(event.getMember())
                                .map(Member::getVoiceState)
                                .map(GuildVoiceState::getChannel)
                                .filter(channel -> fileService.notContainedInFile(fileService.getChannelNameWithoutEmoji(channel.getName())))
                                .ifPresent(channel -> {
                                    audioManager.openAudioConnection(channel);
                                    audioManager.setSendingHandler(audioPlayerSendHandler);
                                    joined.set(true);
                                })
                );
        return joined.get();
    }

}