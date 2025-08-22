package lepo.bot.event.guildvoiceupdate;

import lepo.bot.audio.AudioFiles;
import lepo.bot.audio.AudioPlayerLoadResultHandler;
import lepo.bot.audio.AudioPlayerSendHandler;
import lepo.bot.filemanager.FileService;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.managers.AudioManager;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
public class GuildVoiceUpdateEventListener extends ListenerAdapter {

    private final Random rng;
    private final List<File> welcomeAudioFiles;
    private final List<File> goodbyeAudioFiles;
    private final List<File> customAudioFiles;
    private final ScheduledExecutorService scheduler;
    private final AudioPlayerSendHandler audioPlayerSendHandler;
    private final AudioPlayerLoadResultHandler audioPlayerLoadResultHandler;
    private final FileService fileService;
    //ID of a voice channel the bot must not join
    private static final String FORBIDDEN_VC_ID = "1247215648862502925";


    public GuildVoiceUpdateEventListener(AudioFiles audioFiles, ScheduledExecutorService scheduler, AudioPlayerSendHandler audioPlayerSendHandler, AudioPlayerLoadResultHandler audioPlayerLoadResultHandler, FileService fileService) {
        this.customAudioFiles = audioFiles.getCustomAudioFileList();
        this.fileService = fileService;
        this.rng = new Random();
        this.welcomeAudioFiles = audioFiles.getWelcomeAudioFileList();
        this.goodbyeAudioFiles = audioFiles.getGoodbyeAudioFileList();
        this.scheduler = scheduler;
        this.audioPlayerSendHandler = audioPlayerSendHandler;
        this.audioPlayerLoadResultHandler = audioPlayerLoadResultHandler;
    }

    /**
     * Entry point for guild voice state changes. Evaluates join/move/leave events
     * for members and coordinates connecting/disconnecting the bot and playing audio.
     *
     * @param event the GuildVoiceUpdateEvent provided by JDA
     */
    @Override
    public void onGuildVoiceUpdate(@NotNull GuildVoiceUpdateEvent event) {
        Member member = event.getMember();
        AudioChannelUnion joinedChannelUnion = event.getChannelJoined();
        AudioChannelUnion leftChannelUnion = event.getChannelLeft();
        AudioManager audioManager = event.getGuild().getAudioManager();
        audioManager.setSelfDeafened(true);
        handleMemberJoin(joinedChannelUnion, member, audioManager);
        handleMemberMove(joinedChannelUnion, leftChannelUnion, member, audioManager, event);
        handleMemberLeft(leftChannelUnion, joinedChannelUnion, audioManager, event);
        checkIfBotCantConnectToNewChannelThenDisconnect(event, joinedChannelUnion, leftChannelUnion, audioManager);
        onlyBotsArePresent(joinedChannelUnion, leftChannelUnion, audioManager);
    }

    /**
     * Handles a member joining a voice channel: if the channel is allowed and the bot
     * is not already connected, open an audio connection and set the send handler.
     *
     * @param joinedChannel the channel the member joined (may be null)
     * @param member        the member who joined
     * @param audioManager  the guild audio manager to control connections
     */
    private void handleMemberJoin(AudioChannelUnion joinedChannel, Member member, AudioManager audioManager) {
        Optional.ofNullable(joinedChannel)
                .filter(joinEvent -> fileService.notContainedInFile(fileService.getChannelNameWithoutEmoji(joinedChannel.getName())))
                .map(channel -> {
                    log.info("[JOIN EVENT] - {} joined {}", member.getUser().getName(), fileService.getChannelNameWithoutEmoji(channel.getName()));
                    return !audioManager.isConnected() ? channel : null;
                }).ifPresent(
                        channel -> {
                            log.info("[JOIN EVENT] - Bot joined {}", fileService.getChannelNameWithoutEmoji(channel.getName()));
                            audioManager.openAudioConnection(channel);
                            audioManager.setSendingHandler(audioPlayerSendHandler);
                        }
                );
    }

