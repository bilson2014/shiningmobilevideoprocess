package com.example.fucfuc;

public class testJNIapi {
	static {

		System.loadLibrary("avutil-54");
		System.loadLibrary("swscale-3");
		System.loadLibrary("swresample-1");
		System.loadLibrary("avcodec-56");
		System.loadLibrary("avformat-56");
		System.loadLibrary("avfilter-5");
		System.loadLibrary("avdevice-56");
		System.loadLibrary("fuc");
	}
    /*
     * crop video 
     * videoPath1  --> video path src
     * GenerationPath --> out videoPath
     */
	public static native int SynthesisVideo(String videoPath1, String GenerationPath);

	/*
	 * ReplaceAudio
	 */
	public static native int ReplaceAudio(String video,String Audio);
	
	/*
	 * crop video width=height 
	 */
	public static native int  cropVideo(String inPath,String VideoWidth);
}
