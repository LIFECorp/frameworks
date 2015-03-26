/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

//package com.android.internal.telephony.psarbitrator;
package com.android.internal.telephony;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneBase;
import com.android.internal.telephony.PhoneProxy;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.LteDcManager;
import com.android.internal.telephony.RIL;
import com.android.internal.telephony.RILConstants;
import com.android.internal.telephony.RIL.RilPsArbitrator;
import com.android.internal.telephony.DctConstants;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.dataconnection.DcTrackerBase;
import com.android.internal.telephony.worldphone.LteModemSwitchHandler;
import com.android.internal.util.AsyncChannel;
import com.android.internal.util.Protocol;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.app.PendingIntent;
import android.net.ProxyProperties;
import android.net.NetworkUtils;
import android.os.AsyncResult;
import android.os.Message;
import android.os.Handler;
import android.os.Looper;
import android.os.RegistrantList;
import android.os.Registrant;
import android.os.SystemProperties;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.TimeUtils;
import android.telephony.Rlog;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * {@hide}
 *
 * PsArbitrator Handler Module.
 *
 *
 * Instances are asynchronous state machines and have two primary entry points
 * <code>connect()</code> and <code>disconnect</code>. The message a parameter will be returned
 * hen the operation completes. The <code>msg.obj</code> will contain an AsyncResult
 * object and <code>AsyncResult.userObj</code> is the original <code>msg.obj</code>. if successful
 * with the <code>AsyncResult.result == null</code> and <code>AsyncResult.exception == null</code>.
 * If an error <code>AsyncResult.result = FailCause</code> and
 * <code>AsyncResult.exception = new Exception()</code>.
 *
 * The other public methods are provided for debugging.
 */
public class PsArbitrator extends Handler implements Runnable{
    protected static final boolean DBG = true;
    protected static final boolean VDBG = false;

    private static final String LOG_TAG = "PsArbitrator";
    private static final String SM_NAME = "PsArbitrator";

    // ***** PS Domain Preferred
    public static final int PS_PREFERRD_LTE = 0 ;
    public static final int PS_PREFERRD_QUALITY = 1 ;

    // ***** PS Domain mode
    public static final int PS_MODE_GSM = 0 ;
    public static final int PS_MODE_UMTS = 1 ;
    public static final int PS_MODE_LTE = 2 ; 
    public static final int PS_MODE_NONE = 255 ;

    public static final int PSD_DECISION_REJECT = 0;
    public static final int PSD_DECISION_OK = 1;
    public static final int PSD_DECISION_AP = 2;

    public static final int PS_PROTOCOL_1 = 0;
    public static final int PS_PROTOCOL_2 = 1;
   
    private static final int MMDC_PSD_SWITCH_CAUSE_PSDM = 0;
    private static final int MMDC_PSD_SWITCH_CAUSE_MANUAL_SELECTION = 1;
    private static final int MMDC_PSD_SWITCH_CAUSE_AUTO_RESELECTION = 2;
    private static final int MMDC_PSD_SWITCH_CAUSE_NC0 = 3;
    private static final int MMDC_PSD_SWITCH_CAUSE_UNKNOWN = 255;

    private static final int PS_DETACH_RETRY = 5;


