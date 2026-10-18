package com.wingedsheep.engine.mechanics.combat.rules

import com.wingedsheep.engine.state.components.battlefield.chosenCreatureOf
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.mechanics.combat.AttackSacrificeCosts
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.mechanics.combat.CombatDefenders
import com.wingedsheep.engine.state.components.player.InAdditionalCombatPhaseComponent
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.CantAttackUnless
import com.wingedsheep.sdk.scripting.CantAttackYou
import com.wingedsheep.sdk.scripting.CantBeAttackedBy
import com.wingedsheep.sdk.scripting.CantBeAttackedWhileAttached
import com.wingedsheep.sdk.scripting.StaticAbility
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.CompositeStaticAbility
import com.wingedsheep.engine.mechanics.durations.GrantDurationGate
import com.wingedsheep.engine.state.components.identity.RoomFaceStatics
import com.wingedsheep.engine.state.components.identity.TextChanges
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.mechanics.SoulbondPairing
import com.wingedsheep.sdk.scripting.filters.unified.Scope

// =========================================================================
// Per-creature attack restrictions (AttackRestrictionRule)
// =========================================================================

/**
 * Must be a creature (projected types to handle animated lands etc.).
 */
class MustBeCreatureAttackRule : AttackRestrictionRule {
    override fun check(ctx: AttackCheckContext): String? {
        if (!ctx.projected.isCreature(ctx.attackerId)) {
            val name = ctx.state.getEntity(ctx.attackerId)?.get<CardComponent>()?.name ?: "Entity"
            return "Only creatures can attack: $name"
        }
        // CR 508.1a / 506.3f: a creature that is also a battle can't attack.
        if (ctx.projected.isBattle(ctx.attackerId)) {
            val name = ctx.state.getEntity(ctx.attackerId)?.get<CardComponent>()?.name ?: "Entity"
            return "A battle can't attack: $name"
        }
        return null
    }
}

/**
 * Must be controlled by the attacking player (projected controller for control-changing effects).
 */
class ControlledByAttackerRule : AttackRestrictionRule {
    override fun check(ctx: AttackCheckContext): String? {
        // CR 805.10b — under shared team turns the attacking team's combined attack may include
        // creatures controlled by any active-team member, so accept control by any teammate. Without
        // shared team turns (Team vs. Team — CR 808.4, non-team games) only the active player attacks
        // on their own turn, so sharedTurnTeam collapses to just the declaring player.
        val controller = ctx.projected.getController(ctx.attackerId)
        if (controller == null || controller !in ctx.state.sharedTurnTeam(ctx.attackingPlayer)) {
            val name = ctx.state.getEntity(ctx.attackerId)?.get<CardComponent>()?.name ?: "Creature"
            return "You don't control $name"
        }
        return null
    }
}

/**
 * Must be untapped.
 */
class MustBeUntappedAttackRule : AttackRestrictionRule {
    override fun check(ctx: AttackCheckContext): String? {
        val container = ctx.state.getEntity(ctx.attackerId) ?: return null
        if (container.has<TappedComponent>()) {
            val name = container.get<CardComponent>()?.name ?: "Creature"
            return "$name is tapped and cannot attack"
        }
        return null
    }
}

/**
 * Summoning sickness prevents attacking unless haste or an attack-only permission lifts it.
 */
class SummoningSicknessAttackRule : AttackRestrictionRule {
    override fun check(ctx: AttackCheckContext): String? {
        val container = ctx.state.getEntity(ctx.attackerId) ?: return null
        val hasHaste = ctx.projected.hasKeyword(ctx.attackerId, Keyword.HASTE)
        if (!hasHaste && !ctx.projected.canAttackAsThoughHasty(ctx.attackerId) &&
            container.has<SummoningSicknessComponent>()) {
            val name = container.get<CardComponent>()?.name ?: "Creature"
            return "$name has summoning sickness"
        }
        return null
    }
}

/**
 * Cannot have defender keyword, unless the creature has a conditional ability to bypass defender.
 */
