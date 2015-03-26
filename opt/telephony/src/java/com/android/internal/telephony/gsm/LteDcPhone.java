/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

/*
 *
 */

package com.android.internal.telephony.gsm;

import android.content.Context;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.Registrant;
import android.os.RegistrantList;
import android.os.SystemProperties;

import android.text.TextUtils;
import android.telephony.Rlog;

import android.provider.Settings;
import android.provider.Telephony;
import android.provider.Telephony.SIMInfo;
import android.telephony.CellLocation;
import android.telephony.PhoneNumberUtils;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;


import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneBase;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.PhoneProxy;;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneNotifier;
import com.android.internal.telephony.RILConstants;
import com.android.internal.telephony.ServiceStateTracker;
import com.android.internal.telephony.OperatorInfo;

import com.android.internal.telephony.gsm.GSMPhone;
import com.android.internal.telephony.gsm.GsmCall;
import com.android.internal.telephony.gsm.GsmMmiCode;
import com.android.internal.telephony.gsm.LteDcServiceStateTracker;

import com.android.internal.telephony.gemini.GeminiNetworkSubUtil;


/**
 * {@hide}
 */
public class LteDcPhone extends GSMPhone {
    private static final boolean LOCAL_DEBUG = true;

    private GSMPhone mPeerGsmPhone = null;
    
    private static final int MESSAGE_GET_PREFERRED_NETWORK_TYPE_MMDC = 0;
    private static final int MESSAGE_SET_PREFERRED_NETWORK_TYPE_MMDC = 1;
    private static final int MESSAGE_QUERY_AVAILABLE_NETWORK_MMDC = 2;
    private static final int MESSAGE_SET_NW_MANUAL_COMPLETE_MMDC=3;

    private int mNetworkType;

    
    public LteDcPhone(Context context, CommandsInterface ci, PhoneNotifier notifier){
        super(context, ci, notifier, "LteDc");
        LOGD("Create LteDcPhone");

        //replace mCi(RIL)
        //mCi = mCi.getLteDcManager();

        //create mSST
        mSST = new LteDcServiceStateTracker(this);

        mPeerGsmPhone = (GSMPhone)(((PhoneProxy)PhoneFactory.getDefaultPhone()).getActivePhone());
        mDcTracker = mPeerGsmPhone.mDcTracker;

        LOGD("setPhoneComponent() with mPeerGsmPhone");
        ci.setPhoneComponent(mPeerGsmPhone); // MVNO-API        
    }


    @Override
    public void dispose() {
        synchronized(PhoneProxy.lockForRadioTechnologyChange) {
            super.dispose();
            //Force all referenced classes to unregister their former registered events
            mSST.dispose();
        }
    }


    /**
     * When overridden the derived class needs to call
     * super.handleMessage(msg) so this method has a
     * a chance to process the message.
     *
     * @param msg
     */
    @Override
    public void handleMessage(Message msg) {
        LOGD("handleMessage");
        switch (msg.what) {
            case MESSAGE_GET_PREFERRED_NETWORK_TYPE_MMDC:
                handleGetPreferredNetworkTypeResponse(msg);
                break;

            //case MESSAGE_SET_PREFERRED_NETWORK_TYPE_MMDC:
            //    handleSetPreferredNetworkTypeResponse(msg);
            //    break;

            case MESSAGE_QUERY_AVAILABLE_NETWORK_MMDC:
                mCi.getAvailableNetworks((Message)msg.obj);
                break;

            // handle the select network completion callbacks.
            //case MESSAGE_SET_NW_MANUAL_COMPLETE_MMDC:
            case EVENT_SET_NETWORK_MANUAL_COMPLETE:
                handleSetSelectNetwork((AsyncResult) msg.obj);
                break;

            default:
               break;
        }
    }

