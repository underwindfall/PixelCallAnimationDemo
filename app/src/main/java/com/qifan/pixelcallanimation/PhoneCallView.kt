package com.qifan.pixelcallanimation

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Interpolator
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.animation.addListener
import androidx.core.animation.doOnCancel
import androidx.core.animation.doOnEnd
import androidx.core.math.MathUtils.clamp
import androidx.core.view.animation.PathInterpolatorCompat
import androidx.interpolator.view.animation.FastOutLinearInInterpolator
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator
import com.qifan.pixelcallanimation.PhoneCallView.AnimationState.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.properties.Delegates
import kotlinx.android.synthetic.main.view_phone_call.view.incoming_call_puck_bg as contactPuckBackground
import kotlinx.android.synthetic.main.view_phone_call.view.incoming_call_puck_container as contactPuckContainer
import kotlinx.android.synthetic.main.view_phone_call.view.incoming_call_puck_icon as contactPuckIcon
import kotlinx.android.synthetic.main.view_phone_call.view.incoming_swipe_to_answer_text as swipeToAnswerText
import kotlinx.android.synthetic.main.view_phone_call.view.incoming_swipe_to_reject_text as swipeToRejectText

private const val BOUNCE_ANIMATION_DELAY: Long = 167
private const val SWIPE_TO_DECLINE_FADE_IN_DELAY_MILLIS: Long = 333
private const val VIBRATION_TIME_MILLIS: Long = 500
private const val ANIMATE_DURATION_SHORT_MILLIS: Long = 667
private const val ANIMATE_DURATION_LONG_MILLIS: Long = 1_500
private const val ANIMATE_DURATION_NORMAL_MILLIS: Long = 1_333
private const val HINT_REJECT_FADE_TRANSLATION_Y_DP = -8f
private const val SHAKE_TRANSLATION_RIGHT = 10f
private const val SHAKE_TRANSLATION_LEFT = -10f
private const val SETTLE_ANIMATION_DURATION_MILLIS: Long = 100

private const val SWIPE_LERP_PROGRESS_FACTOR = 0.5f
private const val SWIPE_TO_ANSWER_MAX_TRANSLATION_Y_DP = 90f
private const val SWIPE_TO_REJECT_MAX_TRANSLATION_Y_DP = 90f

