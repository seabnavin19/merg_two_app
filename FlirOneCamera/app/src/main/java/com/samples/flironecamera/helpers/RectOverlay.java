package com.samples.flironecamera.helpers;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

public class RectOverlay extends GraphicOverlay.Graphic {

    GraphicOverlay _graphicOverlay;
    private Rect _rect;
    private Paint _mRectPaint;

    public RectOverlay(GraphicOverlay graphicOverlay, Rect rect) {
        super(graphicOverlay);
        _mRectPaint = new Paint();
        int _mRectColor = Color.GREEN;
        _mRectPaint.setColor(_mRectColor);
        _mRectPaint.setStyle(Paint.Style.STROKE);
        float _mStrokeWidth = 4.0f;
        _mRectPaint.setStrokeWidth(_mStrokeWidth);
        this._graphicOverlay = graphicOverlay;
        this._rect = rect;
        postInvalidate();
    }

    @Override
    public void draw(Canvas canvas) {
        RectF rectF = new RectF(this._rect);
        rectF.left = translateX(rectF.left);
        rectF.right = translateX(rectF.right);
        rectF.top = translateY(rectF.top);
        rectF.bottom = translateY(rectF.bottom);

        canvas.drawRect(rectF, _mRectPaint);
    }
}