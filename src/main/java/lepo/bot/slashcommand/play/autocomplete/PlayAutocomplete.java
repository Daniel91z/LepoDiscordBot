package lepo.bot.slashcommand.play.autocomplete;

import lepo.bot.audio.AudioPlayerLoadResultHandler;
import lepo.bot.audio.AudioPlayerSendHandler;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.internal.interactions.CommandDataImpl;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import static lepo.bot.slashcommand.play.autocomplete.PlayAutocompleteCommand.PLAY_AUDIO;

@Slf4j
public abstract class PlayAutocomplete extends ListenerAdapter {

    private final Map<String, List<Command.Choice>> autocompleteCache = new HashMap<>();
    private static final int CACHE_SIZE_LIMIT = 150;
    protected final AudioPlayerLoadResultHandler audioPlayerLoadResultHandler;
    protected final AudioPlayerSendHandler audioPlayerSendHandler;
    protected final List<File> playAudioFilesList;
    private final List<String> audioFileNames;
    private static final String OPTION_NAME = "nome";
    private static final String OPTION_DESCRIPTION = "Audio da riprodurre (visualizzati 25 audio casuali ma e' possibile riprodurli tutti)";

    protected PlayAutocomplete(AudioPlayerLoadResultHandler audioPlayerLoadResultHandler, AudioPlayerSendHandler audioPlayerSendHandler, List<File> playAudioFilesList) {
        this.audioPlayerLoadResultHandler = audioPlayerLoadResultHandler;
        this.audioPlayerSendHandler = audioPlayerSendHandler;
        this.playAudioFilesList = playAudioFilesList;
        audioFileNames = getAudioFileNames(playAudioFilesList);
    }

    public SlashCommandData playAuto(String commandName, String description) {
        log.info("Loading command {}", commandName);
        return new CommandDataImpl(commandName, description).setGuildOnly(true)
                .addOption(OptionType.STRING, OPTION_NAME, OPTION_DESCRIPTION, true, true);
    }

    protected void playAuto(String commandName, SlashCommandInteractionEvent event) {
    }

    /**
     * Handles the auto-complete interaction for commands.
     *
     * <p>This method is triggered when a command auto-complete interaction event occurs. It checks if the event
     * name matches {@code PLAY_AUDIO} and the focused option's name matches {@code OPTION_NAME}. If both conditions
     * are met, it filters the list of audio file names based on the user's input, creating a list of choices that
     * either start with the input (ignoring case) or contain the input as a substring (also ignoring case).
     * It then shuffles the filtered list randomly and limits the result to 25 choices, which are then sent back
     * as the auto-complete suggestions.</p>
     *
     * @param event the command auto-complete interaction event
     */
    @Override
    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
        if (event.getName().equals(PLAY_AUDIO) && event.getFocusedOption().getName().equals(OPTION_NAME)) {
            String userInput = event.getFocusedOption().getValue().toLowerCase();

            List<Command.Choice> cachedOptions = autocompleteCache.get(userInput);
            if (cachedOptions != null) {
                log.info("Retrieved from cache: {}", userInput);
                List<Command.Choice> optionsLimited = cachedOptions.stream()
                        .limit(25)
                        .collect(Collectors.toList());
                event.replyChoices(optionsLimited).queue();
                return;
            }

            log.info("Cache miss, calculating options for: {}", userInput);
            List<Command.Choice> options = calculateAutocompleteOptions(userInput);
            Collections.shuffle(options, new Random());
            List<Command.Choice> optionsLimited = options.stream()
                    .limit(25)
                    .collect(Collectors.toList());

            if (autocompleteCache.size() < CACHE_SIZE_LIMIT) {
                autocompleteCache.put(userInput, optionsLimited);
            }

            event.replyChoices(optionsLimited).queue();
        }
    }

    private List<Command.Choice> calculateAutocompleteOptions(String userInput) {
        return audioFileNames.stream()
                .filter(word -> startsWithIgnoreCase(word, userInput) || word.toLowerCase().contains(userInput))
                .map(word -> new Command.Choice(word, word))
                .collect(Collectors.toList());
    }

    private boolean startsWithIgnoreCase(String str, String prefix) {
        return Optional.ofNullable(str)
                .filter(s -> prefix != null)
                .filter(s -> s.length() >= prefix.length())
                .map(s -> s.regionMatches(true, 0, prefix, 0, prefix.length()))
                .orElse(false);
    }

    private @NotNull List<String> getAudioFileNames(List<File> playAudioFilesList) {
        return playAudioFilesList.stream()
                .map(file -> getFileNameWithoutExtension(file.getName()))
                .collect(Collectors.toList());
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

}