package com.onrender.tutrnav.ui.auth;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

public class OrganicBlobView extends View {

    private final Paint paint;
    private final Path path;

    private float progress = 0f;
    private int currentIndex = 0;
    private int nextIndex = 0;

    // EXACTLY 4 Distinct Organic Shapes (8 control points each)
    // Finely tuned to ensure they look like liquid floating in space
    private final float[][] SHAPES = {
            {0.90f, 0.75f, 1.00f, 0.85f, 0.90f, 0.70f, 0.95f, 0.90f}, // Blob 1
            {0.80f, 0.95f, 0.75f, 0.95f, 0.85f, 1.00f, 0.75f, 0.85f}, // Blob 2
            {1.00f, 0.80f, 0.90f, 0.70f, 1.00f, 0.85f, 0.80f, 0.95f}, // Blob 3
            {0.75f, 0.90f, 0.80f, 1.00f, 0.75f, 0.90f, 1.00f, 0.80f}  // Blob 4
    };

    public OrganicBlobView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.parseColor("#25FFFFFF")); // Gorgeous translucent glass
        paint.setStyle(Paint.Style.FILL);
        path = new Path();
    }

    public void setMorphState(int currentIndex, int nextIndex, float progress) {
        this.currentIndex = currentIndex;
        this.nextIndex = nextIndex;
        this.progress = progress;
        invalidate(); // Trigger flawless 60fps redraw
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float baseRadius = Math.min(cx, cy) * 0.85f;

        path.reset();

        // 1. Interpolate smoothly between current blob and next blob
        float[] currentRadii = new float[8];
        for (int i = 0; i < 8; i++) {
            currentRadii[i] = SHAPES[currentIndex][i] + (SHAPES[nextIndex][i] - SHAPES[currentIndex][i]) * progress;
        }

        // 2. Draw Quadratic Bezier Curves
        for (int i = 0; i < 8; i++) {
            float angle1 = (float) Math.toRadians(i * 45);
            float angle2 = (float) Math.toRadians(((i + 1) % 8) * 45);
            float angleMid = (float) Math.toRadians((i + 0.5f) * 45);

            float r1 = baseRadius * currentRadii[i];
            float r2 = baseRadius * currentRadii[(i + 1) % 8];
            float rMid = baseRadius * ((currentRadii[i] + currentRadii[(i + 1) % 8]) / 2f) * 1.15f; // 1.15 smooths the curves out

            float x1 = cx + r1 * (float) Math.cos(angle1);
            float y1 = cy + r1 * (float) Math.sin(angle1);
            float x2 = cx + r2 * (float) Math.cos(angle2);
            float y2 = cy + r2 * (float) Math.sin(angle2);
            float xMid = cx + rMid * (float) Math.cos(angleMid);
            float yMid = cy + rMid * (float) Math.sin(angleMid);

            if (i == 0) path.moveTo(x1, y1);
            path.quadTo(xMid, yMid, x2, y2);
        }
        path.close();

        // 3. Continuous slow rotation (90 degrees per slide)
        float rotation = (currentIndex * 90) + (progress * 90);
        canvas.save();
        canvas.rotate(rotation, cx, cy);
        canvas.drawPath(path, paint);
        canvas.restore();
    }
}