    @Override
    public void setPreferredNetworkType(int networkType, Message response) {
        boolean isNeedTurnOnRadio1 = true;
        boolean isNeedTurnOnRadio2 = true;

        boolean isRequestFromBootUpFlow=false;

        //1. if response.arg = 1 then this request must be come from GsmSST for boot up (setCurrentPreferredNwType())
        //2. if response == null the this request must be come from GeminiPhone. But in MMDSDC there is no GemeinPhone.
        if ((response == null)||(response.arg1 ==  1)){
            isRequestFromBootUpFlow = true;
        }

        //get current type
        int mCurrentNetworkMode = Settings.Global.getInt(getContext().getContentResolver(),
            Settings.Global.PREFERRED_NETWORK_MODE, Phone.PREFERRED_NT_MODE);

        //set to PREFERRED_NETWORK_MOD with new type         
        Settings.Global.putInt(mContext.getContentResolver(),Settings.Global.PREFERRED_NETWORK_MODE, networkType);

        //for boot up with 2G only - RILD need to know user's setting
        //set system prosperity  to sync. with RILD
        SystemProperties.set("gsm.mmdc.network.mode", Integer.toString(networkType));

        LOGD("setPreferredNetworkTypeLteDc currentType=" +mCurrentNetworkMode
            + "  networkType=" + networkType);

        switch (networkType) {
            case Phone.NT_MODE_LTE_GSM_WCDMA:   //switch to "4G Preferred"
                //change networkType to LTE_WCDMA (3/4G) -- AT+ERAT=5,4
                networkType = RILConstants.NETWORK_MODE_LTE_GSM_WCDMA_MMDC; 
                mCi.setPreferredNetworkType(networkType, response);
                break;

            case Phone.NT_MODE_GSM_WCDMA_LTE:   //switch to "3G or 2G Preferred"
                //chenge networkType to NETWORK_MODE_GSM_WCDMA_LTE_MMDC -- AT+ERAT=5,2
                //This type is only for MMDC mode which define in RILConstants not for Phone(APP)
                networkType = RILConstants.NETWORK_MODE_GSM_WCDMA_LTE_MMDC;
                mCi.setPreferredNetworkType(networkType, response);
                break;

            case Phone.NT_MODE_WCDMA_PREF:  //switch to "3G/2G(auto)"
                //chenge networkType to WCDMA only -- AT+ERAT=1,0
                networkType = Phone.NT_MODE_WCDMA_ONLY;
                mCi.setPreferredNetworkType(networkType, response);
                break;

            case Phone.NT_MODE_LTE_GSM:  //switch to "4G/2G"
                //chenge networkType to WCDMA only -- AT+ERAT=3,0
                networkType = RILConstants.NETWORK_MODE_LTE_GSM_MMDC;
                mCi.setPreferredNetworkType(networkType, response);
                break;

            case Phone.NT_MODE_WCDMA_ONLY:       //switch to "3G only" (for EM)
                //chenge networkType to WCDMA only -- AT+ERAT=1,0
                networkType = Phone.NT_MODE_WCDMA_ONLY;
                mCi.setPreferredNetworkType(networkType, response);
                isNeedTurnOnRadio2 = false;
                break;

            case Phone.NT_MODE_LTE_ONLY:       //switch to "4G only" (for EM)
                //chenge networkType to WCDMA only -- AT+ERAT=3,0
                networkType = Phone.NT_MODE_LTE_ONLY;
                mCi.setPreferredNetworkType(networkType, response);
                isNeedTurnOnRadio2 = false;
                break;

            case Phone.NT_MODE_GSM_ONLY:   //switch to "2G only"
                if (!isRequestFromBootUpFlow){
                    isNeedTurnOnRadio1 = false;
                } else {
                    if (response == null) {
                        LOGD("response is NULL");
                    } else if ((mCurrentNetworkMode == Phone.NT_MODE_GSM_ONLY) && (response.arg1 ==  1)) {
                        LOGD("boot up with 2G only");
                    } else {
                        //restore to currcnt network mode        
                        Settings.Global.putInt(mContext.getContentResolver(),Settings.Global.PREFERRED_NETWORK_MODE, mCurrentNetworkMode);
                        //restore to currcnt network mode
                        SystemProperties.set("gsm.mmdc.network.mode", Integer.toString(mCurrentNetworkMode));
                        //don't switch radio mode
                        isRequestFromBootUpFlow=true;                    
                        LOGD("exception on set to 2G only");
                    }
                }
                break;

            default:
                LOGE("RAT mode is not support");
                //restore to currcnt network mode
                Settings.Global.putInt(mContext.getContentResolver(),Settings.Global.PREFERRED_NETWORK_MODE, mCurrentNetworkMode);
                //restore to currcnt network mode
                SystemProperties.set("gsm.mmdc.network.mode", Integer.toString(mCurrentNetworkMode));
                //don't switch radio mode
                isRequestFromBootUpFlow=true;
                break;
            }       

        //response is null when GsmSST call setCurrentPreferredNwType to bootup
        //is this case we don't turn radio mode
        if (!isRequestFromBootUpFlow){
            boolean isRadioOn1 = mCi.getRadioState().isOn();
            boolean isRadioOn2 = mPeerGsmPhone.mCi.getRadioState().isOn();

            if (isNeedTurnOnRadio1 && isNeedTurnOnRadio2){
                if ((!isRadioOn1) || (!isRadioOn2)) {
                    LOGD("need to setRadioMode(MODE_DUAL_SIM) but need to sync with data service");
                    //call setPowerForRatModeChanged to sync with data service
                    mPeerGsmPhone.mSST.setPowerForRatModeChanged(null);
                    //mCi.setRadioMode(GeminiNetworkSubUtil.MODE_DUAL_SIM, null);
                }
            } else if(isNeedTurnOnRadio1 && (!isNeedTurnOnRadio2)){
                //for switch to "3G only" or "4G only" or "4+3G only"(for EM)
                //Turn on Radio1 and Turn off Radio 2 (AT+EFUN=1)
                if ((!isRadioOn1) || (isRadioOn2)) {
                    LOGD("need ot setRadioMode(MODE_SIM1_ONLY) but need to sync with data service" );
                    //call setPowerForRatModeChanged to sync with data service
                    mPeerGsmPhone.mSST.setPowerForRatModeChanged(null);
                    //mCi.setRadioMode(GeminiNetworkSubUtil.MODE_SIM1_ONLY, null);
                }
            } else if((!isNeedTurnOnRadio1) && (isNeedTurnOnRadio2)){
                //Turn off Radio1 (AT+EFUN=2)
                if ((isRadioOn1) || (!isRadioOn2)) { 
                    LOGD("need ot switch mode to GSM_ONLY, but need to sync with data service" );
                    mPeerGsmPhone.mSST.setPowerForRatModeChanged(response);   
                    //mCi.setRadioMode(GeminiNetworkSubUtil.MODE_SIM2_ONLY, response);
                } else {
                    if (response != null){
                        LOGD("no need to change Radio Mode");
                        AsyncResult.forMessage(response, null, null);
                        response.sendToTarget();
                    }
                }
            }
        } else {
            LOGD("RAT mode not support or in bootup flow");
        }
    }

