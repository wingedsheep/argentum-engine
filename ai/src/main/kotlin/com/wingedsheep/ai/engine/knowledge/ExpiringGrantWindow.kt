package com.wingedsheep.ai.engine.knowledge

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.TargetsComponent
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.GrantEvasionKeywordEffect
import com.wingedsheep.sdk.scripting.effects.GrantKeywordEffect
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * "Why are you paying for that now?" — the **activated ability** half of [HoldPolicy].
 *
 * [AmbushWindow] is the same argument about a card in hand, and everything below is the shape it
 * established applied one zone over. The mistake it answers is a real one, off a live game: turn 4,
 * the *opponent's* precombat main, an Olivia's Dragoon (`Discard a card: This creature gains flying
 * until end of turn`) freshly deployed and summoning sick, and the AI discards a Battleground Geist
 * to give it flying. There is no combat this turn it can use, the opponent controls no flier for it
 * to block, and the keyword is gone at cleanup. A card for nothing.
 *
 * ## Why the existing window machinery never sees it
 *
 * [HoldPolicy.verdictFor] resolves an activation to its *source permanent's* name, and
 * [CardIntentAnalyzer] types any permanent carrying a non-mana activated ability as
 * [Speed.ACTIVATED]. `windowVerdictFor` opens by declining everything that is not [Speed.INSTANT],
 * so the [IntentTag.COMBAT_TRICK] branch — whose whole subject is "a pump wears off at cleanup, so
 * it is worth casting only when something will use it before then" — is unreachable for an ability.
 * The identical mistake, printed on a creature instead of on an instant, was unmeasured.
 *
 * ## Why this is a floor and not a discount
 *
 * The same two reasons [AmbushWindow] gives, and the second is stronger here:
 *
 *  1. **A bonus on the good window cannot fix this.** The comparison being corrected happens in the
 *     wrong window, where a reward three steps later is invisible.
 *  2. **The claim is provable.** Activating now is dominated by activating the identical ability at
 *     the next window *unless something happens in between that needs it* — and unlike a card in
 *     hand there is no "it might get discarded / countered" residue to argue about, because the
 *     ability is not a resource that can be stripped. It is still there at
 *     [Step.DECLARE_ATTACKERS], at the same cost, with strictly more information.
 *
 * What the floor deliberately does **not** claim is that the grant is worthless — that is the
 * evaluator's question, and it keeps it. This says only that *here* is not where it gets asked.
 *
 * ## What "expires with nothing to spend it on" means
 *
 * Three guards, each of which must hold, and any one of which failing hands the decision straight
 * back to the leaf score:
 *
 *  - **Every payoff expires at end of turn** — see [everyPayoffExpiresThisTurn]. One permanent
 *    effect anywhere in the tree (a +1/+1 counter, a card drawn, a token) and the ability is buying
 *    something that survives the turn, so waiting is no longer free.
 *  - **A later activation is available** — [TimingRule.InstantSpeed], and a window still ahead. A
 *    sorcery-speed ability has only our own main phases, and the postcombat one is worse than this,
 *    so there is nothing to hold for. "Still ahead" stops one step earlier on our own turn than on
 *    theirs; [laterWindowIsStillAhead] carries why, and it is the guard that keeps the floor from
 *    talking the AI out of the attack the grant was for.
 *  - **Nothing on the stack threatens our board** — [somethingOnTheStackThreatensUs]. A grant is
 *    often the response (indestructible against a wrath, protection from a burn spell, evasion out
 *    of a "target creature without flying gets -3/-3"), and this policy cannot rank those. It reads
 *    both where a stack object points *and* what it is, because the shape that most needs the
 *    release — a sweeper — names no target at all.
 *
 * Plus [Patience]'s three releases, inherited whole — see [holds].
 *
 * Carried on [com.wingedsheep.ai.engine.AiProfile.holdExpiringGrantsForCombat].
 */
internal object ExpiringGrantWindow {

