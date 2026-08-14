package com.wingedsheep.ai.engine.knowledge

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId

/**
 * "Why are you casting that on your own turn?" — the flash creature half of [HoldPolicy].
 *
 * A one-ply evaluator scores the board right after the spell resolves, and a 3/4 flier scores the
 * same there whenever it lands. So a Restoration Angel in hand beats passing in our own precombat
 * main by roughly its whole board value — measured on the live agent at **+4.06** — and the AI
 * jams it, throwing away the only thing flash was ever printed for: the option to hold the mana,
 * see what the opponent does, and ambush an attacker.
 *
 * ## Why this is a floor and not a discount
 *
 * [RemovalPatience] and [CounterPatience] are discounts, and [HoldPolicy]'s removal branch explains
 * at length why the *window* half of the same idea was built, measured and removed: holding removal
 * is a preference between two futures, and the constant large enough to change behaviour was large
 * enough to veto casting the removal at all.
 *
 * Neither argument transfers here, for two reasons that are worth keeping apart:
 *
 *  1. **A bonus on the good window cannot fix this.** The removal branch rewards the end step, which
 *     works because the comparison the AI makes *at* the end step is the one being corrected. Here
 *     the mistake happens in our own main phase, where the comparison is "cast now vs. pass now" and
 *     a bonus that exists three steps later is invisible. Removal survives that asymmetry because
 *     removal held is still removal; a flash creature dumped in main one has already spent the
 *     entire thing being paid for.
 *  2. **The claim is provable, not preferential.** Casting a flash permanent now is dominated by
 *     casting the identical spell at the next free window *unless the permanent does something in
 *     between* — and the list of things it could do is finite and readable off its [CardIntent].
 *     That is exactly the standard [TimingVerdict.NoWindow] sets: structurally certain, never a
 *     preference. So the verdict says the honest thing — whatever the simulation reports, this is
 *     not better than passing — instead of picking a number to lose an argument with.
 *
 * ## What "does something in between" means
 *
 * Four ways a permanent cashes in before the window we would otherwise hold it for, and each maps
 * to something [CardIntent] already carries:
 *
 *  - **It attacks**, or taps for an ability — [CardIntent.hasHaste], which is the only one of the
 *    four that is not a tag.
 *  - **Its ETB changes a combat** — removal, a tapper, a pump, an anthem, granted evasion.
 *  - **Its ETB hands us a resource we can still spend this turn** — a card drawn, a tutor, a land.
 *  - **Something is on the stack** that we might be responding to, which is a window this policy has
 *    no business overruling.
 *
 * [PAYS_OFF_BEFORE_THE_AMBUSH] and [ANSWERS_THEIR_BOARD] are the middle two, split because the
 * second needs a question the tag cannot answer — *whose* permanents does it point at. The tags
 * deliberately in neither are the ones that come out identical a step later: a Blood token, a life
 * total, an opponent's discard, a sacrifice outlet with nothing to feed it yet. A flash permanent
 * the analyzer reads as doing none of them — an ambush bear, a Pack Guardian, or a Restoration
 * Angel whose blink only ever touches our own board — is pure body, and pure body is what this is
 * for.
 *
 * ## The releases
 *
 * [Patience] owns three of them and they are inherited whole: on a turn where doing nothing loses
 * the game there is no holding anything, a hand at [com.wingedsheep.engine.core.MaximumHandSize] is
 * discarding the card anyway, and by [Patience.SPENT_BY_TURN] the bet on a better window has stopped
 * paying. That reuse is most of why this is worth writing — without it a floor this hard is a brick,
 * and with it the card cannot rot in hand.
 *
 * The fourth release is this file's own and it is [laterWindowIsStillAhead]: once the opponent has
 * declared attackers the information is in, and holding longer buys nothing.
 *
 * Carried on [com.wingedsheep.ai.engine.AiProfile.holdFlashPermanentsForAmbush].
 */
internal object AmbushWindow {

    /**
     * Whether casting this flash permanent **here** is dominated by casting it at a later free
     * window — the [TimingVerdict.NoWindow] test.
     *
     * `false` for everything that is not the narrow shape this reasons about, which is most cards
     * and every card at all on a profile with the flag off.
     *
     * @param intent the *candidate's* intent. Only a [CardIntent.flashPermanent] reaches any of it.
     */
    fun holds(state: GameState, playerId: EntityId, intent: CardIntent): Boolean {
        if (!intent.flashPermanent) return false

        // It can attack, or tap for something, the moment it lands. That is a real payoff this turn
        // and it is the whole reason flash-and-haste is a printed combination.
        if (intent.hasHaste) return false

        // Something to respond to is a window in its own right, and one this cannot read well
        // enough to overrule — the same "silence is not a veto" line [HoldPolicy.responseWindowFor]
        // takes about a stack object it cannot identify.
        if (state.stack.isNotEmpty()) return false

        if (intent.tags.any { it in PAYS_OFF_BEFORE_THE_AMBUSH }) return false

        // An ETB that answers *their* board wants to happen before we attack. One that points only
        // at our own — a blink, a self-bounce — is worth exactly the same a step later, which is
        // Restoration Angel and is the position that started this. The tag alone cannot tell them
        // apart; see [CardIntent.targetsOnlyOurPermanents] for why.
        if (intent.tags.any { it in ANSWERS_THEIR_BOARD } && !intent.targetsOnlyOurPermanents) {
            return false
        }

        if (!laterWindowIsStillAhead(state, playerId)) return false

        // Lethal on board, a full hand, or a game gone long — see [Patience], which owns all three
        // and is shared with the two patience bars so there is one definition of each rather than
        // three that can drift.
        return Patience.factorFor(state, state.projectedState, playerId) > 0.0
    }

