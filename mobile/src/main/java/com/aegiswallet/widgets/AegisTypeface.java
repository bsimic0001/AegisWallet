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

import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.widget.TextView;
import com.google.common.base.Optional;

import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;

public class AegisTypeface {

    public static Optional<Font> DEFAULT_FONT = Optional.absent();

    private static final String ASSET_PATH = "fonts/%s.otf";
    private static final Map<Font, Typeface> TYPEFACES = newHashMap();

    // if the order of this enum changes you need to modify the order in app/res/values/attrs.xml
    public enum Font {
        regular,
        semibold
    }

    private AegisTypeface() {
    }

    public static void applyTypeface(TextView widget) {
        applyTypeface(widget, DEFAULT_FONT);
    }

    public static void applyTypeface(TextView widget, Optional<Font> font) {
        Optional<Typeface> typeface = createTypeface(widget, font);
        if (typeface.isPresent()) {
            widget.setTypeface(typeface.get());
        }
    }

    public static Optional<Typeface> createTypeface(TextView widget, Optional<Font> option) {
        if (widget.isInEditMode()) {
            return Optional.absent();
        }
        synchronized (TYPEFACES) {
            Font font = option.or(Font.regular);
            Typeface typeface = TYPEFACES.get(font);
            if (typeface == null) {
                typeface = Typeface.createFromAsset(widget.getContext().getAssets(), getAssetPath(font));
                TYPEFACES.put(font, typeface);
            }
            // This is never null at runtime, but roboelectric cannot create typefaces so it is null during tests
            return Optional.fromNullable(typeface);
        }
    }

    public static String getAssetPath(Font font) {
        return String.format(ASSET_PATH, font.name());
    }

    public static Optional<Font> getFont(TypedArray config, int fontId) {
        return getFont(config.getInt(fontId, -1));
    }

    public static Optional<Font> getFont(int index) {
        Font[] fonts = Font.values(); // order as per app/res/values/attrs.xml
        return (index >= 0 && index < fonts.length) ? Optional.of(fonts[index]) : DEFAULT_FONT;
    }
}
