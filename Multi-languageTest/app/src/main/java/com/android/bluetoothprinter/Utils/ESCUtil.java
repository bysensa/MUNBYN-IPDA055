package com.android.bluetoothprinter.Utils;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

import android.graphics.Bitmap;
import android.nfc.Tag;
import android.util.Log;

public class ESCUtil{
    public static final String TAG = "ESCUtil";
    public static final byte ESC = 27; // 换码
    public static final byte FS = 28;  //  文本分隔符
    public static final byte GS = 29; //  组分隔符
    public static final byte DLE = 16; // 数据连接换码
    public static final byte EOT = 4;    //传输结束
    public static final byte ENQ = 5;    //询问字符
    public static final byte ACK = 6;    //收到通知
    public static final byte SP = 32;     //空格
    public static final byte HT = 9;     //横向跳格
    public static final byte LF = 10;     //打印并换行（水平定位）
    public static final byte CR = 13;     //归位（回车）
    public static final byte FF = 12;     //走纸控制
    public static final byte CAN = 24;    //取消打印

    /**
     *获取打印机状态
     */
    public static byte[] getPrinterStatus()
    {
        byte[] result = new byte[3];
        result[0] = ENQ;
        result[1] = 0x11;
        result[2] = 0x00;
        return result;
    }

    /**
     * 打印机初始化
     *@return
     */
    public static byte[] init_printer()
    {
        byte[] result = new byte[2];
        result[0] = ESC;
        result[1] = 64;
        return result;
    }

    /**
     *换行
     * @param lineNum 换行数
     * @return
     */
    public static byte[] nextLines(int lineNum)
    {
        byte[] result = new byte[lineNum];
        for (int i = 0;i < lineNum;i++)
        {
            result[i] = LF;
        }
        return result;
    }

    /**
     *跳格
     * @return
     */
    public static byte[] HTCmd()
    {
        byte[] result = new byte[]{HT};
        return result;
    }

    /**
     * 设置字符右间距单位像素点，为8的整数倍
     * @return
     */
    public static byte[] setRightSpaceChar(byte n)
    {
        byte[] result = new byte[3];
        result[0] = ESC;
        result[1] = SP;
        result[2] = n;
        return result;
    }

    /**
     * 打印下划线模式设置
     *@return
     */
    public static byte[] printUnderlineModeEn(boolean en){
        byte[] result = new byte[3];
        result[0] = ESC;
        result[1] = 33;
        if(en)
            result[2] = (byte) 0x80;
        else
            result[2] =  0x00;
        return result;
    }

    /**
     * 设置绝对打印位置
     *@param n 单位像素点 为8的整数倍
     *@return
     */
    public static byte[]  absolutePrintPosition(int n)
    {
        byte[] result = new byte[4];
        result[0] = ESC;
        result[1] = 36;
        result[2] = (byte)(n%256);
        result[3] = (byte)(n/256);
        return result;
    }

    /**
     * 打印位图数据
     */
    public static byte[] printBmpData()
    {
        byte[] result = new byte[4];
        return result;
    }

    /**
     * 下划线设置
     * @param n  0, 1 , 2或者 48, 49, 50
     * @return
     */
    public static byte[] underlineWithWidthOn(byte n)
    {
        byte[] result = new byte[3];
        result[0] = ESC;
        result[1] = 45;
        result[2] = n;
        return result;
    }

    /**
     * 设置默认行高
     */
    public static byte[] defaultLineHeight()
    {
        byte[] result = new byte[2];
        result[0] = ESC;
        result[1] = 50;
        return result;
    }


    /**
     * 设置行高
     * @param n 行高像素点
     * @return
     */
    public static byte[] setLineHeight(byte n)
    {
        byte[] result = new byte[3];
        result[0] = ESC;
        result[1] = 51;
        result[2] = n;
        return result;
    }
    /**
     * 水平方向右移n列
     * @param cols 单位字符宽度（包含右间距）
     * @return
     */
    public static byte[] set_HT_position(byte[] cols) {
        byte[] result = new byte[6];
        result[0] = ESC;
        result[1] = 68;
        result[2] = cols[0];
        result[3] = cols[1];
        result[4] = cols[2];
        result[5] = 0;
        return result;
    }

