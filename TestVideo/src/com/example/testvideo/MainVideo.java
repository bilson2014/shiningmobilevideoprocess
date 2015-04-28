package com.example.testvideo;

import java.io.File;
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
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.Parameters;
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
import com.example.tools.FileElement;
import com.example.tools.PreviewSizeComparator;
import com.example.tools.PreviewSizeElement;

@SuppressLint("NewApi")
public class MainVideo extends Activity {
	private SurfaceView surfaceView;
	private Button luXiang_bt, tingZhi_bt;
	private TextView time_tv; // 显示时间的文本框
	private MediaRecorder mRecorder;
	private boolean recording; // 记录是否正在录像,fasle为未录像, true 为正在录像
	private File videoFolder, videFile; // 存放视频的文件夹:视频文件
	private Handler timeControlHandler;
	private int time, count, deltime; // 时间
	private Camera myCamera; // 相机声明
	private SurfaceHolder holder;
	private ProgressBar firstBar = null;
	private Boolean flag = true;
	private String path;
	private MediaPlayer mp = new MediaPlayer();
	private int nowtime;
	private boolean times = true;
	private Button btn, delbtn;
	private String demo = "mm:ss", fs, ss;
	private static String SDPATH = "/sdcard/shinyring/";
	private File f;
	private File[] files;
	private FrameLayout framelayout;
	private AutoFocusCallback myAutoFocusCallback = null;
	private int widths, rh, vHeight, vWidth;
	private double realh, w, h;
	private Camera.Parameters parameters;
	private PreviewSizeElement[] pselist;
	private List<FileElement> lf = new ArrayList<FileElement>();
	private List list = new ArrayList();
	private long clickTime = 0;
	private int cameraIndex = 0;
	private testJNIapi tjni = new testJNIapi();
	final static String sdcardPath = Environment.getExternalStorageDirectory()
			+ "/ShinyRing/";
	// private TextView tv;

	// private FrameLayout.LayoutParams flp;
	// private Camera.Parameters parameters = myCamera.getParameters();

