package com.nao20010128nao.Screen.Locker;

import android.app.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import android.content.*;
import java.lang.ref.*;
import android.net.wifi.*;
import android.telephony.*;
import android.preference.*;
import android.app.admin.*;
import android.util.*;

public class MainActivity extends ActivityGroup {
	static final int RESULT_ENABLE = 1;
	LocalActivityManager lam;
	ViewGroup prefDecor;
	BatteryBroadcastReceiver bbr;
	ScreenBroadcastReceiver  sbr;
	ComponentName mCN;
	DevicePolicyManager mDPM;
	static WeakReference<MainActivity> instance=new WeakReference<>(null);
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
		((ToggleButton)findViewById(R.id.slock_toggle)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
				public void onCheckedChanged(CompoundButton btn, boolean b) {
					if (b) {
						try {
							startLockTask();
						} catch (Throwable e) {
							e.printStackTrace();
							btn.setChecked(false);
						}
						prefDecor.setVisibility(View.GONE);
						try {
							AdditionalOptions ao=AdditionalOptions.instance.get();
							PreferenceScreen ps=ao.getPreferenceScreen();
							CheckBoxPreference lock=(CheckBoxPreference)ps.findPreference("add.sysuihide");
							if (lock.isChecked())
								getWindow().getDecorView().
									setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |// hide nav bar
														  View.SYSTEM_UI_FLAG_FULLSCREEN |// hide status bar
														  View.SYSTEM_UI_FLAG_IMMERSIVE);
						} catch (Throwable e) {
							e.printStackTrace();
						}
					} else {
						try {
							stopLockTask();
						} catch (Throwable e) {
							e.printStackTrace();
						}
						prefDecor.setVisibility(View.VISIBLE);
						try {
							AdditionalOptions ao=AdditionalOptions.instance.get();
							PreferenceScreen ps=ao.getPreferenceScreen();
							CheckBoxPreference lock=(CheckBoxPreference)ps.findPreference("add.lock");
							if (lock.isChecked())
								if (mDPM.isAdminActive(mCN)) 
									mDPM.lockNow();
						} catch (Throwable e) {
							e.printStackTrace();
						}
						try {
							AdditionalOptions ao=AdditionalOptions.instance.get();
							PreferenceScreen ps=ao.getPreferenceScreen();
							CheckBoxPreference lock=(CheckBoxPreference)ps.findPreference("add.sysuihide");
							if (lock.isChecked())
								getWindow().getDecorView().
									setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
						} catch (Throwable e) {
							e.printStackTrace();
						}
					}
				}
			});
		instance = new WeakReference<>(this);
		new AsyncTask<Void,Void,Void>(){
			public Void doInBackground(Void[] unused) {
				WifiManager wm=(WifiManager)getSystemService(WIFI_SERVICE);
				while (true) {
					WifiInfo info=wm.getConnectionInfo();
					if (info != null) {
						setTextIfPossible(R.id.pinfo_wifipoint, getString(R.string.pinfo_wifipoint_head) + info.getSSID());
						setTextIfPossible(R.id.pinfo_wifipower, getString(R.string.pinfo_wifipower_head) + info.getRssi());
					}
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {

					}
				}
			}
		}.execute();
		IntentFilter intf=new IntentFilter();
		intf.addAction("android.intent.action.BATTERY_CHANGED");
		registerReceiver(bbr = new BatteryBroadcastReceiver(), intf);
		intf = new IntentFilter();
		intf.addAction(Intent.ACTION_SCREEN_ON);
		intf.addAction(Intent.ACTION_SCREEN_OFF);
		registerReceiver(sbr = new ScreenBroadcastReceiver(), intf);
		lam = getLocalActivityManager();//new LocalActivityManager(this,false);
		((LinearLayout)findViewById(R.id.add_prefs)).addView(prefDecor = (ViewGroup)lam.startActivity("prefs", new Intent(this, AdditionalOptions.class)).getDecorView());
    	ViewGroup.LayoutParams lp=prefDecor.getLayoutParams();
		lp.height = lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
		prefDecor.setLayoutParams(lp);
		mDPM = (DevicePolicyManager)getSystemService(Context.DEVICE_POLICY_SERVICE);
		mCN = new ComponentName(this, MyDeviceReceiver.class);
		AdditionalOptions ao=AdditionalOptions.instance.get();
		PreferenceScreen ps=ao.getPreferenceScreen();
		CheckBoxPreference lock=(CheckBoxPreference)ps.findPreference("add.lock");
		lock.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener(){
				public boolean onPreferenceChange(Preference pref, Object value) {
					if ((boolean)value) {
						if (!mDPM.isAdminActive(mCN)) {
							// デバイス管理者の登録
							Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);  
							intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, mCN);
							startActivityForResult(intent, RESULT_ENABLE);
						}
					}
					return true;
				}
			});
	}
	@Override
	protected void onDestroy() {
		// TODO: Implement this method
		super.onDestroy();
		unregisterReceiver(bbr);
	}
	public static class BatteryBroadcastReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context p1, Intent intent) {
			// TODO: Implement this method
			if (!intent.getAction().equals(Intent.ACTION_BATTERY_CHANGED))
				return;
			int status = intent.getIntExtra("status", 0);
			int health = intent.getIntExtra("health", 0);
			boolean present = intent.getBooleanExtra("present", false);
			double level = intent.getIntExtra("level", 0);
			double scale = intent.getIntExtra("scale", 0);
			int icon_small = intent.getIntExtra("icon-small", 0);
			int plugged = intent.getIntExtra("plugged", 0);
			int voltage = intent.getIntExtra("voltage", 0);
			int temperature = intent.getIntExtra("temperature", 0);
			String technology = intent.getStringExtra("technology");

			String statusRes = p1.getString(R.string.pinfo_bstate_head);

			switch (status) {
                case BatteryManager.BATTERY_STATUS_UNKNOWN:
                    statusRes += p1.getString(R.string.res_unknown);
                    break;
                case BatteryManager.BATTERY_STATUS_CHARGING:
                    statusRes += p1.getString(R.string.res_charging);
                    break;
                case BatteryManager.BATTERY_STATUS_DISCHARGING:
                    statusRes += p1.getString(R.string.res_discharging);
                    break;
                case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
                    statusRes += p1.getString(R.string.res_not_charging);
                    break;
                case BatteryManager.BATTERY_STATUS_FULL:
                    statusRes += p1.getString(R.string.res_full);
                    break;
			}
			setTextIfPossible(R.id.pinfo_bstate, statusRes);
			setTextIfPossible(R.id.pinfo_battery, p1.getString(R.string.pinfo_battery_head) + (int)Math.floor(level / scale * 100) + "%");
		}
	}
	public static void setTextIfPossible(final int id, final String text) {
		if (instance.get() == null)return;
		instance.get().runOnUiThread(new Runnable(){
				public void run() {
					synchronized (instance.get()) {
						((TextView)instance.get().findViewById(id)).setText(text);
					}
				}
			});
	}
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
			case RESULT_ENABLE:
				if (resultCode == Activity.RESULT_OK) {
					// Has become the device administrator.
					Log.i("tag", "Administration enabled!");
					Toast.makeText(this, R.string.add_lock_enabled, 1).show();
				} else {
					//Canceled or failed.
					Log.i("tag", "Administration enable FAILED!");
					Toast.makeText(this, R.string.add_lock_disable, 1).show();
				}
				return;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
	public static class AdditionalOptions extends PreferenceActivity {
		static WeakReference<AdditionalOptions> instance=new WeakReference<>(null);
		@Override
		protected void onCreate(Bundle savedInstanceState) {
			// TODO: Implement this method
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.additionals);
			instance = new WeakReference<>(this);
		}
	}
	public static class MyDeviceReceiver extends DeviceAdminReceiver {
		@Override
		public void onEnabled(Context context, Intent intent) {
		}
		@Override
		public void onDisabled(Context context, Intent intent) {
		}
	}
	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
		if (((ToggleButton)findViewById(R.id.slock_toggle)).isChecked())
			if (((CheckBoxPreference)AdditionalOptions.instance.get().getPreferenceScreen().findPreference("add.lock")).isChecked())
				if (hasFocus) {
					getWindow().getDecorView().setSystemUiVisibility(
						View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |// hide nav bar
						View.SYSTEM_UI_FLAG_FULLSCREEN |// hide status bar
						View.SYSTEM_UI_FLAG_IMMERSIVE);
				}
	}
	class ScreenBroadcastReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context c, Intent intent) {
			String action = intent.getAction();
			if (action != null) {
				AdditionalOptions ao=AdditionalOptions.instance.get();
				PreferenceScreen ps=ao.getPreferenceScreen();
				CheckBoxPreference amb=(CheckBoxPreference)ps.findPreference("add.ambient");
				if(amb.isChecked()){
					return;
				}
				if (action.equals(Intent.ACTION_SCREEN_ON)) {
					// 画面ON時
					startActivity(new Intent(c,Ambient.class));
				} else if (action.equals(Intent.ACTION_SCREEN_OFF)) {
					// 画面OFF時
					Ambient a=Ambient.instance.get();
					if(a!=null){
						a.finish();
					}
				}
			}
		}
	};
	public static class Ambient extends Activity {
		DevicePolicyManager mDPM=MainActivity.instance.get().mDPM;
		ComponentName mCN=MainActivity.instance.get().mCN;
		static WeakReference<Ambient> instance=new WeakReference(null);
		@Override
		protected void onCreate(Bundle savedInstanceState) {
			// TODO: Implement this method
			super.onCreate(savedInstanceState);
			setContentView(R.layout.ambient);
			getWindow().getDecorView().setOnLongClickListener(new View.OnLongClickListener(){
				public boolean onLongClick(View v){
					finish();
					return true;
				}
			});
			new Thread(){
				public void run(){
					try {
						sleep(1500);
					} catch (InterruptedException e) {
						
					}
					if (mDPM.isAdminActive(mCN)) 
						mDPM.lockNow();
					runOnUiThread(new Runnable(){
						public void run(){
							finish();
						}
					});
				}
			}.start();
			getActionBar().hide();
			instance=new WeakReference(this);
		}
	}
}
