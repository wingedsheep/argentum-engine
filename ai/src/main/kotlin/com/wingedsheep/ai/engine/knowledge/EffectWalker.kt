package com.wingedsheep.ai.engine.knowledge

import com.wingedsheep.engine.handlers.effects.composite.asConditional
import com.wingedsheep.engine.handlers.effects.composite.asMayDecide
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.effects.CreateDelayedTriggerEffect
import com.wingedsheep.sdk.scripting.effects.CreateTokenBatchEffect
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.ConditionalOnCollectionEffect
import com.wingedsheep.sdk.scripting.effects.ConniveEffect
import com.wingedsheep.sdk.scripting.effects.LearnEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.GatedEffect
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.effects.SacrificeTargetEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetRequirement

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
 *  - **Named keyword actions that wrap a composite.** [ConniveEffect] and [LearnEffect] are a name
 *    around an ordinary pipeline, so the walk descends into the body. Without this the wrapper
 *    would hide the pipeline's `DrawCardsEffect` from every conniving or learning card and silently
 *    move their ratings and intent tags — the wrapper was introduced after this walk existed, and
 *    descending is what keeps the numbers identical to the pre-wrapper composite.
 *  - **Everything else is a leaf**, including [ConditionalOnCollectionEffect] (which
 *    [payoffLeaves] and the rater's fold open onto both branches), [com.wingedsheep.sdk.scripting.effects.ModalEffect]
 *    modes, `ForEach` bodies and pipeline stages. Each consumer opens those in its own leaf instead:
 *    the rater folds a modal spell's best modes and a `ForEach` body and prices a pipeline by the
 *    analyzer's reading of it, and [CardIntentAnalyzer] reads them in `tagsOf` and by reading the script's
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
        if (effect is CreateTokenBatchEffect) return f.composite(effect.groups.map { fold(it, f) })
        if (effect is ConniveEffect) return fold(effect.body, f)
        if (effect is LearnEffect) return fold(effect.body, f)
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

    /**
     * One effect slot on a card: the root of an effect tree, where it was found, and the target
     * requirements declared beside it (what [RemovalBreadth] reads to tell Murder from Execute).
     */
    data class Slot(
        val effect: Effect,
        val origin: Origin,
        val targets: List<TargetRequirement> = emptyList(),
    )

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
        script.spellEffect?.let { add(Slot(it, Origin.SPELL, script.targetRequirements)) }
        script.triggeredAbilities.forEach {
            add(Slot(it.effect, Origin.TRIGGERED, listOfNotNull(it.targetRequirement) + it.additionalTargetRequirements))
        }
        script.activatedAbilities
            .filterNot { it.isManaAbility }
            .forEach { add(Slot(it.effect, Origin.ACTIVATED, it.targetRequirements)) }
    }

    /**
     * Flatten [effect] to its leaves. Gate and composite structure is discarded, so this is the
     * right reader for a consumer that only asks "does this card do X anywhere?" — which is exactly
     * what an intent tag is.
     */
    fun leaves(effect: Effect): List<Effect> = fold(effect, LeafCollector)

    /**
     * [leaves], with every gate the walk stops at opened onto its payoff: the `then` and
     * `otherwise` of "you may pay {E}. If you do, …" ([com.wingedsheep.sdk.scripting.effects.Gate.MayPay]),
     * "you may discard a card. If you do, …" ([com.wingedsheep.sdk.scripting.effects.Gate.DoAction]),
     * a pay-X or pay-life gate, and a "you may …, otherwise …" choice. A pipeline's "if those cards
     * include …" branch ([ConditionalOnCollectionEffect]) opens the same way, both sides: whether it
     * fires depends on what the earlier steps gathered, but what it does is the card's payoff.
     *
     * The gate's own cost or action is left out on purpose: discarding or sacrificing to pay for
     * the payoff is the price, not what the ability is for, and reading it would call Pia's
     * "you may discard a card" a discard spell. A gate someone else decides ([GatedEffect.decisionMaker],
     * the punisher shape — "its owner may put it on top of their library") stays a leaf: which branch
     * happens is the opponent's call, so neither branch is what the card does.
     *
     * The static readers that ask what a card is *for* ([CardIntentAnalyzer], [TargetPolarityAnalyzer])
     * read this walk. [leaves] keeps stopping at the gate, because widening it would move the rater's
     * numbers — the same reason it stops at modes and `ForEach` bodies.
     */
    fun payoffLeaves(effect: Effect): List<Effect> = leaves(effect).flatMap { leaf ->
        when {
            leaf is ConditionalOnCollectionEffect ->
                listOfNotNull(leaf.ifNotEmpty, leaf.ifEmpty).flatMap { payoffLeaves(it) }
            leaf !is GatedEffect || leaf.decisionMaker != null -> listOf(leaf)
            else -> listOfNotNull(leaf.then, leaf.otherwise).flatMap { payoffLeaves(it) }
        }
    }

    /**
     * What a delayed trigger does when it fires, as the payoff walk reads it: [payoffLeaves] of its
     * effect, minus the steps that only take back what the scheduling ability itself handled.
     *
     * "Put a creature card from your hand onto the battlefield; sacrifice it at the beginning of the
     * next end step" (Through the Breach), "return it to its owner's hand at end of turn" (Ilharg),
     * "exile it at the beginning of the next end step" after reanimating a target (Coalstoke
     * Gearhulk): the delayed step aims at "it" — a creature a pipeline of the ability moved, the
     * entity a `ForEach` around it is iterating, a permanent tapped to pay its cost, or anything
     * [siblings] (the other payoff steps of the scheduling ability) put onto the battlefield — and
     * is the price of the ability, not an answer. A "you sacrifice it" can only ever take our own
     * permanent, and a move off the battlefield that the same delayed effect moves back is a blink
     * (Long River Lurker). What remains is the delayed payoff: the draw at the next upkeep, the
     * blocker destroyed at end of combat.
     */
    fun firedLeaves(delayed: CreateDelayedTriggerEffect, siblings: List<Effect> = emptyList()): List<Effect> {
        val fired = payoffLeaves(delayed.effect)
        val putOntoBattlefield = (siblings + fired)
            .filter { it is MoveToZoneEffect && it.destination == Zone.BATTLEFIELD }
            .mapTo(HashSet()) { (it as MoveToZoneEffect).target }
        return fired.filterNot { takesBackItsOwn(it, putOntoBattlefield) }
    }

    private fun takesBackItsOwn(leaf: Effect, putOntoBattlefield: Set<EffectTarget>): Boolean = when (leaf) {
        is SacrificeTargetEffect -> !leaf.sacrificedByItsController || isIt(leaf.target) || leaf.target in putOntoBattlefield
        is MoveToZoneEffect -> leaf.destination != Zone.BATTLEFIELD && (isIt(leaf.target) || leaf.target in putOntoBattlefield)
        else -> false
    }

    /** "It": an object the scheduling ability already handled, bound before the delayed trigger fires. */
    private fun isIt(target: EffectTarget): Boolean =
        target is EffectTarget.PipelineTarget || target is EffectTarget.TappedAsCost ||
            target == EffectTarget.IterationEntity

    private object LeafCollector : Fold<List<Effect>> {
        override fun leaf(effect: Effect): List<Effect> = listOf(effect)
        override fun composite(parts: List<List<Effect>>): List<Effect> = parts.flatten()
        override fun conditional(thenValue: List<Effect>, elseValue: List<Effect>?): List<Effect> =
            thenValue + elseValue.orEmpty()

        override fun may(thenValue: List<Effect>): List<Effect> = thenValue
    }
}