    /**
     * Handles complex move scenarios (member moved between channels). Plays welcome
     * or goodbye audio as appropriate, manages the track scheduler, and moves the bot
     * if necessary while avoiding forbidden channels.
     *
     * @param joinedChannel the channel the member joined (may be null)
     * @param leftChannel   the channel the member left (may be null)
     * @param member        the member who moved
     * @param audioManager  the guild audio manager to control connections
     * @param event         the original GuildVoiceUpdateEvent for additional context
     */
    private void handleMemberMove(AudioChannelUnion joinedChannel, AudioChannelUnion leftChannel, Member member, AudioManager audioManager, GuildVoiceUpdateEvent event) {

        Optional.ofNullable(joinedChannel)
                .filter(vc -> !FORBIDDEN_VC_ID.equals(vc.getId()))
                .filter(joinEvent -> fileService.notContainedInFile(fileService.getChannelNameWithoutEmoji(joinedChannel.getName())))
                .filter(channel -> !Objects.equals(channel, leftChannel))
                .map(channel -> {
                    var movedChannel = audioManager.isConnected() && !Objects.equals(audioManager.getConnectedChannel(), channel) ? channel : null;
                    Optional.ofNullable(leftChannel)
                            .ifPresentOrElse(noLeftChannel -> {
                                log.info("[MOVE EVENT] - clean queue and say hello");
                                audioPlayerSendHandler.getTrackScheduler().clearQueueAndStop();
                                customSayHello(member.getEffectiveName());
                                playRandomAudio(welcomeAudioFiles, fileService.getChannelNameWithoutEmoji(joinedChannel.getName()));
                            }, () -> {
                                audioPlayerSendHandler.getTrackScheduler().nextTrack();
                                customSayHello(member.getEffectiveName());
                                playRandomAudio(welcomeAudioFiles, fileService.getChannelNameWithoutEmoji(joinedChannel.getName()));
                                audioPlayerSendHandler.getTrackScheduler().nextTrack();
                            });
                    return movedChannel;
                })
                .ifPresent(channel -> {
                    log.info("[MOVE EVENT] - Bot moved to new voice channel");
                    audioPlayerSendHandler.getTrackScheduler().clearQueueAndStop();
                    audioManager.closeAudioConnection();
                    scheduler.schedule(() -> audioManager.openAudioConnection(channel), 100, TimeUnit.MILLISECONDS);
                });

        //Someone left play goodbye
        Optional.ofNullable(leftChannel)
                .filter(vc -> !FORBIDDEN_VC_ID.equals(vc.getId()))
                .filter(channel -> Objects.isNull(joinedChannel))
                .map(AudioChannelUnion::getName).flatMap(channelWhereUserIsConnected -> Optional.ofNullable(event.getGuild().getSelfMember().getVoiceState())
                        .map(GuildVoiceState::getChannel)
                        .filter(e -> e.getMembers().size() > 1)
                        .map(AudioChannelUnion::getName)
                        .filter(channelWhereUserIsConnected::equals)
                        .filter(channel -> !member.getUser().isBot()))
                .ifPresent(channel -> {
                    log.info("[MOVE EVENT] - {} left, play a random goodbye sound", member.getUser().getName());
                    scheduler.schedule(() -> playRandomAudio(goodbyeAudioFiles, ""), 200, TimeUnit.MILLISECONDS);
                });

        //Move event where bot cant join forbidden channel and it's alone in a previous one
        Optional.ofNullable(leftChannel)
                .filter(lc -> lc.getMembers().size() == 1)
                .filter(lc -> lc.getMembers().stream().allMatch(checkMember -> checkMember.getUser().isBot()))
                .flatMap(lc -> Optional.ofNullable(joinedChannel)
                        .filter(jc -> FORBIDDEN_VC_ID.equals(jc.getId())))
                .ifPresent(jc -> {
                    log.info("[LEFT EVENT] - Bot is alone in voice chat and cant connect to {}", jc.getName());
                    audioPlayerSendHandler.getTrackScheduler().clearQueueAndStop();
                    audioManager.closeAudioConnection();
                });

    }

