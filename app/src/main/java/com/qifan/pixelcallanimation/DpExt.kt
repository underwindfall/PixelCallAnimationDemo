package com.qifan.pixelcallanimation

import android.content.Context


fun Context.pxToDp(px: Float): Float = px / resources.displayMetrics.density


fun Context.dpToPx(dp: Float): Float = dp * resources.displayMetrics.density