    private void handleGetPreferredNetworkTypeResponse(Message msg){
        AsyncResult ar = (AsyncResult) msg.obj;

        Message response  =  (Message)(ar.userObj);

        if (ar.exception == null) {
            int[] modemNetworkMode = ((int[])ar.result);

            if (modemNetworkMode.length >= 1) {
                int P1NetworkType = modemNetworkMode[0];
                switch (P1NetworkType) {
                    case RILConstants.NETWORK_MODE_LTE_GSM_WCDMA_MMDC:
                        // AT+ERAT=x,x,5,4
                        //change network mode to 4G preferred (4G/3G/2G auto)
                        //because protocol 2 is always on (2G only)
                        modemNetworkMode[0] = Phone.NT_MODE_LTE_GSM_WCDMA;

                        LOGD("handleGetPreferredNetworkTypeResponse P1NetworkType= "
                                + P1NetworkType + " transfer to " + modemNetworkMode[0]);

                        AsyncResult.forMessage(response, modemNetworkMode, null);         
                        break;

                    case RILConstants.NETWORK_MODE_GSM_WCDMA_LTE_MMDC:
                        // AT+ERAT=x,x,5,2
                        //change network mode to 3G or 2G preferred
                        //because protocol 2 is always on (2G only)
                        modemNetworkMode[0] = Phone.NT_MODE_GSM_WCDMA_LTE;

                        LOGD("handleGetPreferredNetworkTypeResponse P1NetworkType= "
                                + P1NetworkType + " transfer to " + modemNetworkMode[0]);

                        AsyncResult.forMessage(response, modemNetworkMode, null);         
                        break;

                    case Phone.NT_MODE_WCDMA_ONLY:
                        // AT+ERAT=x,x,1,0

                        // if Radio 2 is on then it is 3G/2G(auto)  mode
                        if (mPeerGsmPhone.mCi.getRadioState().isOn()){
                            //change network mode to 3G/2G only (= 3G/2G auto = WCDMA preferred)
                           //because protocol 2 is always on (2G only) 
                            modemNetworkMode[0] = Phone.NT_MODE_WCDMA_PREF;
                        } else {
                            //Radio 2 is OFF 
                            //change network mode to 3G only (WCDMA only)
                            modemNetworkMode[0] = Phone.NT_MODE_WCDMA_ONLY;
                        }
                        LOGD("handleGetPreferredNetworkTypeResponse P1NetworkType= "
                                + P1NetworkType + " transfer to " + modemNetworkMode[0]);

                        AsyncResult.forMessage(response, modemNetworkMode, null);
                        break;

                    case Phone.NT_MODE_LTE_ONLY:
                        // AT+ERAT=x,x,3,0
                        // if Radio 2 is on then it is 4G/2G only mode
                        if (mPeerGsmPhone.mCi.getRadioState().isOn()){
                            //change network mode to 4G/2G only (= 4G/2G auto)
                            //because protocol 2 is always on (2G only) 
                            modemNetworkMode[0] = Phone.NT_MODE_LTE_GSM;
                        } else {
                            //change network mode to 4G only
                            modemNetworkMode[0] = Phone.NT_MODE_LTE_ONLY;
                        }

                        LOGD("handleGetPreferredNetworkTypeResponse P1NetworkType= "
                                    + P1NetworkType + " transfer to " + modemNetworkMode[0]);

                        AsyncResult.forMessage(response, modemNetworkMode, null);
                        break;

                    case Phone.NT_MODE_LTE_WCDMA:
                        // AT+ERAT=x,x,5,0
                        if (mPeerGsmPhone.mCi.getRadioState().isOn()){
                            modemNetworkMode[0] = Phone.NT_MODE_LTE_GSM_WCDMA;
                            LOGD("handleGetPreferredNetworkTypeResponse P1NetworkType= "
                                      + P1NetworkType + "and radio2 is on -> transfer to " + modemNetworkMode[0]);
                        } else {
                            modemNetworkMode[0] = Phone.NT_MODE_LTE_WCDMA;
                            LOGD("handleGetPreferredNetworkTypeResponse P1NetworkType= "
                                      + P1NetworkType + "and radio2 is off -> transfer to " + modemNetworkMode[0]);
                        }   
                        AsyncResult.forMessage(response, modemNetworkMode, null);
                        break;
                        
                    default:
                        LOGE("RAT mode is not support");
                        modemNetworkMode[0] = P1NetworkType;
                        AsyncResult.forMessage(response, modemNetworkMode, 
                                    new CommandException(CommandException.Error.INVALID_RESPONSE));                        
                    break;
                }                           
            }
        }else {
            AsyncResult.forMessage(response, ((int[])ar.result), ar.exception);
        }
        response.sendToTarget();
    }

