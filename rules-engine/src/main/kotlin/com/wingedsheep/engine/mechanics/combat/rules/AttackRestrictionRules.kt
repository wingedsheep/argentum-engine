package com.wingedsheep.engine.mechanics.combat.rules

import com.wingedsheep.engine.handlers.ConditionEvaluator
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
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.player.InAdditionalCombatPhaseComponent
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.CantAttackUnless
import com.wingedsheep.sdk.scripting.CantBeAttackedBy
import com.wingedsheep.sdk.scripting.CantBeAttackedWhileAttached

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
 * Cannot have summoning sickness (unless it has haste).
 */
class SummoningSicknessAttackRule : AttackRestrictionRule {
    override fun check(ctx: AttackCheckContext): String? {
        val container = ctx.state.getEntity(ctx.attackerId) ?: return null
        val hasHaste = ctx.projected.hasKeyword(ctx.attackerId, Keyword.HASTE)
        if (!hasHaste && container.has<SummoningSicknessComponent>()) {
            val name = container.get<CardComponent>()?.name ?: "Creature"
            return "$name has summoning sickness"
        }
        return null
    }
}

/**
 * Cannot have defender keyword, unless the creature has a conditional ability to bypass defender.
 */
class DefenderAttackRule : AttackRestrictionRule {
    override fun check(ctx: AttackCheckContext): String? {
        if (!ctx.projected.hasKeyword(ctx.attackerId, Keyword.DEFENDER)) return null

        // The Defender restriction is lifted by a temporary "attack this turn as though it didn't
        // have defender" grant or a satisfied CanAttackDespiteDefender static ability. Both live in
        // DefenderBypass so this enforcement path and the client's "can attack" badge agree exactly.
        if (DefenderBypass.isActive(ctx.state, ctx.attackerId, ctx.attackingPlayer, ctx.cardRegistry)) return null

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
class AdditionalCombatPhaseAttackerRule : AttackRestrictionRule {
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
        private val predicateEvaluator = PredicateEvaluator()
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
 * CantAttackUnless: creature can't attack unless a condition is met relative
 * to the defender (e.g., Goblin Goon — "can't attack unless you control more
 * creatures than defending player").
 */
class CantAttackUnlessDefenderRule : AttackDefenderRule {
    override fun check(ctx: AttackCheckContext, defenderId: EntityId): String? {
        val container = ctx.state.getEntity(ctx.attackerId) ?: return null
        if (container.has<FaceDownComponent>()) return null
        val cardComponent = container.get<CardComponent>() ?: return null
        val cardDef = ctx.cardRegistry.getCard(cardComponent.cardDefinitionId) ?: return null

        val restriction = cardDef.staticAbilities
            .filterIsInstance<CantAttackUnless>()
            .firstOrNull { it.filter.scope is com.wingedsheep.sdk.scripting.filters.unified.Scope.Self } ?: return null

        val defendingPlayer = findDefendingPlayer(ctx, defenderId)

        val effectContext = EffectContext(
            sourceId = ctx.attackerId,
            controllerId = ctx.attackingPlayer,
            defendingPlayerId = defendingPlayer,
        )
        if (!conditionEvaluator.evaluate(ctx.state, restriction.condition, effectContext)) {
            return "${cardComponent.name} ${restriction.description}"
        }
        return null
    }

    companion object {
        private val conditionEvaluator = ConditionEvaluator()
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
class CantBeAttackedByDefenderRule : AttackDefenderRule {
    override fun check(ctx: AttackCheckContext, defenderId: EntityId): String? {
        // Attacking a planeswalker/battle a player controls is not attacking *them*, so the only
        // defender this rule speaks about is a player — the one thing on the battlefield-adjacent
        // side of combat that has a life total.
        if (ctx.state.getEntity(defenderId)?.has<LifeTotalComponent>() != true) return null
        val defendingPlayer = defenderId

        val defenderPermanents = ctx.projected.getBattlefieldControlledBy(defendingPlayer)
        for (permId in defenderPermanents) {
            val container = ctx.state.getEntity(permId) ?: continue
            if (container.has<FaceDownComponent>()) continue
            val cardComponent = container.get<CardComponent>() ?: continue
            val cardDef = ctx.cardRegistry.getCard(cardComponent.cardDefinitionId) ?: continue
            for (ability in cardDef.staticAbilities) {
                if (ability is CantBeAttackedBy) {
                    val matches = predicateEvaluator.matches(
                        ctx.state,
                        ctx.projected,
                        ctx.attackerId,
                        ability.attackerFilter,
                        PredicateContext(controllerId = defendingPlayer, sourceId = permId)
                    )
                    if (matches) {
                        val attackerName = ctx.state.getEntity(ctx.attackerId)?.get<CardComponent>()?.name ?: "Creature"
                        return "$attackerName can't attack: ${ability.description}"
                    }
                }
            }
        }
        return null
    }

    companion object {
        private val predicateEvaluator = PredicateEvaluator()
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
 * attack the opponent in the adjacent seat (or a planeswalker/battle that opponent controls). The
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

private fun findDefendingPlayer(ctx: AttackCheckContext, defenderId: EntityId): EntityId {
    if (ctx.state.getEntity(defenderId)?.has<LifeTotalComponent>() == true) {
        return defenderId
    }
    return ctx.state.getEntity(defenderId)?.get<ControllerComponent>()?.playerId ?: defenderId
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
class CantAttackUnlessSacrificeRule : AttackRestrictionRule {
    override fun check(ctx: AttackCheckContext): String? {
        val requirement = AttackSacrificeCosts.requirementFor(ctx.state, ctx.attackerId, ctx.cardRegistry)
            ?: return null
        val available = AttackSacrificeCosts.eligiblePermanents(
            ctx.state, ctx.attackingPlayer, ctx.attackerId, requirement
        )
        if (available.size < requirement.count) {
            val name = ctx.state.getEntity(ctx.attackerId)?.get<CardComponent>()?.name ?: "Creature"
            return "$name ${requirement.description}"
        }
        return null
    }
}

fun defaultAttackRestrictionRules(): List<AttackRestrictionRule> = listOf(
    MustBeCreatureAttackRule(),
    ControlledByAttackerRule(),
    MustBeUntappedAttackRule(),
    SummoningSicknessAttackRule(),
    DefenderAttackRule(),
    CantAttackProjectedRule(),
    CantAttackUnlessSacrificeRule(),
    AdditionalCombatPhaseAttackerRule(),
    NotAlreadyAttackingRule()
)

fun defaultAttackDefenderRules(): List<AttackDefenderRule> = listOf(
    CantAttackUnlessDefenderRule(),
    CantBeAttackedByDefenderRule(),
    CantBeAttackedWhileAttachedDefenderRule(),
    AttackModeDefenderRule()
)
