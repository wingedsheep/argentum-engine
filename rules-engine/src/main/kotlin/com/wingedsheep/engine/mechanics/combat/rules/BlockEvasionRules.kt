package com.wingedsheep.engine.mechanics.combat.rules

import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.scripting.CantBeBlockedBy
import com.wingedsheep.sdk.scripting.CantBeBlockedExceptBy
import com.wingedsheep.sdk.scripting.CantBeBlockedIfCastSpellType
import com.wingedsheep.sdk.scripting.CantBeBlockedUnlessDefenderSharesCreatureType
import com.wingedsheep.sdk.scripting.GrantCantBeBlockedToSmallCreatures
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate

/**
 * Unblockable: Cannot be blocked at all (CANT_BE_BLOCKED flag).
 */
class UnblockableRule : BlockEvasionRule {
    override fun check(ctx: BlockCheckContext): String? {
        if (ctx.projected.hasKeyword(ctx.attackerId, com.wingedsheep.sdk.core.AbilityFlag.CANT_BE_BLOCKED)) {
            val attackerName = ctx.state.getEntity(ctx.attackerId)?.get<CardComponent>()?.name ?: "Creature"
            return "$attackerName can't be blocked"
        }
        return null
    }
}

/**
 * Flying: Can only be blocked by creatures with flying or reach.
 */
class FlyingRule : BlockEvasionRule {
    override fun check(ctx: BlockCheckContext): String? {
        if (ctx.projected.hasKeyword(ctx.attackerId, Keyword.FLYING)) {
            val canBlockFlying = ctx.projected.hasKeyword(ctx.blockerId, Keyword.FLYING) ||
                ctx.projected.hasKeyword(ctx.blockerId, Keyword.REACH)
            if (!canBlockFlying) {
                val blockerName = ctx.state.getEntity(ctx.blockerId)?.get<CardComponent>()?.name ?: "Creature"
                val attackerName = ctx.state.getEntity(ctx.attackerId)?.get<CardComponent>()?.name ?: "Creature"
                return "$blockerName cannot block $attackerName (flying)"
            }
        }
        return null
    }
}

/**
 * Horsemanship: Can only be blocked by creatures with horsemanship.
 */
class HorsemanshipRule : BlockEvasionRule {
    override fun check(ctx: BlockCheckContext): String? {
        if (ctx.projected.hasKeyword(ctx.attackerId, Keyword.HORSEMANSHIP)) {
            if (!ctx.projected.hasKeyword(ctx.blockerId, Keyword.HORSEMANSHIP)) {
                val blockerName = ctx.state.getEntity(ctx.blockerId)?.get<CardComponent>()?.name ?: "Creature"
                val attackerName = ctx.state.getEntity(ctx.attackerId)?.get<CardComponent>()?.name ?: "Creature"
                return "$blockerName cannot block $attackerName (horsemanship)"
            }
        }
        return null
    }
}

/**
 * Shadow: Can only be blocked by creatures with shadow.
 */
class ShadowRule : BlockEvasionRule {
    override fun check(ctx: BlockCheckContext): String? {
        if (ctx.projected.hasKeyword(ctx.attackerId, Keyword.SHADOW)) {
            if (!ctx.projected.hasKeyword(ctx.blockerId, Keyword.SHADOW)) {
                val blockerName = ctx.state.getEntity(ctx.blockerId)?.get<CardComponent>()?.name ?: "Creature"
                val attackerName = ctx.state.getEntity(ctx.attackerId)?.get<CardComponent>()?.name ?: "Creature"
                return "$blockerName cannot block $attackerName (shadow)"
            }
        }
        return null
    }
}

/**
 * Fear: Can only be blocked by artifact creatures or black creatures.
 */
