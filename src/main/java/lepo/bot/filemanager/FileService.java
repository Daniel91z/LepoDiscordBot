package lepo.bot.filemanager;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class FileService {

    private final Path pathAndFileName;

    public void writeToFilePath(String input) {
        try {
            Files.write(pathAndFileName, (input + System.lineSeparator()).getBytes(), StandardOpenOption.APPEND, StandardOpenOption.CREATE);
            log.info("{} added from channels where the bot cant connect", input);
        } catch (IOException e) {
            log.info(e.getMessage());
        }
    }

    public void removeFromFile(String input) {
        try {
            List<String> lines = Files.readAllLines(pathAndFileName);
            List<String> filteredLines = lines.stream()
                    .filter(line -> !line.equals(input))
                    .collect(Collectors.toList());
            Files.write(pathAndFileName, filteredLines);
            log.info("{} removed from channels where the bot cant connect", input);
        } catch (IOException e) {
            log.info(e.getMessage());
        }
    }

    public boolean notContainedInFile(String input) {
        try {
            List<String> lines = Files.readAllLines(pathAndFileName);
            Optional<String> matchingLine = lines.stream()
                    .filter(line -> line.equals(input))
                    .findFirst();
            return matchingLine.isEmpty();
        } catch (IOException e) {
            log.info(e.getMessage());
            return true;
        }
    }

    public String getChannelNameWithoutEmoji(String input) {
        return input.codePoints()
                .mapToObj(c -> new String(Character.toChars(c)))
                .collect(Collectors.collectingAndThen(
                        Collectors.partitioningBy(this::isEmoji),
                        result -> {
                            String channelNameWithoutEmoji = result.get(false).isEmpty()
                                    ? "Canale vocale"
                                    : String.join("", result.get(false));
                            return channelNameWithoutEmoji.replaceFirst("^\\s+", ""); // Sometimes the channel name starts with a space — i want to remove that
                        }));
    }

    public boolean isEmoji(String input) {
        return input.codePoints().allMatch(c -> Character.UnicodeBlock.of(c) == Character.UnicodeBlock.MISCELLANEOUS_SYMBOLS_AND_PICTOGRAPHS ||
                Character.UnicodeBlock.of(c) == Character.UnicodeBlock.EMOTICONS ||
                Character.UnicodeBlock.of(c) == Character.UnicodeBlock.TRANSPORT_AND_MAP_SYMBOLS ||
                Character.UnicodeBlock.of(c) == Character.UnicodeBlock.SUPPLEMENTAL_SYMBOLS_AND_PICTOGRAPHS);
    }

}