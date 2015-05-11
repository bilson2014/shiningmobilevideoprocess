package com.example.testvideo;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.StatFs;
import android.util.Log;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.fucfuc.testJNIapi;
import com.example.tools.PreviewSizeComparator;
import com.example.tools.PreviewSizeElement;

@SuppressLint("NewApi")
public class MainVideo extends Activity {
	private SurfaceView surfaceView;
	private Button Recorder_bt, Pause_bt, Reset_bt;
	private TextView time_tv; // 显示时间的文本框
	private MediaRecorder mRecorder;
	private boolean recording; // 记录是否正在录像,fasle为未录像, true 为正在录像
	private File videoFolder, videoFile; // 存放视频的文件夹:视频文件
	private Handler timeControlHandler;
	private int time, VideoCount, deltime; // 时间//视频总个数//需要删除的时间
	private Camera myCamera; // 相机声明
	private SurfaceHolder holder; // 显示一个surface的抽象接口，使你可以控制surface的大小和格式，
									// 以及在surface上编辑像素，和监视surace的改变。这个接口通常通过SurfaceView类实现
	private ProgressBar firstBar = null;// 进度条
	private Boolean flag = true;
	private String musicPath;
	private MediaPlayer mp = new MediaPlayer();
	private int nowtime;
	private boolean musicNotStarted = true; //判断录像时音乐是初始化(true)还是已经开始播放(false)

	private FrameLayout framelayout;
	private int widths, vHeight, vWidth; //计算video尺寸
	private double realh, priview_width, priview_height; //计算preview尺寸
	private Camera.Parameters parameters;
	private PreviewSizeElement[] pselist;
	
	private List list = new ArrayList(); //视频列表
	private long clickTime = 0; //双击判断辅助
	private testJNIapi tjni = new testJNIapi();
	final static String sdcardPath = Environment.getExternalStorageDirectory()
			+ "/ShinyRing/";
	private String demo = "mm:ss", fs;
	// 拓展用参数
	// private int cameraIndex = 0;
	// private TextView tv;
	// private FrameLayout.LayoutParams flp;
	// private Camera.Parameters parameters = myCamera.getParameters();
	// private List<FileElement> lf = new ArrayList<FileElement>();

	/**
	 * 录制过程中,时间变化,进度条变化，到达拍摄时间的处理
	 */
	private Runnable timeRun = new Runnable() {

		@Override
		public void run() {

			deltime++;
			flag = true;

			timeControlHandler.postDelayed(timeRun, 1000);
			time++;
			time_tv.setText(time + "秒");

			if (firstBar.getProgress() <= firstBar.getMax() && flag == true) {
				// 设置主进度条的当前值
				firstBar.setProgress(firstBar.getProgress() + 7);
				// 设置第二进度条的当前值 后期特效渲染
				// firstBar.setSecondaryProgress(firstBar.getProgress() + 10);
				// 因为默认的进度条无法显示进行的状态
				// secondBar.setProgress(i);

				if (firstBar.getProgress() >= 100) {
					Log.i("bar", "下载完毕");
					flag = false;
					mRecorder.stop();
					mRecorder.release();
					timeControlHandler.removeCallbacks(timeRun);
					Recorder_bt.setEnabled(false);
					firstBar.setVisibility(View.GONE);
					mp.stop();
					mp.release();
					time_tv.setText(time + "秒视频录制完毕");
					// 当一次性完成15秒拍摄list只存在当前拍摄视频 不做合成处理
					addList();

				}
			}

		}
	};

	// 预览图及VideoSize自适应
	public Pair<Double, Double> calculateVideoSize(double previewScale) {

		// step1 . get all supported sizes & calculate absolute difference with
		// previewScale, then store in a list
		List<Size> supportedVideoSizes = parameters.getSupportedVideoSizes();
		pselist = new PreviewSizeElement[supportedVideoSizes.size()];

		for (int i = 0; i < supportedVideoSizes.size(); i++) {
			Size size = supportedVideoSizes.get(i);

			double priWidth = size.width;
			double priHeight = size.height;
			double pri = priWidth / priHeight;
			double priReal = pri - previewScale;
			double x = Math.abs(priReal);
			PreviewSizeElement pse = new PreviewSizeElement(priWidth,
					priHeight, x);
			pselist[i] = pse;
		}

		// step2. sort the list based on absolute difference, from small to big
		Arrays.sort(pselist, new PreviewSizeComparator());
		for (int i = 0; i < pselist.length; i++) {
			Log.i("lll", "" + pselist[i].toString());

		}

		// step3. pick the the smallest difference value - if multiple value
		// yield to a same value, pick the one with closest width to 480/
		int k = pselist.length - 1;
		double standardHeight = 480;
		double lowestDifference = Math.abs(pselist[pselist.length - 1]
				.getHeight() - standardHeight);
		if (lowestDifference != 0) {
			for (int i = k - 1; i >= 0; i--) {
				if (pselist[i].getPri() == pselist[pselist.length - 1].getPri()) {
					double difference = Math.abs(pselist[i].getHeight()
							- standardHeight);

					if (difference < lowestDifference) {
						k = i;
						lowestDifference = difference;

						if (difference == 0)
							break;
						// else continue;
					}
					// else pass
				} else
					break;
			}
		}

		return new Pair<Double, Double>(pselist[k].getHeight(),
				pselist[k].getWidth());
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// 系统进度条调用
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.activity_main);