class DefenderAttackRule(
    private val predicateEvaluator: PredicateEvaluator
) : AttackRestrictionRule {
    override fun check(ctx: AttackCheckContext): String? {
        if (!ctx.projected.hasKeyword(ctx.attackerId, Keyword.DEFENDER)) return null

        // The Defender restriction is lifted by a temporary "attack this turn as though it didn't
        // have defender" grant or a satisfied CanAttackDespiteDefender static ability. Both live in
        // DefenderBypass so this enforcement path and the client's "can attack" badge agree exactly.
        if (DefenderBypass.isActive(ctx.state, ctx.attackerId, ctx.attackingPlayer, ctx.cardRegistry, predicateEvaluator = predicateEvaluator)) return null

        return errorMsg(ctx)
    }

    private fun errorMsg(ctx: AttackCheckContext): String {
        val name = ctx.state.getEntity(ctx.attackerId)?.get<CardComponent>()?.name ?: "Creature"
        return "$name has defender and cannot attack"
    }
}

/**
 * Cannot have "can't attack" (projected, e.g., from Pacifism).
 */
class CantAttackProjectedRule : AttackRestrictionRule {
    override fun check(ctx: AttackCheckContext): String? {
        if (ctx.projected.cantAttack(ctx.attackerId)) {
            val name = ctx.state.getEntity(ctx.attackerId)?.get<CardComponent>()?.name ?: "Creature"
            return "$name can't attack"
        }
        return null
    }
}

/**
 * During an *inserted* combat phase that carries an attacker restriction (Bumi, Unleashed: "there is
 * an additional combat phase. Only land creatures can attack during that combat phase"), a creature
 * may be declared as an attacker only if it matches that phase's filter (CR 508.1c — the active
 * player checks each creature for attacking restrictions, and an illegal one voids the declaration).
 * The restriction lives on the active
 * player's [InAdditionalCombatPhaseComponent] and is scoped to exactly that phase, so the natural
 * combat phase (which never sets the marker) and any unrestricted extra combat impose nothing here.
 *
 * The filter is matched with projected state so animated lands read as the land *creatures* they are
 * ("land creatures" = `GameObjectFilter.Creature and GameObjectFilter.Land`).
 */
class AdditionalCombatPhaseAttackerRule(
    private val predicateEvaluator: PredicateEvaluator
) : AttackRestrictionRule {
    override fun check(ctx: AttackCheckContext): String? {
        val activePlayer = ctx.state.activePlayerId ?: return null
        val restriction = ctx.state.getEntity(activePlayer)
            ?.get<InAdditionalCombatPhaseComponent>()
            ?.attackerRestriction ?: return null

        val matches = predicateEvaluator.matches(
            ctx.state,
            ctx.projected,
            ctx.attackerId,
            restriction,
            PredicateContext(controllerId = ctx.attackingPlayer, sourceId = ctx.attackerId)
        )
        if (matches) return null

        val name = ctx.state.getEntity(ctx.attackerId)?.get<CardComponent>()?.name ?: "Creature"
        return "$name can't attack this combat phase: only ${restriction.description} can attack"
    }

    companion object {
    }
}

/**
 * Cannot be already attacking.
 */
class NotAlreadyAttackingRule : AttackRestrictionRule {
    override fun check(ctx: AttackCheckContext): String? {
        val container = ctx.state.getEntity(ctx.attackerId) ?: return null
        if (container.has<AttackingComponent>()) {
            val name = container.get<CardComponent>()?.name ?: "Creature"
            return "$name is already attacking"
        }
        return null
    }
}

// =========================================================================
// Per-defender attack restrictions (AttackDefenderRule)
// =========================================================================

/**
 * [CantAttackUnless]: the attacker is covered by a restriction whose condition must hold against
 * the defender it is declared against (CR 508.1c) — the creature's own (Goblin Goon's "can't attack
 * unless you control more creatures than defending player", Deep-Sea Serpent) or a global one
 * printed on another permanent (Arboria's "creatures can't attack a player unless that player …").
 *
 * Every battlefield permanent's printed statics are read, the way [CantAttackYouDefenderRule] reads
 * them: the filter and condition are resolved relative to the holder ("you" is its projected
 * controller), the defending player is bound into the condition's context, and a face-down or
 * ability-stripped holder contributes nothing (CR 708.2). With `coversPlaneswalkers = false` the
 * restriction only speaks about attacking a player — planeswalkers and battles stay attackable.
 */
