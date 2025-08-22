package lepo.bot.slashcommand.voicechannelmanager;

import lepo.bot.filemanager.FileService;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.entities.emoji.UnicodeEmoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.managers.AudioManager;
import net.dv8tion.jda.internal.interactions.CommandDataImpl;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static lepo.bot.slashcommand.voicechannelmanager.VoiceChannelManagerCommand.VOICE_CHANNEL_MANAGER_COMMAND;

@Slf4j
@Component
@SuppressWarnings("all")
public abstract class VoiceChannelManagerListenerAdapter extends ListenerAdapter {

    public static final String RED_SIGN = "\u26D4";
    private static final String GREEN_SIGN = "\uD83D\uDFE2";
    private final String adminId;
    private final String powerUserId;
    private static final String SELECT_VOICE_CHANNEL = "select-voice-channel";
    private final FileService fileService;

    protected VoiceChannelManagerListenerAdapter(FileService fileService, String adminId, String powerUserId) {
        this.fileService = fileService;
        this.powerUserId = powerUserId;
        this.adminId = adminId;
    }

    public SlashCommandData manageVoiceChannel(String commandName, String description) {
        log.info("Loading command {}", commandName);
        return new CommandDataImpl(commandName, description).setGuildOnly(true);
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        boolean isAcknowledged = false;
        boolean isAdmin = event.getMember().hasPermission(Permission.ADMINISTRATOR);
        List<String> allowedUserId = List.of(powerUserId, adminId);
        Consumer<InteractionHook> interactionHookConsumer = interactionHook -> {
            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
            scheduler.schedule(() -> interactionHook.deleteOriginal().queue(), 5, TimeUnit.MINUTES);
        };
        boolean isVoiceChannelCommand = event.getName().equals(VOICE_CHANNEL_MANAGER_COMMAND);
        if (isVoiceChannelCommand && (allowedUserId.contains(event.getMember().getUser().getId()) || isAdmin)) {
            List<SelectOption> voiceChannels = Objects.requireNonNull(event.getGuild()).getVoiceChannels().stream()
                    .map(channel -> SelectOption.of(
                                    addProhibitedEmojiIfNeeded(fileService.getChannelNameWithoutEmoji(channel.getName())), channel.getId())
                            .withEmoji(getFirstEmojiInChannelName(channel.getName())))
                    .collect(Collectors.toList());
            event.reply("Lista canali audio")
                    .addActionRow(
                            StringSelectMenu.create(SELECT_VOICE_CHANNEL)
                                    .addOptions(voiceChannels)
                                    .build())
                    .setEphemeral(true)
                    .queue(interactionHookConsumer);
            isAcknowledged = true;
        }
        if ((event.getName().equals(VOICE_CHANNEL_MANAGER_COMMAND) && !isAdmin) && !isAcknowledged) {
            event.reply("Non hai i permessi per usare questo comando.")
                    .setEphemeral(true)
                    .queue(interactionHookConsumer);
        }
    }

    /**
     * After adding or removing a channel from the allowed voice channel list,
     * restore the selection menu to its defaultChannels position and return the selection menu.
     */
    @Override
    public void onStringSelectInteraction(@NotNull StringSelectInteractionEvent event) {
        String botName = Objects.requireNonNull(event.getGuild()).getSelfMember().getEffectiveName();
        String channel = removeGreenEmoji(removeProhibitedEmoji(getSelectedVoiceChannelName(event)));
        boolean isChannelRemoved = false;
        if (fileService.notContainedInFile(channel)) {
            fileService.writeToFilePath(channel);
            conditionalDisconnect(event);
        } else {
            isChannelRemoved = true;
            fileService.removeFromFile(channel);
        }
        List<SelectOption> defaultChannelsPosition = Objects.requireNonNull(event.getGuild()).getVoiceChannels().stream()
                .map(c -> SelectOption.of(addProhibitedEmojiIfNeeded(fileService.getChannelNameWithoutEmoji(c.getName())), c.getId())
                        .withEmoji(getFirstEmojiInChannelName(c.getName())))
                .collect(Collectors.toList());
        String entra = String.format("%s %s ora potra' entrare nel canale %s", GREEN_SIGN, botName, channel);
        String nonEntra = String.format("%s %s non potra' piu' entrare in %s", RED_SIGN, botName, channel);
        event.editMessage(isChannelRemoved ? (entra + " " + GREEN_SIGN) : (nonEntra + " " + RED_SIGN))
                .setActionRow(
                        StringSelectMenu.create(SELECT_VOICE_CHANNEL)
                                .addOptions(defaultChannelsPosition)
                                .build())
                .queue();
    }

    private void conditionalDisconnect(StringSelectInteractionEvent event) {
        String channelWhereBotIsConnected = Optional.ofNullable(event.getGuild())
                .map(guild -> guild.getAudioManager().getConnectedChannel())
                .filter(VoiceChannel.class::isInstance)
                .map(channel -> fileService.getChannelNameWithoutEmoji(channel.getName()))
                .orElse("Bot is not connected to any voice channel.");
        String selection = fileService.getChannelNameWithoutEmoji(getSelectedVoiceChannelName(event));
        Optional.of(selection)
                .filter(sel -> sel.contains(channelWhereBotIsConnected))
                .map(sel -> event.getGuild())
                .map(Guild::getAudioManager)
                .ifPresentOrElse(
                        AudioManager::closeAudioConnection,
                        () -> log.info("Do not disconnect")
                );
    }

    private String addProhibitedEmojiIfNeeded(String channelName) {
        return !fileService.notContainedInFile(channelName) ? channelName + RED_SIGN : (channelName + GREEN_SIGN);
    }

    private String removeGreenEmoji(String channelName) {
        return channelName.replace(GREEN_SIGN, "");
    }

    private String removeProhibitedEmoji(String channelName) {
        return channelName.replace(RED_SIGN, "");
    }

    private String getSelectedVoiceChannelName(StringSelectInteractionEvent event) {
        return event.getSelectedOptions().stream().findFirst().map(SelectOption::getLabel).orElse("channelNameNotFound");
    }

    /**
     * If input is not presente use microphone emoji
     *
     * @param input
     * @return
     */
    private UnicodeEmoji getFirstEmojiInChannelName(String input) {
        return input.codePoints()
                .mapToObj(c -> new String(Character.toChars(c)))
                .filter(fileService::isEmoji)
                .map(Emoji::fromUnicode)
                .findFirst()
                .orElse(Emoji.fromUnicode("\uD83C\uDFA4"));
    }

}