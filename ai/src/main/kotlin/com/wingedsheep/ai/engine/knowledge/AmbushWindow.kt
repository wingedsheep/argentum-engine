package com.wingedsheep.ai.engine.knowledge

import com.wingedsheep.ai.engine.CombatMath
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.stack.ActivatedAbilityOnStackComponent
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
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
 *  - **Something of theirs is on the stack** that we might be responding to, which is a window this
 *    policy has no business overruling. Our own spells and triggers are not that window.
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
    fun holds(
        state: GameState,
        playerId: EntityId,
        intent: CardIntent,
        /**
         * [com.wingedsheep.ai.engine.AiProfile.holdInstantSpeedDeploys]: judge the payoffs by
         * [CardIntent.tagsOnArrival] instead of every tag the card carries, and release the hold
         * on our own turn when holding would make it fight another instant for the same mana —
         * see [manaIdleNowButContestedLater].
         */
        readArrival: Boolean = false,
        /** The card being cast, for [manaIdleNowButContestedLater]. Null skips that release. */
        castCardId: EntityId? = null,
        intents: IntentCatalog? = null,
    ): Boolean {
        if (!intent.flashPermanent) return false

        // It can attack, or tap for something, the moment it lands. That is a real payoff this turn
        // and it is the whole reason flash-and-haste is a printed combination.
        if (intent.hasHaste) return false

        // Something to respond to is a window in its own right, and one this cannot read well
        // enough to overrule — the same "silence is not a veto" line [HoldPolicy.responseWindowFor]
        // takes about a stack object it cannot identify. Only *their* objects count: see
        // [opponentObjectOnTheStack].
        if (opponentObjectOnTheStack(state, playerId)) return false

        // Under [readArrival], only what the card can cash in this turn: a loot on attacking is
        // no reason to land a no-haste body early — see [CardIntent.arrivalTags].
        val tags = if (readArrival) intent.tagsOnArrival else intent.tags
        // Both payoffs below are "before we attack", and in our own upkeep or draw step our main
        // phase is still ahead of the attack: casting there is the same payoff with a card more
        // seen, so neither releases the hold yet (a Little Bear flashed in on our own upkeep).
        val mainPhaseAhead = state.activePlayerId == playerId && state.step in BEFORE_OUR_MAIN
        if (!mainPhaseAhead && tags.any { it in PAYS_OFF_BEFORE_THE_AMBUSH }) return false

        // An ETB that answers *their* board wants to happen before we attack. One that points only
        // at our own — a blink, a self-bounce — is worth exactly the same a step later, which is
        // Restoration Angel and is the position that started this. The tag alone cannot tell them
        // apart; see [CardIntent.targetsOnlyOurPermanents] for why.
        //
        // Tapping is the one answer that is *only* about the attack: a blocker tapped on our turn
        // with nothing of ours able to attack is untapped again before it matters (Glamermite's
        // "tap target creature", game 6d7c6893).
        val answers = tags.filter { it in ANSWERS_THEIR_BOARD }
        val answerPaysNow = answers.any { it != IntentTag.TAPPER } || !tappedBlockerIsWasted(state, playerId)
        if (!mainPhaseAhead && answers.isNotEmpty() && answerPaysNow && !intent.targetsOnlyOurPermanents) {
            return false
        }

        if (!laterWindowIsStillAhead(state, playerId)) return false

        if (readArrival && castCardId != null && intents != null &&
            manaIdleNowButContestedLater(state, playerId, castCardId, intents)
        ) {
            return false
        }

        // Lethal on board, a full hand, or a game gone long — see [Patience], which owns all three
        // and is shared with the two patience bars so there is one definition of each rather than
        // three that can drift.
        return Patience.factorFor(state, state.projectedState, playerId) > 0.0
    }

    /**
     * The same floor for an **instant that deploys** — a token maker, or a spell that empowers a
     * planeswalker — under [com.wingedsheep.ai.engine.AiProfile.holdInstantSpeedDeploys].
     *
     * The argument is [holds]'s, moved from a flash permanent to the permanent an instant leaves
     * behind: a token body cast on our own turn does nothing until we untap, and cast on theirs it
     * is the same body plus an ambush and a turn of their decisions in hand. The misplay that asked
     * for it (2026-10-14, Reality Fracture draft, turn 6): Jace's Machinations — whose whole first
     * sentence is "activate Jace's loyalty abilities at instant speed" — cast in our own main phase
     * with nothing else to spend the mana on, its Jace activated at once and then attacked down to
     * three loyalty on the opponent's turn. Cast at their end step it is the same draw, a Jace they
     * never get to attack, and a second activation on our turn.
     *
     * Read off the card only as far as the card can say it: an instant whose every tag is a body
     * ([BODY_ONLY]) — anything that draws, pumps, removes or counters has its own branch in
     * [HoldPolicy] and is never reached here. What the body can do on arrival is read off the
     * [leaf] instead, because a token's characteristics are not on the card that makes it: see
     * [deployActsBeforeTheWindow].
     *
     * The releases are [holds]'s: an opposing stack object, their declared attack, [Patience], and
     * mana the hold would make contested.
     *
     * @param leaf the position the cast resolves to. Null — nothing to read — declines.
     */
    fun holdsInstantDeploy(
        state: GameState,
        leaf: GameState?,
        playerId: EntityId,
        intent: CardIntent,
        castCardId: EntityId,
        intents: IntentCatalog,
    ): Boolean {
        if (leaf == null) return false
        if (intent.flashPermanent || intent.speed != Speed.INSTANT) return false
        if (IntentTag.TOKEN_MAKER !in intent.tags || !intent.tags.all { it in BODY_ONLY }) return false
        if (opponentObjectOnTheStack(state, playerId)) return false
        if (!laterWindowIsStillAhead(state, playerId)) return false
        if (deployActsBeforeTheWindow(state, leaf, playerId, intent)) return false
        if (manaIdleNowButContestedLater(state, playerId, castCardId, intents)) return false
        return Patience.factorFor(state, state.projectedState, playerId) > 0.0
    }

    /**
     * Whether anything the cast put under our control — a new permanent, or a planeswalker whose
     * loyalty it raised — can **act** before the window we would hold the spell for.
     *
     *  - A creature acts only with haste; a no-haste token waits for our untap like any flash body.
     *  - A planeswalker acts at once: its loyalty abilities are ours to use in this main phase
     *    (CR 606.3). Unless the spell also lets us use them at instant speed
     *    ([CardIntent.grantsInstantSpeedLoyalty]) — then they are just as usable at the opponent's
     *    end step, and a walker cast there is one they never get to attack.
     *  - Anything else (a Treasure, a Clue, a Food) can be spent the moment it lands, and holding
     *    it buys nothing provable, so it declines.
     *
     * A token maker whose leaf shows nothing new deployed nothing to hold for, and declines too.
     */
    private fun deployActsBeforeTheWindow(
        state: GameState,
        leaf: GameState,
        playerId: EntityId,
        intent: CardIntent,
    ): Boolean {
        val ours = state.projectedState.getBattlefieldControlledBy(playerId).toSet()
        val projected = leaf.projectedState
        var deployed = false
        for (id in projected.getBattlefieldControlledBy(playerId)) {
            val fresh = id !in ours
            val empowered = !fresh && projected.isPlaneswalker(id) && loyalty(leaf, id) > loyalty(state, id)
            if (!fresh && !empowered) continue
            deployed = true
            when {
                projected.isPlaneswalker(id) -> if (!intent.grantsInstantSpeedLoyalty) return true
                projected.isCreature(id) -> if (projected.hasKeyword(id, Keyword.HASTE)) return true
                else -> return true
            }
        }
        return !deployed
    }

    private fun loyalty(state: GameState, id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    /**
     * Whether holding the card past our own turn would leave it competing for mana that casting it
     * now would not — [holds]' `readArrival` release, and [holdsInstantDeploy]'s.
     *
     * The ambush argument assumes the mana held up is free. It is not when another instant-speed
     * card in hand wants the same mana on the opponent's turn: with four lands, a held Sphinx and a
     * held Murder are one play between them, while the Sphinx cast now spends mana nothing else this
     * turn was going to use and keeps the Murder up. So on our own turn the floor stands only while
     * the mana we have can pay for this card and the cheapest other instant-speed card together.
     * It compares mana values, not colours, and errs toward releasing: a release hands the choice
     * back to the evaluator, it never forces the cast.
     */
    private fun manaIdleNowButContestedLater(
        state: GameState,
        playerId: EntityId,
        castCardId: EntityId,
        intents: IntentCatalog,
    ): Boolean {
        if (state.activePlayerId != playerId) return false
        val cost = state.getEntity(castCardId)?.get<CardComponent>()?.manaCost?.cmc ?: return false
        val available = intents.availableMana(state, playerId) ?: return false
        // An instant the mana we have cannot pay for at all is not competing for it.
        val cheapestOther = state.getZone(playerId, Zone.HAND)
            .filter { it != castCardId }
            .mapNotNull { id ->
                val card = state.getEntity(id)?.get<CardComponent>() ?: return@mapNotNull null
                if (intents.forName(card.name)?.speed != Speed.INSTANT) return@mapNotNull null
                card.manaCost.cmc
            }
            .filter { it <= available }
            .minOrNull() ?: return false
        return available < cost + cheapestOther
    }

    /**
     * Whether the stack holds a spell or ability we do not control — the one stack shape that is a
     * window to respond in.
     *
     * Our own objects are excluded because nothing about them is new information and none of them
     * is answered by a body arriving early. The position that taught this: our own upkeep, our own
     * delayed Bird-token trigger from The Eagles Are Coming! on the stack, and a bare
     * `isNotEmpty()` read it as a window — so the AI flashed in Little Bear before its draw step,
     * and the bear's "untap another target creature you control" fizzled for want of a target.
     * [ExpiringGrantWindow] learned the same lesson about its own stack test.
     *
     * A stack object whose controller cannot be read counts as theirs: declining the floor is the
     * fail-open direction, the one that leaves the decision to the evaluator.
     */
    private fun opponentObjectOnTheStack(state: GameState, playerId: EntityId): Boolean =
        state.stack.any { stackId -> controllerOfStackObject(state, stackId) != playerId }

    private fun controllerOfStackObject(state: GameState, stackId: EntityId): EntityId? {
        val container = state.getEntity(stackId) ?: return null
        container.get<SpellOnStackComponent>()?.let { return it.casterId }
        container.get<ActivatedAbilityOnStackComponent>()?.let { return it.controllerId }
        container.get<TriggeredAbilityOnStackComponent>()?.let { return it.controllerId }
        return container.get<ControllerComponent>()?.playerId
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
     * including [Step.END]. A turn where they cannot attack at all still has its declare-attackers
     * step (CR 508.8 skips only blockers and damage), so the release lands there too.
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
    /** Our own turn's steps that come before our first main phase. */
    private val BEFORE_OUR_MAIN = setOf(Step.UNTAP, Step.UPKEEP, Step.DRAW)

    /**
     * Whether tapping one of their creatures now buys nothing: it is our turn and nothing we
     * control can attack. On their turn a tap still stops an attacker, so it never is.
     */
    private fun tappedBlockerIsWasted(state: GameState, playerId: EntityId): Boolean =
        state.activePlayerId == playerId &&
            CombatMath.getCreaturesThatCanAttack(state, state.projectedState, playerId).isEmpty()

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

    /**
     * The tags of an instant whose whole payoff is what it leaves on the battlefield — see
     * [holdsInstantDeploy]. Life gained is worth the same a window later, as [holds] argues.
     */
    private val BODY_ONLY = setOf(IntentTag.TOKEN_MAKER, IntentTag.LIFEGAIN, IntentTag.CARD_SELECTION)

    /** Their turn, from the moment attackers are known — see [laterWindowIsStillAhead]. */
    private val RELEASED_ON_THEIR_TURN = setOf(
        Step.DECLARE_ATTACKERS, Step.DECLARE_BLOCKERS, Step.FIRST_STRIKE_COMBAT_DAMAGE,
        Step.COMBAT_DAMAGE, Step.END_COMBAT, Step.POSTCOMBAT_MAIN, Step.END,
    )
}
