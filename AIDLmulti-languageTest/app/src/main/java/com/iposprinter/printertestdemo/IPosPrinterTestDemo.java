package com.iposprinter.printertestdemo;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Random;

import android.support.v7.widget.LinearLayoutCompat;
import android.view.WindowManager;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;
import com.iposprinter.iposprinterservice.*;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;
import com.iposprinter.printertestdemo.Utils.ButtonDelayUtils;
import com.iposprinter.printertestdemo.Utils.HandlerUtils;

public class IPosPrinterTestDemo extends Activity implements OnClickListener {

    private static final String TAG = "IPosPrinterTestDemo";
    /* Demo 版本号*/
    private static final String VERSION = "V1.0.0";

    /*定义打印机状态*/
    private final int PRINTER_NORMAL = 0;
    private final int PRINTER_PAPERLESS = 1;
    private final int PRINTER_THP_HIGH_TEMPERATURE = 2;
    private final int PRINTER_MOTOR_HIGH_TEMPERATURE = 3;
    private final int PRINTER_IS_BUSY = 4;
    private final int PRINTER_ERROR_UNKNOWN = 5;
    /*打印机当前状态*/
    private int printerStatus = 0;

    /*定义状态广播*/
    private final String PRINTER_NORMAL_ACTION = "com.iposprinter.iposprinterservic e.NORMAL_ACTION";
    private final String PRINTER_PAPERLESS_ACTION = "com.iposprinter.iposprinterservice.PAPERLESS_ACTION";
    private final String PRINTER_PAPEREXISTS_ACTION = "com.iposprinter.iposprinterservice.PAPEREXISTS_ACTION";
    private final String PRINTER_THP_HIGHTEMP_ACTION = "com.iposprinter.iposprinterservice.THP_HIGHTEMP_ACTION";
    private final String PRINTER_THP_NORMALTEMP_ACTION = "com.iposprinter.iposprinterservice.THP_NORMALTEMP_ACTION";
    private final String PRINTER_MOTOR_HIGHTEMP_ACTION = "com.iposprinter.iposprinterservice.MOTOR_HIGHTEMP_ACTION";
    private final String PRINTER_BUSY_ACTION = "com.iposprinter.iposprinterservice.BUSY_ACTION";
    private final String PRINTER_CURRENT_TASK_PRINT_COMPLETE_ACTION = "com.iposprinter.iposprinterservice.CURRENT_TASK_PRINT_COMPLETE_ACTION";
    private final String GET_CUST_PRINTAPP_PACKAGENAME_ACTION = "android.print.action.CUST_PRINTAPP_PACKAGENAME";

    /*定义消息*/
    private final int MSG_TEST = 1;
    private final int MSG_IS_NORMAL = 2;
    private final int MSG_IS_BUSY = 3;
    private final int MSG_PAPER_LESS = 4;
    private final int MSG_PAPER_EXISTS = 5;
    private final int MSG_THP_HIGH_TEMP = 6;
    private final int MSG_THP_TEMP_NORMAL = 7;
    private final int MSG_MOTOR_HIGH_TEMP = 8;
    private final int MSG_MOTOR_HIGH_TEMP_INIT_PRINTER = 9;
    private final int MSG_CURRENT_TASK_PRINT_COMPLETE = 10;


    private TextView info;
    private IPosPrinterService mIPosPrinterService;
    private IPosPrinterCallback callback = null;

