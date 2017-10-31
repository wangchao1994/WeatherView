package com.android.topview;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationClientOption.AMapLocationMode;
import com.amap.api.location.AMapLocationClientOption.AMapLocationProtocol;
import com.amap.api.location.AMapLocationListener;
import com.android.topview.bean.Cityweather;
import com.android.topview.tool.NetCheckUtil;
import com.android.topview.view.ExpandView;

public class WeatherView extends RelativeLayout implements OnClickListener {

	private boolean isFirstIn = false;//判断用户是否是第一次进入
	/**
	 * View
	 * */
	private TextView tv_view_title_open;
	private ImageView iv_view_weather_image;
	private TextView tv_view_city_name;
	private TextView tv_view_weather_info;
	private TextView tv_view_weather_date;
	private TextView tv_view_current_temp;
	private TextView tv_view_top_temp;
	private TextView tv_view_bottom_temp;
	private TextView tv_view_line;
	private View view;
	//	private View future_view;
	private TextView tv_firstIn_fail_info;
	private RelativeLayout rl_main_net_weather_info;
	private TextView tv_future_fail_info;
	private ExpandView expandView;
	/**
	 * location
	 * */
	//	private LocationManager locationManager;
	private URL weatherUrl; //需要传入的URL

	private AMapLocationClientOption mLocationOption = null;
	private AMapLocationClient locationClient = null;

