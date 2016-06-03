package com.idevicesinc.sweetblue;

import com.idevicesinc.sweetblue.listeners.ReadWriteListener;

import java.util.UUID;

/**
 * Created by ryanbis on 6/3/16.
 */
public class P_Task_Write extends P_Task_RequiresConnection
{

    private UUID mServiceUuid;
    private UUID mCharUuid;
    private ReadWriteListener mListener;
    private byte[] mValue;


    public P_Task_Write(BleDevice device, IStateListener listener, UUID serviceUuid, UUID charUuid, byte[] value, ReadWriteListener writeListener)
    {
        super(device, listener);
        mServiceUuid = serviceUuid;
        mCharUuid = charUuid;
        mListener = writeListener;
        mValue = value;
    }

    @Override public void execute()
    {
        if (!getDevice().mGattManager.write(mServiceUuid, mCharUuid))
        {
            failImmediately();
        }
    }

    void onWrite(ReadWriteListener.ReadWriteEvent event)
    {
        if (mListener != null)
        {
            mListener.onEvent(event);
        }
        succeed();
    }

    byte[] getValue()
    {
        return mValue;
    }

    @Override public P_TaskPriority getPriority()
    {
        return P_TaskPriority.MEDIUM;
    }
}