    /**
     * Handles a member leaving a channel: if the bot was the only remaining member,
     * clear audio queue and disconnect the bot from the voice channel.
     *
     * @param leftChannelUnion   the channel the member left
     * @param joinedChannelUnion the channel the member joined (if move; otherwise null)
     * @param audioManager       the guild audio manager to control connections
     * @param event              the original GuildVoiceUpdateEvent for context
     */
    private void handleMemberLeft(AudioChannelUnion leftChannelUnion, AudioChannelUnion joinedChannelUnion, AudioManager audioManager, GuildVoiceUpdateEvent event) {
        Optional.ofNullable(leftChannelUnion)
                .filter(isMove -> Objects.isNull(joinedChannelUnion))
                .filter(channel -> channel.getMembers().size() == 1 && channel.getMembers().get(0).equals(event.getGuild().getSelfMember()))
                .ifPresent(channel -> {
                    log.info("[LEFT EVENT] - Bot disconnected from voice channel because it was the only member");
                    audioPlayerSendHandler.getTrackScheduler().clearQueueAndStop();
                    audioManager.closeAudioConnection();
                });
    }

    /**
     * Checks whether, after a leave event, only bots remain in the channel (and the
     * channel is not forbidden). If so, clears the queue and disconnects the bot.
     *
     * @param joinedChannel the channel a user joined (may be null)
     * @param leftChannel   the channel a user left
     * @param audioManager  the guild audio manager to control connections
     */
    private void onlyBotsArePresent(AudioChannelUnion joinedChannel, AudioChannelUnion leftChannel, AudioManager audioManager) {
        Optional.ofNullable(leftChannel)
                .filter(channel -> Objects.isNull(joinedChannel))
                .filter(channel -> channel.getMembers().stream().allMatch(member -> member.getUser().isBot()))
                .filter(channel -> channel.getMembers().size() > 1)
                .filter(vc -> !FORBIDDEN_VC_ID.equals(vc.getId()))
                .ifPresent(channel -> {
                    log.info("[LEFT EVENT] - Bot is alone in voice chat with other bots and should disconnected");
                    audioPlayerSendHandler.getTrackScheduler().clearQueueAndStop();
                    audioManager.closeAudioConnection();
                });

    }

    /**
     * Detects situations where the bot is left alone in the old channel and cannot
     * connect to the new (forbidden) channel; closes the audio connection if detected.
     *
     * @param event              the GuildVoiceUpdateEvent triggering the check
     * @param joinedChannelUnion the channel the member joined (may be null)
     * @param leftChannelUnion   the channel the member left (may be null)
     * @param audioManager       the guild audio manager to control connections
     */
    private void checkIfBotCantConnectToNewChannelThenDisconnect(GuildVoiceUpdateEvent event, AudioChannelUnion joinedChannelUnion, AudioChannelUnion leftChannelUnion, AudioManager audioManager) {
        Optional.ofNullable(leftChannelUnion)
                .filter(channel -> Objects.nonNull(event.getChannelLeft()))
                .map(channel -> channel.getMembers().stream()
                        .findFirst()
                        .map(member -> member.getUser().isBot())
                        .orElse(false)
                        && channel.getMembers().size() == 1)
                .filter(isBotAndSizeIsOne -> isBotAndSizeIsOne)
                .flatMap(channel -> Optional.ofNullable(joinedChannelUnion)
                        .filter(vc -> !FORBIDDEN_VC_ID.equals(vc.getId()))
                        .map(Channel::getName)
                        .map(fileService::getChannelNameWithoutEmoji)
                        .filter(channelName -> !fileService.notContainedInFile(channelName)))
                .ifPresent(contained -> {
                    audioManager.closeAudioConnection();
                    log.info("[BOT] - Bot cant connect to the new channel and is alone in the old one, disconnecting");
                });
    }