    // ***** Event codes for driving the state machine
    public static final int BASE = 0x00049000;//Protocol.BASE_XXX;
    public static final int EVENT_PS_DECISION_CHANGED = BASE + 0;
    public static final int EVENT_PS_SWITCH = BASE + 1;
    public static final int EVENT_PS_SWITCH_CONFIRM = BASE + 2;
    public static final int EVENT_PS_SWITCH_ATTACH = BASE + 3;
    public static final int EVENT_SWITCH_RESET_GPRS_MODE = BASE + 4;
    public static final int EVENT_PS_SWITCH_DONE = BASE + 5;//Switch done
    public static final int EVENT_SYNC_DATA_CALL_LIST = BASE + 6;//Switch done
    public static final int EVENT_PS_QUERY_PSDM = BASE + 7;
    public static final int EVENT_SET_RAT_DETACH = BASE + 8;
    public static final int EVENT_SET_RAT_RESET_GPRS_MODE = BASE + 9;
    public static final int EVENT_RADIO_AVAILABLE_LTE = BASE + 10;
    public static final int EVENT_RADIO_NOT_AVAILABLE_LTE = BASE + 11;
    public static final int EVENT_RADIO_AVAILABLE_GSM = BASE + 12;
    public static final int EVENT_RADIO_NOT_AVAILABLE_GSM = BASE + 13;    
    public static final int EVENT_FLIGHT_MODE_ON = BASE + 14;
    public static final int EVENT_FLIGHT_MODE_OFF = BASE + 15;
    public static final int EVENT_SPECIAL_BAND_CHANGED = BASE + 16;
    public static final int EVENT_PS_SWITCH_REJECT = BASE + 17;
    public static final int EVENT_PS_DECISION_PDP = BASE + 18;
    
                
    private static final int CMD_TO_STRING_COUNT = EVENT_PS_DECISION_PDP - BASE + 1;
    private static String[] sCmdToString = new String[CMD_TO_STRING_COUNT];
    static {
        sCmdToString[EVENT_PS_DECISION_CHANGED - BASE] = "EVENT_PS_DECISION_CHANGED";
        sCmdToString[EVENT_PS_SWITCH - BASE] = "EVENT_PS_SWITCH";
        sCmdToString[EVENT_PS_SWITCH_CONFIRM - BASE] = "EVENT_PS_SWITCH_CONFIRM";
        sCmdToString[EVENT_PS_SWITCH_ATTACH - BASE] = "EVENT_PS_SWITCH_ATTACH";
        sCmdToString[EVENT_SWITCH_RESET_GPRS_MODE - BASE] = "EVENT_SWITCH_RESET_GPRS_MODE";
        sCmdToString[EVENT_PS_SWITCH_DONE - BASE] = "EVENT_PS_SWITCH_DONE";
        sCmdToString[EVENT_SYNC_DATA_CALL_LIST - BASE] = "EVENT_SYNC_DATA_CALL_LIST";
        sCmdToString[EVENT_PS_QUERY_PSDM - BASE] = "EVENT_PS_QUERY_PSDM";
        sCmdToString[EVENT_SET_RAT_DETACH - BASE] = "EVENT_SET_RAT_DETACH";
        sCmdToString[EVENT_SET_RAT_RESET_GPRS_MODE - BASE] = "EVENT_SET_RAT_RESET_GPRS_MODE";
        sCmdToString[EVENT_RADIO_AVAILABLE_LTE - BASE] = "EVENT_RADIO_AVAILABLE_LTE";
        sCmdToString[EVENT_RADIO_NOT_AVAILABLE_LTE - BASE] = "EVENT_RADIO_NOT_AVAILABLE_LTE";
        sCmdToString[EVENT_RADIO_AVAILABLE_GSM - BASE] = "EVENT_RADIO_AVAILABLE_GSM";
        sCmdToString[EVENT_RADIO_NOT_AVAILABLE_GSM - BASE] = "EVENT_RADIO_NOT_AVAILABLE_GSM";
        sCmdToString[EVENT_FLIGHT_MODE_ON - BASE] = "EVENT_FLIGHT_MODE_ON";
        sCmdToString[EVENT_FLIGHT_MODE_OFF - BASE] = "EVENT_FLIGHT_MODE_OFF";  
        sCmdToString[EVENT_SPECIAL_BAND_CHANGED - BASE] = "EVENT_SPECIAL_BAND_CHANGED"; 
        sCmdToString[EVENT_PS_SWITCH_REJECT - BASE] = "EVENT_PS_SWITCH_REJECT"; 
        sCmdToString[EVENT_PS_DECISION_PDP - BASE] = "EVENT_PS_DECISION_PDP";         
    }

    //***** Member Variables
    protected PhoneBase mPhone = null;
    protected RIL mRil;
    protected LteDcManager mLteDcManager;
    private boolean mInitDone = false;
    /**
     *    mDecisionPsMode --- PSDM notify to set (URC +EPSD)
     *    mCurrentPsMode --- Now PsArbitrator module think which one our data path. (like previous )
     *    RilArbitrator mDecisionPsMode --- over switching will sync to Ril (if ==none will queue data cmd)
     *    RilArbitrator mCurrentPsMode --- the real data use
     */   
    private int mDecisionPsMode = PS_MODE_NONE;
    private int mCurrentPsMode = PS_MODE_NONE;
    private int mDispatchProtocol = PS_PROTOCOL_2; // Ril some cmd will use through this variable to dispatch
    private boolean mUserDataEnabled = false;
    private boolean mIsAcceptPsdmSwitch = true;
    private static boolean mIsSwitching = false;
    private int mRat = Phone.NT_MODE_LTE_GSM_WCDMA;
    private boolean mLteModemOn = true;
    private int mDetachRetry = 0;
    private boolean isTurnOffLteBefore = false;
    private boolean mIsBandReattach = false;

