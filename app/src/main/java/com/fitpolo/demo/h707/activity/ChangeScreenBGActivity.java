package com.fitpolo.demo.h707.activity;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.fitpolo.demo.h707.AppConstants;
import com.fitpolo.demo.h707.R;
import com.fitpolo.demo.h707.dialog.AlertMessageDialog;
import com.fitpolo.demo.h707.dialog.ChoosePhotoDialog;
import com.fitpolo.demo.h707.dialog.LoadingMessageDialog;
import com.fitpolo.demo.h707.dialog.LoadingProgressDialog;
import com.fitpolo.demo.h707.service.MokoService;
import com.fitpolo.demo.h707.utils.FileUtils;
import com.fitpolo.demo.h707.utils.PhotoModule;
import com.fitpolo.demo.h707.utils.ToastUtils;
import com.fitpolo.support.MokoConstants;
import com.fitpolo.support.MokoSupport;
import com.fitpolo.support.entity.BleDevice;
import com.fitpolo.support.entity.OrderEnum;
import com.fitpolo.support.entity.OrderTaskResponse;
import com.fitpolo.support.handler.UpgradeHandler;
import com.fitpolo.support.log.LogModule;
import com.fitpolo.support.task.ZWriteDialTask;
import com.fitpolo.support.task.ZWriteScreenBGTask;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * @Date 2017/5/11
 * @Author wenzheng.liu
 * @Description
 */

public class ChangeScreenBGActivity extends BaseActivity implements UpgradeHandler.IUpgradeCallback {
    private static final String TAG = "ChangeScreenBGActivity";
    @Bind(R.id.iv_bg)
    ImageView ivBg;

