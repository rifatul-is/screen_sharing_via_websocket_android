package com.janatawifi.screen_sharing_android;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import android.Manifest;

import okhttp3.OkHttpClient;
import okhttp3.Request;

import android.view.WindowManager;


public class MainActivity extends AppCompatActivity {

    private Button shareBtn;
    private Button sendBtn;
    private Button stopBtn;
    private static final int REQUEST_CODE_CAPTURE_PERM = 1234;
    private MediaCodec mediaCodec;
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private WebSocketEcho echo = new WebSocketEcho();

    private SurfaceView surfaceView;

    private ImageReader imageReader;

    private static final int REQUEST_CODE_CAPTURE_PERMISSION = 1;
    private MediaProjectionManager mediaProjectionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        shareBtn = findViewById(R.id.share_btn);
        sendBtn = findViewById(R.id.send_btn);
        surfaceView = findViewById(R.id.surfaceView);

        echo.run("ws://0.tcp.ap.ngrok.io:19057/ws");


        shareBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestScreenCapturePermission();
                //echo.run("wss://127.0.0.1:8000/ws");
                //mProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
                // Start request
                //startActivityForResult(mProjectionManager.createScreenCaptureIntent(), REQUEST_CODE_CAPTURE_PERM);
                //startMediaProjectionRequest();
            }
        });

        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                echo.sendMessage("Testing local web socket");
            }
        });

