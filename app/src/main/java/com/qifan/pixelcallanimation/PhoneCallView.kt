package com.qifan.pixelcallanimation

import android.animation.*
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.animation.Interpolator
import android.widget.FrameLayout
import androidx.core.animation.addListener
import androidx.core.animation.doOnCancel
import androidx.core.animation.doOnEnd
import androidx.core.view.animation.PathInterpolatorCompat
import androidx.interpolator.view.animation.FastOutLinearInInterpolator
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator
import com.qifan.pixelcallanimation.PhoneCallView.AnimationState.*
import kotlinx.android.synthetic.main.view_phone_call.view.incoming_swipe_to_answer_text as swipeToAnswerText
import kotlinx.android.synthetic.main.view_phone_call.view.incoming_swipe_to_reject_text as swipeToRejectText
import kotlinx.android.synthetic.main.view_phone_call.view.incoming_call_puck_container as contactPuckContainer
import kotlinx.android.synthetic.main.view_phone_call.view.incoming_call_puck_bg as contactPuckBackground
import kotlin.properties.Delegates

private const val BOUNCE_ANIMATION_DELAY: Long = 167
private const val SWIPE_TO_DECLINE_FADE_IN_DELAY_MILLIS: Long = 333
private const val ANIMATE_DURATION_SHORT_MILLIS: Long = 667
private const val ANIMATE_DURATION_LONG_MILLIS: Long = 1_500
private const val ANIMATE_DURATION_NORMAL_MILLIS: Long = 1_333
private const val HINT_REJECT_FADE_TRANSLATION_Y_DP = -8f

