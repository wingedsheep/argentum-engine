package com.wingedsheep.engine.mechanics.combat.rules

import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.CanAttackDespiteDefender
import com.wingedsheep.sdk.scripting.CantAttackUnless
import com.wingedsheep.sdk.scripting.CantBeAttackedWithout

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
        val controller = ctx.projected.getController(ctx.attackerId)
        if (controller != ctx.attackingPlayer) {
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
 * Cannot have defender keyword, unless a CanAttackDespiteDefender ability's condition is met.
 */
class DefenderAttackRule : AttackRestrictionRule {
    override fun check(ctx: AttackCheckContext): String? {
        if (!ctx.projected.hasKeyword(ctx.attackerId, Keyword.DEFENDER)) return null

        val container = ctx.state.getEntity(ctx.attackerId) ?: return errorMsg(ctx)
        val cardComp = container.get<CardComponent>() ?: return errorMsg(ctx)
        val cardDef = ctx.cardRegistry.getCard(cardComp.cardDefinitionId) ?: return errorMsg(ctx)

        val effectContext = EffectContext(sourceId = ctx.attackerId, controllerId = ctx.attackingPlayer, opponentId = null)
        val canAttackDespite = cardDef.staticAbilities
            .filterIsInstance<CanAttackDespiteDefender>()
            .any { conditionEvaluator.evaluate(ctx.state, it.condition, effectContext) }

        if (canAttackDespite) return null
        return errorMsg(ctx)
    }

    private fun errorMsg(ctx: AttackCheckContext): String {
        val name = ctx.state.getEntity(ctx.attackerId)?.get<CardComponent>()?.name ?: "Creature"
        return "$name has defender and cannot attack"
    }

    companion object {
        private val conditionEvaluator = ConditionEvaluator()
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
            opponentId = defendingPlayer
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
 * CantBeAttackedWithout: defender's battlefield has a permanent requiring
 * attackers to have a specific keyword (e.g., Form of the Dragon —
 * "creatures without flying can't attack you").
 */
class CantBeAttackedWithoutDefenderRule : AttackDefenderRule {
    override fun check(ctx: AttackCheckContext, defenderId: EntityId): String? {
        val defendingPlayer = findDefendingPlayer(ctx, defenderId)

        val defenderPermanents = ctx.projected.getBattlefieldControlledBy(defendingPlayer)
        for (permId in defenderPermanents) {
            val container = ctx.state.getEntity(permId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue
            val cardDef = ctx.cardRegistry.getCard(cardComponent.cardDefinitionId) ?: continue
            for (ability in cardDef.staticAbilities) {
                if (ability is CantBeAttackedWithout) {
                    if (!ctx.projected.hasKeyword(ctx.attackerId, ability.requiredKeyword)) {
                        val attackerName = ctx.state.getEntity(ctx.attackerId)?.get<CardComponent>()?.name ?: "Creature"
                        return "$attackerName can't attack: ${ability.description}"
                    }
                }
            }
        }
        return null
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

fun defaultAttackRestrictionRules(): List<AttackRestrictionRule> = listOf(
    MustBeCreatureAttackRule(),
    ControlledByAttackerRule(),
    MustBeUntappedAttackRule(),
    SummoningSicknessAttackRule(),
    DefenderAttackRule(),
    CantAttackProjectedRule(),
    NotAlreadyAttackingRule()
)

fun defaultAttackDefenderRules(): List<AttackDefenderRule> = listOf(
    CantAttackUnlessDefenderRule(),
    CantBeAttackedWithoutDefenderRule()
)
