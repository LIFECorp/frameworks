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

package com.android.internal.tedongle;

/**
 * Contains a list of string constants used to get or set telephone properties
 * in the system. You can use {@link android.os.SystemProperties os.SystemProperties}
 * to get and set these values.
 * @hide
 */
public interface TelephonyProperties
{
    //****** Baseband and Radio Interface version

    //TODO T: property strings do not have to be gsm specific
    //        change gsm.*operator.*" properties to "operator.*" properties

    /**
     * Baseband version
     * Availability: property is available any time radio is on
     */
    static final String PROPERTY_BASEBAND_VERSION = "3gd.version.baseband";

    /** Radio Interface Layer (RIL) library implementation. */
    static final String PROPERTY_RIL_IMPL = "gsm.version.ril-impl-dongle";

    //****** Current Network

    /** Alpha name of current registered operator.<p>
     *  Availability: when registered to a network. Result may be unreliable on
     *  CDMA networks.
     */
    static final String PROPERTY_OPERATOR_ALPHA = "3gd.operator.alpha";
    //TODO: most of these properties are generic, substitute gsm. with phone. bug 1856959

    /** Numeric name (MCC+MNC) of current registered operator.<p>
     *  Availability: when registered to a network. Result may be unreliable on
     *  CDMA networks.
     */
    static final String PROPERTY_OPERATOR_NUMERIC = "3gd.operator.numeric";

    /** 'true' if the device is on a manually selected network
     *
     *  Availability: when registered to a network
     */
    static final String PROPERTY_OPERATOR_ISMANUAL = "3gd.operator.ismanual";

    /** 'true' if the device is considered roaming on this network for GSM
     *  purposes.
     *  Availability: when registered to a network
     */
    static final String PROPERTY_OPERATOR_ISROAMING = "3gd.operator.isroaming";

    /** The ISO country code equivalent of the current registered operator's
     *  MCC (Mobile Country Code)<p>
     *  Availability: when registered to a network. Result may be unreliable on
     *  CDMA networks.
     */
    static final String PROPERTY_OPERATOR_ISO_COUNTRY = "3gd.operator.iso-country";

    /**
     * The contents of this property is the value of the kernel command line
     * product_type variable that corresponds to a product that supports LTE on CDMA.
     * {@see BaseCommands#getLteOnCdmaMode()}
     */
    static final String PROPERTY_LTE_ON_CDMA_PRODUCT_TYPE = "3gd.lteOnCdmaProductType";

    /**
     * The contents of this property is the one of {@link Phone#LTE_ON_CDMA_TRUE} or
     * {@link Phone#LTE_ON_CDMA_FALSE}. If absent the value will assumed to be false
     * and the {@see #PROPERTY_LTE_ON_CDMA_PRODUCT_TYPE} will be used to determine its
     * final value which could also be {@link Phone#LTE_ON_CDMA_FALSE}.
     * {@see BaseCommands#getLteOnCdmaMode()}
     */
    static final String PROPERTY_LTE_ON_CDMA_DEVICE = "3gd.lteOnCdmaDevice";

    static final String CURRENT_ACTIVE_PHONE = "3gd.current.phone-type";

    //****** SIM Card
    /**
     * One of <code>"UNKNOWN"</code> <code>"ABSENT"</code> <code>"PIN_REQUIRED"</code>
     * <code>"PUK_REQUIRED"</code> <code>"NETWORK_LOCKED"</code> or <code>"READY"</code>
     */
    static String PROPERTY_SIM_STATE = "sys.3gd.sim.state";

    /** The MCC+MNC (mobile country code+mobile network code) of the
     *  provider of the SIM. 5 or 6 decimal digits.
     *  Availability: SIM state must be "READY"
     */
    static String PROPERTY_ICC_OPERATOR_NUMERIC = "3gd.operator.numeric";

    /** PROPERTY_ICC_OPERATOR_ALPHA is also known as the SPN, or Service Provider Name.
     *  Availability: SIM state must be "READY"
     */
    static String PROPERTY_ICC_OPERATOR_ALPHA = "3gd.operator.alpha";

    /** ISO country code equivalent for the SIM provider's country code*/
    static String PROPERTY_ICC_OPERATOR_ISO_COUNTRY = "3gd.operator.iso-country";

    /**
     * Indicates the available radio technology.  Values include: <code>"unknown"</code>,
     * <code>"GPRS"</code>, <code>"EDGE"</code> and <code>"UMTS"</code>.
     */
    static String PROPERTY_DATA_NETWORK_TYPE = "3gd.network.type";

    /** Indicate if phone is in emergency callback mode */
    static final String PROPERTY_INECM_MODE = "3gd.ril.cdma.inecmmode";

    /** Indicate the timer value for exiting emergency callback mode */
    static final String PROPERTY_ECM_EXIT_TIMER = "ro.3gd.cdma.ecmexittimer";

    /** The international dialing prefix conversion string */
    static final String PROPERTY_IDP_STRING = "ro.3gd.cdma.idpstring";

    /**
     * Defines the schema for the carrier specified OTASP number
     */
    static final String PROPERTY_OTASP_NUM_SCHEMA = "ro.3gd.cdma.otaspnumschema";

    /**
     * Disable all calls including Emergency call when it set to true.
     */
    static final String PROPERTY_DISABLE_CALL = "ro.3gd.disable-call";

    /**
     * Set to true for vendor RIL's that send multiple UNSOL_CALL_RING notifications.
     */
    static final String PROPERTY_RIL_SENDS_MULTIPLE_CALL_RING =
        "ro.3gd.call_ring.multiple";

    /**
     * The number of milliseconds between CALL_RING notifications.
     */
    static final String PROPERTY_CALL_RING_DELAY = "ro.3gd.call_ring.delay";

    /**
     * Track CDMA SMS message id numbers to ensure they increment
     * monotonically, regardless of reboots.
     */
    static final String PROPERTY_CDMA_MSG_ID = "persist.3gd.radio.cdma.msgid";

    /**
     * Property to override DEFAULT_WAKE_LOCK_TIMEOUT
     */
    static final String PROPERTY_WAKE_LOCK_TIMEOUT = "ro.3gd.ril.wake_lock_timeout";

    /**
     * Set to true to indicate that the modem needs to be reset
     * when there is a radio technology change.
     */
    static final String PROPERTY_RESET_ON_RADIO_TECH_CHANGE = "3gd.radio.reset_on_switch";

    /**
     * Set to false to disable SMS receiving, default is
     * the value of config_sms_capable
     */
    static final String PROPERTY_SMS_RECEIVE = "3gd.sms.receive";

    /**
     * Set to false to disable SMS sending, default is
     * the value of config_sms_capable
     */
    static final String PROPERTY_SMS_SEND = "3gd.sms.send";

    /**
     * Set to true to indicate a test CSIM card is used in the device.
     * This property is for testing purpose only. This should not be defined
     * in commercial configuration.
     */
    static final String PROPERTY_TEST_CSIM = "persist.3gd.radio.test-csim";

    /**
     * Ignore RIL_UNSOL_NITZ_TIME_RECEIVED completely, used for debugging/testing.
     */
    static final String PROPERTY_IGNORE_NITZ = "3gd.test.ignore.nitz";
}