		// STEP 1. 由preview尺寸算出video尺寸
		myCamera = Camera.open();
		parameters = myCamera.getParameters();// 获得相机参数

		Size s = parameters.getPreviewSize();

		priview_width = s.width;
		priview_height = s.height;

		double needSize = (priview_width / priview_height);
		Pair<Double, Double> videoSize = calculateVideoSize(needSize);
		vHeight = (videoSize.first).intValue();
		vWidth = (videoSize.second).intValue();

		myCamera.release();
		myCamera = null;

		// STEP 2. 绑定控件
		// 初始化控件
		init();
		// 绑定重置事件
		DelClick();
		// 绑定合成事件
		EndClick();

		// STEP 3. 初始化音乐播放器组件
		Intent intent = this.getIntent();
		musicPath = intent.getStringExtra("path");
		mp = new MediaPlayer();

		// 初始化时间控件 使用handler来进行时间点操作
		timeControlHandler = new Handler();

		// STEP 4. 绑定拍摄事件
		ButtonListener b = new ButtonListener();
		Recorder_bt.setOnClickListener(b);
		Recorder_bt.setOnTouchListener(b);

		// 设置surfaceView不管理的缓冲区
		//surfaceView.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		// 设置surfaceView分辨率
		//surfaceView.getHolder().setFixedSize(720, 1230);
		// STEP 5. 清理log
		clearLog();

		// STEP 6. 生成输出文件夹
		// 判断sd卡是否存在
		boolean sdCardExist = Environment.getExternalStorageState().equals(
				android.os.Environment.MEDIA_MOUNTED);

		if (sdCardExist) {
			// 设定存放视频的文件夹的路径
			String path = Environment.getExternalStorageDirectory()
					.getAbsolutePath()
					+ File.separator
					+ "ShinyRing"
					+ File.separator;

			// 声明存放视频的文件夹的File对象
			videoFolder = new File(path);

			// 如果不存在此文件夹,则创建
			if (!videoFolder.exists()) {
				videoFolder.mkdirs();
			}

		} else {
			Log.i("err", "SD卡不存在");
		}

		// STEP 7. Preview回调
		// 当Surface第一次创建后会立即调用该函数。程序可以在该函数中做些和绘制界面相关的初始化工作，一般情况下都是在另外的线程来绘制界面，所以不在这个函数中绘制Surface实现预览。
		holder = surfaceView.getHolder();
		holder.addCallback(new SurfaceHolder.Callback() {

			@SuppressLint("NewApi")
			@Override
			public void surfaceCreated(SurfaceHolder holder) {
				System.out.println("------surfaceCreated------");
				// 开启相机
				if (myCamera == null) {
					myCamera = Camera.open();

					try {
						myCamera.setPreviewDisplay(holder);

						myCamera.setDisplayOrientation(90);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}

			}

			// 当Surface的状态（大小和格式）发生变化的时候会调用该函数，在surfaceCreated调用后该函数至少会被调用一次，因此在这里打开预览
			@Override
			public void surfaceChanged(SurfaceHolder holder, int format,
					int width, int height) {

				System.out.println("------surfaceChanged------");
				// 开始预览
				myCamera.startPreview();
				// 聚焦
				autoCamera();
				// 相机锁定
				myCamera.unlock();

			}

			// 当Surface被摧毁前会调用该函数，该函数被调用后就不能继续使用Surface了，一般在该函数中来清理使用的资源。
			@Override
			public void surfaceDestroyed(SurfaceHolder holder) {
				System.out.println("------surfaceDestroyed------");
				// 关闭预览并释放资源
				if (myCamera != null) {
					myCamera.stopPreview();
					myCamera.release();
					myCamera = null;
				}
			}
		});

	}