//        stopBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//
//            }
//        });
    }




    public void requestScreenCapturePermission() {
        mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        Intent captureIntent = mediaProjectionManager.createScreenCaptureIntent();


        //Starting Foreground Services
        //Works Only Till Android 10
        //Newer Implementation needed for Newer Android's
        Intent intent = new Intent(this, ScreenCaptureService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d("TAG", "Android OS : Grater Then Android Oreo");
            startForegroundService(intent);
            Log.d("TAG", "startForegroundService Function Executed");
        }



        startActivityForResult(captureIntent, REQUEST_CODE_CAPTURE_PERMISSION);

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_CAPTURE_PERMISSION) {
            if (resultCode == RESULT_OK && data != null) {
                mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data);

                // Register the callback
                mediaProjection.registerCallback(mediaProjectionCallback, null);


                if (mediaProjection != null) {
                    //initializeImageReader(getApplicationContext()); // Now safe to proceed with this
                    setUpVirtualDisplay();
                    //startScreenShare(resultCode, data);
                } else {
                    Log.d("TAG", "onActivityResult: Media projection is null failed");
                    // Handle the case where mediaProjection is null (e.g., permission denied)
                }
            }
        }
    }

    private final MediaProjection.Callback mediaProjectionCallback = new MediaProjection.Callback() {
        @Override
        public void onStop() {
            super.onStop();
            // Handle the projection stopping (e.g., release resources)
            Log.e("MediaProjection", "MediaProjection has stopped. Release resources here.");
            // Remember to unregister this callback when it's no longer needed.
            if (mediaProjection != null) {
                mediaProjection.unregisterCallback(this);
                mediaProjection = null;
            }
        }
    };

    private void setUpVirtualDisplay() {
        Log.d("TAG", "setUpVirtualDisplay: Inside");
        // Define the screen dimensions and image format
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int screenWidth = metrics.widthPixels;
        int screenHeight = metrics.heightPixels;
        int screenDensity = metrics.densityDpi;
        final long[] lastTimestamp = {0};
        // Last time an image was processed
        //Log.d("TAG", "setUpVirtualDisplay: " + screenWidth + " " + screenHeight +  " " + screenDensity);

//        int screenWidth = 1000;
//        int screenHeight = 1000;
//        int screenDensity = metrics.densityDpi;

        // Set up the ImageReader
        imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 1);
        Log.d("TAG", "setUpVirtualDisplay: Image Reader created");

        // Create the VirtualDisplay
        virtualDisplay = mediaProjection.createVirtualDisplay("ScreenCapture",
                screenWidth, screenHeight, screenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.getSurface(), null, null);

        Log.d("TAG", "setUpVirtualDisplay: Virtual display created");

        imageReader.setOnImageAvailableListener(reader -> {
            long currentTime = System.currentTimeMillis();
            Log.d("TAG", "onImageAvailable: listening to new images");

            Log.d("TAG", "setUpVirtualDisplay: current time : " + currentTime);
            Log.d("TAG", "setUpVirtualDisplay: last time stamp : " + lastTimestamp[0]);

            // Check if 5 seconds have passed since the last image was processed
                Image image = null;
                try {
                    image = reader.acquireLatestImage();
                    if (image != null) {
                        if (currentTime - lastTimestamp[0] >= 2000) {
                            Log.d("TAG", "onImageAvailable: aquired latest image " + image);
                            // Process the image here
                            Image.Plane[] planes = image.getPlanes();
                            ByteBuffer buffer = planes[0].getBuffer();
                            // For simplicity, assuming the image format is compatible
                            byte[] data = new byte[buffer.remaining()];
                            buffer.get(data);
                            Log.d("TAG", "onImageAvailable: " + data.toString());
                            //echo.sendMessage(data);


//                    final Image.Plane[] planes = image.getPlanes();
//                    final ByteBuffer buffer = planes[0].getBuffer();
//                    int offset = 0;
//                    int pixelStride = planes[0].getPixelStride();
//                    int rowStride = planes[0].getRowStride();
//                    int rowPadding = rowStride - pixelStride * screenWidth;
//// create bitmap
//                    Bitmap bmp = Bitmap.createBitmap(screenWidth+rowPadding/pixelStride, screenHeight, Bitmap.Config.RGB_565);
//                    bmp.copyPixelsFromBuffer(buffer);


                            Bitmap bitmap = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.ARGB_8888);
                            ByteBuffer.wrap(data).rewind(); // Ensure the buffer is ready to be read
                            bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(data));

////creating base 64 from bitmap
                            // Step 1: Compress the Bitmap
                            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
                            byte[] byteArray = byteArrayOutputStream.toByteArray();
                            String encodedImage = Base64.encodeToString(byteArray, Base64.NO_WRAP);
                            echo.sendMessage(encodedImage);
                            Log.d("TAG", "setUpVirtualDisplay: base 64 image data" + encodedImage);


//Sacing bitmap to file
//                        ContentValues contentValues = new ContentValues();
//                        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "MyImage_" + System.currentTimeMillis() + ".jpg");
//                        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
//                        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + File.separator + "MyApp");
//
//                        Uri imageUri = getApplicationContext().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
//
//                        try (OutputStream out = getApplicationContext().getContentResolver().openOutputStream(imageUri)) {
//                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
//                            Log.d("SaveImage", "Image saved successfully: " + imageUri);
//                        } catch (IOException e) {
//                            Log.e("SaveImage", "Error saving image", e);
//                        }


                            lastTimestamp[0] = System.currentTimeMillis();
                            Log.d("TAG", "setUpVirtualDisplay: last time inside image avilable " + lastTimestamp[0]);
                            image.close();
                        } else {
                            Log.d("TAG", "setUpVirtualDisplay: Not time yet");
                        }


                        //converting byte to bitmap to base 64

//                    int width = 1080;
//                    int height = 2119;
//                    byte[] rgba = data;
//
//                    // Convert byte[] to int[]
//                    int[] pixels = new int[rgba.length / 4];
//                    ByteBuffer.wrap(rgba).asIntBuffer().get(pixels);
//
//                    // Create a Bitmap from the int[] array
//                    Bitmap bitmap = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888);
//
//                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
//                    byte[] jpegData = outputStream.toByteArray();
//                    String base64Image = Base64.encodeToString(jpegData, Base64.DEFAULT);
//                    Log.d("TAG", "setUpVirtualDisplay: " + base64Image);


                        // Now 'data' contains the raw frame data
                    } else {
                        Log.d("TAG", "onImageAvailable: image not avilable");
                    }
                } finally {
                    if (image != null) {
                        image.close();
                    }
                }

        }, null);
    }


    private void startScreenShare(int resultCode, Intent data) {
        MediaProjection mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data);
        this.mediaProjection = mediaProjection;

        WindowManager windowManager = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);
        int screenWidth = metrics.widthPixels;
        int screenHeight = metrics.heightPixels;


        MediaFormat format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, screenWidth / 2, screenHeight / 2);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, 5 * (int) Math.pow(2, 20) * 8); // 5 Mbps
        format.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2);


        try {
            mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

        // Get the input surface from the encoder, and use it to create the virtual display
        Surface inputSurface = mediaCodec.createInputSurface();

        mediaProjection.createVirtualDisplay("ScreenCapture",
                1280, 720, 300,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                inputSurface, null, null);

//         Start the encoder
        mediaCodec.start();

        // Now you can retrieve and process the encoded data in a separate thread
        //new Thread(this::encodeToBlobs).start();


        new Thread(() -> {
            while (true) { // 'encoding' is a boolean flag to control the loop
                byte[] frame = encodeFrame();
                if (frame != null) {
                    // Process the encoded frame...
                    Log.d("TAG", "start: " + frame);
                    //echo.sendMessage(frame);


                    //sacing image from bytes
                    Context context = getApplicationContext(); // Get your context here (e.g., from an Activity or Application)
                    String fileName = "MyImage_" + System.currentTimeMillis() + ".jpg"; // Dynamic file name

                    File picturesDirectory = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                    File imageFile = new File(picturesDirectory, fileName);

                    try (FileOutputStream fos = new FileOutputStream(imageFile)) {
                        fos.write(frame); // 'data' is the byte array obtained from the ByteBuffer
                        fos.flush(); // Ensure all data is written out
                        Log.d("SaveImage", "Image saved successfully: " + imageFile.getAbsolutePath());
                    } catch (IOException e) {
                        Log.e("SaveImage", "Error saving image", e);
                    }


                }
                // Implement appropriate timing control, e.g., sleep if needed
            }
        }).start();


        // At this point, the screen capture is displayed on the SurfaceView.
        // You can also start sending the display to a remote server for real screen sharing.
    }


    private void initializeImageReader(Context context) {
        // Obtain the device screen dimensions
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);
        int screenWidth = metrics.widthPixels;
        int screenHeight = metrics.heightPixels;

        // Set up the ImageReader
        //imageReader = ImageReader.newInstance(screenWidth, screenHeight, ImageFormat.JPEG, 2);


        int width = 1080; // Consider reducing if allocation fails
        int height = 1920; // Adjust based on aspect ratio or specific needs
        int format = ImageFormat.JPEG; // Or another supported format
        int maxImages = 1; // Keep as low as feasible

        try {
            Log.d("TAG", "initializeImageReader: Success");
            imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 1);

        } catch (IllegalArgumentException e) {
            Log.e("ScreenCapture", "Failed to create ImageReader. Check format and size.", e);
            // Consider fallback or adjustment logic here
        }

        Log.d("TAG", "initializeImageReader: Creating virtual display");

        //         Create a VirtualDisplay to capture the screen
        virtualDisplay = mediaProjection.createVirtualDisplay("ScreenCapture",
                screenWidth, screenHeight, metrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.getSurface(), null, null);

        Log.d("TAG", "initializeImageReader: virtual display created");

        imageReader.setOnImageAvailableListener(reader -> {
            // Here is where you call convertFrameToBase64 when a new frame is available
            Log.d("TAG", "initializeImageReader: Trying to create images");
            String base64Image = convertFrameToBase64(reader);
            Log.d("TAG", "initializeImageReader: " + base64Image);
            //echo.sendMessage(base64Image);
            // Do something with the base64Image string here
        }, null); // Consider using a Handler for a background thread


    }

    public String convertFrameToBase64(ImageReader imageReader) {
        Image image = null;
        String base64Image = "";

        try {
            image = imageReader.acquireLatestImage();

            if (image != null) {
                Image.Plane[] planes = image.getPlanes();
                ByteBuffer buffer = planes[0].getBuffer();
                //byte[] data = new byte[buffer.capacity()];
                byte[] data = new byte[buffer.remaining()]; // Use remaining() to ensure you read the full data

                buffer.get(data);

                Log.d("TAG", "convertFrameToBase64: data : " + data);

                // Assuming the image format is JPEG
                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);

                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                if (bitmap == null) {
                    Log.e("Tag", "Failed to convert Image to Bitmap.");
                } else {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream);
                    byte[] imageBytes = outputStream.toByteArray();

                    // Convert byte array to Base64
                    base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT);
                }

            }
        } finally {
            if (image != null) {
                image.close();
            }
        }

        return base64Image;
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


    private void encodeToBlobs() {
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        //ByteBuffer[] outputBuffers = mediaCodec.getOutputBuffers();
        //ArrayList<byte[]> blobs = new ArrayList<>();

        while (!Thread.interrupted()) {
            Log.d("TAG", "encodeToBlobs: Inside Method");
//            int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 100000);
//            if (outputBufferIndex >= 0) {
//                ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
//                byte[] data = new byte[bufferInfo.size];
//                outputBuffer.get(data);
//                // Now 'data' contains the binary data for this frame. This is your 'blob'.
//                //Log.d("TAG", "encodeToBlobs: " + data);
//                echo.sendMessage(data);
//                blobs.add(data);
//
//
//                mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
//            } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
//                outputBuffers = mediaCodec.getOutputBuffers();
//            } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
//                // The output format has changed, handle it if necessary.
//            }


            int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 10000);
            if (outputBufferIndex >= 0) {
                Log.d("TAG", "encodeToBlobs: Output Buffer > 0");
                ByteBuffer outputBuffer = mediaCodec.getOutputBuffer(outputBufferIndex);
                byte[] frameData = new byte[bufferInfo.size];
                outputBuffer.get(frameData);
                mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
                echo.sendMessage(frameData);
            }

        }

        // At this point, 'blobs' contains all your frames as binary data.
        // You can now process these blobs as needed.
    }


    private void encodeToVideoFile() {
        ByteBuffer[] outputBuffers = mediaCodec.getOutputBuffers();
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

        while (true) { // You should have a proper condition to stop this loop
            int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 10000);
            if (outputBufferIndex >= 0) {
                ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
                // Here you can process the buffer (save to file, stream, etc.)
                Log.d("TAG", "encodeToVideoFile: " + outputBuffer);

                mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
            } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                outputBuffers = mediaCodec.getOutputBuffers();
            } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // Output format has changed, you can handle it here (e.g., by adjusting the file header if saving to a file)
            }
        }
    }
}