class CantAttackUnlessDefenderRule(
    private val predicateEvaluator: PredicateEvaluator
) : AttackDefenderRule {
    override fun check(ctx: AttackCheckContext, defenderId: EntityId): String? {
        val state = ctx.state
        val projected = ctx.projected
        val attackingAPlayer = state.getEntity(defenderId)?.has<LifeTotalComponent>() == true
        val defendingPlayer = findDefendingPlayer(ctx, defenderId)

        fun violated(holder: EntityId, you: EntityId, ability: StaticAbility): CantAttackUnless? = when (ability) {
            is ConditionalStaticAbility ->
                if (predicateEvaluator.conditions.evaluate(state, ability.condition,
                        EffectContext(sourceId = holder, controllerId = you))) {
                    violated(holder, you, ability.ability)
                } else null
            is CompositeStaticAbility -> ability.abilities.firstNotNullOfOrNull { violated(holder, you, it) }
            is CantAttackUnless -> ability.takeIf {
                (attackingAPlayer || it.coversPlaneswalkers) &&
                    coversAttacker(predicateEvaluator, ctx, holder, you, it.filter) &&
                    !predicateEvaluator.conditions.evaluate(state, it.condition,
                        EffectContext(sourceId = holder, controllerId = you, defendingPlayerId = defendingPlayer))
            }
            else -> null
        }

        for (holder in state.getBattlefield()) {
            val container = state.getEntity(holder) ?: continue
            if (container.has<FaceDownComponent>() || projected.hasLostAllAbilities(holder)) continue
            val cardComponent = container.get<CardComponent>() ?: continue
            val cardDef = ctx.cardRegistry.getCard(cardComponent.cardDefinitionId) ?: continue
            val you = projected.getController(holder) ?: continue
            val text = TextChanges.of(state, holder)
            for (ability in RoomFaceStatics.activeStaticAbilities(container, cardDef)) {
                val restriction = violated(holder, you, if (text == null) ability else ability.applyTextReplacement(text))
                    ?: continue
                val name = state.getEntity(ctx.attackerId)?.get<CardComponent>()?.name ?: "Creature"
                return "$name ${restriction.description}"
            }
        }
        return null
    }
}

/**
 * [CantBeAttackedBy]: the defender's battlefield holds a permanent that forbids a whole class of
 * attackers from attacking its controller (CR 508.1c) — Form of the Dragon's "creatures without
 * flying can't attack you", Teferi's Moat's chosen-color variant, Storm, Windrider's "creatures
 * with flying can't attack you".
 *
 * The attacker is matched against the ability's filter with the projection in hand (so granted /
 * removed keywords, type changes and control changes all count) and with the restricting permanent
 * as the predicate source and the defending player as "you", so chosen-color and `youControl()`
 * predicates read off the restriction's own side.
 *
 * "Can't attack **you**" is about the player and nothing else: attacking a planeswalker (or battle)
 * that player controls stays legal — "Unless some effect explicitly says otherwise, a creature that
 * can't attack you can still attack a planeswalker you control" (Form of the Dragon, 2014-02-01
 * ruling; CR 506.3 — only a player, planeswalker or battle can be attacked — and CR 508.1b, which
 * announces which of those each attacker is attacking). So the restriction only fires when the
 * chosen defender *is* the restricting permanent's controller.
 *
 * A face-down permanent has no abilities (CR 708.2), so it never contributes a restriction.
 */
