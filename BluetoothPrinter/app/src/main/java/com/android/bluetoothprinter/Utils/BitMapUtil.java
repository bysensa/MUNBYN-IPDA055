package com.android.bluetoothprinter.Utils;


import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;

public class BitMapUtil{

    private static final String TAG = "BitMapUtil";
    /**
     * 图片去色,返回灰度图片
     *
     * @param bmpOriginal 传入的图片
     * @return 去色后的图片
     */
    public static Bitmap toGrayscale(Bitmap bmpOriginal, int progress)
    {
        int width, height;
        height = bmpOriginal.getHeight();
        width = bmpOriginal.getWidth();

        Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        Canvas c = new Canvas(bmpGrayscale);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        //设置饱和度
        cm.setSaturation((float) (progress / 100.0));
        // 改变亮度
        int brightness = progress - 127;
        cm.set(new float[]{1, 0, 0, 0, brightness, 0, 1, 0, 0, brightness, 0, 0, 1, 0, brightness, 0, 0, 0, 1, 0});
        //改变对比度
        float contrast = (float) ((progress + 64) / 128.0);
        cm.set(new float[]{contrast, 0, 0, 0, 0, 0, contrast, 0, 0, 0, 0, 0, contrast, 0, 0, 0, 0, 0, 1, 0});
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
        c.drawBitmap(bmpOriginal, 0, 0, paint);
        return bmpGrayscale;
    }
    /**
     * 重画bmp
     */
    private static Bitmap remapSizeAndtoGrayscale(Bitmap bitmapOrg,int imgWidth) {
        // 获取这个图片的宽和高
        int width = bitmapOrg.getWidth();
        int height = bitmapOrg.getHeight();
        float scaleWidth = ((float)imgWidth) / width;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleWidth);
        // 定义预转换成的图片的宽度和高度
       Bitmap targetBmp = toGrayscale(Bitmap.createBitmap(bitmapOrg, 0, 0, width, height, matrix, true),100);
       return targetBmp;
    }

    /**
     *  获取位图点阵数据
     * @param bmp     位图
     * @param width   位图宽 0-384
     * @param bmpMode
     * @return
     */
    public static byte[] getBitmapPrintData(Bitmap bmp , int width, int bmpMode){

        bmp = remapSizeAndtoGrayscale(bmp,width);
        int bmpNewWidth = bmp.getWidth();
        int bmpNewHeight = bmp.getHeight();

        byte[] printBMPPackageHead = ESCUtil.bmpCmdHead(bmpMode,bmpNewWidth);

        int bmpBlockHeight = 0;
        int bmpBlockNums =0;
        if((bmpMode == 0) || (bmpMode ==1))
        {
            bmpBlockHeight = 8;
        }
        else if((bmpMode == 32) || (bmpMode ==33))
        {
            bmpBlockHeight = 24;
        }
        else
        {
            Log.d(TAG,"****bmpMode set error!!*****");
            return (new byte[1]);
        }
        bmpBlockNums = ((bmpNewHeight % bmpBlockHeight) == 0)? (bmpNewHeight/bmpBlockHeight) : (bmpNewHeight/bmpBlockHeight +1);
        int bmpBlockCMDSize = printBMPPackageHead.length + bmpNewWidth*bmpBlockHeight/8;
        byte[] bmpPrintData = new byte[bmpBlockNums*bmpBlockCMDSize];
        for(int n = 0; n < bmpBlockNums; n++)
        {
            byte[] bmpBlockPxBytes = getBitmapBlockData(n,bmpNewWidth,bmpBlockHeight,bmp);

            byte[][] bmpBlockPrintData = {printBMPPackageHead,bmpBlockPxBytes};
            System.arraycopy(ESCUtil.byteMerger(bmpBlockPrintData),0,bmpPrintData,n*bmpBlockCMDSize,bmpBlockCMDSize);
        }

        if (bmp != null && !bmp.isRecycled())
        {
            bmp.recycle();
        }

        return bmpPrintData;
    }

    /**
     * 获取块数据
     */
    public static byte[] getBitmapBlockData(int blocknum,int bmpWidth,int bmpBlockHeight,Bitmap bmp)
    {
        int blockHeightBytes = bmpBlockHeight/8;
        byte[] blockData = new byte[bmpWidth*blockHeightBytes];
        for (int i = 0;i < bmpWidth; i++)
        {
            for(int j = 0;j < blockHeightBytes;j++)
            {
                for(int p = 0; p < 8; p++)
                {
                    byte px = px2Byte(i,blocknum * bmpBlockHeight+j*8+p,bmp);
                    blockData[i*blockHeightBytes+j] |= (px << (7-p));
                }
            }
        }
        return  blockData;
    }

    /**
     * 图片二值化，黑色是1，白色是0
     *
     * @param x   横坐标
     * @param y   纵坐标
     * @param bmp 位图
     * @return
     */
    private static byte px2Byte(int x, int y, Bitmap bmp) {
        if (x < bmp.getWidth() && y < bmp.getHeight()) {
            byte b;
            int pixelColor = bmp.getPixel(x, y);
            if (pixelColor != -1) {
                b = 1;
            } else {
                b = 0;
            }
            return b;
        }
        return 0;
    }

    /**
     * 图片灰度的转化
     */
    private static int RGB2Gray(int r, int g, int b) {
        int gray = (int) (0.29900 * r + 0.58700 * g + 0.11400 * b); // 灰度转化公式
        return gray;
    }

    /**
     * 获取光栅位图数据
     */
    public static byte[] getRasterBmpData(Bitmap bmp, int width, int mode)
    {
        bmp = remapSizeAndtoGrayscale(bmp,width);
        int bmpNewWidth = bmp.getWidth();
        int bmpNewHeight = bmp.getHeight();
        int newWidthBytes = ((bmpNewWidth%8) ==0)? (bmpNewWidth/8):(bmpNewWidth/8 +1);
        byte[] rasterBMPHead = ESCUtil.rasterBmpHead(mode,newWidthBytes,bmpNewHeight);
        byte[] rasterBMPData = new byte[newWidthBytes*bmpNewHeight];

        for (int i = 0; i < bmpNewHeight; i++)
        {
            for (int j = 0; j < newWidthBytes; j++)
            {
                //初始化数据
                byte data = 0x00;
                for (int k = 0; k < 8; k++)
                {
                   int pixelColor = bmp.getPixel((j * 8) + k, i);
                    if (pixelColor != -1)
                    {
                        data |= (byte) (0x80 >> (k % 8));
                    }
                }
                rasterBMPData[i*newWidthBytes+j] = data;
            }
        }

        if (bmp != null && !bmp.isRecycled())
        {
            bmp.recycle();
        }
        byte[][] rasterBMPPrintData = {rasterBMPHead,rasterBMPData};
        return ESCUtil.byteMerger(rasterBMPPrintData);
    }
}

