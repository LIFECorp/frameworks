package com.mediatek.aop;

import java.lang.reflect.Modifier;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;

import android.util.Log;

/**
 * @author vend_john.bai
 * The Aspect is for logging the message exchange between BT java framework & BT native layer
 */
public aspect TraceAspect {
    private final static String TAG = "Aspect-BTSDK";
    private final static String INDENT = "        ";

    // pointcut to intercept a method in the application project

    ///Intercept framework logs
    protected pointcut traceOperations(): 
        execution(public * *(..))
        && (within(android.bluetooth.BluetoothAdapter) || within(android.bluetooth.BluetoothHeadset))
        && if(!thisJoinPointStaticPart.getSignature().getDeclaringType().isAnonymousClass());

    before() : traceOperations() {
        Signature sig = thisJoinPointStaticPart.getSignature();
        Object[] args = thisJoinPoint.getArgs();

        log("SDK ->Invoke [" + sig.toLongString() + "]");

        ///Log arguments
        printArgs(args);
    }

    after() returning(Object r) : traceOperations() {
        Signature sig = thisJoinPointStaticPart.getSignature();
        
        ///Log return
        log("SDK ->Return [" + sig.getName() + "]" + "Value:[" + r + "]");
    }


    ///Print the arguments' content based on different typess
    protected void printArgs(Object[] args)
    {   
        boolean bIsNeedIndent = false;

        for(int i = 0; i< args.length; i++){

            StringBuffer sb = new StringBuffer();
            sb.append(INDENT + "Arg");
            sb.append(i).append(":[");

            ///Layout log string based on different argument types
            if(args[i] instanceof int[])
            {
                int[] temp = (int[])args[i];
                for(int j = 0; j < temp.length; j++)
                {
                    sb.append(temp[j]);

                    if(j != temp.length - 1)
                        sb.append(",");
                }
            }
            else if(args[i] instanceof byte[][])
            {
                bIsNeedIndent = true;

                byte[][] temp = (byte[][])args[i];

                for(int j = 0; j < temp.length; j++)
                {
                    if(j == 0)
                        sb.append("\n");

                    sb.append(INDENT).append(INDENT).
                    append("["+j+"] = ");

                    for(int k = 0; k < temp[j].length; k++)
                    {
                        sb.append(temp[j][k]);
                        if(k != temp[j].length - 1)
                            sb.append(",");
                    }
                    if(j != temp.length)
                    {
                        sb.append("\n");
                    }
                    else
                    {
                        sb.append("\n").append(INDENT);
                    }
                }
            }
            else if(args[i] instanceof byte[])
            {
                sb.append(addressToString((byte[])args[i]));
            }
            else
            {
                sb.append(args[i]);
            }

            if(bIsNeedIndent)
                sb.append(INDENT);

            sb.append("]");

            log(sb.toString());

            ///Separate Args
            if(i == args.length - 1)
                log("        \n");
        }
    }

    protected void log(String msg) {
        Log.i(TAG, msg);
    }

    protected String addressToString(byte[] address) {
        String s = null;

        for(int i = 0; i< address.length ; i++)
        {
            if(s == null)
                s  = String.format("%h", address[i] & 0xFF);
            else
                s  = s + String.format(":%h", address[i] & 0xFF);
        }
        return s;
    }
}