class CantBeAttackedByDefenderRule(
    private val predicateEvaluator: PredicateEvaluator
) : AttackDefenderRule {
    override fun check(ctx: AttackCheckContext, defenderId: EntityId): String? {
        // Attacking a planeswalker/battle a player controls is not attacking *them*, so the only
        // defender this rule speaks about is a player — the one thing on the battlefield-adjacent
        // side of combat that has a life total.
        if (ctx.state.getEntity(defenderId)?.has<LifeTotalComponent>() != true) return null
        val defendingPlayer = defenderId

        fun checkAbility(ability: StaticAbility, source: EntityId): String? {
            return when (ability) {
                is ConditionalStaticAbility -> {
                    if (predicateEvaluator.conditions.evaluate(ctx.state, ability.condition,
                            EffectContext(sourceId = source, controllerId = defendingPlayer))) {
                        checkAbility(ability.ability, source)
                    } else null
                }
                is CompositeStaticAbility ->
                    ability.abilities.firstNotNullOfOrNull { checkAbility(it, source) }
                is CantBeAttackedBy -> {
                    if (predicateEvaluator.matches(ctx.state, ctx.projected, ctx.attackerId,
                            ability.attackerFilter,
                            PredicateContext(controllerId = defendingPlayer, sourceId = source))) {
                        val name = ctx.state.getEntity(ctx.attackerId)?.get<CardComponent>()?.name ?: "Creature"
                        "$name can't attack: ${ability.description}"
                    } else null
                }
                else -> null
            }
        }

        val defenderPermanents = ctx.projected.getBattlefieldControlledBy(defendingPlayer)
        for (permId in defenderPermanents) {
            val container = ctx.state.getEntity(permId) ?: continue
            if (container.has<FaceDownComponent>() || ctx.projected.hasLostAllAbilities(permId)) continue
            val cardComponent = container.get<CardComponent>() ?: continue
            val cardDef = ctx.cardRegistry.getCard(cardComponent.cardDefinitionId) ?: continue
            val text = TextChanges.of(ctx.state, permId)
            for (ability in RoomFaceStatics.activeStaticAbilities(container, cardDef)) {
                checkAbility(if (text == null) ability else ability.applyTextReplacement(text), permId)
                    ?.let { return it }
            }
        }
        // A player-held grant changes the rules of combat, not an object's characteristics.
        // Recheck its filter for every attacker, including creatures entering after it resolved.
        for (grant in ctx.state.grantedStaticAbilities) {
            val holder = grant.entityId
            if (holder != defendingPlayer && holder !in defenderPermanents) continue
            if (holder != defendingPlayer && ctx.projected.hasLostAllAbilities(holder)) continue
            if (!GrantDurationGate.holds(
                    ctx.state, holder, grant.sourceId, grant.duration)) continue
            checkAbility(grant.ability, holder)?.let { return it }
        }
        return null
    }

}

/**
 * [CantAttackYou]: the attacker is covered by a restriction that forbids it from attacking one
 * particular player — "you", the restriction's side — and, when the ability says so, that player's
 * planeswalkers (CR 508.1c). Attacking anyone else, or a battle, stays legal.
 *
 * Restrictions come from two places, read the way [com.wingedsheep.engine.mechanics.combat.BlockStaticRules]
 * reads its rule statics:
 *  - **printed** on a battlefield permanent (Vow of Duty on the creature it enchants, a lord over a
 *    group) — "you" is that permanent's projected controller, and a face-down or ability-stripped
 *    host contributes nothing;
 *  - **granted** by a resolving effect (Promise of Loyalty) — "you" is the granting effect's
 *    controller, recorded on the grant, and the grant's own "for as long as …" duration gates it.
 *    The restriction is the spell's effect, not an ability of the creature, so losing all abilities
 *    doesn't end it.
 *
 * The [CantAttackYou.filter] is resolved relative to the holder and matched on projected state.
 */