    //epsd queue
    private boolean mIsInDecisionFlow = false;
    private boolean mIsQueueEpsd = false;
    private int mQueueDecision = PS_MODE_NONE;
    private int mQueueCause = MMDC_PSD_SWITCH_CAUSE_PSDM;

    protected static String cmdToString(int cmd) {
        cmd -= BASE;
        if ((cmd >= 0) && (cmd < sCmdToString.length)) {
            return sCmdToString[cmd];
        } else {
            return null;
        }
    }
    /**
     * @return the string for msg.what as our info.
     */   
    protected String getWhatToString(int what) {
        String info = null;
        info = cmdToString(what);

        return info;
    }    


   //***** Constructor
    public PsArbitrator(Looper looper, RIL ril) {

        super(looper);
        if (DBG) log("PsArbitrator constructor E");
        
        mRil = ril;
        mLteDcManager = mRil.getLteDcManager();

        //register the DecisionChanged
        if (mRil != null ){
            if (DBG) log("PsArbitrator register PSDM decision change to RIL");
            mRil.registerForPsdmDecisionChanged(this, DctConstants.EVENT_PSDM_DECISION_CHANGED, null);
            
        }
        if (mLteDcManager != null){
            if (DBG) log("PsArbitrator register PSDM decision change to LTEManager");

            //for Radio 1 on/off
            mLteDcManager.registerForPsdmDecisionChanged(this, DctConstants.EVENT_PSDM_DECISION_CHANGED, null);
            mLteDcManager.registerForAvailable(this, EVENT_RADIO_AVAILABLE_LTE, null);
            mLteDcManager.registerForOffOrNotAvailable(this, EVENT_RADIO_NOT_AVAILABLE_LTE, null);
        }
        
        initBroadCastReceiver();
        

        if (DBG) log("PsArbitrator constructor X");
    }

    public void
        run() {
            //setup if needed
    }    


    @Override
    protected void finalize() {
        dispose();
    }

    public void dispose() {
        if (DBG) log("PsArbitrator.dispose");
        mRil.unregisterForPsdmDecisionChanged(this);
        mLteDcManager.unregisterForPsdmDecisionChanged(this);
        mLteDcManager.unregisterForAvailable(this);
        mLteDcManager.unregisterForOffOrNotAvailable(this);        
    }

    //For Flight mode reset state
    protected void onLteRadioOff(AsyncResult ar){
        log("Radio 1 off reset Ps Arbitrator state");
        mLteModemOn = false;
        
        // reset the decision of RilPsArbitrator, then the data cmd queue will enqueue cmds 
        //mDecisionPsMode = PS_MODE_NONE;
        syncRilDecisionMode(PS_MODE_NONE);
        
        mIsInDecisionFlow = false;
        mIsQueueEpsd = false;
    }

    protected void onLteRadioOn(AsyncResult ar){
        log("Radio 1 on reset Ps Arbitrator state");
        mLteModemOn = true;
    }
    
    public boolean getLteModemOn(){
        return mLteModemOn;
    }
    public RIL getRil(){     
        return mRil;
    }

    public int getDecisionPsMode(){                               
        return mDecisionPsMode;
    }

    public int getCurrentPsMode(){        
        return mCurrentPsMode;
    }    

    public int getUsingProtocol(){
        int protocol = PS_PROTOCOL_1;

        if (mCurrentPsMode == PS_MODE_LTE || mCurrentPsMode == PS_MODE_UMTS){
            protocol = PS_PROTOCOL_1;//3G, 4G
        } else {            
            protocol = PS_PROTOCOL_2;//2G
        }
        
        return protocol;    
    }

    /**
        *    RIL.java send some requests which one protocol should dispatch via this API
        *
        *    @return: mDispatchProtocol (will be modified when Reset old protocol / attach new protocol)
        */
    public int getDispatchProtocol(){        
        return mDispatchProtocol;
    }

    /**
        *    Framework use this API to check the switching state
        *
        *    @return: mIsSwitching (when 3/4G <---> 2G will be set on, and set off after Ps switch done
        *                                      use setSwitchingState to set this value to sync System Property         )
        */
    public static boolean getIsSwitching(){
        return mIsSwitching;
    }

