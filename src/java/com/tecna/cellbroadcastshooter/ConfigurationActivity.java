package com.tecna.cellbroadcastshooter;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Button;
import android.telephony.SubscriptionManager;
import android.provider.Telephony;
import android.Manifest;
import android.app.Activity;
import android.app.AppOpsManager;
import android.os.UserHandle;
import android.content.BroadcastReceiver;
import android.widget.Toast;
import android.telephony.TelephonyManager;
import com.android.internal.telephony.gsm.*;
import android.telephony.*;
import com.android.internal.telephony.*;
import android.os.UserHandle;


public class ConfigurationActivity extends Activity {

    private final int SEND_BROADCAST = 1;
    private final int BROADCAST_COMPLETE = 2;
    int mChannelId = -1;
    int mUpdateNumber = 0;
    int mSerialNumber = 1;
    String mSlotId = null;
    String mMessageBody = "This is for test";

    EditText mChannelIdInput = null;
    EditText mUpdateNumberInput = null;
    EditText mMessageBodyInput = null;
    EditText mSlotIdInput = null;
    EditText mSerialNumberInput = null;
    Button mShotButton = null;

    SmsCbMessage mMessage = null;
    SmsCbCmasInfo mCmasWarningInfo = null;
    SmsCbEtwsInfo mSmsCbEtwsInfo = null;
    SmsCbLocation mSmsCbLocation = new SmsCbLocation("djlin");
//    public TelephonyManager mTelephonyManager = new TelephonyManager(this);


    Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SEND_BROADCAST:
//                    Intent intent = new Intent();
//                    intent.putExtra("message", mMessage);
                    handleBroadcastSms(mMessage);
                    break;
                case BROADCAST_COMPLETE:
                    Toast.makeText(ConfigurationActivity.this, "broadcast already sent cpmlete!!", Toast.LENGTH_SHORT).show();
                    mMessage = null;
                    mCmasWarningInfo = null;
                    mSmsCbEtwsInfo = null;
                default:
                    super.handleMessage(msg);
            }

        }
    };

    protected final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mHandler.sendEmptyMessage(BROADCAST_COMPLETE);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configuration);

        mChannelIdInput = (EditText) findViewById(R.id.channel_id_input);
        mSerialNumberInput = (EditText) findViewById(R.id.serial_number_input);
        mUpdateNumberInput = (EditText) findViewById(R.id.update_number_input);
        mMessageBodyInput = (EditText) findViewById(R.id.message_body_input);
        mSlotIdInput = (EditText) findViewById(R.id.slot_id_input);
        mShotButton = (Button) findViewById(R.id.shot_button);


        mShotButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String channelId = mChannelIdInput.getText().toString();
                String serialNumberInput = mSerialNumberInput.getText().toString();
                String updateNumber = mUpdateNumberInput.getText().toString();
                String messageBody = mMessageBodyInput.getText().toString();
                
                int messageIdentifier = TextUtils.isEmpty(channelId)?0:Integer.parseInt(channelId);
                int serialNumber = TextUtils.isEmpty(serialNumberInput)?1:Integer.parseInt(serialNumberInput);
                
                if (isCmasMessage(messageIdentifier)) {
                	int cmasMessageClass = getCmasMessageClass(messageIdentifier);
                	mCmasWarningInfo =
                			new SmsCbCmasInfo(cmasMessageClass, SmsCbCmasInfo.CMAS_CATEGORY_UNKNOWN,
                					SmsCbCmasInfo.CMAS_RESPONSE_TYPE_UNKNOWN, getCmasSeverity(messageIdentifier),
                					getCmasUrgency(messageIdentifier), getCmasCertainty(messageIdentifier));
                    mMessage = new SmsCbMessage(1/* 3Gpp format */,
                            1/* geographicalScope default value*/,
                            serialNumber/* serialNumber default value */,
                            mSmsCbLocation/* location default value */,
                            messageIdentifier/* serviceCategory */,
                            "en"/* language default value */,
                            TextUtils.isEmpty(messageBody)?mMessageBody:messageBody/* message body */,
                            1/* priority default value */,
                            null/* etwsWarningInfo default value */,
                            mCmasWarningInfo/* cmasWarningInfo default value */);
                	} else if (isEtwsMessage(messageIdentifier)) {
                		byte[] warningSecurityInformation = IccUtils.hexStringToBytes("20170707070707");
                		
                		mSmsCbEtwsInfo = new SmsCbEtwsInfo(getEtwsWarningType(messageIdentifier),
                				isEtwsEmergencyUserAlert(serialNumber), isEtwsPopupAlert(serialNumber),
                				/*true  make true by default, */ warningSecurityInformation);
                		
                        mMessage = new SmsCbMessage(1/* 3Gpp format */,
                                1/* geographicalScope default value*/,
                                serialNumber/* serialNumber default value */,
                                mSmsCbLocation/* location default value */,
                                messageIdentifier/* serviceCategory */,
                                "en"/* language default value */,
                                TextUtils.isEmpty(messageBody)?mMessageBody:messageBody/* message body */,
                                1/* priority default value */,
                                mSmsCbEtwsInfo/* etwsWarningInfo default value */,
                                null/* cmasWarningInfo default value */);
                	} else {
                        mMessage = new SmsCbMessage(1/* 3Gpp format */,
                                1/* geographicalScope default value*/,
                                serialNumber/* serialNumber default value */,
                                mSmsCbLocation/* location default value */,
                                messageIdentifier/* serviceCategory */,
                                "en"/* language default value */,
                                TextUtils.isEmpty(messageBody)?mMessageBody:messageBody/* message body */,
                                1/* priority default value */,
                                null/* etwsWarningInfo default value */,
                                null/* cmasWarningInfo default value */);
                	}
                

                mHandler.sendEmptyMessage(SEND_BROADCAST);
            }
        });

    }

    protected void handleBroadcastSms(SmsCbMessage message) {
        String receiverPermission;
        int appOp;

        Intent intent;
        if (message.isEmergencyMessage()) {
            android.util.Log.d("djlin", "Dispatching emergency SMS CB, SmsCbMessage is: " + message);
//            intent = new Intent("android.provider.Telephony.SMS_CB_RECEIVED.test");
            intent = new Intent(Telephony.Sms.Intents.SMS_CB_RECEIVED_ACTION);
            receiverPermission = Manifest.permission.RECEIVE_EMERGENCY_BROADCAST;
            appOp = 17; //AppOpsManager.OP_RECEIVE_EMERGECY_SMS;
        } else {
            android.util.Log.d("djlin", "Dispatching SMS CB, SmsCbMessage is: " + message);
//            intent = new Intent("android.provider.Telephony.SMS_CB_RECEIVED.test");
            intent = new Intent(Telephony.Sms.Intents.SMS_CB_RECEIVED_ACTION);
            receiverPermission = Manifest.permission.RECEIVE_SMS;
            appOp = 16; //AppOpsManager.OP_RECEIVE_SMS;
        }
        intent.putExtra("message", message);
        int slotId = TextUtils.isEmpty(mSlotId)?0:Integer.parseInt(mSlotId);
//        intent.putExtra("phone"/*PhoneConstants.PHONE_KEY*/, slotId);
//        intent.putExtra("slot"/*PhoneConstants.SLOT_KEY*/, slotId);
//        SubscriptionManager.putPhoneIdAndSubIdExtra(intent, mPhone.getPhoneId());
        SubscriptionManager.putPhoneIdAndSubIdExtra(intent, slotId);
        this.sendOrderedBroadcastAsUser(intent, UserHandle.ALL/*UserHandle.ALL*/, receiverPermission, appOp,
                mReceiver, mHandler, Activity.RESULT_OK, null, null);
//        this.sendBroadcast(intent);
        Toast.makeText(ConfigurationActivity.this, "broadcast already sent!", Toast.LENGTH_SHORT).show();
    }
    
    private int getCmasMessageClass(int messageIdentifier) {
        switch (messageIdentifier) {
            case SmsCbConstants.MESSAGE_ID_CMAS_ALERT_PRESIDENTIAL_LEVEL:
                return SmsCbCmasInfo.CMAS_CLASS_PRESIDENTIAL_LEVEL_ALERT;

            case SmsCbConstants.MESSAGE_ID_CMAS_ALERT_EXTREME_IMMEDIATE_OBSERVED:
            case SmsCbConstants.MESSAGE_ID_CMAS_ALERT_EXTREME_IMMEDIATE_LIKELY:
            case SmsCbConstants.MESSAGE_ID_CMAS_ALERT_EXTREME_EXPECTED_OBSERVED:
            case SmsCbConstants.MESSAGE_ID_CMAS_ALERT_EXTREME_EXPECTED_LIKELY:
                return SmsCbCmasInfo.CMAS_CLASS_EXTREME_THREAT;

            case SmsCbConstants.MESSAGE_ID_CMAS_ALERT_SEVERE_IMMEDIATE_OBSERVED:
            case SmsCbConstants.MESSAGE_ID_CMAS_ALERT_SEVERE_IMMEDIATE_LIKELY:
            case SmsCbConstants.MESSAGE_ID_CMAS_ALERT_SEVERE_EXPECTED_OBSERVED:
            case SmsCbConstants.MESSAGE_ID_CMAS_ALERT_SEVERE_EXPECTED_LIKELY:
                return SmsCbCmasInfo.CMAS_CLASS_SEVERE_THREAT;

            case SmsCbConstants.MESSAGE_ID_CMAS_ALERT_CHILD_ABDUCTION_EMERGENCY:
                return SmsCbCmasInfo.CMAS_CLASS_CHILD_ABDUCTION_EMERGENCY;

            case SmsCbConstants.MESSAGE_ID_CMAS_ALERT_REQUIRED_MONTHLY_TEST:
                return SmsCbCmasInfo.CMAS_CLASS_REQUIRED_MONTHLY_TEST;

            case SmsCbConstants.MESSAGE_ID_CMAS_ALERT_EXERCISE:
                return SmsCbCmasInfo.CMAS_CLASS_CMAS_EXERCISE;

            case SmsCbConstants.MESSAGE_ID_CMAS_ALERT_OPERATOR_DEFINED_USE:
                return SmsCbCmasInfo.CMAS_CLASS_OPERATOR_DEFINED_USE;

            default:
                return SmsCbCmasInfo.CMAS_CLASS_UNKNOWN;
        }
    }

    /**
         * Return whether this broadcast is an ETWS emergency message type.
         * @return true if this message is ETWS emergency type; false otherwise
         */
        private boolean isEtwsMessage(int messageIdentifier) {
            return (messageIdentifier & SmsCbConstants.MESSAGE_ID_ETWS_TYPE_MASK)
                    == SmsCbConstants.MESSAGE_ID_ETWS_TYPE;
        }
        
        /**
         * Return whether this message is a CMAS emergency message type.
         * @return true if this message is CMAS emergency type; false otherwise
         */
        private boolean isCmasMessage(int messageIdentifier) {
            return messageIdentifier >= SmsCbConstants.MESSAGE_ID_CMAS_FIRST_IDENTIFIER
                    && messageIdentifier <= SmsCbConstants.MESSAGE_ID_CMAS_LAST_IDENTIFIER;
        }

        
        /**
         * Returns the severity for a CMAS warning notification. This is only available for extreme
         * and severe alerts, not for other types such as Presidential Level and AMBER alerts.
         * This method assumes that the message ID has already been checked for CMAS type.
         * @return the CMAS severity as defined in {@link SmsCbCmasInfo}
         */
        private int getCmasSeverity(int messageIdentifier) {
            switch (messageIdentifier) {
                case SmsCbConstants.MESSAGE_ID_CMAS_ALERT_EXTREME_IMMEDIATE_OBSERVED:
                case SmsCbConstants.MESSAGE_ID_CMAS_ALERT_EXTREME_IMMEDIATE_LIKELY:
                case SmsCbConstants.MESSAGE_ID_CMAS_ALERT_EXTREME_EXPECTED_OBSERVED:
                case SmsCbConstants.MESSAGE_ID_CMAS_ALERT_EXTREME_EXPECTED_LIKELY:
                    return SmsCbCmasInfo.CMAS_SEVERITY_EXTREME;

                case SmsCbConstants.MESSAGE_ID_CMAS_ALERT_SEVERE_IMMEDIATE_OBSERVED:
                case SmsCbConstants.MESSAGE_ID_CMAS_ALERT_SEVERE_IMMEDIATE_LIKELY:
                case SmsCbConstants.MESSAGE_ID_CMAS_ALERT_SEVERE_EXPECTED_OBSERVED:
                case SmsCbConstants.MESSAGE_ID_CMAS_ALERT_SEVERE_EXPECTED_LIKELY:
                    return SmsCbCmasInfo.CMAS_SEVERITY_SEVERE;

                default:
                    return SmsCbCmasInfo.CMAS_SEVERITY_UNKNOWN;
            }
        }

        /**
         * Returns the urgency for a CMAS warning notification. This is only available for extreme
         * and severe alerts, not for other types such as Presidential Level and AMBER alerts.
         * This method assumes that the message ID has already been checked for CMAS type.
         * @return the CMAS urgency as defined in {@link SmsCbCmasInfo}
         */
        private int getCmasUrgency(int messageIdentifier) {
            switch (messageIdentifier) {
                case SmsCbConstants.MESSAGE_ID_CMAS_ALERT_EXTREME_IMMEDIATE_OBSERVED:
                case SmsCbConstants.MESSAGE_ID_CMAS_ALERT_EXTREME_IMMEDIATE_LIKELY:
                case SmsCbConstants.MESSAGE_ID_CMAS_ALERT_SEVERE_IMMEDIATE_OBSERVED:
                case SmsCbConstants.MESSAGE_ID_CMAS_ALERT_SEVERE_IMMEDIATE_LIKELY:
                    return SmsCbCmasInfo.CMAS_URGENCY_IMMEDIATE;

                case SmsCbConstants.MESSAGE_ID_CMAS_ALERT_EXTREME_EXPECTED_OBSERVED:
                case SmsCbConstants.MESSAGE_ID_CMAS_ALERT_EXTREME_EXPECTED_LIKELY:
                case SmsCbConstants.MESSAGE_ID_CMAS_ALERT_SEVERE_EXPECTED_OBSERVED:
                case SmsCbConstants.MESSAGE_ID_CMAS_ALERT_SEVERE_EXPECTED_LIKELY:
                    return SmsCbCmasInfo.CMAS_URGENCY_EXPECTED;

                default:
                    return SmsCbCmasInfo.CMAS_URGENCY_UNKNOWN;
            }
        }
        
        /**
         * Returns the certainty for a CMAS warning notification. This is only available for extreme
         * and severe alerts, not for other types such as Presidential Level and AMBER alerts.
         * This method assumes that the message ID has already been checked for CMAS type.
         * @return the CMAS certainty as defined in {@link SmsCbCmasInfo}
         */
        private int getCmasCertainty(int messageIdentifier) {
            switch (messageIdentifier) {
                case SmsCbConstants.MESSAGE_ID_CMAS_ALERT_EXTREME_IMMEDIATE_OBSERVED:
                case SmsCbConstants.MESSAGE_ID_CMAS_ALERT_EXTREME_EXPECTED_OBSERVED:
                case SmsCbConstants.MESSAGE_ID_CMAS_ALERT_SEVERE_IMMEDIATE_OBSERVED:
                case SmsCbConstants.MESSAGE_ID_CMAS_ALERT_SEVERE_EXPECTED_OBSERVED:
                    return SmsCbCmasInfo.CMAS_CERTAINTY_OBSERVED;

                case SmsCbConstants.MESSAGE_ID_CMAS_ALERT_EXTREME_IMMEDIATE_LIKELY:
                case SmsCbConstants.MESSAGE_ID_CMAS_ALERT_EXTREME_EXPECTED_LIKELY:
                case SmsCbConstants.MESSAGE_ID_CMAS_ALERT_SEVERE_IMMEDIATE_LIKELY:
                case SmsCbConstants.MESSAGE_ID_CMAS_ALERT_SEVERE_EXPECTED_LIKELY:
                    return SmsCbCmasInfo.CMAS_CERTAINTY_LIKELY;

                default:
                    return SmsCbCmasInfo.CMAS_CERTAINTY_UNKNOWN;
            }
        }

        /**
         * Return whether the popup alert flag is set for an ETWS warning notification.
         * This method assumes that the message ID has already been checked for ETWS type.
         *
         * @return true if the message code indicates a popup alert should be displayed
         */
        private boolean isEtwsPopupAlert(int serialNumber) {
//            return (serialNumber & SmsCbConstants.SERIAL_NUMBER_ETWS_ACTIVATE_POPUP) != 0;
        	return true;
        }

        /**
         * Return whether the emergency user alert flag is set for an ETWS warning notification.
         * This method assumes that the message ID has already been checked for ETWS type.
         *
         * @return true if the message code indicates an emergency user alert
         */
        private boolean isEtwsEmergencyUserAlert(int serialNumber) {
//            return (serialNumber & SmsCbConstants.SERIAL_NUMBER_ETWS_EMERGENCY_USER_ALERT) != 0;
        	return true;
        }

        /**
         * Returns the warning type for an ETWS warning notification.
         * This method assumes that the message ID has already been checked for ETWS type.
         *
         * @return the ETWS warning type defined in 3GPP TS 23.041 section 9.3.24
         */
        private int getEtwsWarningType(int messageIdentifier) {
            return messageIdentifier - SmsCbConstants.MESSAGE_ID_ETWS_EARTHQUAKE_WARNING;
        }

    
}
