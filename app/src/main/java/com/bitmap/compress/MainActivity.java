package com.bitmap.compress;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;

public class MainActivity extends AppCompatActivity {

    private String stringName;

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    //点击事件，压缩图片
    public void compressImage(View view) {
        //decodeResource 解码图片控制参数主要包含下面两个
        //opts.inDensity 表示像素密度 根据Drawable目录进行计算
        //opts.inTargetDensity 画到屏幕上的像素密度
        //BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher_background);
        //图片放在不同的drawable目录下或显示在不同屏幕密度的设备上，会有不同程度的缩放，与资源加载机制有关
        //为了得到原始图片在内存中占有大小，没有选择将图片放到drawable资源目录，直接放到手机storage/emulated/0路径下
        //可自行选择图片代替代码中的beauty.jpeg

        File file = new File(Environment.getExternalStorageDirectory(), "beauty.jpeg");
        stringName = file.getAbsolutePath();
        Bitmap inputBitmap = BitmapFactory.decodeFile(stringName);
        Log.d("compressImage: image" ,+ inputBitmap.getWidth() + "X"+ inputBitmap.getHeight() + " 占用内存大小：" + inputBitmap.getByteCount());



        // 质量压缩
        File output1= new File(Environment.getExternalStorageDirectory(), "质量压缩.jpeg");
        compressQuality(inputBitmap, output1);
        // 尺寸压缩
        File output2= new File(Environment.getExternalStorageDirectory(), "尺寸压缩.jpeg");
        compressSize(inputBitmap,output2);

        // 采样率压缩
        File output3 = new File(Environment.getExternalStorageDirectory(), "采样率压缩.jpeg");
        compressInSampleSize(inputBitmap, output3);

        // 哈夫曼/微信压缩
        File output= new File(Environment.getExternalStorageDirectory(), "微信压缩.jpeg");
        compress(inputBitmap, output.getAbsolutePath());
    }

    /**
     * 哈夫曼压缩
     * @param bitmap
     * @param path
     */
    public native void compress(Bitmap bitmap, String path);


    /**
     * 质量压缩
     * 设置bitmap options属性，降低图片的质量，像素不会减少
     * 第一个参数为需要压缩的bitmap图片对象，第二个参数为压缩后图片保存的位置
     * 设置options 属性0-100，来实现压缩
     * 原理：通过算法扣掉(同化)了图片中的一些某个点附近相近的像素，达到降低质量减少文件大小的目的减小了图片质量
     * 注意：它其实只能实现对file的影响，对加载这个图片出来的bitmap内存是无法节省的，还是那么大因为bitmap
     * 在内存中的大小是按照像素计算的，也就是width*height,对于质量压缩，并不会改变图片的真实像素(像素大小不会变)
     * 使用场景：将图片压缩后保存到本地，或者将图片上传到服务器，根据实际需求来使用。
     *
     * 质量压缩并不会改变图片在内存中的大小，仅仅会减小图片所占用的磁盘空间的大小，
     * 因为质量压缩不会改变图片的分辨率，而图片在内存中的大小是根据width*height*一个
     * 像素的所占用的字节数计算的，宽高没变，在内存中占用的大小自然不会变，质量压缩的
     * 原理是通过改变图片的位深和透明度来减小图片占用的磁盘空间大小，所以不适合作为缩
     * 略图，可以用于想保持图片质量的同时减小图片所占用的磁盘空间大小。另外，由于png是
     * 无损压缩，所以设置quality无效。
     *
     * 一张分辨率为1024*1024的图片
     * RGB_565 单个像素占2个字节      占用内存1024*1024*2=2M
     * ARGB_8888 单个像素占4个字节    占用内存1024*1024*4=4M
     * @param bmp
     * @param file
     */
    public void compressQuality(Bitmap bmp,File file) {
        // 0-100 100为不压缩
        int quality = 10;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // 把压缩后的数据存放到baos中
        bmp.compress(Bitmap.CompressFormat.JPEG,  quality, baos);
        try {
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(baos.toByteArray());
            fos.flush();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 2. 尺寸压缩
     * 通过减少单位尺寸的像素值，真正意义上的降低像素
     * 通过缩放图片像素来达到减少图片占用内存大小的效果
     * 使用场景：缩略图(头像)
     * @param bmp
     * @param file
     */

    public void compressSize(Bitmap bmp, File file){
        // 尺寸压缩倍数,值越大，图片尺寸越小
        int ratio = 6;
        // 压缩Bitmap到对应尺寸
        Bitmap result = Bitmap.createBitmap(bmp.getWidth() / ratio, bmp.getHeight() / ratio, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        Rect rect = new Rect(0, 0, bmp.getWidth() / ratio, bmp.getHeight() / ratio);
        //将原图画在缩放之后的矩形上
        canvas.drawBitmap(bmp, null, rect, null);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // 把压缩后的数据存放到baos中
        result.compress(Bitmap.CompressFormat.JPEG, 100 ,baos);
        try {
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(baos.toByteArray());
            fos.flush();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 采样率压缩
     * @param bitmap
     * @param file
     */
    public void compressInSampleSize(Bitmap bitmap, File file) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        //正常的做法是通过ImageView的宽高，和图片自身的宽高计算采样率
        //inJustDecodeBounds为true的时候不会真正的加载图片，只是解析图片原始宽高信息并不会去真正加载图片，轻量级操作
        //options.inJustDecodeBounds = true;
        //BitmapFactory.decodeFile(stringName, options);
        //通过计算获取采样率inSampleSize
        //options.inSampleSize = calculateInSampleSize(options, 150, 100);

        //这里只是为了为了通过采用率压缩图片，不需要根据ImageView的宽高，和图片自身的宽高计算采样率
        //直接赋值采样率为8，获得采样率后，再设置inJustDecodeBounds为false，真正的加载图片
        //采样率数值越高，图片像素越低
        int inSampleSize = 8;
        options.inSampleSize = inSampleSize;
        options.inJustDecodeBounds = false;
        Bitmap bitmap1 = BitmapFactory.decodeFile(stringName, options);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // 把压缩后的数据保存到baos中
        bitmap1.compress(Bitmap.CompressFormat.JPEG, 100, baos);// 不进行质量压缩

        try {
            if (file.exists()) {
                file.delete();
            } else {
                file.createNewFile();
            }
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(baos.toByteArray());
            fos.flush();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取采样率
     * @param options
     * @param reqWidth 需要显示/控件定义的宽度
     * @param reqHeight 需要显示/控件定义的高度
     * @return
     */
    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int width = options.outWidth;
        int height = options.outHeight;

        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            int halfWidth = width / 2;
            int halfHeight = height / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *=2;
            }
        }

        return inSampleSize;
    }

}
