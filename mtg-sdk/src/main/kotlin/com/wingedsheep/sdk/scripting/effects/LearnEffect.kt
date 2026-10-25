package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.scripting.text.TextReplacer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * "Learn" (CR 701.48) — the named keyword action, wrapping the pipeline that carries it out.
 *
 * Learn itself stays a composition: [body] is the discard-to-draw / Lesson-from-outside-the-game
 * pipeline that [com.wingedsheep.sdk.dsl.MechanicPatterns.learn] builds, and the engine executes it
 * unchanged. This wrapper exists only to give that pipeline a *name*, which replacement needs and a
 * bare `Composite` cannot provide: "if you would learn, you may instead …" (Retriever Phoenix) is a
 * [com.wingedsheep.sdk.scripting.ReplaceKeywordAction] over
 * [com.wingedsheep.sdk.scripting.EventPattern.LearnEvent], and the executor has to know that a
 * learn is what's about to happen.
 *
 * The learning player is the effect's controller. This mirrors [ConniveEffect], the other keyword
 * action whose body is an SDK composition.
 *
 * @property body The learn pipeline proper.
 * @property replacementsApplied Set on the re-issue after every applicable replacement was
 *   declined, so none is offered twice (CR 614.5). Never set in card data.
 */
@SerialName("Learn")
@Serializable
data class LearnEffect(
    val body: Effect,
    val replacementsApplied: Boolean = false
) : Effect {
    override val description: String = "Learn"

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newBody = body.applyTextReplacement(replacer)
        return if (newBody !== body) copy(body = newBody) else this
    }
}