class CantAttackYouDefenderRule(
    private val predicateEvaluator: PredicateEvaluator
) : AttackDefenderRule {
    override fun check(ctx: AttackCheckContext, defenderId: EntityId): String? {
        val state = ctx.state
        val projected = ctx.projected
        val isPlayer = state.getEntity(defenderId)?.has<LifeTotalComponent>() == true
        val isPlaneswalker = !isPlayer && projected.isPlaneswalker(defenderId)
        if (!isPlayer && !isPlaneswalker) return null
        val defendingPlayer = if (isPlayer) defenderId else projected.getController(defenderId) ?: return null

        fun violates(holder: EntityId, you: EntityId, ability: StaticAbility): Boolean = when (ability) {
            is ConditionalStaticAbility ->
                predicateEvaluator.conditions.evaluate(state, ability.condition,
                    EffectContext(sourceId = holder, controllerId = you)) &&
                    violates(holder, you, ability.ability)
            is CompositeStaticAbility -> ability.abilities.any { violates(holder, you, it) }
            is CantAttackYou ->
                you == defendingPlayer && (isPlayer || ability.coversPlaneswalkers) &&
                    coversAttacker(predicateEvaluator, ctx, holder, you, ability.filter)
            else -> false
        }

        fun error(): String {
            val name = state.getEntity(ctx.attackerId)?.get<CardComponent>()?.name ?: "Creature"
            return if (isPlayer) "$name can't attack that player" else "$name can't attack that planeswalker"
        }

        val battlefield = state.getBattlefield()
        for (holder in battlefield) {
            val container = state.getEntity(holder) ?: continue
            if (container.has<FaceDownComponent>() || projected.hasLostAllAbilities(holder)) continue
            val cardComponent = container.get<CardComponent>() ?: continue
            val cardDef = ctx.cardRegistry.getCard(cardComponent.cardDefinitionId) ?: continue
            val you = projected.getController(holder) ?: continue
            val text = TextChanges.of(state, holder)
            for (ability in RoomFaceStatics.activeStaticAbilities(container, cardDef)) {
                if (violates(holder, you, if (text == null) ability else ability.applyTextReplacement(text))) {
                    return error()
                }
            }
        }
        for (grant in state.grantedStaticAbilities) {
            if (grant.entityId !in battlefield) continue
            if (!GrantDurationGate.holds(state, grant.entityId, grant.sourceId, grant.duration)) continue
            val you = grant.controllerId ?: projected.getController(grant.entityId) ?: continue
            if (violates(grant.entityId, you, grant.ability)) return error()
        }
        return null
    }
}

/** Prevents an attached permanent with [CantBeAttackedWhileAttached] from being attacked. */
class CantBeAttackedWhileAttachedDefenderRule : AttackDefenderRule {
    override fun check(ctx: AttackCheckContext, defenderId: EntityId): String? {
        val defender = ctx.state.getEntity(defenderId) ?: return null
        if (defender.get<com.wingedsheep.engine.state.components.battlefield.AttachedToComponent>() == null) {
            return null
        }
        val card = defender.get<CardComponent>() ?: return null
        val cardDef = ctx.cardRegistry.getCard(card.cardDefinitionId) ?: return null
        if (cardDef.staticAbilities.none { it is CantBeAttackedWhileAttached }) return null
        return "${card.name} can't be attacked while it's attached"
    }
}

/**
 * AttackMode (CR 802 / 803): when the game uses attack-left or attack-right, a creature may only
 * attack the opponent in the adjacent seat (or a planeswalker that opponent controls or a battle they protect). The
 * set of legal opponents is computed centrally by
 * [com.wingedsheep.engine.mechanics.combat.CombatDefenders.legalDefendingPlayers], so this rule and
 * the legal-action enumerator never disagree. A no-op under [com.wingedsheep.sdk.core.AttackMode.MULTIPLE]
 * (and in any two-player game, where every mode permits the sole opponent).
 */
class AttackModeDefenderRule : AttackDefenderRule {
    override fun check(ctx: AttackCheckContext, defenderId: EntityId): String? {
        if (ctx.state.attackMode == com.wingedsheep.sdk.core.AttackMode.MULTIPLE) return null
        val defendingPlayer = findDefendingPlayer(ctx, defenderId)
        val legal = com.wingedsheep.engine.mechanics.combat.CombatDefenders
            .legalDefendingPlayers(ctx.state, ctx.attackingPlayer)
        if (defendingPlayer in legal) return null
        val side = when (ctx.state.attackMode) {
            com.wingedsheep.sdk.core.AttackMode.LEFT -> "the player to your left"
            com.wingedsheep.sdk.core.AttackMode.RIGHT -> "the player to your right"
            com.wingedsheep.sdk.core.AttackMode.MULTIPLE -> "an eligible player"
        }
        return "Can only attack $side"
    }
}

// =========================================================================
// Shared helpers
// =========================================================================


/**
 * Whether a restriction's [filter], printed on [holder] (controlled by [you]), covers the attacker:
 * its scope (the holder itself, everything, the creature it's attached to, …) plus the base filter,
 * matched on projected state.
 */