	private SharedPreferences sp;
	private RecyclerView recycleview;
	private RecycleViewInfoAdapter  recycleViewAdapter;
	private Editor edit;
	@SuppressWarnings("unused")
	private RelativeLayout ll_future;
	@SuppressLint("HandlerLeak")
	private Handler handler = new Handler(){
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case 0x01:
				tv_firstIn_fail_info.setVisibility(View.VISIBLE);
				rl_main_net_weather_info.setVisibility(View.INVISIBLE);
				break;

			case 0x02:
				//预报天气视图打开
				if(expandView.isExpand()){
					//网络不可用 从本地直接读取主View视图信息
					recycleview.setVisibility(View.GONE);
					tv_future_fail_info.setVisibility(View.VISIBLE);
					tv_future_fail_info.setText(R.string.ll_future_info);
					Toast.makeText(getContext(), "please check network", Toast.LENGTH_SHORT).show();
				}
				//没有网络链接 直接从本地获取数据
				ReadDataToUpdateUI();

				break;
			case 0x03:

				//当有网络可用 /  用户手动点击

				initData(getContext());

				//如果本地存储有数据，直接读本地数据
				ReadDataToUpdateUI();
				break;
			case 0x04:
				if(expandView.isEnabled() && recycleview.getVisibility() == View.GONE && tv_future_fail_info.getVisibility() == View.VISIBLE){
					//						ll_future.setVisibility(View.VISIBLE);
					recycleview.setVisibility(View.VISIBLE);
					tv_future_fail_info.setVisibility(View.GONE);
				}

				if(rl_main_net_weather_info.getVisibility() == View.INVISIBLE && tv_firstIn_fail_info.getVisibility() == View.VISIBLE){
					rl_main_net_weather_info.setVisibility(View.VISIBLE);
					tv_firstIn_fail_info.setVisibility(View.GONE);
				}

			default:
				break;
			}

		};
	};


	/**
	 * 预报天气对象
	 * */
	private  List<Cityweather> weather_list;

	int[] weatherpicture = new int[] { R.drawable.leftscreen_org3_ww0,
			R.drawable.leftscreen_org3_ww1, R.drawable.leftscreen_org3_ww2, R.drawable.leftscreen_org3_ww3,
			R.drawable.leftscreen_org3_ww4, R.drawable.leftscreen_org3_ww5, R.drawable.leftscreen_org3_ww6,
			R.drawable.leftscreen_org3_ww7, R.drawable.leftscreen_org3_ww8, R.drawable.leftscreen_org3_ww9,
			R.drawable.leftscreen_org3_ww10, R.drawable.leftscreen_org3_ww11, R.drawable.leftscreen_org3_ww12,
			R.drawable.leftscreen_org3_ww13, R.drawable.leftscreen_org3_ww14, R.drawable.leftscreen_org3_ww15,
			R.drawable.leftscreen_org3_ww16, R.drawable.leftscreen_org3_ww17, R.drawable.leftscreen_org3_ww18,
			R.drawable.leftscreen_org3_ww19, R.drawable.leftscreen_org3_ww20, R.drawable.leftscreen_org3_ww21,
			R.drawable.leftscreen_org3_ww22, R.drawable.leftscreen_org3_ww23, R.drawable.leftscreen_org3_ww24,
			R.drawable.leftscreen_org3_ww25, R.drawable.leftscreen_org3_ww26, R.drawable.leftscreen_org3_ww27,
			R.drawable.leftscreen_org3_ww28

	};

	/**
	 * 采用枚举类型
	 * 便于进行属性添加
	 * */
	enum weatherType {
		weather;
	}


	public WeatherView(Context context) {
		super(context);
		initView(context);
	}


	public WeatherView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initView(context);
	}


	public WeatherView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		// TODO Auto-generated constructor stub
		initView(context);
	}

	/**
	 * 
	 * 初始化页面布局
	 * */

	private void initView(final Context context) {
		sp = context.getSharedPreferences("city_data", Context.MODE_PRIVATE);
		edit = sp.edit();
		view = LayoutInflater.from(context).inflate(R.layout.leftscreen_weather_layout, this);
		tv_view_title_open = (TextView) view.findViewById(R.id.tv_viewOpen);
		iv_view_weather_image = (ImageView) view.findViewById(R.id.iv_view_weather_image);
		tv_view_city_name = (TextView) view.findViewById(R.id.tv_view_city_name);
		tv_view_weather_info = (TextView) view.findViewById(R.id.tv_view_weather_info);
		tv_view_weather_date = (TextView) view.findViewById(R.id.tv_view_weather_date);
		tv_view_current_temp = (TextView) view.findViewById(R.id.tv_view_current_temp);
		tv_view_top_temp = (TextView) view.findViewById(R.id.tv_view_top_temp);
		tv_view_line = (TextView) view.findViewById(R.id.tv_view_line);
		tv_view_bottom_temp = (TextView) view.findViewById(R.id.tv_view_bottom_temp);
		tv_firstIn_fail_info = (TextView) view.findViewById(R.id.tv_firstIn_fail_info);
		tv_firstIn_fail_info.setOnClickListener(this);
		rl_main_net_weather_info = (RelativeLayout) view.findViewById(R.id.rl_main_net_weather_info);
		//title 开关   设置页面展开的监听
		tv_view_title_open.setOnClickListener(this);
		expandView = (ExpandView) view.findViewById(R.id.expandView);
		expandView.setContentView();
		expandView.setClickable(true);


		View view_weather_future = LayoutInflater.from(context).inflate(R.layout.leftscreen_weather_future_layout, this);
		tv_future_fail_info = (TextView) view_weather_future.findViewById(R.id.tv_future_fail_info);
		ll_future = (RelativeLayout) view_weather_future.findViewById(R.id.ll_future);
		recycleview = (RecyclerView) view_weather_future.findViewById(R.id.recycle_view_future);



		//无网络 
		tv_future_fail_info.setOnClickListener(this);

		LinearLayoutManager layoutManager = new LinearLayoutManager(context);

		if(null == recycleViewAdapter ){

			layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);//设置recycleview横向布局滑动
			recycleview.setLayoutManager(layoutManager);
			Log.d("weather list no net", "weather list"+weather_list);
			//获取数据后进行加载
			recycleViewAdapter = new RecycleViewInfoAdapter(weather_list);

			recycleview.setAdapter(recycleViewAdapter);

			handler.sendEmptyMessage(0x04);
			
			Log.d("weather net available","weather forecast visible");

		}


		boolean isFirstIn = sp.getBoolean("isFirstIn", true);
		//初始化数据的时候判断用户是否是第一次进行定位
		int netWorkState = NetCheckUtil.getNetWorkState(context);
		if (isFirstIn) {
			if (netWorkState == -1) {
				handler.sendEmptyMessage(0x01);
			}else{
				handler.sendEmptyMessage(0x03);
			}
			edit.putBoolean("isFirstIn", false);
			edit.commit();
		}else{
			if (netWorkState == -1) {
				//网络不可用
				recycleview.setVisibility(View.GONE);
				tv_future_fail_info.setVisibility(View.VISIBLE);
				tv_future_fail_info.setText(R.string.ll_future_info);
				String subJson = sp.getString("subJson",null);
				if(subJson == null){
					handler.sendEmptyMessage(0x01);
				}
				Toast.makeText(context, "net fail", 0).show();
			}else {
				handler.sendEmptyMessage(0x03);
			}
		}

	}


	/**
	 * 控件监听事件
	 * */
	@Override
	public void onClick(View v) {
		int netWorkState = NetCheckUtil.getNetWorkState(getContext());
		switch (v.getId()) {
		case R.id.tv_viewOpen:

			if (expandView.isExpand()) {
				expandView.collapse();
				//布局关闭
				expandView.setVisibility(View.GONE);
				if(ll_future.getVisibility() == View.VISIBLE){
					ll_future.setVisibility(View.GONE);
					tv_view_title_open.setText(R.string.main_title_open);
				}

			}else{
				expandView.setVisibility(View.VISIBLE);
				expandView.expand();
				//布局打开
				if(ll_future.getVisibility() == View.GONE){
					ll_future.setVisibility(View.VISIBLE);
					tv_view_title_open.setText(R.string.main_title_close);
				}

				if (netWorkState == -1) {
					//网络不可用
					handler.sendEmptyMessage(0x02);

				}
				else {
					//网络可用
					//加载数据
					recycleview.setVisibility(View.VISIBLE);
					tv_future_fail_info.setVisibility(View.GONE);
				}
			}
			break;
		case R.id.tv_future_fail_info:
			//			handler.sendEmptyMessage(0x03);

			if (netWorkState == -1) {
				//当前无网络
				handler.sendEmptyMessage(0x02);
			}
			/*else{
				//有网络链接
				tv_future_fail_info.setVisibility(View.GONE);
				recycleview.setVisibility(View.VISIBLE);
				handler.sendEmptyMessage(0x03);
			}
		case R.id.tv_firstIn_fail_info:
			if(netWorkState == 1 || netWorkState == 0){
				handler.sendEmptyMessage(0x03);
				tv_firstIn_fail_info.setVisibility(View.GONE);
			}*/
			break;
		default:
			break;
		}
	}

	@Override
	protected void onAttachedToWindow() {
		// TODO Auto-generated method stub
		super.onAttachedToWindow();

		//net change receiver
		IntentFilter filter = new IntentFilter();
		filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
		getContext().registerReceiver(broadcastrecevier_network, filter, null, getHandler());
	}

	/**
	 *视图加载完成后进行数据的展示
	 * 
	 * **/
	@SuppressLint("NewApi")
	public void initData(Context context){
		//开始定位
		initLocation();
	}

	/*
	 * 
	 * 初始化定位
	 * **/
	private void initLocation() {
		//初始化client
		locationClient = new AMapLocationClient(getContext());
		mLocationOption = getDefaultOption();
		//设置定位参数
		locationClient.setLocationOption(mLocationOption);
		// 设置定位监听
		locationClient.setLocationListener(mlocationListener);
		// 启动定位
		locationClient.startLocation();
	}

	/**
	 * 默认的定位参数
	 *
	 */

	private AMapLocationClientOption getDefaultOption(){
		AMapLocationClientOption mOption = new AMapLocationClientOption();
		//可选，设置定位模式，可选的模式有高精度、仅设备、仅网络。默认为高精度模式
		mOption.setLocationMode(AMapLocationMode.Hight_Accuracy);
		//可选，设置是否gps优先，只在高精度模式下有效。默认关闭
		mOption.setGpsFirst(false);
		//可选，设置网络请求超时时间。默认为30秒。在仅设备模式下无效
		mOption.setHttpTimeOut(30000);
		//可选，设置定位间隔。默认为2秒
		mOption.setInterval(2000);
		//可选，设置是否返回逆地理地址信息。默认是true
		mOption.setNeedAddress(true);
		//可选，设置是否单次定位。默认是false
		mOption.setOnceLocation(false);
		//可选，设置是否等待wifi刷新，默认为false.如果设置为true,会自动变为单次定位，持续定位时不要使用
		mOption.setOnceLocationLatest(false);
		//可选， 设置网络请求的协议。可选HTTP或者HTTPS。默认为HTTP
		AMapLocationClientOption.setLocationProtocol(AMapLocationProtocol.HTTP);
		//可选，设置是否使用传感器。默认是false
		mOption.setSensorEnable(false);
		//可选，设置是否开启wifi扫描。默认为true，如果设置为false会同时停止主动刷新，停止以后完全依赖于系统刷新，定位位置可能存在误差
		mOption.setWifiScan(true); 
		//可选，设置是否使用缓存定位，默认为true
		mOption.setLocationCacheEnable(true); 
		return mOption;
	}

	/**
	 * 定位监听
	 */
	AMapLocationListener mlocationListener = new AMapLocationListener() {

		@Override
		public void onLocationChanged(AMapLocation location) {
			if (null != location ) {
				/**
				 * 根据高德定位SDK获取经纬度
				 * 获取当地位置的经纬度
				 * 解析定位结果
				 * */
				GetLocationName(String.valueOf(location.getLongitude()),String.valueOf(location.getLatitude()));
			} else {
				Toast.makeText(getContext(), "定位失败，loc is null", Toast.LENGTH_SHORT).show();

			}
		}
	};


	/**
	 * 
	 * 根据传入的经纬度获取开启线程查询
	 * */
	public void GetLocationName(final String longitude,final String latitude){
		new Thread(new Runnable() {

			@Override
			public void run() {
				GetNameBylaAndLong(longitude,latitude);
			}
		}).start();

	}


	//==========================================================获取城市名  (谷歌地图api language  en)======================================================================================
	/**
	 * 获取城市名
	 * */
	public void GetNameBylaAndLong(String longtitude,String latitude){

		String locationaddr = "http://maps.google.cn/maps/api/geocode/json?latlng="
				+ latitude + "," + longtitude + "&sensor=true&language=en-US";
		try {
			// HttpClient httpClient = new DefaultHttpClient();
			KeyStore trustStore = KeyStore.getInstance(KeyStore  
					.getDefaultType());  
			trustStore.load(null, null);  

			SSLSocketFactory sf = new SSLSocketFactoryEx(trustStore);  
			sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);  

			HttpParams params = new BasicHttpParams();  
			HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);  
			HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);  

			SchemeRegistry registry = new SchemeRegistry();  
			registry.register(new Scheme("http", PlainSocketFactory  
					.getSocketFactory(), 80));  
			registry.register(new Scheme("https", sf, 443));  

			ClientConnectionManager ccm = new ThreadSafeClientConnManager(  
					params, registry);  
			HttpClient httpClient = new DefaultHttpClient(ccm, params);
			HttpConnectionParams.setConnectionTimeout(httpClient.getParams(),
					5000);// 连接超时设置

			HttpGet httpGet = new HttpGet(locationaddr);

			HttpResponse httpResponse = httpClient.execute(httpGet);

			if (httpResponse.getStatusLine().getStatusCode() == 200) {

				HttpEntity entity = httpResponse.getEntity();
				String CityData = EntityUtils.toString(entity, "utf-8");
				JsonParseCityName(CityData);

			}
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
		} catch (IOException e) {
			e.printStackTrace();
		} catch (KeyStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CertificateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (KeyManagementException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnrecoverableKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	// ======================================================================解析城市名字==============================================================================
	public void JsonParseCityName(String cityData) {

		try {
			JSONObject object = new JSONObject(cityData);

			JSONArray jsonArray_res = object.getJSONArray("results");

			JSONObject object_1 = jsonArray_res.getJSONObject(0);

			JSONArray Jsonarry_add = object_1.getJSONArray("address_components");
			JSONObject object_2 = Jsonarry_add.getJSONObject(3);
			String cityName = object_2.getString("long_name");

			System.out.println("cityName=="+cityName);

			if (cityName != null) {
				new DoWeatherTask(getContext()).execute(acquireUrl(weatherType.weather, cityName));//通过获取的城市名通过雅虎的API获取天气情况
			}

		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}



	//===============================(内部类)用SSL构建安全的Socket start ========================================================================================

	class SSLSocketFactoryEx  extends SSLSocketFactory{

		SSLContext sslContext = SSLContext.getInstance("TLS"); 
		public SSLSocketFactoryEx(KeyStore truststore)
				throws NoSuchAlgorithmException, KeyManagementException,
				KeyStoreException, UnrecoverableKeyException {
			super(truststore);
			// TODO Auto-generated constructor stub
			TrustManager tm = new X509TrustManager() {        

				public java.security.cert.X509Certificate[] getAcceptedIssuers() {        
					return null;        
				}        

				@Override        
				public void checkClientTrusted(        
						java.security.cert.X509Certificate[] chain, String authType)        
								throws java.security.cert.CertificateException {        

				}        
				@Override        
				public void checkServerTrusted(        
						java.security.cert.X509Certificate[] chain, String authType)        
								throws java.security.cert.CertificateException {        

				}        
			};        

			sslContext.init(null, new TrustManager[] { tm }, null);   
		}
		@Override        
		public Socket createSocket(Socket socket, String host, int port,        
				boolean autoClose) throws IOException, UnknownHostException {        
			return sslContext.getSocketFactory().createSocket(socket, host, port,        
					autoClose);        
		}        

		@Override        
		public Socket createSocket() throws IOException {        
			return sslContext.getSocketFactory().createSocket();        
		}        

	}
	//=========================================(内部类)用SSL构建安全的Socket end================================================================================================



	//=========================================传入CityName 获取数据================================================================================================
	/**
	 * 传入City Name
	 * get url
	 * **/
	private String acquireUrl(weatherType type, String tempCityName) {
		if (tempCityName != null) {
			switch (type) {
			case weather:
				return "https://query.yahooapis.com/v1/public/yql?q=select%20*%20from%20weather.forecast%20where%20woeid%20in%20%28select%20woeid%20from%20geo.places%281%29%20where%20text=%22"+tempCityName+"%22%29&format=json&env=store%3A%2F%2Fdatatables.org%2Falltableswithkeys";
			default:
				break;
			}
		}
		return null;

	}
	//	============================Do task  (联网操作-数据解析)=================================================================================
	/**
	 * 执行异步任务
	 * */
	class DoWeatherTask extends AsyncTask<String, String, String> {

		private Context context;
		DoWeatherTask(Context c) {
			context = c;
		}
		/***
		 * 
		 * 进行联网操作
		 * */
		@Override
		protected String doInBackground(String... params) {
			BufferedReader in = null;
			String rst = "";

			if (params.length > 0) {
				try {
					weatherUrl = new URL(params[0]);
					URLConnection connection = weatherUrl.openConnection();
					connection.connect();
					in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
					String tmpRst;
					while ((tmpRst = in.readLine()) != null) {
						rst += tmpRst;
					}
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					try {
						if (in != null) {
							in.close();
						}
					} catch (IOException ioE) {
						ioE.printStackTrace();
					}
				}
			}
			return rst;
		}

		/**
		 * 界面数据更新
		 * */
		@Override
		protected void onPostExecute(String s) {
			//			super.onPostExecute(s);
			Log.d("weather json","jsonData=="+s);
			/**
			 * 获取数据成功后
			 * 解析Json数据
			 * */
			if (!TextUtils.isEmpty(s)) {
				parseJson(s);//数据的解析
			} 		
		}

	}
	//===================================Json解析======================================================================================================
	/**
	 * 
	 * 解析返回的JSON数据
	 * */
	@SuppressLint("NewApi")
	public void parseJson(String s) {
		weather_list = new ArrayList<Cityweather>();
		try {
			JSONObject mainJson = new JSONObject(s).getJSONObject("query");

			if (mainJson.getString("count") .equals("1")) {

				if (mainJson.has("results") && mainJson.getJSONObject("results").has("channel")) {
					JSONObject subJson = mainJson.getJSONObject("results").getJSONObject("channel");
					edit.putString("subJson", subJson.toString());
					Log.d("weather subjson", "subjson---->"+subJson);

					if (subJson.has("location") && subJson.has("item") && subJson.getJSONObject("item").has("condition") && subJson.getJSONObject("item").has("forecast")){
						//解析当前位置
						JSONObject jsonObject_location = subJson.getJSONObject("location");
						if (jsonObject_location.has("city")) {
							String city = jsonObject_location.getString("city");
							//可进行设置当前位置
							tv_view_city_name.setText(city);
							edit.putString("city", city);
						}
						//----------------------------------------------------------------------------------------------------------
						JSONObject temp_sub= subJson.getJSONObject("item").getJSONObject("condition");
						//温度
						if (temp_sub.has("temp")) {
							float temp = (temp_sub.getInt("temp") - 32) / 1.8f;
							int currentTemp = (int)((Math.round(temp * 100)) / 100f) ;
							tv_view_current_temp.setText(currentTemp+"ºC");
							edit.putInt("currentTemp", currentTemp);
						}

						//天气	
						if (temp_sub.has("text")) {
							String weather_text = temp_sub.getString("text");
							tv_view_weather_info.setText(weather_text);
							edit.putString("weather_text", weather_text);
						}
						//图片
						if (temp_sub.has("code")){
							//获取code id
							String code_id = temp_sub.getString("code");
							int rightCode = getRightCode(code_id);
							iv_view_weather_image.setImageResource(weatherpicture[rightCode]);
							edit.putString("code_id", code_id);
						}

						//最高温度和最低温度
						JSONArray jsonArray_temp = subJson.getJSONObject("item").getJSONArray("forecast");
						Log.d("weather jsonArray temp","jsonArray temp"+jsonArray_temp);
						if (jsonArray_temp.length() > 0 && jsonArray_temp != null) {

							JSONObject temp_float = jsonArray_temp.getJSONObject(0);
							if (temp_float.has("high")) {
								float temp = (temp_float.getInt("high") - 32) / 1.8f;
								int Top_temp = (int)((Math.round(temp * 100)) / 100f);
								tv_view_top_temp.setText(Top_temp + "ºC");
								edit.putInt("Top_temp", Top_temp);
							}
							if (temp_float.has("low")) {
								float temp = (temp_float.getInt("low") - 32) / 1.8f;
								int bottom_temp = (int)((Math.round(temp * 100)) / 100f);
								tv_view_bottom_temp.setText(bottom_temp + "ºC");
								tv_view_line.setText(R.string.temp_devi);

								edit.putInt("bottom_temp", bottom_temp);
							}
							if (temp_float.has("date")) {
								String date = temp_float.getString("date");
								tv_view_weather_date.setText(date);
								edit.putString("date", date);
							}
							//加载完数据后进行显示
							//main weather info visible
							handler.sendEmptyMessage(0x04);
							Log.d("weather main info","send msg===visible");
						}

						//-----------------------------------------------------------------------------------------------------
						if (jsonArray_temp.length() > 0 && jsonArray_temp != null) {
							for (int i = 0; i < jsonArray_temp.length(); i++) {
								Cityweather cityweather = new Cityweather();
								Log.d("weather forecast","forecast info=="+cityweather);
								JSONObject forecast_jsonObject = jsonArray_temp.getJSONObject(i);
								if (forecast_jsonObject.has("code")) {
									//获取图片code
									String code = forecast_jsonObject.getString("code");
									Log.d("", "");
									cityweather.setCode(code);
								}
								if (forecast_jsonObject.has("high")) {
									String highTemp = forecast_jsonObject.getString("high");

									cityweather.setHightemp(highTemp);
								}
								if (forecast_jsonObject.has("low")) {
									String lowtemp = forecast_jsonObject.getString("low");
									cityweather.setLowtemp(lowtemp);
								}
								if (forecast_jsonObject.has("day")) {
									String date = forecast_jsonObject.getString("day");
									cityweather.setDay(date);
								}
								weather_list.add(cityweather);
								recycleViewAdapter.notifyDataSetChanged();
								Log.d("weather list","weather size="+weather_list.size());
							}

						}
					}

				}
				edit.commit();//提交存储获取的数据

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	//==============================================================================================================================================
	/**
	 * 获取正确的code id匹配本地图片值
	 * 
	 * **/
	private int getRightCode(String code_id) {
		Log.e("wc_log","code_id = " + code_id);
		if(code_id == null)return 28;
		int code = Integer.parseInt(code_id);
		if ((code == 0) || (code == 1) || (code == 2) || (code == 3)
				|| (code == 19) || (code == 20) || (code == 21) || (code == 22)
				|| (code == 23) || (code == 24) || (code == 25)) {
			return 27;
		} else if ((code == 36) || (code == 34) || (code == 32)) {
			return 0;
		} else if ((code == 33) || (code == 31)) {
			return 20;
		} else if ((code == 28) || (code == 30) || (code == 26) || (code == 44)) {
			return 1;
		} else if ((code == 29) || (code == 27)) {
			return 21;
		} else if ((code == 37) || (code == 38) || (code == 39) || (code == 45)
				|| (code == 47)) {
			return 4;
		} else if ((code == 3) || (code == 4)) {
			return 10;
		} else if ((code == 5) || (code == 6) || (code == 7) || (code == 8)
				|| (code == 10) || (code == 18) || (code == 35)) {
			return 6;
		} else if ((code == 9) || (code == 11) || (code == 12) || (code == 40)) {
			return 7;
		} else if ((code == 13) || (code == 14) || (code == 15) || (code == 16)
				|| (code == 42) || (code == 46)) {
			return 12;
		} else if ((code == 41) || (code == 43)) {
			return 15;
		} else if (code == 17) {
			return 26;
		}

		return 28;
	}


	//======================================网络监听=================================================================

	/**
	 * 
	 * 实时监听网络的变化
	 * state = -1 当前无网络
	 * state = 0 || state = 1 当前有网络
	 * 
	 * */
	BroadcastReceiver broadcastrecevier_network = new BroadcastReceiver() {


		@Override
		public void onReceive(final Context context, Intent intent) {
			// TODO Auto-generated method stub

			String action = intent.getAction();

			if(action.equals("android.net.conn.CONNECTIVITY_CHANGE")){
				int netWorkState = NetCheckUtil.getNetWorkState(context);

				//Toast.makeText(context, "netState="+netWorkState, Toast.LENGTH_SHORT).show();
				/**
				 * 1||0  当前网络可用
				 * -1    当前无网络可用
				 * */
				if(netWorkState  == -1){

					if(expandView.isExpand()){

						//网络不可用  
						//第一次进入且网络不可用 
						handler.sendEmptyMessage(0x02);
					}

				}else if(netWorkState == 1 || netWorkState == 0){

					//网络可用时直接执行定位操作
					handler.sendEmptyMessage(0x03);
					/*
					if(expandView.isEnabled() && recycleview.getVisibility() == View.GONE && tv_future_fail_info.getVisibility() == View.VISIBLE){
						//						ll_future.setVisibility(View.VISIBLE);
						recycleview.setVisibility(View.VISIBLE);
						tv_future_fail_info.setVisibility(View.GONE);
					}
					 */

					//网络可用时
					/*
					if(rl_main_net_weather_info.getVisibility() == View.INVISIBLE && tv_firstIn_fail_info.getVisibility() == View.VISIBLE){
						rl_main_net_weather_info.setVisibility(View.VISIBLE);
						tv_firstIn_fail_info.setVisibility(View.GONE);
					}
					 */
				}

			} 
			ReadDataToUpdateUI();
		}
	};



	/**
	 * 网络不可用直接从本地获取数据
	 * */
	@SuppressWarnings("unchecked")
	protected void ReadDataToUpdateUI() {
		//图片
		String code = sp.getString("code_id", null);
		if(code != null){
			int rightCode = getRightCode(code);
			iv_view_weather_image.setImageResource(weatherpicture[rightCode]);
		}
		//城市
		String city = sp.getString("city", null);
		System.out.println("city==="+city);
		if(city != null){
			tv_view_city_name.setText(city);
		}
		//天气信息
		String weather_text = sp.getString("weather_text", null);
		if(weather_text != null){
			tv_view_weather_info.setText(weather_text);
		}
		//日期
		String date = sp.getString("date", null);
		if(date != null){
			tv_view_weather_date.setText(date);
		}
		//当前温度
		int currentTemp = sp.getInt("currentTemp", 0);
		if (currentTemp != 0) {
			tv_view_current_temp.setText(currentTemp+"ºC");
		}
		//最高温度
		int Top_temp = sp.getInt("Top_temp", 0);
		if(Top_temp != 0){
			tv_view_top_temp.setText(Top_temp + "ºC");
		}
		//最低温度
		int bottom_temp = sp.getInt("bottom_temp",0);
		if(bottom_temp != 0){
			tv_view_bottom_temp.setText(bottom_temp + "ºC");
			tv_view_line.setText(R.string.temp_devi);
		}



	};


	//=======================================================================================================
	/**
	 * 当View离开窗口时触发
	 * */
	@SuppressLint("MissingSuperCall")
	protected void onDetachedFromWindow() {
		if (broadcastrecevier_network != null) {
			getContext().unregisterReceiver(broadcastrecevier_network);
			broadcastrecevier_network = null;
		}
		if (locationClient != null) {
			locationClient.stopLocation();//View移除，停止定位
		}

	}

	/*
	//================================================================================================
	/*
	 *
	 *RecycleView布局适配 
	 *
	 */
	class RecycleViewInfoAdapter extends RecyclerView.Adapter<RecycleViewInfoAdapter.ViewHolderInfo>{

		private List<Cityweather> list ;

		public RecycleViewInfoAdapter(List<Cityweather> list){
			this.list = list;
		}

		@Override
		public int getItemCount() {
			return weather_list == null ? 0 : weather_list.size();//显示10天的天气情况
		}

		//设置数据
		@Override
		public void onBindViewHolder(ViewHolderInfo holder, int arg1) {

			ViewHolderInfo holderInfo = (ViewHolderInfo)holder;
			Log.d("weather holderinfo","hodler"+holderInfo);
			Cityweather cityweather = weather_list.get(arg1);
			if (cityweather != null ) {
				if (cityweather.getDay()!= null) {
					holderInfo.tv_future_date.setText(cityweather.getDay());
					//					holder.itemView.setTag(cityweather.getDay());
				}
				if (cityweather.getCode() != null) {
					String code = cityweather.getCode();
					System.out.println("code");
					int rightCode = getRightCode(code);
					holderInfo.iv_future_weather_image.setImageResource(weatherpicture[rightCode]);
					//					holder.itemView.setTag(weatherpicture[rightCode]);
				}
				if (cityweather.getHightemp() != null) {
					String hightemp = cityweather.getHightemp();
					float temp = (Integer.parseInt(hightemp)- 32) / 1.8f;
					int Top_temp = (int)((Math.round(temp * 100)) / 100f);
					holderInfo.tv_future_hightemp.setText(Top_temp + "ºC");
					//					holder.itemView.setTag(Top_temp + "ºC");
				}
				if (cityweather.getLowtemp() != null) {
					String lowtemp = cityweather.getLowtemp();
					float temp = (Integer.parseInt(lowtemp)- 32) / 1.8f;
					int bottom_temp = (int)((Math.round(temp * 100)) / 100f);
					holderInfo.tv_future_lowtemp.setText(bottom_temp + "ºC");
					tv_view_line.setText(R.string.temp_devi);
					//					holder.itemView.setTag(bottom_temp + "ºC");
				}
			}
		}


		//初始化代码
		@Override
		public ViewHolderInfo onCreateViewHolder(ViewGroup parent, int position) {
			View weather_view = LayoutInflater.from(parent.getContext()).inflate(R.layout.leftscreen_weather_item,parent,false);
			RecycleViewInfoAdapter.ViewHolderInfo holder = new RecycleViewInfoAdapter.ViewHolderInfo(weather_view);
			return holder;
		}

		public class ViewHolderInfo extends RecyclerView.ViewHolder{
			ImageView iv_future_weather_image;
			TextView tv_future_date;
			TextView tv_future_hightemp;
			TextView tv_future_lowtemp;

			public ViewHolderInfo(View view) {
				super(view);
				iv_future_weather_image= (ImageView) view.findViewById(R.id.iv_future_weather_image);
				tv_future_date = (TextView) view.findViewById(R.id.tv_future_date);
				tv_future_hightemp = (TextView) view.findViewById(R.id.tv_future_view_top_temp);
				tv_future_lowtemp = (TextView) view.findViewById(R.id.tv_future_view_bottom_temp);

			}

		}
	}





	//=======================================================================================================


}