    /**
     * 倍宽，倍高模式
     * @return
     */
    public static byte[] widthAndheightMode(byte mode)
    {
        byte[] result = new byte[3];
        result[0] = ESC;
        result[1] = 33;
        result[2] = (byte)(mode & 0x30);
        return result;
    }

    /**
     * 加粗模式  暂不支持
     * @return
     */
    public static byte[] boldOnOff(byte mode)
    {
        byte[] result = new byte[3];
        result[0] = ESC;
        result[1] = 69;
        result[2] = mode;
        return result;
    }

    /**
     * 打印并走纸n点行
     */
    public static byte[] performPrintAndFeedPaper(byte n)
    {
        byte[] result = new byte[3];
        result[0] = ESC;
        result[1] = 74;
        result[2] = n;
        return result;
    }

    /**
     * 打印并走纸n行 字符行字符高度
     */
    public static byte[] performPrintFeedPaperLines(byte n)
    {
        byte[] result = new byte[3];
        result[0] = ESC;
        result[1] = 100;
        result[2] = n;
        return result;
    }

    /**
     * 设置横向相对打印位置
     * @param n 当前字体大小下ASCII字符个数
    */
    public static byte[]  relativePrintPosition(int n)
    {
        byte[] result = new byte[4];
        result[0] = ESC;
        result[1] = 92;
        result[2] = (byte)(n%256);
        result[3] = (byte)(n/256);
        return result;
    }
    /**
     * 对齐模式
     * @return
     */
    public static byte[] alignMode(byte align) {
        byte[] result = new byte[3];
        result[0] = ESC;
        result[1] = 97;
        result[2] = align;
        return result;
    }


    /**
     * 字体（大小）设置
     * @param size :设置倍宽倍高
     * @return
     */
    public static byte[] fontSizeSet(byte size){

        byte[] result = new byte[3];


        result[0] = GS;
        result[1] = 33;
        result[2] = size;
        return result;
    }

    /**
     * 设置打印左边距
     *@param n 单位像素点 为8的整数倍
     *@return
     */
    public static byte[]  printLeftMargin(int n)
    {
        byte[] result = new byte[4];
        result[0] = GS;
        result[1] = 76;
        result[2] = (byte)(n%256);
        result[3] = (byte)(n/256);
        return result;
    }

    /**
     * 设置打印区域宽度
     *@param n 单位像素点 为8的整数倍
     *@return
     */
    public static byte[]  printAreaWidth(int n)
    {
        byte[] result = new byte[4];
        result[0] = GS;
        result[1] = 87;
        result[2] = (byte)(n%256);
        result[3] = (byte)(n/256);
        return result;
    }

    /**
     * 设置汉字模式
     */
    public static byte[] selectChineseMode()
    {
        byte[] result = new byte[2];
        result[0] = FS;
        result[1] = 38;
        return result;
    }

    /**
     * 取消汉字模式
     */
    public static byte[] CancelChineseMode()
    {
        byte[] result = new byte[2];
        result[0] = FS;
        result[1] = 46;
        return result;
    }

    /**
     * 编码系统选择
     * @param mode 0,48 :系统默认  1，49 ：GBK
     * @return
     */
    public static byte[] selectCharCodeSystem(byte mode) {
        byte[] result = new byte[3];
        result[0] = FS;
        result[1] = 67;
        result[2] = mode;
        return result;
    }

    /**
     * 条码打印
     */
    public static byte[] barcodePrint()
    {
        byte[] result = new byte[]{GS,107};
        return result;
    }

    /**
     * 设置HRI字符的打印位置
     */
    public static byte[] setHRIPosition(int position)
    {
        byte[] result = new byte[3];
        result[0] = GS;
        result[1] = 72;
        result[2] = (byte)position;
        return result;
    }

    /**
     * 设置条码高度
     * @param height :1-16 单位24个像素点
     * @return
     */

