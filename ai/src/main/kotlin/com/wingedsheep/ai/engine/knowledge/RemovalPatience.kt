package com.wingedsheep.ai.engine.knowledge

import com.wingedsheep.ai.engine.evaluation.BoardPresence
import com.wingedsheep.ai.engine.isOpponentTo
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.model.EntityId

/**
 * "Is this creature worth the removal spell?" — the half of the hold question a *window* cannot
 * answer.
 *
 * [HoldPolicy] asks whether this is the right moment; this asks whether this is the right target,
 * and it is the reason the AI would otherwise Pacifism the first 1/1 that shows up. A one-ply
 * evaluator scores the board right after the removal resolves, sees an opposing creature gone, and
 * has no term at all for the option the card *was*. The removal therefore fires at the first legal
 * target on the board, every game, whatever it is.
 *
 * ## Why the previous attempt failed, and what is different here
 *
 * Phase 6 built this as a **constant** — "instant removal in our own main phase, −2.0" — measured
 * it, and removed it. `HoldPolicy`'s own KDoc records the verdict: a penalty large enough to change
 * behaviour also vetoed casting a Disenchant at the one artifact on the table, which is the exact
 * play card knowledge exists to enable. The diagnosis in
 * `backlog/engine-ai-improvement.md` § Phase 6 is that holding removal is *a preference between two
 * futures*, and a constant cannot price one.
 *
 * That diagnosis is right about a constant and wrong about the question. The mistake is not "the AI
 * casts removal in its main phase" — it is "the AI casts removal at a **target that is not worth a
 * card**", which is a comparison, not a preference, and it has an answer that scales:
 *
 * > A removal spell should answer a creature at least as expensive as itself. The penalty is what
 * > the target is *short* of that bar, priced at the rate the evaluator already prices board value.
 *
 * A 1/1 under a Murder is 2.8 points short and takes a real penalty. A 3/3 is exactly fair and
 * takes none. A 6/4 is a bargain and takes none. Nothing here fires on a target the removal
 * genuinely wants, so the Disenchant that killed the constant is untouched twice over — it is not
 * a creature, and [discount] declines on non-creature targets by construction.
 *
 * ## The releases
 *
 * Patience that never ends is just a dead card. [Patience.factorFor] owns when it ends — we are
 * dying, the hand is full, the game has moved on — and [CounterPatience] releases on exactly the
 * same three. What is local to this file is the *bar*.
 *
 * Short of those, the discount is only ever a nudge the evaluator is free to outvote, and on the
 * numbers it does: it is capped at `1.4 × manaValue × boardPresence` — about 6 for a three-mana
 * removal spell — while `ThreatAssessment` pays 10.0 raw for being dead on board and
 * `LifeDifferential` prices the life on top of that. That is what keeps the pressure cases below
 * outright lethal honest without a second threshold to tune.
 *
 * Every number below is a hand-set prior, in the same sense as `CardIntent.staticPriorValue` — and
 * carried on [com.wingedsheep.ai.engine.AiProfile.holdRemovalForBetterTargets], so the arena can
 * price the whole idea at one flag.
 */
object RemovalPatience {

    /**
     * How much to subtract from a removal spell's leaf score for pointing at a target that is not
     * worth it yet, in raw evaluator units. `0.0` whenever the question does not apply — which is
     * most of the time, and every early return below says which case it is.
     *
     * @param card the removal spell being cast. Its [CardComponent.manaValue] sets the bar.
     * @param targets the *materialized* targets the AI would submit, not the requirement list.
     * @param boardPresenceWeight the profile's `EvaluationWeights.boardPresence`, so the discount is
     *   quoted in the same currency as the board value it is comparing against.
     */
    fun discount(
        state: GameState,
        playerId: EntityId,
        intent: CardIntent,
        card: CardComponent,
        targets: List<ChosenTarget>,
        boardPresenceWeight: Double,
    ): Double {
        // A sweeper's worth is the whole board it answers, and it names no targets to judge.
        if (IntentTag.SWEEPER in intent.tags) return 0.0
        // A fight's cost is a creature of ours taking the damage back, which the leaf score already
        // prices, and its reach is the fighter's power rather than anything about the card.
        if (IntentTag.FIGHT in intent.tags) return 0.0
        // A body with a removal rider (Flametongue Kavu) is not a card spent on the removal — you
        // keep the 4/2. Only a card whose *whole* purchase is the answer is making this trade.
        if (card.isCreature) return 0.0
        if (intent.tags.none { it in ANSWERED_BY_ONE_CARD }) return 0.0

        val projected = state.projectedState
        val victim = targets
            .filterIsInstance<ChosenTarget.Permanent>()
            .map { it.entityId }
            .filter { id -> projected.getController(id)?.let { state.isOpponentTo(it, playerId) } == true }
            // Exactly one, deliberately. A spell pointing at two of their permanents is answering a
            // board rather than making a trade, and one aimed at none of them is not removal being
            // spent at all (an Aura we are using as our own pump).
            .singleOrNull()
            ?: return 0.0

        // Creatures only. A creature is the thing a *better one* replaces next turn, which is the
        // entire bet being made here. An opposing artifact or enchantment is a fixed, already-visible
        // quantity — there is nothing better coming for the Disenchant to wait for, and penalizing it
        // is precisely the regression that killed the Phase 6 attempt (`noncreature-01`).
        if (!projected.isCreature(victim)) return 0.0

        val patience = Patience.factorFor(state, projected, playerId)
        if (patience <= 0.0) return 0.0

        val victimCard = state.getEntity(victim)?.get<CardComponent>() ?: return 0.0
        val worth = BoardPresence.permanentValue(state, projected, victim, victimCard)
        val fairTrade = Patience.FAIR_TRADE_VALUE_PER_MANA * card.manaValue
        return boardPresenceWeight * patience * (fairTrade - worth).coerceAtLeast(0.0)
    }

    /** The intents that spend one whole card to answer one permanent. */
    private val ANSWERED_BY_ONE_CARD = setOf(
        IntentTag.REMOVAL, IntentTag.EXILE_REMOVAL, IntentTag.NEUTRALIZE,
    )
}
