package lepo.bot.slashcommand.tts;

import lepo.bot.audio.AudioPlayerLoadResultHandler;
import lepo.bot.audio.AudioPlayerSendHandler;
import lepo.bot.filemanager.FileService;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.internal.interactions.CommandDataImpl;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static lepo.bot.slashcommand.tts.TtsCommand.TTS_COMMAND;
import static lepo.bot.slashcommand.tts.TtsCommand.TTS_DESCRIPTION;

@Slf4j
@Component
public abstract class TtsAdapter extends ListenerAdapter {

    private final AudioPlayerSendHandler audioPlayerSendHandler;
    private final AudioPlayerLoadResultHandler audioPlayerLoadResultHandler;
    private final FileService fileService;
    private static final String TESTO = "testo";

    protected TtsAdapter(AudioPlayerSendHandler audioPlayerSendHandler, AudioPlayerLoadResultHandler audioPlayerLoadResultHandler, FileService fileService) {
        this.audioPlayerSendHandler = audioPlayerSendHandler;
        this.audioPlayerLoadResultHandler = audioPlayerLoadResultHandler;
        this.fileService = fileService;
    }

    public SlashCommandData tts(String commandName, String description) {
        log.info("Loading command {}", commandName);
        return new CommandDataImpl(commandName, description)
                .addOptions(new OptionData(OptionType.STRING, TESTO, TTS_DESCRIPTION, true))
                .setGuildOnly(true);
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals(TTS_COMMAND)) {
            return;
        }
        boolean joinedVoiceChannel = joinVoiceChannelIfNotConnected(event);
        if (joinedVoiceChannel) {
            String inputMessage = Optional.ofNullable(event.getOption(TESTO))
                    .map(OptionMapping::getAsString)
                    .orElse("");
            log.info("Input message for TTS: {}", inputMessage);
            try {
                String encodedString = encodeURL(inputMessage);
                log.info("Encoded input message: {}", encodedString);
                event.reply("Messaggio elaborato correttamente")
                        .setEphemeral(true)
                        .queue(interactionHook -> {
                            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
                            scheduler.schedule(() -> {
                                String ttsUrl = String.format("ftts://%s", encodedString);
                                audioPlayerSendHandler.getAudioPlayerManager()
                                        .loadItem(ttsUrl, audioPlayerLoadResultHandler);
                            }, 0, TimeUnit.MILLISECONDS);
                            scheduler.schedule(() -> interactionHook.deleteOriginal().queue(), 3, TimeUnit.SECONDS);
                        });
            } catch (Exception e) {
                event.reply("Prova di nuovo più tardi")
                        .setEphemeral(true)
                        .queue(setInteractionTimeout());
            }
        } else {
            event.reply("\u2620\uFE0F  Non posso riprodurre audio qui  \u2620\uFE0F")
                    .setEphemeral(true)
                    .queue(setInteractionTimeout());
        }
    }

    private String encodeURL(String input) {
        return input.chars()
                .mapToObj(c -> (char) c)
                .map(ch -> ch == ' ' ? "%20" : URLEncoder.encode(String.valueOf(ch), StandardCharsets.UTF_8))
                .collect(Collectors.joining());
    }

    private Consumer<InteractionHook> setInteractionTimeout() {
        return interactionHook -> {
            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
            scheduler.schedule(() -> interactionHook.deleteOriginal().queue(), 3, TimeUnit.SECONDS);
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