    public static byte[] setBarcodeHeight(int height)
    {
        byte[] result = new byte[3];
        result[0] = GS;
        result[1] = 104;
        result[2] = (byte)height;
        return result;
    }

    /**
     * 设置条码宽度
     * @param width ：1-16 单位24个像素点
     * @return
     */
    public static byte[] setBarcodeWidth(int width)
    {
        byte[] result = new byte[3];
        result[0] = GS;
        result[1] = 119;
        result[2] = (byte)width;
        return result;
    }

    /**
     * 生成条码校验码
     */
    public static int barcodeCheckCode(String codedata)
    {
        int sum=0;
        int sum1=0;
        int sum2=0;
        int checkCode=0;
        for(int i= codedata.length()-1;i >= 0;i -= 2)
        {
           sum1 += (codedata.charAt(i) -'0');
        }

        for(int i= codedata.length()-2;i >= 0;i -= 2)
        {
            sum2 += (codedata.charAt(i) -'0');
        }

        sum = sum1*3 + sum2;

        if(sum%10 == 0)
        {
            checkCode = 0;
        }
        else
        {
            checkCode = 10 - (sum%10);
        }
        Log.d(TAG,"checkCode:"+checkCode);
        return checkCode;
    }
    /**
     * 生成条码数据
     */
    public static byte[] barcodeData(int mode,String data)
    {

        String barcodeData = data;
        byte[] modetype= new byte[]{(byte)mode};
        byte[] codePackageFlage = new byte[1];
        int checkCode = 0;
        switch (mode)
        {
            case 0:
            case 1:
            case 2:
            case 3:
                checkCode = barcodeCheckCode(barcodeData);
                Log.d(TAG,"barcodeData:"+barcodeData+"  ***String.valueOf(checkCode):"+String.valueOf(checkCode));
                barcodeData += String.valueOf(checkCode);
            case 4:
            case 5:
            case 6:
                codePackageFlage[0] = 0;
                break;
            case 65:
            case 66:
            case 67:
            case 68:
                checkCode = (byte)barcodeCheckCode(barcodeData);
                barcodeData += String.valueOf(checkCode);
            case 69:
            case 70:
            case 71:
            case 72:
            case 73:
                codePackageFlage[0] = (byte)barcodeData.length();
                break;
            default:
        }
        Log.d(TAG,"modetype:"+modetype[0]+"## barcodeData:"+barcodeData+"  **codePackageFlage:"+codePackageFlage[0]);
        byte[] mBarcodeData = new byte[barcodeData.length()];

        try {
            mBarcodeData = barcodeData.getBytes("gb2312");
        }
        catch (UnsupportedEncodingException e)
        {
            e.printStackTrace();
        }

        if(codePackageFlage[0] == 0)
        {
            byte[][] result = {modetype,mBarcodeData,codePackageFlage};
            return byteMerger(result);
        }
        else
        {
            byte[][] result = {modetype,codePackageFlage,mBarcodeData};
            return byteMerger(result);
        }
    }

    /**
     * 设置二维码大小
     * @param size 1-16 单位 24像素点
     */
    public static byte[] setQRsize(int size)
    {
        byte[] result = new byte[]{GS,40,107,3,0,49,67,0};
        result[7] = (byte)size;
        return result;
    }



    /**
     * 设置二维码纠错等级
     * @param corr
     */
    public static byte[] setQRCorrectionLevel(int corr)
    {
        byte[] result = new byte[]{GS,40,107,3,0,49,69,0};
        result[7] = (byte)corr;
        return result;
    }

    /**
     * 存储二维码数据
     */
    public static byte[] cacheQRData(byte[] mQRData)
    {
        byte[] mQRHeadCode = new byte[]{GS,40,107,0,0,49,80,48};
        int cmdDataLen = mQRData.length + 3;
        mQRHeadCode[3] = (byte)(cmdDataLen%256);
        mQRHeadCode[4] = (byte)(cmdDataLen/256);

        byte[][] cmdData = {mQRHeadCode,mQRData};

        return byteMerger(cmdData);
    }

