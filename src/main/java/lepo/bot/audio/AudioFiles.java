package lepo.bot.audio;


import lepo.bot.event.buttoninteraction.Play;
import lepo.bot.event.guildvoiceupdate.GuildVoiceUpdateEventListener;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.PropertySource;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@PropertySource("classpath:application-${spring.profiles.active}.properties")
@Slf4j
@Getter
public class AudioFiles {

    private final List<File> playAudioFileList;
    private final List<File> welcomeAudioFileList;
    private final List<File> goodbyeAudioFileList;
    private final List<File> customAudioFileList;

    /**
     * The {@link #playAudioFileList} is used by {@link Play}
     * to reproduce audio files included in this list.
     * The {@link #welcomeAudioFileList} is used by {@link GuildVoiceUpdateEventListener}
     * to reproduce audio files included in this list, which last for a maximum of 3 to 4 seconds.
     * TBD JAVA DOC
     */
    public AudioFiles(String path, String welcomeAudioPath, String goodbyeAudioPath, String customAudioPath) {
        this.playAudioFileList = setPlayAudioFileList(path);
        this.welcomeAudioFileList = setWelcomeAudioFileList(welcomeAudioPath);
        this.goodbyeAudioFileList = setGoodbyeAudioFileList(goodbyeAudioPath);
        this.customAudioFileList = setCustomAudioFileList(customAudioPath);
    }

    private List<File> setPlayAudioFileList(String path) {
        if (Objects.nonNull(path)) {
            File directory = new File(path);
            File[] files = directory.listFiles((dir, name) -> name.toLowerCase().endsWith(".opus"));
            assert files != null;
            // Convert array to list
            List<File> audioList = Arrays.asList(files);
            // Sort the list alphabetically by file name (case-insensitive)
            audioList.sort((file1, file2) -> file1.getName().compareToIgnoreCase(file2.getName()));
            audioList.forEach(file -> log.info("Loading playAudioFileList {}", file.getName()));
            return audioList;
        }
        return new ArrayList<>();
    }

    private List<File> setWelcomeAudioFileList(String welcomeAudioPath) {
        if (Objects.nonNull(welcomeAudioPath)) {
            File directory = new File(welcomeAudioPath);
            File[] files = directory.listFiles((dir, name) -> name.toLowerCase().endsWith(".opus"));
            assert files != null;
            List<File> audioList = Arrays.stream(files).filter(Objects::nonNull).collect(Collectors.toList());
            audioList.forEach(file -> log.info("Loading welcomeAudioFileList {}", file.getName()));
            return audioList;
        }
        return new ArrayList<>();
    }

    private List<File> setGoodbyeAudioFileList(String goodbyeAudioPath) {
        if (Objects.nonNull(goodbyeAudioPath)) {
            File directory = new File(goodbyeAudioPath);
            File[] files = directory.listFiles((dir, name) -> name.toLowerCase().endsWith(".opus"));
            assert files != null;
            List<File> audioList = Arrays.stream(files).filter(Objects::nonNull).collect(Collectors.toList());
            audioList.forEach(file -> log.info("Loading goodbyeAudioFileList {}", file.getName()));
            return audioList;
        }
        return new ArrayList<>();
    }

    private List<File> setCustomAudioFileList(String customAudioPath) {
        if (Objects.nonNull(customAudioPath)) {
            File directory = new File(customAudioPath);
            File[] files = directory.listFiles((dir, name) -> name.toLowerCase().endsWith(".opus"));
            assert files != null;
            List<File> audioList = Arrays.stream(files).filter(Objects::nonNull).collect(Collectors.toList());
            audioList.forEach(file -> log.info("Loading customAudioFileList {}", file.getName()));
            return audioList;
        }
        return new ArrayList<>();
    }

}