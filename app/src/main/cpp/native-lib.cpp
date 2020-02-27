#include <jni.h>
#include <string>
#include <android/bitmap.h>
#include <malloc.h>
#include <jpeglib.h>

extern "C"
{
#include "jpeglib.h"
}
void write_JPEG_file(uint8_t *data, int w, int h, const char *path) {
    //创建jpeg压缩对象
    jpeg_compress_struct jcs;
    //设置错误处理信息
    jpeg_error_mgr error;
    jcs.err = jpeg_std_error(&error);
    //类比Bitmap.create();
    //给结构体分配内存
    jpeg_create_compress(&jcs);
    //压缩bitmap---->file
    //指定存储文件
    FILE *file = fopen(path, "wb");
    jpeg_stdio_dest(&jcs, file);
    //设置压缩参数
    jcs.image_width = w;
    jcs.image_height = h;

    //开启哈夫曼功能
    jcs.arith_code = FALSE;
    //优化编码
    jcs.optimize_coding = TRUE;
    //rgb, JCS_RGB 不要A
    jcs.in_color_space = JCS_RGB;
    jcs.input_components = 3;
    //其它参数设置为默认
    jpeg_set_defaults(&jcs);
    //设置质量压缩比例
    jpeg_set_quality(&jcs, 20, true);

    //开始压缩
    jpeg_start_compress(&jcs, 1);
    //循环写入每一行数
    int row_stride = w * 3;//一行的字节数
    JSAMPROW row[1];
    while (jcs.next_scanline < jcs.image_height) {
        //取一行数据
        uint8_t *pixels = data + jcs.next_scanline * row_stride;
        row[0]=pixels;
        jpeg_write_scanlines(&jcs,row,1);
    }
    //压缩完成
    jpeg_finish_compress(&jcs);
    //释放jpeg对象
    fclose(file);
    jpeg_destroy_compress(&jcs);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_bitmap_compress_MainActivity_compress(JNIEnv *env, jobject thiz, jobject bitmap,
                                               jstring path_) {
    const char *path = env->GetStringUTFChars(path_, 0);
    //从bitmap获取argb数据
    AndroidBitmapInfo info;//info=new 对象();
    //获取里面的信息
    AndroidBitmap_getInfo(env, bitmap, &info);//  void method(list)
    //得到图片中的像素信息
    uint8_t *pixels;//uint8_t char    java   byte     *pixels可以当byte[]
    AndroidBitmap_lockPixels(env, bitmap, (void **) &pixels);
    //jpeg argb中去掉他的a ===>rgb
    int w = info.width;
    int h = info.height;
    int color;
    //开一块内存用来存入rgb信息
    uint8_t* data = (uint8_t *) malloc(w * h * 3);//data中可以存放图片的所有内容
    uint8_t* temp = data;
    uint8_t r, g, b;//byte
    //循环取图片的每一个像素
    for (int i = 0; i < h; i++) {
        for (int j = 0; j < w; j++) {
            color = *(int *) pixels;//0-3字节  color4 个字节  一个点
            //颜色值为16进制表示，通过运算取出rgb
            r = (color >> 16) & 0xFF;//    #00rrggbb  16  0000rr   8  00rrgg
            g = (color >> 8) & 0xFF;
            b = color & 0xFF;
            //存放，以前的主流格式jpeg    bgr
            *data = b;
            *(data + 1) = g;
            *(data + 2) = r;
            data += 3;
            //指针跳过4个字节
            pixels += 4;
        }
    }
    //把得到的新的图片的信息存入一个新文件 中
    write_JPEG_file(temp, w, h, path);
    //释放内存
    free(temp);
    AndroidBitmap_unlockPixels(env, bitmap);
    env->ReleaseStringUTFChars(path_, path);

}