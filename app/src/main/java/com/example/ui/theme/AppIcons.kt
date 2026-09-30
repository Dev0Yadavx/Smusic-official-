package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object AppIcons {

    /**
     * Material Symbols Outlined 24dp - Download
     * https://fonts.gstatic.com/render/v1/Material+Symbols+Outlined/24dp/download.kt?var=opsz,wght,FILL,GRAD,ROND@24,400,0,0,50
     */
    val Download: ImageVector
        get() {
            if (_download != null) {
                return _download!!
            }
            _download = ImageVector.Builder(
                name = "download",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f
            ).apply {
                path(
                    fill = SolidColor(Color.Black),
                    fillAlpha = 1f,
                    stroke = null,
                    strokeAlpha = 1f,
                    strokeLineWidth = 1f,
                    strokeLineCap = StrokeCap.Butt,
                    strokeLineJoin = StrokeJoin.Bevel,
                    strokeLineMiter = 1f,
                    pathFillType = PathFillType.Companion.NonZero
                ) {
                    moveTo(12f, 16f)
                    lineTo(7f, 11f)
                    lineTo(8.4f, 9.55f)
                    lineToRelative(2.6f, 2.6f)
                    verticalLineTo(4f)
                    horizontalLineToRelative(2f)
                    verticalLineToRelative(8.15f)
                    lineToRelative(2.6f, -2.6f)
                    lineTo(17f, 11f)
                    lineToRelative(-5f, 5f)
                    close()
                    moveTo(6f, 20f)
                    quadTo(5.18f, 20f, 4.59f, 19.41f)
                    reflectiveQuadTo(4f, 18f)
                    verticalLineTo(15f)
                    horizontalLineTo(6f)
                    verticalLineToRelative(3f)
                    horizontalLineTo(18f)
                    verticalLineTo(15f)
                    horizontalLineToRelative(2f)
                    verticalLineToRelative(3f)
                    quadToRelative(0f, 0.82f, -0.59f, 1.41f)
                    reflectiveQuadTo(18f, 20f)
                    horizontalLineTo(6f)
                    close()
                }
            }.build()
            return _download!!
        }
    private var _download: ImageVector? = null

    /**
     * Material Symbols Outlined 24dp - Download For Offline
     * https://fonts.gstatic.com/render/v1/Material+Symbols+Outlined/24dp/download_for_offline.kt?var=opsz,wght,FILL,GRAD,ROND@24,400,0,0,50
     */
    val DownloadForOffline: ImageVector
        get() {
            if (_downloadForOffline != null) {
                return _downloadForOffline!!
            }
            _downloadForOffline = ImageVector.Builder(
                name = "download_for_offline",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f
            ).apply {
                path(
                    fill = SolidColor(Color.Black),
                    fillAlpha = 1f,
                    stroke = null,
                    strokeAlpha = 1f,
                    strokeLineWidth = 1f,
                    strokeLineCap = StrokeCap.Butt,
                    strokeLineJoin = StrokeJoin.Bevel,
                    strokeLineMiter = 1f,
                    pathFillType = PathFillType.Companion.NonZero
                ) {
                    moveTo(7f, 17f)
                    horizontalLineTo(17f)
                    verticalLineTo(15f)
                    horizontalLineTo(7f)
                    verticalLineToRelative(2f)
                    close()
                    moveToRelative(5f, -3f)
                    lineToRelative(4f, -4f)
                    lineTo(14.6f, 8.6f)
                    lineTo(13f, 10.15f)
                    verticalLineTo(6f)
                    horizontalLineTo(11f)
                    verticalLineToRelative(4.15f)
                    lineTo(9.4f, 8.6f)
                    lineTo(8f, 10f)
                    lineToRelative(4f, 4f)
                    close()
                    moveToRelative(0f, 8f)
                    quadTo(9.93f, 22f, 8.1f, 21.21f)
                    quadTo(6.28f, 20.43f, 4.93f, 19.08f)
                    quadTo(3.58f, 17.73f, 2.79f, 15.9f)
                    reflectiveQuadTo(2f, 12f)
                    quadTo(2f, 9.92f, 2.79f, 8.1f)
                    quadTo(3.58f, 6.27f, 4.93f, 4.93f)
                    quadTo(6.28f, 3.57f, 8.1f, 2.79f)
                    quadTo(9.93f, 2f, 12f, 2f)
                    reflectiveQuadToRelative(3.9f, 0.79f)
                    reflectiveQuadToRelative(3.17f, 2.14f)
                    quadToRelative(1.35f, 1.35f, 2.14f, 3.17f)
                    quadTo(22f, 9.92f, 22f, 12f)
                    reflectiveQuadToRelative(-0.79f, 3.9f)
                    reflectiveQuadToRelative(-2.14f, 3.17f)
                    quadToRelative(-1.35f, 1.35f, -3.17f, 2.14f)
                    reflectiveQuadTo(12f, 22f)
                    close()
                    moveToRelative(0f, -2f)
                    quadToRelative(3.35f, 0f, 5.68f, -2.32f)
                    reflectiveQuadTo(20f, 12f)
                    reflectiveQuadTo(17.68f, 6.32f)
                    reflectiveQuadTo(12f, 4f)
                    reflectiveQuadTo(6.33f, 6.32f)
                    reflectiveQuadTo(4f, 12f)
                    reflectiveQuadToRelative(2.33f, 5.68f)
                    reflectiveQuadTo(12f, 20f)
                    close()
                    moveToRelative(0f, -8f)
                    close()
                }
            }.build()
            return _downloadForOffline!!
        }
    private var _downloadForOffline: ImageVector? = null

    /**
     * Material Symbols Outlined 24dp - Skip Previous
     * https://fonts.gstatic.com/render/v1/Material+Symbols+Outlined/24dp/skip_previous.kt?var=opsz,wght,FILL,GRAD,ROND@24,400,0,0,50
     */
    val SkipPrevious: ImageVector
        get() {
            if (_skipPrevious != null) {
                return _skipPrevious!!
            }
            _skipPrevious = ImageVector.Builder(
                name = "skip_previous",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            ).apply {
                path(
                    fill = SolidColor(Color.Black),
                    fillAlpha = 1f,
                    stroke = null,
                    strokeAlpha = 1f,
                    strokeLineWidth = 1f,
                    strokeLineCap = StrokeCap.Butt,
                    strokeLineJoin = StrokeJoin.Bevel,
                    strokeLineMiter = 1f,
                    pathFillType = PathFillType.Companion.NonZero,
                ) {
                    moveTo(5.5f, 18f)
                    verticalLineTo(6f)
                    horizontalLineToRelative(2f)
                    verticalLineTo(18f)
                    horizontalLineToRelative(-2f)
                    close()
                    moveToRelative(13f, 0f)
                    lineToRelative(-9f, -6f)
                    lineToRelative(9f, -6f)
                    verticalLineTo(18f)
                    close()
                    moveToRelative(-2f, -6f)
                    close()
                    moveToRelative(0f, 2.25f)
                    verticalLineTo(9.75f)
                    lineTo(13.1f, 12f)
                    lineToRelative(3.4f, 2.25f)
                    close()
                }
            }.build()
            return _skipPrevious!!
        }
    private var _skipPrevious: ImageVector? = null

    /**
     * Material Symbols Outlined 24dp - Skip Next
     * https://fonts.gstatic.com/render/v1/Material+Symbols+Outlined/24dp/skip_next.kt?var=opsz,wght,FILL,GRAD,ROND@24,400,0,0,50
     */
    val SkipNext: ImageVector
        get() {
            if (_skipNext != null) {
                return _skipNext!!
            }
            _skipNext = ImageVector.Builder(
                name = "skip_next",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            ).apply {
                path(
                    fill = SolidColor(Color.Black),
                    fillAlpha = 1f,
                    stroke = null,
                    strokeAlpha = 1f,
                    strokeLineWidth = 1f,
                    strokeLineCap = StrokeCap.Butt,
                    strokeLineJoin = StrokeJoin.Bevel,
                    strokeLineMiter = 1f,
                    pathFillType = PathFillType.Companion.NonZero,
                ) {
                    moveTo(16.5f, 18f)
                    verticalLineTo(6f)
                    horizontalLineToRelative(2f)
                    verticalLineTo(18f)
                    horizontalLineToRelative(-2f)
                    close()
                    moveToRelative(-11f, 0f)
                    verticalLineTo(6f)
                    lineToRelative(9f, 6f)
                    lineToRelative(-9f, 6f)
                    close()
                    moveToRelative(2f, -6f)
                    close()
                    moveToRelative(0f, 2.25f)
                    lineTo(10.9f, 12f)
                    lineTo(7.5f, 9.75f)
                    verticalLineToRelative(4.5f)
                    close()
                }
            }.build()
            return _skipNext!!
        }
    private var _skipNext: ImageVector? = null
}
