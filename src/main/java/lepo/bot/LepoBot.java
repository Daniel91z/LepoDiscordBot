package lepo.bot;


import lepo.bot.audio.AudioFiles;
import lepo.bot.audio.AudioPlayerLoadResultHandler;
import lepo.bot.audio.AudioPlayerSendHandler;
import lepo.bot.event.guildvoiceupdate.GuildVoiceUpdateEventListener;
import lepo.bot.event.guildvoiceupdate.SkipBotEvent;
import lepo.bot.filemanager.FileService;
import lepo.bot.slashcommand.Command;
import lepo.bot.slashcommand.SlashCommands;
import lepo.bot.slashcommand.help.HelpCommand;
import lepo.bot.slashcommand.play.Play1;
import lepo.bot.slashcommand.play.Play2;
import lepo.bot.slashcommand.play.Play3;
import lepo.bot.slashcommand.play.autocomplete.PlayAutocompleteCommand;
import lepo.bot.slashcommand.sendmessagetodan.SendLinkCommand;
import lepo.bot.slashcommand.tts.TtsCommand;
import lepo.bot.slashcommand.voicechannelmanager.VoiceChannelManagerCommand;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static lepo.bot.slashcommand.help.HelpCommand.HELP_COMMAND;
import static lepo.bot.slashcommand.help.HelpCommand.HELP_DESCRIPTION;
import static lepo.bot.slashcommand.play.Play1.*;
import static lepo.bot.slashcommand.play.Play2.*;
import static lepo.bot.slashcommand.play.Play3.*;
import static lepo.bot.slashcommand.play.autocomplete.PlayAutocompleteCommand.PLAY_AUDIO;
import static lepo.bot.slashcommand.play.autocomplete.PlayAutocompleteCommand.PLAY_AUDIO_DESCRIPTION;
import static lepo.bot.slashcommand.sendmessagetodan.SendLinkCommand.SEND_LINK_COMMAND;
import static lepo.bot.slashcommand.sendmessagetodan.SendLinkCommand.SEND_LINK_DESCRIPTION;
import static lepo.bot.slashcommand.tts.TtsCommand.TTS_COMMAND;
import static lepo.bot.slashcommand.tts.TtsCommand.TTS_DESCRIPTION;
import static lepo.bot.slashcommand.voicechannelmanager.VoiceChannelManagerCommand.*;

@Slf4j
@Component
@PropertySource("classpath:application-${spring.profiles.active}.properties")
public class LepoBot extends ListenerAdapter {

    @Value("${bot.token}")
    private String token;
    @Value("${bot.admin.id}")
    private String adminId;
    @Value("${bot.poweruser.id}")
    private String powerUserId;
    @Value("${bot.audio.path}")
    private String path;
    @Value("${bot.welcomeAudio.path}")
    private String welcomeAudioPath;
    @Value("${bot.goodbyeAudio.path}")
    private String goodbyeAudioPath;
    @Value("${bot.customAudio.path}")
    private String customAudioPath;
    @Value("${bot.volume.directory}")
    private String volumeDirectory;

    private JDA jda;
    private ScheduledExecutorService scheduler;
    private AudioPlayerSendHandler audioPlayerSendHandler;
    private AudioPlayerLoadResultHandler audioPlayerLoadResultHandler;
    private AudioFiles audioFiles;
    private PlayAutocompleteCommand playAutoCommand;
    private Play1 play1;
    private Play2 play2;
    private Play3 play3;
    private VoiceChannelManagerCommand voiceChannelManagerAdapterCommand;
    private SendLinkCommand sendLinkCommand;
    private Map<String, Command> commandMap;
    private FileService fileService;
    private HelpCommand helpCommand;
    private TtsCommand ttsCommand;

    @Bean
    public Path pathAndFileName() {
        return Path.of(volumeDirectory, "excluded-voice-channels.txt");
    }