    private Random random = new Random();
    private HandlerUtils.MyHandler handler;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;
    /**
     * 消息处理
     */
    private HandlerUtils.IHandlerIntent iHandlerIntent = new HandlerUtils.IHandlerIntent() {
        @Override
        public void handlerIntent(Message msg) {
            switch (msg.what) {
                case MSG_TEST:
                    break;
                case MSG_IS_NORMAL:

                    break;
                case MSG_IS_BUSY:
                    Toast.makeText(IPosPrinterTestDemo.this, R.string.printer_is_working, Toast.LENGTH_SHORT).show();
                    break;
                case MSG_PAPER_LESS:
                    Toast.makeText(IPosPrinterTestDemo.this, R.string.out_of_paper, Toast.LENGTH_SHORT).show();
                    break;
                case MSG_PAPER_EXISTS:
                    Toast.makeText(IPosPrinterTestDemo.this, R.string.exists_paper, Toast.LENGTH_SHORT).show();
                    break;
                case MSG_THP_HIGH_TEMP:
                    Toast.makeText(IPosPrinterTestDemo.this, R.string.printer_high_temp_alarm, Toast.LENGTH_SHORT).show();
                    break;
                case MSG_MOTOR_HIGH_TEMP:
                    Toast.makeText(IPosPrinterTestDemo.this, R.string.motor_high_temp_alarm, Toast.LENGTH_SHORT).show();
                    handler.sendEmptyMessageDelayed(MSG_MOTOR_HIGH_TEMP_INIT_PRINTER, 180000);  //马达高温报警，等待3分钟后复位打印机
                    break;
                case MSG_MOTOR_HIGH_TEMP_INIT_PRINTER:
                    printerInit();
                    break;
                case MSG_CURRENT_TASK_PRINT_COMPLETE:
                    Toast.makeText(IPosPrinterTestDemo.this, R.string.printer_current_task_print_complete, Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
        }
    };

    private BroadcastReceiver IPosPrinterStatusListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) {
                Log.d(TAG, "IPosPrinterStatusListener onReceive action = null");
                return;
            }
            Log.d(TAG, "IPosPrinterStatusListener action = " + action);
            if (action.equals(PRINTER_NORMAL_ACTION)) {
                handler.sendEmptyMessageDelayed(MSG_IS_NORMAL, 0);
            } else if (action.equals(PRINTER_PAPERLESS_ACTION)) {
                handler.sendEmptyMessageDelayed(MSG_PAPER_LESS, 0);
            } else if (action.equals(PRINTER_BUSY_ACTION)) {
                handler.sendEmptyMessageDelayed(MSG_IS_BUSY, 0);
            } else if (action.equals(PRINTER_PAPEREXISTS_ACTION)) {
                handler.sendEmptyMessageDelayed(MSG_PAPER_EXISTS, 0);
            } else if (action.equals(PRINTER_THP_HIGHTEMP_ACTION)) {
                handler.sendEmptyMessageDelayed(MSG_THP_HIGH_TEMP, 0);
            } else if (action.equals(PRINTER_THP_NORMALTEMP_ACTION)) {
                handler.sendEmptyMessageDelayed(MSG_THP_TEMP_NORMAL, 0);
            } else if (action.equals(PRINTER_MOTOR_HIGHTEMP_ACTION))  //此时当前任务会继续打印，完成当前任务后，请等待2分钟以上时间，继续下一个打印任务
            {
                handler.sendEmptyMessageDelayed(MSG_MOTOR_HIGH_TEMP, 0);
            } else if (action.equals(PRINTER_CURRENT_TASK_PRINT_COMPLETE_ACTION)) {
                handler.sendEmptyMessageDelayed(MSG_CURRENT_TASK_PRINT_COMPLETE, 0);
            } else if (action.equals(GET_CUST_PRINTAPP_PACKAGENAME_ACTION)) {
                String mPackageName = intent.getPackage();
                Log.d(TAG, "*******GET_CUST_PRINTAPP_PACKAGENAME_ACTION：" + action + "*****mPackageName:" + mPackageName);

            } else {
                handler.sendEmptyMessageDelayed(MSG_TEST, 0);
            }
        }
    };


    /**
     * 绑定服务实例
     */
    private ServiceConnection connectService = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mIPosPrinterService = IPosPrinterService.Stub.asInterface(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mIPosPrinterService = null;
        }
    };

    public static void writePrintDataToCacheFile(String printStr, byte[] printByteData) {
        String printDataDirPath = Environment.getExternalStorageDirectory()+File.separator+"PrintDataCache";
        String printDataFilePath1 = printDataDirPath +File.separator+ "printdata_1.txt";
        String printDataFilePath2 = printDataDirPath +File.separator+ "printdata_2.txt";
        Log.d(TAG, "printDataDirPath:" + printDataDirPath);

        File fileDir = new File(printDataDirPath);
        if (!fileDir.exists()) {
            fileDir.mkdirs();
        }


        if (fileDir.exists()) {
            Log.d(TAG, printDataDirPath + " is exists!!!!!");
        } else {
            Log.d(TAG, printDataDirPath + " is not exists!!!!!");
        }

        File printDataFile = new File(printDataFilePath1);
        if (printDataFile.exists() && printDataFile.isFile()) {
            if (printDataFile.length() > 5 * 1024 * 1024) {
                printDataFile = new File(printDataFilePath2);
                if (printDataFile.exists() && printDataFile.isFile()) {
                    if (printDataFile.length() > 5 * 1024 * 1024) {
                        return;
                    }
                } else {
                    try {
                        printDataFile.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } else {
            printDataFile = new File(printDataFilePath2);
            if (printDataFile.exists() && printDataFile.isFile()) {
                if (printDataFile.length() > 5 * 1024 * 1024) {
                    printDataFile = new File(printDataFilePath1);
                }
            } else {
                printDataFile = new File(printDataFilePath1);
                try {
                    printDataFile.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        if ((printStr == null) && (printByteData == null)) {
            return;
        }

        FileOutputStream fileOut = null;
        if (printStr != null) {
            BufferedWriter outw = null;
            try {
                fileOut = new FileOutputStream(printDataFile, true);
                outw = new BufferedWriter(new OutputStreamWriter(fileOut));
                outw.write(printStr);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e1) {
                e1.printStackTrace();
            } finally {
                try {
                    if (outw != null) {
                        outw.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        if (printByteData != null) {
            BufferedOutputStream bufOut = null;
            try {
                fileOut = new FileOutputStream(printDataFile, true);
                bufOut = new BufferedOutputStream(fileOut);
                bufOut.write(printByteData);

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e1) {
                e1.printStackTrace();
            } finally {
                try {
                    if (fileOut != null) {
                        fileOut.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                try {
                    if (bufOut != null) {
                        bufOut.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ipos_printer_test_demo);
        //设置屏幕一直亮着，不进入休眠状态
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        handler = new HandlerUtils.MyHandler(iHandlerIntent);
        innitView();
        callback = new IPosPrinterCallback.Stub() {

            @Override
            public void onRunResult(final boolean isSuccess) throws RemoteException {
                Log.i(TAG, "result:" + isSuccess + "\n");
            }

            @Override
            public void onReturnString(final String value) throws RemoteException {
                Log.i(TAG, "result:" + value + "\n");
            }
        };

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(IPosPrinterTestDemo.this,new String[]{Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            }
        }

        writePrintDataToCacheFile("*****************", null);
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        //询问用户权限
        if (permissions[0].equals(Manifest.permission.WRITE_EXTERNAL_STORAGE) && grantResults[0]
                == PackageManager.PERMISSION_GRANTED) {
            //用户同意
        } else {
            //用户不同意
        }
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "activity onResume");
        super.onResume();
        //绑定服务
        Intent intent = new Intent();
        intent.setPackage("com.iposprinter.iposprinterservice");
        intent.setAction("com.iposprinter.iposprinterservice.IPosPrintService");
        //startService(intent);
        bindService(intent, connectService, Context.BIND_AUTO_CREATE);
        //注册打印机状态接收器
        IntentFilter printerStatusFilter = new IntentFilter();
        printerStatusFilter.addAction(PRINTER_NORMAL_ACTION);
        printerStatusFilter.addAction(PRINTER_PAPERLESS_ACTION);
        printerStatusFilter.addAction(PRINTER_PAPEREXISTS_ACTION);
        printerStatusFilter.addAction(PRINTER_THP_HIGHTEMP_ACTION);
        printerStatusFilter.addAction(PRINTER_THP_NORMALTEMP_ACTION);
        printerStatusFilter.addAction(PRINTER_MOTOR_HIGHTEMP_ACTION);
        printerStatusFilter.addAction(PRINTER_BUSY_ACTION);
        printerStatusFilter.addAction(GET_CUST_PRINTAPP_PACKAGENAME_ACTION);

        registerReceiver(IPosPrinterStatusListener, printerStatusFilter);
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "activity onPause");
        super.onPause();
    }

    @Override
    protected void onStop() {
        Log.e(TAG, "activity onStop");
        super.onStop();// ATTENTION: This was auto-generated to implement the App Indexing API.
// See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.end(client, getIndexApiAction());
        unregisterReceiver(IPosPrinterStatusListener);
        unbindService(connectService);
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.disconnect();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "activity onDestroy");
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }

    private void innitView() {

        findViewById(R.id.b_Chinese).setOnClickListener(this);
        findViewById(R.id.b_Traditional_Chinese).setOnClickListener(this);
        findViewById(R.id.b_english).setOnClickListener(this);
        findViewById(R.id.b_Korean).setOnClickListener(this);
        findViewById(R.id.b_Japanese).setOnClickListener(this);
        findViewById(R.id.b_Indonesia).setOnClickListener(this);
        findViewById(R.id.b_Malay).setOnClickListener(this);
        findViewById(R.id.b_Catalonia).setOnClickListener(this);
        findViewById(R.id.b_Czech).setOnClickListener(this);
        findViewById(R.id.b_Denmark).setOnClickListener(this);
        findViewById(R.id.b_German).setOnClickListener(this);
        findViewById(R.id.b_Estonia).setOnClickListener(this);
        findViewById(R.id.b_Spain).setOnClickListener(this);
        findViewById(R.id.b_Philippines).setOnClickListener(this);
        findViewById(R.id.b_French).setOnClickListener(this);
        findViewById(R.id.b_Croatia).setOnClickListener(this);
        findViewById(R.id.b_Italy).setOnClickListener(this);
        findViewById(R.id.b_Latvia).setOnClickListener(this);
        findViewById(R.id.b_Lithuania).setOnClickListener(this);
        findViewById(R.id.b_Hungary).setOnClickListener(this);
        findViewById(R.id.b_Dutch).setOnClickListener(this);
        findViewById(R.id.b_Norway).setOnClickListener(this);
        findViewById(R.id.b_poland).setOnClickListener(this);
        findViewById(R.id.b_Portugal).setOnClickListener(this);
        findViewById(R.id.b_Romania).setOnClickListener(this);
        findViewById(R.id.b_Slovakia).setOnClickListener(this);
        findViewById(R.id.b_Slovenia).setOnClickListener(this);
        findViewById(R.id.b_Finland).setOnClickListener(this);
        findViewById(R.id.b_Sweden).setOnClickListener(this);
        findViewById(R.id.b_Vietnam).setOnClickListener(this);
        findViewById(R.id.b_Turkey).setOnClickListener(this);
        findViewById(R.id.b_Greek).setOnClickListener(this);
        findViewById(R.id.b_Bulgaria).setOnClickListener(this);
        findViewById(R.id.b_Kazakh).setOnClickListener(this);
        findViewById(R.id.b_Russian).setOnClickListener(this);
        findViewById(R.id.b_Serbia).setOnClickListener(this);
        findViewById(R.id.b_Ukraine).setOnClickListener(this);
        findViewById(R.id.b_Thai).setOnClickListener(this);
        findViewById(R.id.b_exit).setOnClickListener(this);

        info = (TextView) findViewById(R.id.info);
        info.setText(VERSION);
    }

    @Override
    public void onClick(View v) {
        if (ButtonDelayUtils.isFastDoubleClick()) {
            return;
        }
        switch (v.getId()) {

            case R.id.b_Chinese:
                if (getPrinterStatus() == PRINTER_NORMAL)
                    printChinese();
                break;

            case R.id.b_Traditional_Chinese:
                if (getPrinterStatus() == PRINTER_NORMAL)
                    printTraditionalChinese();
                break;

            case R.id.b_english:
                if (getPrinterStatus() == PRINTER_NORMAL)
                {
                    printEnglish();
                }
                break;

            case R.id.b_Korean:
                if (getPrinterStatus() == PRINTER_NORMAL)
                     printKorean();
                break;
            case R.id.b_Japanese:
                if (getPrinterStatus() == PRINTER_NORMAL)
                    printJapanese();
                break;
            case R.id.b_Indonesia:
                if (getPrinterStatus() == PRINTER_NORMAL)
                    printIndonesia();
                break;
            case R.id.b_Malay:
                if (getPrinterStatus() == PRINTER_NORMAL)
                    printMalay();
                break;
            case R.id.b_Catalonia:
                if (getPrinterStatus() == PRINTER_NORMAL)
                    printCatalonia();
                break;
            case R.id.b_Czech:
                if (getPrinterStatus() == PRINTER_NORMAL)
                    printCzech();
                break;
            case R.id.b_Denmark:
                if (getPrinterStatus() == PRINTER_NORMAL)
                    printDenmark();
                break;

            case R.id.b_German:
                if (getPrinterStatus() == PRINTER_NORMAL)
                     printGerman();
                break;
            case R.id.b_Estonia:
                if (getPrinterStatus() == PRINTER_NORMAL)
                    printEstonia();
                break;
            case R.id.b_Spain:
                if (getPrinterStatus() == PRINTER_NORMAL)
                    printSpain();
                break;
            case R.id.b_Philippines:
                if (getPrinterStatus() == PRINTER_NORMAL)
                    printPhilippines();
                break;
            case R.id.b_French:
                if (getPrinterStatus() == PRINTER_NORMAL)
                    printFrench();
                break;
            case R.id.b_Croatia:
                if (getPrinterStatus() == PRINTER_NORMAL)
                    printCroatia();
                break;
            case R.id.b_Italy:
                if (getPrinterStatus() == PRINTER_NORMAL)
                    printItaly();
                break;
            case R.id.b_Latvia:
                if (getPrinterStatus() == PRINTER_NORMAL) {
                    printLatvia();
                }
                break;
            case R.id.b_Lithuania:
                if (getPrinterStatus() == PRINTER_NORMAL)
                    printLithuania();
                break;

            case R.id.b_Hungary:
                if (getPrinterStatus() == PRINTER_NORMAL)
                    printHungary();
                break;

            case R.id.b_Dutch:
                if (getPrinterStatus() == PRINTER_NORMAL)
                {
                    printDutch();
                }
                break;
            case R.id.b_Norway:
                if (getPrinterStatus() == PRINTER_NORMAL)
                    printNorway();
                break;
            case R.id.b_poland:
                if (getPrinterStatus() == PRINTER_NORMAL)
                    printpoland();
                break;
            case R.id.b_Portugal:
                if (getPrinterStatus() == PRINTER_NORMAL)
                    printPortugal();
                break;
            case R.id.b_Romania:
                if (getPrinterStatus() == PRINTER_NORMAL)
                    printRomania();
                break;
            case R.id.b_Slovakia:
                if (getPrinterStatus() == PRINTER_NORMAL)
                    printSlovakia();
                break;
            case R.id.b_Slovenia:
                if (getPrinterStatus() == PRINTER_NORMAL)
                    printSlovenia();
                break;
            case R.id.b_Finland:
                if (getPrinterStatus() == PRINTER_NORMAL)
                    printFinland();
                break;
            case R.id.b_Sweden:
                if (getPrinterStatus() == PRINTER_NORMAL)
                    printSweden();
                break;
            case R.id.b_Vietnam:
                if (getPrinterStatus() == PRINTER_NORMAL)
                    printVietnam();
                break;
            case R.id.b_Turkey:
                if (getPrinterStatus() == PRINTER_NORMAL)
                    printTurkey();
                break;
            case R.id.b_Greek:
                if (getPrinterStatus() == PRINTER_NORMAL)
                    printGreek();
                break;
            case R.id.b_Bulgaria:
                if (getPrinterStatus() == PRINTER_NORMAL)
                    printBulgaria();
                break;
            case R.id.b_Kazakh:
                if (getPrinterStatus() == PRINTER_NORMAL)
                    printKazakh();
                break;
            case R.id.b_Russian:
                if (getPrinterStatus() == PRINTER_NORMAL)
                    printRussian();
                break;
            case R.id.b_Serbia:
                if (getPrinterStatus() == PRINTER_NORMAL)
                    printSerbia();
                break;
            case R.id.b_Ukraine:
                if (getPrinterStatus() == PRINTER_NORMAL)
                    printUkraine();
                break;
            case R.id.b_Thai:
                if (getPrinterStatus() == PRINTER_NORMAL)
                    printThai();
                break;
            //退出应用
            case R.id.b_exit:
                finish();
                break;



            default:
                break;
        }
    }

    /**
     * 获取打印机状态
     */
    public int getPrinterStatus() {

        Log.i(TAG, "***** printerStatus" + printerStatus);
        try {
            printerStatus = mIPosPrinterService.getPrinterStatus();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        Log.i(TAG, "#### printerStatus" + printerStatus);
        return printerStatus;
    }

    /**
     * 打印机初始化
     */
    public void printerInit() {
        ThreadPoolManager.getInstance().executeTask(new Runnable() {
            @Override
            public void run() {
                try {
                    mIPosPrinterService.printerInit(callback);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void printChinese() {
        ThreadPoolManager.getInstance().executeTask(new Runnable() {
            @Override
            public void run() {
                try {
                    mIPosPrinterService.printSpecifiedTypeText("汉语，即汉族的传统语言，是中国通用语言，国际通用语言之一，属汉藏语系，同中国境内的藏语、壮语、傣语、侗语、黎语、彝语、苗语、瑶语，中国境外的泰语、缅甸语等都是亲属语言\n", "ST", 16, callback);
                    mIPosPrinterService.printSpecifiedTypeText("汉语，即汉族的传统语言，是中国通用语言，国际通用语言之一，属汉藏语系，同中国境内的藏语、壮语、傣语、侗语、黎语、彝语、苗语、瑶语，中国境外的泰语、缅甸语等都是亲属语言\n", "ST", 24, callback);
                    mIPosPrinterService.printSpecifiedTypeText("汉语，即汉族的传统语言，是中国通用语言，国际通用语言之一，属汉藏语系，同中国境内的藏语、壮语、傣语、侗语、黎语、彝语、苗语、瑶语，中国境外的泰语、缅甸语等都是亲属语言\n", "ST", 32, callback);
                    mIPosPrinterService.printSpecifiedTypeText("汉语，即汉族的传统语言，是中国通用语言，国际通用语言之一，属汉藏语系，同中国境内的藏语、壮语、傣语、侗语、黎语、彝语、苗语、瑶语，中国境外的泰语、缅甸语等都是亲属语言\n", "ST", 48, callback);
                    mIPosPrinterService.printerPerformPrint(160, callback);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void printTraditionalChinese() {
        ThreadPoolManager.getInstance().executeTask(new Runnable() {
            @Override
            public void run() {
                try {
                    mIPosPrinterService.printSpecifiedTypeText("繁體中文（又稱正體中文），即傳統上的中華文化中所使用的中文書寫體系，目前已有二千年以上的歷史，直到20世紀一直是各地華人中通用的中文書寫標準。 1950年代開始大陸官方在繁體中文的基礎上所做的簡化形成了新的中文書寫標準，即簡體中文。\n", "ST", 16, callback);
                    mIPosPrinterService.printSpecifiedTypeText("繁體中文（又稱正體中文），即傳統上的中華文化中所使用的中文書寫體系，目前已有二千年以上的歷史，直到20世紀一直是各地華人中通用的中文書寫標準。 1950年代開始大陸官方在繁體中文的基礎上所做的簡化形成了新的中文書寫標準，即簡體中文。\n", "ST", 24, callback);
                    mIPosPrinterService.printSpecifiedTypeText("繁體中文（又稱正體中文），即傳統上的中華文化中所使用的中文書寫體系，目前已有二千年以上的歷史，直到20世紀一直是各地華人中通用的中文書寫標準。 1950年代開始大陸官方在繁體中文的基礎上所做的簡化形成了新的中文書寫標準，即簡體中文。\n", "ST", 32, callback);
                    mIPosPrinterService.printSpecifiedTypeText("繁體中文（又稱正體中文），即傳統上的中華文化中所使用的中文書寫體系，目前已有二千年以上的歷史，直到20世紀一直是各地華人中通用的中文書寫標準。 1950年代開始大陸官方在繁體中文的基礎上所做的簡化形成了新的中文書寫標準，即簡體中文。\n", "ST", 48, callback);
                    mIPosPrinterService.printerPerformPrint(160, callback);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void printEnglish() {
        ThreadPoolManager.getInstance().executeTask(new Runnable() {
            @Override
            public void run() {
                try {
                    mIPosPrinterService.printSpecifiedTypeText("English is a language of the Indo-European-Germanic language. It consists of 26 letters. The English letters are derived from the Latin alphabet. The Latin alphabet is derived from the Greek alphabet, while the Greek alphabet is derived from the Phoenician alphabet. of. English is the official language of international design (as a native language) and the most widely spoken first language in the world.\n", "ST", 16, callback);
                    mIPosPrinterService.printSpecifiedTypeText("English is a language of the Indo-European-Germanic language. It consists of 26 letters. The English letters are derived from the Latin alphabet. The Latin alphabet is derived from the Greek alphabet, while the Greek alphabet is derived from the Phoenician alphabet. of. English is the official language of international design (as a native language) and the most widely spoken first language in the world.\n", "ST", 24, callback);
                    mIPosPrinterService.printSpecifiedTypeText("English is a language of the Indo-European-Germanic language. It consists of 26 letters. The English letters are derived from the Latin alphabet. The Latin alphabet is derived from the Greek alphabet, while the Greek alphabet is derived from the Phoenician alphabet. of. English is the official language of international design (as a native language) and the most widely spoken first language in the world.\n", "ST", 32, callback);
                    mIPosPrinterService.printSpecifiedTypeText("English is a language of the Indo-European-Germanic language. It consists of 26 letters. The English letters are derived from the Latin alphabet. The Latin alphabet is derived from the Greek alphabet, while the Greek alphabet is derived from the Phoenician alphabet. of. English is the official language of international design (as a native language) and the most widely spoken first language in the world.\n", "ST", 48, callback);
                    mIPosPrinterService.printerPerformPrint(160, callback);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void printKorean() {
        ThreadPoolManager.getInstance().executeTask(new Runnable() {
            @Override
            public void run() {
                try {
                    mIPosPrinterService.printSpecifiedTypeText("한국어 (한국어), 한국어 (조선말), 한국어 (Dao)로 불리는이 텍스트는 한국 미술이라고합니다. 그것은 한국 민족이 사용하는 언어입니다.\n", "ST", 16, callback);
                    mIPosPrinterService.printSpecifiedTypeText("한국어 (한국어), 한국어 (조선말), 한국어 (Dao)로 불리는이 텍스트는 한국 미술이라고합니다. 그것은 한국 민족이 사용하는 언어입니다.\n", "ST", 24, callback);
                    mIPosPrinterService.printSpecifiedTypeText("한국어 (한국어), 한국어 (조선말), 한국어 (Dao)로 불리는이 텍스트는 한국 미술이라고합니다. 그것은 한국 민족이 사용하는 언어입니다.\n", "ST", 32, callback);
                    mIPosPrinterService.printSpecifiedTypeText("한국어 (한국어), 한국어 (조선말), 한국어 (Dao)로 불리는이 텍스트는 한국 미술이라고합니다. 그것은 한국 민족이 사용하는 언어입니다.\n", "ST", 48, callback);
                    mIPosPrinterService.printerPerformPrint(160, callback);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void printJapanese() {
        ThreadPoolManager.getInstance().executeTask(new Runnable() {
            @Override
            public void run() {
                try {
                    mIPosPrinterService.printSpecifiedTypeText("日本語は日本語 - 高句麗語または扶余語に分類され、ネイティブスピーカーの人数は1億2500万人で、日本人の人口は世界人口の3.1％を占めています。\n", "ST", 16, callback);
                    mIPosPrinterService.printSpecifiedTypeText("日本語は日本語 - 高句麗語または扶余語に分類され、ネイティブスピーカーの人数は1億2500万人で、日本人の人口は世界人口の3.1％を占めています。\n", "ST", 24, callback);
                    mIPosPrinterService.printSpecifiedTypeText("日本語は日本語 - 高句麗語または扶余語に分類され、ネイティブスピーカーの人数は1億2500万人で、日本人の人口は世界人口の3.1％を占めています。\n", "ST", 32, callback);
                    mIPosPrinterService.printSpecifiedTypeText("日本語は日本語 - 高句麗語または扶余語に分類され、ネイティブスピーカーの人数は1億2500万人で、日本人の人口は世界人口の3.1％を占めています。\n", "ST", 48, callback);
                    mIPosPrinterService.printerPerformPrint(160, callback);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void printIndonesia() {
        ThreadPoolManager.getInstance().executeTask(new Runnable() {
            @Override
            public void run() {
                try {
                    mIPosPrinterService.printSpecifiedTypeText("Bahasa Indonesia adalah bahasa Melayu berdasarkan dialek Riau dan merupakan bahasa resmi Indonesia. Sekitar 30 juta orang di dunia menggunakan bahasa Indonesia sebagai bahasa ibu mereka, dan sekitar 140 juta orang menggunakan bahasa Indonesia sebagai bahasa kedua untuk membaca dan berbicara bahasa Indonesia.\n", "ST", 16, callback);
                    mIPosPrinterService.printSpecifiedTypeText("Bahasa Indonesia adalah bahasa Melayu berdasarkan dialek Riau dan merupakan bahasa resmi Indonesia. Sekitar 30 juta orang di dunia menggunakan bahasa Indonesia sebagai bahasa ibu mereka, dan sekitar 140 juta orang menggunakan bahasa Indonesia sebagai bahasa kedua untuk membaca dan berbicara bahasa Indonesia.\n", "ST", 24, callback);
                    mIPosPrinterService.printSpecifiedTypeText("Bahasa Indonesia adalah bahasa Melayu berdasarkan dialek Riau dan merupakan bahasa resmi Indonesia. Sekitar 30 juta orang di dunia menggunakan bahasa Indonesia sebagai bahasa ibu mereka, dan sekitar 140 juta orang menggunakan bahasa Indonesia sebagai bahasa kedua untuk membaca dan berbicara bahasa Indonesia.\n", "ST", 32, callback);
                    mIPosPrinterService.printSpecifiedTypeText("Bahasa Indonesia adalah bahasa Melayu berdasarkan dialek Riau dan merupakan bahasa resmi Indonesia. Sekitar 30 juta orang di dunia menggunakan bahasa Indonesia sebagai bahasa ibu mereka, dan sekitar 140 juta orang menggunakan bahasa Indonesia sebagai bahasa kedua untuk membaca dan berbicara bahasa Indonesia.\n", "ST", 48, callback);
                    mIPosPrinterService.printerPerformPrint(160, callback);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void printMalay() {
        ThreadPoolManager.getInstance().executeTask(new Runnable() {
            @Override
            public void run() {
                try {
                    mIPosPrinterService.printSpecifiedTypeText("Bahasa Melayu adalah bahasa yang sama dengan bahasa Indonesia. Ia adalah bahasa rasmi Malaysia dan Brunei dan salah satu bahasa rasmi Singapura. Terdapat dua makna dalam pengertian luas dan sempit. Bahasa Melayu secara merujuk merujuk kepada bahasa-bahasa kepunyaan keluarga bahasa Indonesia keluarga bahasa Pulau Selatan, bahasa Melayu dalam arti sempit merujuk kepada bahasa yang digunakan di sekitar Selat Melaka.\n", "ST", 16, callback);
                    mIPosPrinterService.printSpecifiedTypeText("Bahasa Melayu adalah bahasa yang sama dengan bahasa Indonesia. Ia adalah bahasa rasmi Malaysia dan Brunei dan salah satu bahasa rasmi Singapura. Terdapat dua makna dalam pengertian luas dan sempit. Bahasa Melayu secara merujuk merujuk kepada bahasa-bahasa kepunyaan keluarga bahasa Indonesia keluarga bahasa Pulau Selatan, bahasa Melayu dalam arti sempit merujuk kepada bahasa yang digunakan di sekitar Selat Melaka.\n", "ST", 24, callback);
                    mIPosPrinterService.printSpecifiedTypeText("Bahasa Melayu adalah bahasa yang sama dengan bahasa Indonesia. Ia adalah bahasa rasmi Malaysia dan Brunei dan salah satu bahasa rasmi Singapura. Terdapat dua makna dalam pengertian luas dan sempit. Bahasa Melayu secara merujuk merujuk kepada bahasa-bahasa kepunyaan keluarga bahasa Indonesia keluarga bahasa Pulau Selatan, bahasa Melayu dalam arti sempit merujuk kepada bahasa yang digunakan di sekitar Selat Melaka.\n", "ST", 32, callback);
                    mIPosPrinterService.printSpecifiedTypeText("Bahasa Melayu adalah bahasa yang sama dengan bahasa Indonesia. Ia adalah bahasa rasmi Malaysia dan Brunei dan salah satu bahasa rasmi Singapura. Terdapat dua makna dalam pengertian luas dan sempit. Bahasa Melayu secara merujuk merujuk kepada bahasa-bahasa kepunyaan keluarga bahasa Indonesia keluarga bahasa Pulau Selatan, bahasa Melayu dalam arti sempit merujuk kepada bahasa yang digunakan di sekitar Selat Melaka.\n", "ST", 48, callback);
                    mIPosPrinterService.printerPerformPrint(160, callback);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void printCatalonia() {
        ThreadPoolManager.getInstance().executeTask(new Runnable() {
            @Override
            public void run() {
                try {
                    mIPosPrinterService.printSpecifiedTypeText("El català (català, d'ara endavant denominat canadenc), el llenguatge dels catalans, també conegut en algunes àrees com el valencià (Valencià) és un romance indoeuropeo, una de les llengües oficials d'Espanya.\n", "ST", 16, callback);
                    mIPosPrinterService.printSpecifiedTypeText("El català (català, d'ara endavant denominat canadenc), el llenguatge dels catalans, també conegut en algunes àrees com el valencià (Valencià) és un romance indoeuropeo, una de les llengües oficials d'Espanya.\n", "ST", 24, callback);
                    mIPosPrinterService.printSpecifiedTypeText("El català (català, d'ara endavant denominat canadenc), el llenguatge dels catalans, també conegut en algunes àrees com el valencià (Valencià) és un romance indoeuropeo, una de les llengües oficials d'Espanya.\n", "ST", 32, callback);
                    mIPosPrinterService.printSpecifiedTypeText("El català (català, d'ara endavant denominat canadenc), el llenguatge dels catalans, també conegut en algunes àrees com el valencià (Valencià) és un romance indoeuropeo, una de les llengües oficials d'Espanya.\n", "ST", 48, callback);
                    mIPosPrinterService.printerPerformPrint(160, callback);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void printCzech() {
        ThreadPoolManager.getInstance().executeTask(new Runnable() {
            @Override
            public void run() {
                try {
                    mIPosPrinterService.printSpecifiedTypeText("Český jazyk je oficiálním jazykem České republiky. Čeština a polština, slovenština, sorbština apod. Patří do slovanské větve slovanského jazyka. Čeština je velmi obtížné se učit. Jedním z důvodů je, že český jazyk je bohatý na formu. Podle statistik je v českém jazyce více než 200 formulářů.\n", "ST", 16, callback);
                    mIPosPrinterService.printSpecifiedTypeText("Český jazyk je oficiálním jazykem České republiky. Čeština a polština, slovenština, sorbština apod. Patří do slovanské větve slovanského jazyka. Čeština je velmi obtížné se učit. Jedním z důvodů je, že český jazyk je bohatý na formu. Podle statistik je v českém jazyce více než 200 formulářů.\n", "ST", 24, callback);
                    mIPosPrinterService.printSpecifiedTypeText("Český jazyk je oficiálním jazykem České republiky. Čeština a polština, slovenština, sorbština apod. Patří do slovanské větve slovanského jazyka. Čeština je velmi obtížné se učit. Jedním z důvodů je, že český jazyk je bohatý na formu. Podle statistik je v českém jazyce více než 200 formulářů.\n", "ST", 32, callback);
                    mIPosPrinterService.printSpecifiedTypeText("Český jazyk je oficiálním jazykem České republiky. Čeština a polština, slovenština, sorbština apod. Patří do slovanské větve slovanského jazyka. Čeština je velmi obtížné se učit. Jedním z důvodů je, že český jazyk je bohatý na formu. Podle statistik je v českém jazyce více než 200 formulářů.\n", "ST", 48, callback);
                    mIPosPrinterService.printerPerformPrint(160, callback);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void printDenmark() {
        ThreadPoolManager.getInstance().executeTask(new Runnable() {
            @Override
            public void run() {
                try {
                    mIPosPrinterService.printSpecifiedTypeText("Dansk, Kongeriget Danmarks officielle sprog, taler i Kongeriget Danmark og dets territorier på Færøerne og Grønland. Det er også sporadisk i dele af Tyskland, Norge og Sverige. Det tilhører den europæiske indo-europæisk-germansk nordnorske tyske afdeling.\n", "ST", 16, callback);
                    mIPosPrinterService.printSpecifiedTypeText("Dansk, Kongeriget Danmarks officielle sprog, taler i Kongeriget Danmark og dets territorier på Færøerne og Grønland. Det er også sporadisk i dele af Tyskland, Norge og Sverige. Det tilhører den europæiske indo-europæisk-germansk nordnorske tyske afdeling.\n", "ST", 24, callback);
                    mIPosPrinterService.printSpecifiedTypeText("Dansk, Kongeriget Danmarks officielle sprog, taler i Kongeriget Danmark og dets territorier på Færøerne og Grønland. Det er også sporadisk i dele af Tyskland, Norge og Sverige. Det tilhører den europæiske indo-europæisk-germansk nordnorske tyske afdeling.\n", "ST", 32, callback);
                    mIPosPrinterService.printSpecifiedTypeText("Dansk, Kongeriget Danmarks officielle sprog, taler i Kongeriget Danmark og dets territorier på Færøerne og Grønland. Det er også sporadisk i dele af Tyskland, Norge og Sverige. Det tilhører den europæiske indo-europæisk-germansk nordnorske tyske afdeling.\n", "ST", 48, callback);
                    mIPosPrinterService.printerPerformPrint(160, callback);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void printGerman() {
        ThreadPoolManager.getInstance().executeTask(new Runnable() {
            @Override
            public void run() {
                try {
                    mIPosPrinterService.printSpecifiedTypeText("Die Anzahl der Menschen in Deutschland beträgt 3,01% der Weltbevölkerung, sie ist die sechstplazierte Sprache der Welt, gemessen an der Anzahl der Länder, und eine der größten Sprachen der Welt und die am häufigsten verwendete Muttersprache in der Europäischen Union.\n", "ST", 16, callback);
                    mIPosPrinterService.printSpecifiedTypeText("Die Anzahl der Menschen in Deutschland beträgt 3,01% der Weltbevölkerung, sie ist die sechstplazierte Sprache der Welt, gemessen an der Anzahl der Länder, und eine der größten Sprachen der Welt und die am häufigsten verwendete Muttersprache in der Europäischen Union.\n", "ST", 24, callback);
                    mIPosPrinterService.printSpecifiedTypeText("Die Anzahl der Menschen in Deutschland beträgt 3,01% der Weltbevölkerung, sie ist die sechstplazierte Sprache der Welt, gemessen an der Anzahl der Länder, und eine der größten Sprachen der Welt und die am häufigsten verwendete Muttersprache in der Europäischen Union.\n", "ST", 32, callback);
                    mIPosPrinterService.printSpecifiedTypeText("Die Anzahl der Menschen in Deutschland beträgt 3,01% der Weltbevölkerung, sie ist die sechstplazierte Sprache der Welt, gemessen an der Anzahl der Länder, und eine der größten Sprachen der Welt und die am häufigsten verwendete Muttersprache in der Europäischen Union.\n", "ST", 48, callback);
                    mIPosPrinterService.printerPerformPrint(160, callback);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void printEstonia() {
        ThreadPoolManager.getInstance().executeTask(new Runnable() {
            @Override
            public void run() {
                try {
                    mIPosPrinterService.printSpecifiedTypeText("Estoni (Eesti keel) on Ida-Euroopa riikides Eesti riigikeel. Seda kasutab umbes 1,1 miljonit inimest ja seda kasutab 900 000 inimest emakeelena. See on üks Euroopa Liidu ametlikest keeltest.\n", "ST", 16, callback);
                    mIPosPrinterService.printSpecifiedTypeText("Estoni (Eesti keel) on Ida-Euroopa riikides Eesti riigikeel. Seda kasutab umbes 1,1 miljonit inimest ja seda kasutab 900 000 inimest emakeelena. See on üks Euroopa Liidu ametlikest keeltest.\n", "ST", 24, callback);
                    mIPosPrinterService.printSpecifiedTypeText("Estoni (Eesti keel) on Ida-Euroopa riikides Eesti riigikeel. Seda kasutab umbes 1,1 miljonit inimest ja seda kasutab 900 000 inimest emakeelena. See on üks Euroopa Liidu ametlikest keeltest.\n", "ST", 32, callback);
                    mIPosPrinterService.printSpecifiedTypeText("Estoni (Eesti keel) on Ida-Euroopa riikides Eesti riigikeel. Seda kasutab umbes 1,1 miljonit inimest ja seda kasutab 900 000 inimest emakeelena. See on üks Euroopa Liidu ametlikest keeltest.\n", "ST", 48, callback);
                    mIPosPrinterService.printerPerformPrint(160, callback);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void printSpain() {
        ThreadPoolManager.getInstance().executeTask(new Runnable() {
            @Override
            public void run() {
                try {
                    mIPosPrinterService.printSpecifiedTypeText("El español se conoce como español (español) en partes de España, Estados Unidos, México, América Central, el Caribe, Colombia, Ecuador y Uruguay, mientras que en otras regiones, el español se conoce principalmente como Castellano. . El español es uno de los seis idiomas oficiales de las Naciones Unidas.\n", "ST", 16, callback);
                    mIPosPrinterService.printSpecifiedTypeText("El español se conoce como español (español) en partes de España, Estados Unidos, México, América Central, el Caribe, Colombia, Ecuador y Uruguay, mientras que en otras regiones, el español se conoce principalmente como Castellano. . El español es uno de los seis idiomas oficiales de las Naciones Unidas.\n", "ST", 24, callback);
                    mIPosPrinterService.printSpecifiedTypeText("El español se conoce como español (español) en partes de España, Estados Unidos, México, América Central, el Caribe, Colombia, Ecuador y Uruguay, mientras que en otras regiones, el español se conoce principalmente como Castellano. . El español es uno de los seis idiomas oficiales de las Naciones Unidas.\n", "ST", 32, callback);
                    mIPosPrinterService.printSpecifiedTypeText("El español se conoce como español (español) en partes de España, Estados Unidos, México, América Central, el Caribe, Colombia, Ecuador y Uruguay, mientras que en otras regiones, el español se conoce principalmente como Castellano. . El español es uno de los seis idiomas oficiales de las Naciones Unidas.\n", "ST", 48, callback);
                    mIPosPrinterService.printerPerformPrint(160, callback);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void printPhilippines() {
        ThreadPoolManager.getInstance().executeTask(new Runnable() {
            @Override
            public void run() {
                try {
                    mIPosPrinterService.printSpecifiedTypeText("Ang wikang Tagalog ay kabilang sa pamilyang Malay-Polynesian na pamilya ng pamilyang wika ng South Island at higit sa lahat ay ginagamit sa Pilipinas. Ang \"Filipino\", na kilala bilang opisyal na wika ng Pilipinas, ay binuo sa Tagalog bilang pangunahing katawan.\n", "ST", 16, callback);
                    mIPosPrinterService.printSpecifiedTypeText("Ang wikang Tagalog ay kabilang sa pamilyang Malay-Polynesian na pamilya ng pamilyang wika ng South Island at higit sa lahat ay ginagamit sa Pilipinas. Ang \"Filipino\", na kilala bilang opisyal na wika ng Pilipinas, ay binuo sa Tagalog bilang pangunahing katawan.\n", "ST", 24, callback);
                    mIPosPrinterService.printSpecifiedTypeText("Ang wikang Tagalog ay kabilang sa pamilyang Malay-Polynesian na pamilya ng pamilyang wika ng South Island at higit sa lahat ay ginagamit sa Pilipinas. Ang \"Filipino\", na kilala bilang opisyal na wika ng Pilipinas, ay binuo sa Tagalog bilang pangunahing katawan.\n", "ST", 32, callback);
                    mIPosPrinterService.printSpecifiedTypeText("Ang wikang Tagalog ay kabilang sa pamilyang Malay-Polynesian na pamilya ng pamilyang wika ng South Island at higit sa lahat ay ginagamit sa Pilipinas. Ang \"Filipino\", na kilala bilang opisyal na wika ng Pilipinas, ay binuo sa Tagalog bilang pangunahing katawan.\n", "ST", 48, callback);
                    mIPosPrinterService.printerPerformPrint(160, callback);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void printFrench() {
        ThreadPoolManager.getInstance().executeTask(new Runnable() {
            @Override
            public void run() {
                try {
                    mIPosPrinterService.printSpecifiedTypeText("Le français est l'une des langues les plus indépendantes de la langue romane après la langue espagnole. Il y a actuellement 87 millions de personnes dans le monde qui l'utilisent comme langue maternelle et 285 millions d'autres personnes (y compris ceux qui l'utilisent comme langue seconde)\n", "ST", 16, callback);
                    mIPosPrinterService.printSpecifiedTypeText("Le français est l'une des langues les plus indépendantes de la langue romane après la langue espagnole. Il y a actuellement 87 millions de personnes dans le monde qui l'utilisent comme langue maternelle et 285 millions d'autres personnes (y compris ceux qui l'utilisent comme langue seconde)\n", "ST", 24, callback);
                    mIPosPrinterService.printSpecifiedTypeText("Le français est l'une des langues les plus indépendantes de la langue romane après la langue espagnole. Il y a actuellement 87 millions de personnes dans le monde qui l'utilisent comme langue maternelle et 285 millions d'autres personnes (y compris ceux qui l'utilisent comme langue seconde)\n", "ST", 32, callback);
                    mIPosPrinterService.printSpecifiedTypeText("Le français est l'une des langues les plus indépendantes de la langue romane après la langue espagnole. Il y a actuellement 87 millions de personnes dans le monde qui l'utilisent comme langue maternelle et 285 millions d'autres personnes (y compris ceux qui l'utilisent comme langue seconde)\n", "ST", 48, callback);
                    mIPosPrinterService.printerPerformPrint(160, callback);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void printCroatia() {
        ThreadPoolManager.getInstance().executeTask(new Runnable() {
            @Override
            public void run() {
                try {
                    mIPosPrinterService.printSpecifiedTypeText("Hrvatska je razvijena kapitalistička zemlja. Gospodarstvo dominira tercijarnom industrijom, a sekundarna industrija je zamjenik. Turizam je važan dio nacionalnog gospodarstva. Hrvatska ima čvrste ekonomske temelje i visoku razinu razvoja turizma, građevinarstva, brodogradnje i farmaceutske industrije. Nogomet i tenis vezani su za prvi pokret u Hrvatskoj.\n", "ST", 16, callback);
                    mIPosPrinterService.printSpecifiedTypeText("Hrvatska je razvijena kapitalistička zemlja. Gospodarstvo dominira tercijarnom industrijom, a sekundarna industrija je zamjenik. Turizam je važan dio nacionalnog gospodarstva. Hrvatska ima čvrste ekonomske temelje i visoku razinu razvoja turizma, građevinarstva, brodogradnje i farmaceutske industrije. Nogomet i tenis vezani su za prvi pokret u Hrvatskoj.\n", "ST", 24, callback);
                    mIPosPrinterService.printSpecifiedTypeText("Hrvatska je razvijena kapitalistička zemlja. Gospodarstvo dominira tercijarnom industrijom, a sekundarna industrija je zamjenik. Turizam je važan dio nacionalnog gospodarstva. Hrvatska ima čvrste ekonomske temelje i visoku razinu razvoja turizma, građevinarstva, brodogradnje i farmaceutske industrije. Nogomet i tenis vezani su za prvi pokret u Hrvatskoj.\n", "ST", 32, callback);
                    mIPosPrinterService.printSpecifiedTypeText("Hrvatska je razvijena kapitalistička zemlja. Gospodarstvo dominira tercijarnom industrijom, a sekundarna industrija je zamjenik. Turizam je važan dio nacionalnog gospodarstva. Hrvatska ima čvrste ekonomske temelje i visoku razinu razvoja turizma, građevinarstva, brodogradnje i farmaceutske industrije. Nogomet i tenis vezani su za prvi pokret u Hrvatskoj.\n", "ST", 48, callback);
                    mIPosPrinterService.printerPerformPrint(160, callback);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void printItaly() {
        ThreadPoolManager.getInstance().executeTask(new Runnable() {
            @Override
            public void run() {
                try {
                    mIPosPrinterService.printSpecifiedTypeText("L'italiano è una delle lingue più belle del mondo. L'italiano sembra bello e bello, e la gente elogia l'italiano chiaro come il vento, con parole come fiori che sbocciano. L'italiano è conosciuto come la lingua più artistica e la lingua più musicale del mondo.\n", "ST", 16, callback);
                    mIPosPrinterService.printSpecifiedTypeText("L'italiano è una delle lingue più belle del mondo. L'italiano sembra bello e bello, e la gente elogia l'italiano chiaro come il vento, con parole come fiori che sbocciano. L'italiano è conosciuto come la lingua più artistica e la lingua più musicale del mondo.\n", "ST", 24, callback);
                    mIPosPrinterService.printSpecifiedTypeText("L'italiano è una delle lingue più belle del mondo. L'italiano sembra bello e bello, e la gente elogia l'italiano chiaro come il vento, con parole come fiori che sbocciano. L'italiano è conosciuto come la lingua più artistica e la lingua più musicale del mondo.\n", "ST", 32, callback);
                    mIPosPrinterService.printSpecifiedTypeText("L'italiano è una delle lingue più belle del mondo. L'italiano sembra bello e bello, e la gente elogia l'italiano chiaro come il vento, con parole come fiori che sbocciano. L'italiano è conosciuto come la lingua più artistica e la lingua più musicale del mondo.\n", "ST", 48, callback);
                    mIPosPrinterService.printerPerformPrint(160, callback);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void printLatvia() {
        ThreadPoolManager.getInstance().executeTask(new Runnable() {
            @Override
            public void run() {
                try {
                    mIPosPrinterService.printSpecifiedTypeText("Latvija ir vienota valsts, kas sastāv no 109 pašvaldībām un 9 pašvaldībām. Latvieši pieder pie Baltijas iedzīvotājiem un ir kulturāli tuvu lietuviešiem. Latviešu valoda ir indoeiropiešu valoda, un latviešu un lietuviešu valodas ir vienīgās Baltijas valodu saimes valodas.\n", "ST", 16, callback);
                    mIPosPrinterService.printSpecifiedTypeText("Latvija ir vienota valsts, kas sastāv no 109 pašvaldībām un 9 pašvaldībām. Latvieši pieder pie Baltijas iedzīvotājiem un ir kulturāli tuvu lietuviešiem. Latviešu valoda ir indoeiropiešu valoda, un latviešu un lietuviešu valodas ir vienīgās Baltijas valodu saimes valodas.\n", "ST", 24, callback);
                    mIPosPrinterService.printSpecifiedTypeText("Latvija ir vienota valsts, kas sastāv no 109 pašvaldībām un 9 pašvaldībām. Latvieši pieder pie Baltijas iedzīvotājiem un ir kulturāli tuvu lietuviešiem. Latviešu valoda ir indoeiropiešu valoda, un latviešu un lietuviešu valodas ir vienīgās Baltijas valodu saimes valodas.\n", "ST", 32, callback);
                    mIPosPrinterService.printSpecifiedTypeText("Latvija ir vienota valsts, kas sastāv no 109 pašvaldībām un 9 pašvaldībām. Latvieši pieder pie Baltijas iedzīvotājiem un ir kulturāli tuvu lietuviešiem. Latviešu valoda ir indoeiropiešu valoda, un latviešu un lietuviešu valodas ir vienīgās Baltijas valodu saimes valodas.\n", "ST", 48, callback);
                    mIPosPrinterService.printerPerformPrint(160, callback);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void printLithuania() {
        ThreadPoolManager.getInstance().executeTask(new Runnable() {
            @Override
            public void run() {
                try {
                    mIPosPrinterService.printSpecifiedTypeText("Lietuviška (lietuvių kalba) yra lietuvių kalba, kuri yra oficiali Lietuvos kalba, kurią naudoja 4 milijonai lietuvių. Lietuvių kalba yra viena iš dviejų esamų baltų kalbų (kita - latvių). Baltijos kalbų šeima yra indoeuropiečių kalbos šeimos dalis. Tai indoeuropiečių kalbos šeimos rytinė šaka ir baltųjų kalbų šeima. Tai viena iš dviejų likusių kalbų šeimos kalbų (kita - latvių).\n", "ST", 16, callback);
                    mIPosPrinterService.printSpecifiedTypeText("Lietuviška (lietuvių kalba) yra lietuvių kalba, kuri yra oficiali Lietuvos kalba, kurią naudoja 4 milijonai lietuvių. Lietuvių kalba yra viena iš dviejų esamų baltų kalbų (kita - latvių). Baltijos kalbų šeima yra indoeuropiečių kalbos šeimos dalis. Tai indoeuropiečių kalbos šeimos rytinė šaka ir baltųjų kalbų šeima. Tai viena iš dviejų likusių kalbų šeimos kalbų (kita - latvių).\n", "ST", 24, callback);
                    mIPosPrinterService.printSpecifiedTypeText("Lietuviška (lietuvių kalba) yra lietuvių kalba, kuri yra oficiali Lietuvos kalba, kurią naudoja 4 milijonai lietuvių. Lietuvių kalba yra viena iš dviejų esamų baltų kalbų (kita - latvių). Baltijos kalbų šeima yra indoeuropiečių kalbos šeimos dalis. Tai indoeuropiečių kalbos šeimos rytinė šaka ir baltųjų kalbų šeima. Tai viena iš dviejų likusių kalbų šeimos kalbų (kita - latvių).\n", "ST", 32, callback);
                    mIPosPrinterService.printSpecifiedTypeText("Lietuviška (lietuvių kalba) yra lietuvių kalba, kuri yra oficiali Lietuvos kalba, kurią naudoja 4 milijonai lietuvių. Lietuvių kalba yra viena iš dviejų esamų baltų kalbų (kita - latvių). Baltijos kalbų šeima yra indoeuropiečių kalbos šeimos dalis. Tai indoeuropiečių kalbos šeimos rytinė šaka ir baltųjų kalbų šeima. Tai viena iš dviejų likusių kalbų šeimos kalbų (kita - latvių).\n", "ST", 48, callback);
                    mIPosPrinterService.printerPerformPrint(160, callback);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void printHungary() {
        ThreadPoolManager.getInstance().executeTask(new Runnable() {
            @Override
            public void run() {
                try {
                    mIPosPrinterService.printSpecifiedTypeText("Magyarország hivatalos nyelve több mint 14 millió lakosot használ, melyből Magyarországon 9 millió, a maradék több mint 4 millió roma, szlovák, szerb, ukrán, osztrák és más országban szétszórva.\n", "ST", 16, callback);
                    mIPosPrinterService.printSpecifiedTypeText("Magyarország hivatalos nyelve több mint 14 millió lakosot használ, melyből Magyarországon 9 millió, a maradék több mint 4 millió roma, szlovák, szerb, ukrán, osztrák és más országban szétszórva.\n", "ST", 24, callback);
                    mIPosPrinterService.printSpecifiedTypeText("Magyarország hivatalos nyelve több mint 14 millió lakosot használ, melyből Magyarországon 9 millió, a maradék több mint 4 millió roma, szlovák, szerb, ukrán, osztrák és más országban szétszórva.\n", "ST", 32, callback);
                    mIPosPrinterService.printSpecifiedTypeText("Magyarország hivatalos nyelve több mint 14 millió lakosot használ, melyből Magyarországon 9 millió, a maradék több mint 4 millió roma, szlovák, szerb, ukrán, osztrák és más országban szétszórva.\n", "ST", 48, callback);
                    mIPosPrinterService.printerPerformPrint(160, callback);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void printDutch() {
        ThreadPoolManager.getInstance().executeTask(new Runnable() {
            @Override
            public void run() {
                try {
                    mIPosPrinterService.printSpecifiedTypeText("Nederlands is de officiële taal van Nederland en een van de officiële talen van België en behoort tot de \"Indo-Europees-Germaans-West-Duitse tak\". Gebruikers zijn voornamelijk gevestigd in Nederland, België, Zuid-Afrika, Suriname, de Caribische Antillen en andere plaatsen.\n", "ST", 16, callback);
                    mIPosPrinterService.printSpecifiedTypeText("Nederlands is de officiële taal van Nederland en een van de officiële talen van België en behoort tot de \"Indo-Europees-Germaans-West-Duitse tak\". Gebruikers zijn voornamelijk gevestigd in Nederland, België, Zuid-Afrika, Suriname, de Caribische Antillen en andere plaatsen.\n", "ST", 24, callback);
                    mIPosPrinterService.printSpecifiedTypeText("Nederlands is de officiële taal van Nederland en een van de officiële talen van België en behoort tot de \"Indo-Europees-Germaans-West-Duitse tak\". Gebruikers zijn voornamelijk gevestigd in Nederland, België, Zuid-Afrika, Suriname, de Caribische Antillen en andere plaatsen.\n", "ST", 32, callback);
                    mIPosPrinterService.printSpecifiedTypeText("Nederlands is de officiële taal van Nederland en een van de officiële talen van België en behoort tot de \"Indo-Europees-Germaans-West-Duitse tak\". Gebruikers zijn voornamelijk gevestigd in Nederland, België, Zuid-Afrika, Suriname, de Caribische Antillen en andere plaatsen.\n", "ST", 48, callback);
                    mIPosPrinterService.printerPerformPrint(160, callback);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void printNorway() {
        ThreadPoolManager.getInstance().executeTask(new Runnable() {
            @Override
            public void run() {
                try {
                    mIPosPrinterService.printSpecifiedTypeText("Norsk (Norsk) er norsk offisielle språk. I tillegg til befolkningen på 4,2 millioner i landet, er det rundt 600.000 nordmenn som har flyttet til USA. Etter en lang periode med endring på norsk, ble det veldig lik svensk og dansk, og folk på disse tre språkene kunne kommunisere med hverandre.\n", "ST", 16, callback);
                    mIPosPrinterService.printSpecifiedTypeText("Norsk (Norsk) er norsk offisielle språk. I tillegg til befolkningen på 4,2 millioner i landet, er det rundt 600.000 nordmenn som har flyttet til USA. Etter en lang periode med endring på norsk, ble det veldig lik svensk og dansk, og folk på disse tre språkene kunne kommunisere med hverandre.\n", "ST", 24, callback);
                    mIPosPrinterService.printSpecifiedTypeText("Norsk (Norsk) er norsk offisielle språk. I tillegg til befolkningen på 4,2 millioner i landet, er det rundt 600.000 nordmenn som har flyttet til USA. Etter en lang periode med endring på norsk, ble det veldig lik svensk og dansk, og folk på disse tre språkene kunne kommunisere med hverandre.\n", "ST", 32, callback);
                    mIPosPrinterService.printSpecifiedTypeText("Norsk (Norsk) er norsk offisielle språk. I tillegg til befolkningen på 4,2 millioner i landet, er det rundt 600.000 nordmenn som har flyttet til USA. Etter en lang periode med endring på norsk, ble det veldig lik svensk og dansk, og folk på disse tre språkene kunne kommunisere med hverandre.\n", "ST", 48, callback);
                    mIPosPrinterService.printerPerformPrint(160, callback);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void printpoland() {
        ThreadPoolManager.getInstance().executeTask(new Runnable() {
            @Override
            public void run() {
                try {
                    mIPosPrinterService.printSpecifiedTypeText("Polski jest językiem Polaków. Populacja wynosi około 48 milionów, z czego około 38 milionów znajduje się w Polsce, a około 10 milionów jest w innych krajach. W początkach Polski przyjmowano języki łacińskie i czeskie, a teksty pisane oficjalnie powstawały dopiero w XIV wieku.\n", "ST", 16, callback);
                    mIPosPrinterService.printSpecifiedTypeText("Polski jest językiem Polaków. Populacja wynosi około 48 milionów, z czego około 38 milionów znajduje się w Polsce, a około 10 milionów jest w innych krajach. W początkach Polski przyjmowano języki łacińskie i czeskie, a teksty pisane oficjalnie powstawały dopiero w XIV wieku.\n", "ST", 24, callback);
                    mIPosPrinterService.printSpecifiedTypeText("Polski jest językiem Polaków. Populacja wynosi około 48 milionów, z czego około 38 milionów znajduje się w Polsce, a około 10 milionów jest w innych krajach. W początkach Polski przyjmowano języki łacińskie i czeskie, a teksty pisane oficjalnie powstawały dopiero w XIV wieku.\n", "ST", 32, callback);
                    mIPosPrinterService.printSpecifiedTypeText("Polski jest językiem Polaków. Populacja wynosi około 48 milionów, z czego około 38 milionów znajduje się w Polsce, a około 10 milionów jest w innych krajach. W początkach Polski przyjmowano języki łacińskie i czeskie, a teksty pisane oficjalnie powstawały dopiero w XIV wieku.\n", "ST", 48, callback);
                    mIPosPrinterService.printerPerformPrint(160, callback);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void printPortugal() {
        ThreadPoolManager.getInstance().executeTask(new Runnable() {
            @Override
            public void run() {
                try {
                    mIPosPrinterService.printSpecifiedTypeText("Português (Português: Português) é abreviado como Português, pertencente ao ramo indo-europeu-romeno-ocidental Romance. O português é uma das poucas línguas amplamente distribuídas no mundo e a quinta (ou sexta) maior língua do mundo.\n", "ST", 16, callback);
                    mIPosPrinterService.printSpecifiedTypeText("Português (Português: Português) é abreviado como Português, pertencente ao ramo indo-europeu-romeno-ocidental Romance. O português é uma das poucas línguas amplamente distribuídas no mundo e a quinta (ou sexta) maior língua do mundo.\n", "ST", 24, callback);
                    mIPosPrinterService.printSpecifiedTypeText("Português (Português: Português) é abreviado como Português, pertencente ao ramo indo-europeu-romeno-ocidental Romance. O português é uma das poucas línguas amplamente distribuídas no mundo e a quinta (ou sexta) maior língua do mundo.\n", "ST", 32, callback);
                    mIPosPrinterService.printSpecifiedTypeText("Português (Português: Português) é abreviado como Português, pertencente ao ramo indo-europeu-romeno-ocidental Romance. O português é uma das poucas línguas amplamente distribuídas no mundo e a quinta (ou sexta) maior língua do mundo.\n", "ST", 48, callback);
                    mIPosPrinterService.printerPerformPrint(160, callback);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void printRomania() {
        ThreadPoolManager.getInstance().executeTask(new Runnable() {
            @Override
            public void run() {
                try {
                    mIPosPrinterService.printSpecifiedTypeText("Română (limba română). Limba oficială a României, cu o populație globală de aproximativ 26 de milioane, este concentrată în Balcani din Europa. În Moldova, limba română este numită moldovenească. În plus, există și un număr mare de utilizatori în Ucraina, Bulgaria, Serbia, Ungaria, Albania, Grecia și Statele Unite.\n", "ST", 16, callback);
                    mIPosPrinterService.printSpecifiedTypeText("Română (limba română). Limba oficială a României, cu o populație globală de aproximativ 26 de milioane, este concentrată în Balcani din Europa. În Moldova, limba română este numită moldovenească. În plus, există și un număr mare de utilizatori în Ucraina, Bulgaria, Serbia, Ungaria, Albania, Grecia și Statele Unite.\n", "ST", 24, callback);
                    mIPosPrinterService.printSpecifiedTypeText("Română (limba română). Limba oficială a României, cu o populație globală de aproximativ 26 de milioane, este concentrată în Balcani din Europa. În Moldova, limba română este numită moldovenească. În plus, există și un număr mare de utilizatori în Ucraina, Bulgaria, Serbia, Ungaria, Albania, Grecia și Statele Unite.\n", "ST", 32, callback);
                    mIPosPrinterService.printSpecifiedTypeText("Română (limba română). Limba oficială a României, cu o populație globală de aproximativ 26 de milioane, este concentrată în Balcani din Europa. În Moldova, limba română este numită moldovenească. În plus, există și un număr mare de utilizatori în Ucraina, Bulgaria, Serbia, Ungaria, Albania, Grecia și Statele Unite.\n", "ST", 48, callback);
                    mIPosPrinterService.printerPerformPrint(160, callback);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void printSlovakia() {
        ThreadPoolManager.getInstance().executeTask(new Runnable() {
            @Override
            public void run() {
                try {
                    mIPosPrinterService.printSpecifiedTypeText("Slovaško republiko (slovaško: Slovenská republika), ki se imenuje Slovaška, meji na Češko na severozahodu, na Poljskem na severu, Ukrajini na vzhodu, na Madžarskem na jugu in Avstriji na jugozahodu. Je neobalna država v Srednji Evropi.\n", "ST", 16, callback);
                    mIPosPrinterService.printSpecifiedTypeText("Slovaško republiko (slovaško: Slovenská republika), ki se imenuje Slovaška, meji na Češko na severozahodu, na Poljskem na severu, Ukrajini na vzhodu, na Madžarskem na jugu in Avstriji na jugozahodu. Je neobalna država v Srednji Evropi.\n", "ST", 24, callback);
                    mIPosPrinterService.printSpecifiedTypeText("Slovaško republiko (slovaško: Slovenská republika), ki se imenuje Slovaška, meji na Češko na severozahodu, na Poljskem na severu, Ukrajini na vzhodu, na Madžarskem na jugu in Avstriji na jugozahodu. Je neobalna država v Srednji Evropi.\n", "ST", 32, callback);
                    mIPosPrinterService.printSpecifiedTypeText("Slovaško republiko (slovaško: Slovenská republika), ki se imenuje Slovaška, meji na Češko na severozahodu, na Poljskem na severu, Ukrajini na vzhodu, na Madžarskem na jugu in Avstriji na jugozahodu. Je neobalna država v Srednji Evropi.\n", "ST", 48, callback);
                    mIPosPrinterService.printerPerformPrint(160, callback);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void printSlovenia() {
        ThreadPoolManager.getInstance().executeTask(new Runnable() {
            @Override
            public void run() {
                try {
                    mIPosPrinterService.printSpecifiedTypeText("Slovenski jezik pripada indoevropskim in slovanskim jezikom in se razdeli v Sloveniji, pa tudi na Madžarskem, v Avstriji in Italiji, s približno 2 milijoni prebivalcev. Ko je eden izmed treh uradnih jezikov nekdanje Jugoslavije (drugi dve srbsko-hrvaški in makedonski), je zdaj eden od 24 uradnih jezikov Evropske unije.\n", "ST", 16, callback);
                    mIPosPrinterService.printSpecifiedTypeText("Slovenski jezik pripada indoevropskim in slovanskim jezikom in se razdeli v Sloveniji, pa tudi na Madžarskem, v Avstriji in Italiji, s približno 2 milijoni prebivalcev. Ko je eden izmed treh uradnih jezikov nekdanje Jugoslavije (drugi dve srbsko-hrvaški in makedonski), je zdaj eden od 24 uradnih jezikov Evropske unije.\n", "ST", 24, callback);
                    mIPosPrinterService.printSpecifiedTypeText("Slovenski jezik pripada indoevropskim in slovanskim jezikom in se razdeli v Sloveniji, pa tudi na Madžarskem, v Avstriji in Italiji, s približno 2 milijoni prebivalcev. Ko je eden izmed treh uradnih jezikov nekdanje Jugoslavije (drugi dve srbsko-hrvaški in makedonski), je zdaj eden od 24 uradnih jezikov Evropske unije.\n", "ST", 32, callback);
                    mIPosPrinterService.printSpecifiedTypeText("Slovenski jezik pripada indoevropskim in slovanskim jezikom in se razdeli v Sloveniji, pa tudi na Madžarskem, v Avstriji in Italiji, s približno 2 milijoni prebivalcev. Ko je eden izmed treh uradnih jezikov nekdanje Jugoslavije (drugi dve srbsko-hrvaški in makedonski), je zdaj eden od 24 uradnih jezikov Evropske unije.\n", "ST", 48, callback);
                    mIPosPrinterService.printerPerformPrint(160, callback);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void printFinland() {
        ThreadPoolManager.getInstance().executeTask(new Runnable() {
            @Override
            public void run() {
                try {
                    mIPosPrinterService.printSpecifiedTypeText("Suomi (Suomi) on suomenkielinen, ja useimmat (92%) kansalliset kielet käyttävät myös ulkomaalaisia suomalaisia. Se on yksi Suomen kahdesta virallisesta kielestä ja Ruotsin laillisesta vähemmistökielestä.\n", "ST", 16, callback);
                    mIPosPrinterService.printSpecifiedTypeText("Suomi (Suomi) on suomenkielinen, ja useimmat (92%) kansalliset kielet käyttävät myös ulkomaalaisia suomalaisia. Se on yksi Suomen kahdesta virallisesta kielestä ja Ruotsin laillisesta vähemmistökielestä.\n", "ST", 24, callback);
                    mIPosPrinterService.printSpecifiedTypeText("Suomi (Suomi) on suomenkielinen, ja useimmat (92%) kansalliset kielet käyttävät myös ulkomaalaisia suomalaisia. Se on yksi Suomen kahdesta virallisesta kielestä ja Ruotsin laillisesta vähemmistökielestä.\n", "ST", 32, callback);
                    mIPosPrinterService.printSpecifiedTypeText("Suomi (Suomi) on suomenkielinen, ja useimmat (92%) kansalliset kielet käyttävät myös ulkomaalaisia suomalaisia. Se on yksi Suomen kahdesta virallisesta kielestä ja Ruotsin laillisesta vähemmistökielestä.\n", "ST", 48, callback);
                    mIPosPrinterService.printerPerformPrint(160, callback);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void printSweden() {
        ThreadPoolManager.getInstance().executeTask(new Runnable() {
            @Override
            public void run() {
                try {
                    mIPosPrinterService.printSpecifiedTypeText("Svenska, som huvudsakligen används i Sverige, Finland (särskilt på Öland), med mer än 9 miljoner människor. Det är samma språk som de övriga två språken i Skandinavien - danska och norska.\n", "ST", 16, callback);
                    mIPosPrinterService.printSpecifiedTypeText("Svenska, som huvudsakligen används i Sverige, Finland (särskilt på Öland), med mer än 9 miljoner människor. Det är samma språk som de övriga två språken i Skandinavien - danska och norska.\n", "ST", 24, callback);
                    mIPosPrinterService.printSpecifiedTypeText("Svenska, som huvudsakligen används i Sverige, Finland (särskilt på Öland), med mer än 9 miljoner människor. Det är samma språk som de övriga två språken i Skandinavien - danska och norska.\n", "ST", 32, callback);
                    mIPosPrinterService.printSpecifiedTypeText("Svenska, som huvudsakligen används i Sverige, Finland (särskilt på Öland), med mer än 9 miljoner människor. Det är samma språk som de övriga två språken i Skandinavien - danska och norska.\n", "ST", 48, callback);
                    mIPosPrinterService.printerPerformPrint(160, callback);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void printVietnam() {
        ThreadPoolManager.getInstance().executeTask(new Runnable() {
            @Override
            public void run() {
                try {
                    mIPosPrinterService.printSpecifiedTypeText("Tiếng Việt, ngôn ngữ chính thức của Việt Nam, sử dụng 94,6 triệu người, thuộc về nhóm ngôn ngữ Nam Á - nhóm ngôn ngữ Việt - chi nhánh tiếng Việt, có liên quan đến tiếng Khmer, được đánh dấu bằng các ký tự Trung Quốc trong lịch sử và kết hợp từ vựng tiếng Trung. Khoảng 10.000 người Jing ở thành phố Dongxing, Trung Quốc sử dụng tiếng Việt.\n", "ST", 16, callback);
                    mIPosPrinterService.printSpecifiedTypeText("Tiếng Việt, ngôn ngữ chính thức của Việt Nam, sử dụng 94,6 triệu người, thuộc về nhóm ngôn ngữ Nam Á - nhóm ngôn ngữ Việt - chi nhánh tiếng Việt, có liên quan đến tiếng Khmer, được đánh dấu bằng các ký tự Trung Quốc trong lịch sử và kết hợp từ vựng tiếng Trung. Khoảng 10.000 người Jing ở thành phố Dongxing, Trung Quốc sử dụng tiếng Việt.\n", "ST", 24, callback);
                    mIPosPrinterService.printSpecifiedTypeText("Tiếng Việt, ngôn ngữ chính thức của Việt Nam, sử dụng 94,6 triệu người, thuộc về nhóm ngôn ngữ Nam Á - nhóm ngôn ngữ Việt - chi nhánh tiếng Việt, có liên quan đến tiếng Khmer, được đánh dấu bằng các ký tự Trung Quốc trong lịch sử và kết hợp từ vựng tiếng Trung. Khoảng 10.000 người Jing ở thành phố Dongxing, Trung Quốc sử dụng tiếng Việt.\n", "ST", 32, callback);
                    mIPosPrinterService.printSpecifiedTypeText("Tiếng Việt, ngôn ngữ chính thức của Việt Nam, sử dụng 94,6 triệu người, thuộc về nhóm ngôn ngữ Nam Á - nhóm ngôn ngữ Việt - chi nhánh tiếng Việt, có liên quan đến tiếng Khmer, được đánh dấu bằng các ký tự Trung Quốc trong lịch sử và kết hợp từ vựng tiếng Trung. Khoảng 10.000 người Jing ở thành phố Dongxing, Trung Quốc sử dụng tiếng Việt.\n", "ST", 48, callback);
                    mIPosPrinterService.printerPerformPrint(160, callback);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void printTurkey() {
        ThreadPoolManager.getInstance().executeTask(new Runnable() {
            @Override
            public void run() {
                try {
                    mIPosPrinterService.printSpecifiedTypeText("Türklerin dili olan Türkçe (Türkçe, Türk dili), özellikle Türkiye'de kullanılan 65 ila 73 milyon kişinin kullandığı ve Azerbaycan, Kıbrıs, Yunanistan, Makedonya, Romanya ve Batı Avrupa'da yaşayan milyonlarca Türk göçmen (esas olarak Almanya'da yoğunlaşmıştır). Türkçenin dikkat çekici bir özelliği, ünlü uyumu ve çok sayıda eki.\n", "ST", 16, callback);
                    mIPosPrinterService.printSpecifiedTypeText("Türklerin dili olan Türkçe (Türkçe, Türk dili), özellikle Türkiye'de kullanılan 65 ila 73 milyon kişinin kullandığı ve Azerbaycan, Kıbrıs, Yunanistan, Makedonya, Romanya ve Batı Avrupa'da yaşayan milyonlarca Türk göçmen (esas olarak Almanya'da yoğunlaşmıştır). Türkçenin dikkat çekici bir özelliği, ünlü uyumu ve çok sayıda eki.\n", "ST", 24, callback);
                    mIPosPrinterService.printSpecifiedTypeText("Türklerin dili olan Türkçe (Türkçe, Türk dili), özellikle Türkiye'de kullanılan 65 ila 73 milyon kişinin kullandığı ve Azerbaycan, Kıbrıs, Yunanistan, Makedonya, Romanya ve Batı Avrupa'da yaşayan milyonlarca Türk göçmen (esas olarak Almanya'da yoğunlaşmıştır). Türkçenin dikkat çekici bir özelliği, ünlü uyumu ve çok sayıda eki.\n", "ST", 32, callback);
                    mIPosPrinterService.printSpecifiedTypeText("Türklerin dili olan Türkçe (Türkçe, Türk dili), özellikle Türkiye'de kullanılan 65 ila 73 milyon kişinin kullandığı ve Azerbaycan, Kıbrıs, Yunanistan, Makedonya, Romanya ve Batı Avrupa'da yaşayan milyonlarca Türk göçmen (esas olarak Almanya'da yoğunlaşmıştır). Türkçenin dikkat çekici bir özelliği, ünlü uyumu ve çok sayıda eki.\n", "ST", 48, callback);
                    mIPosPrinterService.printerPerformPrint(160, callback);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void printGreek() {
        ThreadPoolManager.getInstance().executeTask(new Runnable() {
            @Override
            public void run() {
                try {
                    mIPosPrinterService.printSpecifiedTypeText("Ελληνική, ελληνική γλώσσα, που ανήκει στην ινδοευρωπαϊκή ελληνική γλώσσα, χρησιμοποιείται ευρέως στην Ελλάδα, την Αλβανία, την Κύπρο και άλλες χώρες, καθώς και σε ορισμένες περιοχές της Τουρκίας. Το αρχικό ελληνικό αλφάβητο με 26 γράμματα εξελίχθηκε σταδιακά μετά την εποχή του Ομήρου και αποφασίστηκε να είναι 24, το οποίο έχει χρησιμοποιηθεί στη νεοελληνική γλώσσα.", "ST", 16, callback);
                    mIPosPrinterService.printSpecifiedTypeText("Ελληνική, ελληνική γλώσσα, που ανήκει στην ινδοευρωπαϊκή ελληνική γλώσσα, χρησιμοποιείται ευρέως στην Ελλάδα, την Αλβανία, την Κύπρο και άλλες χώρες, καθώς και σε ορισμένες περιοχές της Τουρκίας. Το αρχικό ελληνικό αλφάβητο με 26 γράμματα εξελίχθηκε σταδιακά μετά την εποχή του Ομήρου και αποφασίστηκε να είναι 24, το οποίο έχει χρησιμοποιηθεί στη νεοελληνική γλώσσα.", "ST", 24, callback);
                    mIPosPrinterService.printSpecifiedTypeText("Ελληνική, ελληνική γλώσσα, που ανήκει στην ινδοευρωπαϊκή ελληνική γλώσσα, χρησιμοποιείται ευρέως στην Ελλάδα, την Αλβανία, την Κύπρο και άλλες χώρες, καθώς και σε ορισμένες περιοχές της Τουρκίας. Το αρχικό ελληνικό αλφάβητο με 26 γράμματα εξελίχθηκε σταδιακά μετά την εποχή του Ομήρου και αποφασίστηκε να είναι 24, το οποίο έχει χρησιμοποιηθεί στη νεοελληνική γλώσσα.", "ST", 32, callback);
                    mIPosPrinterService.printSpecifiedTypeText("Ελληνική, ελληνική γλώσσα, που ανήκει στην ινδοευρωπαϊκή ελληνική γλώσσα, χρησιμοποιείται ευρέως στην Ελλάδα, την Αλβανία, την Κύπρο και άλλες χώρες, καθώς και σε ορισμένες περιοχές της Τουρκίας. Το αρχικό ελληνικό αλφάβητο με 26 γράμματα εξελίχθηκε σταδιακά μετά την εποχή του Ομήρου και αποφασίστηκε να είναι 24, το οποίο έχει χρησιμοποιηθεί στη νεοελληνική γλώσσα.", "ST", 48, callback);
                    mIPosPrinterService.printerPerformPrint(160, callback);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void printBulgaria() {
        ThreadPoolManager.getInstance().executeTask(new Runnable() {
            @Override
            public void run() {
                try {
                    mIPosPrinterService.printSpecifiedTypeText("Български език, Български език. English Bulgarian принадлежи към индоевропейско-славянската езикова група - югославски клон - югоизточен славянски подотрасъл. Това е официалният език на България.\n", "ST", 16, callback);
                    mIPosPrinterService.printSpecifiedTypeText("Български език, Български език. English Bulgarian принадлежи към индоевропейско-славянската езикова група - югославски клон - югоизточен славянски подотрасъл. Това е официалният език на България.\n", "ST", 24, callback);
                    mIPosPrinterService.printSpecifiedTypeText("Български език, Български език. English Bulgarian принадлежи към индоевропейско-славянската езикова група - югославски клон - югоизточен славянски подотрасъл. Това е официалният език на България.\n", "ST", 32, callback);
                    mIPosPrinterService.printSpecifiedTypeText("Български език, Български език. English Bulgarian принадлежи към индоевропейско-славянската езикова група - югославски клон - югоизточен славянски подотрасъл. Това е официалният език на България.\n", "ST", 48, callback);
                    mIPosPrinterService.printerPerformPrint(160, callback);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void printKazakh() {
        ThreadPoolManager.getInstance().executeTask(new Runnable() {
            @Override
            public void run() {
                try {
                    mIPosPrinterService.printSpecifiedTypeText("Алтай тіліндегі отбасылардағы қазақ тілі - қазақ тілі, қазақ тілінің бүкіл әлемде пайдаланатын тілі және басқа түркі тілдерінің ұлттық тіліне өте жақын.\n", "ST", 16, callback);
                    mIPosPrinterService.printSpecifiedTypeText("Алтай тіліндегі отбасылардағы қазақ тілі - қазақ тілі, қазақ тілінің бүкіл әлемде пайдаланатын тілі және басқа түркі тілдерінің ұлттық тіліне өте жақын.\n", "ST", 24, callback);
                    mIPosPrinterService.printSpecifiedTypeText("Алтай тіліндегі отбасылардағы қазақ тілі - қазақ тілі, қазақ тілінің бүкіл әлемде пайдаланатын тілі және басқа түркі тілдерінің ұлттық тіліне өте жақын.\n", "ST", 32, callback);
                    mIPosPrinterService.printSpecifiedTypeText("Алтай тіліндегі отбасылардағы қазақ тілі - қазақ тілі, қазақ тілінің бүкіл әлемде пайдаланатын тілі және басқа түркі тілдерінің ұлттық тіліне өте жақын.\n", "ST", 48, callback);
                    mIPosPrinterService.printerPerformPrint(160, callback);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void printRussian() {
        ThreadPoolManager.getInstance().executeTask(new Runnable() {
            @Override
            public void run() {
                try {
                    mIPosPrinterService.printSpecifiedTypeText("Официальный язык Российской Федерации также является четвертым по величине языком в мире по числу носителей языка и пользователей второго языка. Число людей, использующих российские счета, составляет 5,7% населения мира. Русский относится к восточнославянскому отделению семьи славянских языков в индоевропейской языковой семье.\n", "ST", 16, callback);
                    mIPosPrinterService.printSpecifiedTypeText("Официальный язык Российской Федерации также является четвертым по величине языком в мире по числу носителей языка и пользователей второго языка. Число людей, использующих российские счета, составляет 5,7% населения мира. Русский относится к восточнославянскому отделению семьи славянских языков в индоевропейской языковой семье.\n", "ST", 24, callback);
                    mIPosPrinterService.printSpecifiedTypeText("Официальный язык Российской Федерации также является четвертым по величине языком в мире по числу носителей языка и пользователей второго языка. Число людей, использующих российские счета, составляет 5,7% населения мира. Русский относится к восточнославянскому отделению семьи славянских языков в индоевропейской языковой семье.\n", "ST", 32, callback);
                    mIPosPrinterService.printSpecifiedTypeText("Официальный язык Российской Федерации также является четвертым по величине языком в мире по числу носителей языка и пользователей второго языка. Число людей, использующих российские счета, составляет 5,7% населения мира. Русский относится к восточнославянскому отделению семьи славянских языков в индоевропейской языковой семье.\n", "ST", 48, callback);
                    mIPosPrinterService.printerPerformPrint(160, callback);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void printSerbia() {
        ThreadPoolManager.getInstance().executeTask(new Runnable() {
            @Override
            public void run() {
                try {
                    mIPosPrinterService.printSpecifiedTypeText("Српски и хрватски називани су \"српско-хрватски\" током периода Социјалистичке Федеративне Републике Југославије. Након распада Социјалистичке Федеративне Републике Југославије, \"српско-хрватски\" је постао српски и хрватски самостално.\n", "ST", 16, callback);
                    mIPosPrinterService.printSpecifiedTypeText("Српски и хрватски називани су \"српско-хрватски\" током периода Социјалистичке Федеративне Републике Југославије. Након распада Социјалистичке Федеративне Републике Југославије, \"српско-хрватски\" је постао српски и хрватски самостално.\n", "ST", 24, callback);
                    mIPosPrinterService.printSpecifiedTypeText("Српски и хрватски називани су \"српско-хрватски\" током периода Социјалистичке Федеративне Републике Југославије. Након распада Социјалистичке Федеративне Републике Југославије, \"српско-хрватски\" је постао српски и хрватски самостално.\n", "ST", 32, callback);
                    mIPosPrinterService.printSpecifiedTypeText("Српски и хрватски називани су \"српско-хрватски\" током периода Социјалистичке Федеративне Републике Југославије. Након распада Социјалистичке Федеративне Републике Југославије, \"српско-хрватски\" је постао српски и хрватски самостално.\n", "ST", 48, callback);
                    mIPosPrinterService.printerPerformPrint(160, callback);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }
    public void printUkraine() {
        ThreadPoolManager.getInstance().executeTask(new Runnable() {
            @Override
            public void run() {
                try {
                    mIPosPrinterService.printSpecifiedTypeText("Українська, українська мова. У світі близько 45 мільйонів людей, і в Північній Америці є кілька користувачів, що належать до індоєвропейської слов'янської мови східнослов'янської гілки.\n", "ST", 16, callback);
                    mIPosPrinterService.printSpecifiedTypeText("Українська, українська мова. У світі близько 45 мільйонів людей, і в Північній Америці є кілька користувачів, що належать до індоєвропейської слов'янської мови східнослов'янської гілки.\n", "ST", 24, callback);
                    mIPosPrinterService.printSpecifiedTypeText("Українська, українська мова. У світі близько 45 мільйонів людей, і в Північній Америці є кілька користувачів, що належать до індоєвропейської слов'янської мови східнослов'янської гілки.\n", "ST", 32, callback);
                    mIPosPrinterService.printSpecifiedTypeText("Українська, українська мова. У світі близько 45 мільйонів людей, і в Північній Америці є кілька користувачів, що належать до індоєвропейської слов'янської мови східнослов'янської гілки.\n", "ST", 48, callback);
                    mIPosPrinterService.printerPerformPrint(160, callback);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void printThai() {
        ThreadPoolManager.getInstance().executeTask(new Runnable() {
            @Override
            public void run() {
                try {
                    mIPosPrinterService.printSpecifiedTypeText("ชาวไทยส่วนใหญ่ใช้ในประเทศไทยลาวพม่าตะวันตกเฉียงเหนือเวียดนามตะวันตกเฉียงเหนือของกัมพูชาทางตะวันตกเฉียงใต้ของจีนและตะวันออกเฉียงเหนือของอินเดีย\n", "ST", 16, callback);
                    mIPosPrinterService.printSpecifiedTypeText("ชาวไทยส่วนใหญ่ใช้ในประเทศไทยลาวพม่าตะวันตกเฉียงเหนือเวียดนามตะวันตกเฉียงเหนือของกัมพูชาทางตะวันตกเฉียงใต้ของจีนและตะวันออกเฉียงเหนือของอินเดีย\n", "ST", 24, callback);
                    mIPosPrinterService.printSpecifiedTypeText("ชาวไทยส่วนใหญ่ใช้ในประเทศไทยลาวพม่าตะวันตกเฉียงเหนือเวียดนามตะวันตกเฉียงเหนือของกัมพูชาทางตะวันตกเฉียงใต้ของจีนและตะวันออกเฉียงเหนือของอินเดีย\n", "ST", 32, callback);
                    mIPosPrinterService.printSpecifiedTypeText("ชาวไทยส่วนใหญ่ใช้ในประเทศไทยลาวพม่าตะวันตกเฉียงเหนือเวียดนามตะวันตกเฉียงเหนือของกัมพูชาทางตะวันตกเฉียงใต้ของจีนและตะวันออกเฉียงเหนือของอินเดีย\n", "ST", 48, callback);
                    mIPosPrinterService.printerPerformPrint(160, callback);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("IPosPrinterTestDemo Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        AppIndex.AppIndexApi.start(client, getIndexApiAction());
    }
}
