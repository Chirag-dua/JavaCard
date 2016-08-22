package com.gotrust.sesdtest;

import android.app.Activity;
import android.content.Context;
import android.widget.TextView;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.widget.ListView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.EditText;

import java.io.File;
import java.util.*;

import com.gotrust.sesdapi.*;
import com.gotrust.sesdtest.R;

public class SESDTest extends Activity
{
	private String ioFileName;
	
	private EditText mEditText;
	private ListView mListView1;
	private TextView mTextView1;
	private TextView mTextResult;
	private String[] myOpt;
	private boolean inTesting = false;
	
	Thread createIoFileThread;
	CreateIoFileRunnable createIoFileRunnable;
	QueryCreateIoFileProgressTask queryProgressTask;

	public static final int MSG_CREATE_FILE_FINISHED = 1;
	
	private Handler createFileHandler;
	
	// add 
    SESDAPI obj = new SESDAPI();

    // The path of secure microSD
    String ioFilePath = new String();

    // 
    int fd = -1;

	private PowerManager.WakeLock mWakeLock; 
	private PowerManager mPowerManager;
	private boolean ifLocked;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
	{
    	super.onCreate(savedInstanceState);
    	
    	ioFileName = "SMART_IO.CRD";
    	
    	setContentView(R.layout.main);
    	
    	createFileHandler = new Handler() {
    		public void handleMessage(Message msg) {
    			switch(msg.what)
    			{
    				case MSG_CREATE_FILE_FINISHED:
    					queryProgressTask.cancel(false);
    					
    					switch(createIoFileRunnable.getResult())
    					{
    						case 0:
    							mTextResult.setText("Created I/O file successfully!\n");
    							break;
    							
    						case SESDAPI.SDSC_DEV_NO_SPACE_ERROR:
    							mTextResult.setText("Insufficient disk space. Extra " + createIoFileRunnable.getExtraSpaceNeeded() + "MB needed!\n");
    							break;
    							
    						default:
    							mTextResult.setText("Failed to create I/O file.\nError code = " + createIoFileRunnable.getResult() + "\n");
    							break;
    					}
    					break;
    				
    				default:
    					break;
    			}
    		}
    	};

    	mPowerManager = (PowerManager)getSystemService(Context.POWER_SERVICE); 
        // WakeLock
        mWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "FULL_WAKE_LOCK");
        
