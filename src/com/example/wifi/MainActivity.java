package com.example.wifi;

import java.io.IOException;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import android.app.Activity;
import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.format.Formatter;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends Activity {
	
	private EditText AccountView;
	private EditText PasswordView;
	private String Account;
	private String Password;
	private WifiInfo mWifiInfo;
	private WifiManager wifiManager;
	private Button LoginButton;
	private Button LogoutButton;
	private static String LogInURL = "https://wlan.whu.edu.cn/portal/login";
	private static String LogOutURL = "http://wlan.whu.edu.cn/portal/logOff";
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		AccountView=(EditText) findViewById(R.id.editText1);
		PasswordView=(EditText) findViewById(R.id.editText2);
		LoginButton=(Button) findViewById(R.id.button1);
		LoginButton.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Account = AccountView.getText().toString();
				Password = PasswordView.getText().toString();
				CheckNetwork();
				if(!Account.equals("") && !Password.equals("")){
					if (GetWifiStatus()){
						LoginButton.setText("登陆中……");
						new Thread(new OnlineThread()).start();
						LoginButton.setEnabled(false);
					}
					else
						CheckNetwork();
				}
			}
			
		});
		LogoutButton=(Button) findViewById(R.id.button2);
		LogoutButton.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				CheckNetwork();
				new Thread(new LogOutThread()).start();
				LogoutButton.setEnabled(false);
			}
			
		});
	}
	
	Handler OnlineHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			LoginButton.setText("登陆");
			if (msg.arg1 == 0) {
				Toast.makeText(MainActivity.this, "验证成功",Toast.LENGTH_LONG).show();			
			}
			if (msg.arg1 == 1) {
				String strtemp = (String) msg.obj;
				Toast.makeText(MainActivity.this, strtemp,Toast.LENGTH_LONG).show();	
				LoginButton.setEnabled(true);
			}
			if (msg.arg1 == 2) {
				Toast.makeText(MainActivity.this, "网络异常",Toast.LENGTH_LONG).show();	
				LoginButton.setEnabled(true);
			}
			if (msg.arg1 == 3) {
				Toast.makeText(MainActivity.this, "网络异常",Toast.LENGTH_LONG).show();	
				LoginButton.setEnabled(true);
			}
		}

	};
	
	Handler LogOutHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			if (msg.arg1 == 1) {
				Toast.makeText(MainActivity.this, (String) msg.obj,Toast.LENGTH_LONG).show();
				LogoutButton.setEnabled(true);
			}
		}
	};
	
	public static HttpClient getNewHttpClient() {
		try {
			KeyStore trustStore = KeyStore.getInstance(KeyStore
					.getDefaultType());
			trustStore.load(null, null);

			SSLSocketFactory sf = new SSLSocketFactoryEx(trustStore);
			sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

			HttpParams params = new BasicHttpParams();
			HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
			HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);
			HttpConnectionParams.setConnectionTimeout(params, 5 * 1000);
			HttpConnectionParams.setSoTimeout(params, 5 * 1000);

			SchemeRegistry registry = new SchemeRegistry();
			registry.register(new Scheme("http", PlainSocketFactory
					.getSocketFactory(), 80));
			registry.register(new Scheme("https", sf, 443));

			ClientConnectionManager ccm = new ThreadSafeClientConnManager(
					params, registry);

			return new DefaultHttpClient(ccm, params);
		} catch (Exception e) {
			return new DefaultHttpClient();
		}
	}
	
	public String getWifiCookie(String url) {
		HttpClient httpclient = getNewHttpClient();
		String cookie = "";
		HttpGet httpget = new HttpGet(url);
		try {
			HttpResponse httpResponse = httpclient.execute(httpget);
			if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				Header[] headers = httpResponse.getAllHeaders();
				for (int i = 0; i < headers.length; i++) {
					if (headers[i].getName().contains("Cookie")) {
						// System.out.println(headers[i].getName()
						// + "=="+
						// headers[i].getValue());
						String tempcookie = headers[i].getValue();
						tempcookie = tempcookie.substring(0,
								tempcookie.indexOf(";"));
						cookie = cookie + tempcookie + ";";
					}
				}
			}
		} catch (ClientProtocolException e) {
		} catch (IOException e) {
		} catch (Exception e) {
		} finally {
			httpclient.getConnectionManager().shutdown();
		}
		cookie = cookie.substring(0, cookie.length() - 1);
		return cookie;
	}
	
	public class OnlineThread implements Runnable {
		@Override
		public void run() {
			// TODO Auto-generated method stub
			Message msg = OnlineHandler.obtainMessage();
			String strResult = null;
			List<NameValuePair> pairs = new ArrayList<NameValuePair>();
			pairs.add(new BasicNameValuePair("username", Account));
			pairs.add(new BasicNameValuePair("password", Password));
			HttpClient httpclient = getNewHttpClient();
			try {
				// 设置字符集
				HttpEntity httpentity = new UrlEncodedFormEntity(pairs, "UTF-8");
				// HttpPost连接对象
				HttpPost httpRequest = new HttpPost(LogInURL);
				httpRequest.addHeader("Accept", "*/*");
				httpRequest.addHeader("Accept-Language", "zh-cn");
				httpRequest.addHeader("Cache-Control", "no-cache");
				httpRequest.addHeader("Connection", "gzip, deflate");
				httpRequest.addHeader("Accept-Encoding", "Keep-Alive");
				httpRequest.addHeader("Content-Type",
						"application/x-www-form-urlencoded");
				String ip = getipAddress();
				String mac = getmacAddress();
				// String switchip = getswitchip();
				String switchip = getgateway();
				// temp=switchip;
				String CookieUrl = "https://202.114.79.246/portal?cmd=login&switchip="
						+ switchip
						+ "&mac="
						+ mac
						+ "&ip="
						+ ip
						+ "&essid=WHU%2DWLAN&url=http%3A%2F%2Fbaidu%2Ecom%2F";
				String cookie = getWifiCookie(CookieUrl);
				httpRequest.addHeader("Cookie", cookie);
				httpRequest.addHeader("Host", "202.114.79.246");
				httpRequest.addHeader("Referer", CookieUrl);
				httpRequest
						.addHeader(
								"User-Agent",
								"Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.1; Trident/4.0; .NET4.0C; .NET4.0E; .NET CLR 2.0.50727; .NET CLR 3.0.04506.648; .NET CLR 3.5.21022)");
				// 请求httpRequest
				httpRequest.setEntity(httpentity);
				// 取得HttpResponse
				HttpResponse httpResponse = httpclient.execute(httpRequest);
				// HttpStatus.SC_OK表示连接成功
				int statusCode = httpResponse.getStatusLine().getStatusCode();
				// System.out.println(statusCode);
				if (statusCode == HttpStatus.SC_OK) {
					// 取得返回的字符串
					strResult = EntityUtils.toString(httpResponse.getEntity());
					String strReturnMessage = getErrorMessage(strResult);

					if (strReturnMessage.length() <= 4) {
						msg.arg1 = 0;
					} else {
						msg.arg1 = 1;
						msg.obj = strReturnMessage;
					}
				} else {
					msg.arg1 = 0;
				}
			} catch (ClientProtocolException e) {
				e.printStackTrace();
				msg.arg1 = 2;
			} catch (IOException e) {
				e.printStackTrace();
				msg.arg1 = 3;
			} catch (Exception e) {
				e.printStackTrace();
				msg.arg1 = 3;
			} finally {
				httpclient.getConnectionManager().shutdown();
			}
			OnlineHandler.sendMessage(msg);
		}
	}
	
	public class LogOutThread implements Runnable {

		@Override
		public void run() {
			// TODO Auto-generated method stub
			Message msg = LogOutHandler.obtainMessage();
			HttpClient httpclient = new DefaultHttpClient();
			HttpGet httpLogOff = new HttpGet(LogOutURL);
			try {
				HttpResponse httpResponse = httpclient.execute(httpLogOff);
				if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
					String strResult = EntityUtils.toString(httpResponse
							.getEntity());
					msg.obj = (strResult);
					msg.arg1 = 1;
				}
			} catch (ClientProtocolException e) {
			} catch (IOException e) {
			} catch (Exception e) {
			} finally {
				httpclient.getConnectionManager().shutdown();
			}
			LogOutHandler.sendMessage(msg);
		}
	}
	
	public void CheckNetwork() {
		wifiManager =  (WifiManager)getSystemService(Context.WIFI_SERVICE);
		Toast.makeText(MainActivity.this, "当前Wifi网卡状态为" +
				wifiManager.getWifiState(), Toast.LENGTH_SHORT).show();
		if (!GetWifiStatus()) {
			Toast.makeText(MainActivity.this,"请打开Wifi",Toast.LENGTH_SHORT).show();
		}
	}
	
	public static String getErrorMessage(String html) {
		Document doc = null;
		doc = Jsoup.parse(html);
		Elements links = doc.getElementsByClass("msg");
		return links.text().toString();
	}
	
	public String IpIntToString(int i) {
		return (i & 0xFF) + "." + ((i >> 8) & 0xFF) + "." + ((i >> 16) & 0xFF)
				+ "." + ((i >> 24) & 0xFF);
	}
	
	public boolean GetWifiStatus() {
		return wifiManager.isWifiEnabled();
	}
	
	public String getipAddress() {
		mWifiInfo = wifiManager.getConnectionInfo();
		int i = mWifiInfo.getIpAddress();
		String str1 = IpIntToString(i);
		return str1;
	}
	
	public String getgateway() {
		DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();
		String str1 = IpIntToString(dhcpInfo.gateway);
		return str1;
	}
	
	public String getmacAddress() {
		mWifiInfo = wifiManager.getConnectionInfo();
		return mWifiInfo.getMacAddress();
	}
}
