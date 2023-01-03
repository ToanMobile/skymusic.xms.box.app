
package com.nct.xmusicstation.library;

import android.content.Context;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.util.AttributeSet;
import android.widget.ImageButton;

/**
 * Applies a pressed state color filter or disabled state alpha for the button's background
 * drawable.
 * 
 * @author ducth
 */
public class AlphaOnClickButton extends ImageButton {

    public AlphaOnClickButton(Context context) {
        super(context);
    }

    public AlphaOnClickButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AlphaOnClickButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        // Replace the original background drawable (e.g. image) with a
        // LayerDrawable that
        // contains the original drawable.
        AlphaLayerDrawable layer = new AlphaLayerDrawable(drawable);
        super.setImageDrawable(layer);
    }

    /**
     * The stateful LayerDrawable used by this button.
     */
    protected class AlphaLayerDrawable extends LayerDrawable {

        // The color filter to apply when the button is pressed
        protected ColorFilter _pressedFilter = new LightingColorFilter(
                Color.LTGRAY, 1);

        // Alpha value when the button is disabled
        protected int _dimAlpha = 100;

        protected int _opacityAlpha = 255;

        public AlphaLayerDrawable(Drawable d) {
            super(new Drawable[] {
                    d
            });
        }

        @Override
        protected boolean onStateChange(int[] states) {
            boolean enabled = false;
            boolean pressed = false;

            for (int state : states) {
                if (state == android.R.attr.state_enabled)
                    enabled = true;
                else if (state == android.R.attr.state_pressed)
                    pressed = true;
            }

            mutate();
            if (enabled && pressed) {
                setColorFilter(_pressedFilter);
                setAlpha(_dimAlpha);
            } else if (!enabled) {
                setColorFilter(_pressedFilter);
                setAlpha(_dimAlpha);
            } else {
                setColorFilter(null);
                setAlpha(_opacityAlpha);
            }

            invalidateSelf();

            return super.onStateChange(states);
        }

        @Override
        public boolean isStateful() {
            return true;
        }
    }

}
