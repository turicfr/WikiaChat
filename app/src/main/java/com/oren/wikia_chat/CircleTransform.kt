package com.oren.wikia_chat

import android.graphics.*
import com.squareup.picasso.Transformation
import kotlin.math.min

class CircleTransform : Transformation {
    override fun transform(source: Bitmap): Bitmap {
        val size = min(source.width, source.height)

        val x = (source.width - size) / 2
        val y = (source.height - size) / 2

        val squaredBitmap = Bitmap.createBitmap(source, x, y, size, size)
        if (squaredBitmap != source) {
            source.recycle()
        }

        val bitmap = Bitmap.createBitmap(size, size, source.config)

        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            shader = BitmapShader(
                squaredBitmap,
                Shader.TileMode.CLAMP,
                Shader.TileMode.CLAMP,
            )
            isAntiAlias = true
        }

        val r = size / 2f
        canvas.drawCircle(r, r, r, paint)

        squaredBitmap.recycle()
        return bitmap
    }

    override fun key() = "circle"
}