class FearRule : BlockEvasionRule {
    override fun check(ctx: BlockCheckContext): String? {
        if (ctx.projected.hasKeyword(ctx.attackerId, Keyword.FEAR)) {
            val blockerCard = ctx.state.getEntity(ctx.blockerId)?.get<CardComponent>() ?: return null
            val isArtifactCreature = blockerCard.typeLine.isArtifactCreature
            val isBlackCreature = Color.BLACK in blockerCard.colors
            if (!isArtifactCreature && !isBlackCreature) {
                val attackerName = ctx.state.getEntity(ctx.attackerId)?.get<CardComponent>()?.name ?: "Creature"
                return "${blockerCard.name} cannot block $attackerName (fear)"
            }
        }
        return null
    }
}

/**
 * Landwalk: Cannot be blocked if defending player controls land of that type.
 */
class LandwalkRule : BlockEvasionRule {

    private val landwalkToSubtype = mapOf(
        Keyword.FORESTWALK to Subtype.FOREST,
        Keyword.SWAMPWALK to Subtype.SWAMP,
        Keyword.ISLANDWALK to Subtype.ISLAND,
        Keyword.MOUNTAINWALK to Subtype.MOUNTAIN,
        Keyword.PLAINSWALK to Subtype.PLAINS
    )

    override fun check(ctx: BlockCheckContext): String? {
        val attackerName = ctx.state.getEntity(ctx.attackerId)?.get<CardComponent>()?.name ?: "Creature"
        for ((landwalkKeyword, landSubtype) in landwalkToSubtype) {
            if (ctx.projected.hasKeyword(ctx.attackerId, landwalkKeyword)) {
                if (playerControlsLandWithSubtype(ctx, landSubtype)) {
                    return "$attackerName has ${landwalkKeyword.displayName} and cannot be blocked"
                }
            }
        }
        return null
    }

    private fun playerControlsLandWithSubtype(ctx: BlockCheckContext, landSubtype: Subtype): Boolean {
        return ctx.state.getBattlefield().any { entityId ->
            val container = ctx.state.getEntity(entityId) ?: return@any false
            val cardComponent = container.get<CardComponent>() ?: return@any false
            val controller = ctx.projected.getController(entityId)
            controller == ctx.blockingPlayer &&
                cardComponent.typeLine.isLand &&
                cardComponent.typeLine.hasSubtype(landSubtype)
        }
    }
}

/**
 * CantBeBlockedBy: Cannot be blocked by creatures matching a GameObjectFilter.
 * Unified rule replacing CantBeBlockedBySubtypeRule, CantBeBlockedByPowerRule,
 * CantBeBlockedByPowerOrLessRule, and the previously missing CantBeBlockedByColor handling.
 */
class CantBeBlockedByRule(
    private val predicateEvaluator: PredicateEvaluator
) : BlockEvasionRule {
    override fun check(ctx: BlockCheckContext): String? {
        val attackerCard = ctx.state.getEntity(ctx.attackerId)?.get<CardComponent>() ?: return null
        val cardDef = ctx.cardRegistry.getCard(attackerCard.cardDefinitionId) ?: return null
        val restrictions = cardDef.staticAbilities.filterIsInstance<CantBeBlockedBy>()
        if (restrictions.isEmpty()) return null

        val attackerController = ctx.projected.getController(ctx.attackerId) ?: return null
        val predicateContext = PredicateContext(controllerId = attackerController, sourceId = ctx.attackerId)

        for (restriction in restrictions) {
            if (predicateEvaluator.matchesWithProjection(
                    ctx.state, ctx.projected, ctx.blockerId, restriction.blockerFilter, predicateContext
                )
            ) {
                val blockerName = ctx.state.getEntity(ctx.blockerId)?.get<CardComponent>()?.name ?: "Creature"
                return "$blockerName cannot block ${attackerCard.name} (${restriction.description})"
            }
        }
        return null
    }
}

/**
 * CantBeBlockedExceptByColor: Can only be blocked by creatures of the specified color (floating effect).
 */
