package com.janatawifi.screen_sharing_android;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;

public class ScreenCaptureEncoder {
    private MediaCodec mediaCodec;
    private Surface inputSurface;
    private MediaProjection mediaProjection;

    public ScreenCaptureEncoder(MediaProjection mediaProjection) {
        this.mediaProjection = mediaProjection;
    }

    public void start() throws IOException {
        // Configure MediaFormat for the desired output (e.g., AVC video)
        MediaFormat format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, 1280, 720);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, 5000000);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 5);

        // Create and configure MediaCodec as an encoder
        mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
        mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        inputSurface = mediaCodec.createInputSurface();
        mediaCodec.start();

        // Create a Virtual Display to receive screen content
        mediaProjection.createVirtualDisplay("ScreenCapture",
                1280, 720, 1,
                0, inputSurface, null, null);

        new Thread(() -> {
            while (true) { // 'encoding' is a boolean flag to control the loop
                byte[] frame = encodeFrame();
                if (frame != null) {
                    // Process the encoded frame...
                    Log.d("TAG", "start: " + frame);
                }
                // Implement appropriate timing control, e.g., sleep if needed
            }
        }).start();
    }

    public void stop() {
        if (mediaCodec != null) {
            mediaCodec.stop();
            mediaCodec.release();
            mediaCodec = null;
        }
        if (inputSurface != null) {
            inputSurface.release();
            inputSurface = null;
        }
    }

    public byte[] encodeFrame() {
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 10000);
        if (outputBufferIndex >= 0) {
            ByteBuffer outputBuffer = mediaCodec.getOutputBuffer(outputBufferIndex);
            byte[] frameData = new byte[bufferInfo.size];
            outputBuffer.get(frameData);
            mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
            return frameData;
        }
        return null;
    }
}
