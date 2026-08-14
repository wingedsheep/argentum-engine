package com.wingedsheep.ai.engine.knowledge

import com.wingedsheep.ai.engine.evaluation.BoardPresence
import com.wingedsheep.ai.engine.isOpponentTo
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId

/**
 * "Is this spell worth the counterspell?" — [RemovalPatience]'s question, asked about the stack.
 *
 * The AI counters the first thing it is offered for the same reason it used to Pacifism the first
 * 1/1: the leaf score sees an opposing spell gone and has no term at all for the option the counter
 * *was*. `respond-02` is the position that names it — a Counterspell spent on a Grizzly Bears while
 * the opponent still has five lands untapped and a hand to spend them from. Measured on the live
 * agent, countering there beat passing by **+1.28**, where every counter the AI *should* make in the
 * same category wins by 10 to 20. So the mistake is real and it is small, which is exactly the shape
 * a discount can fix without touching the plays that are right.
 *
 * ## The bar is their open mana, not the counter's cost
 *
 * [RemovalPatience] bets that a better target will show up in some future turn, which is a bet a
 * constant can only approximate. A counterspell's bar is sharper, and it is visible on the table:
 *
 * > A counterspell should answer the best spell the opponent can still cast **this turn**. What they
 * > can still cast is bounded by the mana they have left, and holding costs us nothing — our own
 * > mana stays up either way.
 *
 * So the bar is `1.4 × their untapped lands`, at the same [Patience.FAIR_TRADE_VALUE_PER_MANA] rate
 * the removal bar uses, and the discount is what the spell in front of us falls *short* of it. An
 * opponent who tapped out has no better spell coming and no discount at all — which is why every
 * other counterspell position in the suite (`respond-01`, `-03`, `-04`, `-06`, all of them cast by a
 * tapped-out opponent) is untouched by construction rather than by the size of a number.
 *
 * ## What the spell is worth is what it *is*, not what it cost
 *
 * Pricing the countered spell at its mana value would close `respond-02` just as well and would be a
 * worse model: it would tell the AI to let a two-mana 5/5 resolve, which is the commonest real
 * position where countering a cheap spell is right. So the worth comes from
 * [BoardPresence.spellValue] — the same scale the evaluator prices the battlefield on, read off the
 * spell's printed body or its [CardIntent] prior.
 *
 * That is also what makes the anthem case come out right, and it is the case that motivated using a
 * *board* value rather than a static prior: "creatures you control get +1/+1" cast into an empty
 * board is worth almost nothing and should be allowed to resolve; the same card cast by a player
 * with ten creatures prices out above any bar and gets countered.
 *
 * **Instants and sorceries are declined outright**, by the same reasoning that makes
 * [RemovalPatience] decline on non-creature permanents: their worth *is* what they do to the board,
 * which the leaf score already simulates, so there is no option value for a prior to add. That is
 * the whole of `respond-03` (Wrath) and `respond-04` (Murder), answered before any arithmetic runs.
 *
 * Carried on [com.wingedsheep.ai.engine.AiProfile.holdCountersForBetterSpells], so the arena can
 * price the whole idea at one flag.
 */
object CounterPatience {

    /**
     * How much to subtract from a counterspell's leaf score for pointing at a spell that is not
     * worth it yet, in raw evaluator units. `0.0` whenever the question does not apply — which is
     * most of the time, and every early return below says which case it is.
     *
     * @param intent the *counterspell's* intent. Only [IntentTag.COUNTERSPELL] reaches the bar.
     * @param targets the *materialized* targets the AI would submit, not the requirement list.
     * @param boardPresenceWeight the profile's `EvaluationWeights.boardPresence`, so the discount is
     *   quoted in the same currency as the board value it is comparing against.
     */
    fun discount(
        state: GameState,
        playerId: EntityId,
        intent: CardIntent,
        targets: List<ChosenTarget>,
        intents: IntentCatalog,
        boardPresenceWeight: Double,
    ): Double {
        if (IntentTag.COUNTERSPELL !in intent.tags) return 0.0

        // Exactly one, deliberately, for the same reason [RemovalPatience] insists on it: a spell
        // answering two things at once is answering a stack rather than making a trade.
        val spellId = targets.filterIsInstance<ChosenTarget.Spell>().singleOrNull()?.spellEntityId
            ?: return 0.0
        val spell = state.getEntity(spellId) ?: return 0.0
        val casterId = spell.get<SpellOnStackComponent>()?.casterId ?: return 0.0
        if (!state.isOpponentTo(casterId, playerId)) return 0.0
        val card = spell.get<CardComponent>() ?: return 0.0

        val projected = state.projectedState
        // Null is "not a question this can answer" — an instant, a sorcery, or a permanent this
        // agent cannot read. See [BoardPresence.spellValue].
        val worth = BoardPresence.spellValue(projected, card, casterId, intents) ?: return 0.0

        // Patience is a bet that something better is coming, and an empty hand is the one case where
        // we can *see* that nothing is. Hand size is public information (CR 400.2), unlike its
        // contents, so this costs the fair-play line nothing.
        if (state.getZone(casterId, Zone.HAND).isEmpty()) return 0.0

        val patience = Patience.factorFor(state, projected, playerId)
        if (patience <= 0.0) return 0.0

        val bar = Patience.FAIR_TRADE_VALUE_PER_MANA * openMana(state, projected, casterId)
        return boardPresenceWeight * patience * (bar - worth).coerceAtLeast(0.0)
    }

    /**
     * How much more the caster can still spend this turn — the size of the best spell still to come,
     * and the release that costs nothing to state: an opponent who tapped out scores `0.0` here, so
     * the bar collapses and the counter is spent on what is actually in front of us.
     *
     * Untapped **lands**, the same thing `Tempo` counts and the same approximation
     * `BoardPresence.landSequencing` makes: a Sol Ring or a mana creature is not counted, and
     * neither are colours, cost reductions or floating mana. This term only has to say "they can
     * clearly still deploy something bigger" and a `ManaSolver` run per evaluated candidate is not
     * affordable.
     */
    private fun openMana(state: GameState, projected: ProjectedState, casterId: EntityId): Int =
        projected.getBattlefieldControlledBy(casterId)
            .filter { projected.hasType(it, "LAND") }
            .count { state.getEntity(it)?.has<TappedComponent>() != true }
}