    @PostConstruct
    public void init() {
        log.info("Initializing LepoBot");
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.audioPlayerSendHandler = new AudioPlayerSendHandler();
        this.audioPlayerLoadResultHandler = new AudioPlayerLoadResultHandler(audioPlayerSendHandler);
        this.audioFiles = new AudioFiles(path, welcomeAudioPath, goodbyeAudioPath, customAudioPath);
        this.playAutoCommand = new PlayAutocompleteCommand(audioPlayerLoadResultHandler, audioPlayerSendHandler, audioFiles.getPlayAudioFileList(), fileService);
        this.play1 = new Play1(audioPlayerLoadResultHandler, audioPlayerSendHandler, audioFiles.getPlayAudioFileList(), fileService);
        this.play2 = new Play2(audioPlayerLoadResultHandler, audioPlayerSendHandler, audioFiles.getPlayAudioFileList(), fileService);
        this.play3 = new Play3(audioPlayerLoadResultHandler, audioPlayerSendHandler, audioFiles.getPlayAudioFileList(), fileService);
        this.fileService = new FileService(pathAndFileName());
        this.voiceChannelManagerAdapterCommand = new VoiceChannelManagerCommand(fileService, adminId, powerUserId);
        this.commandMap = setCommandsMap();
        this.sendLinkCommand = new SendLinkCommand(adminId);
        this.helpCommand = new HelpCommand(helpCommands());

        this.ttsCommand = new TtsCommand(audioPlayerSendHandler, audioPlayerLoadResultHandler, fileService);
        //setup bot
        this.jda = createLepoBot();
        setCommandsInJda(this.jda);
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        disconnectIfAloneInVoiceChannel();
    }


    /**
     * scheduler: To manage connection delays.
     * audioPlayerSendHandler: Manager for the audio player.
     * audioPlayerLoadResultHandler: Used for playing audio files.
     * audioFiles: Defines the list of audio files used by the bot.
     * setCommands: to set slash commands
     *
     * @return An instance of JDA configured for the Lepo bot.
     */
    private JDA createLepoBot() {
        log.info("createLepoBot method");
        return JDABuilder
                .create(token, GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_VOICE_STATES)
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .enableCache(CacheFlag.VOICE_STATE)
                //.setAutoReconnect(false) //testing with this disabled
                //.setStatus(OnlineStatus.DO_NOT_DISTURB)
                .addEventListeners(new SkipBotEvent(new GuildVoiceUpdateEventListener(audioFiles, scheduler, audioPlayerSendHandler, audioPlayerLoadResultHandler, fileService)))
                .addEventListeners(new SlashCommands(commandMap))
                .addEventListeners(new PlayAutocompleteCommand(audioPlayerLoadResultHandler, audioPlayerSendHandler, audioFiles.getPlayAudioFileList(), fileService))
                .addEventListeners(new Play1(audioPlayerLoadResultHandler, audioPlayerSendHandler, audioFiles.getPlayAudioFileList(), fileService))
                .addEventListeners(new Play2(audioPlayerLoadResultHandler, audioPlayerSendHandler, audioFiles.getPlayAudioFileList(), fileService))
                .addEventListeners(new Play3(audioPlayerLoadResultHandler, audioPlayerSendHandler, audioFiles.getPlayAudioFileList(), fileService))
                .addEventListeners(new VoiceChannelManagerCommand(fileService, adminId, powerUserId))
                .addEventListeners(new SendLinkCommand(adminId))
                .addEventListeners(new HelpCommand(helpCommands()))
                .addEventListeners(new TtsCommand(audioPlayerSendHandler, audioPlayerLoadResultHandler, fileService))
                //zucca: "\uD83C\uDF83/help\uD83C\uDF83"
                //babbo natale: "\uD83C\uDF85/help\uD83C\uDF85"
                .setActivity(Activity.customStatus("/help"))
                .build();
    }

    /**
     * It takes some time to update or add a command.
     * Reboot discord if a new command does not register
     * Override all existing commands.
     *
     * @param jda as defined before
     */
    private void setCommandsInJda(JDA jda) {
        CommandListUpdateAction commands = jda.updateCommands()
                .addCommands(playAutoCommand.playAuto(PLAY_AUDIO, PLAY_AUDIO_DESCRIPTION))
                .addCommands(play1.play(PLAY1_COMMAND, PLAY1_DESCRIPTION))
                .addCommands(play2.play(PLAY2_COMMAND, PLAY2_DESCRIPTION))
                .addCommands(play3.play(PLAY3_COMMAND, PLAY3_DESCRIPTION))
                .addCommands(voiceChannelManagerAdapterCommand.manageVoiceChannel(VOICE_CHANNEL_MANAGER_COMMAND, VOICE_CHANNEL_MANAGER_DESCRIPTION))
                .addCommands(sendLinkCommand.sendLink(SEND_LINK_COMMAND, SEND_LINK_DESCRIPTION))
                .addCommands(helpCommand.help(HELP_COMMAND, HELP_DESCRIPTION))
                .addCommands(ttsCommand.tts(TTS_COMMAND, TTS_DESCRIPTION));
        commands.queue();
    }

