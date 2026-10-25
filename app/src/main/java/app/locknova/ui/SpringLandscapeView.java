package app.locknova.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.LinearGradient;
import android.util.AttributeSet;
import android.view.View;

/** Original pastel railway landscape drawn in code so the app ships without network-loaded artwork. */
public final class SpringLandscapeView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    public SpringLandscapeView(Context context) { super(context); }
    public SpringLandscapeView(Context context, AttributeSet attrs) { super(context, attrs); }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float sx = getWidth() / 360f;
        float sy = getHeight() / 190f;
        canvas.save();
        canvas.scale(sx, sy);
        paint.setShader(new LinearGradient(0, 0, 360, 190, new int[]{0xFFB9C9FF, 0xFFFFD4E2, 0xFFFFE2C6}, null, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, 360, 190, paint);
        paint.setShader(null);
        cloud(canvas, 56, 40, 0.9f);
        cloud(canvas, 234, 32, 0.7f);
        hill(canvas, 0xFFD8C9F6, 0, 111, new float[]{0, 102, 38, 78, 76, 92, 124, 66, 166, 91, 218, 58, 271, 85, 320, 62, 360, 77, 360, 190, 0, 190});
        hill(canvas, 0xFF9FAFE8, 0, 132, new float[]{0, 121, 45, 90, 95, 119, 146, 87, 191, 115, 238, 82, 294, 116, 336, 91, 360, 103, 360, 190, 0, 190});
        hill(canvas, 0xFF85B8C2, 0, 151, new float[]{0, 143, 42, 120, 93, 139, 144, 111, 198, 141, 247, 116, 303, 139, 350, 119, 360, 124, 360, 190, 0, 190});
        paint.setColor(0xFF8E8395);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2.4f);
        Path rail = new Path();
        rail.moveTo(0, 172); rail.cubicTo(93, 159, 221, 163, 360, 171);
        canvas.drawPath(rail, paint);
        paint.setColor(0xFFEEE7F3);
        paint.setStrokeWidth(1.1f);
        Path railHighlight = new Path();
        railHighlight.moveTo(0, 168); railHighlight.cubicTo(93, 155, 221, 159, 360, 167);
        canvas.drawPath(railHighlight, paint);
        paint.setStyle(Paint.Style.FILL);
        drawTrain(canvas, 112, 132);
        drawBranch(canvas);
        paint.setShader(new LinearGradient(0, 140, 0, 190, 0x000F1937, 0x774A416B, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 140, 360, 190, paint);
        paint.setShader(null);
        canvas.restore();
    }

    private void cloud(Canvas canvas, float x, float y, float scale) {
        canvas.save(); canvas.translate(x, y); canvas.scale(scale, scale);
        paint.setColor(0xBFFFFFFF);
        canvas.drawCircle(0, 7, 14, paint); canvas.drawCircle(15, 0, 19, paint);
        canvas.drawCircle(33, 8, 15, paint); canvas.drawRoundRect(0, 7, 35, 20, 10, 10, paint);
        canvas.restore();
    }
    private void hill(Canvas canvas, int color, float ignored, float ignored2, float[] points) {
        Path path = new Path();
        path.moveTo(points[0], points[1]);
        int i = 2;
        while (i + 1 < points.length - 4) { path.quadTo(points[i], points[i + 1], points[i + 2], points[i + 3]); i += 4; }
        path.lineTo(360, 190); path.lineTo(0, 190); path.close();
        paint.setColor(color);
        canvas.drawPath(path, paint);
    }
    private void drawTrain(Canvas canvas, float x, float y) {
        canvas.save(); canvas.translate(x, y);
        paint.setColor(0xFF4C557F);
        canvas.drawRoundRect(0, 0, 126, 26, 7, 7, paint);
        paint.setColor(0xFF6F7EAE);
        canvas.drawRoundRect(5, -7, 121, 8, 8, 8, paint);
        paint.setColor(0xFFFFE8B8);
        for (int i = 0; i < 5; i++) canvas.drawRoundRect(12 + i * 20, -3, 26 + i * 20, 7, 3, 3, paint);
        paint.setColor(0xFF373D65);
        canvas.drawCircle(20, 27, 5, paint); canvas.drawCircle(103, 27, 5, paint);
        paint.setColor(0xFFFFC5D6);
        canvas.drawRect(0, 12, 126, 16, paint);
        canvas.restore();
    }
    private void drawBranch(Canvas canvas) {
        paint.setColor(0xFF6A776C);
        paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(2.3f);
        Path branch = new Path(); branch.moveTo(-7, -1); branch.cubicTo(18, 20, 28, 41, 54, 68);
        branch.moveTo(17, 19); branch.quadTo(22, 2, 39, -8);
        branch.moveTo(26, 34); branch.quadTo(48, 25, 64, 26);
        branch.moveTo(39, 49); branch.quadTo(58, 44, 72, 49);
        canvas.drawPath(branch, paint); paint.setStyle(Paint.Style.FILL);
        blossom(canvas, 34, 1, 0.92f); blossom(canvas, 58, 25, 0.72f);
        blossom(canvas, 11, 23, 0.7f); blossom(canvas, 47, 47, 0.62f);
    }
    private void blossom(Canvas canvas, float x, float y, float scale) {
        canvas.save(); canvas.translate(x, y); canvas.scale(scale, scale);
        int[] colors = {0xFFFFF2FA, 0xFFFFD3E8, 0xFFFFE2F1, 0xFFEABDE1, 0xFFFFF8FC};
        for (int i = 0; i < 5; i++) {
            double angle = Math.PI * 2 * i / 5.0;
            paint.setColor(colors[i]);
            canvas.drawCircle((float) Math.cos(angle) * 6, (float) Math.sin(angle) * 6, 4.2f, paint);
        }
        paint.setColor(0xFFFFC66F); canvas.drawCircle(0, 0, 2.2f, paint);
        canvas.restore();
    }
}