    /**
        *    For LTE baseband B3/B39 switch, we need reattach to make sure data can work (modem request)
        */
    public void setPsReattach(){
        if (!mIsSwitching){
            log("send EVENT_SPECIAL_BAND_CHANGED to reattach");
            mRil.requestPsReattach(obtainMessage(EVENT_SPECIAL_BAND_CHANGED));
            mIsBandReattach = true;
        } else {
            log("Ignore EVENT_SPECIAL_BAND_CHANGED when switching");
        }

    }

    /**
        *       Set mode
        *       AT+EPSD=<AP decision>, <target rat>
        *       <AP decision> 0: reject, 1: OK, 2: by AP        
        *       <target rat>    0: 2G,     1: 3G, 2: 4G, 255: Not available
        */
    public void setApDecisionPsMode(int mode){

        log("Ap set decision to PSDM: DecisionPSMode:" + mode + "CurrentPsMode" + mCurrentPsMode);
        //Manual set
        mRil.confirmPsSwitch(PSD_DECISION_AP, mode, obtainMessage(EVENT_PS_SWITCH_CONFIRM, mCurrentPsMode, 0));
        
    }

    /**
        *    Set mCurrentPsMode and sync to system property
        *    
        *    @param: mode : none/234G
        *                  sync  : sync back to RilPsArbitrator
        */
    public void updateCurrentPsMode(int mode , boolean sync){

        log("updateCurrentPsMode: new:" + mode + "old: " + mCurrentPsMode);
        
        mCurrentPsMode = mode;
        setProtocolSysProperty();

        if (sync){
            syncRilPsMode();
        }
            
    }