	/**
	 * 录制过程中,时间变化
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
				// 设置第二进度条的当前值
				//firstBar.setSecondaryProgress(firstBar.getProgress() + 10);
				// 因为默认的进度条无法显示进行的状态
				// secondBar.setProgress(i);
				//Log.i("bar", "" + firstBar.getProgress());

				if (firstBar.getProgress() >= 100) {
					Log.i("bar", "下载完毕");
					flag = false;
					//nowtime = mp.getCurrentPosition();
					//String f = "/sdcard/shinyring/sr" + count + ".mp4";
					//String s = "/sdcard/shinyring/sr" + count + ".mp4";
					//Log.i("TTT", s);
					//getPatch(f, s);

					mRecorder.stop();
					mRecorder.release();
					timeControlHandler.removeCallbacks(timeRun);
					luXiang_bt.setEnabled(false);
					//tingZhi_bt.setEnabled(false);
					firstBar.setVisibility(View.GONE);
					mp.stop();
					mp.release();
					time_tv.setText(time + "秒视频录制完毕");
					addList();
					
					
					//long currentTimeMillis = System.currentTimeMillis();

					//Log.i("hh", "" + getFormatedDateTime(demo, nowtime));// 00:15
					//Log.i("oo", "" + nowtime);// 15380
					//Log.i("mm", "" + currentTimeMillis);// 现在的时间戳
					// finish();

				}
			}

		}
	};

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
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.activity_main);

		
		//STEP 1. 由preview尺寸算出video尺寸
		myCamera = Camera.open();
		parameters = myCamera.getParameters();// 获得相机参数

		// Size s = parameters.getPictureSize();
		Size s = parameters.getPreviewSize();
		// parameters.setPreviewSize(480, 864);

		w = s.width;
		h = s.height;

		//Log.i("lll", w + "w");
		//Log.i("lll", h + "h");

		double needSize = (w / h);

		Pair<Double, Double> videoSize = calculateVideoSize(needSize);

		vHeight = (videoSize.first).intValue();
		vWidth = (videoSize.second).intValue();

		//Log.i("lll", vHeight + "vh");
		//Log.i("lll", vWidth + "vw");

		myCamera.release();
		myCamera = null;

		//STEP 2. 绑定控件
		// 初始化控件
		init();
		// 绑定重置事件
		DelClick();
		// 绑定合成事件
		EndClick();
		// 初始化时间控件
		timeControlHandler = new Handler();

		
		//STEP 3. 初始化音乐播放器
		Intent intent = this.getIntent();
		path = intent.getStringExtra("path");

		//Log.i("sss", path + "path");

		mp = new MediaPlayer();

		
		
		//STEP 4. 绑定拍摄事件
		ButtonListener b = new ButtonListener();
		luXiang_bt.setOnClickListener(b);
		luXiang_bt.setOnTouchListener(b);
		
		
		//STEP 5. 设置SurfaceView参数
		holder = surfaceView.getHolder();
		// 设置surfaceView不管理的缓冲区
		surfaceView.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		// 设置surfaceView分辨率
		surfaceView.getHolder().setFixedSize(720, 1230);

		
		
		//STEP 6. 生成输出文件夹
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

			
		}
		else {
			Log.i("err", "SD卡不存在");
		}

		
		
		//STEP 7. Preview回调
		holder.addCallback(new SurfaceHolder.Callback() {

			@SuppressLint("NewApi")
			@Override
			public void surfaceCreated(SurfaceHolder holder) {
				System.out.println("------surfaceCreated------");
				// 开启相机
				if (myCamera == null) {
					myCamera = Camera.open();

					Parameters parameters = myCamera.getParameters();
					//
					// List<Size> previewSizes = parameters
					// .getSupportedPreviewSizes();
					// for (int i = 0; i < previewSizes.size(); i++) {
					// Size size = previewSizes.get(i);
					// Log.i("ttt", "previewSizes:width = " + size.width
					// + " height = " + size.height);
					// }

					try {
						myCamera.setPreviewDisplay(holder);

						myCamera.setDisplayOrientation(90);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}

			}

			@Override
			public void surfaceChanged(SurfaceHolder holder, int format,
					int width, int height) {

				// Camera.Parameters parameters = myCamera.getParameters();//
				// 获得相机参数
				// Size s = parameters.getPictureSize();
				// double w = s.width;
				// double h = s.height;
				//
				// // int ss = (int)(height*(w/h));
				//
				// Toast.makeText(MainVideo.this, w+"",
				// Toast.LENGTH_LONG).show();
				//
				//
				//
				//
				// if( width>height )
				// {
				// surfaceView.setLayoutParams(new LinearLayout.LayoutParams(
				// (int)(height*(w/h)), height) );
				// }
				// else
				// {
				// surfaceView.setLayoutParams(new LinearLayout.LayoutParams(
				// width, (int)(width*(h/w)) ) );
				// }
				//
				// Parameters parameters = myCamera.getParameters();
				System.out.println("------surfaceChanged------");
				// 开始预览
				myCamera.startPreview();
				autoCamera();
				myCamera.unlock();
				// 聚焦
				// myCamera.autoFocus(myAutoFocusCallback);
				Log.i("auto", "auto");
				// autoCamera();

			}

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

	// 初始化
	public void init() {
		surfaceView = (SurfaceView) findViewById(R.id.surfaceview);
		luXiang_bt = (Button) findViewById(R.id.luXiang_bt);
		tingZhi_bt = (Button) findViewById(R.id.tingZhi_bt);
	    delbtn = (Button) findViewById(R.id.del_bt);
		time_tv = (TextView) findViewById(R.id.time);
		framelayout = (FrameLayout) findViewById(R.id.fl);

		WindowManager wm = this.getWindowManager();
		widths = wm.getDefaultDisplay().getWidth();
		int heights = wm.getDefaultDisplay().getHeight();
		Log.i("lll", "ws" + widths);
		Log.i("lll", "hs" + heights);

		if (w > h) {
			realh = (w / h);

		} else if (h > w) {
			realh = (h / w);
		}

		int sh = (int) (realh * widths);
		rh = (int) (realh * 480);

		LayoutParams lp = surfaceView.getLayoutParams();
		lp.width = widths;
		lp.height = sh;
		surfaceView.setLayoutParams(lp);

		int fh = heights - widths;

		Log.i("ttt", "w" + widths);
		Log.i("ttt", "h" + heights);
		Log.i("ttt", "fh" + fh);

		RelativeLayout.LayoutParams linearParams = (RelativeLayout.LayoutParams) framelayout
				.getLayoutParams(); // 取控件textView当前的布局参数
		linearParams.height = fh - getStatusBarHeight();// 控件的高强制设成xx

		linearParams.width = widths;// 控件的宽强制设成xx

		framelayout.setLayoutParams(linearParams); // 使设置好的布局参数应用到控件</pre>

		f = new File("/sdcard/shinyring");

		// 根据控件的ID来取得代表控件的对象
		firstBar = (ProgressBar) findViewById(R.id.firstBar);

		// 更改此进度条不确定模式。在不确定模式，进度被忽略，进度条显示了无限的动画代替
		firstBar.setIndeterminate(false);

		String sdcard = Environment.getExternalStorageDirectory().getPath();
		StatFs statFs = new StatFs(sdcard);
		long blockSize = statFs.getBlockSize();
		long blocks = statFs.getAvailableBlocks();
		long availableSpare = (blocks * blockSize) / (1024 * 1024);
		Log.d("剩余空间", "availableSpare = " + availableSpare);

		if (availableSpare < 5) {
			Toast.makeText(MainVideo.this, "SDCard容量低于5mb建议您清理一下哒~",
					Toast.LENGTH_LONG).show();

		}

	}

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
		super.onDestroy();
	}

	class ButtonListener implements OnClickListener, OnTouchListener {

		public void onClick(View v) {
			if (v.getId() == R.id.luXiang_bt) {
				Log.d("test", "cansal button ---> click");
			}
		}

		public boolean onTouch(View v, MotionEvent event) {
			if (v.getId() == R.id.luXiang_bt) {
				if (event.getAction() == MotionEvent.ACTION_DOWN) {

					if (!recording) {
						try {
							// 关闭预览并释放资源
							deltime = 0;

							// if (myCamera != null) {
							// // myCamera.stopPreview();
							// myCamera.release();
							// myCamera = null;
							// }

							// myCamera = Camera.open();
							//
							// 设置摄像头预览顺时针旋转90度，才能使预览图像显示为正确的，而不是逆时针旋转90度的
							//
							// myCamera.setDisplayOrientation(90);
							// // parameters.setPreviewSize(480, 480);
							// myCamera.startPreview(); //开启预览
							// // autoCamera();
							//
							// myCamera.unlock();

							if (times != false) {
								mp.reset();
								mp.setDataSource(path);
								mp.prepare();
								// mp.start();
							} else {
								// mp.start();
							}

							mRecorder = new MediaRecorder();
							mRecorder.reset();

							mRecorder.setCamera(myCamera);

							files = f.listFiles();
							count = files.length;
							// 获取当前时间,作为视频文件的文件名
							String nowTime = java.text.MessageFormat.format(
									"{0,date,yyyyMMdd_HHmmss}",
									new Object[] { new java.sql.Date(System
											.currentTimeMillis()) });
							// 声明视频文件对象
							int op = count + 1;
							time_tv.setVisibility(View.VISIBLE); // 设置文本框可见
							timeControlHandler.post(timeRun); // 调用Runable
							recording = true; // 改变录制状态为正在录制
							fs = sdcardPath + "sr" + op + ".mp4";

							videFile = new File(videoFolder.getAbsoluteFile()
									+ File.separator + "sr" + op + ".mp4");
							// 创建此视频文件
							videFile.createNewFile();
							// System.out.println("闪铃: "
							// + videFile.getAbsolutePath());

							mRecorder.setPreviewDisplay(surfaceView.getHolder()
									.getSurface()); // 预览
							// mRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));

							mRecorder
									.setVideoSource(MediaRecorder.VideoSource.CAMERA); // 视频源
							mRecorder.setOrientationHint(90);
							mRecorder
									.setAudioSource(MediaRecorder.AudioSource.MIC); // 录音源为麦克风
							mRecorder
									.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4); // 输出格式为mp4
							/* 引用android.util.DisplayMetrics 获取分辨率 */
							// DisplayMetrics dm = new DisplayMetrics();
							// getWindowManager().getDefaultDisplay().getMetrics(dm);
							mRecorder.setVideoSize(vWidth, vHeight); // 视频尺寸//800
																		// 480

