package com.nao20010128nao.Screen.Locker;

import android.app.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import android.content.*;
import java.lang.ref.*;
import android.net.wifi.*;
import android.telephony.*;

public class MainActivity extends Activity
{
	BatteryBroadcastReceiver bbr;
	static WeakReference<MainActivity> instance=new WeakReference<>(null);
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
		((ToggleButton)findViewById(R.id.slock_toggle)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
			public void onCheckedChanged(CompoundButton btn,boolean b){
				if(b){
					try{
						startLockTask();
					}catch(Throwable e){
						e.printStackTrace();
						btn.setChecked(false);
					}
				}else{
					try{
						stopLockTask();
					}catch(Throwable e){
						e.printStackTrace();
					}
				}
			}
		});
		instance=new WeakReference<>(this);
		new AsyncTask<Void,Void,Void>(){
			public Void doInBackground(Void[] unused){
				WifiManager wm=(WifiManager)getSystemService(WIFI_SERVICE);
				while(true){
					WifiInfo info=wm.getConnectionInfo();
					if(info!=null){
						setTextIfPossible(R.id.pinfo_wifipoint,getString(R.string.pinfo_wifipoint_head)+info.getSSID());
						setTextIfPossible(R.id.pinfo_wifipower,getString(R.string.pinfo_wifipower_head)+info.getRssi());
					}
					try{
						Thread.sleep(100);
					}catch (InterruptedException e){
						
					}
				}
			}
		}.execute();
		IntentFilter intf=new IntentFilter();
		intf.addAction("android.intent.action.BATTERY_CHANGED");
		registerReceiver(bbr=new BatteryBroadcastReceiver(),intf);
    }
	@Override
	protected void onDestroy()
	{
		// TODO: Implement this method
		super.onDestroy();
		unregisterReceiver(bbr);
	}
	public static class BatteryBroadcastReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context p1, Intent intent)
		{
			// TODO: Implement this method
			if(!intent.getAction().equals(Intent.ACTION_BATTERY_CHANGED))
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
			setTextIfPossible(R.id.pinfo_bstate,statusRes);
			setTextIfPossible(R.id.pinfo_battery,p1.getString(R.string.pinfo_battery_head)+(int)Math.floor(level/scale*100)+"%");
		}
	}
	public static void setTextIfPossible(final int id,final String text){
		if(instance.get()==null)return;
		instance.get().runOnUiThread(new Runnable(){
			public void run(){
				synchronized(instance.get()){
					((TextView)instance.get().findViewById(id)).setText(text);
				}
			}
		});
	}
}
