package com.android.bluetoothprinter;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.bluetoothprinter.Utils.BitMapUtil;
import com.android.bluetoothprinter.Utils.BluetoothUtil;
import com.android.bluetoothprinter.Utils.ESCUtil;
import com.android.bluetoothprinter.Utils.ButtonDelayUtils;
import com.android.bluetoothprinter.Utils.HandlerUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Random;

public class BlutetoothPrinterDemo extends AppCompatActivity implements OnClickListener{
    private static final String TAG = "BlutetoothPrinter";
    /* Demo 版本号*/
    private static final String VERSION        = "V1.1.1";

    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothDevice mBluetoothPrinterDevice = null;
    private BluetoothSocket socket = null;

    private Button btn_printer_test,btn_leftMargin_test,btn_printArea_test,btn_rightSpce_test;
    private Button btn_alignMode_test,btn_relativePosition_test,btn_absolute_test,btn_tab_test,btn_underline_test;
    private Button btn_bmp_test,btn_barcode_test,btn_QRcode_test,btn_raster_test;
    private Button btn_koubei_test, btn_baidu_test,btn_meituan_test,btn_elemo_test;
    private Button btn_multiThread_test,btn_exit;
    private Button btn_load_bluetoothPrinter,btn_printer_init;
    /*定义打印机状态*/
    private final int PRINTER_NORMAL = 0;
    private final int PRINTER_PAPERLESS = 1;
    private final int PRINTER_THP_HIGH_TEMPERATURE = 2;
    private final int PRINTER_MOTOR_HIGH_TEMPERATURE = 3;
    private final int PRINTER_IS_BUSY = 4;
    private final int PRINTER_ERROR_UNKNOWN = 5;
    /*打印机当前状态*/
    private int printerStatus = PRINTER_ERROR_UNKNOWN;

    /*定义状态广播*/
    private final String  PRINTER_NORMAL_ACTION = "com.iposprinter.iposprinterservice.NORMAL_ACTION";
    private final String  PRINTER_PAPERLESS_ACTION = "com.iposprinter.iposprinterservice.PAPERLESS_ACTION";
    private final String  PRINTER_PAPEREXISTS_ACTION = "com.iposprinter.iposprinterservice.PAPEREXISTS_ACTION";
    private final String  PRINTER_THP_HIGHTEMP_ACTION = "com.iposprinter.iposprinterservice.THP_HIGHTEMP_ACTION";
    private final String  PRINTER_THP_NORMALTEMP_ACTION = "com.iposprinter.iposprinterservice.THP_NORMALTEMP_ACTION";
    private final String  PRINTER_MOTOR_HIGHTEMP_ACTION = "com.iposprinter.iposprinterservice.MOTOR_HIGHTEMP_ACTION";
    private final String  PRINTER_BUSY_ACTION = "com.iposprinter.iposprinterservice.BUSY_ACTION";
    private final String  PRINTER_CURRENT_TASK_PRINT_COMPLETE_ACTION = "com.iposprinter.iposprinterservice.CURRENT_TASK_PRINT_COMPLETE_ACTION";

    /*定义消息*/
    private final int MSG_TEST                               = 1;
    private final int MSG_IS_NORMAL                          = 2;
    private final int MSG_IS_BUSY                            = 3;
    private final int MSG_PAPER_LESS                         = 4;
    private final int MSG_PAPER_EXISTS                       = 5;
    private final int MSG_THP_HIGH_TEMP                      = 6;
    private final int MSG_THP_TEMP_NORMAL                    = 7;
    private final int MSG_MOTOR_HIGH_TEMP                    = 8;
    private final int MSG_MOTOR_HIGH_TEMP_INIT_PRINTER       = 9;
    private final int MSG_CURRENT_TASK_PRINT_COMPLETE     = 10;

    /*循环打印类型*/
    private final int  MULTI_THREAD_LOOP_PRINT  = 1;
    private final int  DEFAULT_LOOP_PRINT       = 0;

    //循环打印标志位
    private       int  loopPrintFlag            = DEFAULT_LOOP_PRINT;

    private boolean isBluetoothOpen = false;
    private TextView info;
    private Random random = new Random();
    private HandlerUtils.PrinterHandler mPrinterHandler;

