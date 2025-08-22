package lepo.bot.audio;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

@Slf4j
public class AudioPlayerLoadResultHandler implements AudioLoadResultHandler {

    private final AudioPlayerSendHandler audioPlayerSendHandler;

    public AudioPlayerLoadResultHandler(AudioPlayerSendHandler audioPlayerSendHandler) {
        this.audioPlayerSendHandler = audioPlayerSendHandler;
    }

    @Override
    public void trackLoaded(AudioTrack track) {
        log.info("Playing {}", getAudioFileName(track));
        audioPlayerSendHandler.getTrackScheduler().queue(track);
    }

    @Override
    public void playlistLoaded(AudioPlaylist playlist) {
        log.info("playlistLoaded");
    }

    @Override
    public void noMatches() {
        log.info("noMatches");
    }

    @Override
    public void loadFailed(FriendlyException exception) {
        log.info("loadFailed");
    }

    private String getAudioFileName(AudioTrack track) {
        return Arrays.stream(track.getInfo().identifier.split("\\\\")).map(s -> s.split("\\.")[0]).reduce((first, second) -> second).orElse("AUDIO_FILE_NOT_FOUND");
    }

}