    //Standard Hello
   /*private void sayHello(String user) {
        log.info("[BOT] - Hello {}!", user);
        String encodedUser = encodeURL(user);
        String identifier = String.format("ftts://%s", encodedUser);
        audioPlayerSendHandler.getAudioPlayerManager().loadItem(identifier, audioPlayerLoadResultHandler);
    }*/

    /**
     * Schedules and triggers a text-to-speech greeting for a given username by
     * constructing a TTS identifier (with a randomized suffix) and loading it via
     * the audio player manager.
     *
     * @param user the display name of the user to greet
     */
    private void customSayHello(String user) {

        scheduler.schedule(() -> log.info("Waiting 500ms to say hello"), 500, TimeUnit.MILLISECONDS);

        log.info("[BOT] - Hello {}!", user);
        /*String identifier = "";
        if (user.equalsIgnoreCase("The Honored One")) {
            log.info("[BOT] - Custom hello to The Honored One");
            identifier = String.format("ftts://%s?speed=0.7&voice=9fdbf949-6112-5b46-972e-0f0ba15d039e", encodeURL(user)); //C3PO
        } else if (user.equalsIgnoreCase("Ianfire")) {
            log.info("[BOT] - Custom hello to Ianfire");
            identifier = String.format("ftts://%s?speed=0.8&voice=ad56ba82-23f9-5042-b132-3a1848e1788ebe296e7604b", user);
        } else {
            identifier = String.format("ftts://%s", user);
        }*/

        //Forced jap voice
        String identifier = String.format("ftts://%s", urlEncodeTex(user));
        audioPlayerSendHandler.getAudioPlayerManager().loadItem(identifier, audioPlayerLoadResultHandler);
    }

    /**
     * Percent-encodes each character of the input string for safe inclusion in a URL,
     * using UTF-8 and replacing spaces with %20.
     *
     * @param input the raw string to encode
     * @return the URL-encoded representation of the input
     */
    private String urlEncodeTex(String input) {
        return input.chars()
                .mapToObj(c -> (char) c)
                .map(ch -> ch == ' ' ? "%20" : URLEncoder.encode(String.valueOf(ch), StandardCharsets.UTF_8))
                .collect(Collectors.joining());
    }

    /**
     * Selects and plays a random audio file from the provided list, optionally
     * choosing a custom audio pool based on keywords extracted from the voice channel name.
     *
     * @param audio            the fallback list of audio files to choose from
     * @param voiceChannelName the (normalized) name of the voice channel used to pick filters
     */
    private void playRandomAudio(List<File> audio, String voiceChannelName) {
        Map<String, Predicate<File>> filters = Map.of(
                "LOVEEEEEE", file -> file.getName().toUpperCase().contains("LOVEEEEEE")
                //"DALARAN", file -> file.getName().toUpperCase().contains("DALARAN")
                //"CINEMA", file -> file.getName().toUpperCase().contains("CINEMA")
        );
        Optional<String> filterKeyword = filters.keySet().stream()
                .filter(keyword -> voiceChannelName.toUpperCase().contains(keyword))
                .findFirst();
        var audioSelected = filterKeyword
                .map(keyword -> new ArrayList<>(customAudioFiles))
                .orElse((ArrayList<File>) audio);
        audioPlayerSendHandler.getAudioPlayerManager().loadItem(getRandomAudioFilePath(audioSelected), audioPlayerLoadResultHandler);
        log.info("[BOT] - Audio list selected is {}", filterKeyword.orElse("the default one"));
    }

    private String getRandomAudioFilePath(List<File> audioFiles) {
        int randomIndex = rng.nextInt(audioFiles.size());
        return audioFiles.get(randomIndex).getAbsolutePath();
    }

}