    /**
     * 消息处理
     */
    private HandlerUtils.IHandlerIntent mHandlerIntent = new HandlerUtils.IHandlerIntent()
    {
        @Override
        public void handlerIntent(Message msg)
        {
            switch (msg.what)
            {
                case MSG_TEST:
                    break;
                case MSG_IS_NORMAL:
                    if (getPrinterStatus() == PRINTER_NORMAL) {
                        print_loop(loopPrintFlag);
                    }
                    break;
                case MSG_IS_BUSY:
                    Toast.makeText(BlutetoothPrinterDemo.this, R.string.printer_is_working, Toast.LENGTH_SHORT).show();
                    break;
                case MSG_PAPER_LESS:
                    loopPrintFlag = DEFAULT_LOOP_PRINT;
                    Toast.makeText(BlutetoothPrinterDemo.this, R.string.out_of_paper, Toast.LENGTH_SHORT).show();
                    break;
                case MSG_PAPER_EXISTS:
                    Toast.makeText(BlutetoothPrinterDemo.this, R.string.exists_paper, Toast.LENGTH_SHORT).show();
                    break;
                case MSG_THP_HIGH_TEMP:
                    Toast.makeText(BlutetoothPrinterDemo.this, R.string.printer_high_temp_alarm, Toast.LENGTH_SHORT).show();
                    break;
                case MSG_MOTOR_HIGH_TEMP:
                    loopPrintFlag = DEFAULT_LOOP_PRINT;
                    Toast.makeText(BlutetoothPrinterDemo.this, R.string.motor_high_temp_alarm, Toast.LENGTH_SHORT).show();
                    mPrinterHandler.sendEmptyMessageDelayed(MSG_MOTOR_HIGH_TEMP_INIT_PRINTER, 180000);  //马达高温报警，等待3分钟后复位打印机
                    break;
                case MSG_MOTOR_HIGH_TEMP_INIT_PRINTER:
                    loopPrintFlag = DEFAULT_LOOP_PRINT;
                    printerInit();
                    break;
                case MSG_CURRENT_TASK_PRINT_COMPLETE:
                    Toast.makeText(BlutetoothPrinterDemo.this, R.string.printer_current_task_print_complete, Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
        }
    };

    private BroadcastReceiver IPosPrinterStatusListener = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent){
            String action = intent.getAction();
            if(action == null)
            {
                Log.d(TAG,"IPosPrinterStatusListener onReceive action = null");
                return;
            }
           // Log.d(TAG,"IPosPrinterStatusListener action = "+action);
            if(action.equals(PRINTER_NORMAL_ACTION))
            {
                mPrinterHandler.sendEmptyMessageDelayed(MSG_IS_NORMAL,0);
            }
            else if (action.equals(PRINTER_PAPERLESS_ACTION))
            {
                mPrinterHandler.sendEmptyMessageDelayed(MSG_PAPER_LESS,0);
            }
            else if (action.equals(PRINTER_BUSY_ACTION))
            {
                mPrinterHandler.sendEmptyMessageDelayed(MSG_IS_BUSY,0);
            }
            else if (action.equals(PRINTER_PAPEREXISTS_ACTION))
            {
                mPrinterHandler.sendEmptyMessageDelayed(MSG_PAPER_EXISTS,0);
            }
            else if (action.equals(PRINTER_THP_HIGHTEMP_ACTION))
            {
                mPrinterHandler.sendEmptyMessageDelayed(MSG_THP_HIGH_TEMP,0);
            }
            else if (action.equals(PRINTER_THP_NORMALTEMP_ACTION))
            {
                mPrinterHandler.sendEmptyMessageDelayed(MSG_THP_TEMP_NORMAL,0);
            }
            else if (action.equals(PRINTER_MOTOR_HIGHTEMP_ACTION))  //此时当前任务会继续打印，完成当前任务后，请等待2分钟以上时间，继续下一个打印任务
            {
                mPrinterHandler.sendEmptyMessageDelayed(MSG_MOTOR_HIGH_TEMP,0);
            }
            else if(action.equals(PRINTER_CURRENT_TASK_PRINT_COMPLETE_ACTION))
            {
                mPrinterHandler.sendEmptyMessageDelayed(MSG_CURRENT_TASK_PRINT_COMPLETE,0);
            }
            else if(action.equals(BluetoothAdapter.ACTION_STATE_CHANGED))
            {
                int state= intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        Log.d("aaa", "STATE_OFF 蓝牙关闭");
                        isBluetoothOpen = false;
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.d("aaa", "STATE_TURNING_OFF 蓝牙正在关闭");
                        isBluetoothOpen = false;
                        if(mBluetoothAdapter != null)
                            mBluetoothAdapter = null;
                        if(mBluetoothPrinterDevice != null)
                            mBluetoothPrinterDevice = null;
                        try {
                            if (socket != null && (socket.isConnected())) {
                                socket.close();
                                socket = null;
                            }
                        }
                        catch (IOException e)
                        {
                            e.printStackTrace();
                        }
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.d("aaa", "STATE_ON 蓝牙开启");
                        loopPrintFlag = DEFAULT_LOOP_PRINT;
                        isBluetoothOpen = true;
                        LoadBluetoothPrinter();
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        isBluetoothOpen = true;
                        Log.d("aaa", "STATE_TURNING_ON 蓝牙正在开启");
                        break;
                }
            }
            else
            {
                mPrinterHandler.sendEmptyMessageDelayed(MSG_TEST,0);
            }
        }
    };

    private void setButtonEnable(boolean flag){
        btn_load_bluetoothPrinter.setEnabled(flag);
        btn_printer_init.setEnabled(flag);
        btn_printer_test.setEnabled(flag);
        btn_leftMargin_test.setEnabled(flag);
        btn_printArea_test.setEnabled(flag);
        btn_rightSpce_test.setEnabled(flag);
        btn_alignMode_test.setEnabled(flag);
        btn_relativePosition_test.setEnabled(flag);
        btn_absolute_test.setEnabled(flag);
        btn_tab_test.setEnabled(flag);
        btn_underline_test.setEnabled(flag);
        btn_bmp_test.setEnabled(flag);
        btn_barcode_test.setEnabled(flag);
        btn_QRcode_test.setEnabled(flag);
        btn_raster_test.setEnabled(flag);
        btn_koubei_test.setEnabled(flag);
        btn_baidu_test.setEnabled(flag);
        btn_meituan_test.setEnabled(flag);
        btn_elemo_test.setEnabled(flag);
        btn_multiThread_test.setEnabled(flag);
        btn_exit.setEnabled(flag);
    }

    private void innitView()
    {
        btn_load_bluetoothPrinter = (Button) findViewById(R.id.btn_load_bluetoothPrinter);
        btn_printer_init = (Button) findViewById(R.id.btn_printer_init);
        btn_printer_test = (Button) findViewById(R.id.btn_printer_test);
        btn_leftMargin_test = (Button) findViewById(R.id.btn_leftMargin_test);
        btn_printArea_test = (Button) findViewById(R.id.btn_printArea_test);
        btn_rightSpce_test = (Button) findViewById(R.id.btn_rightSpce_test);
        btn_alignMode_test = (Button) findViewById(R.id.btn_alignMode_test);
        btn_relativePosition_test = (Button) findViewById(R.id.btn_relativePosition_test);
        btn_absolute_test = (Button) findViewById(R.id.btn_absolute_test);
        btn_tab_test = (Button)findViewById(R.id.btn_tab_test);
        btn_underline_test = (Button)findViewById(R.id.btn_underline_test);
        btn_bmp_test = (Button) findViewById(R.id.btn_bmp_test);
        btn_barcode_test = (Button) findViewById(R.id.btn_barcode_test);
        btn_QRcode_test = (Button) findViewById(R.id.btn_QRcode_test);
        btn_raster_test = (Button) findViewById(R.id.btn_raster_test);
        btn_koubei_test = (Button) findViewById(R.id.btn_koubei_test);
        btn_baidu_test = (Button) findViewById(R.id.btn_baidu_test);
        btn_meituan_test = (Button) findViewById(R.id.btn_meituan_test);
        btn_elemo_test = (Button) findViewById(R.id.btn_elemo_test);
        btn_multiThread_test = (Button) findViewById(R.id.btn_multiThread_test);
        btn_exit = (Button) findViewById(R.id.btn_exit);

        btn_load_bluetoothPrinter.setOnClickListener(this);
        btn_printer_init.setOnClickListener(this);
        btn_printer_test.setOnClickListener(this);
        btn_leftMargin_test.setOnClickListener(this);
        btn_printArea_test.setOnClickListener(this);
        btn_rightSpce_test.setOnClickListener(this);
        btn_alignMode_test.setOnClickListener(this);
        btn_relativePosition_test.setOnClickListener(this);
        btn_absolute_test.setOnClickListener(this);
        btn_tab_test.setOnClickListener(this);
        btn_underline_test.setOnClickListener(this);
        btn_bmp_test.setOnClickListener(this);
        btn_barcode_test.setOnClickListener(this);
        btn_QRcode_test.setOnClickListener(this);
        btn_raster_test.setOnClickListener(this);
        btn_koubei_test.setOnClickListener(this);
        btn_baidu_test.setOnClickListener(this);
        btn_meituan_test.setOnClickListener(this);
        btn_elemo_test.setOnClickListener(this);
        btn_multiThread_test.setOnClickListener(this);
        btn_exit.setOnClickListener(this);
        setButtonEnable(true);
        info = (TextView) findViewById(R.id.info);
        info.setText(VERSION);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blutetooth_printer_demo);
        //设置屏幕一直亮着，不进入休眠状态
       // getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mPrinterHandler = new HandlerUtils.PrinterHandler(mHandlerIntent);
        innitView();
    }

    @Override
    protected void onResume()
    {
       // Log.d(TAG, "activity onResume");
        super.onResume();
        //注册打印机状态接收器
        IntentFilter printerStatusFilter = new IntentFilter();
        printerStatusFilter.addAction(PRINTER_NORMAL_ACTION);
        printerStatusFilter.addAction(PRINTER_PAPERLESS_ACTION);
        printerStatusFilter.addAction(PRINTER_PAPEREXISTS_ACTION);
        printerStatusFilter.addAction(PRINTER_THP_HIGHTEMP_ACTION);
        printerStatusFilter.addAction(PRINTER_THP_NORMALTEMP_ACTION);
        printerStatusFilter.addAction(PRINTER_MOTOR_HIGHTEMP_ACTION);
        printerStatusFilter.addAction(PRINTER_BUSY_ACTION);
        printerStatusFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(IPosPrinterStatusListener,printerStatusFilter);

        loopPrintFlag = DEFAULT_LOOP_PRINT;
        LoadBluetoothPrinter();
        if(getPrinterStatus() == PRINTER_NORMAL)
            printerInit();
    }

    @Override
    protected void onPause()
    {
       // Log.d(TAG, "activity onPause");
        super.onPause();
    }

    @Override
    protected void onStop()
    {
       // Log.e(TAG, "activity onStop");
        super.onStop();
        unregisterReceiver(IPosPrinterStatusListener);
        loopPrintFlag = DEFAULT_LOOP_PRINT;
    }

    @Override
    protected void onDestroy()
    {
       // Log.d(TAG, "activity onDestroy");
        super.onDestroy();
        mPrinterHandler.removeCallbacksAndMessages(null);
        if(mBluetoothAdapter != null)
            mBluetoothAdapter = null;
        if(mBluetoothPrinterDevice != null)
            mBluetoothPrinterDevice = null;
        try {
            if (socket != null && (socket.isConnected())) {
                socket.close();
                socket = null;
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View v){
        if(ButtonDelayUtils.isFastDoubleClick())
        {
            return;
        }

        switch (v.getId())
        {
            //加载蓝牙打印机
            case R.id.btn_load_bluetoothPrinter:
                loopPrintFlag = DEFAULT_LOOP_PRINT;
                LoadBluetoothPrinter();
                break;
            //打印机初始化
            case R.id.btn_printer_init:
                loopPrintFlag = DEFAULT_LOOP_PRINT;
                if(getPrinterStatus() == PRINTER_NORMAL)
                    printerInit();
                break;
            //蓝牙打印测试
            case R.id.btn_printer_test:
                if(getPrinterStatus() == PRINTER_NORMAL)
                    bluetoothPrinterTest();
                break;
            //左边距测试
            case R.id.btn_leftMargin_test:
                if(getPrinterStatus() == PRINTER_NORMAL)
                    printLeftMarginTest();
                break;
            //打印区域宽度测试
            case R.id.btn_printArea_test:
                if(getPrinterStatus() == PRINTER_NORMAL)
                    printAreaTest();
                break;
            //打印字符右间距测试
            case R.id.btn_rightSpce_test:
                if(getPrinterStatus() == PRINTER_NORMAL)
                    printCharRightSpaceTest();
                break;
            //打印对齐方式测试
            case R.id.btn_alignMode_test:
                if(getPrinterStatus() == PRINTER_NORMAL)
                    printAlignModeTest();
                break;
            //打印相对位置测试
            case R.id.btn_relativePosition_test:
                if(getPrinterStatus() == PRINTER_NORMAL)
                    printRelativePositionTest();
                break;
            //绝对打印位置测试
            case R.id.btn_absolute_test:
                if(getPrinterStatus() == PRINTER_NORMAL)
                    printAbsolutePositionTest();
                break;
            //跳格测试
            case R.id.btn_tab_test:
                if(getPrinterStatus() == PRINTER_NORMAL)
                    printTabTest();
                break;
            //下划线测试
            case R.id.btn_underline_test:
                if(getPrinterStatus() == PRINTER_NORMAL)
                    printUnderlineTest();
                break;
            //位图测试
            case R.id.btn_bmp_test:
                if(getPrinterStatus() == PRINTER_NORMAL)
                    printBitmapTest();
                break;
            //条码测试
            case R.id.btn_barcode_test:
                if(getPrinterStatus() == PRINTER_NORMAL)
                    printBarcodeTest();
                break;
            //二维码测试
            case R.id.btn_QRcode_test:
                if(getPrinterStatus() == PRINTER_NORMAL)
                    printQRcodeTest();
                break;
            //光栅位图测试
            case R.id.btn_raster_test:
                if(getPrinterStatus() == PRINTER_NORMAL)
                    printRasterBmpTest();
                break;
            //打印口碑小票
            case R.id.btn_koubei_test:
                if(getPrinterStatus() == PRINTER_NORMAL)
                    printKoubeiBill();
                break;
            //打印百度小票
            case R.id.btn_baidu_test:
                if(getPrinterStatus() == PRINTER_NORMAL)
                    printBaiduBill();
                break;
            //打印美团小票
            case R.id.btn_meituan_test:
                if(getPrinterStatus() == PRINTER_NORMAL)
                    printMeituanBill();
                break;
            //打印饿了么小票
            case R.id.btn_elemo_test:
                if(getPrinterStatus() == PRINTER_NORMAL)
                    printElemoBill();
                break;
            //多线程测试
            case R.id.btn_multiThread_test:
                if(getPrinterStatus() == PRINTER_NORMAL) {
                    multiThreadPrintTest();
                    loopPrintFlag = MULTI_THREAD_LOOP_PRINT;
                }
                break;
            //退出应用
            case R.id.btn_exit:
                loopPrintFlag = DEFAULT_LOOP_PRINT;
                finish();
                break;
            default:
                break;

        }
    }

    /**
     * 循环打印
     */
    public void print_loop(int flag)
    {
        switch (flag)
        {
            case MULTI_THREAD_LOOP_PRINT:
                multiThreadPrintTest();
                break;
            default:
                break;
        }
    }

    public void multiThreadPrintTest()
    {
        switch (random.nextInt(16))
        {
            case 0:
                bluetoothPrinterTest();
                break;
            case 1:
                printLeftMarginTest();
                break;
            case 2:
                printAreaTest();
                break;
            case 3:
                printCharRightSpaceTest();
                break;
            case 4:
                printAlignModeTest();
                break;
            case 5:
                printRelativePositionTest();
                break;
            case 6:
                printAbsolutePositionTest();
                break;
            case 7:
                printTabTest();
                break;
            case 8:
                printUnderlineTest();
                break;
            case 9:
                printBitmapTest();
                break;
            case 10:
                printBarcodeTest();
                break;
            case 11:
                printQRcodeTest();
                break;
            case 12:
                printRasterBmpTest();
                break;
            case 13:
                printKoubeiBill();
                break;
            case 14:
                printBaiduBill();
                break;
            case 15:
                printMeituanBill();
                break;
            case 16:
                printElemoBill();
                break;
            default:
                break;
        }
    }

    public void LoadBluetoothPrinter()
    {
        // 1: Get BluetoothAdapter
        mBluetoothAdapter = BluetoothUtil.getBluetoothAdapter();
        if(mBluetoothAdapter == null)
        {
            Toast.makeText(getBaseContext(), R.string.get_BluetoothAdapter_fail, Toast.LENGTH_LONG).show();
            isBluetoothOpen = false;
            return;
        }
        else
        {
            isBluetoothOpen =true;
        }
        //2: Get bluetoothPrinter Devices
        mBluetoothPrinterDevice = BluetoothUtil.getIposPrinterDevice(mBluetoothAdapter);
        if(mBluetoothPrinterDevice == null)
        {
            Toast.makeText(getBaseContext(), R.string.get_BluetoothPrinterDevice_fail, Toast.LENGTH_LONG).show();
            return;
        }
        //3: Get conect Socket
        try {
            socket = BluetoothUtil.getSocket(mBluetoothPrinterDevice);
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return;
        }
        Toast.makeText(getBaseContext(), R.string.get_BluetoothPrinterDevice_success, Toast.LENGTH_LONG).show();
    }

    public int getPrinterStatus()
    {
        byte[] statusData = new byte[3];
        if(!isBluetoothOpen)
        {
            printerStatus = PRINTER_ERROR_UNKNOWN;
            return printerStatus;
        }
        if((socket == null) || (!socket.isConnected()))
        {
            try {
                socket = BluetoothUtil.getSocket(mBluetoothPrinterDevice);
            }
            catch (IOException e)
            {
                e.printStackTrace();
                return printerStatus;
            }
        }
        try {
            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();
            byte[] data = ESCUtil.getPrinterStatus();
            out.write(data,0,data.length);
            int readsize = in.read(statusData);
           Log.d(TAG,"~~~ readsize:"+readsize+" statusData:"+statusData[0]+" "+statusData[1]+" "+statusData[2]);
            if((readsize > 0) &&(statusData[0] == ESCUtil.ACK && statusData[1] == 0x11)) {
                printerStatus = statusData[2];
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return printerStatus;
    }

    private void printerInit()
    {
        ThreadPoolManager.getInstance().executeTask(new Runnable() {
            @Override
            public void run() {
                try {
                    if((socket == null) || (!socket.isConnected()))
                    {
                        socket = BluetoothUtil.getSocket(mBluetoothPrinterDevice);
                    }
                    //Log.d(TAG,"=====printerInit======");
                    OutputStream out = socket.getOutputStream();
                    byte[] data = ESCUtil.init_printer();
                    out.write(data,0,data.length);
                    out.close();
                    socket.close();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        });
    }


    private void bluetoothPrinterTest()
    {
        ThreadPoolManager.getInstance().executeTask(new Runnable() {
            @Override
            public void run() {
               try{
                    byte[] printer_init = ESCUtil.init_printer();
                    byte[] fontSize0 = ESCUtil.fontSizeSet((byte) 0x00);
                    byte[] fontSize1 = ESCUtil.fontSizeSet((byte) 0x01);
                    byte[] fontSize2 = ESCUtil.fontSizeSet((byte) 0x10);
                    byte[] fontSize3 = ESCUtil.fontSizeSet((byte) 0x11);
                    byte[] lineH0 = ESCUtil.setLineHeight((byte)26);
                    byte[] lineH1 = ESCUtil.setLineHeight((byte)50);

                    byte[] align0 = ESCUtil.alignMode((byte)0);
                    byte[] align1 = ESCUtil.alignMode((byte)1);
                    byte[] align2 = ESCUtil.alignMode((byte)2);
                    byte[] title1 = "蓝牙打印机测试\n".getBytes("GBK");
                    byte[] title2 = "Bluetooth Printer test\n".getBytes("GBK"); //
                    byte[] sign1 = "************************\n".getBytes("GBK");
                    byte[] fontTest0 = "这是一行默认大小字体\n".getBytes("GBK");
                    byte[] fontTest1 = "这是一行倍高字体\n".getBytes("GBK");
                    byte[] fontTest2 = "这是一行倍宽字体\n".getBytes("GBK");
                    byte[] fontTest3 = "这是一行倍宽倍高字体\n".getBytes("GBK");
                    byte[] orderSerinum = "1234567890\n".getBytes("GBK");
                    byte[] specialSign= "!@#$%^&*(κρχκμνκλρκνκνμρτυφ)\n".getBytes("GBK");
                    byte[] testSign = "------------------------\n".getBytes("GBK");
                    byte[] testInfo = "欢迎使用蓝牙打印机\n".getBytes("GBK");
                    byte[] nextLine = ESCUtil.nextLines(1);
                    byte[] performPrint = ESCUtil.performPrintAndFeedPaper((byte)200);

                    byte[][] cmdBytes = {printer_init,lineH0,fontSize3,align1,title1,fontSize1,title2,nextLine,align0,
                            fontSize0,sign1,fontSize0,fontTest0,lineH1,fontSize1,fontTest1,lineH0,fontSize2,fontTest2,
                            lineH1,fontSize3,fontTest3,align2,lineH0,fontSize0,orderSerinum,specialSign,testSign,
                            align1,fontSize1,lineH1,testInfo,nextLine,performPrint};
                    try {
                        if((socket == null) || (!socket.isConnected()))
                        {
                            socket = BluetoothUtil.getSocket(mBluetoothPrinterDevice);
                        }
                        byte[] data = ESCUtil.byteMerger(cmdBytes);
                        OutputStream out = socket.getOutputStream();
                        out.write(data,0,data.length);
                        out.close();
                        socket.close();
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }
                catch (UnsupportedEncodingException e)
                {
                    e.printStackTrace();
                }

            }
        });
    }

    private void printLeftMarginTest()
    {
        ThreadPoolManager.getInstance().executeTask(new Runnable() {
            @Override
            public void run() {
                try{
                    byte[] printer_init = ESCUtil.init_printer();
                    byte[] selectChinese = ESCUtil.selectChineseMode();
                    byte[] charCode = ESCUtil.selectCharCodeSystem((byte) 0x01);
                    byte[] fontSize0 = ESCUtil.fontSizeSet((byte) 0x00);
                    byte[] fontSize1 = ESCUtil.fontSizeSet((byte) 0x10);
                    byte[] fontSize2 = ESCUtil.fontSizeSet((byte) 0x01);
                    byte[] fontSize3 = ESCUtil.fontSizeSet((byte) 0x11);
                    byte[] lineH0 = ESCUtil.setLineHeight((byte)16);
                    byte[] align0 = ESCUtil.alignMode((byte)0);
                    byte[] leftMargin0 = ESCUtil.printLeftMargin(0);
                    byte[] leftMargin1 = ESCUtil.printLeftMargin(8);
                    byte[] leftMargin2 = ESCUtil.printLeftMargin(16);
                    byte[] leftMargin3 = ESCUtil.printLeftMargin(24);
                    byte[] text0 = "左边距0点测试\n".getBytes("GBK");
                    byte[] text1 = "左边距8点测试\n".getBytes("GBK");
                    byte[] text2 = "左边距16点测试\n".getBytes("GBK");
                    byte[] text3 = "左边距24点测试\n".getBytes("GBK");
                    byte[] nextLine = ESCUtil.nextLines(1);
                    byte[] performPrint = ESCUtil.performPrintAndFeedPaper((byte)160);

                    byte[][] cmdBytes = {printer_init,selectChinese,charCode,fontSize0,lineH0,align0,
                            leftMargin0,text0,
                            leftMargin1,fontSize1,text1,
                            leftMargin2,fontSize2,text2,
                            leftMargin3,fontSize0,text3,
                            nextLine,
                            performPrint};
                    try {
                        if((socket == null) || (!socket.isConnected()))
                        {
                            socket = BluetoothUtil.getSocket(mBluetoothPrinterDevice);
                        }
                        byte[] data = ESCUtil.byteMerger(cmdBytes);
                        OutputStream out = socket.getOutputStream();
                        out.write(data,0,data.length);
                        out.close();
                        socket.close();
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }
                catch (UnsupportedEncodingException e)
                {
                    e.printStackTrace();
                }

            }
        });
    }

    private void printAreaTest()
    {
        ThreadPoolManager.getInstance().executeTask(new Runnable() {
            @Override
            public void run() {
                try{
                    byte[] printer_init = ESCUtil.init_printer();
                    byte[] selectChinese = ESCUtil.selectChineseMode();
                    byte[] charCode = ESCUtil.selectCharCodeSystem((byte) 0x01);
                    byte[] fontSize0 = ESCUtil.fontSizeSet((byte) 0x00);
                    byte[] fontSize1 = ESCUtil.fontSizeSet((byte) 0x10);
                    byte[] fontSize2 = ESCUtil.fontSizeSet((byte) 0x01);
                    byte[] fontSize3 = ESCUtil.fontSizeSet((byte) 0x01);
                    byte[] lineH0 = ESCUtil.setLineHeight((byte)16);
                    byte[] lineH1 = ESCUtil.setLineHeight((byte)26);
                    byte[] lineH2 = ESCUtil.setLineHeight((byte)33);
                    byte[] lineH3 = ESCUtil.setLineHeight((byte)50);
                    byte[] align0 = ESCUtil.alignMode((byte)0);
                    byte[] leftMargin0 = ESCUtil.printLeftMargin(0);
                    byte[] leftMargin1 = ESCUtil.printLeftMargin(24);
                    byte[] printarea0 = ESCUtil.printAreaWidth(320);
                    byte[] printarea1 = ESCUtil.printAreaWidth(304);
                    byte[] printarea2 = ESCUtil.printAreaWidth(256);
                    byte[] text0 = "左边距0点,打印区域320点宽测试\n".getBytes("GBK");
                    byte[] text1 = "左边距24点,打印区域304点宽测试\n".getBytes("GBK");
                    byte[] text2 = "左边距24点,打印区域256点宽测试\n".getBytes("GBK");
                    byte[] nextLine = ESCUtil.nextLines(1);
                    byte[] performPrint = ESCUtil.performPrintAndFeedPaper((byte)160);

                    byte[][] cmdBytes = {printer_init,selectChinese,charCode,fontSize0,lineH0,align0,
                            leftMargin0,printarea0,text0,
                            leftMargin1,fontSize1,printarea1,text1,
                            printarea2,fontSize2,text2,
                            nextLine,
                            performPrint};
                    try {
                        if((socket == null) || (!socket.isConnected()))
                        {
                            socket = BluetoothUtil.getSocket(mBluetoothPrinterDevice);
                        }
                        byte[] data = ESCUtil.byteMerger(cmdBytes);
                        OutputStream out = socket.getOutputStream();
                        out.write(data,0,data.length);
                        out.close();
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }
                catch (UnsupportedEncodingException e)
                {
                    e.printStackTrace();
                }

            }
        });
    }

    private void printCharRightSpaceTest()
    {
        ThreadPoolManager.getInstance().executeTask(new Runnable() {
            @Override
            public void run() {
                try{
                    byte[] printer_init = ESCUtil.init_printer();
                    byte[] selectChinese = ESCUtil.selectChineseMode();
                    byte[] charCode = ESCUtil.selectCharCodeSystem((byte) 0x01);
                    byte[] fontSize0 = ESCUtil.fontSizeSet((byte) 0x00);
                    byte[] fontSize1 = ESCUtil.fontSizeSet((byte) 0x10);
                    byte[] fontSize2 = ESCUtil.fontSizeSet((byte) 0x01);
                    byte[] fontSize3 = ESCUtil.fontSizeSet((byte) 0x01);
                    byte[] lineH0 = ESCUtil.setLineHeight((byte)26);
                    byte[] align0 = ESCUtil.alignMode((byte)0);
                    byte[] rightSpace0 =ESCUtil.setRightSpaceChar((byte)0);
                    byte[] rightSpace1 =ESCUtil.setRightSpaceChar((byte)8);
                    byte[] rightSpace2 =ESCUtil.setRightSpaceChar((byte)24);
                    byte[] text0 = "右间距0点测试\n".getBytes("GBK");
                    byte[] text1 = "右间距8点测试\n".getBytes("GBK");
                    byte[] text2 = "右间距24点测试\n".getBytes("GBK");
                    byte[] nextLine = ESCUtil.nextLines(1);
                    byte[] performPrint = ESCUtil.performPrintAndFeedPaper((byte)160);

                    byte[][] cmdBytes = {printer_init,selectChinese,charCode,fontSize0,lineH0,align0,
                            rightSpace0,text0,
                            rightSpace1,text1,
                            rightSpace2,text2,
                            fontSize1,
                            rightSpace0,text0,
                            rightSpace1,text1,
                            rightSpace2,text2,
                            fontSize2,
                            rightSpace0,text0,
                            rightSpace1,text1,
                            rightSpace2,text2,
                            performPrint};
                    try {
                        if((socket == null) || (!socket.isConnected()))
                        {
                            socket = BluetoothUtil.getSocket(mBluetoothPrinterDevice);
                        }
                        byte[] data = ESCUtil.byteMerger(cmdBytes);
                        OutputStream out = socket.getOutputStream();
                        out.write(data,0,data.length);
                        out.close();
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }
                catch (UnsupportedEncodingException e)
                {
                    e.printStackTrace();
                }

            }
        });
    }

    private void printAlignModeTest()
    {
        ThreadPoolManager.getInstance().executeTask(new Runnable() {
            @Override
            public void run() {
                try{
                    byte[] printer_init = ESCUtil.init_printer();
                    byte[] selectChinese = ESCUtil.selectChineseMode();
                    byte[] charCode = ESCUtil.selectCharCodeSystem((byte) 0x01);
                    byte[] fontSize0 = ESCUtil.fontSizeSet((byte) 0x00);
                    byte[] fontSize1 = ESCUtil.fontSizeSet((byte) 0x01);
                    byte[] fontSize2 = ESCUtil.fontSizeSet((byte) 0x10);
                    byte[] fontSize3 = ESCUtil.fontSizeSet((byte) 0x01);
                    byte[] lineH0 = ESCUtil.setLineHeight((byte)26);
                    byte[] align0 = ESCUtil.alignMode((byte)0);
                    byte[] align1 = ESCUtil.alignMode((byte)1);
                    byte[] align2 = ESCUtil.alignMode((byte)2);
                    byte[] leftMargin0 = ESCUtil.printLeftMargin(0);
                    byte[] text0 = "左对齐测试\n".getBytes("GBK");
                    byte[] text1 = "居中测试\n".getBytes("GBK");
                    byte[] text2 = "右对齐测试\n".getBytes("GBK");
                    byte[] nextLine = ESCUtil.nextLines(1);
                    byte[] performPrint = ESCUtil.performPrintAndFeedPaper((byte)160);
                    
                    byte[][] cmdBytes = {printer_init,selectChinese,charCode,leftMargin0,lineH0,
                            fontSize0,
                            align0,text0,
                            align1,text1,
                            align2,text2,
                            fontSize1,
                            align0,text0,
                            align1,text1,
                            align2,text2,
                            fontSize2,
                            align0,text0,
                            align1,text1,
                            align2,text2,
                            performPrint};
                    try {
                        if((socket == null) || (!socket.isConnected()))
                        {
                            socket = BluetoothUtil.getSocket(mBluetoothPrinterDevice);
                        }
                        byte[] data = ESCUtil.byteMerger(cmdBytes);
                        OutputStream out = socket.getOutputStream();
                        out.write(data,0,data.length);
                        out.close();
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }
                catch (UnsupportedEncodingException e)
                {
                    e.printStackTrace();
                }

            }
        });
    }


    private void printRelativePositionTest()
    {
        ThreadPoolManager.getInstance().executeTask(new Runnable() {
            @Override
            public void run() {
                try{
                    byte[] printer_init = ESCUtil.init_printer();
                    byte[] selectChinese = ESCUtil.selectChineseMode();
                    byte[] charCode = ESCUtil.selectCharCodeSystem((byte) 0x01);
                    byte[] fontSize0 = ESCUtil.fontSizeSet((byte) 0x00);
                    byte[] fontSize1 = ESCUtil.fontSizeSet((byte) 0x01);
                    byte[] fontSize2 = ESCUtil.fontSizeSet((byte) 0x10);
                    byte[] fontSize3 = ESCUtil.fontSizeSet((byte) 0x01);
                    byte[] lineH0 = ESCUtil.setLineHeight((byte)16);
                    byte[] lineH1 = ESCUtil.setLineHeight((byte)26);
                    byte[] lineH2 = ESCUtil.setLineHeight((byte)33);
                    byte[] lineH3 = ESCUtil.setLineHeight((byte)50);
                    byte[] align0 = ESCUtil.alignMode((byte)0);
                    byte[] align1 = ESCUtil.alignMode((byte)1);
                    byte[] align2 = ESCUtil.alignMode((byte)2);
                    byte[] leftMargin0 = ESCUtil.printLeftMargin(0);
                    byte[] leftMargin1 = ESCUtil.printLeftMargin(24);
                    byte[] printarea0 = ESCUtil.printAreaWidth(384);
                    byte[] printarea1 = ESCUtil.printAreaWidth(304);
                    byte[] rightSpace0 =ESCUtil.setRightSpaceChar((byte)0);
                    byte[] rightSpace1 =ESCUtil.setRightSpaceChar((byte)8);
                    byte[] rightSpace3 =ESCUtil.setRightSpaceChar((byte)24);
                    byte[] relative1 = ESCUtil.relativePrintPosition(24);
                    byte[] relative2 = ESCUtil.relativePrintPosition(48);
                    byte[] relative3 = ESCUtil.relativePrintPosition(96);
                    byte[] text0 = "右间距0点".getBytes("GBK");
                    byte[] text1 = "相对位置24点测试".getBytes("GBK");
                    byte[] text2 = "右间距0点".getBytes("GBK");
                    byte[] text3 = "相对位置48点测试".getBytes("GBK");
                    byte[] text4 = "右间距8点".getBytes("GBK");
                    byte[] text5 = "相对位置48测试".getBytes("GBK");
                    byte[] text6 = "右间距24点".getBytes("GBK");
                    byte[] text7 = "相对位置96测试".getBytes("GBK");
                    byte[] nextLine = ESCUtil.nextLines(1);
                    byte[] performPrint = ESCUtil.performPrintAndFeedPaper((byte)160);

                    byte[][] cmdBytes = {printer_init,selectChinese,charCode,fontSize0,lineH0,align0,leftMargin0,printarea0,rightSpace0,text0,
                            relative1,text1,leftMargin0,text2,relative2,text3,leftMargin1,rightSpace1,text4,relative2,text5,leftMargin1,
                            printarea1,rightSpace3,text6,relative3,text7,
                            fontSize1,lineH1,align0,leftMargin0,printarea0,rightSpace0,text0,
                            relative1,text1,leftMargin0,text2,relative2,text3,leftMargin1,rightSpace1,text4,relative2,text5,leftMargin1,
                            fontSize1,lineH1,align0,leftMargin0,printarea0,rightSpace0,text0,
                            relative1,relative1,text3,relative2,leftMargin0,text2,relative2,text3,leftMargin1,rightSpace1,text4,relative2,text5,leftMargin1,
                            printarea1,rightSpace3,text6,relative3,text7,
                            performPrint};
                    try {
                        if((socket == null) || (!socket.isConnected()))
                        {
                            socket = BluetoothUtil.getSocket(mBluetoothPrinterDevice);
                        }
                        byte[] data = ESCUtil.byteMerger(cmdBytes);
                        OutputStream out = socket.getOutputStream();
                        out.write(data,0,data.length);
                        out.close();
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }
                catch (UnsupportedEncodingException e)
                {
                    e.printStackTrace();
                }
            }
        });
    }

    private void printAbsolutePositionTest()
    {
        ThreadPoolManager.getInstance().executeTask(new Runnable() {
            @Override
            public void run() {
                try{
                    byte[] printer_init = ESCUtil.init_printer();
                    byte[] selectChinese = ESCUtil.selectChineseMode();
                    byte[] charCode = ESCUtil.selectCharCodeSystem((byte) 0x01);
                    byte[] fontSize0 = ESCUtil.fontSizeSet((byte) 0x00);
                    byte[] fontSize1 = ESCUtil.fontSizeSet((byte) 0x01);
                    byte[] fontSize2 = ESCUtil.fontSizeSet((byte) 0x10);
                    byte[] fontSize3 = ESCUtil.fontSizeSet((byte) 0x01);
                    byte[] lineH0 = ESCUtil.setLineHeight((byte)16);
                    byte[] lineH1 = ESCUtil.setLineHeight((byte)26);
                    byte[] lineH2 = ESCUtil.setLineHeight((byte)33);
                    byte[] lineH3 = ESCUtil.setLineHeight((byte)50);
                    byte[] align0 = ESCUtil.alignMode((byte)0);
                    byte[] align1 = ESCUtil.alignMode((byte)1);
                    byte[] align2 = ESCUtil.alignMode((byte)2);
                    byte[] leftMargin0 = ESCUtil.printLeftMargin(0);
                    byte[] absolute0 = ESCUtil.absolutePrintPosition(0);
                    byte[] absolute1 = ESCUtil.absolutePrintPosition(96);
                    byte[] text0 = "绝对打印位置0点测试\n".getBytes("GBK");
                    byte[] text1 = "绝对打印位置96点测试\n".getBytes("GBK");
                    byte[] nextLine = ESCUtil.nextLines(1);
                    byte[] performPrint = ESCUtil.performPrintAndFeedPaper((byte)160);

                    byte[][] cmdBytes = {printer_init,selectChinese,charCode,fontSize0,lineH0,align0,leftMargin0,
                            absolute0,text0,absolute1,text1,
                            fontSize1,lineH1,absolute0,text0,absolute1,text1,
                            fontSize2,lineH2,absolute0,text0,absolute1,text1,
                            fontSize3,lineH3,absolute0,text0,absolute1,text1,
                            performPrint};
                    try {
                        if((socket == null) || (!socket.isConnected()))
                        {
                            socket = BluetoothUtil.getSocket(mBluetoothPrinterDevice);
                        }
                        byte[] data = ESCUtil.byteMerger(cmdBytes);
                        OutputStream out = socket.getOutputStream();
                        out.write(data,0,data.length);
                        out.close();
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }
                catch (UnsupportedEncodingException e)
                {
                    e.printStackTrace();
                }

            }
        });
    }

    private void printTabTest()
    {
        ThreadPoolManager.getInstance().executeTask(new Runnable() {
            @Override
            public void run() {
                try{
                    byte[] tabPosition = new byte[]{4,6,10};
                    byte[] printer_init = ESCUtil.init_printer();
                    byte[] selectChinese = ESCUtil.selectChineseMode();
                    byte[] charCode = ESCUtil.selectCharCodeSystem((byte) 0x01);
                    byte[] fontSize0 = ESCUtil.fontSizeSet((byte) 0x00);
                    byte[] fontSize1 = ESCUtil.fontSizeSet((byte) 0x01);
                    byte[] fontSize2 = ESCUtil.fontSizeSet((byte) 0x10);
                    byte[] fontSize3 = ESCUtil.fontSizeSet((byte) 0x11);
                    byte[] lineH0 = ESCUtil.setLineHeight((byte)16);
                    byte[] lineH1 = ESCUtil.setLineHeight((byte)26);
                    byte[] lineH2 = ESCUtil.setLineHeight((byte)33);
                    byte[] lineH3 = ESCUtil.setLineHeight((byte)50);
                    byte[] align0 = ESCUtil.alignMode((byte)0);
                    byte[] align1 = ESCUtil.alignMode((byte)1);
                    byte[] align2 = ESCUtil.alignMode((byte)2);
                    byte[] leftMargin0 = ESCUtil.printLeftMargin(0);
                    byte[] leftMargin1 = ESCUtil.printLeftMargin(16);
                    byte[] absolute0 = ESCUtil.absolutePrintPosition(8);
                    byte[] absolute1 = ESCUtil.absolutePrintPosition(24);
                    byte[] tabSet = ESCUtil.set_HT_position(tabPosition);
                    byte[] Tab = ESCUtil.HTCmd();
                    byte[] text0 = "跳格".getBytes("GBK");
                    byte[] text1 = "4个ascii字符".getBytes("GBK");
                    byte[] text2 = "6个ascii字符".getBytes("GBK");
                    byte[] text3 = "10个ascii字符".getBytes("GBK");
                    byte[] nextLine = ESCUtil.nextLines(1);
                    byte[] performPrint = ESCUtil.performPrintAndFeedPaper((byte)160);

                    byte[][] cmdBytes = {printer_init,selectChinese,charCode,fontSize0,lineH0,leftMargin0,
                            tabSet,text0,Tab,text1,leftMargin0,text0,Tab,text2,leftMargin0,text0,Tab,text3,
                            fontSize0,lineH0,leftMargin0,
                            tabSet,text0,Tab,Tab,text1,leftMargin0,text0,text2,leftMargin0,text0,Tab,text3,
                            fontSize1,lineH1,leftMargin0,
                            tabSet,text0,Tab,text1,leftMargin0,text0,Tab,text2,leftMargin0,text0,Tab,text3,
                            fontSize2,lineH2,leftMargin0,
                            tabSet,text0,Tab,text1,leftMargin0,text0,Tab,text2,leftMargin0,text0,Tab,text3,
                            fontSize3,lineH3,leftMargin0,
                            tabSet,text0,Tab,text1,leftMargin0,text0,Tab,text2,leftMargin0,text0,Tab,text3,

                            performPrint};
                    try {
                        if((socket == null) || (!socket.isConnected()))
                        {
                            socket = BluetoothUtil.getSocket(mBluetoothPrinterDevice);
                        }
                        byte[] data = ESCUtil.byteMerger(cmdBytes);
                        OutputStream out = socket.getOutputStream();
                        out.write(data,0,data.length);
                        out.close();
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }
                catch (UnsupportedEncodingException e)
                {
                    e.printStackTrace();
                }

            }
        });
    }

    private void printUnderlineTest()
    {
        ThreadPoolManager.getInstance().executeTask(new Runnable() {
            @Override
            public void run() {
                try{
                    byte[] tabPosition = new byte[]{8,12,18};
                    byte[] printer_init = ESCUtil.init_printer();
                    byte[] selectChinese = ESCUtil.selectChineseMode();
                    byte[] charCode = ESCUtil.selectCharCodeSystem((byte) 0x01);
                    byte[] fontSize0 = ESCUtil.fontSizeSet((byte) 0x00);
                    byte[] fontSize1 = ESCUtil.fontSizeSet((byte) 0x01);
                    byte[] fontSize2 = ESCUtil.fontSizeSet((byte) 0x10);
                    byte[] fontSize3 = ESCUtil.fontSizeSet((byte) 0x11);
                    byte[] lineH0 = ESCUtil.setLineHeight((byte)16);
                    byte[] lineH1 = ESCUtil.setLineHeight((byte)26);
                    byte[] lineH2 = ESCUtil.setLineHeight((byte)33);
                    byte[] lineH3 = ESCUtil.setLineHeight((byte)50);
                    byte[] align0 = ESCUtil.alignMode((byte)0);
                    byte[] align1 = ESCUtil.alignMode((byte)1);
                    byte[] align2 = ESCUtil.alignMode((byte)2);
                    byte[] leftMargin0 = ESCUtil.printLeftMargin(0);
                    byte[] leftMargin1 = ESCUtil.printLeftMargin(16);
                    byte[] absolute0 = ESCUtil.absolutePrintPosition(8);
                    byte[] absolute1 = ESCUtil.absolutePrintPosition(24);
                    byte[] tabSet = ESCUtil.set_HT_position(tabPosition);
                    byte[] Tab = ESCUtil.HTCmd();
                    byte[] underlineWidth1 = ESCUtil.underlineWithWidthOn((byte)1);
                    byte[] underlineWidth2 = ESCUtil.underlineWithWidthOn((byte)2);
                    byte[] underlineEn = ESCUtil.printUnderlineModeEn(true);
                    byte[] underlineDisable = ESCUtil.printUnderlineModeEn(false);
                    byte[] relative2 = ESCUtil.relativePrintPosition(48);
                    byte[] text0 = "下划线".getBytes("GBK");
                    byte[] text1 = "ABC".getBytes("GBK");
                    byte[] text2 = "123".getBytes("GBK");
                    byte[] text3 = "下划线".getBytes("GBK");
                    byte[] text4 = "测试".getBytes("GBK");
                    byte[] nextLine = ESCUtil.nextLines(1);
                    byte[] performPrint = ESCUtil.performPrintAndFeedPaper((byte)160);

                    byte[][] cmdBytes = {printer_init,selectChinese,charCode,underlineWidth1,fontSize0,lineH0,leftMargin0,
                            tabSet,text0,underlineEn,underlineEn,text1,text2,underlineDisable,text3,underlineEn,text4,underlineDisable,
                            text0,text1,underlineEn,text2,underlineDisable,text3,underlineEn,text4,underlineDisable,
                            fontSize0,lineH0,text0,Tab,relative2,underlineEn,relative2,text1,text2,underlineDisable,text3,underlineEn,relative2,text4,underlineDisable,
                            fontSize1,lineH1,text0,underlineEn,text1,Tab,text2,underlineDisable,text3,underlineEn,relative2,text4,underlineDisable,
                            fontSize1,lineH1,text0,underlineEn,text1,Tab,text2,underlineDisable,text3,underlineEn,relative2,text4,underlineDisable,
                            fontSize2,lineH1,text0,underlineEn,text1,relative2,text2,underlineDisable,text3,underlineEn,text1,text4,underlineDisable,
                            tabSet,fontSize3,lineH1,text0,Tab,underlineEn,Tab,text1,text2,underlineDisable,text4,text3,relative2,underlineEn,text1,relative2,underlineDisable,text4,
                            performPrint};
                    try {
                        if((socket == null) || (!socket.isConnected()))
                        {
                            socket = BluetoothUtil.getSocket(mBluetoothPrinterDevice);
                        }
                        byte[] data = ESCUtil.byteMerger(cmdBytes);
                        OutputStream out = socket.getOutputStream();
                        out.write(data,0,data.length);
                        out.close();
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }
                catch (UnsupportedEncodingException e)
                {
                    e.printStackTrace();
                }

            }
        });
    }

    private void printBarcodeTest()
    {
        ThreadPoolManager.getInstance().executeTask(new Runnable() {
            @Override
            public void run() {
                byte[] printer_init = ESCUtil.init_printer();
                byte[] selectChinese = ESCUtil.selectChineseMode();
                byte[] charCode = ESCUtil.selectCharCodeSystem((byte) 0x01);
                byte[] align0 = ESCUtil.alignMode((byte)0);
                byte[] align1 = ESCUtil.alignMode((byte)1);
                byte[] align2 = ESCUtil.alignMode((byte)2);
                byte[] leftMargin0 = ESCUtil.printLeftMargin(0);
                byte[] leftMargin1 = ESCUtil.printLeftMargin(16);
                byte[] printBarcode = ESCUtil.barcodePrint();
                String barcodeContent0 = "1234567";
                String barcodeContent1 = "01234567890";
                String barcodeContent2 = "012345678901";
                byte[] nexLine32 = ESCUtil.nextLines(1);
                byte[] performPrint = ESCUtil.performPrintAndFeedPaper((byte)160);

                byte[][] cmdBytes = {printer_init,selectChinese,charCode,align1,leftMargin0,
                        ESCUtil.setHRIPosition(3),ESCUtil.setBarcodeHeight(5),ESCUtil.setBarcodeWidth(12),
                        printBarcode,ESCUtil.barcodeData(0,barcodeContent1),nexLine32,
                        printBarcode,ESCUtil.barcodeData(2,barcodeContent2),nexLine32,
                        printBarcode,ESCUtil.barcodeData(3,barcodeContent0),nexLine32,
                        printBarcode,ESCUtil.barcodeData(4,barcodeContent0),nexLine32,
                        printBarcode,ESCUtil.barcodeData(5,barcodeContent1),nexLine32,
                        printBarcode,ESCUtil.barcodeData(6,barcodeContent1),nexLine32,
                        printBarcode,ESCUtil.barcodeData(65,barcodeContent1),nexLine32,
                        printBarcode,ESCUtil.barcodeData(67,barcodeContent2),nexLine32,
                        printBarcode,ESCUtil.barcodeData(68,barcodeContent0),nexLine32,
                        printBarcode,ESCUtil.barcodeData(69,barcodeContent0),nexLine32,
                        printBarcode,ESCUtil.barcodeData(70,barcodeContent0),nexLine32,
                        printBarcode,ESCUtil.barcodeData(71,barcodeContent0),nexLine32,
                        printBarcode,ESCUtil.barcodeData(73,barcodeContent0),nexLine32,
                        performPrint};
                try {
                    if((socket == null) || (!socket.isConnected()))
                    {
                        socket = BluetoothUtil.getSocket(mBluetoothPrinterDevice);
                    }
                    byte[] data = ESCUtil.byteMerger(cmdBytes);
                    OutputStream out = socket.getOutputStream();
                    out.write(data,0,data.length);
                    out.close();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        });
    }

    private void printQRcodeTest()
    {
        ThreadPoolManager.getInstance().executeTask(new Runnable() {
            @Override
            public void run() {
                try {
                    byte[] printer_init = ESCUtil.init_printer();
                    byte[] selectChinese = ESCUtil.selectChineseMode();
                    byte[] charCode = ESCUtil.selectCharCodeSystem((byte) 0x01);
                    byte[] align0 = ESCUtil.alignMode((byte) 0);
                    byte[] align1 = ESCUtil.alignMode((byte) 1);
                    byte[] align2 = ESCUtil.alignMode((byte) 2);
                    byte[] leftMargin0 = ESCUtil.printLeftMargin(0);
                    byte[] QRcodeData = "http://www.baidu.com".getBytes("GBK");
                    byte[] nexLine32 = ESCUtil.nextLines(1);
                    byte[] performPrint = ESCUtil.performPrintAndFeedPaper((byte) 160);

                    byte[][] cmdBytes = {printer_init, selectChinese, charCode,
                            leftMargin0,align0,
                            ESCUtil.setQRsize(8),
                            ESCUtil.setQRCorrectionLevel(48),
                            ESCUtil.cacheQRData(QRcodeData),
                            align1,
                            ESCUtil.setQRsize(8),
                            ESCUtil.setQRCorrectionLevel(49),
                            ESCUtil.cacheQRData(QRcodeData),
                            align2,
                            ESCUtil.setQRsize(8),
                            ESCUtil.setQRCorrectionLevel(50),
                            ESCUtil.cacheQRData(QRcodeData),
                            //ESCUtil.printCacheQRdata(),
                            performPrint
                            };
                    try {
                        if((socket == null) || (!socket.isConnected()))
                        {
                            socket = BluetoothUtil.getSocket(mBluetoothPrinterDevice);
                        }
                        byte[] data = ESCUtil.byteMerger(cmdBytes);
                        OutputStream out = socket.getOutputStream();
                        out.write(data, 0, data.length);
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                catch (UnsupportedEncodingException e)
                {
                    e.printStackTrace();
                }
            }
        });
    }

    private void printBitmapTest()
    {
        ThreadPoolManager.getInstance().executeTask(new Runnable() {
            @Override
            public void run() {
                Bitmap mBitmap1 = BitmapFactory.decodeResource(getResources(), R.mipmap.test_p);
                Bitmap mBitmap2 = BitmapFactory.decodeResource(getResources(), R.mipmap.test);

                byte[] printer_init = ESCUtil.init_printer();
                byte[] selectChinese = ESCUtil.selectChineseMode();
                byte[] charCode = ESCUtil.selectCharCodeSystem((byte) 0x01);
                byte[] align0 = ESCUtil.alignMode((byte) 0);
                byte[] align1 = ESCUtil.alignMode((byte) 1);
                byte[] align2 = ESCUtil.alignMode((byte) 2);
                byte[] leftMargin0 = ESCUtil.printLeftMargin(0);
                byte[] nexLine = ESCUtil.nextLines(1);
                byte[] performPrint = ESCUtil.performPrintAndFeedPaper((byte) 160);

                byte[][] cmdBytes = {printer_init, selectChinese, charCode,leftMargin0,
                        align0, BitMapUtil.getBitmapPrintData(mBitmap2,128,0),
                        align1, BitMapUtil.getBitmapPrintData(mBitmap2,128,1),
                        align2,BitMapUtil.getBitmapPrintData(mBitmap2,128,32),
                };
                byte[][] cmdBytes1 ={leftMargin0,
                        align0, BitMapUtil.getBitmapPrintData(mBitmap1,128,0),
                        align1, BitMapUtil.getBitmapPrintData(mBitmap1,256,0),
                        align2,  BitMapUtil.getBitmapPrintData(mBitmap1,320,1),
                        performPrint
                };
                try {
                    if((socket == null) || (!socket.isConnected()))
                    {
                        socket = BluetoothUtil.getSocket(mBluetoothPrinterDevice);
                    }
                    byte[] data = ESCUtil.byteMerger(cmdBytes);
                    OutputStream out = socket.getOutputStream();
                    out.write(data, 0, data.length);
                    byte[] data1 = ESCUtil.byteMerger(cmdBytes1);
                    out.write(data1, 0, data1.length);
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void printRasterBmpTest()
    {
        ThreadPoolManager.getInstance().executeTask(new Runnable() {
            @Override
            public void run() {
                Bitmap mBitmap1 = BitmapFactory.decodeResource(getResources(), R.mipmap.test);
                Bitmap mBitmap2 = BitmapFactory.decodeResource(getResources(), R.mipmap.test_r);
                byte[] printer_init = ESCUtil.init_printer();
                byte[] selectChinese = ESCUtil.selectChineseMode();
                byte[] charCode = ESCUtil.selectCharCodeSystem((byte) 0x01);
                byte[] align0 = ESCUtil.alignMode((byte) 0);
                byte[] align1 = ESCUtil.alignMode((byte) 1);
                byte[] align2 = ESCUtil.alignMode((byte) 2);
                byte[] leftMargin0 = ESCUtil.printLeftMargin(0);
                byte[] performPrint = ESCUtil.performPrintAndFeedPaper((byte) 160);

                byte[][] cmdBytes = {printer_init, selectChinese, charCode,leftMargin0,
                        align0, BitMapUtil.getRasterBmpData(mBitmap1,128,0),
                        align1, BitMapUtil.getRasterBmpData(mBitmap1,128,1),
                        align2, BitMapUtil.getRasterBmpData(mBitmap1,128,2),
                        align2, BitMapUtil.getRasterBmpData(mBitmap1,128,3),
                        align0, BitMapUtil.getRasterBmpData(mBitmap2,128,0),
                        align1, BitMapUtil.getRasterBmpData(mBitmap2,128,0),
                        performPrint
                };
                try {
                    if((socket == null) || (!socket.isConnected()))
                    {
                        socket = BluetoothUtil.getSocket(mBluetoothPrinterDevice);
                    }
                    byte[] data = ESCUtil.byteMerger(cmdBytes);
                    OutputStream out = socket.getOutputStream();
                    out.write(data, 0, data.length);
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void printKoubeiBill()
    {
        ThreadPoolManager.getInstance().executeTask(new Runnable() {
            @Override
            public void run() {
                try{
                    byte[] printer_init = ESCUtil.init_printer();
                    byte[] selectChinese = ESCUtil.selectChineseMode();
                    byte[] charCode = ESCUtil.selectCharCodeSystem((byte) 0x01);
                    byte[] fontSize0 = ESCUtil.fontSizeSet((byte) 0x00);
                    byte[] fontSize1 = ESCUtil.fontSizeSet((byte) 0x01);
                    byte[] fontSize2 = ESCUtil.fontSizeSet((byte) 0x10);
                    byte[] fontSize3 = ESCUtil.fontSizeSet((byte) 0x11);
                    byte[] lineH0 = ESCUtil.setLineHeight((byte)16);
                    byte[] lineH1 = ESCUtil.setLineHeight((byte)26);
                    byte[] lineH2 = ESCUtil.setLineHeight((byte)33);
                    byte[] lineH3 = ESCUtil.setLineHeight((byte)50);
                    byte[] align0 = ESCUtil.alignMode((byte)0);
                    byte[] align1 = ESCUtil.alignMode((byte)1);
                    byte[] align2 = ESCUtil.alignMode((byte)2);
                    byte[] text0 = "   #4口碑外卖\n".getBytes("GBK");
                    byte[] text1 = "         冯记黄焖鸡米饭\n".getBytes("GBK");
                    byte[] text2 = "17:20 尽快送达\n".getBytes("GBK");
                    byte[] text3 = "--------------------------------\n".getBytes("GBK");
                    byte[] text4 = "18610858337韦小宝创智天地广场7号楼(605室)".getBytes("GBK");
                    byte[] text5 = "下单: 16:35\n".getBytes("GBK");
                    byte[] text6 = "********************************\n".getBytes("GBK");
                    byte[] text7 = "菜品          数量   单价   金额\n".getBytes("GBK");
                    byte[] text8 = "黄焖五花肉 (大) (不辣)\n".getBytes("GBK");
                    byte[] text9 = "               1      25      25\n".getBytes("GBK");
                    byte[] text10 = "黄焖五花肉 (小) (不辣)\n".getBytes("GBK");
                    byte[] text11 = "黄焖五花肉 (小) (微辣)\n".getBytes("GBK");
                    byte[] text12 = "配送费                         2\n".getBytes("GBK");
                    byte[] text13 = "            实付金额: 27\n\n".getBytes("GBK");
                    byte[] text14 = "    口碑外卖\n\n\n".getBytes("GBK");
                    byte[] nextLine = ESCUtil.nextLines(1);
                    byte[] performPrint = ESCUtil.performPrintAndFeedPaper((byte)160);

                    byte[][] cmdBytes = {printer_init,selectChinese,charCode,align0,fontSize3,lineH3,text0,
                            fontSize1,lineH1,text1,text6,
                            fontSize3,lineH3,text2,
                            fontSize1,lineH1,text3,
                            fontSize3,lineH3,text4,
                            fontSize1,lineH1,text3,
                            fontSize3,lineH3,text5,
                            fontSize1,lineH1,text6,text7,text3,text8,text9,text10,text9,text11,text9,text3,text12,text3,
                            fontSize2,lineH2,text13,
                            fontSize3,lineH3,text14,
                            performPrint};
                    try {
                        if((socket == null) || (!socket.isConnected()))
                        {
                            socket = BluetoothUtil.getSocket(mBluetoothPrinterDevice);
                        }
                        byte[] data = ESCUtil.byteMerger(cmdBytes);
                        OutputStream out = socket.getOutputStream();
                        out.write(data,0,data.length);
                        out.close();
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }
                catch (UnsupportedEncodingException e)
                {
                    e.printStackTrace();
                }

            }
        });
    }

    private void printBaiduBill()
    {
        ThreadPoolManager.getInstance().executeTask(new Runnable() {
            @Override
            public void run() {
                try{
                    byte[] printer_init = ESCUtil.init_printer();
                    byte[] selectChinese = ESCUtil.selectChineseMode();
                    byte[] charCode = ESCUtil.selectCharCodeSystem((byte) 0x01);
                    byte[] fontSize0 = ESCUtil.fontSizeSet((byte) 0x00);
                    byte[] fontSize1 = ESCUtil.fontSizeSet((byte) 0x01);
                    byte[] fontSize2 = ESCUtil.fontSizeSet((byte) 0x10);
                    byte[] fontSize3 = ESCUtil.fontSizeSet((byte) 0x11);
                    byte[] lineH0 = ESCUtil.setLineHeight((byte)16);
                    byte[] lineH1 = ESCUtil.setLineHeight((byte)26);
                    byte[] lineH2 = ESCUtil.setLineHeight((byte)33);
                    byte[] lineH3 = ESCUtil.setLineHeight((byte)50);
                    byte[] align0 = ESCUtil.alignMode((byte)0);
                    byte[] align1 = ESCUtil.alignMode((byte)1);
                    String Baidu = "本店留存\n************************\n      百度外卖\n      [货到付款]\n" +
                            "************************\n期望送达时间：立即配送\n" +
                            "订单备注:送到西门,不要辣\n发票信息:百度外卖\n************************\n下单编号: 14187186911689\n下单时间: " +
                            "2014-12-16 16:31************************\n" +
                            "菜品名称     数量  金额\n------------------------\n" +
                            "香辣面套餐     1   40.00\n素食天线汉堡   1   38.00\n香辣面套餐     1   40.00\n" +
                            "素食天线汉堡   1   38.00\n香辣面         1   43.00\n" +
                            "素食天线       1   34.00\n" +
                            "------------------------\n" +
                            "************************\n姓名:百度测试\n" +
                            "地址:泰然工贸园\n电话:18665248965\n" +
                            "************************\n百度测试商户\n" +
                            "18665248965\n#15 百度外卖 11月09号 \n\n\n";
                    byte[] BaiduData = Baidu.getBytes("GBK");
                    byte[] nextLine = ESCUtil.nextLines(1);
                    byte[] performPrint = ESCUtil.performPrintAndFeedPaper((byte)160);

                    byte[][] cmdBytes = {printer_init,selectChinese,charCode,align0,fontSize1,lineH2,BaiduData,
                            performPrint};
                    try {
                        if((socket == null) || (!socket.isConnected()))
                        {
                            socket = BluetoothUtil.getSocket(mBluetoothPrinterDevice);
                        }
                        byte[] data = ESCUtil.byteMerger(cmdBytes);
                        OutputStream out = socket.getOutputStream();
                        out.write(data,0,data.length);
                        out.close();
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }
                catch (UnsupportedEncodingException e)
                {
                    e.printStackTrace();
                }

            }
        });
    }

    private void printMeituanBill()
    {
        ThreadPoolManager.getInstance().executeTask(new Runnable() {
            @Override
            public void run() {
                try{
                    byte[] printer_init = ESCUtil.init_printer();
                    byte[] selectChinese = ESCUtil.selectChineseMode();
                    byte[] charCode = ESCUtil.selectCharCodeSystem((byte) 0x01);
                    byte[] fontSize0 = ESCUtil.fontSizeSet((byte) 0x00);
                    byte[] fontSize1 = ESCUtil.fontSizeSet((byte) 0x01);
                    byte[] fontSize2 = ESCUtil.fontSizeSet((byte) 0x10);
                    byte[] fontSize3 = ESCUtil.fontSizeSet((byte) 0x11);
                    byte[] lineH0 = ESCUtil.setLineHeight((byte)16);
                    byte[] lineH1 = ESCUtil.setLineHeight((byte)26);
                    byte[] lineH2 = ESCUtil.setLineHeight((byte)33);
                    byte[] lineH3 = ESCUtil.setLineHeight((byte)50);
                    byte[] align0 = ESCUtil.alignMode((byte)0);
                    byte[] align1 = ESCUtil.alignMode((byte)1);
                    byte[] align2 = ESCUtil.alignMode((byte)2);
                    byte[] text0 = "  #1  美团测试\n\n".getBytes("GBK");
                    byte[] text1 = "      粤香港式烧腊(第1联)\n\n".getBytes("GBK");
                    byte[] text2 = "------------------------\n\n*********预订单*********\n".getBytes("GBK");
                    byte[] text3 = "--------------------------------\n".getBytes("GBK");
                    byte[] text4 = "  期望送达时间:[18:00]\n\n".getBytes("GBK");
                    byte[] text5 = "备注: 别太辣\n".getBytes("GBK");
                    byte[] text6 = "菜品          数量   小计金额\n".getBytes("GBK");
                    byte[] text7 = "红烧肉          X1    12\n红烧肉1         X1    12\n红烧肉2         X1    12\n".getBytes("GBK");
                    byte[] text8 = "配送费                         5\n".getBytes("GBK");
                    byte[] text9 = "餐盒费                         1\n".getBytes("GBK");
                    byte[] text10 = "[超时赔付] - 详见订单\n".getBytes("GBK");
                    byte[] text11 = "可口可乐: x1\n".getBytes("GBK");
                    byte[] text12 = "合计                18元\n".getBytes("GBK");
                    byte[] text13 = "张* 18312345678\n地址信息\n".getBytes("GBK");
                    byte[] text14 = "  #1  美团测试\n\n\n".getBytes("GBK");
                    byte[] text15 = "下单时间: 01-01 12:00".getBytes("GBK");
                    byte[] nextLine = ESCUtil.nextLines(1);
                    byte[] performPrint = ESCUtil.performPrintAndFeedPaper((byte)160);

                    byte[][] cmdBytes = {printer_init,selectChinese,charCode,align0,
                            fontSize3,lineH3,text0,
                            fontSize1,lineH1,text1,
                            fontSize1,lineH2,text2,text4,
                            fontSize1,lineH1,text3,text15,
                            fontSize1,lineH2,text5,
                            fontSize1,lineH1,text6,text3,nextLine,
                            fontSize1,lineH2,text7,nextLine,
                            fontSize1,lineH1,text3,
                            fontSize1,lineH1,text8,text9,text10,text11,text3,
                            fontSize1,lineH2,text12,
                            fontSize1,lineH1,text3,
                            fontSize3,lineH3,text13,
                            fontSize1,lineH1,text3,text14,
                            fontSize3,lineH3,
                            performPrint};
                    try {
                        if((socket == null) || (!socket.isConnected()))
                        {
                            socket = BluetoothUtil.getSocket(mBluetoothPrinterDevice);
                        }
                        byte[] data = ESCUtil.byteMerger(cmdBytes);
                        OutputStream out = socket.getOutputStream();
                        out.write(data,0,data.length);
                        out.close();
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }
                catch (UnsupportedEncodingException e)
                {
                    e.printStackTrace();
                }

            }
        });
    }

    private void printElemoBill()
    {
        ThreadPoolManager.getInstance().executeTask(new Runnable() {
            @Override
            public void run() {
                try{
                    byte[] printer_init = ESCUtil.init_printer();
                    byte[] selectChinese = ESCUtil.selectChineseMode();
                    byte[] charCode = ESCUtil.selectCharCodeSystem((byte) 0x01);
                    byte[] fontSize0 = ESCUtil.fontSizeSet((byte) 0x00);
                    byte[] fontSize1 = ESCUtil.fontSizeSet((byte) 0x01);
                    byte[] fontSize2 = ESCUtil.fontSizeSet((byte) 0x10);
                    byte[] fontSize3 = ESCUtil.fontSizeSet((byte) 0x11);
                    byte[] lineH0 = ESCUtil.setLineHeight((byte)16);
                    byte[] lineH1 = ESCUtil.setLineHeight((byte)26);
                    byte[] lineH2 = ESCUtil.setLineHeight((byte)33);
                    byte[] lineH3 = ESCUtil.setLineHeight((byte)50);
                    byte[] align0 = ESCUtil.alignMode((byte)0);
                    byte[] align1 = ESCUtil.alignMode((byte)1);
                    String Elemo = "****#1饿了么外卖订单****\n        卡萨披萨       \n       --已支付--      \n" +
                            "      预计19:00送达     \n[时间]:2014-12-03 16:21\n  不吃辣 辣一点 多加米\n " +
                            "[发票]这是一个发票抬头\n------------------------\n菜名          数量  " +
                            "小计\n--------1号篮子---------\n测试美食一        X4   4\n" +
                            "测试美食二        X6   6\n测试美食三        X2   2\n" +
                            "--------2号篮子---------\n" +
                            "测试1             X1   1\n测试2             X1   1\n" +
                            "测试3             X1  23\n(+)测试西式甜点   X1   1\n" +
                            "(+)测试酸辣       X1   1\n--------3号篮子---------\n" +
                            "测试菜品名字很长很长很长\n测试              X1   1\n--------其它费用--------\n配送费\n\n";
                    byte[] ElemoData = Elemo.getBytes("GBK");
                    byte[] nextLine = ESCUtil.nextLines(1);
                    byte[] performPrint = ESCUtil.performPrintAndFeedPaper((byte)160);

                    byte[][] cmdBytes = {printer_init,selectChinese,charCode,align0,fontSize0,lineH1,ElemoData,
                            performPrint};
                    try {
                        if((socket == null) || (!socket.isConnected()))
                        {
                            socket = BluetoothUtil.getSocket(mBluetoothPrinterDevice);
                        }
                        byte[] data = ESCUtil.byteMerger(cmdBytes);
                        OutputStream out = socket.getOutputStream();
                        out.write(data,0,data.length);
                        out.close();
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }
                catch (UnsupportedEncodingException e)
                {
                    e.printStackTrace();
                }

            }
        });
    }

}
