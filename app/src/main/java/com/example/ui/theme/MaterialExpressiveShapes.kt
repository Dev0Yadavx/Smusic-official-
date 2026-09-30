package com.example.ui.theme

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.cos
import kotlin.math.sin

/**
 * Material 3 Expressive Cookie4Sided Shape
 * Official Reference: https://developer.android.com/reference/kotlin/androidx/compose/material3/MaterialShapes#Cookie4Sided()
 * A 4-sided cookie shape with 4 smooth rounded lobes and gentle concave valleys in between.
 */
class Cookie4SidedShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path()
        val width = size.width
        val height = size.height
        val cx = width / 2f
        val cy = height / 2f
        val rOut = minOf(width, height) / 2f
        val rIn = rOut * 0.74f

        val numPoints = 8
        val pointsX = FloatArray(numPoints)
        val pointsY = FloatArray(numPoints)

        // 8 points: 4 outer peaks (top, right, bottom, left) and 4 inner valleys
        // Shift by -PI/2 so the first peak points up at 12 o'clock
        for (i in 0 until numPoints) {
            val angle = (i * Math.PI / 4.0 - Math.PI / 2.0).toFloat()
            val r = if (i % 2 == 0) rOut else rIn
            pointsX[i] = cx + r * cos(angle)
            pointsY[i] = cy + r * sin(angle)
        }

        path.moveTo(pointsX[0], pointsY[0])

        for (i in 0 until numPoints) {
            val prev = (i - 1 + numPoints) % numPoints
            val curr = i
            val next = (i + 1) % numPoints
            val nextNext = (i + 2) % numPoints

            // Catmull-Rom spline tangents converted to cubic Bezier control points
            val c1x = pointsX[curr] + (pointsX[next] - pointsX[prev]) / 5.5f
            val c1y = pointsY[curr] + (pointsY[next] - pointsY[prev]) / 5.5f
            val c2x = pointsX[next] - (pointsX[nextNext] - pointsX[curr]) / 5.5f
            val c2y = pointsY[next] - (pointsY[nextNext] - pointsY[curr]) / 5.5f

            path.cubicTo(c1x, c1y, c2x, c2y, pointsX[next], pointsY[next])
        }

        path.close()
        return Outline.Generic(path)
    }
}

val Cookie4Sided = Cookie4SidedShape()

/**
 * Custom scalloped star/flower shape for MiniPlayer artwork
 * Parsed directly from ic_mini_player_shape.xml vector path
 */
class MiniPlayerArtworkShape : Shape {
    companion object {
        private const val PATH_DATA = "M243.5,15.35C235.22,18.09 230.24,21.57 221.01,31.09C208.42,44.06 203.5,46.51 190,46.49C182.24,46.48 179.22,46 173.5,43.87C163.78,40.25 156.9,39.58 148.15,41.4C139.08,43.29 131.98,47.1 125.54,53.53C119.23,59.84 115.3,67.82 113.33,78.33C111.34,88.93 107.97,95.93 101.95,101.95C95.93,107.97 88.93,111.34 78.33,113.33C67.83,115.3 59.94,119.19 53.28,125.68C44.44,134.3 38.98,149.11 40.46,160.5C40.81,163.25 42.47,169.77 44.15,175C46.8,183.24 47.14,185.52 46.7,192.2C45.93,204.01 42.96,209.46 30.97,221.08C23.03,228.77 20.38,232.1 17.77,237.6C14.67,244.15 14.5,245.09 14.5,256C14.5,266.89 14.67,267.86 17.74,274.35C20.32,279.79 23.02,283.16 30.94,290.85C44.08,303.59 46.51,308.46 46.49,322C46.48,329.76 46,332.78 43.87,338.5C40.25,348.22 39.58,355.1 41.4,363.85C43.27,372.81 47.08,379.96 53.32,386.21C59.69,392.6 67.36,396.33 79.39,398.88C99.49,403.14 108.86,412.5 113.15,432.57C114.32,438.03 116.55,445.06 118.11,448.18C125.68,463.29 144.48,473.55 160.85,471.5C163.83,471.13 170.38,469.46 175.39,467.81C183.14,465.24 185.62,464.86 192.02,465.22C203.7,465.88 209.46,469.01 221.15,481.06C228.84,488.98 232.21,491.68 237.65,494.26C244.14,497.33 245.1,497.5 256,497.5C266.94,497.5 267.84,497.34 274.48,494.2C280.11,491.53 283.39,488.9 291.48,480.59C299.95,471.88 302.43,469.93 307.5,468.02C317.35,464.29 325.75,464.25 337,467.85C349.05,471.72 355,472.47 362.47,471.09C381.46,467.58 394.74,454.16 398.51,434.68C400.99,421.87 403.8,416.15 411.04,409.19C416.54,403.9 423.83,400.52 433.67,398.67C453.28,394.99 465.85,383.46 470.65,364.74C472.71,356.69 472,349.22 468.05,337.28C461.75,318.23 465.57,304.68 481.15,290.87C494.9,278.68 500.12,266.04 497.91,250.33C496.29,238.88 492.42,232.17 481.06,221.15C467.93,208.42 465.5,203.54 465.5,190C465.5,182.58 465.98,179.26 467.71,174.83C468.92,171.71 470.42,166.27 471.03,162.74C474.03,145.52 463.74,125.52 447.93,117.82C444.9,116.35 438.62,114.39 433.96,113.46C413.02,109.27 403.19,99.75 398.85,79.43C395.11,61.91 387.77,51.44 374.74,45.06C362.45,39.04 352.86,38.82 336.61,44.19C328.86,46.76 326.38,47.14 319.98,46.78C308.3,46.12 302.54,42.99 290.85,30.94C277.95,17.64 272,14.67 257.5,14.29C251.2,14.12 246.02,14.51 243.5,15.35"
        private const val VIEWPORT_SIZE = 512f
    }

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val composePath = PathParser().parsePathString(PATH_DATA).toPath()
        val matrix = Matrix().apply {
            scale(size.width / VIEWPORT_SIZE, size.height / VIEWPORT_SIZE)
        }
        composePath.transform(matrix)
        return Outline.Generic(composePath)
    }
}

val MiniPlayerScallopedShape = MiniPlayerArtworkShape()