class PhoneCallView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var lockEntryAnim: AnimatorSet? = null
    private var lockSettleAnim: AnimatorSet? = null
    private var lockBounceAnim: Animator? = null
    private var vibrationAnimator: Animator? = null

    private var animationState: AnimationState by Delegates.observable(NONE) { property, oldValue, newValue ->
        debug("${property.name} is being changed from $oldValue to $newValue")
        updateAnimationState(oldValue, newValue)
    }


    private var downY = 0f
    private var offsetY = 0f
    private var slideProgress = 0f

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
            SWIPE -> startSwipeToAnswerSwipeAnimation()
            SETTLE -> startSwipeToAnswerSettleAnimation()
            COMPLETED -> clearSwipeToAnswerUi()
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

            addVibrationAnimator(this)

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

    private fun startSwipeToAnswerSwipeAnimation() {
        debug("Swipe answer animation.")
        resetTouchState()
        endAnimation()
    }

    private fun startSwipeToAnswerSettleAnimation() {
        endAnimation()
        val puckScale = ObjectAnimator.ofPropertyValuesHolder(
            contactPuckBackground,
            PropertyValuesHolder.ofFloat(View.SCALE_X, 1f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f)
        ).apply {
            duration = SETTLE_ANIMATION_DURATION_MILLIS
        }
        val swipeToAnswerTextFade =
            createFadeAnimation(swipeToAnswerText, 1f, SETTLE_ANIMATION_DURATION_MILLIS)
        val contactPuckContainerFade =
            createFadeAnimation(contactPuckContainer, 1f, SETTLE_ANIMATION_DURATION_MILLIS)
        val contactPuckBackgroundFade =
            createFadeAnimation(contactPuckBackground, 1f, SETTLE_ANIMATION_DURATION_MILLIS)
        val contactPuckIconFade =
            createFadeAnimation(contactPuckIcon, 1f, SETTLE_ANIMATION_DURATION_MILLIS)
        val contactPuckTranslation = ObjectAnimator.ofPropertyValuesHolder(
            contactPuckContainer,
            PropertyValuesHolder.ofFloat(View.TRANSLATION_X, 0f),
            PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, 0f)
        ).apply {
            duration = SETTLE_ANIMATION_DURATION_MILLIS
        }
        lockSettleAnim = AnimatorSet()
        lockSettleAnim?.apply {
            play(puckScale)
                .with(swipeToAnswerTextFade)
                .with(contactPuckContainerFade)
                .with(contactPuckBackgroundFade)
                .with(contactPuckIconFade)
                .with(contactPuckTranslation)
            addListener(
                onCancel = { animationState = NONE },
                onEnd = { animationState = BOUNCE }
            )
            start()
        }
    }

    private fun createFadeAnimation(
        target: View,
        targetAlpha: Float,
        duration: Long
    ): ObjectAnimator {
        return ObjectAnimator.ofFloat(target, View.ALPHA, targetAlpha)
            .setDuration(duration)
    }

    private fun updateSwipeTextAndPuckForTouch() {
        val clampedProgress = clamp(slideProgress, -1f, 1f)
        debug("updateSwipeTextAndPuckForTouch $clampedProgress")
        // Cancel view property animators on views we're about to mutate
        swipeToAnswerText.animate().cancel()
        contactPuckIcon.animate().cancel()

        // Fade out the "swipe up to answer". It only takes 1 slot to complete the fade.
        val swipeTextAlpha = max(0f, 1 - abs(clampedProgress))
        fadeToward(swipeToAnswerText, swipeTextAlpha)
        // Fade out the "swipe down to dismiss" at the same time. Don't ever increase its alpha
        fadeToward(swipeToRejectText, min(swipeTextAlpha, swipeToRejectText.alpha))
        // Move swipe text back to zero.
        if (slideProgress > 0) {
            //reject animation
            moveTowardY(
                contactPuckContainer,
                clampedProgress * context.dpToPx(SWIPE_TO_ANSWER_MAX_TRANSLATION_Y_DP)
            )
        } else {
            //answer animation
            moveTowardY(
                contactPuckContainer,
                clampedProgress * context.dpToPx(SWIPE_TO_REJECT_MAX_TRANSLATION_Y_DP)
            )
        }

    }

    private fun moveTowardY(view: View, newY: Float) {
        val newTransY = view.translationY + (newY - view.translationY) * SWIPE_LERP_PROGRESS_FACTOR
        view.translationY = newTransY
    }

    private fun fadeToward(view: View, newAlpha: Float) {
        val lastAlpha = view.alpha + (newAlpha - view.alpha) * SWIPE_LERP_PROGRESS_FACTOR
        view.alpha = lastAlpha
    }

    private fun clearSwipeToAnswerUi() {
        debug("clear Swipe Animation")
        endAnimation()
        swipeToAnswerText.visibility = View.GONE
        contactPuckContainer.visibility = View.GONE
    }

    private fun resetTouchState() {
        contactPuckContainer.animate()
            .scaleX(1f /* scaleX */)
            .scaleY(1f /*scaleY*/)
        contactPuckBackground.animate()
            .scaleX(1f /* scaleX */)
            .scaleY(1f /*scaleY*/)
        contactPuckBackground.apply {
            backgroundTintList = null
        }
        contactPuckIcon.apply {
            backgroundTintList = ColorStateList.valueOf(Color.WHITE)
        }
        contactPuckIcon.animate().rotation(0f)

        swipeToAnswerText.animate().alpha(1f)
        swipeToAnswerText.animate().alpha(1f)
        contactPuckContainer.animate().alpha(1f)
        contactPuckBackground.animate().alpha(1f)
        contactPuckIcon.animate().alpha(1f)
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
        addVibrationAnimator(breatheAnimation)
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
        lockSettleAnim?.cancel()
        lockSettleAnim = null
        lockBounceAnim?.cancel()
        lockBounceAnim = null
        lockEntryAnim?.cancel()
        lockEntryAnim = null
    }

    private fun addVibrationAnimator(animatorSet: AnimatorSet) {
        vibrationAnimator?.end()
        // animate the value between 0 and 1
        vibrationAnimator =
            ObjectAnimator.ofFloat(
                contactPuckContainer, View.TRANSLATION_X,
                0f,
                SHAKE_TRANSLATION_RIGHT,
                SHAKE_TRANSLATION_LEFT,
                SHAKE_TRANSLATION_RIGHT,
                SHAKE_TRANSLATION_LEFT,
                SHAKE_TRANSLATION_RIGHT,
                SHAKE_TRANSLATION_LEFT,
                SHAKE_TRANSLATION_RIGHT,
                SHAKE_TRANSLATION_LEFT,
                0f
            )
                .apply {
                    duration = VIBRATION_TIME_MILLIS
                    interpolator = AccelerateDecelerateInterpolator()
                }
        animatorSet.play(vibrationAnimator).after(0/* delay */)
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

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downY = event.y
                animationState = SWIPE
                slideProgress = 0f
            }
            MotionEvent.ACTION_MOVE -> {
                offsetY = event.y - downY
                slideProgress = offsetY / (height * 0.5f)
                updateSwipeTextAndPuckForTouch()
                debug("Action move offsetY $slideProgress")
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (abs(offsetY) >= height * 0.5) {
                    if (offsetY > 0) {
                        performReject()
                    } else {
                        performAnswer()
                    }
                    animationState = COMPLETED
                } else {
                    resetTouchState()
                    slideProgress = 0f
                    animationState = SETTLE
                }
            }
        }
        return true
    }

    private fun performReject() {
        toast("Perform Rejecting phone call")
    }

    private fun performAnswer() {
        toast("Perform Answering phone call")
    }


    private fun toast(string: String) {
        Toast.makeText(context, string, Toast.LENGTH_LONG).show()
    }


    private enum class AnimationState {
        NONE,

        // Entry animation for incoming call
        ENTRY,

        // An idle state in which text and icon slightly bounces off its base repeatedly
        BOUNCE,

        // A special state in which text and icon follows the finger movement
        SWIPE,

        // A short animation to reset from swipe and prepare for  bounce
        SETTLE,

        // Animation loop completed. Occurs after user swipes beyond threshold
        COMPLETED;
    }
}