    /**
        *    Major function to handle Ps switch flow
        *    
        *    @param: ar :   ar.result --->  +EPSD: <decision>, <cause>
        *                  
        */
    protected void onDecisionChanged(AsyncResult ar){

        // only in MMDC mode, turn on this mechanism
        if (LteModemSwitchHandler.getActiveModemType() == LteModemSwitchHandler.MD_TYPE_LTNG){//MMDC mode

            if (ar.exception != null) {
                if (DBG) log("onDecisionChanged(ar): exception; likely radio not available, ignore");
                return;
            }
            
            //Init
            mPhone = (PhoneBase)(((PhoneProxy)PhoneFactory.getDefaultPhone()).getActivePhone());
            mUserDataEnabled = Settings.Global.getInt(
                    mPhone.getContext().getContentResolver(), Settings.Global.MOBILE_DATA, 1) == 1;
            mRat = Settings.Global.getInt(mPhone.getContext().getContentResolver(),
                    Settings.Global.PREFERRED_NETWORK_MODE, Phone.PREFERRED_NT_MODE);
            boolean isNowHasPs = (mCurrentPsMode != PS_MODE_NONE) ? true : false;
            int newDecision = mDecisionPsMode;//default value
            int cause = MMDC_PSD_SWITCH_CAUSE_PSDM;
            
            /**
                     *   Check if decision come close and need enqueue
                     *   mIsInDecisionFlow:  set "on" when in onDecisionChanged, set "off" when after send CGATT to attach
                     *   mIsQueueEpsd      :  if there decision in EPSD queue
                     */
            if (mIsInDecisionFlow){                
                mQueueDecision = ((int[])ar.result)[0]; //get new decision
                mQueueCause = ((int[])ar.result)[1];
                if (DBG) log("onDecisionChanged(ar): queue this Decision:" + mQueueDecision + ", cause:" + mQueueCause);
                mIsQueueEpsd = true;
                return;
            } else {
                //setDecisionFlow start, normal case
                mIsInDecisionFlow = true;            
                if (DBG) log("onDecisionChanged(ar): start");
            }

            /**
                     *   New decision or Pop EPSD queue to handle with
                     */            
            if (mIsQueueEpsd){
                newDecision = mQueueDecision;
                cause = mQueueCause;
                log("Pop Queue Decision"); 
                mIsQueueEpsd = false;
            } else {                                 
                //Read Urc data                   
                newDecision = ((int[])ar.result)[0]; //get new decision
                cause = ((int[])ar.result)[1];
            }

            
            int oldProtocol = (mCurrentPsMode == PS_MODE_GSM) ? PS_PROTOCOL_2 : PS_PROTOCOL_1;   
            if (mCurrentPsMode == PS_MODE_NONE) oldProtocol = -1;
            int newProtocol = (newDecision == PS_MODE_GSM) ? PS_PROTOCOL_2 : PS_PROTOCOL_1;
            if (newDecision == PS_MODE_NONE) newProtocol = -1;

            if (newProtocol == PS_PROTOCOL_1) mLteModemOn = true; // ESPD to Protocol1 means LTE on
            if (mRat == Phone.NT_MODE_GSM_ONLY) mLteModemOn = false;// Gsm only mode means Protocol1 off
            
            log("Current mode = " + mCurrentPsMode + ", old decision = " + mDecisionPsMode);        
            log("New decision = " + newDecision + ", cause : " + cause); 
            log("New Protocol / old Protocol: " + newProtocol + "/" + oldProtocol);
            log("mUserDataEnabled: " + mUserDataEnabled + "mLteModemOn: " + mLteModemOn);


            /**
                     *   For LTE baseband B3/B39 switch, we need reattach to make sure data can work (modem request)
                     *   if In reattach, abandon this EPSD
                     */
            if (mIsBandReattach) {
                log("B3/B39 reattach, reject this epsd");                
                mDispatchProtocol = (oldProtocol == -1) ? PS_PROTOCOL_2 : oldProtocol;
                if (mRat == Phone.NT_MODE_GSM_ONLY){
                    //if gsm only, confirm must send to protocol 2
                    mDispatchProtocol = PS_PROTOCOL_2;
                }                                
                mRil.confirmPsSwitch(PSD_DECISION_REJECT, newDecision, obtainMessage(EVENT_PS_SWITCH_REJECT, mCurrentPsMode, 0));
                return;
            }
            
                          
            //***Ps Switch Check*** start            
            //Check if need change
            if (newProtocol == oldProtocol && isNowHasPs){
                /* 
                        * [C1] 3/4G interRat, no need to switch
                        *        No need to change, if 3G <---> 4G , only do inter rat  
                        *        and no need to response EPSD confirm to modem
                        */
                if (DBG) log("[C1]newDecision/currentPs:" + newDecision + "/" + mCurrentPsMode + ", no need to switch!");
                if (DBG) log("[C1]3/4G interRat, no need to switch");
                mDecisionPsMode = newDecision;
                updateCurrentPsMode(newDecision, false); //3/4G update

                syncRilPsMode(); 
                
                if (mIsQueueEpsd){
                    log("Next decision to be handled");
                    sendMessage(obtainMessage(EVENT_PS_DECISION_PDP));
                } else {
                    log("EVENT_SYNC_DATA_CALL_LIST done, then trigger Ril executeMmdcDataCmdsQueue");
                    mIsInDecisionFlow = false;
                    mRil.getRilPsArbitrator().executeMmdcDataCmdsQueue();
                }        

                return;

            } else if (newProtocol != oldProtocol && (isAcceptPsdmSwitch() || cause == MMDC_PSD_SWITCH_CAUSE_MANUAL_SELECTION 
                    || cause == MMDC_PSD_SWITCH_CAUSE_AUTO_RESELECTION) ){
                //=====Trigger Switch!=====
                /* Ps is on one side, need to change to the other (Data on)
                           * send EPSD confirm !
                           * the flow is : 
                           *    EVENT_PS_SWITCH_CONFIRM (EPSD) -> 
                           *    EVENT_PS_SWITCH_ATTACH (EGTYPE=2, CGATT=1 to target protocol) -> 
                           *    EVENT_PS_SWITCH_DONE (EGTYPE=0, to original protocol) then switch done
                           *
                           * new flow
                           *    EVENT_PS_SWITCH_CONFIRM (EPSD) -> 
                           *    EVENT_RESET_GPRS_MODE (EGTYPE=0, to original protocol)  -> 
                           *    EVENT_PS_SWITCH_DONE (EGTYPE=2, CGATT=1 to target protocol)then switch done
                           */
               
                if (isNowHasPs){
                     /* 
                                * [C2] Normal Switch case,  2G <--->3/4G
                                *        Need to confirm EPSD, and follow the above flow 
                                */                
                    if (DBG) log("[C2]newDecision/currentPs:" + newDecision + "/" + mCurrentPsMode + ", need to switch!");
                    if (DBG) log("[C2]Normal switch case");


                    setSwitchingState(true);
                    mDecisionPsMode = newDecision;
                    //need confirm back to current
                    mDispatchProtocol = (oldProtocol == -1) ? PS_PROTOCOL_2 : oldProtocol;

                    if (mRat == Phone.NT_MODE_GSM_ONLY){
                        //if gsm only, confirm must send to protocol 2
                        mDispatchProtocol = PS_PROTOCOL_2;
                    }
                    
                    mRil.confirmPsSwitch(PSD_DECISION_OK, newDecision, obtainMessage(EVENT_PS_SWITCH_CONFIRM, mCurrentPsMode, 0));

                    return;
                                    
                }
                else if (mCurrentPsMode == PS_MODE_NONE){                
                     /* 
                                * [C3] Normal Start Setup Case
                                *        When Origin mobile data is off ---> on or Bootup
                                *        Need to setup decision and execute the RilPsQueue 
                                */              
                    if (DBG) log("[C3]newDecision/currentPs:" + newDecision + "/" + mCurrentPsMode + ", need to switch!");
                    if (DBG) log("[C3]Data Enable case");

                    setSwitchingState(true);
                    mDecisionPsMode = newDecision;
                    updateCurrentPsMode(mDecisionPsMode, false);
                    
                    executeRilQueue();
                    return;
                    
                } else {
                    //[C4] now shouldn't into this
                    //No data use now, only update the decision                          
                    if (DBG) log("[C4]newDecision/currentPs:" + newDecision + "/" + mCurrentPsMode + ", need to switch!");
                    mDecisionPsMode = newDecision;
                }
               
                
            } else if ( !isAcceptPsdmSwitch() && cause != MMDC_PSD_SWITCH_CAUSE_MANUAL_SELECTION){
                //[C5] Special setting
                //reject PSDM, except the cause is manual selection, we can't reject it
                if (DBG) log("[C5]Reject the PSDM switch suggestion!");            
                //need confirm back to current
                mDispatchProtocol = (oldProtocol == -1) ? PS_PROTOCOL_2 : oldProtocol;
                if (mRat == Phone.NT_MODE_GSM_ONLY){
                    //if gsm only, confirm must send to protocol 2
                    mDispatchProtocol = PS_PROTOCOL_2;
                }
                mRil.confirmPsSwitch(PSD_DECISION_REJECT, newDecision, obtainMessage(EVENT_PS_SWITCH_CONFIRM, mCurrentPsMode, 0));            

                return;
            
            } else if (mUserDataEnabled){
                 /* 
                            * [C6] Normal Start Setup Case
                            *        When Origin mobile data is off ---> on or Bootup
                            *        Need to setup decision and execute the RilPsQueue 
                            */              
                if (DBG) log("[C6]newDecision/currentPs:" + newDecision + "/" + mCurrentPsMode + ", need to switch!");
                if (DBG) log("[C6]Data Enable case");

                setSwitchingState(true);
                mDecisionPsMode = newDecision;
                updateCurrentPsMode(mDecisionPsMode, false);
                executeRilQueue();
                return;            

            } else {
                //[C7]nothing
                if (DBG) log("[C7]No Ps now");
                mDecisionPsMode = newDecision;
            }

            //***Ps Switch Check*** end
            
            //update the decision to RilPsArbitrator
            //syncRilPsMode();    
            
        }
    }