    private MokoService mMokoService;
    private BleDevice mDevice;
    private LoadingMessageDialog mLoadingMessageDialog;
    private LoadingProgressDialog mLoadingProgressDialog;
    private UpgradeHandler upgradeHandler;
    private boolean isUpgradeDone = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_screen_bg);
        ButterKnife.bind(this);
        mDevice = (BleDevice) getIntent().getSerializableExtra("device");


        bindService(new Intent(this, MokoService.class), mServiceConnection, BIND_AUTO_CREATE);
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                String action = intent.getAction();
                if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                    int blueState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
                    switch (blueState) {
                        case BluetoothAdapter.STATE_TURNING_OFF:
                            if (upgradeHandler != null) {
                                upgradeHandler.setStop(true);
                            }
                            MokoSupport.getInstance().pollTask();
                            finishActivityWithMessage(getString(R.string.upgrade_error), RESULT_CANCELED);
                            return;
                    }
                }
                if (MokoConstants.ACTION_CONN_STATUS_DISCONNECTED.equals(action)) {
                    if (isUpgradeDone)
                        return;
                    Toast.makeText(ChangeScreenBGActivity.this, "Disconnect before sending the file", Toast.LENGTH_SHORT).show();
                    abortBroadcast();
                }
                if (MokoConstants.ACTION_ORDER_RESULT.equals(action)) {
                    OrderTaskResponse response = (OrderTaskResponse) intent.getSerializableExtra(MokoConstants.EXTRA_KEY_RESPONSE_ORDER_TASK);
                    OrderEnum orderEnum = response.order;
                    byte[] value = response.responseValue;
                    switch (orderEnum) {
                        case Z_WRITE_DIAL:
                            if (mLoadingMessageDialog != null && mLoadingMessageDialog.isVisible()) {
                                mLoadingMessageDialog.dismissAllowingStateLoss();
                            }
                            break;
                        case Z_WRITE_SCREEN_BG:
                            if (mLoadingMessageDialog != null && mLoadingMessageDialog.isVisible()) {
                                mLoadingMessageDialog.dismissAllowingStateLoss();
                            }
                            if (value.length > 3 && value[3] == 0) {
                                mLoadingProgressDialog = new LoadingProgressDialog();
                                mLoadingProgressDialog.show(ChangeScreenBGActivity.this.getSupportFragmentManager());
                                isUpgradeDone = false;
                                // 发送图片
                                String screenBGPath = PhotoModule.getInstance(ChangeScreenBGActivity.this).getScreenBGPath();
                                upgradeHandler = new UpgradeHandler(ChangeScreenBGActivity.this);
                                upgradeHandler.setFilePath(screenBGPath, mDevice.address, ChangeScreenBGActivity.this);
                            }
                            break;
                    }
                }
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
        unbindService(mServiceConnection);
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mMokoService = ((MokoService.LocalBinder) service).getService();
            // 注册广播接收器
            IntentFilter filter = new IntentFilter();
            filter.addAction(MokoConstants.ACTION_CONN_STATUS_DISCONNECTED);
            filter.addAction(MokoConstants.ACTION_ORDER_RESULT);
            filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
            filter.setPriority(300);
            registerReceiver(mReceiver, filter);
            // The dial needs to be set to 3 or 4 for the background to change
            MokoSupport.getInstance().sendOrder(new ZWriteDialTask(mMokoService, 4));
            mLoadingMessageDialog = new LoadingMessageDialog();
            mLoadingMessageDialog.setDialogDismissCallback(new LoadingMessageDialog.DialogDissmissCallback() {

                @Override
                public void onOvertimeDismiss() {
                    ToastUtils.showToast(ChangeScreenBGActivity.this, R.string.setting_sync_failed);
                    finish();
                }

                @Override
                public void onDismiss() {
                }
            });
            mLoadingMessageDialog.show(getSupportFragmentManager());
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };

    public void setCustomBG(View view) {
        // 选择图片
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!isCameraPermissionOpen()) {
                showRequestPermissionDialog();
                return;
            }
        }
        showPhotoDialog();
    }

    private void showOpenSettingsDialog() {
        AlertMessageDialog dialog = new AlertMessageDialog();
        dialog.setTitle(R.string.permission_camera_close_title);
        dialog.setMessage(R.string.permission_camera_close_content);
        dialog.setConfirm(R.string.permission_open);
        dialog.setOnAlertConfirmListener(new AlertMessageDialog.OnAlertConfirmListener() {
            @Override
            public void onClick() {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                // 根据包名打开对应的设置界面
                intent.setData(Uri.parse("package:" + ChangeScreenBGActivity.this.getPackageName()));
                startActivityForResult(intent, AppConstants.REQUEST_CODE_PERMISSION);
            }
        });
        dialog.show(getSupportFragmentManager());
    }

    private void showRequestPermissionDialog() {
        AlertMessageDialog dialog = new AlertMessageDialog();
        dialog.setTitle(R.string.permission_camera_need_title);
        dialog.setMessage(R.string.permission_camera_need_content);
        dialog.setConfirm(R.string.permission_open);
        dialog.setOnAlertConfirmListener(new AlertMessageDialog.OnAlertConfirmListener() {
            @Override
            public void onClick() {
                ActivityCompat.requestPermissions(ChangeScreenBGActivity.this, new String[]{Manifest.permission.CAMERA}, AppConstants.PERMISSION_REQUEST_CODE);
            }
        });
        dialog.show(getSupportFragmentManager());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case AppConstants.PERMISSION_REQUEST_CODE: {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                        // 判断用户是否 点击了不再提醒。(检测该权限是否还可以申请)
                        boolean shouldShowRequest = shouldShowRequestPermissionRationale(permissions[0]);
                        if (shouldShowRequest) {
                            showRequestPermissionDialog();
                        } else {
                            showOpenSettingsDialog();
                        }
                    } else {
                        showPhotoDialog();
                    }
                }
            }
        }
    }

    public void showPhotoDialog() {
        ChoosePhotoDialog dialog = new ChoosePhotoDialog();
        dialog.setListener(new ChoosePhotoDialog.OnChoosePhotoListener() {
            @Override
            public void onGalleryChecked() {
                Intent i = PhotoModule.getInstance(ChangeScreenBGActivity.this).takeScreen(PhotoModule.REQUEST_CODE_PICTURE);
                if (i != null) {
                    startActivityForResult(i, PhotoModule.REQUEST_CODE_PICTURE);
                }
            }

            @Override
            public void onCameraChecked() {
                Intent i = PhotoModule.getInstance(ChangeScreenBGActivity.this).takeScreen(PhotoModule.REQUEST_CODE_CAMERA);
                if (i != null) {
                    startActivityForResult(i, PhotoModule.REQUEST_CODE_CAMERA);
                }
            }
        });
        dialog.show(getSupportFragmentManager());
    }

    private byte[] screenRGB565Bytes;

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case PhotoModule.REQUEST_CODE_CROP_PHOTO:
                    Uri uriTempFile = Uri.parse(PhotoModule.getInstance(this).getScreenBGURI(false).toString());
                    Bitmap bitmap = null;
                    try {
                        bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(uriTempFile));
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                    ivBg.setImageBitmap(bitmap);
                    // Convert the image to RGB565
                    screenRGB565Bytes = new byte[240 * 240 * 2];
                    int[] pixels = new int[240 * 240];
                    bitmap.getPixels(pixels, 0, 240, 0, 0, 240, 240);
                    for (int i = 0, length = pixels.length; i < length; i++) {
                        short color = (short) RGB888ToRGB565(pixels[i]);
                        screenRGB565Bytes[i * 2] = (byte) (color >> 8 & 0xFF);
                        screenRGB565Bytes[i * 2 + 1] = (byte) (color & 0xFF);
                    }
                    try {
                        String filePath = PhotoModule.getInstance(this).getScreenBGPath();
                        File file = new File(filePath);
                        FileOutputStream fos = new FileOutputStream(file);
                        fos.write(screenRGB565Bytes);
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (!MokoSupport.getInstance().isConnDevice(this, mDevice.address)) {
                        ToastUtils.showToast(this, getString(R.string.match_pair_firstly));
                        finish();
                        return;
                    }
                    mLoadingMessageDialog = new LoadingMessageDialog();
                    mLoadingMessageDialog.setDialogDismissCallback(new LoadingMessageDialog.DialogDissmissCallback() {

                        @Override
                        public void onOvertimeDismiss() {
                            ToastUtils.showToast(ChangeScreenBGActivity.this, R.string.setting_sync_failed);
                        }

                        @Override
                        public void onDismiss() {
                        }
                    });
                    mLoadingMessageDialog.show(getSupportFragmentManager());
                    // If the dial is set to 3 and the background index is set to 0
                    // If the dial is set to 4 and the background index is set to 1
                    MokoSupport.getInstance().sendOrder(new ZWriteScreenBGTask(mMokoService, screenRGB565Bytes.length, 1));
                    break;
                case PhotoModule.REQUEST_CODE_PICTURE:
                    Uri uri = data.getData();
                    String url = FileUtils.getPath(this, uri);
                    if (TextUtils.isEmpty(url)) {
                        return;
                    }
                    Uri imageUri;
                    File imageFile = new File(url);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        imageUri = FileProvider.getUriForFile(this, "com.fitpolo.demo.h707.fileprovider", imageFile);
                    } else {
                        imageUri = Uri.fromFile(imageFile);
                    }
                    Intent intent = PhotoModule.getInstance(this).cropScreenBG(imageUri);
                    if (intent != null) {
                        startActivityForResult(intent, PhotoModule.REQUEST_CODE_CROP_PHOTO);
                    }
                    break;
                case PhotoModule.REQUEST_CODE_CAMERA:
                    Intent i = PhotoModule.getInstance(this).cropScreenBG();
                    if (i != null) {
                        startActivityForResult(i, PhotoModule.REQUEST_CODE_CROP_PHOTO);
                    }
                    break;
            }
        }
        if (requestCode == AppConstants.REQUEST_CODE_PERMISSION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!this.isCameraPermissionOpen()) {
                    showOpenSettingsDialog();
                } else {
                    showPhotoDialog();
                }
            }
        }
    }

    public int RGB888ToRGB565(int n888Color) {
        int n565Color;

        // 获取RGB单色，并截取高位的5位6位5位作为RGB_565的值
        int cRed = (n888Color & 0x00ff0000) >> 19;
        int cGreen = (n888Color & 0x0000ff00) >> 10;
        int cBlue = (n888Color & 0x000000ff) >> 3;

        // 重新组合
        n565Color = (cRed << 11) + (cGreen << 5) + (cBlue << 0);
        return n565Color;
    }

    @Override
    public void onUpgradeError(int errorCode) {
        switch (errorCode) {
            case UpgradeHandler.EXCEPTION_UPGRADE_FAILURE:
                finishActivityWithMessage(getString(R.string.upgrade_error), RESULT_CANCELED);
                break;
        }
        if (mLoadingProgressDialog != null && mLoadingProgressDialog.isVisible())
            mLoadingProgressDialog.dismissAllowingStateLoss();
    }

    @Override
    public void onProgress(int progress) {
        LogModule.i("完成百分比：" + progress);
        if (mLoadingProgressDialog != null && mLoadingProgressDialog.isVisible()) {
            mLoadingProgressDialog.setProgress(progress);
        }
    }

    @Override
    public void onUpgradeDone() {
        isUpgradeDone = true;
        if (mLoadingProgressDialog != null && mLoadingProgressDialog.isVisible())
            mLoadingProgressDialog.dismissAllowingStateLoss();
        finishActivityWithMessage(getString(R.string.upgrade_success), RESULT_OK);
    }

    private void finishActivityWithMessage(final String message, final int result) {
        LogModule.v(message);
        Intent intent = new Intent(MokoConstants.ACTION_CONN_STATUS_DISCONNECTED);
        sendOrderedBroadcast(intent, null);
        ToastUtils.showToast(this, message);
        finish();
    }
}
