package com.curbos.pos.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * CurbOS Shape Scale
 * 
 * Provides consistent corner radii across the application.
 * Use these instead of inline RoundedCornerShape() calls.
 */
object CurbOSShapes {
    /** 4.dp - Badges, chips, small indicators */
    val small = RoundedCornerShape(4.dp)
    
    /** 8.dp - Buttons, inputs, small cards */
    val medium = RoundedCornerShape(8.dp)
    
    /** 12.dp - Standard cards, elevated surfaces */
    val large = RoundedCornerShape(12.dp)
    
    /** 16.dp - Dialogs, bottom sheets, large containers */
    val extraLarge = RoundedCornerShape(16.dp)
    
    /** 24.dp - Hero sections, success dialogs */
    val xxLarge = RoundedCornerShape(24.dp)
    
    /** 50% - Pill-shaped buttons, circular badges */
    val pill = RoundedCornerShape(50)
}
