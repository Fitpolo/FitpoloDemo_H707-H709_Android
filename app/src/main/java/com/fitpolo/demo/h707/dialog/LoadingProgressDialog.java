package com.fitpolo.demo.h707.dialog;

import android.support.v4.content.ContextCompat;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.fitpolo.demo.h707.R;
import com.scwang.smartrefresh.layout.internal.ProgressDrawable;

import butterknife.Bind;
import butterknife.ButterKnife;

public class LoadingProgressDialog extends BaseDialog {
    public static final String TAG = LoadingProgressDialog.class.getSimpleName();
    @Bind(R.id.iv_loading)
    ImageView ivLoading;
    @Bind(R.id.tv_loading_message)
    TextView tvLoadingMessage;

    @Override
    public int getLayoutRes() {
        return R.layout.dialog_loading_message;
    }

    @Override
    public void bindView(View v) {
        ButterKnife.bind(this, v);
        ProgressDrawable progressDrawable = new ProgressDrawable();
        progressDrawable.setColor(ContextCompat.getColor(getContext(), R.color.text_black_4d4d4d));
        ivLoading.setImageDrawable(progressDrawable);
        progressDrawable.start();
        tvLoadingMessage.setText(getString(R.string.setting_syncing));
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
    public boolean getCancelOutside() {
        return false;
    }

    @Override
    public boolean getCancellable() {
        return true;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ((ProgressDrawable) ivLoading.getDrawable()).stop();
        ButterKnife.unbind(this);
    }

    public void setProgress(int progress) {
        if (tvLoadingMessage != null) {
            tvLoadingMessage.setText(String.format(getString(R.string.progress_tips), progress));
        }
    }
}