    /**
     * Whether activating [ability] **here** is dominated by activating it at a later window this
     * turn — the [TimingVerdict.NoWindow] test.
     *
     * `false` for everything that is not the narrow shape this reasons about, which is the
     * overwhelming majority of abilities and every ability at all on a profile with the flag off.
     */
    fun holds(
        state: GameState,
        playerId: EntityId,
        ability: ActivatedAbility,
        intents: IntentCatalog,
    ): Boolean {
        if (ability.isManaAbility) return false

        // A sorcery-speed ability has no later window worth holding for: our own main phases are
        // the only ones it gets, and the postcombat one is strictly worse than this.
        if (ability.timing != TimingRule.InstantSpeed) return false

        if (!everyPayoffExpiresThisTurn(ability.effect)) return false

        if (!laterWindowIsStillAhead(state, playerId)) return false

        if (somethingOnTheStackThreatensUs(state, playerId, intents)) return false

        // Lethal on board, a hand at maximum size, a game gone long — see [Patience], which owns all
        // three and is shared with the patience bars and [AmbushWindow] so there is one definition
        // of each rather than four that can drift.
        //
        // The hand-size release earns its keep twice over here, because the commonest cost on this
        // ability shape is a discard: at a full hand the card being pitched is the one the cleanup
        // step was going to take anyway, so the activation is free and the floor has no business
        // stopping it.
        return Patience.factorFor(state, state.projectedState, playerId) > 0.0
    }

    /**
     * Whether **everything** [effect] does is gone at cleanup.
     *
     * The test is over [EffectWalker.leaves], so a composite, a gate and a "you may" are all seen
     * through to the leaves that actually do something — and both branches of a conditional count,
     * which is the conservative reading: an ability that *might* make a token is an ability that
     * buys something permanent.
     *
     * Only the three duration-carrying grant leaves are recognized as expiring. Everything else —
     * including an unrecognized effect — fails the test, so a shape this cannot read keeps the
     * pre-flag behaviour rather than earning a veto. That is the same fail-safe direction
     * [IntentCatalog] takes about a card it has never seen.
     *
     * [Duration.EndOfTurn] specifically, not "any bounded duration": [Duration.UntilYourNextTurn]
     * spans the opponent's whole turn, which is precisely a payoff that outlives the window this
     * policy would defer past.
     *
     * And a [ModifyStatsEffect] counts only when it **pumps**. "Target creature gets -2/-2 until end
     * of turn" is removal with a duration on it: the modifier expires, the creature it killed does
     * not, so its payoff is permanent and waiting is not free. `CardIntentAnalyzer` draws the same
     * line one file over, tagging a negative toughness modifier `REMOVAL` rather than `PUMP`. A
     * modifier this cannot read as a number at all — anything but a [DynamicAmount.Fixed] — fails
     * for the same fail-safe reason an unrecognized leaf does.
     */
    private fun everyPayoffExpiresThisTurn(effect: Effect): Boolean {
        val leaves = EffectWalker.leaves(effect)
        return leaves.isNotEmpty() && leaves.all { leaf ->
            when (leaf) {
                is GrantKeywordEffect -> leaf.duration == Duration.EndOfTurn
                is GrantEvasionKeywordEffect -> leaf.duration == Duration.EndOfTurn
                is ModifyStatsEffect ->
                    leaf.duration == Duration.EndOfTurn && isPump(leaf)
                else -> false
            }
        }
    }

    /** Whether [effect] only ever adds stats — see [everyPayoffExpiresThisTurn]. */
    private fun isPump(effect: ModifyStatsEffect): Boolean {
        val power = (effect.powerModifier as? DynamicAmount.Fixed)?.amount ?: return false
        val toughness = (effect.toughnessModifier as? DynamicAmount.Fixed)?.amount ?: return false
        return power >= 0 && toughness >= 0
    }

    /**
     * Whether a window at least as good as this one is still coming this turn.
     *
     * **The deadline is one step earlier on our own turn than on theirs**, and the asymmetry is
     * load-bearing rather than fussy — it is the difference between holding a grant and throwing
     * away the attack it was for.
     *
     * *Their turn* releases at [Step.DECLARE_ATTACKERS]. That is the last window an evasion grant
     * can still change a block (CR 509.1a — a creature with flying can be blocked only by a
     * creature with flying or reach, and blockers are declared in the next step), and by then the
     * attack is known, which is the whole reason to wait.
     *
     * *Our turn* releases at [Step.BEGIN_COMBAT], one step sooner, because our attack is declared
     * *in* `DECLARE_ATTACKERS` and the AI receives priority there only after declaring. `CombatAdvisor`
     * reads the board as it stands, so a grant held past begin-combat is not in force when the
     * attack is chosen: the 2/2 that would have attacked as a flier stays home, and the floor has
     * cost the line it was protecting. Begin-combat is the last window that is still strictly better
     * than the main phase (a flash blocker may have appeared) *and* still ahead of the declaration.
     *
     * Both are the earliest of the last-useful windows across the grants this covers rather than the
     * latest — a raw +3/+0 could honestly wait until after blocks — and releasing early is the
     * conservative direction: from there on this policy says nothing and the leaf score decides, so
     * it can cost the AI a slightly early activation but can never make it miss the payoff.
     *
     * A turn where no attackers are declared skips the declare-attackers step entirely, so on the
     * opponent's turn the floor then stands to cleanup — correctly. With no combat the grant had
     * nothing to buy in the first place, and paying a card to watch it expire in the end step is the
     * mistake, not the missed window.
     */
    private fun laterWindowIsStillAhead(state: GameState, playerId: EntityId): Boolean =
        if (state.activePlayerId == playerId) state.step in BEFORE_OUR_ATTACK
        else state.step in BEFORE_THEIR_ATTACK

