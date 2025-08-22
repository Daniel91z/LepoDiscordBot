package lepo.bot.event.buttoninteraction;

import lepo.bot.audio.AudioPlayerLoadResultHandler;
import lepo.bot.audio.AudioPlayerSendHandler;
import lepo.bot.filemanager.FileService;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.internal.interactions.CommandDataImpl;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@Slf4j
public abstract class Play extends ListenerAdapter {

    protected final AudioPlayerLoadResultHandler audioPlayerLoadResultHandler;
    protected final AudioPlayerSendHandler audioPlayerSendHandler;
    protected final List<File> playAudioFilesList;
    private final FileService fileService;
    protected static final int FIRST_25_AUDIO = 25;
    protected static final int SECOND_25_AUDIO = 25;
    protected static final int DELAY_DELETE_MESSAGE = 6;

    protected Play(AudioPlayerLoadResultHandler audioPlayerLoadResultHandler, AudioPlayerSendHandler audioPlayerSendHandler, List<File> playAudioFilesList, FileService fileService) {
        this.audioPlayerLoadResultHandler = audioPlayerLoadResultHandler;
        this.audioPlayerSendHandler = audioPlayerSendHandler;
        this.playAudioFilesList = playAudioFilesList;
        this.fileService = fileService;
    }

    public SlashCommandData play(String commandName, String description) {
        log.info("Loading command {}", commandName);
        return new CommandDataImpl(commandName, description).setGuildOnly(true);
    }

    protected void play(String commandName, SlashCommandInteractionEvent event) {
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        boolean joined = joinVoiceChannelIfNotConnected(event);
        if (event.isAcknowledged()) {
            log.info("Interaction already acknowledged.");
            return;
        }
        String name = getUserName(event);
        log.info("{} pushed button {}", name, event.getComponentId());
        var audioFilePath = playAudioFilesList.stream()
                .filter(file -> getFileNameWithoutExtension(file.getName()).equals(event.getComponentId()))
                .map(File::getAbsolutePath)
                .findFirst()
                .orElse("AUDIO_NOT_FOUND");
        if (joined && fileService.notContainedInFile(fileService.getChannelNameWithoutEmoji(event.getChannel().getName()))) {
            log.info("Playing audio");
            audioPlayerSendHandler.getTrackScheduler().nextTrack();
            audioPlayerSendHandler.getAudioPlayerManager().loadItem(audioFilePath, audioPlayerLoadResultHandler);
            audioPlayerSendHandler.getTrackScheduler().nextTrack();
            event.deferReply().setEphemeral(true).setContent("\uD83D\uDD0A " + event.getComponentId())
                    .queue(setInteractionTimeout());
        } else {
            log.info("Cant Play audio");
            event.reply("\u2620\uFE0F  Non posso riprodurre audio qui  \u2620\uFE0F")
                    .setEphemeral(true)
                    .queue(setInteractionTimeout());
        }
    }

    private Consumer<InteractionHook> setInteractionTimeout() {
        return interactionHook -> {
            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
            scheduler.schedule(() -> interactionHook.deleteOriginal().queue(), 6, TimeUnit.SECONDS);
        };
    }

    protected String getFileNameWithoutExtension(String input) {
        return input.substring(0, input.indexOf('.'));
    }

    protected String getUserName(ButtonInteractionEvent event) {
        return Objects.nonNull(event.getMember()) ? event.getMember().getUser().getName() : "USER_NOT_FOUND";
    }

    protected String getUserName(SlashCommandInteractionEvent event) {
        return Objects.nonNull(event.getMember()) ? event.getMember().getUser().getName() : "USER_NOT_FOUND";
    }

    private boolean joinVoiceChannelIfNotConnected(ButtonInteractionEvent event) {
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