class CantBeBlockedExceptByColorRule : BlockEvasionRule {
    override fun check(ctx: BlockCheckContext): String? {
        val colorRestriction = ctx.state.floatingEffects
            .filter { floatingEffect ->
                floatingEffect.effect.modification is SerializableModification.CantBeBlockedExceptByColor &&
                    ctx.attackerId in floatingEffect.effect.affectedEntities
            }
            .map { it.effect.modification as SerializableModification.CantBeBlockedExceptByColor }
            .firstOrNull()
            ?: return null

        val blockerCard = ctx.state.getEntity(ctx.blockerId)?.get<CardComponent>() ?: return null
        val requiredColor = Color.valueOf(colorRestriction.color)
        if (!blockerCard.colors.contains(requiredColor)) {
            val attackerName = ctx.state.getEntity(ctx.attackerId)?.get<CardComponent>()?.name ?: "Creature"
            return "${blockerCard.name} cannot block $attackerName (can only be blocked by ${requiredColor.displayName.lowercase()} creatures)"
        }
        return null
    }
}

/**
 * CantBeBlockedByColor: Cannot be blocked by creatures of the specified color (floating effect).
 */
class CantBeBlockedByColorRule : BlockEvasionRule {
    override fun check(ctx: BlockCheckContext): String? {
        val colorRestriction = ctx.state.floatingEffects
            .filter { floatingEffect ->
                floatingEffect.effect.modification is SerializableModification.CantBeBlockedByColor &&
                    ctx.attackerId in floatingEffect.effect.affectedEntities
            }
            .map { it.effect.modification as SerializableModification.CantBeBlockedByColor }
            .firstOrNull()
            ?: return null

        val blockerCard = ctx.state.getEntity(ctx.blockerId)?.get<CardComponent>() ?: return null
        val forbiddenColor = Color.valueOf(colorRestriction.color)
        if (blockerCard.colors.contains(forbiddenColor)) {
            val attackerName = ctx.state.getEntity(ctx.attackerId)?.get<CardComponent>()?.name ?: "Creature"
            return "${blockerCard.name} cannot block $attackerName (can't be blocked by ${forbiddenColor.displayName.lowercase()} creatures)"
        }
        return null
    }
}

/**
 * CantBeBlockedExceptBy: Can only be blocked by creatures matching a GameObjectFilter.
 * Unified rule replacing CantBeBlockedExceptByKeywordRule.
 */
class CantBeBlockedExceptByRule(
    private val predicateEvaluator: PredicateEvaluator
) : BlockEvasionRule {
    override fun check(ctx: BlockCheckContext): String? {
        val attackerCard = ctx.state.getEntity(ctx.attackerId)?.get<CardComponent>() ?: return null
        val cardDef = ctx.cardRegistry.getCard(attackerCard.cardDefinitionId) ?: return null
        val restrictions = cardDef.staticAbilities.filterIsInstance<CantBeBlockedExceptBy>()
        if (restrictions.isEmpty()) return null

        val attackerController = ctx.projected.getController(ctx.attackerId) ?: return null
        val predicateContext = PredicateContext(controllerId = attackerController, sourceId = ctx.attackerId)

        for (restriction in restrictions) {
            if (predicateEvaluator.matchesWithProjection(
                    ctx.state, ctx.projected, ctx.blockerId, restriction.blockerFilter, predicateContext
                )
            ) continue

            // Per MTG rules, reach allows blocking creatures with "can't be blocked except by flying"
            if (filterRequiresFlying(restriction.blockerFilter) &&
                ctx.projected.hasKeyword(ctx.blockerId, Keyword.REACH)
            ) continue

            val blockerName = ctx.state.getEntity(ctx.blockerId)?.get<CardComponent>()?.name ?: "Creature"
            return "$blockerName cannot block ${attackerCard.name} (${restriction.description})"
        }
        return null
    }

    private fun filterRequiresFlying(filter: GameObjectFilter): Boolean {
        return filter.cardPredicates.any { it is CardPredicate.HasKeyword && it.keyword == Keyword.FLYING }
    }
}

/**
 * CantBeBlockedExceptBySubtype: Can only be blocked by creatures with a specific subtype (projected).
 */
