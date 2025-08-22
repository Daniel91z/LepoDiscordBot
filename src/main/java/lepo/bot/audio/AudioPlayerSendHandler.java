package lepo.bot.audio;

import com.github.topi314.lavasrc.flowerytts.FloweryTTSSourceManager;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.track.playback.MutableAudioFrame;
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer;
import lombok.Getter;
import net.dv8tion.jda.api.audio.AudioSendHandler;

import java.nio.ByteBuffer;

@Getter
public class AudioPlayerSendHandler implements AudioSendHandler {

    /**
     * Tts voice:
     * "name": "Stella",
     * "gender": "Female",
     * "source": "Google Cloud",
     * voice at <a href="https://api.flowery.pw/v1/tts/voices">TTS_VOICE</a>
     */
    private static final String TTS_VOICE = "d93be03a-bc71-52fd-8e51-1e5cf24cf621";

    private final AudioPlayerManager audioPlayerManager;
    private final AudioPlayer audioPlayer;
    private final ByteBuffer buffer;
    private final MutableAudioFrame frame;
    private final TrackScheduler trackScheduler;

    public AudioPlayerSendHandler() {
        this.audioPlayerManager = new DefaultAudioPlayerManager();
        this.audioPlayerManager.getConfiguration().setFrameBufferFactory(NonAllocatingAudioFrameBuffer::new);
        AudioSourceManagers.registerRemoteSources(audioPlayerManager);
        AudioSourceManagers.registerLocalSource(audioPlayerManager);
        this.audioPlayer = audioPlayerManager.createPlayer();
        this.buffer = ByteBuffer.allocate(2048);
        this.frame = new MutableAudioFrame();
        this.frame.setBuffer(buffer);
        FloweryTTSSourceManager ttsSourceManager = new FloweryTTSSourceManager(TTS_VOICE);
        audioPlayerManager.registerSourceManager(ttsSourceManager);
        this.trackScheduler = new TrackScheduler(this.audioPlayer);
        this.audioPlayer.addListener(trackScheduler);
    }

    @Override
    public boolean canProvide() {
        return this.audioPlayer.provide(this.frame);
    }

    @Override
    public ByteBuffer provide20MsAudio() {
        return this.buffer.flip();
    }

    @Override
    public boolean isOpus() {
        return true;
    }

}