private fun coversAttacker(
    predicateEvaluator: PredicateEvaluator,
    ctx: AttackCheckContext,
    holder: EntityId,
    you: EntityId,
    filter: com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
): Boolean {
    val attacker = ctx.attackerId
    val inScope = when (val scope = filter.scope) {
        Scope.Self -> holder == attacker
        Scope.Battlefield -> true
        Scope.AttachedTo -> ctx.state.getEntity(holder)?.get<AttachedToComponent>()?.targetId == attacker
        Scope.SoulbondPair -> SoulbondPairing.isInPairOf(ctx.state, holder, attacker)
        Scope.ChosenCreature -> ctx.state.chosenCreatureOf(holder) == attacker
        is Scope.Specific -> scope.entityId == attacker
    }
    return inScope && !(filter.excludeSelf && holder == attacker) &&
        predicateEvaluator.matches(ctx.state, ctx.projected, attacker, filter.baseFilter,
            PredicateContext(sourceId = holder, controllerId = you))
}

private fun findDefendingPlayer(ctx: AttackCheckContext, defenderId: EntityId): EntityId {
    if (ctx.state.getEntity(defenderId)?.has<LifeTotalComponent>() == true) {
        return defenderId
    }
    // A battle is defended by its protector, not its controller (CR 310.9d).
    return CombatDefenders.defendingPlayerOf(ctx.state, defenderId)
}

// =========================================================================
// Default rule lists
// =========================================================================

/**
 * [com.wingedsheep.sdk.scripting.CantAttackUnlessSacrifice]: the creature carries a sacrifice cost
 * for attacking (Leviathan — "can't attack unless you sacrifice two Islands"). This rule only
 * enforces the half that can be decided up front: a cost you *cannot* pay makes the declaration
 * illegal, so the creature can't attack when its controller doesn't control enough matching
 * permanents. Actually paying it is a pause in the declare-attackers step, in the same window the
 * generic-mana attack tax is paid — see `AttackPhaseManager.pauseForAttackSacrifice`.
 *
 * The cost is per *creature*, so two Leviathans attacking together owe two sacrifices of two
 * Islands each; the affordability check totals them.
 */
class CantAttackUnlessSacrificeRule(
    private val predicateEvaluator: PredicateEvaluator
) : AttackRestrictionRule {
    override fun check(ctx: AttackCheckContext): String? {
        val requirement = AttackSacrificeCosts.requirementFor(ctx.state, ctx.attackerId, ctx.cardRegistry)
            ?: return null
        val available = AttackSacrificeCosts.eligiblePermanents(
            ctx.state, ctx.attackingPlayer, ctx.attackerId, requirement,
            predicateEvaluator = predicateEvaluator
        )
        if (available.size < requirement.count) {
            val name = ctx.state.getEntity(ctx.attackerId)?.get<CardComponent>()?.name ?: "Creature"
            return "$name ${requirement.description}"
        }
        return null
    }
}

fun defaultAttackRestrictionRules(predicateEvaluator: PredicateEvaluator): List<AttackRestrictionRule> = listOf(
    MustBeCreatureAttackRule(),
    ControlledByAttackerRule(),
    MustBeUntappedAttackRule(),
    SummoningSicknessAttackRule(),
    DefenderAttackRule(predicateEvaluator = predicateEvaluator),
    CantAttackProjectedRule(),
    CantAttackUnlessSacrificeRule(predicateEvaluator = predicateEvaluator),
    AdditionalCombatPhaseAttackerRule(predicateEvaluator = predicateEvaluator),
    NotAlreadyAttackingRule()
)

fun defaultAttackDefenderRules(predicateEvaluator: PredicateEvaluator): List<AttackDefenderRule> = listOf(
    CantAttackUnlessDefenderRule(predicateEvaluator = predicateEvaluator),
    CantBeAttackedByDefenderRule(predicateEvaluator = predicateEvaluator),
    CantAttackYouDefenderRule(predicateEvaluator = predicateEvaluator),
    CantBeAttackedWhileAttachedDefenderRule(),
    AttackModeDefenderRule()
)
