package com.example.jiaodu;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.SmsMessage;

public class SmsReciver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		Bundle bundle = intent.getExtras();
		SmsMessage msg = null;
		if (null != bundle) {
			Object[] smsObj = (Object[]) bundle.get("pdus");
			for (Object object : smsObj) {
				msg = SmsMessage.createFromPdu((byte[]) object);
				// 在这里写自己的逻辑
				if (msg.getDisplayMessageBody() != null) {
					Intent intentAction = new Intent(AngleCalculationApplication.updateGPS);
					intentAction.putExtra(AngleCalculationApplication.intentData, msg.getDisplayMessageBody());
					LocalBroadcastManager.getInstance(context).sendBroadcast(intentAction);					
				}
			}
		}
	}

}
