package com.curbos.pos.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

// Custom Icons matching the Web Admin Aesthetic (Tabler/GameIcons style)

val Icons.Filled.Taco: ImageVector
    get() = _taco ?: ImageVector.Builder(
            name = "Filled.Taco",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) { // Tint overrides this anyway
                moveTo(2.0f, 12.0f)
                curveTo(2.0f, 6.5f, 6.5f, 2.0f, 12.0f, 2.0f)
                curveTo(17.5f, 2.0f, 22.0f, 6.5f, 22.0f, 12.0f)
                verticalLineTo(14.0f)
                horizontalLineTo(2.0f)
                verticalLineTo(12.0f)
                close()
                // Filling inside
                moveTo(4.0f, 13.0f)
                horizontalLineTo(20.0f)
                curveTo(20.0f, 9.0f, 16.5f, 5.5f, 12.0f, 5.5f)
                curveTo(7.5f, 5.5f, 4.0f, 9.0f, 4.0f, 13.0f)
                close()
            }
        }.build().also { _taco = it }

private var _taco: ImageVector? = null

val Icons.Filled.Soda: ImageVector
    get() = _soda ?: ImageVector.Builder(
            name = "Filled.Soda",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(5.0f, 11.0f)
                horizontalLineTo(19.0f)
                verticalLineTo(8.0f)
                horizontalLineTo(5.0f)
                close()
                moveTo(17.5f, 11.0f)
                lineTo(16.0f, 21.0f)
                horizontalLineTo(8.0f)
                lineTo(6.5f, 11.0f)
                close()
                moveTo(15.0f, 5.0f)
                verticalLineTo(3.0f)
                horizontalLineTo(17.0f) // Straw tip
                verticalLineTo(8.0f) // Straw connection
                horizontalLineTo(15.0f)
                close()
            }
        }.build().also { _soda = it }

private var _soda: ImageVector? = null

val Icons.Filled.Shirt: ImageVector
    get() = _shirt ?: ImageVector.Builder(
            name = "Filled.Shirt",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(15.0f, 4.0f)
                lineTo(21.0f, 6.0f)
                verticalLineTo(11.0f)
                horizontalLineTo(18.0f)
                verticalLineTo(19.0f)
                curveTo(18.0f, 19.55f, 17.55f, 20.0f, 17.0f, 20.0f)
                horizontalLineTo(7.0f)
                curveTo(6.45f, 20.0f, 6.0f, 19.55f, 6.0f, 19.0f)
                verticalLineTo(11.0f)
                horizontalLineTo(3.0f)
                verticalLineTo(6.0f)
                lineTo(9.0f, 4.0f)
                curveTo(9.0f, 4.0f, 10.0f, 6.0f, 12.0f, 6.0f)
                curveTo(14.0f, 6.0f, 15.0f, 4.0f, 15.0f, 4.0f)
                close()
            }
        }.build().also { _shirt = it }

private var _shirt: ImageVector? = null

val Icons.Filled.IceCreamCone: ImageVector
    get() = _iceCream ?: ImageVector.Builder(
            name = "Filled.IceCreamCone",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12.0f, 2.0f)
                curveTo(8.13f, 2.0f, 5.0f, 5.13f, 5.0f, 9.0f)
                curveTo(5.0f, 9.7f, 5.1f, 10.37f, 5.29f, 11.0f)
                horizontalLineTo(18.7f) // Cone top
                curveTo(18.9f, 10.37f, 19.0f, 9.7f, 19.0f, 9.0f)
                curveTo(19.0f, 5.13f, 15.87f, 2.0f, 12.0f, 2.0f)
                close()
                moveTo(12.0f, 22.0f)
                lineTo(6.5f, 12.0f)
                horizontalLineTo(17.5f)
                lineTo(12.0f, 22.0f)
                close()
            }
        }.build().also { _iceCream = it }

private var _iceCream: ImageVector? = null
