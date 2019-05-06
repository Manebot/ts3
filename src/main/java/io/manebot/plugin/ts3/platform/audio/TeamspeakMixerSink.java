package io.manebot.plugin.ts3.platform.audio;

import com.github.manevolent.ts3j.audio.Microphone;
import com.github.manevolent.ts3j.enums.CodecType;
import io.manebot.plugin.audio.mixer.output.AbstractOpusMixerSink;
import io.manebot.plugin.audio.opus.OpusParameters;

import javax.sound.sampled.AudioFormat;

public class TeamspeakMixerSink extends AbstractOpusMixerSink implements Microphone {
    public TeamspeakMixerSink(AudioFormat audioFormat,
                              OpusParameters opusParameters,
                              int bufferSizeInBytes) {
        super(audioFormat, opusParameters, bufferSizeInBytes);
    }

    @Override
    public boolean isMuted() {
        return false;
    }

    @Override
    public CodecType getCodec() {
        return CodecType.OPUS_MUSIC;
    }
}
