package com.wingedsheep.ai.engine.knowledge

import com.wingedsheep.engine.handlers.effects.composite.asConditional
import com.wingedsheep.engine.handlers.effects.composite.asMayDecide
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.Effect

/**
 * The one traversal of a card's effect tree that both card scorers share.
 *
 * Two consumers want to read the same structure for different reasons:
 * [com.wingedsheep.ai.engine.LimitedCardRater] folds it into a 0–5 limited rating, and
 * [CardIntentAnalyzer] folds it into a set of [IntentTag]s. Before Phase 6 the rater owned a
 * private copy of the walk; extracting it here is what keeps the two from drifting.
 *
 * The shape it knows about is deliberately narrow — exactly what the rater walked before, so
 * moving it here changed no rating:
 *
 *  - **Gates.** `asConditional()` / `asMayDecide()` recognize the *lowered* `GatedEffect` forms of
 *    "if X, do Y" and "you may do Y" (see `ConditionalGate.kt`). Both branches are visited.
 *  - **Composites.** [CompositeEffect] children are visited in order.
 *  - **Everything else is a leaf**, including [com.wingedsheep.sdk.scripting.effects.ModalEffect]
 *    modes, `ForEach` bodies and pipeline stages. Widening the walk into those would change the
 *    rater's numbers — and therefore every generated sealed deck — so it is a deliberate follow-up
 *    rather than a free improvement. [CardIntentAnalyzer] compensates by reading the script's
 *    *structural* slots (static abilities, activation costs, triggers) directly.
 *
 * The fold is generic because the two consumers combine children differently: the rater sums with
 * a per-composite cap, the analyzer unions. One recursion, two interpretations.
 */
object EffectWalker {

    /**
     * How to combine the parts of an effect tree into a `T`.
     *
     * Implement [leaf] for the effect types you care about and the three combinators for how a
     * parent node folds its children.
     */
    interface Fold<T> {
        /** An effect with no interior this walk descends into. */
        fun leaf(effect: Effect): T

        /** A [CompositeEffect]'s children, already folded, in printed order. */
        fun composite(parts: List<T>): T

        /** "If <condition>, [thenValue]. Otherwise, [elseValue]." [elseValue] is null for no else. */
        fun conditional(thenValue: T, elseValue: T?): T

        /** "You may [thenValue]." */
        fun may(thenValue: T): T
    }

    /** Fold [effect] and everything under it with [f]. */
    fun <T> fold(effect: Effect, f: Fold<T>): T {
        effect.asConditional()?.let { branch ->
            return f.conditional(fold(branch.then, f), branch.otherwise?.let { fold(it, f) })
        }
        effect.asMayDecide()?.let { return f.may(fold(it.then, f)) }
        if (effect is CompositeEffect) return f.composite(effect.effects.map { fold(it, f) })
        return f.leaf(effect)
    }

    /**
     * Where in a card's script an effect lives, and how much a scorer should discount it for that.
     *
     * The discounts are the rater's, kept verbatim: a triggered ability is conditional on an event
     * happening, and an activated one is repeatable but costs resources every time.
     */
    enum class Origin(val discount: Double) {
        /** The spell's own effect — an instant or sorcery resolving. */
        SPELL(1.0),

        /** A triggered ability's effect. */
        TRIGGERED(0.8),

        /** A non-mana activated ability's effect. */
        ACTIVATED(0.6),
    }

    /** One effect slot on a card: the root of an effect tree plus where it was found. */
    data class Slot(val effect: Effect, val origin: Origin)

    /**
     * Every effect slot on [script] that the walk knows how to read: the spell effect, each
     * triggered ability, and each non-mana activated ability.
     *
     * Two omissions, both to keep the rater's numbers exactly what they were:
     * `stateTriggeredAbilities` (the rater never read them — [CardIntentAnalyzer] adds them on top
     * of this list itself) and mana abilities, which are the card's cost side rather than its
     * payoff and which the analyzer reads separately to recognize a mana rock.
     */
    fun slots(script: CardScript): List<Slot> = buildList {
        script.spellEffect?.let { add(Slot(it, Origin.SPELL)) }
        script.triggeredAbilities.forEach { add(Slot(it.effect, Origin.TRIGGERED)) }
        script.activatedAbilities
            .filterNot { it.isManaAbility }
            .forEach { add(Slot(it.effect, Origin.ACTIVATED)) }
    }

    /**
     * Flatten [effect] to its leaves. Gate and composite structure is discarded, so this is the
     * right reader for a consumer that only asks "does this card do X anywhere?" — which is exactly
     * what an intent tag is.
     */
    fun leaves(effect: Effect): List<Effect> = fold(effect, LeafCollector)

    private object LeafCollector : Fold<List<Effect>> {
        override fun leaf(effect: Effect): List<Effect> = listOf(effect)
        override fun composite(parts: List<List<Effect>>): List<Effect> = parts.flatten()
        override fun conditional(thenValue: List<Effect>, elseValue: List<Effect>?): List<Effect> =
            thenValue + elseValue.orEmpty()

        override fun may(thenValue: List<Effect>): List<Effect> = thenValue
    }
}
