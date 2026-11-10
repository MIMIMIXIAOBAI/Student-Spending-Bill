package com.campusbill.app.chat.voice;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** 读出 16-bit PCM WAV 的 data 块。 */
public final class WavPcm {

    public final int sampleRate;
    public final int channels;
    public final int bitsPerSample;
    public final byte[] pcm;

    public WavPcm(int sampleRate, int channels, int bitsPerSample, byte[] pcm) {
        this.sampleRate = sampleRate;
        this.channels = channels;
        this.bitsPerSample = bitsPerSample;
        this.pcm = pcm;
    }

    public static WavPcm read(InputStream input) throws IOException {
        byte[] file = readAll(input);
        if (file.length < 44 || !ascii(file, 0, "RIFF") || !ascii(file, 8, "WAVE")) {
            throw new IOException("不是 WAV");
        }
        int sampleRate = 0;
        int channels = 0;
        int bits = 0;
        byte[] pcm = null;
        int offset = 12;
        while (offset + 8 <= file.length) {
            String id = new String(file, offset, 4, StandardCharsets.US_ASCII);
            int size = le32(file, offset + 4);
            int dataAt = offset + 8;
            if (dataAt + size > file.length) {
                break;
            }
            if ("fmt ".equals(id) && size >= 16) {
                channels = le16(file, dataAt + 2);
                sampleRate = le32(file, dataAt + 4);
                bits = le16(file, dataAt + 14);
            } else if ("data".equals(id)) {
                pcm = new byte[size];
                System.arraycopy(file, dataAt, pcm, 0, size);
                break;
            }
            offset = dataAt + size + (size % 2);
        }
        if (pcm == null) {
            throw new IOException("WAV 没有 data");
        }
        return new WavPcm(sampleRate, channels, bits, pcm);
    }

    private static byte[] readAll(InputStream input) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = input.read(buf)) >= 0) {
            if (n > 0) {
                out.write(buf, 0, n);
            }
        }
        return out.toByteArray();
    }

    private static boolean ascii(byte[] file, int offset, String text) {
        byte[] raw = text.getBytes(StandardCharsets.US_ASCII);
        if (offset + raw.length > file.length) {
            return false;
        }
        for (int i = 0; i < raw.length; i++) {
            if (file[offset + i] != raw[i]) {
                return false;
            }
        }
        return true;
    }

    private static int le16(byte[] file, int offset) {
        return (file[offset] & 0xff) | ((file[offset + 1] & 0xff) << 8);
    }

    private static int le32(byte[] file, int offset) {
        return (file[offset] & 0xff)
                | ((file[offset + 1] & 0xff) << 8)
                | ((file[offset + 2] & 0xff) << 16)
                | ((file[offset + 3] & 0xff) << 24);
    }
}