    /**
     * Whether a window at least as good as this one is still coming before the card would have to be
     * cast anyway.
     *
     * **Our own turn: always.** Every step of it is worse than any step of theirs, and the reasoning
     * does not need to rank them against each other — a no-haste body deployed at any point on our
     * turn does exactly nothing until we untap, and by then we could have cast it on their turn
     * instead with a turn of their decisions in hand.
     *
     * **Their turn: until attackers are declared.** [Step.DECLARE_ATTACKERS] is the moment the
     * ambush stops being a guess, and it is the last window where a blocker can still be deployed in
     * time (CR 509.1 declares blockers in the *next* step). Everything from there on is released,
     * including [Step.END] — which is the release that matters when they do not attack at all, since
     * the engine skips the declare-attackers step entirely in that case and a policy that only
     * released there would hold the card to cleanup.
     */
    private fun laterWindowIsStillAhead(state: GameState, playerId: EntityId): Boolean =
        if (state.activePlayerId == playerId) true else state.step !in RELEASED_ON_THEIR_TURN

    /**
     * Tags whose payoff is lost, or deferred at a cost, by holding the card — whichever side of the
     * table they point at.
     *
     * Two groups, both about *this turn*:
     *
     *  - **Improves our own attack**: [IntentTag.PUMP], [IntentTag.ANTHEM],
     *    [IntentTag.EVASION_GRANT], [IntentTag.PROTECTION]. An ETB that pumps the team is worth
     *    having before we swing, and by construction it aims at our own permanents — so unlike
     *    [ANSWERS_THEIR_BOARD] there is no side-of-the-table question to ask about it.
     *  - **Hands us something to spend**: [IntentTag.DRAW], [IntentTag.TUTOR], [IntentTag.RAMP],
     *    [IntentTag.RECURSION]. A card drawn in our main phase can be cast in our main phase; the
     *    same card drawn on their end step cannot.
     *
     * [IntentTag.COUNTERSPELL] and [IntentTag.COMBAT_TRICK] are absent on purpose. They are not
     * exceptions this set forgot — [HoldPolicy] branches on both *before* it reaches here, so
     * listing them would imply a guard that lives somewhere else.
     *
     * So are [IntentTag.LIFEGAIN], [IntentTag.TOKEN_MAKER], [IntentTag.DISCARD] and
     * [IntentTag.SACRIFICE_OUTLET], and those are the deliberate inclusions in the floor: a Blood
     * token, three life, an opponent's discard and an outlet with nothing to feed it are all worth
     * exactly the same one window later.
     */
    private val PAYS_OFF_BEFORE_THE_AMBUSH = setOf(
        IntentTag.PUMP, IntentTag.ANTHEM, IntentTag.EVASION_GRANT, IntentTag.PROTECTION,
        IntentTag.DRAW, IntentTag.TUTOR, IntentTag.RAMP, IntentTag.RECURSION,
    )

    /**
     * Tags that answer something on the **opponent's** board, and so want to resolve before we
     * attack rather than after they do.
     *
     * Split out from [PAYS_OFF_BEFORE_THE_AMBUSH] because these are the tags the analyzer applies
     * to a card that only ever touches *our* permanents — a blink, a self-sacrifice, a self-bounce
     * — and telling those apart takes [CardIntent.targetsOnlyOurPermanents] rather than the tag.
     */
    private val ANSWERS_THEIR_BOARD = setOf(
        IntentTag.REMOVAL, IntentTag.EXILE_REMOVAL, IntentTag.SWEEPER, IntentTag.NEUTRALIZE,
        IntentTag.TAPPER, IntentTag.FIGHT,
    )

    /** Their turn, from the moment attackers are known — see [laterWindowIsStillAhead]. */
    private val RELEASED_ON_THEIR_TURN = setOf(
        Step.DECLARE_ATTACKERS, Step.DECLARE_BLOCKERS, Step.FIRST_STRIKE_COMBAT_DAMAGE,
        Step.COMBAT_DAMAGE, Step.END_COMBAT, Step.POSTCOMBAT_MAIN, Step.END,
    )
}