class CantBeBlockedExceptBySubtypeRule : BlockEvasionRule {
    override fun check(ctx: BlockCheckContext): String? {
        val requiredSubtypes = ctx.projected.getCantBeBlockedExceptBySubtypes(ctx.attackerId)
        if (requiredSubtypes.isEmpty()) return null

        val blockerName = ctx.state.getEntity(ctx.blockerId)?.get<CardComponent>()?.name ?: "Creature"
        val attackerName = ctx.state.getEntity(ctx.attackerId)?.get<CardComponent>()?.name ?: "Creature"

        for (requiredSubtype in requiredSubtypes) {
            if (!ctx.projected.hasSubtype(ctx.blockerId, requiredSubtype)) {
                return "$blockerName cannot block $attackerName (can only be blocked by ${requiredSubtype}s)"
            }
        }
        return null
    }
}

/**
 * CantBeBlockedUnlessDefenderSharesCreatureType: Attacker can't be blocked unless
 * the defending player controls N+ creatures sharing a creature type (e.g. Graxiplon).
 */
class CantBeBlockedUnlessDefenderSharesCreatureTypeRule : BlockEvasionRule {
    override fun check(ctx: BlockCheckContext): String? {
        val attackerCard = ctx.state.getEntity(ctx.attackerId)?.get<CardComponent>() ?: return null
        val cardDef = ctx.cardRegistry.getCard(attackerCard.cardDefinitionId) ?: return null
        val restriction = cardDef.staticAbilities
            .filterIsInstance<CantBeBlockedUnlessDefenderSharesCreatureType>().firstOrNull()
            ?: return null

        if (!defenderHasEnoughSharedCreatureTypes(ctx, restriction.minSharedCount)) {
            return "${attackerCard.name} can't be blocked unless you control ${restriction.minSharedCount} or more creatures that share a creature type"
        }
        return null
    }

    private fun defenderHasEnoughSharedCreatureTypes(ctx: BlockCheckContext, minCount: Int): Boolean {
        val creatures = ctx.projected.getBattlefieldControlledBy(ctx.blockingPlayer).filter { entityId ->
            val card = ctx.state.getEntity(entityId)?.get<CardComponent>() ?: return@filter false
            card.typeLine.isCreature
        }

        val subtypeCounts = mutableMapOf<String, Int>()
        for (entityId in creatures) {
            for (subtype in ctx.projected.getSubtypes(entityId)) {
                subtypeCounts[subtype] = (subtypeCounts[subtype] ?: 0) + 1
            }
        }
        return subtypeCounts.values.any { it >= minCount }
    }
}

/**
 * Protection from color: Attacker can't be blocked by creatures of a color it has protection from.
 */
class ProtectionFromColorRule : BlockEvasionRule {
    override fun check(ctx: BlockCheckContext): String? {
        val attackerName = ctx.state.getEntity(ctx.attackerId)?.get<CardComponent>()?.name ?: "Creature"
        val blockerName = ctx.state.getEntity(ctx.blockerId)?.get<CardComponent>()?.name ?: "Creature"

        for (colorName in ctx.projected.getColors(ctx.blockerId)) {
            if (ctx.projected.hasKeyword(ctx.attackerId, "PROTECTION_FROM_$colorName")) {
                return "$attackerName has protection from ${colorName.lowercase()} and can't be blocked by $blockerName"
            }
        }
        return null
    }
}

/**
 * Protection from each opponent: Attacker can't be blocked by creatures controlled by
 * any of its controller's opponents (Rule 702.16e).
 */
class ProtectionFromEachOpponentRule : BlockEvasionRule {
    override fun check(ctx: BlockCheckContext): String? {
        if (!ctx.projected.hasKeyword(ctx.attackerId, "PROTECTION_FROM_EACH_OPPONENT")) return null

        val attackerController = ctx.projected.getController(ctx.attackerId) ?: return null
        val blockerController = ctx.projected.getController(ctx.blockerId) ?: return null
        if (blockerController == attackerController) return null

        val attackerName = ctx.state.getEntity(ctx.attackerId)?.get<CardComponent>()?.name ?: "Creature"
        val blockerName = ctx.state.getEntity(ctx.blockerId)?.get<CardComponent>()?.name ?: "Creature"
        return "$attackerName has protection from each of its controller's opponents and can't be blocked by $blockerName"
    }
}

