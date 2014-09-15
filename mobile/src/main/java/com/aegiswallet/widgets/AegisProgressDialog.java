/*
 * Aegis Bitcoin Wallet - The secure Bitcoin wallet for Android
 * Copyright 2014 Bojan Simic and specularX.co, designed by Reuven Yamrom
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.aegiswallet.widgets;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import com.aegiswallet.R;

/**
 * Created by bsimic on 5/9/14.
 */
public class AegisProgressDialog extends Dialog {

    private TextView spinnerText;
    private ImageView spinnerImage;
    private AnimationDrawable spinAnimation;
    private HoloCircularProgressBar holoCircularProgressBar;
    private ObjectAnimator mProgressBarAnimator;


    public AegisProgressDialog(Context context, int theme, String value){
        super(context, theme);
        setCancelable(false);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.custom_spinner);

        spinnerText = (TextView) findViewById(R.id.custom_spinner_text);
        spinnerText.setText(value);

        spinnerImage = (ImageView) findViewById(R.id.custom_spinner_image);
        spinnerImage.setBackgroundResource(R.drawable.spinner);
        spinAnimation = (AnimationDrawable) spinnerImage.getBackground();
        holoCircularProgressBar = (HoloCircularProgressBar) findViewById(R.id.spinner_progress_bar);
        holoCircularProgressBar.setProgressBackgroundColor(context.getResources().getColor(R.color.aegis_white));
    }

    @Override
    public void show(){
        super.show();
        startAnimiation();
    }

    public void startAnimiation(){
        holoCircularProgressBar.setMarkerProgress(0f);
        animate(holoCircularProgressBar, null);
    }

    private void animate(final HoloCircularProgressBar progressBar, final Animator.AnimatorListener listener) {
        //final float progress = (float) (Math.random() * 2);
        final float progress = 9f;
        int duration = 10000;
        animate(progressBar, listener, progress, duration);
    }

    private void animate(final HoloCircularProgressBar progressBar, final Animator.AnimatorListener listener,
                         final float progress, final int duration) {

        mProgressBarAnimator = ObjectAnimator.ofFloat(progressBar, "progress", progress);
        mProgressBarAnimator.setDuration(duration);
        mProgressBarAnimator.setRepeatCount(10);
        mProgressBarAnimator.setRepeatMode(ValueAnimator.RESTART);


        mProgressBarAnimator.addListener(new Animator.AnimatorListener() {

            @Override
            public void onAnimationCancel(final Animator animation) {
            }

            @Override
            public void onAnimationEnd(final Animator animation) {
                progressBar.setProgress(progress);

            }

            @Override
            public void onAnimationRepeat(final Animator animation) {
            }

            @Override
            public void onAnimationStart(final Animator animation) {
            }
        });

        if (listener != null) {
            mProgressBarAnimator.addListener(listener);
        }

        mProgressBarAnimator.reverse();
        mProgressBarAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(final ValueAnimator animation) {
                progressBar.setProgress((Float) animation.getAnimatedValue());
            }
        });
        progressBar.setMarkerProgress(progress);
        mProgressBarAnimator.start();
    }



}