class PhoneCallView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var lockEntryAnim: AnimatorSet? = null
    private var lockBounceAnim: Animator? = null

    private var animationState: AnimationState by Delegates.observable(NONE) { property, oldValue, newValue ->
        debug("${property.name} is being changed from $oldValue to $newValue")
        updateAnimationState(oldValue, newValue)
    }

    init {
        View.inflate(context, R.layout.view_phone_call, this)
    }

    private fun updateAnimationState(oldState: AnimationState, newState: AnimationState) {
        if (oldState == newState) {
            debug("PhoneCall animation state doesn't change")
            return
        }
        if (oldState == COMPLETED) {
            debug("PhoneCall animation has been finished won't do anything further")
            return
        }
        when (newState) {
            ENTRY -> startSwipeToAnswerEntryAnimation()
            BOUNCE -> startSwipeToAnswerBounceAnimation()
            COMPLETED -> endAnimation()
            else -> debug("Do nothing")
        }
    }


    private fun startSwipeToAnswerEntryAnimation() {
        debug("Swipe entry animation.")
        endAnimation()
        lockEntryAnim = AnimatorSet()

        val textUp: Animator = ObjectAnimator.ofFloat(
            swipeToAnswerText,
            View.TRANSLATION_Y,
            context.dpToPx(192f /* dp */),
            context.dpToPx(-20f /* dp */)
        )
            .setDuration(ANIMATE_DURATION_NORMAL_MILLIS)
            .apply { interpolator = LinearOutSlowInInterpolator() }
        val textDown: Animator = ObjectAnimator.ofFloat(
            swipeToAnswerText,
            View.TRANSLATION_Y,
            context.dpToPx(-20f) /* dp */,
            0f /* end pos */
        )
            .setDuration(ANIMATE_DURATION_NORMAL_MILLIS)
            .apply { interpolator = FastOutSlowInInterpolator() }
        // "Swipe down to reject" text fades in with a slight translation
        swipeToRejectText.alpha = 0f
        val rejectTextShow: Animator = ObjectAnimator.ofPropertyValuesHolder(
            swipeToRejectText,
            PropertyValuesHolder.ofFloat(View.ALPHA, 1f),
            PropertyValuesHolder.ofFloat(
                View.TRANSLATION_Y,
                context.dpToPx(HINT_REJECT_FADE_TRANSLATION_Y_DP),
                0f
            )
        )
            .setDuration(ANIMATE_DURATION_SHORT_MILLIS)
            .apply {
                interpolator = FastOutSlowInInterpolator()
                startDelay = SWIPE_TO_DECLINE_FADE_IN_DELAY_MILLIS
            }

        val puckUp: Animator = ObjectAnimator.ofFloat(
            contactPuckContainer,
            View.TRANSLATION_Y,
            context.dpToPx(400f /* dp */),
            context.dpToPx(-12f /* dp */)
        )
            .setDuration(ANIMATE_DURATION_LONG_MILLIS)
            .apply {
                interpolator = PathInterpolatorCompat.create(
                    0f /* controlX1 */,
                    0f /* controlY1 */,
                    0f /* controlX2 */,
                    1f /* controlY2 */
                )
            }

        val puckDown = ObjectAnimator.ofFloat(
            contactPuckContainer,
            View.TRANSLATION_Y,
            context.dpToPx(-12f /* dp */),
            0f /* end pos */
        )
            .setDuration(ANIMATE_DURATION_NORMAL_MILLIS)
            .apply { interpolator = FastOutSlowInInterpolator() }

        val puckScaleDown = createUniformScaleAnimators(
            contactPuckBackground,
            0.33f /* beginScale */,
            1.1f /* endScale */,
            ANIMATE_DURATION_NORMAL_MILLIS,
            PathInterpolatorCompat.create(
                0.4f /* controlX1 */, 0f /* controlY1 */, 0f/* controlX2 */, 1f /* controlY2 */
            )
        )

        val puckScaleUp = createUniformScaleAnimators(
            contactPuckBackground,
            1.1f /* beginScale */,
            1f /* endScale */,
            ANIMATE_DURATION_NORMAL_MILLIS,
            FastOutSlowInInterpolator()
        )
        lockEntryAnim?.apply {
            // Upward animation chain.
            play(textUp).with(puckScaleUp).with(puckUp)
            // Downward animation chain.
            play(textDown).with(puckDown).with(puckScaleDown).after(puckUp)
            play(rejectTextShow).after(puckUp)

//         TODO Add vibration animation.
//        addVibrationAnimator(lockEntryAnim);

            var canceled = false
            addListener(
                onCancel = { canceled = true },
                onEnd = { if (!canceled) onEntryAnimationDone() }
            )
            start()
        }
    }


    private fun startSwipeToAnswerBounceAnimation() {
        debug("Swipe bounce animation.")
        endAnimation()
        lockBounceAnim = createBreatheAnimation().apply {
            doOnEnd {
                if (animationState == BOUNCE) {
                    lockBounceAnim?.start()
                }
            }
        }
        lockBounceAnim?.start()
    }

    private fun createBreatheAnimation(): Animator {
        val breatheAnimation = AnimatorSet()
        val textOffset = context.dpToPx(42f/* dp */)
        val textUp = ObjectAnimator.ofFloat(
            swipeToAnswerText,
            View.TRANSLATION_Y,
            0f /* begin pos */,
            -textOffset
        )
            .apply {
                interpolator = FastOutSlowInInterpolator()
                duration = ANIMATE_DURATION_NORMAL_MILLIS
            }
        val textDown = ObjectAnimator.ofFloat(
            swipeToAnswerText,
            View.TRANSLATION_Y,
            -textOffset,
            0f /* end pos */
        ).apply {
            interpolator = FastOutSlowInInterpolator()
            duration = ANIMATE_DURATION_NORMAL_MILLIS
        }
        // "Swipe down to reject" text fade in
        val rejectTextShow = ObjectAnimator.ofFloat(swipeToRejectText, View.ALPHA, 1f)
            .apply {
                interpolator = LinearOutSlowInInterpolator()
                duration = ANIMATE_DURATION_SHORT_MILLIS
                startDelay = SWIPE_TO_DECLINE_FADE_IN_DELAY_MILLIS
            }

        // reject hint text translate in
        val rejectTextTranslate = ObjectAnimator.ofFloat(
            swipeToRejectText,
            View.TRANSLATION_Y,
            context.dpToPx(HINT_REJECT_FADE_TRANSLATION_Y_DP),
            0f
        ).apply {
            interpolator = FastOutSlowInInterpolator()
            duration = ANIMATE_DURATION_NORMAL_MILLIS
        }
        // reject hint text fade out
        val rejectTextHide = ObjectAnimator.ofFloat(swipeToRejectText, View.ALPHA, 0f)
            .apply {
                interpolator = FastOutLinearInInterpolator()
                duration = ANIMATE_DURATION_SHORT_MILLIS
            }

        val curve = PathInterpolatorCompat.create(
            0.4f /* controlX1 */, 0f /* controlY1 */, 0f /* controlX2 */, 1f /* controlY2 */
        )
        val puckOffset = context.dpToPx(42f)
        val puckUp = ObjectAnimator.ofFloat(contactPuckContainer, View.TRANSLATION_Y, -puckOffset)
            .apply {
                interpolator = curve
                duration = ANIMATE_DURATION_LONG_MILLIS
            }

        val scale = 1.0625f
        val puckScaleUp = createUniformScaleAnimators(
            contactPuckBackground,
            1f /* beginScale */,
            scale,
            ANIMATE_DURATION_NORMAL_MILLIS,
            curve
        )

        val puckDown =
            ObjectAnimator.ofFloat(contactPuckContainer, View.TRANSLATION_Y, 0f /* end pos */)
                .apply {
                    interpolator = FastOutSlowInInterpolator()
                    duration = ANIMATE_DURATION_NORMAL_MILLIS
                }
        val puckScaleDown = createUniformScaleAnimators(
            contactPuckBackground,
            scale,
            1f/* endScale */,
            ANIMATE_DURATION_NORMAL_MILLIS,
            FastOutSlowInInterpolator()
        )
        // Bounce upward animation chain.
        breatheAnimation
            .play(textUp)
            .with(rejectTextHide)
            .with(puckUp)
            .with(puckScaleUp)
            .after(167 /* delay */)

        // Bounce downward animation chain.
        breatheAnimation
            .play(puckDown)
            .with(textDown)
            .with(puckScaleDown)
            .with(rejectTextShow)
            .with(rejectTextTranslate)
            .after(puckUp)
        // TODO Add vibration animation to the animator set.
//        addVibrationAnimator(breatheAnimation)
        return breatheAnimation
    }

    private fun onEntryAnimationDone() {
        debug("onEntryAnimationDone Swipe entry anim ends")
        if (animationState == ENTRY) {
            animationState = BOUNCE
        }
    }

    private fun endAnimation() {
        debug("Clear all animations")
        lockBounceAnim?.cancel()
        lockBounceAnim = null
        lockEntryAnim?.cancel()
        lockEntryAnim = null
    }

    // Create an animator to scale on X/Y directions uniformly.
    private fun createUniformScaleAnimators(
        target: View,
        begin: Float,
        end: Float,
        duration: Long,
        interpolator: Interpolator
    ): Animator {
        return ObjectAnimator.ofPropertyValuesHolder(
            target,
            PropertyValuesHolder.ofFloat(View.SCALE_X, begin, end),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, begin, end)
        )
            .apply {
                setDuration(duration)
                setInterpolator(interpolator)
            }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        animationState = ENTRY
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        endAnimation()
    }


    private enum class AnimationState {
        NONE,

        // Entry animation for incoming call
        ENTRY,

        // An idle state in which text and icon slightly bounces off its base repeatedly
        BOUNCE,

        // A special state in which text and icon follows the finger movement
        SWIPE,

        // A short animation to reset from swipe and prepare for hint or bounce
        SETTLE,

        // Jump animation to suggest what to do
        HINT,

        // Animation loop completed. Occurs after user swipes beyond threshold
        COMPLETED;
    }
}