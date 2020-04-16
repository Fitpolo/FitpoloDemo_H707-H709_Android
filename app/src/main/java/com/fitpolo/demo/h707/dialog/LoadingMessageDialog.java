package com.fitpolo.demo.h707.dialog;

import android.content.DialogInterface;
import android.support.v4.content.ContextCompat;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.fitpolo.demo.h707.R;
import com.scwang.smartrefresh.layout.internal.ProgressDrawable;

import butterknife.Bind;
import butterknife.ButterKnife;

public class LoadingMessageDialog extends BaseDialog {
    private static final int DIALOG_DISMISS_DELAY_TIME = 5000;
    public static final String TAG = LoadingMessageDialog.class.getSimpleName();
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
        tvLoadingMessage.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isVisible()) {
                    dismissAllowingStateLoss();
                    if (callback != null) {
                        callback.onOvertimeDismiss();
                    }
                }
            }
        }, DIALOG_DISMISS_DELAY_TIME);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        if (callback != null) {
            callback.onDismiss();
        }
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

    private DialogDissmissCallback callback;

    public void setDialogDismissCallback(final DialogDissmissCallback callback) {
        this.callback = callback;
    }

    public interface DialogDissmissCallback {
        void onOvertimeDismiss();

        void onDismiss();
    }
}