    private void onSwitchPsConfirmDone(AsyncResult ar){
        // Confirm EPSD done , 
        // Next step is reset gprs mode to original protocol        
        if (ar.exception != null) {
            if (DBG) log("onSwitchPsConfirmDone: exception; likely radio not available, ignore; or EPSD return error, don't switch");
            //Clean switching flag and pop queue
            //restore decision
            mDecisionPsMode = mRil.getRilPsArbitrator().mDecisionMode;
            mIsInDecisionFlow = false;
            executeRilQueue();
            return;
        }

        //for CGATT will slow response, update here first , when switch done, then update to Ril
        updateCurrentPsMode(mDecisionPsMode, false);
        
        //[S2] Send to "origin modem" reset GPRS mode        
        int protocol = (mDecisionPsMode == PS_MODE_GSM) ? PS_PROTOCOL_1 : PS_PROTOCOL_2;        
        if (DBG) log("onSwitchPsConfirmDone: mDecision:" + mDecisionPsMode + " reset protocol(peer decision):" + protocol);
        mRil.resetModeAfterSwitch(protocol, obtainMessage(EVENT_SWITCH_RESET_GPRS_MODE, mCurrentPsMode, 0));

    }   
    private void onResetGprsModeDone(AsyncResult ar){
        // Reset gprs to original protocol done , 
        // Next step is attach to new protocol
        
        if (ar.exception != null) {
            if (DBG) log("onResetGprsModeDone: exception; likely radio not available, ignore, but attach follow keep going");
            //Clean switching flag and pop queue
            //executeRilQueue();
            //return;
        }        
        
        //[S3] attach to new : send CGATT
        int protocol = (mDecisionPsMode == PS_MODE_GSM || mDecisionPsMode == PS_MODE_NONE) ? PS_PROTOCOL_2 : PS_PROTOCOL_1;
        if (DBG) log("onResetGprsModeDone: mDecision:" + mDecisionPsMode + " attach protocol:" + protocol);        
        mRil.confirmPsSwitchAttach(protocol, obtainMessage(EVENT_PS_SWITCH_DONE, mCurrentPsMode, 0));        

        /*
              * For PSDM, after send CGATT, will finish a cycle of PSDM
              * but for AP PsArbitrator, will finish till ExeQueue
              */
        //setDecisionFlow start
        mIsInDecisionFlow = false;
    }

