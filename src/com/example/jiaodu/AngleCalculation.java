package com.example.jiaodu;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.List;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class AngleCalculation extends Activity implements OnClickListener, BDLocationListener {

	private final String TAG = "AngleCalculation";
	private LocationManager locationManager;
	private TextView locallongitude;
	private TextView locallatitude;
	private TextView localaltitude;
	private TextView localaccuracy;
	private TextView localtracking;
	private TextView otherlongitude;
	private TextView otherlatitude;
	private TextView otheraltitude;
	private TextView otheraccuracy;
	private TextView othertracking;
	private EditText otherphone;
	private EditText otherip;
	private TextView azimuth;
	private TextView elevation;
	private DatagramSocket socket;
	private LocationClient mLocationClient = null;
	private final int updata = 1000;
	private LocalBroadcastManager localBroadcastManager;
	private PendingIntent sentPI;
	private PendingIntent deliverPI;
	public static String iphone;
	private MyBroadcastReceiver myBroadcastReceiver = new MyBroadcastReceiver();
	private RxBroadcastReceiver rxBroadcastReceiver = new RxBroadcastReceiver();
	private TxBroadcastReceiver txBroadcastReceiver = new TxBroadcastReceiver();

	private class MyBroadcastReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			Log.v(TAG, "MyBroadcastReceiver-->" + intent.getStringExtra(AngleCalculationApplication.intentData));
			if (AngleCalculationApplication.updateGPS.equals(action)) {
				String str = intent.getStringExtra(AngleCalculationApplication.intentData);
				updateOther(str);
			}
		}
	}
	
	private void updateOther(String str) {
		String[] arrays = str.split(",");
		if (arrays != null && arrays.length == 5) {
			for (String s : arrays) {
				if (s.indexOf("lon") == 0) {
					otherlongitude.setText(s.split("=")[1]);
				} else if (s.indexOf("lat") == 0) {
					otherlatitude.setText(s.split("=")[1]);
				} else if (s.indexOf("alt") == 0) {
					otheraltitude.setText(s.split("=")[1]);
				} else if (s.indexOf("acc") == 0) {
					otheraccuracy.setText(s.split("=")[1]);
				} else if (s.indexOf("tra") == 0) {
					othertracking.setText(s.split("=")[1]);
				}
			}
		}
	}

	private class RxBroadcastReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context _context, Intent _intent) {
			Toast.makeText(AngleCalculation.this, "收信人已经成功接收", Toast.LENGTH_SHORT).show();
		}
	}

	private class TxBroadcastReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context _context, Intent _intent) {
			switch (getResultCode()) {
			case Activity.RESULT_OK:
				Toast.makeText(AngleCalculation.this, "短信发送成功", Toast.LENGTH_SHORT).show();
				break;
			case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
				break;
			case SmsManager.RESULT_ERROR_RADIO_OFF:
				break;
			case SmsManager.RESULT_ERROR_NULL_PDU:
				break;
			}
		}
	}

	private Handler handler = new Handler() {
		public void handleMessage(Message msg) {
			if (msg.what == updata) {
				BDLocation location = (BDLocation) msg.obj;
				locallongitude.setText(location.getLongitude() + "");
				locallatitude.setText(location.getLatitude() + "");
				if (location.getLocType() == BDLocation.TypeGpsLocation) {
					localaltitude.setText(location.getAltitude() + "");
				} else {
					localaltitude.setText("invalid");
				}
				localaccuracy.setText(location.getRadius() + "");
				int num = location.getSatelliteNumber();
				localtracking.setText(num < 0 ? "0":num + "");
			}
		};
	};

	public void sendSMS(String phoneNumber, String message) {
		Log.v(TAG, message);
		// 获取短信管理器
		android.telephony.SmsManager smsManager = android.telephony.SmsManager.getDefault();
		// 拆分短信内容（手机短信长度限制）
		List<String> divideContents = smsManager.divideMessage(message);
		for (String text : divideContents) {
			smsManager.sendTextMessage(phoneNumber, null, text, sentPI, deliverPI);
		}
	}

	/**
	 * 初始化定位
	 */
	private void initLocation() {
		mLocationClient = new LocationClient(getApplicationContext());// 声明LocationClient类
		LocationClientOption option = new LocationClientOption();
		option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);// 可选，默认高精度，设置定位模式，高精度，低功耗，仅设备
		option.setCoorType(BDLocation.BDLOCATION_GCJ02_TO_BD09LL);// 可选，默认gcj02，设置返回的定位结果坐标系
		option.setScanSpan(1000);// 可选，默认0，即仅定位一次，设置发起定位请求的间隔需要大于等于1000ms才是有效的
		option.setOpenGps(true);// 可选，默认false,设置是否使用gps
		option.setLocationNotify(true);// 可选，默认false，设置是否当gps有效时按照1S1次频率输出GPS结果
		option.setIgnoreKillProcess(false);// 可选，默认true，定位SDK内部是一个SERVICE，并放到了独立进程，设置是否在stop的时候杀死这个进程，默认不杀死
		mLocationClient.setLocOption(option);
		mLocationClient.registerLocationListener(this);
		mLocationClient.start();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		initUI();
		mLocationClient = new LocationClient(getApplicationContext());
		mLocationClient.registerLocationListener(this);
		initLocation();

		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			Toast.makeText(this, "请开启GPS导航...", Toast.LENGTH_SHORT).show();
			// 返回开启GPS导航设置界面
			Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
			startActivityForResult(intent, 0);
			finish();
			return;
		}
		initNetWork();
		localBroadcastManager = LocalBroadcastManager.getInstance(this);
	}

	@Override
	protected void onStart() {
		super.onStart();
		localBroadcastManager.registerReceiver(myBroadcastReceiver, new IntentFilter(AngleCalculationApplication.updateGPS));
		// 处理返回的发送状态
		String SENT_SMS_ACTION = "SENT_SMS_ACTION";
		Intent sentIntent = new Intent(SENT_SMS_ACTION);
		sentPI = PendingIntent.getBroadcast(this, 0, sentIntent, 0);
		registerReceiver(txBroadcastReceiver, new IntentFilter(SENT_SMS_ACTION));

		// 处理返回的接收状态
		String DELIVERED_SMS_ACTION = "DELIVERED_SMS_ACTION";
		Intent deliverIntent = new Intent(DELIVERED_SMS_ACTION);
		deliverPI = PendingIntent.getBroadcast(this, 0, deliverIntent, 0);
		registerReceiver(rxBroadcastReceiver, new IntentFilter(DELIVERED_SMS_ACTION));
	}

	@Override
	protected void onPause() {
		super.onPause();
		localBroadcastManager.unregisterReceiver(myBroadcastReceiver);
		unregisterReceiver(rxBroadcastReceiver);
		unregisterReceiver(txBroadcastReceiver);
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	private void initUI() {
		setContentView(R.layout.anglecalculation);
		locallongitude = (TextView) findViewById(R.id.locallongitude);
		locallatitude = (TextView) findViewById(R.id.locallatitude);
		localaltitude = (TextView) findViewById(R.id.localaltitude);
		localaccuracy = (TextView) findViewById(R.id.localaccuracy);
		localtracking = (TextView) findViewById(R.id.localtracking);
		otherlongitude = (TextView) findViewById(R.id.otherlongitude);
		otherlatitude = (TextView) findViewById(R.id.otherlatitude);
		otheraltitude = (TextView) findViewById(R.id.otheraltitude);
		otheraccuracy = (TextView) findViewById(R.id.otheraccuracy);
		othertracking = (TextView) findViewById(R.id.othertracking);
		otherphone = (EditText) findViewById(R.id.otherphone);
		findViewById(R.id.phonequery).setOnClickListener(this);
		findViewById(R.id.copy).setOnClickListener(this);
		findViewById(R.id.paste).setOnClickListener(this);
		otherip = (EditText) findViewById(R.id.otherip);
		findViewById(R.id.ipquery).setOnClickListener(this);
		azimuth = (TextView) findViewById(R.id.azimuth);
		elevation = (TextView) findViewById(R.id.elevation);
		findViewById(R.id.calculateAZ_EL).setOnClickListener(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case R.id.option_item1:
			Intent intent = new Intent(this, Help.class);
			startActivity(intent);
			break;
		}
		return super.onMenuItemSelected(featureId, item);
	}

	private void jisuan() {
		// 其他
		double cosAB = 0;
		double sinAB = 0;
		try {
			double lat = new BigDecimal(otherlatitude.getText().toString()).doubleValue();
			double log = new BigDecimal(otherlongitude.getText().toString()).doubleValue();
			double bLat = Math.toRadians(lat);
			double bLog = Math.toRadians(log);
			/*************************************/
			// 北京
			double bjlat = new BigDecimal(locallatitude.getText().toString()).doubleValue();
			double bjlog = new BigDecimal(locallongitude.getText().toString()).doubleValue();
			double aLat = Math.toRadians(bjlat);
			double aLog = Math.toRadians(bjlog);
			if (log - bjlog == 0 && lat - bjlat == 0) {
				System.out.println("0");
				azimuth.setText("0°");
			} else if (log - bjlog == 0 && lat - bjlat > 0) {
				System.out.println("0");
				azimuth.setText("0°");
			} else if (log - bjlog == 0 && lat - bjlat < 0) {
				System.out.println("180");
				azimuth.setText("180°");
			} else if (log - bjlog > 0 && lat - bjlat == 0) {
				System.out.println("90");
				azimuth.setText("90°");
			} else if (log - bjlog < 0 && lat - bjlat == 0) {
				System.out.println("270");
				azimuth.setText("90°");
			} else {
				cosAB = ArithmeticUtils.mul(Math.cos(ArithmeticUtils.sub(ArithmeticUtils.div(Math.PI, 2), aLat)), Math.cos(ArithmeticUtils.sub(ArithmeticUtils.div(Math.PI, 2), bLat))) + ArithmeticUtils.mul(ArithmeticUtils.mul(Math.sin(ArithmeticUtils.sub(ArithmeticUtils.div(Math.PI, 2), aLat)), Math.sin(ArithmeticUtils.sub(ArithmeticUtils.div(Math.PI, 2), bLat))), Math.cos(ArithmeticUtils.sub(bLog, aLog)));
				sinAB = Math.sqrt(ArithmeticUtils.sub(1, ArithmeticUtils.mul(cosAB, cosAB)));
				double sinBAN = ArithmeticUtils.div(ArithmeticUtils.mul(Math.sin(ArithmeticUtils.sub(ArithmeticUtils.div(Math.PI, 2), bLat)), Math.sin(ArithmeticUtils.sub(bLog, aLog))), sinAB);
				double pianjiao = Math.toDegrees(Math.asin(sinBAN));
				if (log - bjlog > 0 && lat - bjlat > 0) {// 第一象限
					pianjiao = pianjiao;
				} else if (log - bjlog > 0 && lat - bjlat < 0) {// 第二象限
					pianjiao = 180 - pianjiao;
				} else if (log - bjlog < 0 && lat - bjlat < 0) {// 第三象限
					pianjiao = 180 - pianjiao;
				} else if (log - bjlog < 0 && lat - bjlat > 0) {// 第四象限
					pianjiao = 360 + pianjiao;
				}

				System.out.println("偏角：   " + pianjiao);
				azimuth.setText(pianjiao + "°");
			}
		} catch (NumberFormatException e) {
			e.printStackTrace();
		}

		try {
			double banjing = 6377830;
			double a = new BigDecimal(localaltitude.getText().toString()).doubleValue();// 海拔
			double b = new BigDecimal(otheraltitude.getText().toString()).doubleValue();// 海拔
			double ab2 = ArithmeticUtils.sub(ArithmeticUtils.add(ArithmeticUtils.mul(ArithmeticUtils.add(banjing, a), ArithmeticUtils.add(banjing, a)), ArithmeticUtils.mul(ArithmeticUtils.add(banjing, b), ArithmeticUtils.add(banjing, b))), ArithmeticUtils.mul(ArithmeticUtils.mul(2, ArithmeticUtils.add(banjing, a)), ArithmeticUtils.mul(ArithmeticUtils.add(banjing, b), cosAB)));
			double sinoba = ArithmeticUtils.mul(ArithmeticUtils.div(ArithmeticUtils.add(banjing, a), Math.sqrt(ab2)), sinAB);
			double yangjiao = Math.toDegrees(Math.asin(sinoba));

			System.out.println("仰角：    " + yangjiao);
			elevation.setText(yangjiao + "°");
		} catch (NumberFormatException e) {
			e.printStackTrace();
		}

	}

	private void initNetWork() {
		new Thread() {
			public void run() {
				try {
					socket = new DatagramSocket(8888);
					socket.setSoTimeout(5 * 1000);
				} catch (SocketException e) {
					e.printStackTrace();
				}

				// while (true) {
				// byte[] otherdata = new byte[1024];// 创建字节数组，指定接收的数据包的大小
				// DatagramPacket otherPacket = new DatagramPacket(otherdata,
				// otherdata.length);
				// try {
				// socket.receive(otherPacket);
				// } catch (Exception e) {
				// e.printStackTrace();
				// }
				// if ("getGPS".equals(new String(otherPacket.getData()))) {
				// StringBuffer sBuffer = new StringBuffer();
				// sBuffer.append("longitude:" + longitude + ",");
				// sBuffer.append("latitude:" + latitude + ",");
				// sBuffer.append("altitude:" + altitude + ",");
				// sBuffer.append("accuracy:" + accuracy);
				// byte[] data = sBuffer.toString().getBytes();
				// DatagramPacket packet = new DatagramPacket(data, data.length,
				// otherPacket.getAddress(), otherPacket.getPort());
				// try {
				// socket.send(packet);
				// } catch (IOException e) {
				// e.printStackTrace();
				// }
				// }
				// }
			};
		}.start();

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mLocationClient.stop();
	}

	private String myLocation() {
		StringBuffer sBuffer = new StringBuffer();
		sBuffer.append("lon=" + locallongitude.getText().toString());
		sBuffer.append(",lat=" + locallatitude.getText().toString());
		sBuffer.append(",alt=" + localaltitude.getText().toString());
		sBuffer.append(",acc=" + localaccuracy.getText().toString());
		sBuffer.append(",tra=" + localtracking.getText().toString());
		return sBuffer.toString();
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.phonequery) {
			iphone = otherphone.getText().toString();
			if (iphone != null && !"".equals(iphone)) {
				sendSMS(iphone, myLocation());
			} else {
				Toast.makeText(this, "手机号不能为空", Toast.LENGTH_SHORT).show();
			}
		} else if (v.getId() == R.id.ipquery) {
			final String host = otherip.getText().toString();
			if (!host.matches(new MyRegex().pattern)) {
				Toast.makeText(this, "ip地址格式不正确！", Toast.LENGTH_SHORT).show();
			} else {
				new Thread() {
					public void run() {// getGPS
						byte[] senddata = new byte[] { 'g', 'e', 't', 'G', 'P', 'S' };
						InetAddress inetAddress;
						try {
							inetAddress = InetAddress.getByName(host);
							DatagramPacket sendpacket = new DatagramPacket(senddata, senddata.length, inetAddress, 8888);
							socket.send(sendpacket);
							byte[] data = new byte[1024];
							DatagramPacket packet = new DatagramPacket(data, data.length);
							socket.receive(packet);
						} catch (UnknownHostException e1) {
							e1.printStackTrace();
						} catch (SocketException e) {
							e.printStackTrace();
						} catch (IOException e) {
							e.printStackTrace();
						}

					}
				}.start();
			}
		} else if (v.getId() == R.id.calculateAZ_EL) {
			jisuan();
		} else if (v.getId() == R.id.copy) {
			ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
	        cm.setText(myLocation());
		} else if (v.getId() == R.id.paste) {
			ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
			if (cm.getText() != null) {
				updateOther(cm.getText().toString());
			}
		}

	}

	class MyRegex {
		String pattern = "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
				+ "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";
	}

	public void onConnectHotSpotMessage(String arg0, int arg1) {

	}

	@Override
	public void onReceiveLocation(BDLocation location) {
		// 获取定位结果
		StringBuffer sb = new StringBuffer(256);
		sb.append("time : ");
		sb.append(location.getTime()); // 获取定位时间
		sb.append("\nerror code : ");
		sb.append(location.getLocType()); // 获取类型类型
		sb.append("\nlatitude : ");
		sb.append(location.getLatitude()); // 获取纬度信息
		sb.append("\nlontitude : ");
		sb.append(location.getLongitude()); // 获取经度信息
		sb.append("\naltitude : ");
		sb.append(location.getAltitude()); // 获取海拔信息
		sb.append("\nradius : ");
		sb.append(location.getRadius()); // 获取定位精准度
		sb.append("\nSatelliteNumber : ");
		sb.append(location.getSatelliteNumber()); // 获取定位精准度
		Log.i("BaiduLocationApiDem", sb.toString());
		// GPS定位结果
		Message msg = handler.obtainMessage();
		msg.obj = location;
		msg.what = updata;
		handler.sendMessage(msg);

	}
}