    private Map<String, Command> setCommandsMap() {
        return Map.of(
                PLAY_AUDIO, new PlayAutocompleteCommand(audioPlayerLoadResultHandler, audioPlayerSendHandler, audioFiles.getPlayAudioFileList(), fileService),
                PLAY1_COMMAND, new Play1(audioPlayerLoadResultHandler, audioPlayerSendHandler, audioFiles.getPlayAudioFileList(), fileService),
                PLAY2_COMMAND, new Play2(audioPlayerLoadResultHandler, audioPlayerSendHandler, audioFiles.getPlayAudioFileList(), fileService),
                PLAY3_COMMAND, new Play3(audioPlayerLoadResultHandler, audioPlayerSendHandler, audioFiles.getPlayAudioFileList(), fileService),
                VOICE_CHANNEL_MANAGER_COMMAND, new VoiceChannelManagerCommand(fileService, adminId, powerUserId),
                SEND_LINK_COMMAND, new SendLinkCommand(adminId),
                HELP_COMMAND, new HelpCommand(helpCommands()),
                TTS_COMMAND, new TtsCommand(audioPlayerSendHandler, audioPlayerLoadResultHandler, fileService)
        );
    }

    /**
     * Returns a map containing a command and the relative description.
     *
     * @return a map with commands as keys and their descriptions as values
     */
    private Map<String, String> helpCommands() {
        Map<String, String> helpCommands = new LinkedHashMap<>();
        helpCommands.put(PLAY_AUDIO, PLAY_AUDIO_DESCRIPTION);
        helpCommands.put(PLAY1_COMMAND, PLAY1_HELP_DESCRIPTION);
        helpCommands.put(PLAY2_COMMAND, PLAY2_HELP_DESCRIPTION);
        helpCommands.put(PLAY3_COMMAND, PLAY3_HELP_DESCRIPTION);
        helpCommands.put(VOICE_CHANNEL_MANAGER_COMMAND, VOICE_CHANNEL_MANAGER_HELP_DESCRIPTION);
        helpCommands.put(SEND_LINK_COMMAND, SEND_LINK_DESCRIPTION);
        helpCommands.put(HELP_COMMAND, HELP_DESCRIPTION);
        helpCommands.put(TTS_COMMAND, TTS_DESCRIPTION);
        return helpCommands;
    }

    /**
     * Timeout value in ms
     * 21600000 = 6 hours
     */
    @Scheduled(fixedRate = 21600000)
    private void disconnectIfAloneInVoiceChannel() {
        jda.getGuilds().stream()
                .filter(guild -> Objects.nonNull(guild.getSelfMember().getVoiceState())
                        && Objects.nonNull(guild.getSelfMember().getVoiceState().getChannel())
                        && guild.getSelfMember().getVoiceState().getChannel() instanceof VoiceChannel
                        && guild.getSelfMember().getVoiceState().getChannel().getMembers().size() == 1)
                .map(guild -> guild.getSelfMember().getVoiceState().getChannel())
                .forEach(channel -> {
                    log.info("Bot was alone for 6 hours in {}, disconnecting", fileService.getChannelNameWithoutEmoji(channel.getName()));
                    channel.getGuild().getAudioManager().closeAudioConnection();
                });
    }

    /**
     * Encrypts a sample string using Jasypt with AES-256 and a random IV.
     * Prints the encrypted value. Password here is a placeholder and should
     * never be hardcoded in real applications.
     */
    /*private void encryptor() {
        String keyToEncrypt = "StringToEncrypt";
        StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
        encryptor.setAlgorithm("PBEWITHHMACSHA512ANDAES_256");
        encryptor.setPassword("yourPassword");
        encryptor.setIvGenerator(new RandomIvGenerator());
        String encryptedKey = encryptor.encrypt(keyToEncrypt);
        System.out.println("Encrypt key: " + encryptedKey);
    }*/

}