	// 系统销毁
	@Override
	protected void onDestroy() {
		timeControlHandler.removeCallbacks(timeRun);
		if (mRecorder != null) {

			mRecorder.release();
		}
		if (myCamera != null) {
			myCamera.stopPreview();
			myCamera.release();
		}
		mp.release();
		
		mRecorder =null;
		myCamera = null;
		mp = null;
		
		super.onDestroy();
	}

	// step8:触发事件录像
	class ButtonListener implements OnClickListener, OnTouchListener {

		@Override
		public void onClick(View v) {
			if (v.getId() == R.id.shining_video_recorder_bt) {
				Log.d("test", "cansal button ---> click");
			}
		}

		// 录像开始
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			if (v.getId() == R.id.shining_video_recorder_bt) {
				if (event.getAction() == MotionEvent.ACTION_DOWN) {

					
					
					if (!recording) {
						try {
							// 关闭预览并释放资源
							deltime = 0;
                      
							// 第一次播放音乐时reset组件
							if (musicNotStarted) {
								mp.reset();
								mp.setDataSource(musicPath);
								mp.prepare();

							}

							// 重置media recorder
							mRecorder = new MediaRecorder();
							mRecorder.reset();
							mRecorder.setCamera(myCamera);
							
							// 创建输出文件 - 为ShinyRing下面的总文件数目+1
							File f = new File( Environment.getExternalStorageDirectory()+"/ShinyRing/");
							File[] files=f.listFiles();
							VideoCount = files.length;
							int op = VideoCount + 1;
							fs = sdcardPath + "sr" + op + ".mp4";
							videoFile = new File(videoFolder.getAbsoluteFile()
									+ File.separator + "sr" + op + ".mp4");
							videoFile.createNewFile();
							


							//Log.i("opop", VideoCount+"opop");
							// 获取当前时间,作为视频文件的文件名
							// String nowTime = java.text.MessageFormat.format(
							// "{0,date,yyyyMMdd_HHmmss}",
							// new Object[] { new java.sql.Date(System
							// .currentTimeMillis()) });
							time_tv.setVisibility(View.VISIBLE); // 设置文本框可见
							timeControlHandler.post(timeRun); // 调用Runable
							
							

							mRecorder.setPreviewDisplay(surfaceView.getHolder()
									.getSurface()); // 预览
							mRecorder
									.setVideoSource(MediaRecorder.VideoSource.CAMERA); // 视频源
							mRecorder.setOrientationHint(90);
							mRecorder
									.setAudioSource(MediaRecorder.AudioSource.MIC); // 录音源为麦克风
							mRecorder
									.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4); // 输出格式为mp4
							// 详细配置时修改以下代码
							/* 引用android.util.DisplayMetrics 获取分辨率 */
							// DisplayMetrics dm = new DisplayMetrics();
							// getWindowManager().getDefaultDisplay().getMetrics(dm);
							mRecorder.setVideoSize(vWidth, vHeight); // 视频尺寸//800
																		// 480

							mRecorder.setVideoFrameRate(25); // 视频帧频率

							mRecorder.setVideoEncodingBitRate(1 * 1024 * 1024); // 比特率设置
							mRecorder
									.setVideoEncoder(MediaRecorder.VideoEncoder.H264); // 视频编码
							mRecorder
									.setAudioEncoder(MediaRecorder.AudioEncoder.AAC); // 音频编码//原ARM_NB
							mRecorder.setMaxDuration(3000000);// 原1800000
							mRecorder.setOutputFile(videoFile.getAbsolutePath());
							mRecorder.prepare(); // 准备录像

							
							mRecorder.start(); // 开始录像

							mp.start(); // start audio play

							recording = true; // 改变录制状态为正在录制
							
							// 延时防止Recorder组件崩溃 使用Button机制时 可以去除 touch时必须添加
							try {
								Thread.sleep(500);
							} catch (InterruptedException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}

						} catch (IOException e1) {
							e1.printStackTrace();
						} catch (IllegalStateException e) {
							e.printStackTrace();
						}
					} else {
						// 录制中重新出发录制
						Toast.makeText(MainVideo.this, "视频正在录制中...",
								Toast.LENGTH_LONG).show();
					}

				}

