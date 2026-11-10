package com.campusbill.app.chat.voice;

import org.vosk.Model;

import java.io.IOException;

/** 边喂 PCM 边在停顿处收成一段。调用方只在结束时拿整句。 */
public final class VoskSegmentReader implements AutoCloseable {

    private VoskPcmSession session;
    private final PauseSegmenter pauses = new PauseSegmenter();
    private final UtteranceBuffer buffer = new UtteranceBuffer();

    public VoskSegmentReader(Model model) throws IOException {
        this.session = new VoskPcmSession(model);
    }

    public String push(byte[] pcm, int length) throws IOException {
        if (pcm == null || length <= 0) {
            return buffer.text();
        }
        boolean endpoint = session.accept(pcm, length);
        if (endpoint) {
            buffer.commit(session.result());
            pauses.noteFlushed();
        } else if (pauses.push(pcm, length).flush) {
            buffer.commit(session.finishAndReset());
        } else {
            buffer.setPartial(session.partial());
        }
        return buffer.text();
    }

    public String finish() throws IOException {
        buffer.commit(session.finish());
        buffer.setPartial("");
        return buffer.text();
    }

    @Override
    public void close() {
        if (session != null) {
            session.close();
            session = null;
        }
    }
}
