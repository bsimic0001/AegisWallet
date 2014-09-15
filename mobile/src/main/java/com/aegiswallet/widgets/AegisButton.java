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

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;

import com.aegiswallet.R;

import static com.aegiswallet.widgets.AegisTypeface.applyTypeface;
import static com.aegiswallet.widgets.AegisTypeface.getFont;

public class AegisButton extends Button {

    private boolean preventRepeatClick;

    public AegisButton(Context context) {
        super(context);
        applyTypeface(this);
    }

    public AegisButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        applyTypeface(this);
        init(context, attrs);
    }

    public AegisButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        applyTypeface(this);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        TypedArray config = context.obtainStyledAttributes(attrs, R.styleable.AegisButton);
        preventRepeatClick = config.getBoolean(R.styleable.AegisButton_scsPreventRepeatClick, false);
        applyTypeface(this, getFont(config, R.styleable.AegisButton_scsFont));
        config.recycle();
    }

    @Override
    public void setOnClickListener(OnClickListener listener) {
        if ((listener != null) && preventRepeatClick) {
            super.setOnClickListener(new PreventRepeatClickListener(listener));
        } else {
            super.setOnClickListener(listener);
        }
    }

    private static class PreventRepeatClickListener implements OnClickListener {

        private final OnClickListener delegate;

        public PreventRepeatClickListener(OnClickListener delegate) {
            this.delegate = delegate;
        }

        @Override
        public void onClick(View v) {
            v.setEnabled(false);
            delegate.onClick(v);
        }
    }
}