    //old flow, temporary not be used now
    private void onSwitchPsAttachDone(AsyncResult ar){

    }    

    /**
     *  Switch data domain
     */
    private void onSwitchPsDone(AsyncResult ar){
                
        if (ar.exception != null) {
            if (DBG) log("onSwitchPsDone: exception; switch CGATT fail, ignore this fail");
            syncRilPsMode();
            if (!mIsInDecisionFlow) {
                if (DBG) log("no other command in process, set mIsSwitching to false");
                setSwitchingState(false);
                executeRilQueue();
            }
            return;
        }

        //===update now data===
        updateCurrentPsMode(mDecisionPsMode, false);
        log("onSwitchPsDone: mCurrentPsMode = " + mCurrentPsMode);

        executeRilQueue();
        
        return;
    }

    private void executeRilQueue(){
        log("switch done and execute Ril queue");
        setSwitchingState(false); // switching done
        syncRilPsMode();
        //[S4]
        //Sync Deactivate call, the return to RilPsArbitrator to pop queue
        mRil.getDataCallList(this.obtainMessage(EVENT_SYNC_DATA_CALL_LIST));
        
    }

    /**
        *    When we finish switching, need to sync back to RilPsArbitrator
        *    to make sure dispatch rule right.
        *    use inter executeRilQueue().
        */
    public void syncRilPsMode(){
        //Update to RilArbitrator
        mRil.getRilPsArbitrator().mDecisionMode = mDecisionPsMode;
        mRil.getRilPsArbitrator().mCurrentMode = mCurrentPsMode;
        log("Sync to Ril: DecisionPsMode:" + mDecisionPsMode + ", CurrentPsMode:" + mCurrentPsMode);
    }

    private void syncRilDecisionMode(int mode){
        mRil.getRilPsArbitrator().mDecisionMode = mode;
        log("Sync to Ril: DecisionPsMode:" + mode);
    }

    private void syncRilCurrentMode(int mode){
        mRil.getRilPsArbitrator().mCurrentMode = mode;
        log("Sync to Ril: CurrentPsMode:" + mode);
    }

    private void setProtocolSysProperty(){
        String value = null;
        if (mCurrentPsMode == PS_MODE_LTE || mCurrentPsMode == PS_MODE_UMTS){            
            value = "0";
        } else if (mCurrentPsMode == PS_MODE_GSM){
            value = "1";
        } else {
            value = "-1";
        }
        log("setProtocolSysProperty : " + value);
        SystemProperties.set("ril.epsd.protocol",value);
    }

    private void setSwitchingState (boolean state){
        mIsSwitching = state;

        String value = (state) ? "1" : "0" ; 
        log("setSwitching : " + value);
        
        SystemProperties.set("ril.epsd.switching", value);
    }

    public boolean isAcceptPsdmSwitch(){
        //This part can set if we accept Psdm for costumer.

        //default return true
        return mIsAcceptPsdmSwitch;
    }

    public void setAcceptPsdmSwitch(boolean accept){
        if (DBG) log("setAcceptPsdmSwitch E, mIsAcceptPsdmSwitch");       
        mIsAcceptPsdmSwitch = accept;
        if (DBG) log("setAcceptPsdmSwitch X, mIsAcceptPsdmSwitch:" + mIsAcceptPsdmSwitch);
    }

    private void onQueryDone(AsyncResult ar){
        log("onQueryDone");  

        if (ar.result == null) return;
        
        mDecisionPsMode = ((int[])ar.result)[1]; //get new decision
        updateCurrentPsMode(mDecisionPsMode, false);        
        syncRilPsMode();
        log("Current mode = " + mCurrentPsMode + ", decision = " + mDecisionPsMode);  
            
    }    

