package com.wingedsheep.ai.engine.knowledge

import com.wingedsheep.ai.engine.evaluation.EvaluationWeights
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.TargetsComponent
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId

/**
 * "Is this the window for this card?" — the timing half of Phase 6.
 *
 * A one-ply evaluator has no notion of a window. It scores the board right after the spell
 * resolves, and a combat trick cast in your own main phase scores exactly as well there as one cast
 * after blockers, because `ThreatAssessment` reads the pumped power either way. The result is an AI
 * that dumps its instants at the first opportunity.
 *
 * Before Phase 6 the only correction was one hardcoded line in `Strategist`: `passScore - 1.5` on
 * the opponent's end step, applied to *every* candidate. That is both too blunt — it *encourages*
 * dumping a pump that is about to wear off in cleanup, exactly as much as it encourages a removal
 * spell that would otherwise rot in hand — and too narrow, since it says nothing about our own main
 * phase, which is where the mistake actually happens. This replaces it with a per-card verdict
 * driven by [CardIntent].
 *
 * A [TimingVerdict.Adjust] is in the board score's own units, so a point here is a point of board
 * value. A [TimingVerdict.NoWindow] is a different kind of claim; see there.
 */
class HoldPolicy(
    private val intents: IntentCatalog,
    /**
     * [AiProfile.combatTricksWaitForBlocks][com.wingedsheep.ai.engine.AiProfile.combatTricksWaitForBlocks]
     * — narrow the combat window to the steps where blocks are already in.
     */
    private val tricksWaitForBlocks: Boolean = false,
    /**
     * [AiProfile.holdRemovalForBetterTargets][com.wingedsheep.ai.engine.AiProfile.holdRemovalForBetterTargets]
     * — charge a removal spell for pointing at a target below a fair trade. See [RemovalPatience],
     * which is where the whole idea lives.
     */
    private val holdRemovalForBetterTargets: Boolean = false,
    /**
     * [AiProfile.holdCountersForBetterSpells][com.wingedsheep.ai.engine.AiProfile.holdCountersForBetterSpells]
     * — charge a counterspell for answering a spell smaller than what the caster can still deploy.
     * See [CounterPatience].
     */
    private val holdCountersForBetterSpells: Boolean = false,
    /**
     * [AiProfile.cashCantripsInTheEndStep][com.wingedsheep.ai.engine.AiProfile.cashCantripsInTheEndStep]
     * — hand an instant-speed draw spell the end-step window this policy already hands removal.
     */
    private val cashCantripsInTheEndStep: Boolean = false,
    /**
     * [AiProfile.holdFlashPermanentsForAmbush][com.wingedsheep.ai.engine.AiProfile.holdFlashPermanentsForAmbush]
     * — stop deploying a flash creature on our own turn when the ambush window is still ahead. See
     * [AmbushWindow], which is where the whole idea lives.
     */
    private val holdFlashPermanentsForAmbush: Boolean = false,
    /**
     * [AiProfile.holdExpiringGrantsForCombat][com.wingedsheep.ai.engine.AiProfile.holdExpiringGrantsForCombat]
     * — stop paying for an activated ability whose whole payoff expires at cleanup, in a window
     * where nothing can spend it. See [ExpiringGrantWindow], which is where the whole idea lives.
     */
    private val holdExpiringGrantsForCombat: Boolean = false,
    /**
     * The profile's `EvaluationWeights.boardPresence`, so [RemovalPatience] can quote its discount
     * in the same currency as the board value it compares against. The default is the compiled
     * fallback's, which is what every profile that does not opt in would have used anyway.
     */
    private val boardPresenceWeight: Double = EvaluationWeights.DEFAULT.boardPresence,
) {

    /** Whether this policy can say anything. False when the agent has no card knowledge. */
    val isEnabled: Boolean get() = intents.isEnabled

    /**
     * The policy's reading of casting [cardName] here — the window it is in, plus what it is being
     * pointed at.
     *
     * The two halves are deliberately asymmetric about speed. **Windows** only judge instant-speed
     * cards: a sorcery has no window to wait for, and penalizing one would just make the AI pass
     * with a full hand. **[RemovalPatience]** judges any removal at all, because "is this creature
     * worth a card?" is the same question for a Pacifism as for a Doom Blade — and the Aura is the
     * case that motivated it.
     *
     * @param cast the materialized spell, when this action is one. Null for an activated ability,
     *   which spends no card and so is never charged for patience.
     * @param activation the materialized activation, when this action is one. The window half below
     *   reads the *card*, which for an activation is its source permanent — a different and much
     *   broader question than what the ability does, and the reason [activationVerdictFor] exists.
     */
    fun verdictFor(
        state: GameState,
        playerId: EntityId,
        cardName: String,
        cast: CastSpell? = null,
        activation: ActivateAbility? = null,
    ): TimingVerdict {
        // Asked first, and it can only ever return [TimingVerdict.NoWindow] or [TimingVerdict.
        // Neutral], so with the flag off — or on any action that is not an activation — everything
        // below is reached exactly as it was.
        activationVerdictFor(state, playerId, cardName, activation)
            .takeIf { it is TimingVerdict.NoWindow }
            ?.let { return it }

        val intent = intents.forName(cardName) ?: return TimingVerdict.Neutral

        val window = windowVerdictFor(state, playerId, intent)
        // A card that accomplishes nothing here is already floored below passing; there is no
        // target trade left to price on top of that.
        if (window is TimingVerdict.NoWindow) return window

        val patience = patienceFor(state, playerId, intent, cast)
        if (patience <= 0.0) return window

        val windowDelta = (window as? TimingVerdict.Adjust)?.delta ?: 0.0
        return TimingVerdict.Adjust(windowDelta - patience, reason = "patience")
    }

    /**
     * The patience discount for [cast], or `0.0` when the flags are off or the shape doesn't fit.
     *
     * The two bars are summed rather than chosen between, which costs nothing: each declines on the
     * other's shape — [RemovalPatience] wants a single opposing *permanent* target and
     * [CounterPatience] a single opposing *spell* — so at most one of them is ever non-zero.
     */
    private fun patienceFor(
        state: GameState,
        playerId: EntityId,
        intent: CardIntent,
        cast: CastSpell?,
    ): Double {
        if (cast == null) return 0.0
        val card = state.getEntity(cast.cardId)?.get<CardComponent>() ?: return 0.0

        val removal = if (holdRemovalForBetterTargets) {
            RemovalPatience.discount(state, playerId, intent, card, cast.targets, boardPresenceWeight)
        } else {
            0.0
        }
        val counter = if (holdCountersForBetterSpells) {
            CounterPatience.discount(state, playerId, intent, cast.targets, intents, boardPresenceWeight)
        } else {
            0.0
        }
        return removal + counter
    }

    /**
     * The window half for an **activated ability**, which the card-driven one below cannot ask.
     *
     * Kept as its own entry point rather than as a branch in [windowVerdictFor] because the two
     * read different objects. That one reads the card, and for an activation the card is the source
     * permanent — [CardIntentAnalyzer] types anything with a non-mana activated ability as
     * [Speed.ACTIVATED], which the window half declines outright, so no branch there has ever seen
     * an activation. This reads the *ability*: what it does, how long it lasts, and whether it can
     * be paid for again later.
     *
     * A null lookup — the flag off, no card knowledge, or an ability granted by something other
     * than the card itself — is "no information" and returns [TimingVerdict.Neutral], leaving the
     * leaf score in charge exactly as before.
     */
    private fun activationVerdictFor(
        state: GameState,
        playerId: EntityId,
        cardName: String,
        activation: ActivateAbility?,
    ): TimingVerdict {
        if (!holdExpiringGrantsForCombat || activation == null) return TimingVerdict.Neutral
        val ability = intents.activatedAbility(cardName, activation.abilityId)
            ?: return TimingVerdict.Neutral
        return if (ExpiringGrantWindow.holds(state, playerId, ability, intents)) TimingVerdict.NoWindow
        else TimingVerdict.Neutral
    }

    /** The window half — "is this the moment?", which only an instant-speed card can get wrong. */
    private fun windowVerdictFor(state: GameState, playerId: EntityId, intent: CardIntent): TimingVerdict {
        if (intent.speed != Speed.INSTANT) return TimingVerdict.Neutral

        val stackHasSomething = state.stack.isNotEmpty()

        return when {
            // A counterspell with nothing to counter is not a play, it is a discard.
            IntentTag.COUNTERSPELL in intent.tags && !stackHasSomething -> TimingVerdict.NoWindow

            // A pump wears off at cleanup, so it is worth casting only when something will use it
            // before then: a fight in combat, or a spell on the stack it can save the creature
            // from — which is [responseWindowFor]'s question, not "is the stack non-empty".
            // Anywhere else — our own main phase, and specifically the opponent's end step, where
            // the old blanket `passScore - 1.5` discount actively *encouraged* dumping it — it buys
            // nothing at all.
            IntentTag.COMBAT_TRICK in intent.tags -> when {
                state.step in combatWindow -> TimingVerdict.Adjust(COMBAT_WINDOW)
                else -> responseWindowFor(state, playerId, intent)
            }

            // A permanent with flash, deployed where the ambush window is still ahead of us.
            //
            // This is the one branch that returns a verdict on the *wrong* window rather than a
            // bonus on the right one, and [AmbushWindow] carries the argument for why the removal
            // branch below does not transfer: a bonus three steps later cannot correct a decision
            // made now, and unlike "hold the removal" the claim here is provable.
            //
            // It sits **above** the removal branch on purpose, and the reason is a bug it has to
            // route around. `hitsAnotherPermanent` reads "does this take someone else's permanent
            // off the battlefield" off the effect target alone, which for a bound target carries no
            // filter and falls through to true — so Restoration Angel, whose ETB blinks a creature
            // *we control*, is tagged `REMOVAL, EXILE_REMOVAL`. Below the removal branch this
            // branch would be unreachable for the exact card it was written for.
            //
            // Ceding to it costs nothing, because [AmbushWindow]'s own guard is strictly the more
            // precise of the two: it asks the same question *and* which side of the table the
            // targeting points at. A flash creature that really does answer their board fails that
            // guard, this branch's condition goes false, and the removal branch below picks it up
            // unchanged. Nothing without flash reaches here at all.
            holdFlashPermanentsForAmbush && AmbushWindow.holds(state, playerId, intent) ->
                TimingVerdict.NoWindow

            // Instant-speed removal: reward the windows that are strictly better than now, and
            // charge nothing for casting it early.
            //
            // The symmetric *window* penalty — "hold it, our own main phase is the wrong time" — is
            // what the plan proposed, and it was built, measured and removed. Holding removal is a
            // preference between two futures, not a provable loss, and a constant cannot price one:
            // the one large enough to change behaviour was large enough to veto casting the removal
            // at all, which is the exact blindness this phase exists to fix.
            //
            // What survives that verdict is [RemovalPatience], and the difference is what it
            // charges *for*. Not the window — the **target**, by how far it falls short of a fair
            // trade, so a 1/1 is charged and the artifact the Disenchant is aimed at is not charged
            // at all. It is applied outside this `when`, to sorcery-speed removal too.
            intent.tags.any { it in REMOVAL_TAGS } -> when {
                stackHasSomething -> TimingVerdict.Adjust(RESPONSE_WINDOW)
                isOpponentEndStep(state, playerId) -> TimingVerdict.Adjust(END_STEP_WINDOW)

                else -> TimingVerdict.Neutral
            }

            // The same end-step window, for the other card that is strictly better cast there.
            //
            // A cantrip's own board value is about zero — a card drawn against a card spent — so on
            // a tie the AI passes, and it passed *every* window, right through to cleanup, where the
            // mana it was saving evaporates. Casting at the opponent's end step costs nothing this
            // spell was going to use and buys a turn of extra information about what to keep.
            //
            // Phase 6 used to get this for free from `Strategist`'s blanket `passScore - 1.5` at the
            // end step, and correctly switched it off for every agent with card knowledge: the same
            // constant also paid for dumping a pump that expires in cleanup. That is `instants-06`,
            // and it stays answered — an expiring pump is [IntentTag.COMBAT_TRICK] and is caught by
            // the branch above this one, whatever else it draws. What is restored here is only the
            // half of the old constant that was right, which is why this is a window on a tag rather
            // than a discount on a step.
            //
            // No symmetric penalty for casting a cantrip in our own main phase, for the reason the
            // removal branch gives at length: rewarding the better window is a comparison, and
            // charging the worse one is a preference a constant cannot price.
            cashCantripsInTheEndStep && IntentTag.DRAW in intent.tags &&
                isOpponentEndStep(state, playerId) -> TimingVerdict.Adjust(END_STEP_WINDOW)

            else -> TimingVerdict.Neutral
        }
    }

    /** The last window before our own turn — the deadline every "hold it" argument runs out at. */
    private fun isOpponentEndStep(state: GameState, playerId: EntityId): Boolean =
        state.activePlayerId != playerId && state.step == Step.END

    /**
     * What a spell already on the stack is worth to a pump — the "last chance" window.
     *
     * A stack object is not by itself a reason to cast a trick. The bonus was flat for any
     * non-empty stack, which paid the AI to answer a Murder with Giant Growth (a 5/5 is destroyed
     * exactly as fast as a 2/2) and to pump a 6/4 that was already walking off a Bolt. Both are
     * `lastchance` puzzles, and both are the *same* mistake: reading "there is a deadline" as "this
     * card meets it".
     *
     * So the window has to be earned. It is real when something on the stack would kill a creature
     * we control **by size** — damage, or -N/-N — and the extra toughness carries it out of range.
     * That is one comparison, and it separates the three positions the suite pairs: Bolt on a 2/2
     * (dying, +3/+3 saves it → window), Bolt on a 6/4 (not dying → nothing to buy), Murder on
     * anything (no reach, so no amount of toughness answers it).
     *
     * **Silence is not a veto.** [TimingVerdict.NoWindow] floors the candidate below passing, which
     * is only honest where "does nothing" is structurally certain, so a stack object this policy
     * cannot read — an unknown card, or a fight, whose reach is the other creature's power and not
     * a property of the card — keeps the old bonus rather than earning a veto. Which makes it
     * load-bearing that [IntentCatalog.forStackObject] reads *abilities* too, since a trigger on the
     * stack is the commonest stack object there is.
     */
    private fun responseWindowFor(state: GameState, playerId: EntityId, pump: CardIntent): TimingVerdict {
        val projected = state.projectedState
        var unreadable = false

        for (stackId in state.stack) {
            val container = state.getEntity(stackId) ?: continue
            val threat = intents.forStackObject(container)
            if (threat == null || IntentTag.FIGHT in threat.tags) {
                unreadable = true
                continue
            }

            // A sweeper names no targets; everything we control is under it.
            val victims = if (IntentTag.SWEEPER in threat.tags) {
                projected.getBattlefieldControlledBy(playerId)
            } else {
                container.get<TargetsComponent>()?.targets.orEmpty()
                    .mapNotNull { (it as? ChosenTarget.Permanent)?.entityId }
                    .filter { projected.getController(it) == playerId }
            }

            // Null reach on a card we *did* read is destruction, exile or bounce — see
            // [CardIntent.removalReach]. Toughness is no defence against any of them.
            val reach = threat.removalReach ?: continue
            if (victims.any { savedByPump(state, projected, it, reach, pump) }) {
                return TimingVerdict.Adjust(RESPONSE_WINDOW)
            }
        }

        return if (unreadable) TimingVerdict.Adjust(RESPONSE_WINDOW) else TimingVerdict.NoWindow
    }

    /**
     * Whether [creature] is dying to [reach] damage and [pump] is enough to change that.
     *
     * Both halves are load-bearing: a creature already out of range buys nothing (there is nothing
     * to save), and neither does one the pump cannot lift out of range (a +1/+0 trick against three
     * damage). Damage already marked counts, because that is what the state-based action will
     * compare against (CR 704.5g).
     */
    private fun savedByPump(
        state: GameState,
        projected: ProjectedState,
        creature: EntityId,
        reach: Int,
        pump: CardIntent,
    ): Boolean {
        if (!projected.isCreature(creature)) return false
        val toughness = projected.getToughness(creature) ?: return false
        val remaining = toughness - (state.getEntity(creature)?.get<DamageComponent>()?.amount ?: 0)
        return remaining <= reach && remaining + pump.pumpToughness > reach
    }

    /**
     * The steps where a combat trick is worth its bonus.
     *
     * [COMBAT_STEPS] is the historical answer and it is one window too wide: it pays the trick in
     * `BEGIN_COMBAT` and `DECLARE_ATTACKERS`, both of which are *before* blocks. Spending a trick
     * there is worse than spending it late for a reason no board evaluation can see — it hands the
     * defender the information. A 2/2 that would have gone unblocked gets chump-blocked once it is
     * visibly a 5/5, and the pump that was exactly lethal buys nothing.
     *
     * [BLOCKS_IN_STEPS] is the constant matching [COMBAT_WINDOW]'s own comment. Every step in it is
     * one where blocks are already declared, whichever side we are on: on our turn we only receive
     * priority in `DECLARE_BLOCKERS` after the defender has declared, and on theirs we are the one
     * who just declared. Before that a trick falls through to [responseWindowFor], which pays it
     * only for something on the stack it can actually answer — so "hold it until blocks are in"
     * costs the AI no legitimate response.
     */
    private val combatWindow: Set<Step> get() = if (tricksWaitForBlocks) BLOCKS_IN_STEPS else COMBAT_STEPS

    private companion object {
        /** Blockers are in; a trick decides the fight. */
        const val COMBAT_WINDOW = 1.0

        /** Something on the stack this card can actually answer — see [responseWindowFor]. */
        const val RESPONSE_WINDOW = 1.0

        /**
         * Their end step: the last moment holding it is still free. Replaces the blanket
         * `passScore - 1.5`, but only for the cards that actually want the window.
         */
        const val END_STEP_WINDOW = 1.5

        val COMBAT_STEPS = setOf(
            Step.BEGIN_COMBAT, Step.DECLARE_ATTACKERS, Step.DECLARE_BLOCKERS,
            Step.FIRST_STRIKE_COMBAT_DAMAGE, Step.COMBAT_DAMAGE,
        )

        /** [COMBAT_STEPS] minus the two windows that come before blocks are declared. */
        val BLOCKS_IN_STEPS = setOf(
            Step.DECLARE_BLOCKERS, Step.FIRST_STRIKE_COMBAT_DAMAGE, Step.COMBAT_DAMAGE,
        )

        val REMOVAL_TAGS = setOf(IntentTag.REMOVAL, IntentTag.EXILE_REMOVAL, IntentTag.SWEEPER)
    }
}

/** What [HoldPolicy] makes of casting a particular card in a particular window. */
sealed interface TimingVerdict {
    /** Timing says nothing here; the board score stands as it is. */
    data object Neutral : TimingVerdict

    /**
     * A nudge in the board score's own units.
     *
     * [reason] is for the local testing mode's decision panel and nothing else: a candidate the AI
     * passed over despite a strong board score is only explicable if the panel can name which half
     * of the policy moved it. Null means the window half, which is what an `Adjust` used to be.
     */
    data class Adjust(val delta: Double, val reason: String? = null) : TimingVerdict

    /**
     * The card **cannot accomplish anything** in this window — a counterspell with an empty stack,
     * a pump that will wear off before any combat.
     *
     * This is a floor, not a penalty, and the difference matters. A penalty is a constant racing an
     * unbounded number, and it loses: `ThreatAssessment` reads a Giant Growth's +3/+3 as a
     * permanently faster clock and pays about 10 points for it, so no defensible literal could
     * outvote it. A verdict says the thing worth saying instead — *whatever the simulation reports,
     * this is not better than passing* — and the Strategist scores it just below the pass score.
     * Reserved for cases where "does nothing" is structurally certain, never for a preference.
     */
    data object NoWindow : TimingVerdict
}