				// 松开停止录像
				if (event.getAction() == MotionEvent.ACTION_UP) {

					if (recording) {
						
						mp.pause();
						writeLog("Stop At:"+mp.getCurrentPosition()+'\n');
						
						
						mRecorder.stop();
						mRecorder.release();
						
						addList();
						firstBar.setSecondaryProgress(firstBar.getProgress() - 10);
						int op = VideoCount + 1;
						nowtime = mp.getCurrentPosition();
						timeControlHandler.removeCallbacks(timeRun);
						time_tv.setVisibility(View.GONE);
						musicNotStarted = false;
						recording = false;
						flag = false;
						int ftime = 15 - time;
						Toast.makeText(MainVideo.this, "您还可以拍摄" + ftime + "秒",
								Toast.LENGTH_LONG).show();
						
					}
					// 释放摄像头
					if (myCamera != null) {
						myCamera.stopPreview();
						myCamera.release();
						myCamera = null;
					}

					try {
						Thread.sleep(500);
					} catch (InterruptedException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}

					// 开启相机
					if (myCamera == null) {
						myCamera = Camera.open();
						try {
							myCamera.setPreviewDisplay(holder);
							myCamera.setDisplayOrientation(90);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					myCamera.startPreview(); // 开启预览
					autoCamera();
					myCamera.unlock();

				}

			}
			return false;
		}

	}

	// 初始化
	public void init() {
		surfaceView = (SurfaceView) findViewById(R.id.surfaceview);
		Recorder_bt = (Button) findViewById(R.id.shining_video_recorder_bt);
		Pause_bt = (Button) findViewById(R.id.shining_video_stop_bt);
		Reset_bt = (Button) findViewById(R.id.shining_video_reset_bt);
		time_tv = (TextView) findViewById(R.id.time);
		framelayout = (FrameLayout) findViewById(R.id.shining_video_fl);
		

		WindowManager wm = this.getWindowManager();
		widths = wm.getDefaultDisplay().getWidth();
		int heights = wm.getDefaultDisplay().getHeight();
		Log.i("lll", "ws" + widths);
		Log.i("lll", "hs" + heights);

		if (priview_width > priview_height) {
			realh = (priview_width / priview_height);

		} else if (priview_height > priview_width) {
			realh = (priview_height / priview_width);
		}

		//当前只考虑surfaceview height > width情况，基于长宽比和width算出height(realh)
		int sh = (int) (realh * widths);

		// surfaceview大小
		LayoutParams lp = surfaceView.getLayoutParams();
		lp.width = widths;
		lp.height = sh;
		surfaceView.setLayoutParams(lp);

		int fh = heights - widths;

		RelativeLayout.LayoutParams linearParams = (RelativeLayout.LayoutParams) framelayout
				.getLayoutParams(); // 取控件textView当前的布局参数
		linearParams.height = fh - getStatusBarHeight();// 控件的高强制设成xx

		linearParams.width = widths;// 控件的宽强制设成xx

		framelayout.setLayoutParams(linearParams); // 使设置好的布局参数应用到控件</pre>

		// 根据控件的ID来取得代表控件的对象
		firstBar = (ProgressBar) findViewById(R.id.firstBar);

		// 更改此进度条不确定模式。在不确定模式，进度被忽略，进度条显示了无限的动画代替
		firstBar.setIndeterminate(false);

		String sdcard = Environment.getExternalStorageDirectory().getPath();
		StatFs statFs = new StatFs(sdcard);
		long blockSize = statFs.getBlockSize();
		long blocks = statFs.getAvailableBlocks();
		long availableSpare = (blocks * blockSize) / (1024 * 1024);

		// SDCARD检查 防止相机无法打开时没有提示
		if (availableSpare < 5) {
			Toast.makeText(MainVideo.this, "SDCard容量低于5mb建议您清理一下哒~",
					Toast.LENGTH_LONG).show();

		}

	}

	// 重置
	public void ResetFile() {

		// 删除时如果存在第2进度条根据录像时间去除对象进度条长度并且删除文件
		// String fs = lf.get(lf.size() - 1).getPath();
		// int del = lf.get(lf.size() - 1).getTimes();
		// lf.remove(lf.get(lf.size() - 1));
		// File file = new File(fs);
		// int delbar = del * 7;
		// time = time - del;
		//
		// Log.i("psl", fs + "path" + deltime + "time");
		// if (file.isFile()) {
		// file.delete();
		// firstBar.setProgress(firstBar.getProgress() - delbar);
		// } else {
		// Toast.makeText(MainVideo.this, "没有可删除的视频", Toast.LENGTH_LONG)
		// .show();
		// }
		//
		// file.exists();
//		File file;
//		for (int i = 0; i < list.size(); i++) {
//			String videoPath = list.get(i).toString();
//			file = new File(videoPath);
//			if (file.exists()) {
//				file.delete();
//			}
//		}
		recording = false;
		flag = false;
		mp.release();
		mp=null;
		myCamera.stopPreview();
		myCamera.release();
		myCamera = null;
		this.finish();
	}