    	goMenuView();
    }

    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        switch(keyCode)
        {
            case KeyEvent.KEYCODE_BACK:
              if(createIoFileThread != null && createIoFileThread.isAlive())
            	  return true;	// wait for creating I/O file
            
              if(inTesting == true)
              {
                  goMenuView();
                  return false;
              }
              
              break;
        }
        
        return super.onKeyDown(keyCode, event);
    }

    // show menu view
    private void goMenuView()
    {
    	setContentView(R.layout.main);
    	inTesting = false;
        
    	mEditText = (EditText) findViewById(R.id.editText1);
    	mEditText.setText(ioFileName);
    	
    	mListView1 =(ListView) findViewById(R.id.myListView1); 
        mTextView1 = (TextView) findViewById(R.id.myTextView1); 
        mTextView1.setText(getResources().getString(R.string.menu));
        myOpt = new String[] {
                                   getResources().getString(R.string.list_opt_create_io_file), 
                                   getResources().getString(R.string.list_opt_list_dev), 
                                   getResources().getString(R.string.list_opt_connect_dev), 
                                   getResources().getString(R.string.list_opt_get_dev_info),
                                   getResources().getString(R.string.list_opt_apdu_test),
                                   getResources().getString(R.string.list_opt_disconnect_dev)
                                 };
        ArrayAdapter<String> adapter = new 
        ArrayAdapter<String> 
        (SESDTest.this, android.R.layout.simple_list_item_1, myOpt);

        mListView1.setAdapter(adapter);
        mListView1.setItemsCanFocus(true);  
        mListView1.setChoiceMode 
        (ListView.CHOICE_MODE_SINGLE); 
        mListView1.setOnItemClickListener(new ListView.OnItemClickListener()
        {
        	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,long arg3)
        	{
        		ioFileName = mEditText.getText().toString();
        		
        		String operationItem = arg0.getAdapter().getItem(arg2).toString();
        		if (operationItem == getResources().getString(R.string.list_opt_create_io_file))
        		{
        			goCreateIoFile();
        		}
        		else if (operationItem == getResources().getString(R.string.list_opt_list_dev))
        		{
        			goListDev();
        		}
        		else if (operationItem == getResources().getString(R.string.list_opt_connect_dev))
        		{
        			goConnectDev();
        		}
        		else if (operationItem == getResources().getString(R.string.list_opt_get_dev_info))
        		{
        			goDevInfo();
        		}
        		else if (operationItem == getResources().getString(R.string.list_opt_apdu_test))
        		{
        			goAPDUTest();
        		}
        		else if (operationItem == getResources().getString(R.string.list_opt_disconnect_dev))
        		{
        			goDisconnectDev();
        		}
        		else
        		{
        			mTextView1.setText("Ooops!!");
        		}
        	}
        });
    }
    
    private void ShowData(byte[] data,int off,int lenData)
    {
        if(data==null)
        {
          return;
        }
        if((off < 0) || (lenData < 0) || (off + lenData > data.length))
        {
          return;     
        }

        String message="";
        int ret=0;

        for (int i = 0; i < lenData; i++)
        {
            if (data[off+i] < 0)
                ret = 256 + data[off+i];
            else
                ret=data[off+i];

            if (ret < 0x10)
                message+="0";
            message+=(Integer.toHexString(ret)+ " ");
            if (i % 16 == 15)
                message=message.concat("\n");
        }
        mTextResult.append(message+"\n");
    }
    
    /**
     * Get the I/O file path on the secure microSD.
     * @return Returns the I/O file path; returns null if microSD not found.
     */
    private String getIoFilePath()
    {
    	if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT)
    	{
        	File[] fileArray = getExternalFilesDirs (null);
        	if(fileArray.length < 2)
        	{
        		return null;	// microSD not inserted
        	}
        	else if(fileArray[1] == null)
        	{
        		return null;	// microSD may not be mounted
        	}
        	
        	String tmp = fileArray[1].getPath();
        	return tmp.substring(0, tmp.indexOf("/files") + 1) + ioFileName;
    	}
    	else
    	{
    		// for non-Kitkat version, the I/O file can be under root directory 
    		return ioFileName;
    	}
    }

    private void goCreateIoFile()
    {
        setContentView(R.layout.myresult);
        inTesting = true;

        mTextResult = (TextView) findViewById(R.id.myTextResult);
        mTextResult.setKeepScreenOn(true);
    	mTextResult.setVerticalScrollBarEnabled(true);
        mTextResult.append("\n");

        String tempIoFilePath;
        
    	if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT)
    	{
    		tempIoFilePath = getIoFilePath();
        	
        	if(tempIoFilePath == null)
        	{
        		mTextResult.append("Please insert microSD first!");
        		return;
        	}
    	}
    	else
    	{
    		mTextResult.append("No need to do this function for non-Kitkat vesion.");
    		return;
    	}
    	
    	createIoFileRunnable = new CreateIoFileRunnable(obj, tempIoFilePath, createFileHandler);
    	createIoFileThread = new Thread(createIoFileRunnable);
    	createIoFileThread.start();
    	queryProgressTask = new QueryCreateIoFileProgressTask();
    	queryProgressTask.execute(tempIoFilePath);
    }
    
    private void goListDev()
    {
        setContentView(R.layout.myresult);
        inTesting = true;

        mTextResult = (TextView) findViewById(R.id.myTextResult);
        mTextResult.setKeepScreenOn(true);
    	mTextResult.setVerticalScrollBarEnabled(true);
        mTextResult.append("\n");

        long time = System.currentTimeMillis();
        
        obj.SDSCSetProductType(SESDAPI.SDSC_PRODUCT_TYPE_NS);
        
        // list dev
        Vector<String> vDevs = new Vector<String>();
        try
        {
        	String tempIoFilePath = getIoFilePath();
        	if(tempIoFilePath == null)
        	{
        		mTextResult.append("Please insert microSD first!");
        		return;
        	}
        	
        	vDevs = obj.SDSCListDevs(tempIoFilePath);
            if(vDevs.size() == 0)
                mTextResult.append("No secure microSD found.\n");
            else
            {
        	    mTextResult.append("Secure microSD path:\n");
                String strTemp = "";
                for(int i=0;i<vDevs.size();i++)
                	strTemp += (String)vDevs.elementAt(i)+"\n";
        	    mTextResult.append(strTemp);

        	    // save first device
        	    ioFilePath = (String)vDevs.elementAt(0);
            }
        	mTextResult.append("\n");
        }
        catch(SDSCException e)
        {
        	mTextResult.append(e.getMessage());
        }
        
        mTextResult.append("Elapsed time: " + (System.currentTimeMillis() - time) + " ms\n");
    }
    
    private void goConnectDev()
    {
        setContentView(R.layout.myresult);
        inTesting = true;

        mTextResult = (TextView) findViewById(R.id.myTextResult);
        mTextResult.setKeepScreenOn(true);
    	mTextResult.setVerticalScrollBarEnabled(true);
    	mTextResult.append("\n");

        // get sdk version
        try
        {
            String strSDKVer; 
            strSDKVer = obj.SDSCGetSDKVersion();
            mTextResult.append("SDK Version: " + strSDKVer + "\n");
        }
        catch(SDSCException e)
        {
            mTextResult.append(e.getMessage());
        }

        // get protocol version
        try
        {
            String strProtocolVer;
            strProtocolVer = obj.SDSCGetProtocolVersion();
            mTextResult.append("Protocol version: " + strProtocolVer + "\n");
        }
        catch(SDSCException e)
        {
            mTextResult.append(e.getMessage());
        }

        long time = System.currentTimeMillis();
        
        // connect dev
        try
        {
    	    fd = obj.SDSCConnectDev(ioFilePath);
    	    
    	    mTextResult.append("SDSCConnectDev ok. Handle = " + fd + "\n");
        }
        catch(SDSCException e)
        {
            mTextResult.append(e.getMessage());
        }
        
	    mTextResult.append("Elapsed time: " + (System.currentTimeMillis() - time) + " ms\n");
    }

    private void goDevInfo()
    {
        setContentView(R.layout.myresult);
        inTesting = true;

        mTextResult = (TextView) findViewById(R.id.myTextResult);
        mTextResult.setKeepScreenOn(true);
    	mTextResult.setVerticalScrollBarEnabled(true);
    	mTextResult.append("\n");
    	
        long time = System.currentTimeMillis();

        // Firmware Ver
        try
        {
            byte bFirmwareVer[] = new byte[SESDAPI.SDSC_FIRMWARE_VER_LEN]; // Firmware version
            int intFirmwareVerLen;
            intFirmwareVerLen = obj.SDSCGetFirmwareVer(fd, bFirmwareVer);
            mTextResult.append("SDSCGetFirmwareVer ok.\n FirmwareVer: ");
            ShowData(bFirmwareVer, 0, intFirmwareVerLen);
        }
        catch(SDSCException e)
        {
            mTextResult.append(e.getMessage());
        }

        // Device Info
        try
        {
            byte bDeviceInfo[] = new byte[SESDAPI.SDSC_DEVICE_INFO_LEN];
            int intDeviceInfoLen; 
            intDeviceInfoLen = obj.SDSCGetDeviceInfo(fd, bDeviceInfo);
            mTextResult.append("SDSCGetDeviceInfo ok.\n Device Info:\n");
            ShowData(bDeviceInfo, 0, intDeviceInfoLen);
        }
        catch(SDSCException e)
        {
            mTextResult.append(e.getMessage());
        }

        // Reset
        try
        {
            byte bAtr[] = new byte[128]; 
            int intAtrLen;
            intAtrLen = obj.SDSCResetCard(fd, bAtr);
            mTextResult.append("SDSCResetCard ok.\n Atr:\n");
     	    ShowData(bAtr, 0, intAtrLen);
        }
        catch(SDSCException e)
        {
            mTextResult.append(e.getMessage());
        }

        // Get SCIO Type
        try
        {
            int intSCIOType;
            intSCIOType = obj.SDSCGetSCIOType(fd);
            mTextResult.append("SDSCGetSCIOType ok.\n SCIOType: ");
            if(intSCIOType == SESDAPI.SDSC_SCIO_7816)
            	mTextResult.append("SDSC_SCIO_7816.\n");
            else if(intSCIOType == SESDAPI.SDSC_SCIO_SPI_V1)
            	mTextResult.append("SDSC_SCIO_SPI_V1.\n");
            else if(intSCIOType == SESDAPI.SDSC_SCIO_SPI_V2)
            	mTextResult.append("SDSC_SCIO_SPI_V2.\n");
            else if(intSCIOType == SESDAPI.SDSC_SCIO_SPI_V3)
            	mTextResult.append("SDSC_SCIO_SPI_V3.\n");
            else
               	mTextResult.append("SDSC_SCIO_UNKNOWN.\n");
        }
        catch(SDSCException e)
        {
            mTextResult.append(e.getMessage());
        }

        mTextResult.append("Elapsed time: " + (System.currentTimeMillis() - time) + " ms\n");
    }

    private void goAPDUTest()
    {
        setContentView(R.layout.myresult);
        inTesting = true;

        mTextResult = (TextView) findViewById(R.id.myTextResult);
        mTextResult.setKeepScreenOn(true);
    	mTextResult.setVerticalScrollBarEnabled(true);
    	mTextResult.append("\n");

        byte bCommand[] = new byte[512];
        byte bOutData[] = new byte[512];
        int intResult = 0;

        long time = System.currentTimeMillis();

        // Test APDU: Select CardManager
        bCommand[0] = 0x00;
        bCommand[1] = (byte)0xA4;
        bCommand[2] = 0x04;
        bCommand[3] = 0x00;
        bCommand[4] = 0x08;
        bCommand[5] = (byte)0xA0;
        bCommand[6] = 0x00;
        bCommand[7] = 0x00;
        bCommand[8] = 0x00;
        bCommand[9] = 0x03;
        bCommand[10] = 0x00;
        bCommand[11] = 0x00;
        bCommand[12] = 0x00;

        try
        {
        	mTextResult.append("Send APDU:\n");
        	ShowData(bCommand, 0, 13);
        	
            intResult = obj.SDSCTransmitEx(fd, bCommand, 0, 13, SESDAPI.SDSC_DEV_DEFAULT_TIME_OUT, bOutData);
        }
        catch(SDSCException e)
        {
            mTextResult.append("Send APDU error.\n");
            mTextResult.append(e.getMessage());
            return;
        }
        
        while(true)
        {
	        if(bOutData[intResult-2] != (byte)0x90 && bOutData[intResult-2] != 0x61)
	        {
	            mTextResult.append("Send APDU error, state code: ");
	            ShowData(bOutData, intResult-2, 2);
	        }
			else
			{
				mTextResult.append("Send APDU OK, state code: ");
				ShowData(bOutData, intResult-2, 2);
				
				if(intResult > 2)
				{
					mTextResult.append("Response:\n");
					ShowData(bOutData, 0, intResult-2);
				}
			}
	        
	        // handle GET RESPONSE
	        if(bOutData[intResult - 2] == 0x61)
	        {
	        	bCommand[0] = 0x00;
	        	bCommand[1] = (byte)0xC0;
	        	bCommand[2] = 0x00;
	        	bCommand[3] = 0x00;
	        	bCommand[4] = bOutData[intResult - 1];
	        	
	            try
	            {
	            	mTextResult.append("Send APDU:\n");
	            	ShowData(bCommand, 0, 5);
	            	
	                intResult = obj.SDSCTransmitEx(fd, bCommand, 0, 5, SESDAPI.SDSC_DEV_DEFAULT_TIME_OUT, bOutData);
	            }
	            catch(SDSCException e)
	            {
	                mTextResult.append("Send APDU error.\n");
	                mTextResult.append(e.getMessage());
	                return;
	            }
	            
	            continue;
	        }
	        
	        break;
        }

        mTextResult.append("Elapsed time: " + (System.currentTimeMillis() - time) + " ms\n");
   }

    private void goDisconnectDev()
    {
        setContentView(R.layout.myresult);
        inTesting = true;

        mTextResult = (TextView) findViewById(R.id.myTextResult);
        mTextResult.setKeepScreenOn(true);
    	mTextResult.setVerticalScrollBarEnabled(true);
    	mTextResult.append("\n");
    	
    	long time = System.currentTimeMillis();

        try
        {
        	obj.SDSCDisconnectDev(fd);
            fd = -1;
            mTextResult.append("SDSCDisconnectDev ok.\n");
        }
        catch(SDSCException e)
        {
        	mTextResult.append("SDSCDisconnectDev error");
            mTextResult.append(e.getMessage());
            return;
        }
        
        mTextResult.append("Elapsed time: " + (System.currentTimeMillis() - time) + " ms\n");
    }

    @Override 
    protected void onResume() 
    {
        wakeLock(); 
        super.onResume(); 
    }

    @Override 
    protected void onPause()
    {
        wakeUnlock();
        super.onPause();
    }
    
    
    protected void onDestroy()
    {
    	if(fd != -1)
    	{
            try
            {
            	obj.SDSCDisconnectDev(fd);
                fd = -1;
            }
            catch(SDSCException e)
            {
            }
    	}

    	super.onDestroy();
    }
    
    private void wakeLock()
    {
        if (!ifLocked)
        {
            ifLocked = true;
            mWakeLock.acquire();
        }
    }

    private void wakeUnlock()
    {
        if (ifLocked)
        {
            mWakeLock.release();
            ifLocked = false;
        }
    }

    protected class QueryCreateIoFileProgressTask extends AsyncTask<String, Short, Short> {
		String ioFilePath;
		
		@Override
		protected Short doInBackground(String... ioFilePaths) {
			short progress = 0;
			this.ioFilePath = ioFilePaths[0];
			
			while(progress < 100 && !isCancelled())
			{
				try
				{
					Thread.sleep(1000);
					progress = obj.SDSCGetCreateIoFileProgress(ioFilePaths[0]);
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (SDSCException e) {
					if(e.getReason() == SESDAPI.SDSC_DEV_NO_FILE_ERROR)
					{
						progress = 0;
					}
					else
					{
						cancel(false);
					}
				}
				
				
				publishProgress(progress);
			}
			
			return progress;
		}

		@Override
		protected void onProgressUpdate(Short... values) {
			mTextResult.setText("Creating I/O file under: " + ioFilePath + "\n");
			mTextResult.append("Progress: " + values[0] + "%");
		}    	
    }
}

class CreateIoFileRunnable implements Runnable
{
	private SESDAPI obj;
	private String ioFilePath;
	private Handler handler;
	private int result;
	private int extraSpaceNeeded; 
	
	CreateIoFileRunnable(SESDAPI obj, String ioFilePath, Handler handler)
	{
		this.obj = obj;
		this.ioFilePath = ioFilePath;
		this.handler = handler;
	}
	
	public void run()
	{
		try
		{
			extraSpaceNeeded = obj.SDSCCreateIoFile(ioFilePath);
			if(extraSpaceNeeded > 0)
				result = SESDAPI.SDSC_DEV_NO_SPACE_ERROR;
			else
				result = 0;
		}
		catch(SDSCException e)
		{
			result = e.getReason();
		}
		
		handler.sendEmptyMessage(SESDTest.MSG_CREATE_FILE_FINISHED);
	}
	
	public int getResult()
	{
		return result;
	}
	
	public int getExtraSpaceNeeded()
	{
		return extraSpaceNeeded;
	}
}
