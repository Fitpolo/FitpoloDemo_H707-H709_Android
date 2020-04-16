package com.fitpolo.demo.h707.utils;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;

import com.fitpolo.demo.h707.R;

import java.io.File;

public class PhotoModule {
    public static final int REQUEST_CODE_CAMERA = 0x1001;
    public static final int REQUEST_CODE_CROP_PHOTO = 0x1002;
    public static final int REQUEST_CODE_PICTURE = 0x1003;
    private static final String TEMP_PHOTO = "temp_photo.jpg";
    private static final String TEMP_SCREEN = "temp_screen.jpg";
    private static final String TEMP_SCREEN_BIN = "temp_screen.bin";
    private static volatile PhotoModule INSTANCE;
    private Context context;

    private PhotoModule(Context context) {
        this.context = context;
    }


    public static PhotoModule getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (PhotoModule.class) {
                if (INSTANCE == null) {
                    INSTANCE = new PhotoModule(context.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }

    public Uri getScreenBGURI(boolean isNeedProvider) {
        File file = new File(context.getExternalCacheDir() + File.separator + TEMP_SCREEN);
        Uri uri;
        if (!isNeedProvider) {
            uri = Uri.fromFile(file);
            return uri;
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                uri = FileProvider.getUriForFile(context, "com.fitpolo.demo.h707.fileprovider", file);
            } else {
                uri = Uri.fromFile(file);
            }
            return uri;
        }
    }

    public String getScreenBGPath() {
        return context.getExternalCacheDir() + File.separator + TEMP_SCREEN_BIN;
    }

    /**
     * 获取照片
     *
     * @param request_code
     */
    public Intent takeScreen(int request_code) {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            // 照相
            if (request_code == REQUEST_CODE_CAMERA) {
                Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
                intent.putExtra(MediaStore.EXTRA_OUTPUT, getScreenBGURI(true));
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }
                intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 0);
                return intent;
            }
            // 相册中选取
            else if (request_code == REQUEST_CODE_PICTURE) {
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("image/*");
                return i;
            }

        } else {
            ToastUtils.showToast(context, context.getString(R.string.photo_sd_dismiss));
        }
        return null;
    }

    /**
     * 裁剪照片
     */
    public Intent cropScreenBG() {
        return cropScreenBG(getScreenBGURI(true));
    }

    /**
     * 裁剪照片
     */
    public Intent cropScreenBG(Uri uri) {
        Intent i = new Intent("com.android.camera.action.CROP");
        i.setDataAndType(uri, "image/*");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        i.putExtra("crop", "true");
        i.putExtra("aspectX", 1);
        i.putExtra("aspectY", 1);
        i.putExtra("outputX", 240);
        i.putExtra("outputY", 240);
        i.putExtra("scale", true);//黑边
        i.putExtra("scaleUpIfNeeded", true);//黑边
        i.putExtra("noFaceDetection", true);
        /**
         * 此方法返回的图片只能是小图片（sumsang测试为高宽160px的图片）
         * 故只保存图片Uri，调用时将Uri转换为Bitmap，此方法还可解决miui系统不能return data的问题
         */
        i.putExtra("return-data", false);
        //裁剪后的图片Uri路径，uritempFile为Uri类变量
        i.putExtra(MediaStore.EXTRA_OUTPUT, getScreenBGURI(false));
        i.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
        return i;
    }
}