/**
 * Protection from subtype: Attacker can't be blocked by creatures of a subtype it has protection from.
 */
class ProtectionFromSubtypeRule : BlockEvasionRule {
    override fun check(ctx: BlockCheckContext): String? {
        val attackerName = ctx.state.getEntity(ctx.attackerId)?.get<CardComponent>()?.name ?: "Creature"
        val blockerName = ctx.state.getEntity(ctx.blockerId)?.get<CardComponent>()?.name ?: "Creature"

        for (subtype in ctx.projected.getSubtypes(ctx.blockerId)) {
            if (ctx.projected.hasKeyword(ctx.attackerId, "PROTECTION_FROM_SUBTYPE_${subtype.uppercase()}")) {
                return "$attackerName has protection from ${subtype.lowercase()}s and can't be blocked by $blockerName"
            }
        }
        return null
    }
}

/**
 * CanOnlyBlockCreaturesWithKeyword: Blocker can only block creatures with a specific keyword
 * (e.g. Cloud Pirates can block only creatures with flying).
 */
class CanOnlyBlockCreaturesWithKeywordRule : BlockEvasionRule {
    override fun check(ctx: BlockCheckContext): String? {
        // Face-down creatures have no abilities — restriction doesn't apply
        if (ctx.state.getEntity(ctx.blockerId)?.has<FaceDownComponent>() == true) return null
        val blockerCard = ctx.state.getEntity(ctx.blockerId)?.get<CardComponent>() ?: return null
        val cardDef = ctx.cardRegistry.getCard(blockerCard.cardDefinitionId) ?: return null
        val restriction = cardDef.staticAbilities
            .filterIsInstance<com.wingedsheep.sdk.scripting.CanOnlyBlockCreaturesWithKeyword>().firstOrNull()
            ?: return null

        if (restriction.target != com.wingedsheep.sdk.scripting.StaticTarget.SourceCreature) return null

        if (!ctx.projected.hasKeyword(ctx.attackerId, restriction.keyword)) {
            return "${blockerCard.name} can block only creatures with ${restriction.keyword.displayName.lowercase()}"
        }
        return null
    }
}

/**
 * CantBlockCreaturesWithGreaterPower: Blocker can't block creatures whose power exceeds
 * the blocker's power (e.g. Spitfire Handler).
 */
class CantBlockCreaturesWithGreaterPowerRule : BlockEvasionRule {
    override fun check(ctx: BlockCheckContext): String? {
        // Face-down creatures have no abilities — restriction doesn't apply
        if (ctx.state.getEntity(ctx.blockerId)?.has<FaceDownComponent>() == true) return null
        val blockerCard = ctx.state.getEntity(ctx.blockerId)?.get<CardComponent>() ?: return null
        val cardDef = ctx.cardRegistry.getCard(blockerCard.cardDefinitionId) ?: return null
        val restriction = cardDef.staticAbilities
            .filterIsInstance<com.wingedsheep.sdk.scripting.CantBlockCreaturesWithGreaterPower>().firstOrNull()
            ?: return null

        if (restriction.target != com.wingedsheep.sdk.scripting.StaticTarget.SourceCreature) return null

        val blockerPower = ctx.projected.getPower(ctx.blockerId) ?: 0
        val attackerPower = ctx.projected.getPower(ctx.attackerId) ?: 0

        if (attackerPower > blockerPower) {
            val attackerName = ctx.state.getEntity(ctx.attackerId)?.get<CardComponent>()?.name ?: "Creature"
            return "${blockerCard.name} can't block $attackerName (power $attackerPower is greater than ${blockerCard.name}'s power $blockerPower)"
        }
        return null
    }
}

