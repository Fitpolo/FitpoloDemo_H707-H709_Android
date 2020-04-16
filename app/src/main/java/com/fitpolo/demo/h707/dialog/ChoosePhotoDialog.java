package com.fitpolo.demo.h707.dialog;

import android.view.Gravity;
import android.view.View;

import com.fitpolo.demo.h707.R;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class ChoosePhotoDialog extends BaseDialog {

    public static final String TAG = ChoosePhotoDialog.class.getSimpleName();

    @OnClick({R.id.tv_setting_gallery, R.id.tv_setting_camera})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.tv_setting_gallery:
                listener.onGalleryChecked();
                dismiss();
                break;
            case R.id.tv_setting_camera:
                listener.onCameraChecked();
                dismiss();
                break;
        }
    }

    private OnChoosePhotoListener listener;

    public void setListener(OnChoosePhotoListener listener) {
        this.listener = listener;
    }

    @Override
    public int getLayoutRes() {
        return R.layout.dialog_setting_photo;
    }

    @Override
    public void bindView(View v) {
        ButterKnife.bind(this, v);
    }

    @Override
    public int getDialogStyle() {
        return R.style.CenterDialog;
    }

    @Override
    public int getGravity() {
        return Gravity.CENTER;
    }

    @Override
    public String getFragmentTag() {
        return TAG;
    }

    @Override
    public float getDimAmount() {
        return 0.7f;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.unbind(this);
    }

    public interface OnChoosePhotoListener {
        void onGalleryChecked();

        void onCameraChecked();
    }
}
