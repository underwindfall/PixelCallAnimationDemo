package com.qifan.pixelcallanimation

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "Call"
        private const val BOUNCE_ANIMATION_DELAY: Long = 167
        private const val SWIPE_TO_DECLINE_FADE_IN_DELAY_MILLIS: Long = 333
        private const val ANIMATE_DURATION_SHORT_MILLIS: Long = 667
        private const val ANIMATE_DURATION_LONG_MILLIS: Long = 1_500
        private const val ANIMATE_DURATION_NORMAL_MILLIS: Long = 1_333
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
//        startBounceAnimation()
    }


//    private fun startEntryAnimation() {
//        Log.d(TAG, "====bounce animation start====")
//        val animatorSet = AnimatorSet()
//
//        val animationCallUp = ObjectAnimator.ofFloat(
//            incoming_call_puck_icon,
//            View.TRANSLATION_Y,
//            dpToPx(400f),
//            dpToPx(-12f)
//        )
//        animationCallUp.duration = ANIMATE_DURATION_LONG_MILLIS
//        animationCallUp.interpolator = PathInterpolatorCompat.create(0f, 0f, 0f, 1f)
//
//
//        val animationCallDown = ObjectAnimator.ofFloat(
//            incoming_call_puck_icon,
//            View.TRANSLATION_Y,
//            dpToPx(-12f),
//            0f
//        )
//        animationCallDown.duration = ANIMATE_DURATION_NORMAL_MILLIS
//        animationCallDown.interpolator = FastOutSlowInInterpolator()
//
//        animatorSet.play(animationCallUp)
//
//        animatorSet.play(animationCallDown).after(animationCallUp)
//
//        animatorSet.start()
//    }
//
//
//    private fun startBounceAnimation() {
//        val textOffSet = dpToPx(42f)
//        val swipeTextUp = ObjectAnimator.ofFloat(
//            incoming_swipe_to_answer_text,
//            View.TRANSLATION_Y,
//            0f,
//            -textOffSet
//        )
//        swipeTextUp.interpolator = FastOutSlowInInterpolator()
//        swipeTextUp.duration = ANIMATE_DURATION_NORMAL_MILLIS
//
//        val swipeTextDown = ObjectAnimator.ofFloat(
//            incoming_swipe_to_answer_text,
//            View.TRANSLATION_Y,
//            -textOffSet,
//            0f
//        )
//        swipeTextDown.interpolator = FastOutSlowInInterpolator()
//        swipeTextDown.duration = ANIMATE_DURATION_NORMAL_MILLIS
//
//
//        val rejectTextShow = ObjectAnimator.ofFloat(incoming_swipe_to_reject_text, View.ALPHA, 1f)
//        rejectTextShow.interpolator = LinearOutSlowInInterpolator()
//        rejectTextShow.duration = ANIMATE_DURATION_SHORT_MILLIS
//        rejectTextShow.startDelay = SWIPE_TO_DECLINE_FADE_IN_DELAY_MILLIS
//
//        val rejectTextHide = ObjectAnimator.ofFloat(incoming_swipe_to_reject_text, View.ALPHA, 0f)
//        rejectTextHide.interpolator = FastOutLinearInInterpolator()
//        rejectTextHide.duration = ANIMATE_DURATION_SHORT_MILLIS
//
//
//        incoming_call_puck_icon.translationY = 0f
//        val animatorSet = AnimatorSet()
//        val callOffset = dpToPx(42f)
//        val curveInterpolator = PathInterpolatorCompat.create(0.4f, 0f, 0f, 1f)
//        val callUp =
//            ObjectAnimator.ofFloat(incoming_call_puck_icon, View.TRANSLATION_Y, -callOffset)
//        callUp.interpolator = curveInterpolator
//        callUp.duration = ANIMATE_DURATION_LONG_MILLIS
//
//        val scale = 1.1f
//        val callUpScale = ObjectAnimator.ofPropertyValuesHolder(
//            incoming_call_puck_icon,
//            PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, scale),
//            PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, scale)
//        )
//        callUpScale.duration = ANIMATE_DURATION_NORMAL_MILLIS
//        callUpScale.interpolator = curveInterpolator
//
//        val callDown = ObjectAnimator.ofFloat(incoming_call_puck_icon, View.TRANSLATION_Y, 0f)
//        callDown.interpolator = FastOutSlowInInterpolator()
//        callDown.duration = ANIMATE_DURATION_NORMAL_MILLIS
//        val callDownScale = ObjectAnimator.ofPropertyValuesHolder(
//            incoming_call_puck_icon,
//            PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, scale),
//            PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, scale)
//        )
//        callDownScale.duration = ANIMATE_DURATION_NORMAL_MILLIS
//        callDownScale.interpolator = FastOutSlowInInterpolator()
//
//
//        animatorSet
//            .play(swipeTextUp)
//            .with(rejectTextHide)
//            .with(callUp)
//            .with(callUpScale)
//            .after(167)
//
//        animatorSet
//            .play(swipeTextDown)
//            .with(callDown)
//            .with(callDownScale)
//            .with(rejectTextShow)
//            .after(callUp)
//
//        animatorSet.addListener(object : AnimatorListenerAdapter() {
//            override fun onAnimationEnd(animation: Animator?) {
//                super.onAnimationEnd(animation)
//                animatorSet.start()
//            }
//        })
//
//        animatorSet.start()
//    }

}