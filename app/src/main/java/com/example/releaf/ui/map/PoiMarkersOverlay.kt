package com.example.releaf.ui.map

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.graphics.Rect
import android.graphics.drawable.Drawable
import com.example.releaf.R
import com.example.releaf.data.remote.dto.PoiDto
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay
import androidx.core.graphics.toColorInt

class PoiMarkersOverlay(
    private val pois: List<PoiDto>,
    private val onPoiClick: (PoiDto) -> Unit
) : Overlay() {

    private var toiletIcon: Drawable? = null
    private var trashIcon: Drawable? = null

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow) return

        val zoom = mapView.zoomLevelDouble
        val scale = ((zoom - 12.0) / 6.0).coerceIn(0.7, 1.5)
        val baseSize = (128 * scale).toInt()
        val context = mapView.context

        if (toiletIcon == null) toiletIcon =
            androidx.core.content.ContextCompat.getDrawable(context, R.drawable.ic_toilet)
        if (trashIcon == null) trashIcon =
            androidx.core.content.ContextCompat.getDrawable(context, R.drawable.ic_trash)

        for (poi in pois) {
            val p = Point()
            mapView.projection.toPixels(GeoPoint(poi.latitude, poi.longitude), p)

            if (p.x < -200 || p.y < -200 || p.x > canvas.width + 200 || p.y > canvas.height + 200) continue

            drawPin(canvas, p.x.toFloat(), p.y.toFloat(), baseSize, poi)
        }
    }

    private fun drawPin(canvas: Canvas, cx: Float, cy: Float, size: Int, poi: PoiDto) {
        val r = size / 2f
        val fillPaint = if (poi.category == "TOILET") TOILET_PAINT else TRASH_PAINT

        canvas.drawCircle(cx, cy - r * 0.1f, r * 0.75f, fillPaint)

        val tipY = cy + r * 0.9f
        val path = android.graphics.Path().apply {
            moveTo(cx - r * 0.3f, cy + r * 0.15f)
            lineTo(cx + r * 0.3f, cy + r * 0.15f)
            lineTo(cx, tipY)
            close()
        }
        canvas.drawPath(path, fillPaint)

        val drawable = if (poi.category == "TOILET") toiletIcon else trashIcon
        if (drawable == null) return

        val iconSize = (r * 1.0f).toInt()
        val left = (cx - iconSize / 2f).toInt()
        val top = (cy - r * 0.1f - iconSize / 2f).toInt()
        drawable.setBounds(left, top, left + iconSize, top + iconSize)
        drawable.draw(canvas)

        if (!poi.is_verified) {
            val badgeR = r * 0.3f
            canvas.drawCircle(cx + r * 0.55f, cy - r * 0.75f, badgeR, BADGE_PAINT)
            BADGE_TEXT_PAINT.textSize = badgeR
            canvas.drawText("!", cx + r * 0.55f, cy - r * 0.75f + badgeR * 0.4f, BADGE_TEXT_PAINT)
        }
    }

    override fun onSingleTapConfirmed(e: android.view.MotionEvent, mapView: MapView): Boolean {
        val p = Point(e.x.toInt(), e.y.toInt())
        for (poi in pois) {
            val screen = Point()
            mapView.projection.toPixels(GeoPoint(poi.latitude, poi.longitude), screen)
            val r = 80
            val rect = Rect(screen.x - r, screen.y - r * 2, screen.x + r, screen.y + r)
            if (rect.contains(p.x, p.y)) {
                onPoiClick(poi)
                return true
            }
        }
        return false
    }

    private companion object {
        val TOILET_PAINT = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = "#E53935".toColorInt()
            style = Paint.Style.FILL
        }
        val TRASH_PAINT = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = "#43A047".toColorInt()
            style = Paint.Style.FILL
        }
        val BADGE_PAINT = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = "#FF9800".toColorInt()
            style = Paint.Style.FILL
        }
        val BADGE_TEXT_PAINT = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
    }
}