/**
 * GrantCantBeBlockedToSmallCreatures: Attacker can't be blocked if it has power or toughness
 * at most N and its controller controls a permanent with this static ability
 * (e.g., Tetsuko Umezawa, Fugitive).
 *
 * Scans the attacking player's battlefield for permanents with GrantCantBeBlockedToSmallCreatures,
 * then checks whether the attacker's projected power or toughness meets the threshold.
 */
class GrantCantBeBlockedToSmallCreaturesRule : BlockEvasionRule {
    override fun check(ctx: BlockCheckContext): String? {
        val attackerController = ctx.projected.getController(ctx.attackerId) ?: return null
        val attackerPower = ctx.projected.getPower(ctx.attackerId) ?: return null
        val attackerToughness = ctx.projected.getToughness(ctx.attackerId) ?: return null

        // Scan the attacking player's battlefield for permanents with this ability
        for (entityId in ctx.projected.getBattlefieldControlledBy(attackerController)) {
            val card = ctx.state.getEntity(entityId)?.get<CardComponent>() ?: continue
            val cardDef = ctx.cardRegistry.getCard(card.cardDefinitionId) ?: continue
            for (ability in cardDef.staticAbilities.filterIsInstance<GrantCantBeBlockedToSmallCreatures>()) {
                if (attackerPower <= ability.maxValue || attackerToughness <= ability.maxValue) {
                    val attackerName = ctx.state.getEntity(ctx.attackerId)?.get<CardComponent>()?.name ?: "Creature"
                    return "$attackerName can't be blocked (power or toughness ${ability.maxValue} or less)"
                }
            }
        }
        return null
    }
}

/**
 * CantBeBlockedIfCastSpellType: Can't be blocked if the attacker's controller
 * has cast a spell matching the filter this turn (e.g., Relic Runner + historic).
 */
class CantBeBlockedIfCastSpellTypeRule(
    private val predicateEvaluator: com.wingedsheep.engine.handlers.PredicateEvaluator
) : BlockEvasionRule {
    override fun check(ctx: BlockCheckContext): String? {
        val attackerCard = ctx.state.getEntity(ctx.attackerId)?.get<CardComponent>() ?: return null
        val cardDef = ctx.cardRegistry.getCard(attackerCard.cardDefinitionId) ?: return null
        val restriction = cardDef.staticAbilities.filterIsInstance<CantBeBlockedIfCastSpellType>().firstOrNull()
            ?: return null

        val attackerController = ctx.projected.getController(ctx.attackerId) ?: return null
        val records = ctx.state.spellsCastThisTurnByPlayer[attackerController] ?: return null

        if (records.any { predicateEvaluator.matchesFilter(it, restriction.spellFilter) }) {
            return "${attackerCard.name} can't be blocked (controller cast a ${restriction.spellFilter.description} spell this turn)"
        }
        return null
    }
}

/**
 * Default set of block evasion rules, ordered for efficient short-circuiting.
 */
fun defaultBlockEvasionRules(
    predicateEvaluator: PredicateEvaluator = PredicateEvaluator()
): List<BlockEvasionRule> = listOf(
    UnblockableRule(),
    FlyingRule(),
    HorsemanshipRule(),
    ShadowRule(),
    FearRule(),
    LandwalkRule(),
    CantBeBlockedByRule(predicateEvaluator),
    CantBeBlockedExceptByColorRule(),
    CantBeBlockedByColorRule(),
    CantBeBlockedExceptByRule(predicateEvaluator),
    CantBeBlockedExceptBySubtypeRule(),
    CantBeBlockedUnlessDefenderSharesCreatureTypeRule(),
    GrantCantBeBlockedToSmallCreaturesRule(),
    CantBeBlockedIfCastSpellTypeRule(predicateEvaluator),
    ProtectionFromColorRule(),
    ProtectionFromSubtypeRule(),
    ProtectionFromEachOpponentRule(),
    CanOnlyBlockCreaturesWithKeywordRule(),
    CantBlockCreaturesWithGreaterPowerRule()
)