							mRecorder.setVideoFrameRate(25); // 视频帧频率
							// mRecorder.setVideoEncodingBitRate(5 * 1024 *
							// 1024);
							mRecorder.setVideoEncodingBitRate(1 * 1024 * 1024);
							mRecorder
									.setVideoEncoder(MediaRecorder.VideoEncoder.H264); // 视频编码
							mRecorder
									.setAudioEncoder(MediaRecorder.AudioEncoder.AAC); // 音频编码//原ARM_NB
							mRecorder.setMaxDuration(3000000);// 原1800000
							mRecorder.setOutputFile(videFile.getAbsolutePath());
							mRecorder.prepare(); // 准备录像

							mRecorder.start(); // 开始录像

							mp.start(); // start audio play
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
						Toast.makeText(MainVideo.this, "视频正在录制中...",
								Toast.LENGTH_LONG).show();
					}

				}
				if (event.getAction() == MotionEvent.ACTION_UP) {

					Log.i("bnn", "bnnnnnnnnnnnnnnnnnnnnnnnn了");
					if (recording) {

						addList();
						firstBar.setSecondaryProgress(firstBar.getProgress() - 10);
						int op = count + 1;
						// fs = "sr" + op + ".mp4";
						// ss = "/sdcard/shinyring/sr" + count + ".mp4";
						// getPatch(fs, ss);
						mp.pause();
						nowtime = mp.getCurrentPosition();
						Log.i("oo", "" + nowtime);
						// mRecorder.stop();
						// mRecorder.release();
						timeControlHandler.removeCallbacks(timeRun);
						time_tv.setVisibility(View.GONE);
						int videoTimeLength = time;
						times = false;
						// time = 0;
						recording = false;
						flag = false;
						int ftime = 15 - time;
						Toast.makeText(MainVideo.this, "您还可以拍摄" + ftime + "秒",
								Toast.LENGTH_LONG).show();
						mRecorder.stop();
						mRecorder.release();
					}

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

	// 删除视频
	public void delFile() {

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
		File file;
		for (int i = 0; i < list.size(); i++) {
			String videoPath = list.get(i).toString();
			file = new File(videoPath);
			if (file.exists()) {
				file.delete();
			}
		}
		this.finish();
	}

	// 删除事件
	private void DelClick() {

		this.delbtn.setOnClickListener(new ReButtonListener());

	}

	class ReButtonListener implements OnClickListener {

		@Override
		public void onClick(View v) {

			Log.i("tme", deltime + "");
			// String p = "sr93.mp4";

			secondDel();

		}
	}

	// 合成
	private void EndClick() {

		this.tingZhi_bt.setOnClickListener(new ReButtonListener2());

	}

	class ReButtonListener2 implements OnClickListener {

		@Override
		public void onClick(View v) {
			String str = "";
			if(list.size()>1)
			{
				for (int i = 0; i < list.size(); i++) {
					str += list.get(i) + "|";
				}
				str = str.substring(0, str.lastIndexOf("|"));
				Toast.makeText(MainVideo.this, str + "", Toast.LENGTH_LONG).show();
				tjni.SynthesisVideo(str, sdcardPath + "xxx.mp4");

				tjni.cropVideo(sdcardPath + "xxx.mp4", vHeight + "");

				tjni.ReplaceAudio(sdcardPath + "xxx.mp4", path);
			}else
			{
				str=list.get(0).toString();
				File file=new File(str);
				file.renameTo(new File(sdcardPath + "xxx.mp4"));
				tjni.cropVideo(sdcardPath + "xxx.mp4", vHeight + "");

				tjni.ReplaceAudio(sdcardPath + "xxx.mp4", path);
			}
			recording = false;
			flag = false;
			
			myCamera.stopPreview();
			myCamera.release();
			myCamera=null;
			
			
			Toast.makeText(MainVideo.this, "请到sd卡根目录shinyring文件下查看xxx.mp4", Toast.LENGTH_SHORT).show();
		}
	}

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

	// 状态栏高度
	public int getStatusBarHeight() {
		int result = 0;
		int resourceId = getResources().getIdentifier("status_bar_height",
				"dimen", "android");
		if (resourceId > 0) {
			result = getResources().getDimensionPixelSize(resourceId);
		}
		return result;
	}

	public void createLog() {

		File file = new File("sdcard/com.log.ShinyRingVideo");
		file.mkdirs();
	}

	// 测试方法
	public void getPatch(String f, String s) {
		Log.i("sssd", f + "");
		Log.i("sssd", s + "");
	}

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
			delFile();
		}

	}

	public static String getFormatedDateTime(String pattern, long dateTime) {
		SimpleDateFormat sDateFormat = new SimpleDateFormat(pattern);
		return sDateFormat.format(new Date(dateTime + 0));
	}

}
