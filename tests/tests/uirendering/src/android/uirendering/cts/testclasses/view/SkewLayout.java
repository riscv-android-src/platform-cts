/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.uirendering.cts.testclasses.view;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

public class SkewLayout extends FrameLayout {

    public SkewLayout(Context context) {
        super(context);
    }

    public SkewLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SkewLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        canvas.save();
        canvas.skew(0.5f, 0f);
        boolean result = super.drawChild(canvas, child, drawingTime);
        canvas.restore();
        return result;
    }
}