    @Override
    public void updateSimIndicateState(){
        LOGD("updateSimIndicateState");
        //mPeerGsmPhone.updateSimIndicateState();
    }

    @Override
    public void notifyOtaspChanged(int otaspMode) {
        LOGD("Override - notifyOtaspChanged");
    }
    /*package*/
    @Override
    void notifyLocationChanged() {
         LOGD("Override - notifyLocationChanged");
    }    

    @Override
    public void notifyDataConnection(String reason) {
        LOGD("Override - notifyDataConnection");    
        //mPeerGsmPhone.notifyDataConnection(reason);
    }

    @Override
    public void getPreferredNetworkType(Message response) {

        //get current type
        int mCurrentNetworkMode = Settings.Global.getInt(getContext().getContentResolver(),
                                                        Settings.Global.PREFERRED_NETWORK_MODE, Phone.PREFERRED_NT_MODE);

        /* we can't check raido state to ensure RAT mode is 2G only, 
            because if data is not using then it will be set to off */
        //if ( !mCi.getRadioState().isOn()){
        if (mCurrentNetworkMode==Phone.NT_MODE_GSM_ONLY){
            LOGD("getPreferredNetworkType() mCurrentNetworkMode="+mCurrentNetworkMode+", return GSM_ONLY");
            int[] modemNetworkMode = new int[1];
            modemNetworkMode[0] = Phone.NT_MODE_GSM_ONLY;
            AsyncResult.forMessage(response, modemNetworkMode, null);
            response.sendToTarget();
        } else {
            mCi.getPreferredNetworkType(obtainMessage(MESSAGE_GET_PREFERRED_NETWORK_TYPE_MMDC, response));
        }
    }

    @Override
    public void
    getAvailableNetworks(Message response) {
        LOGD("before query available network, cleanup all data connections");
        mDcTracker.cleanUpAllConnections(null);
        Message msg = obtainMessage(MESSAGE_QUERY_AVAILABLE_NETWORK_MMDC);
        msg.obj = response;
        sendMessage(msg);
    }

    @Override
    public void removeReferences() {
        mSST = null;
        super.removeReferences();
    }

    protected void finalize() {
        LOGD("LteDcPhone finalized" );
    }

    public String getPhoneName() {
        return "LteDc";
    }

    public int getPhoneType() {
        return PhoneConstants.PHONE_TYPE_GSM;
    }

    @Override
    protected void LOGE(String message) {
        Rlog.e(LOG_TAG, "LteDcPhone(" + (mySimId+1) + ") :" + message);
    }

    @Override    
    protected void LOGI(String message) {
        Rlog.i(LOG_TAG, "LteDcPhone(" + (mySimId+1) + ") :" + message);
    }

    @Override
    protected void LOGD(String message) {
        Rlog.d(LOG_TAG, "LteDcPhone(" + (mySimId+1) + ") :" + message);
    }

    @Override
    protected void LOGW(String message) {
        Rlog.w(LOG_TAG, "LteDcPhone(" + (mySimId+1) + ") :" + message);
    }
}