	// 删除事件
	private void DelClick() {

		this.Reset_bt.setOnClickListener(new ReButtonListener());

	}

	class ReButtonListener implements OnClickListener {

		@Override
		public void onClick(View v) {

			secondDel();

		}
	}

	// 合成
	private void EndClick() {

		this.Pause_bt.setOnClickListener(new ReButtonListener2());

	}

	class ReButtonListener2 implements OnClickListener {

		@Override
		public void onClick(View v) {
			String str = "";
			if (list.size() > 1) {
				for (int i = 0; i < list.size(); i++) {
					str += list.get(i) + "|";
				}
				str = str.substring(0, str.lastIndexOf("|"));
				Toast.makeText(MainVideo.this, str + "", Toast.LENGTH_LONG)
						.show();
				testJNIapi.SynthesisVideo(str, sdcardPath + "xxx.mp4");

				testJNIapi.cropVideo(sdcardPath + "xxx.mp4", vHeight + "");

				testJNIapi.ReplaceAudio(sdcardPath + "xxx.mp4", musicPath);
			} else {
				str = list.get(0).toString();
				File file = new File(str);
				file.renameTo(new File(sdcardPath + "xxx.mp4"));
				testJNIapi.cropVideo(sdcardPath + "xxx.mp4", vHeight + "");
				testJNIapi.ReplaceAudio(sdcardPath + "xxx.mp4", musicPath);
			}
			recording = false;
			flag = false;
			myCamera.stopPreview();
			myCamera.release();
			myCamera = null;
			mp.release();
			mp = null;
			Toast.makeText(MainVideo.this, "请到sd卡根目录shinyring文件下查看xxx.mp4",
					Toast.LENGTH_SHORT).show();
		}
	}

	// 自动聚焦
	public void autoCamera() {

		try {
			Camera.Parameters myParam = myCamera.getParameters();
			myParam.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);

			for (int i = 0; i < myParam.getSupportedFocusModes().size(); i++) {
				Log.i("bnn", myParam.getSupportedFocusModes().get(i) + "");
			}

			myCamera.setParameters(myParam);
		} catch (Exception e) {

			Toast.makeText(MainVideo.this, "手机不支持", Toast.LENGTH_LONG).show();

		}

	}

	// 状态栏高度获取
	public int getStatusBarHeight() {
		int result = 0;
		int resourceId = getResources().getIdentifier("status_bar_height",
				"dimen", "android");
		if (resourceId > 0) {
			result = getResources().getDimensionPixelSize(resourceId);
		}
		return result;
	}

	// 日志生产
	public void writeLog(String s) {

		try{
			File file = new File("sdcard/com.log.ShinyRingVideo.txt");
			
			if(!file.exists())
			{
				file.createNewFile();
			}
		
			FileWriter fw = new FileWriter(file,true);
			/*
			char[] prefix= {'s','t','o','p',' ','a','t',':'};
			for(int i=0;i<prefix.length;i++)
			{
				fw.append(prefix[i]);
			}*/
			
			for(int i=0;i<s.length();i++)
			{	
				fw.append(s.charAt(i));
			}
			
			fw.close();
			
		}catch(IOException ioe)
		{
			ioe.printStackTrace();
		}
	}

	
	//清log
	public void clearLog()
	{
		File file = new File("sdcard/com.log.ShinyRingVideo.txt");
			
		if(file.exists())
			file.delete();
			
		
	}
	
	// 测试用方法
	public void getPatch(String f, String s) {
		Log.i("sssd", f + "");
		Log.i("sssd", s + "");
	}

	// 添加录像文件
	public void addList() {

		// FileElement fe = new FileElement(fs, deltime);
		// lf.add(fe);
		list.add(fs);

	}

	// 双击重置
	public void secondDel() {

		if ((System.currentTimeMillis() - clickTime) > 2000) {
			Toast.makeText(getApplicationContext(), "再按一次重置",
					Toast.LENGTH_SHORT).show();
			clickTime = System.currentTimeMillis();
		}

		else {

			clickTime = 0;
			clearLog();
			ResetFile();
		}

	}

	// 时间转换
	public static String getFormatedDateTime(String pattern, long dateTime) {
		SimpleDateFormat sDateFormat = new SimpleDateFormat(pattern);
		return sDateFormat.format(new Date(dateTime + 0));
	}

}
