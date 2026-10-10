package com.wingedsheep.ai.engine.knowledge

import com.wingedsheep.ai.engine.CombatMath
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.combat.BlockingComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.stack.ActivatedAbilityOnStackComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.TargetsComponent
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.ChooseOptionEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.ForEachEffect
import com.wingedsheep.sdk.scripting.effects.GrantEvasionKeywordEffect
import com.wingedsheep.sdk.scripting.effects.GrantKeywordEffect
import com.wingedsheep.sdk.scripting.effects.IterationSpace
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.effects.SetLandTypeEffect
import com.wingedsheep.sdk.scripting.effects.TapUntapEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
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
        /**
         * The materialized activation — its source and committed targets, which is what the
         * unspendable floor needs to know *which* creature the grant lands on. Null keeps the
         * deferral path alone, which reads the ability and nothing else.
         */
        activation: ActivateAbility? = null,
        /**
         * [com.wingedsheep.ai.engine.AiProfile.holdExpiringGrantsForCombat]: the deferral floor —
         * a window later this turn is strictly better than this one.
         */
        deferToLaterWindow: Boolean = true,
        /**
         * [com.wingedsheep.ai.engine.AiProfile.refuseUnspendableGrants]: the "nothing can spend
         * it" floor — see [nothingCanSpend] — plus a wider reading of the deferral's shapes and no
         * long-game release on it.
         */
        refuseUnspendable: Boolean = false,
    ): Boolean {
        if (ability.isManaAbility) return false

        if (refuseUnspendable && activation != null &&
            nothingCanSpend(state, playerId, ability, activation, intents)
        ) {
            return true
        }
        if (!deferToLaterWindow) return false

        // A sorcery-speed ability has no later window worth holding for: our own main phases are
        // the only ones it gets, and the postcombat one is strictly worse than this.
        if (ability.timing != TimingRule.InstantSpeed) return false

        val expires = if (refuseUnspendable) {
            // Only creature grants defer. A land-type change is bought to deny or fix mana, and
            // mana is spent in the main phase this floor would defer it past.
            expiringPayoffs(ability.effect)?.all { it.kind == PayoffKind.CREATURE } == true
        } else {
            everyPayoffExpiresThisTurn(ability.effect)
        }
        if (!expires) return false

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
        //
        // The long-game release is the one that does not transfer, and [refuseUnspendable] drops
        // it. Patience decays because a card held is a bet that a better *target* or *spell* turns
        // up, and that bet worsens every turn. This floor makes no bet: the window it defers to is
        // later this same turn, and the ability is still there at the same cost. Turn 24 is no
        // reason to pay for deathtouch in a main phase — and that release is what let it through.
        val projected = state.projectedState
        return if (refuseUnspendable) {
            !Patience.releasedOutright(state, projected, playerId)
        } else {
            Patience.factorFor(state, projected, playerId) > 0.0
        }
    }

    /**
     * Whether **no** window left this turn can spend what [activation] buys — the floor
     * [com.wingedsheep.ai.engine.AiProfile.refuseUnspendableGrants] adds.
     *
     * The deferral floor in [holds] answers "is a better window still ahead?". This answers the
     * question it leaves open once that window has come and gone: "is there *any* window?". An
     * end-of-turn grant buys something only through a fight it changes, a spell it answers, or mana
     * it denies or fixes. With none of those left, the leaf score is pricing a board cleanup is
     * about to erase. The shapes off the log review all land here:
     *
     *  - **Esquire of the King** pumping the team, and **Poison Dart Frog** granting deathtouch,
     *    with no creature that can still attack or block — once attackers are declared, a creature
     *    that is not attacking has no combat left to spend the stats in.
     *  - **A grant already in force.** Two of the Frog's four activations in one main phase landed
     *    on a Frog that already had deathtouch, or had it waiting on the stack.
     *  - **Dream Thrush** turning an opponent's land into a Plains on our own turn, with no
     *    landwalker of ours to walk through it.
     *
     * What counts as a consumer is deliberately generous, so this only fires where "does nothing"
     * is structurally certain — the [TimingVerdict.NoWindow] standard. Before attackers are
     * declared, any creature that *could* attack or block keeps the grant alive; whether it will is
     * the leaf's call. A stack object that threatens us releases the floor outright, because a
     * grant is often the answer to one.
     *
     * A shape it cannot read — any leaf that is not an end-of-turn grant, a target it cannot
     * resolve — returns false and keeps the leaf in charge.
     */
    private fun nothingCanSpend(
        state: GameState,
        playerId: EntityId,
        ability: ActivatedAbility,
        activation: ActivateAbility,
        intents: IntentCatalog,
    ): Boolean {
        val payoffs = expiringPayoffs(ability.effect) ?: return false
        if (somethingOnTheStackThreatensUs(state, playerId, intents)) return false

        val projected = state.projectedState
        // A source that taps itself — as the cost ({T}) or as a rider ("Tap it.") — cannot be the
        // creature that attacks or blocks with the grant afterwards.
        val sourceTaps = tapsItsSource(ability)
        val pendingOnStack = state.stack.any { stackId ->
            val onStack = state.getEntity(stackId)?.get<ActivatedAbilityOnStackComponent>()
            onStack != null && onStack.sourceId == activation.sourceId && onStack.effect == ability.effect
        }

        return payoffs.none { payoff ->
            val affected = affectedBy(state, projected, playerId, payoff.target, activation)
                ?: return false
            when (payoff.kind) {
                PayoffKind.CREATURE -> {
                    val keyword = payoff.keyword
                    // Already in force, or already on its way: a second grant of the same keyword
                    // adds nothing. Pending is read for a self-grant only, where "the same ability
                    // from the same source" can only mean the same creature.
                    val fresh = affected.filterNot { id ->
                        keyword != null && (
                            projected.hasKeyword(id, keyword) ||
                                (pendingOnStack && payoff.target == EffectTarget.Self)
                            )
                    }
                    fresh.any { canStillFight(state, projected, playerId, it, activation.sourceId, sourceTaps) }
                }
                PayoffKind.LAND -> affected.any { canStillMatter(state, projected, playerId, it) }
            }
        }
    }

    /**
     * [canStillFight] for a grant that does not come from an activation — no source of ours taps
     * to pay for it. The yes/no half of
     * [com.wingedsheep.ai.engine.AiProfile.holdUnusablePumps] reads it.
     */
    fun creatureCanStillFight(state: GameState, playerId: EntityId, creature: EntityId): Boolean =
        canStillFight(state, state.projectedState, playerId, creature, sourceId = creature, sourceTaps = false)

    /**
     * Whether [creature] can still be in a fight this turn that a grant on it would change.
     *
     * Read off the step, from the seat of whoever is asking:
     *
     *  - **Our turn, before attackers are declared** — it can attack. Whether it *should* is the
     *    leaf's question, not this one's.
     *  - **Our turn, attackers declared, damage not yet dealt** — it is attacking.
     *  - **Their turn, before blockers are declared** — it can block, and once their attack is
     *    known there is something to block.
     *  - **Their turn, blockers declared** — it is blocking.
     *
     * The combat damage step and everything after it is never a fight: priority there comes after
     * the damage (CR 510.2–510.3). An additional combat phase later in the turn is a shape this
     * does not try to see.
     */
    private fun canStillFight(
        state: GameState,
        projected: ProjectedState,
        playerId: EntityId,
        creature: EntityId,
        sourceId: EntityId,
        sourceTaps: Boolean,
    ): Boolean {
        if (!projected.isCreature(creature)) return false
        val entity = state.getEntity(creature) ?: return false
        val tapsAway = sourceTaps && creature == sourceId
        return if (state.isActiveTurnFor(playerId)) {
            when (state.step) {
                in UNTIL_ATTACKERS_DECLARED -> !tapsAway &&
                    creature in CombatMath.getCreaturesThatCanAttack(state, projected, playerId)
                in FIGHT_STILL_AHEAD -> entity.has<AttackingComponent>()
                else -> false
            }
        } else {
            when (state.step) {
                in UNTIL_ATTACKERS_DECLARED -> !tapsAway && !entity.has<TappedComponent>()
                Step.DECLARE_ATTACKERS -> !tapsAway && !entity.has<TappedComponent>() &&
                    state.getBattlefield().any { state.getEntity(it)?.has<AttackingComponent>() == true }
                in FIGHT_STILL_AHEAD -> entity.has<BlockingComponent>()
                else -> false
            }
        }
    }

    /**
     * Whether a land-type change on [land] can still change anything this turn.
     *
     * Three ways, and a land outside them is one whose type no longer matters to anyone: an
     * untapped land of ours (fixing our own colors), an untapped land of theirs on *their* turn
     * (denying the mana they were about to spend), and any land at all while a landwalker of ours
     * can still attack through it. An opponent's land on our own turn is left out on purpose: the
     * only mana it denies is an instant they might cast into our turn, and the price was a creature
     * tapped through *their* whole turn — Dream Thrush, at 4 life.
     */
    private fun canStillMatter(
        state: GameState,
        projected: ProjectedState,
        playerId: EntityId,
        land: EntityId,
    ): Boolean {
        val untapped = state.getEntity(land)?.has<TappedComponent>() == false
        val ours = projected.getController(land) == playerId
        val ourTurn = state.isActiveTurnFor(playerId)
        if (untapped && (ours || !ourTurn)) return true
        if (!ourTurn || state.step !in UNTIL_ATTACKERS_DECLARED) return false
        return CombatMath.getCreaturesThatCanAttack(state, projected, playerId).any { attacker ->
            projected.getKeywords(attacker).any { it in LANDWALK_KEYWORDS }
        }
    }

    /**
     * The permanents a payoff lands on, or null when this cannot tell — which keeps the leaf in
     * charge rather than guessing.
     */
    private fun affectedBy(
        state: GameState,
        projected: ProjectedState,
        playerId: EntityId,
        target: EffectTarget?,
        activation: ActivateAbility,
    ): List<EntityId>? = when (target) {
        EffectTarget.Self -> listOf(activation.sourceId)
        // A player-level grant ("you have hexproof until end of turn") has no creature to fight
        // with and no land to tap, and is not a shape this reads.
        EffectTarget.Controller -> null
        // A group pump ("creatures you control get +1/+1"). Our own creatures are the ones whose
        // fights we can spend it in, whatever else the group reaches.
        EffectTarget.IterationEntity -> projected.getBattlefieldControlledBy(playerId)
            .filter { projected.isCreature(it) }
        null -> null
        else -> activation.targets.mapNotNull { (it as? ChosenTarget.Permanent)?.entityId }
            .filter { it in state.getBattlefield() }
            .ifEmpty { null }
    }

    /** Whether paying for [ability] taps its own source — `{T}` in the cost, or "Tap it." on resolution. */
    private fun tapsItsSource(ability: ActivatedAbility): Boolean {
        fun costTaps(cost: AbilityCost): Boolean = when (cost) {
            AbilityCost.Tap -> true
            is AbilityCost.Composite -> cost.costs.any(::costTaps)
            else -> false
        }
        return costTaps(ability.cost) || EffectWalker.leaves(ability.effect).any {
            it is TapUntapEffect && it.tap && it.target == EffectTarget.Self
        }
    }

    /** What an end-of-turn payoff is bought for — see [nothingCanSpend]. */
    private enum class PayoffKind { CREATURE, LAND }

    /** One leaf of an ability whose payoff ends at cleanup, and where it lands. */
    private data class Payoff(val kind: PayoffKind, val target: EffectTarget?, val keyword: String? = null)

    /**
     * The payoffs of [effect] when **every** one of them is gone at cleanup, or null when any part
     * of it outlives the turn or cannot be read.
     *
     * The `refuseUnspendable` reading of [everyPayoffExpiresThisTurn]: the same three grant leaves,
     * plus the shapes the log review found that one could not see —
     *
     *  - a **group pump**, which `Patterns.Group.modifyStatsForAll` lowers to a `ForEachEffect` over
     *    a group whose body is the ordinary pump bound to the iteration entity (Esquire of the King);
     *  - an end-of-turn **land-type change** (Dream Thrush);
     *  - and two leaves that are not payoffs at all, skipped rather than failing the test: the
     *    source tapping *itself* ("Tap it." on Vanguard of the Rose — a drawback, which
     *    [tapsItsSource] reads separately) and the option choice a land-type change opens with.
     *
     * An effect with no payoff left once those are skipped is not this shape either.
     */
    private fun expiringPayoffs(effect: Effect): List<Payoff>? {
        val payoffs = mutableListOf<Payoff>()
        for (leaf in EffectWalker.leaves(effect)) {
            when {
                leaf is TapUntapEffect && leaf.tap && leaf.target == EffectTarget.Self -> Unit
                leaf is ChooseOptionEffect -> Unit
                leaf is ForEachEffect && leaf.space is IterationSpace.Group -> {
                    val body = expiringPayoffs(leaf.body) ?: return null
                    if (body.any { it.target != EffectTarget.IterationEntity }) return null
                    payoffs += body
                }
                leaf is SetLandTypeEffect && leaf.duration == Duration.EndOfTurn ->
                    payoffs += Payoff(PayoffKind.LAND, leaf.target)
                leaf is GrantKeywordEffect && leaf.duration == Duration.EndOfTurn ->
                    payoffs += Payoff(PayoffKind.CREATURE, leaf.target, leaf.keyword)
                leaf is GrantEvasionKeywordEffect && leaf.duration == Duration.EndOfTurn ->
                    payoffs += Payoff(PayoffKind.CREATURE, leaf.target)
                leaf is ModifyStatsEffect && leaf.duration == Duration.EndOfTurn && isPump(leaf) ->
                    payoffs += Payoff(PayoffKind.CREATURE, leaf.target)
                else -> return null
            }
        }
        return payoffs.ifEmpty { null }
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
     * A turn where no attackers are declared still passes through the declare-attackers step, so on
     * the opponent's turn the floor is released there with nothing attacking — and the leaf score,
     * not this policy, then decides. With no combat the grant had nothing to buy in the first place,
     * so the score sees no payoff for paying a card to watch it expire in the end step.
     */
    private fun laterWindowIsStillAhead(state: GameState, playerId: EntityId): Boolean =
        if (state.isActiveTurnFor(playerId)) state.step in BEFORE_OUR_ATTACK
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

    /**
     * Every step in which, on either side of the table, no attacker has been declared yet — the
     * same set as [BEFORE_THEIR_ATTACK], named for what [canStillFight] reads it as.
     */
    private val UNTIL_ATTACKERS_DECLARED = BEFORE_THEIR_ATTACK

    /** Attackers declared and combat damage still to come — see [canStillFight]. */
    private val FIGHT_STILL_AHEAD = setOf(
        Step.DECLARE_ATTACKERS, Step.DECLARE_BLOCKERS, Step.FIRST_STRIKE_COMBAT_DAMAGE,
    )

    /** The keywords a land-type change can open a path for — see [canStillMatter]. */
    private val LANDWALK_KEYWORDS = setOf(
        Keyword.PLAINSWALK, Keyword.ISLANDWALK, Keyword.SWAMPWALK, Keyword.MOUNTAINWALK,
        Keyword.FORESTWALK, Keyword.DESERTWALK, Keyword.NONBASIC_LANDWALK,
    ).map { it.name }.toSet()
}