    /**
     * Clear all settings called when entering mInactiveState.
     */
    public void clearSettings() {
        if (DBG) log("clearSettings");
        
        mDecisionPsMode = PS_MODE_NONE;
        updateCurrentPsMode(PS_MODE_NONE, false);        
        mUserDataEnabled = false;
        mIsAcceptPsdmSwitch = true;
        setSwitchingState(false);

        mIsInDecisionFlow = false;
        mIsQueueEpsd = false;
        
        syncRilPsMode();
       
    }


    @Override
    public void handleMessage (Message msg) {
        if (DBG) log("PsArbitrator handleMessage msg=" + msg);

        switch (msg.what) {            
            case DctConstants.EVENT_PSDM_DECISION_CHANGED:
                //register EPSD URC
                log("EVENT_PSDM_DECISION_CHANGED");
                onDecisionChanged((AsyncResult) msg.obj);
                break;

            case EVENT_SPECIAL_BAND_CHANGED:
                log("EVENT_PSDM_DECISION_CHANGED done");
                mIsBandReattach = false;
                break;

            case EVENT_RADIO_NOT_AVAILABLE_LTE:
                //register for Radio off or not abailable
                onLteRadioOff((AsyncResult) msg.obj);
                break;

            case EVENT_RADIO_AVAILABLE_LTE:
                //register for Radio on then available 
                onLteRadioOn((AsyncResult) msg.obj);
                break;
                
            case EVENT_PS_SWITCH_CONFIRM:                
                //[S1] confirm done, then reset GPRS mode
                log("EVENT_PS_SWITCH_CONFIRM done");
                onSwitchPsConfirmDone((AsyncResult) msg.obj);                  
                break;

            case EVENT_PS_SWITCH_ATTACH:
                //No use temporary
                log("EVENT_PS_SWITCH_ATTACH done");
                onSwitchPsAttachDone((AsyncResult) msg.obj);  
                break;

            case EVENT_SWITCH_RESET_GPRS_MODE:
                //[S2] reset done, then attach new
                log("EVENT_SWITCH_RESET_GPRS_MODE done");
                onResetGprsModeDone((AsyncResult) msg.obj);  
                break;

            case EVENT_PS_SWITCH_DONE:
                //[S3] attach done, over switch => then sync and to execute queue
                log("EVENT_PS_SWITCH_DONE");
                onSwitchPsDone((AsyncResult) msg.obj);                
                break;                

            case EVENT_SYNC_DATA_CALL_LIST:
                //[S4] query data call list done, sync ap/modem connection, then execute queue

                mRil.SyncNotifyDataCallList((AsyncResult) msg.obj);               
                
                if (mIsQueueEpsd){
                    log("EVENT_SYNC_DATA_CALL_LIST done, wait for next decision end, will executeMmdcDataCmdsQueue");
                    sendMessage(obtainMessage(EVENT_PS_DECISION_PDP));
                } else {
                    log("EVENT_SYNC_DATA_CALL_LIST done, then trigger Ril executeMmdcDataCmdsQueue");
                    mIsInDecisionFlow = false;
                    mRil.getRilPsArbitrator().executeMmdcDataCmdsQueue();
                }
                
                break;               

            case EVENT_PS_SWITCH_REJECT:
                log("EVENT_PS_SWITCH_REJECT done, wait for next EPSD");
                break;

            case EVENT_PS_DECISION_PDP:
                log("EVENT_PS_DECISION_POP, wait for next EPSD");
                int result[] = {mQueueDecision,mQueueCause};
                AsyncResult tmpAr = new AsyncResult (null, result, null);
                mIsInDecisionFlow = false;
                onDecisionChanged(tmpAr);
                break;
                
            case EVENT_PS_QUERY_PSDM:
                log("EVENT_PS_QUERY_PSDM");
                onQueryDone((AsyncResult) msg.obj);
                break;
            default:
                log("Unexpect!");
                break;

        }
    }

    private void initBroadCastReceiver(){
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(TelephonyIntents.ACTION_SERVICE_STATE_CHANGED);
        
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                log("onReceive, action:" + action);
                if (action.equals(TelephonyIntents.ACTION_SERVICE_STATE_CHANGED)) {
                    log("onReceive, action:" + action);
                }
            }
        };
    }

    
    @Override
    public String toString(){
        String ps_state = "[PsArbitrator]targetRat/currentRat:" + mDecisionPsMode + "/" + mCurrentPsMode;
        return ps_state;
    }

    protected void log(String s) {
        Rlog.d(LOG_TAG, s);
    }
    
}