    /**
     * Whether anything on the stack is a reason to spend the grant *now* rather than later.
     *
     * Three ways to be one, and they are ordered from the cheapest question to the most
     * information-hungry:
     *
     *  1. **It points at a permanent we control.** No intent needed and it catches the whole shape
     *     of spot removal, a fight, a bounce, a tapper — anything that names one of our permanents
     *     is something a grant might answer, and this policy has no business ranking those.
     *  2. **We cannot read it, and it is not ours.** The same "silence is not a veto" line
     *     [HoldPolicy.responseWindowFor] takes: an unknown opposing spell is not evidence that
     *     waiting is safe.
     *  3. **We can read it, it is not ours, and it answers a board.** This is the clause a sweeper
     *     needs. A Wrath of God names no targets at all, so clause 1 never sees it, and an ability
     *     that grants indestructible until end of turn is exactly the thing you activate in
     *     response to one — a floor that deferred past it would be actively losing the board.
     *
     * Our own spells and abilities are excluded throughout: an ETB trigger of ours resolving is not
     * a deadline, and treating it as one would release the floor on most turns for no reason.
     *
     * The position that motivated this file has an opposing *creature spell* on the stack — read,
     * not ours, and carrying none of [THREATENS_A_BOARD] — so the floor correctly stands there.
     */
    private fun somethingOnTheStackThreatensUs(
        state: GameState,
        playerId: EntityId,
        intents: IntentCatalog,
    ): Boolean {
        if (state.stack.isEmpty()) return false
        val projected = state.projectedState
        return state.stack.any { stackId ->
            val container = state.getEntity(stackId) ?: return@any false

            val aimedAtUs = container.get<TargetsComponent>()?.targets.orEmpty().any { target ->
                target is ChosenTarget.Permanent && projected.getController(target.entityId) == playerId
            }
            if (aimedAtUs) return@any true

            // Ours, and pointed at nothing of ours: not a deadline. Read off the base state —
            // the projected-state rule is about battlefield permanents, and this is the stack.
            if (container.get<ControllerComponent>()?.playerId == playerId) return@any false

            val intent = intents.forStackObject(container) ?: return@any true
            intent.tags.any { it in THREATENS_A_BOARD }
        }
    }

    /**
     * Tags that answer a board rather than build one — the stack objects a grant is plausibly a
     * response to even when they name no target.
     *
     * The same set [AmbushWindow.ANSWERS_THEIR_BOARD] names, and for a mirrored reason: there it is
     * "this ETB wants to happen before we attack", here it is "this is a deadline we must not defer
     * past". [IntentTag.SWEEPER] is the member that earns the set — it is the one shape that cannot
     * be caught by looking at targets, because a wrath has none.
     */
    private val THREATENS_A_BOARD = setOf(
        IntentTag.REMOVAL, IntentTag.EXILE_REMOVAL, IntentTag.SWEEPER, IntentTag.NEUTRALIZE,
        IntentTag.TAPPER, IntentTag.FIGHT,
    )

    /** Every step before we choose our attackers — see [laterWindowIsStillAhead]. */
    private val BEFORE_OUR_ATTACK = setOf(
        Step.UNTAP, Step.UPKEEP, Step.DRAW, Step.PRECOMBAT_MAIN,
    )

    /** [BEFORE_OUR_ATTACK] plus the step where their attack is not yet known. */
    private val BEFORE_THEIR_ATTACK = BEFORE_OUR_ATTACK + Step.BEGIN_COMBAT
}