    /**
     * 打印已存储的二维码（其它缓存数据清空）
     */
    public static byte[] printCacheQRdata()
    {
        byte[] result = new byte[]{GS,40,107,3,0,49,81,48};
        return result;
    }

    /**
     * 位图数据包，包头信息
     */
    public static byte[] bmpCmdHead(int mode, int bitmapWidth)
    {
        byte[] result = new byte[]{ESC,42,0,0,0};
        result[2] = (byte)mode;
        result[3] = (byte)(bitmapWidth%256);
        result[4] = (byte)(bitmapWidth/256);
        return result;
    }

    /**
     * 光栅位图头信息
     */
    public static byte[] rasterBmpHead(int mode, int widthBytes,int height)
    {
        byte[] result = new byte[]{GS,118,48,0,0,0,0,0};
        result[3] = (byte)mode;
        result[4] = (byte)(widthBytes%256);
        result[5] = (byte)(widthBytes/256);
        result[6] = (byte)(height%256);
        result[7] = (byte)(height/256);
        return result;
    }

    /**
     * 整合打印数据
     * @param byteList
     * @return
     */
    public static byte[] byteMerger(byte[][] byteList) {

        int length = 0;
        for (int i = 0; i < byteList.length; i++) {
            length += byteList[i].length;
        }
        byte[] result = new byte[length];

        int index = 0;
        for (int i = 0; i < byteList.length; i++) {
            byte[] nowByte = byteList[i];
            for (int k = 0; k < byteList[i].length; k++) {
                result[index] = nowByte[k];
                index++;
            }
        }
        for (int i = 0; i < index; i++) {
            // CommonUtils.LogWuwei("", "result[" + i + "] is " + result[i]);
        }
        return result;
    }

    public static byte[] generateUserData(){
        try{
            byte[] initPrinter = ESCUtil.init_printer();
            byte[] selectChinese = ESCUtil.selectChineseMode();
            byte[] charCode = ESCUtil.selectCharCodeSystem((byte) 0x01);
            byte[] next2Line = ESCUtil.nextLines(2);
            byte[] lineH1 = ESCUtil.setLineHeight((byte)35);
            byte[] fontSize1 = ESCUtil.fontSizeSet((byte) 0x33);
            byte[] align1 = ESCUtil.alignMode((byte)0x1);
            byte[] title2 = "蓝牙打印机测试\n".getBytes("gb2312");
            byte[] fontSize2 = ESCUtil.fontSizeSet((byte) 0x22);
            byte[] title1 = "Bluetooth Printer test\n".getBytes("gb2312");
            byte[] align2 = ESCUtil.alignMode((byte)0x0);
            byte[] fontSize3 = ESCUtil.fontSizeSet((byte) 0x22);
            byte[] orderSerinum = "1234567890\n".getBytes("gb2312");
            byte[] fontSize4 = ESCUtil.fontSizeSet((byte) 0x11);
            byte[] specialSign= "!@#$%^&*()κρχκμνκλρκνκνμρτυφ".getBytes("gb2312");
            byte[] fontSize5 = ESCUtil.fontSizeSet((byte) 0x11);
            byte[] testSign = "*****************\n-----------------\n".getBytes("gb2312");
            byte[] fontSize6 = ESCUtil.fontSizeSet((byte) 0x33);
            byte[] testInfo = "欢迎使用打印机".getBytes("gb2312");
            byte[] nextLine = ESCUtil.nextLines(1);
            byte[] performPrint = ESCUtil.performPrintAndFeedPaper((byte)200);

            byte[][] cmdBytes = {initPrinter,selectChinese,charCode,lineH1,fontSize1,align1,title2,fontSize2,title1,next2Line,align2,
                                fontSize3,orderSerinum,fontSize4,specialSign,fontSize5,testSign,fontSize6,testInfo,nextLine,performPrint};

            return ESCUtil.byteMerger(cmdBytes);
        }
        catch (UnsupportedEncodingException e)
        {
            e.printStackTrace();
        }

        return null;
    }

}
