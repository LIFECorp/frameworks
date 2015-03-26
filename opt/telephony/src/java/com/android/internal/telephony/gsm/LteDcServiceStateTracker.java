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
 
package com.android.internal.telephony.gsm;

import android.content.Intent;
import android.os.SystemProperties;
import android.util.Log;

import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.TelephonyProperties;
import com.android.internal.telephony.PhoneFactory;

import com.mediatek.common.featureoption.FeatureOption;

/**
 * {@hide}
 */
final class LteDcServiceStateTracker extends GsmServiceStateTracker {
    static final boolean DBG = true;

    public LteDcServiceStateTracker(GSMPhone phone) {
        super(phone);
        log("Create LteDcServiceStateTracker");
    }    

    @Override
    public void dispose() {
        super.dispose();
    }

    protected void finalize() {
        if(DBG) log("finalize");
    }

    @Override
    protected void log(String s) {
        Log.d(LOG_TAG, "[LteDcSST] " + s);
    }

    @Override
    protected void loge(String s) {
        Log.e(LOG_TAG, "[LteDcSST] " + s);
    }

    private static void sloge(String s) {
        Log.e(LOG_TAG, "[LteDcSST]" + s);
    }

    //MTK-START for LTE
    @Override
    protected int updateOperatorAlpha(int mSimId, String operatorAlphaLong){    
        log("update PROPERTY_OPERATOR_ALPHA_LTEDC to "+operatorAlphaLong);
        mPhone.setSystemProperty(TelephonyProperties.PROPERTY_OPERATOR_ALPHA_LTEDC, operatorAlphaLong);
        return 1;
    } 

    @Override
    protected int updateOperatorNumeric(int mSimId, String operatorNumeric){    
        log("update PROPERTY_OPERATOR_NUMERIC_LTEDC to "+operatorNumeric);
        mPhone.setSystemProperty(TelephonyProperties.PROPERTY_OPERATOR_NUMERIC_LTEDC, operatorNumeric);
        return 1;
    }

    @Override
    protected int updateOperatorIsRoaming(int mSimId, boolean isRoaming){    
        log("update PROPERTY_OPERATOR_ISROAMING_LTEDC to "+ (isRoaming ? "true" : "false"));
        mPhone.setSystemProperty(TelephonyProperties.PROPERTY_OPERATOR_ISROAMING_LTEDC, (isRoaming ? "true" : "false"));
        return 1;
    }    

    @Override
    protected int updateOperatorIsoCountry(int mSimId, String operatorIsoCountry){    
        log("update PROPERTY_OPERATOR_ISO_COUNTRY_LTEDC to "+operatorIsoCountry);
        mPhone.setSystemProperty(TelephonyProperties.PROPERTY_OPERATOR_ISO_COUNTRY_LTEDC, operatorIsoCountry);
        return 1;
    } 

    @Override    
    protected int updateDataNetworkType(int mSimId, String dataNetworkType){    
        log("update PROPERTY_DATA_NETWORK_TYPE_LTEDC to "+dataNetworkType);
        mPhone.setSystemProperty(TelephonyProperties.PROPERTY_DATA_NETWORK_TYPE_LTEDC, dataNetworkType);
        return 1;
    }

    @Override
    protected int updateCsNetworkType(int mSimId, String csNetworkType){    
        log("update PROPERTY_CS_NETWORK_TYPE_LTEDC to "+csNetworkType);
        mPhone.setSystemProperty(TelephonyProperties.PROPERTY_CS_NETWORK_TYPE_LTEDC, csNetworkType);
        return 1;
    }    

    @Override    
    protected int updateRoamingIndicatorNeeded(int mSimId, boolean isNeeded){    
        log("update PROPERTY_ROAMING_INDICATOR_NEEDED_LTEDC to "+(isNeeded ? "true" : "false"));
        mPhone.setSystemProperty(TelephonyProperties.PROPERTY_ROAMING_INDICATOR_NEEDED_LTEDC, isNeeded ? "true" : "false");
        return 1;
    }    

    @Override
    protected String getOperatorNumericBySim(int mSimId){
        String retStr = "";    
        retStr = SystemProperties.get(TelephonyProperties.PROPERTY_OPERATOR_NUMERIC_LTEDC, "");
        return retStr;
    } 

    @Override
    protected String getOperatorIsoCountry(int mSimId){
        String retStr = "";    
        retStr = SystemProperties.get(TelephonyProperties.PROPERTY_OPERATOR_ISO_COUNTRY_LTEDC);
        return retStr;
    }

    @Override
    protected Intent createSpnUpdateIntent(){
        return new Intent(TelephonyIntents.SPN_STRINGS_UPDATED_ACTION_LTE_DC);
    }

    @Override
    protected void setCurrentPreferredNwType(){
        // TODO: needs to send correct ERAT by settings
        log("override:setCurrentPreferredNwType() do nothing.");
    }    

    //For MMDC project this method only used by GsmSST 
    @Override
    protected void setPowerStateToDesired() {
        log("override:setPowerStateToDesired() do nothing.");
    }    
    //MTK-END    
    
}
