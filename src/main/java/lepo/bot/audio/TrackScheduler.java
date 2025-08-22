package lepo.bot.audio;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * This class schedules tracks for the audio player. It contains the queue of tracks.
 */
@Slf4j
public class TrackScheduler extends AudioEventAdapter {

    private final AudioPlayer player;
    private final BlockingQueue<AudioTrack> queue;

    /**
     * @param player The audio player this scheduler uses
     */
    public TrackScheduler(AudioPlayer player) {
        this.player = player;
        this.queue = new LinkedBlockingQueue<>();
    }

    /**
     * Add the next track to queue or play right away if nothing is in the queue.
     * Calling startTrack with the noInterrupt set to true will start the track only if nothing is currently playing. If
     * something is playing, it returns false and does nothing. In that case the player was already playing so this
     * track goes to the queue instead.
     *
     * @param track The track to play or add to queue.
     */
    public void queue(AudioTrack track) {
        if (!player.startTrack(track, true)) {
            if (queue.offer(track)) {
                log.info("Track {} added to queue", getAudioTrackName(track));
            }
        }
    }

    /**
     * Start the next track, stopping the current one if it is playing.
     * Start the next track, regardless of if something is already playing or not. In case queue was empty, we are
     * giving null to startTrack, which is a valid argument and will simply stop the player.
     * StartTrack:
     * true:  does not interrupt a track that is already playing. If there is an active track, the method returns `false` immediately and makes no changes.
     * false: does interrupt the current track (if any) and starts/sets the new track (or stops playback if `track == null`).
     */
    public void nextTrack() {
        player.startTrack(queue.poll(), true); //era false - testo true
    }

    /**
     * Only start the next track if the end reason is suitable for it (FINISHED or LOAD_FAILED)
     */
    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        if (endReason.mayStartNext) {
            nextTrack();
            log.info("Track {} {}", getAudioTrackName(track), endReason.name());
        }

    }

    private String getAudioTrackName(AudioTrack track) {
        String identifier = track.getInfo().identifier;
        return Optional.of(identifier)
                .filter(request -> request.startsWith("ftts://"))
                .or(() -> Optional.of(identifier)
                        .map(id -> id.split("\\\\"))
                        .flatMap(parts -> Arrays.stream(parts)
                                .reduce((first, second) -> second))
                        .map(s -> s.split("\\.")[0]))
                .orElse("AUDIO_FILE_NOT_FOUND");
    }

    public void clearQueueAndStop() {
        queue.clear();
        player.stopTrack();
    }

}