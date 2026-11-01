package com.wingedsheep.ai.engine.knowledge

import com.wingedsheep.ai.engine.CombatMath
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.combat.BlockedComponent
import com.wingedsheep.engine.state.components.combat.BlockingComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
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
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import com.wingedsheep.sdk.scripting.effects.CantAttackEffect
import com.wingedsheep.sdk.scripting.effects.CantBlockEffect
import com.wingedsheep.sdk.scripting.effects.ChooseOptionEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.CreatePredefinedTokenEffect
import com.wingedsheep.sdk.scripting.effects.ForEachEffect
import com.wingedsheep.sdk.scripting.effects.GainControlEffect
import com.wingedsheep.sdk.scripting.effects.GrantEvasionKeywordEffect
import com.wingedsheep.sdk.scripting.effects.GrantKeywordEffect
import com.wingedsheep.sdk.scripting.effects.IterationSpace
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.effects.PreventDamageEffect
import com.wingedsheep.sdk.scripting.effects.PreventionDirection
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
        /**
         * [com.wingedsheep.ai.engine.AiProfile.refuseWastedPayoffs]: read an end-of-turn **shrink**
         * ("target creature gets -3/-1 until end of turn") that kills nothing as a payoff
         * [nothingCanSpend] can judge — see [PayoffKind.SHRINK].
         */
        readShrinks: Boolean = false,
        /**
         * [com.wingedsheep.ai.engine.AiProfile.refuseWastedPayoffs]: read an end-of-turn combat
         * **restriction** ("target creature can't attack this turn") — see [restrictionHasNoWindow].
         */
        readRestrictions: Boolean = false,
        /**
         * [com.wingedsheep.ai.engine.AiProfile.lifelinkWaitsForBlocks]: a grant whose every payoff
         * is lifelink defers through the declare-attackers step too — see [waitsForBlocks].
         */
        lifelinkWaitsForBlocks: Boolean = false,
        /**
         * [com.wingedsheep.ai.engine.AiProfile.borrowedCreaturesGoHome]: read an end-of-turn
         * **control change** ("gain control of target creature until end of turn") as a payoff
         * [nothingCanSpend] can judge — see [PayoffKind.BORROW].
         */
        readBorrows: Boolean = false,
    ): Boolean {
        if (ability.isManaAbility) return false

        if (readRestrictions && activation != null &&
            restrictionHasNoWindow(state, playerId, ability, activation, intents)
        ) {
            return true
        }

        if ((refuseUnspendable || readShrinks || readBorrows) && activation != null &&
            nothingCanSpend(
                state, playerId, ability.effect,
                sourceId = activation.sourceId,
                targets = activation.targets,
                sourceTaps = tapsItsSource(ability),
                pendingSelfGrant = selfGrantPending(state, activation.sourceId, ability),
                intents = intents,
                readGrants = refuseUnspendable,
                readShrinks = readShrinks,
                readBorrows = readBorrows,
            )
        ) {
            return true
        }
        if (!deferToLaterWindow) return false

        // A sorcery-speed ability has no later window worth holding for: our own main phases are
        // the only ones it gets, and the postcombat one is strictly worse than this.
        if (ability.timing != TimingRule.InstantSpeed) return false

        val expires = if (refuseUnspendable) {
            // Only creature grants and damage shields defer. A land-type change is bought to deny
            // or fix mana, and mana is spent in the main phase this floor would defer it past.
            expiringPayoffs(ability.effect)?.all { it.kind in DEFERRABLE } == true
        } else {
            everyPayoffExpiresThisTurn(ability.effect)
        }
        if (!expires) return false
        if (shieldFacesTheStack(state, expiringPayoffs(ability.effect).orEmpty())) return false

        val untilBlocks = lifelinkWaitsForBlocks && waitsForBlocks(ability.effect)
        if (!laterWindowIsStillAhead(state, playerId, untilBlocks)) return false

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
     * Whether **no** window left this turn can spend what [effect] buys — the floor
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
        effect: Effect,
        /** The permanent the ability is printed on; null for a spell, which has no `Self` to fight. */
        sourceId: EntityId?,
        targets: List<ChosenTarget>,
        /**
         * Whether paying taps the source — as the cost ({T}) or as a rider ("Tap it.") — so it
         * cannot be the creature that attacks or blocks with the grant afterwards.
         */
        sourceTaps: Boolean,
        /** The same self-grant is already on the stack from this source. */
        pendingSelfGrant: Boolean,
        intents: IntentCatalog,
        readGrants: Boolean = true,
        readShrinks: Boolean = false,
        readBorrows: Boolean = false,
    ): Boolean {
        val payoffs = expiringPayoffs(effect, readShrinks, readBorrows) ?: return false
        // Without [readGrants] only the shrink half is asked about; a grant is left to the leaf —
        // unless it rides on a borrow, where it lands on the creature the borrow is judged by.
        val borrows = payoffs.any { it.kind == PayoffKind.BORROW }
        if (!readGrants && !borrows && payoffs.any { it.kind != PayoffKind.SHRINK }) return false
        if (somethingOnTheStackThreatensUs(state, playerId, intents)) return false
        if (shieldFacesTheStack(state, payoffs)) return false

        val projected = state.projectedState
        return payoffs.none { payoff ->
            if (payoff.kind == PayoffKind.SHIELD) {
                return@none shieldCanStillPrevent(state, projected, playerId, payoff.target, sourceId, targets, sourceTaps)
                    ?: return false
            }
            val affected = affectedBy(state, projected, playerId, payoff.target, sourceId, targets)
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
                                (pendingSelfGrant && payoff.target == EffectTarget.Self)
                            )
                    }
                    fresh.any {
                        canStillFight(state, projected, playerId, it, sourceId, sourceTaps, keyword)
                    }
                }
                PayoffKind.LAND -> affected.any { canStillMatter(state, projected, playerId, it) }
                PayoffKind.SHIELD -> true // answered above: a shield can land on a player
                PayoffKind.BORROW -> affected.any { projected.isCreature(it) } &&
                    (state.step in UNTIL_ATTACKERS_DECLARED || state.step in FIGHT_STILL_AHEAD ||
                        controlsASacrificeOutlet(state, playerId, intents))
                PayoffKind.SHRINK -> affected.any { id ->
                    killedBy(state, projected, id, payoff.toughnessLoss) ||
                        if (projected.getController(id) == playerId) {
                            canStillFight(state, projected, playerId, id, sourceId, sourceTaps)
                        } else {
                            opposingCanStillFight(state, projected, playerId, id)
                        }
                }
            }
        }
    }

    /** Whether the same ability from the same source is already on the stack. */
    private fun selfGrantPending(state: GameState, sourceId: EntityId, ability: ActivatedAbility): Boolean =
        state.stack.any { stackId ->
            val onStack = state.getEntity(stackId)?.get<ActivatedAbilityOnStackComponent>()
            onStack != null && onStack.sourceId == sourceId && onStack.effect == ability.effect
        }

    /**
     * [nothingCanSpend]'s shrink reading for a **spell** — the floor
     * [com.wingedsheep.ai.engine.AiProfile.refuseWastedPayoffs] puts under a cast whose every
     * payoff is an end-of-turn stat loss on a creature that kills nothing and has no fight left
     * this turn to lose it in.
     *
     * The activation path has read this since the Proft position; the spell twin went unread
     * because the floor hung off the activation alone. Off a live game (2026-10-29, `aa989771`,
     * turn 17): Icy Reception's "target creature gets -5/-0 until end of turn" cast in our own
     * precombat main on the opponent's *tapped* 5/5 flier — a creature that could neither block
     * our attack nor make its own before cleanup — at 6 life, as the noncreature spell that
     * discounted Traxos. The leaf priced the flier's lost power as if it outlived the turn. Held,
     * the same card shrinks that flier when it attacks.
     *
     * [effects] are what the cast resolves — the chosen modes of a modal spell — and [targets] the
     * permanents it commits to. A spell has no source on the battlefield, so a `Self` leaf declines
     * and the leaf score stays in charge. Grants are not asked about here: a pump spell has its own
     * combat-trick reading in [HoldPolicy].
     */
    fun castBuysNothing(
        state: GameState,
        playerId: EntityId,
        effects: List<Effect>,
        targets: List<ChosenTarget>,
        intents: IntentCatalog,
        /** Read an end-of-turn control change as well — see [PayoffKind.BORROW]. */
        readBorrows: Boolean = false,
        /** Read an end-of-turn shrink — the reading this was added for. */
        readShrinks: Boolean = true,
    ): Boolean = effects.isNotEmpty() && effects.all { effect ->
        nothingCanSpend(
            state, playerId, effect,
            sourceId = null,
            targets = targets,
            sourceTaps = false,
            pendingSelfGrant = false,
            intents = intents,
            readGrants = false,
            readShrinks = readShrinks,
            readBorrows = readBorrows,
        )
    }

    /**
     * Whether we control a permanent that can sacrifice a creature as a cost — the one thing that
     * turns a creature borrowed past its last fight into something that outlives the turn
     * ("Threaten, then sacrifice it"). A [PayoffKind.BORROW] with one in play is left to the leaf.
     */
    private fun controlsASacrificeOutlet(state: GameState, playerId: EntityId, intents: IntentCatalog): Boolean {
        fun sacrifices(cost: AbilityCost): Boolean = when (cost) {
            is AbilityCost.Atom -> cost.atom is CostAtom.Sacrifice || cost.atom is CostAtom.SacrificeAll
            is AbilityCost.Composite -> cost.costs.any(::sacrifices)
            else -> false
        }
        val projected = state.projectedState
        return projected.getBattlefieldControlledBy(playerId).any { id ->
            val name = state.getEntity(id)?.get<CardComponent>()?.name ?: return@any false
            intents.definition(name)?.script?.activatedAbilities?.any { sacrifices(it.cost) } == true
        }
    }

    /**
     * Whether [activation] spends an end-of-turn combat **restriction** where it cannot matter — the
     * floor [com.wingedsheep.ai.engine.AiProfile.refuseWastedPayoffs] adds for "target creature
     * can't attack this turn" and its mirror "target creature can't block this turn".
     *
     * Off a live game (`2878b4bb`, turns 20, 22, 24): Edifice of Authority ("{1}, {T}: Target
     * creature can't attack this turn. Put a brick counter on this artifact.") activated in the
     * AI's *own* precombat main on the human's Reaper from the Abyss — a creature that cannot attack
     * on our turn anyway. The Edifice then sat tapped through the human's turn, and on turn 23 the
     * Reaper and six friends took the AI from 18 to 4. Activated in the human's upkeep or beginning
     * of combat, the same activation adds the same brick and keeps the 6/6 home.
     *
     * A restriction only buys something in one window, read off whose turn it is:
     *
     *  - **Can't attack** — on the turn of the creature's controller, before attackers are declared,
     *    on a creature that could attack ([CombatMath.getCreaturesThatCanAttack]).
     *  - **Can't block** — on our own turn, before blockers are declared, on an untapped creature
     *    (and once attackers are declared, only while something is attacking).
     *
     * Outside that window the restriction is worth nothing, and the question is what else the
     * ability buys:
     *
     *  - **Nothing else** — the activation is a waste of its cost, in this window, full stop.
     *  - **A counter on the source** (the brick) — bought just the same by the activation in the
     *    restriction's own window. That is dominance, not a guess, only when activating now *costs*
     *    that window: the source taps to pay ([tapsItsSource]) and stays tapped until our untap step,
     *    which for a can't-attack activated on our own turn is after the opponent's attack. A
     *    can't-block, or a can't-attack after their attack, gets the source back before its next
     *    window, so the counter there is free and stays the leaf's call.
     *
     * Any other leaf, a restriction on a creature of ours, a target that cannot be resolved, or a
     * stack object that threatens us hands the decision back to the leaf score.
     */
    private fun restrictionHasNoWindow(
        state: GameState,
        playerId: EntityId,
        ability: ActivatedAbility,
        activation: ActivateAbility,
        intents: IntentCatalog,
    ): Boolean {
        val restrictions = mutableListOf<Pair<Boolean, EffectTarget>>() // (isCantAttack, target)
        var sideBenefit = false
        for (leaf in EffectWalker.leaves(ability.effect)) {
            when {
                leaf is CantAttackEffect && leaf.duration == Duration.EndOfTurn -> restrictions += true to leaf.target
                leaf is CantBlockEffect && leaf.duration == Duration.EndOfTurn -> restrictions += false to leaf.target
                leaf is AddCountersEffect && leaf.target == EffectTarget.Self -> sideBenefit = true
                leaf is TapUntapEffect && leaf.tap && leaf.target == EffectTarget.Self -> Unit
                else -> return false
            }
        }
        if (restrictions.isEmpty()) return false
        if (somethingOnTheStackThreatensUs(state, playerId, intents)) return false

        val projected = state.projectedState
        val ourTurn = state.isActiveTurnFor(playerId)
        for ((cantAttack, target) in restrictions) {
            val affected = affectedBy(state, projected, playerId, target, activation.sourceId, activation.targets)
                ?: return false
            for (creature in affected) {
                if (!projected.isCreature(creature)) return false
                val controller = projected.getController(creature) ?: return false
                if (controller == playerId) return false
                val spendable = if (cantAttack) {
                    state.isActiveTurnFor(controller) && state.step in UNTIL_ATTACKERS_DECLARED &&
                        creature in CombatMath.getCreaturesThatCanAttack(state, projected, controller)
                } else {
                    ourTurn && opposingCanStillBeBlockedOut(state, creature)
                }
                if (spendable) return false
            }
        }
        if (!sideBenefit) return true

        // The side benefit is bought again in the restriction's own window, unless activating now
        // keeps the source tapped through it.
        return tapsItsSource(ability) && ourTurn && restrictions.all { (cantAttack, _) -> cantAttack }
    }

    /** Whether a can't-block on an opposing [creature] still keeps it out of a block this turn. */
    private fun opposingCanStillBeBlockedOut(state: GameState, creature: EntityId): Boolean {
        val entity = state.getEntity(creature) ?: return false
        if (entity.has<TappedComponent>()) return false
        return when (state.step) {
            in UNTIL_ATTACKERS_DECLARED -> true
            Step.DECLARE_ATTACKERS -> state.getBattlefield().any { state.getEntity(it)?.has<AttackingComponent>() == true }
            else -> false
        }
    }

    /**
     * Whether everything [ability] buys is an until-end-of-turn grant on creatures — the shape
     * whose whole value has to be spent in a fight this turn. Read by `Strategist`'s
     * planned-attack floor ([com.wingedsheep.ai.engine.AiProfile.activatedPumpsNeedAPlannedAttack]).
     */
    fun buysOnlyExpiringCreatureGrants(ability: ActivatedAbility): Boolean =
        !ability.isManaAbility && expiringPayoffs(ability.effect)?.all { it.kind == PayoffKind.CREATURE } == true

    /**
     * Whether [effect] has an end-of-turn timing reading at all — every shape [holds] and
     * [castBuysNothing] can judge under some profile flag. With [creatureGrants] false the
     * creature-grant kind is left out, which is what a *spell* gets: [castBuysNothing] never reads
     * grants (a pump spell's reading is [HoldPolicy]'s combat-trick tag instead).
     *
     * Read only by the AI card-reading audit (`just ai-reading-audit`); nothing decides on it.
     */
    fun hasExpiringReading(effect: Effect, creatureGrants: Boolean): Boolean {
        val payoffs = expiringPayoffs(effect, readShrinks = true, readBorrows = true) ?: return false
        // A spell's shield is not read either: [castBuysNothing] asks only about shrinks.
        return creatureGrants || payoffs.none { it.kind in DEFERRABLE }
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
     *
     * A [keyword] in [MATTERS_ONLY_WHEN_BLOCKED] narrows the attacking side: first strike,
     * deathtouch, indestructible and trample change nothing about an attacker no one blocks. So
     * before blocks it must have a blocker to fear — an untapped opposing creature that can block
     * it — and once blockers are declared it must actually be blocked. A pump, double strike and
     * lifelink keep the wider reading: each still changes the damage an unblocked attacker deals.
     * Off a live game: Leonardo, Leader in Blue buying first strike in the declare-attackers step
     * with every opposing creature tapped, with the mana for a removal spell.
     */
    private fun canStillFight(
        state: GameState,
        projected: ProjectedState,
        playerId: EntityId,
        creature: EntityId,
        sourceId: EntityId?,
        sourceTaps: Boolean,
        keyword: String? = null,
    ): Boolean {
        if (!projected.isCreature(creature)) return false
        val entity = state.getEntity(creature) ?: return false
        val tapsAway = sourceTaps && creature == sourceId
        return if (state.isActiveTurnFor(playerId)) {
            val blockedOnly = keyword in MATTERS_ONLY_WHEN_BLOCKED
            when (state.step) {
                in UNTIL_ATTACKERS_DECLARED -> !tapsAway &&
                    creature in CombatMath.getCreaturesThatCanAttack(state, projected, playerId) &&
                    (!blockedOnly || couldBeBlocked(state, projected, playerId, creature))
                Step.DECLARE_ATTACKERS -> entity.has<AttackingComponent>() &&
                    (!blockedOnly || couldBeBlocked(state, projected, playerId, creature))
                in FIGHT_STILL_AHEAD -> entity.has<AttackingComponent>() &&
                    (!blockedOnly || entity.has<BlockedComponent>())
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
     * Whether any untapped creature of an opponent's can block [attacker] — the evasion check
     * [CombatMath.canBeBlockedBy] makes, which is a heuristic, not the full blocking rules.
     */
    private fun couldBeBlocked(
        state: GameState,
        projected: ProjectedState,
        playerId: EntityId,
        attacker: EntityId,
    ): Boolean = state.getOpponents(playerId).any { opponent ->
        CombatMath.getOpponentUntappedCreatures(state, projected, opponent).any { blocker ->
            CombatMath.canBeBlockedBy(state, projected, attacker, blocker)
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
        sourceId: EntityId?,
        targets: List<ChosenTarget>,
    ): List<EntityId>? = when (target) {
        EffectTarget.Self -> sourceId?.let(::listOf)
        // A player-level grant ("you have hexproof until end of turn") has no creature to fight
        // with and no land to tap, and is not a shape this reads.
        EffectTarget.Controller -> null
        // A group pump ("creatures you control get +1/+1"). Our own creatures are the ones whose
        // fights we can spend it in, whatever else the group reaches.
        EffectTarget.IterationEntity -> projected.getBattlefieldControlledBy(playerId)
            .filter { projected.isCreature(it) }
        null -> null
        else -> targets.mapNotNull { (it as? ChosenTarget.Permanent)?.entityId }
            .filter { it in state.getBattlefield() }
            .ifEmpty { null }
    }

    /**
     * Whether a [PayoffKind.SHIELD] on [target] can still prevent damage this turn, or null when
     * the recipient cannot be told — which keeps the leaf in charge.
     *
     * A creature of ours is the same question as a pump on it: [canStillFight]. Our own life is
     * attacked only on the opponent's turn, until combat damage is dealt. A shield on anything else —
     * a planeswalker or battle of ours, anything of theirs — is a shape this does not judge, and
     * counts as spendable. Damage on the stack is [shieldFacesTheStack]'s, read before this.
     */
    private fun shieldCanStillPrevent(
        state: GameState,
        projected: ProjectedState,
        playerId: EntityId,
        target: EffectTarget?,
        sourceId: EntityId?,
        targets: List<ChosenTarget>,
        sourceTaps: Boolean,
    ): Boolean? {
        val recipients = when (target) {
            EffectTarget.Self -> listOfNotNull(sourceId)
            EffectTarget.Controller -> listOf(playerId)
            null -> emptyList()
            else -> targets.mapNotNull {
                when (it) {
                    is ChosenTarget.Permanent -> it.entityId
                    is ChosenTarget.Player -> it.playerId
                    else -> null
                }
            }
        }.ifEmpty { return null }
        return recipients.any { id ->
            when {
                id == playerId -> !state.isActiveTurnFor(playerId) &&
                    (state.step in UNTIL_ATTACKERS_DECLARED || state.step in FIGHT_STILL_AHEAD)
                id in state.getBattlefield() && projected.getController(id) == playerId && projected.isCreature(id) ->
                    canStillFight(state, projected, playerId, id, sourceId, sourceTaps)
                else -> true
            }
        }
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

    /**
     * Whether a shrink that takes [toughnessLoss] toughness off [creature] kills it — the one way a
     * shrink buys something that outlives the turn. Marked damage counts, as the state-based action
     * will count it (CR 704.5g).
     */
    private fun killedBy(state: GameState, projected: ProjectedState, creature: EntityId, toughnessLoss: Int): Boolean {
        val toughness = projected.getToughness(creature) ?: return false
        val damage = state.getEntity(creature)?.get<DamageComponent>()?.amount ?: 0
        return toughness - damage - toughnessLoss <= 0
    }

    /**
     * Whether an **opposing** creature can still be in a fight this turn that a shrink on it would
     * change — [canStillFight] from the other side of the table.
     *
     *  - **Our turn**: it can block. Before our attack that is any untapped creature of theirs while
     *    a creature of ours can still attack — with none, there is no attack for it to block; once
     *    attackers are declared it needs an attacker to block, and once blocks are in it must be
     *    blocking. A tapped one stays tapped until *their* untap step, so it fights nothing here.
     *  - **Their turn**: it can attack. Before their attack that is any untapped creature; once
     *    attackers are declared it must be attacking.
     *
     * The position that asked for it (2026-10-15, game `3f847e6d`, turn 8): Proft, Sinister
     * Mastermind discarded from hand in our own declare-attackers step, after we declared no attack,
     * to give an opposing 2/4 -3/-1 that wore off in cleanup.
     */
    private fun opposingCanStillFight(
        state: GameState,
        projected: ProjectedState,
        playerId: EntityId,
        creature: EntityId,
    ): Boolean {
        if (!projected.isCreature(creature)) return false
        val entity = state.getEntity(creature) ?: return false
        val untapped = !entity.has<TappedComponent>()
        return if (state.isActiveTurnFor(playerId)) {
            when (state.step) {
                in UNTIL_ATTACKERS_DECLARED -> untapped &&
                    CombatMath.getCreaturesThatCanAttack(state, projected, playerId).isNotEmpty()
                Step.DECLARE_ATTACKERS -> untapped &&
                    state.getBattlefield().any { state.getEntity(it)?.has<AttackingComponent>() == true }
                in FIGHT_STILL_AHEAD -> entity.has<BlockingComponent>()
                else -> false
            }
        } else {
            when (state.step) {
                in UNTIL_ATTACKERS_DECLARED -> untapped
                in FIGHT_STILL_AHEAD -> entity.has<AttackingComponent>()
                else -> false
            }
        }
    }

    /** What an end-of-turn payoff is bought for — see [nothingCanSpend]. */
    private enum class PayoffKind {
        CREATURE,
        LAND,

        /**
         * A stat loss on a creature that is not the source — "target creature gets -3/-1 until end
         * of turn". It buys something only if it kills ([killedBy]) or if the creature it shrinks
         * still has a fight to shrink it in ([opposingCanStillFight]). Read only under
         * [com.wingedsheep.ai.engine.AiProfile.refuseWastedPayoffs].
         */
        SHRINK,

        /**
         * Control of a creature until end of turn — "gain control of target creature until end of
         * turn" (Threaten, Involuntary Employment, Sibling Rivalry). It goes home at cleanup, so it
         * buys only what the creature does before then: an attack or a block on our side, or a fight
         * it is pulled out of on theirs. Once the last combat damage of the turn is dealt there is
         * none left, and a borrow is spent on nothing — unless a sacrifice outlet of ours can turn
         * it into something permanent. Read only under
         * [com.wingedsheep.ai.engine.AiProfile.borrowedCreaturesGoHome].
         */
        BORROW,

        /**
         * A damage shield that ends at cleanup — "prevent the next 1 damage that would be dealt to
         * any target this turn" (Samite Healer, Abuna Acolyte, Benevolent Ancestor). Its payoff
         * is damage that does not happen, so it buys something only while damage can still come
         * to its recipient this turn: a fight the creature is in, an attack on us, or a spell or
         * ability on the stack (which releases the floor outright — see [shieldFacesTheStack]). Raised before it is needed,
         * it is the same shield raised later — at the same cost, with the source still untapped
         * to block in between. Read with the grants, under
         * [com.wingedsheep.ai.engine.AiProfile.refuseUnspendableGrants].
         */
        SHIELD,
    }

    /**
     * Whether a [PayoffKind.SHIELD] is among [payoffs] while anything at all is on the stack. A
     * shield answers damage, and [somethingOnTheStackThreatensUs] sees only part of it: burn aimed at
     * our face reads as removal (Lava Axe), but a damage trigger without that tag ("deals 2 damage
     * to each opponent") names no permanent of ours, and our own burn (an Earthquake) is excluded
     * as ours. So any stack object hands the shield back to the leaf, whose simulation resolves the
     * stack and prices what it prevents.
     */
    private fun shieldFacesTheStack(state: GameState, payoffs: List<Payoff>): Boolean =
        state.stack.isNotEmpty() && payoffs.any { it.kind == PayoffKind.SHIELD }

    /** The payoffs [holds]' deferral holds for a later window this turn. */
    private val DEFERRABLE = setOf(PayoffKind.CREATURE, PayoffKind.SHIELD)

    /** One leaf of an ability whose payoff ends at cleanup, and where it lands. */
    private data class Payoff(
        val kind: PayoffKind,
        val target: EffectTarget?,
        val keyword: String? = null,
        /** For a [PayoffKind.SHRINK]: the toughness it takes away. */
        val toughnessLoss: Int = 0,
    )

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
     *  - a **trade-off pump on the source itself** — "this creature gets +1/-1 until end of turn"
     *    (Undulating Witness). The sign test that keeps "-2/-2 until end of turn" out as removal is
     *    about an effect aimed at someone else; on its own source a lowered toughness is the price,
     *    not the payoff, and the extra power is spent only in a fight like any other pump;
     *  - and two leaves that are not payoffs at all, skipped rather than failing the test: the
     *    source tapping *itself* ("Tap it." on Vanguard of the Rose — a drawback, which
     *    [tapsItsSource] reads separately) and the option choice a land-type change opens with.
     *
     * An effect with no payoff left once those are skipped is not this shape either.
     */
    private fun expiringPayoffs(effect: Effect, readShrinks: Boolean = false, readBorrows: Boolean = false): List<Payoff>? {
        val payoffs = mutableListOf<Payoff>()
        // Leaves that are not payoffs of their own but ride on a borrow — see the end of the loop.
        var borrowRiders = 0
        for (leaf in EffectWalker.leaves(effect)) {
            when {
                readBorrows && leaf is GainControlEffect && leaf.duration == Duration.EndOfTurn ->
                    payoffs += Payoff(PayoffKind.BORROW, leaf.target)
                // "Untap that creature" readies the borrowed creature to fight; "create a Treasure"
                // is a rider holding the card keeps — the same cast in a window with a fight left
                // buys it too, so it is no reason to spend the card where the borrow buys nothing.
                readBorrows && leaf is TapUntapEffect && !leaf.tap && leaf.target != EffectTarget.Self ->
                    borrowRiders++
                readBorrows && leaf is CreatePredefinedTokenEffect -> borrowRiders++
                leaf is TapUntapEffect && leaf.tap && leaf.target == EffectTarget.Self -> Unit
                leaf is ChooseOptionEffect -> Unit
                leaf is ForEachEffect && leaf.space is IterationSpace.Group -> {
                    val body = expiringPayoffs(leaf.body, readShrinks) ?: return null
                    if (body.any { it.target != EffectTarget.IterationEntity }) return null
                    payoffs += body
                }
                leaf is SetLandTypeEffect && leaf.duration == Duration.EndOfTurn ->
                    payoffs += Payoff(PayoffKind.LAND, leaf.target)
                isShield(leaf) -> payoffs += Payoff(PayoffKind.SHIELD, (leaf as PreventDamageEffect).target)
                leaf is GrantKeywordEffect && leaf.duration == Duration.EndOfTurn ->
                    payoffs += Payoff(PayoffKind.CREATURE, leaf.target, leaf.keyword)
                leaf is GrantEvasionKeywordEffect && leaf.duration == Duration.EndOfTurn ->
                    payoffs += Payoff(PayoffKind.CREATURE, leaf.target)
                leaf is ModifyStatsEffect && leaf.duration == Duration.EndOfTurn &&
                    (isPump(leaf) || leaf.target == EffectTarget.Self) ->
                    payoffs += Payoff(PayoffKind.CREATURE, leaf.target)
                readShrinks && leaf is ModifyStatsEffect && leaf.duration == Duration.EndOfTurn &&
                    statsSign(leaf.powerModifier, leaf.toughnessModifier) == AmountSign.LOSS -> {
                    // The loss has to be a number to tell a shrink from removal; anything else is
                    // left to the leaf, which is the fail-safe direction.
                    val toughness = leaf.toughnessModifier as? DynamicAmount.Fixed ?: return null
                    payoffs += Payoff(PayoffKind.SHRINK, leaf.target, toughnessLoss = -toughness.amount)
                }
                else -> return null
            }
        }
        if (borrowRiders > 0 && payoffs.none { it.kind == PayoffKind.BORROW }) return null
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
     * Only the three duration-carrying grant leaves and a damage shield ([isShield]) are recognized as expiring. Everything else —
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
     * modifier whose sign [AmountSign] cannot pin down (power, a pipeline variable) fails for the same
     * fail-safe reason an unrecognized leaf does; a "+X" that cannot go negative is a pump.
     */
    private fun everyPayoffExpiresThisTurn(effect: Effect): Boolean {
        val leaves = EffectWalker.leaves(effect)
        return leaves.isNotEmpty() && leaves.all { leaf ->
            when (leaf) {
                is GrantKeywordEffect -> leaf.duration == Duration.EndOfTurn
                is GrantEvasionKeywordEffect -> leaf.duration == Duration.EndOfTurn
                is ModifyStatsEffect ->
                    leaf.duration == Duration.EndOfTurn && isPump(leaf)
                is PreventDamageEffect -> isShield(leaf)
                else -> false
            }
        }
    }

    /**
     * Whether [effect] is a [PayoffKind.SHIELD]: damage prevented *to* one recipient until end of
     * turn. A shield over a group (Fog's "all combat damage") or one that stops a creature dealing
     * damage ([PreventionDirection.FromTarget], a Maze of Ith) is a different payoff and stays
     * unread, as does a "shield" that lets the damage through ([PreventDamageEffect.preventDamage]).
     */
    private fun isShield(effect: Effect): Boolean =
        effect is PreventDamageEffect && effect.duration == Duration.EndOfTurn && effect.preventDamage &&
            effect.direction == PreventionDirection.ToTarget && effect.recipientGroup == null

    /** Whether [effect] only ever adds stats — see [everyPayoffExpiresThisTurn]. */
    private fun isPump(effect: ModifyStatsEffect): Boolean {
        val sign = statsSign(effect.powerModifier, effect.toughnessModifier)
        return sign == AmountSign.GAIN || sign == AmountSign.ZERO
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
    private fun laterWindowIsStillAhead(state: GameState, playerId: EntityId, untilBlocks: Boolean = false): Boolean =
        if (untilBlocks) state.step in BEFORE_BLOCKS
        else if (state.isActiveTurnFor(playerId)) state.step in BEFORE_OUR_ATTACK
        else state.step in BEFORE_THEIR_ATTACK

    /**
     * Whether everything [effect] buys is a keyword that changes only what the creature's damage
     * *does* — [SPENT_ONLY_IN_DAMAGE] — so the window after blockers are declared is strictly
     * better than any before it, on either side's turn.
     *
     * [laterWindowIsStillAhead]'s ordinary deadline is the last window that can still change a
     * declaration, because most grants change one: evasion decides who can block, and a pump,
     * deathtouch or first strike decides who survives, which is what the block and attack planners
     * read off the board. Lifelink decides neither. It adds life to damage dealt and nothing else,
     * so a blocker gains nothing from having it before it blocks, and an attacker gains nothing
     * from having it before blocks are known. Once they are, [nothingCanSpend] refuses it on a
     * creature that is not in the fight, and on one that is the simulation carries the line through
     * combat damage, so the leaf prices the life actually gained against the cost — two life at 20
     * is not worth a card; two life at 2 is.
     *
     * Off the 2026-10-19 log review: Hardened Academic pitching a card for lifelink in the
     * opponent's declare-attackers step and then not blocking, and pitching one before blocks while
     * attacking an empty board at 20 life.
     */
    private fun waitsForBlocks(effect: Effect): Boolean =
        expiringPayoffs(effect)?.all { it.kind == PayoffKind.CREATURE && it.keyword in SPENT_ONLY_IN_DAMAGE } == true

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
     * Our own spells and abilities are excluded throughout, and that test runs **first**: an ETB
     * trigger of ours resolving is not a deadline, and treating it as one would release the floor on
     * most turns for no reason. Clause 1 used to run ahead of it, so our own spell aimed at our own
     * creature read as a threat — a Rocky Rebuke of ours targeting our Avatar Kyoshi released the
     * floor, and the AI tapped Deserter's Disciple to make a creature that was not attacking
     * unblockable, after attackers were declared.
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

            // Ours: never a deadline, whatever it points at. Read off the base state — the
            // projected-state rule is about battlefield permanents, and this is the stack.
            if (container.get<ControllerComponent>()?.playerId == playerId) return@any false

            val aimedAtUs = container.get<TargetsComponent>()?.targets.orEmpty().any { target ->
                target is ChosenTarget.Permanent && projected.getController(target.entityId) == playerId
            }
            if (aimedAtUs) return@any true

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

    /** Every step before blockers are declared, on either side's turn — see [waitsForBlocks]. */
    private val BEFORE_BLOCKS = BEFORE_THEIR_ATTACK + Step.DECLARE_ATTACKERS

    /**
     * Every step in which, on either side of the table, no attacker has been declared yet — the
     * same set as [BEFORE_THEIR_ATTACK], named for what [canStillFight] reads it as.
     */
    private val UNTIL_ATTACKERS_DECLARED = BEFORE_THEIR_ATTACK

    /** Attackers declared and combat damage still to come — see [canStillFight]. */
    private val FIGHT_STILL_AHEAD = setOf(
        Step.DECLARE_ATTACKERS, Step.DECLARE_BLOCKERS, Step.FIRST_STRIKE_COMBAT_DAMAGE,
    )

    /**
     * Keywords that change a fight only when the attacker is blocked — see [canStillFight]. Double
     * strike and lifelink are left out on purpose: both still matter against an empty board.
     */
    private val MATTERS_ONLY_WHEN_BLOCKED = setOf(
        Keyword.FIRST_STRIKE, Keyword.DEATHTOUCH, Keyword.INDESTRUCTIBLE, Keyword.TRAMPLE,
    ).map { it.name }.toSet()

    /**
     * Keywords that change only what a creature's combat damage does — not who can block it, not
     * who survives the fight. See [waitsForBlocks].
     */
    private val SPENT_ONLY_IN_DAMAGE = setOf(Keyword.LIFELINK.name)

    /** The keywords a land-type change can open a path for — see [canStillMatter]. */
    private val LANDWALK_KEYWORDS = setOf(
        Keyword.PLAINSWALK, Keyword.ISLANDWALK, Keyword.SWAMPWALK, Keyword.MOUNTAINWALK,
        Keyword.FORESTWALK, Keyword.DESERTWALK, Keyword.NONBASIC_LANDWALK,
        Keyword.LEGENDARY_LANDWALK,
    ).map { it.name }.toSet()
}
