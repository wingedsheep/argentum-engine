package com.wingedsheep.engine.mechanics.combat.rules

import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.scripting.CantBeBlockedByPower
import com.wingedsheep.sdk.scripting.CantBeBlockedByPowerOrLess
import com.wingedsheep.sdk.scripting.CantBeBlockedBySubtype
import com.wingedsheep.sdk.scripting.CantBeBlockedExceptByKeyword
import com.wingedsheep.sdk.scripting.CantBeBlockedUnlessDefenderSharesCreatureType

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
 * CantBeBlockedBySubtype: Cannot be blocked by creatures with a specific subtype (e.g., Walls).
 */
class CantBeBlockedBySubtypeRule : BlockEvasionRule {
    override fun check(ctx: BlockCheckContext): String? {
        val attackerCard = ctx.state.getEntity(ctx.attackerId)?.get<CardComponent>() ?: return null
        val cardDef = ctx.cardRegistry?.getCard(attackerCard.cardDefinitionId) ?: return null
        val restriction = cardDef.staticAbilities.filterIsInstance<CantBeBlockedBySubtype>().firstOrNull()
            ?: return null

        if (ctx.projected.hasSubtype(ctx.blockerId, restriction.subtype)) {
            val blockerName = ctx.state.getEntity(ctx.blockerId)?.get<CardComponent>()?.name ?: "Creature"
            return "$blockerName cannot block ${attackerCard.name} (${restriction.subtype}s can't block it)"
        }
        return null
    }
}

/**
 * CantBeBlockedByPower: Cannot be blocked by creatures with power >= N.
 */
class CantBeBlockedByPowerRule : BlockEvasionRule {
    override fun check(ctx: BlockCheckContext): String? {
        val attackerCard = ctx.state.getEntity(ctx.attackerId)?.get<CardComponent>() ?: return null
        val cardDef = ctx.cardRegistry?.getCard(attackerCard.cardDefinitionId) ?: return null
        val powerRestriction = cardDef.staticAbilities.filterIsInstance<CantBeBlockedByPower>().firstOrNull()
            ?: return null

        val blockerPower = ctx.projected.getPower(ctx.blockerId) ?: 0
        if (blockerPower >= powerRestriction.minPower) {
            val blockerName = ctx.state.getEntity(ctx.blockerId)?.get<CardComponent>()?.name ?: "Creature"
            return "$blockerName cannot block ${attackerCard.name} (power $blockerPower or greater)"
        }
        return null
    }
}

/**
 * CantBeBlockedByPowerOrLess: Cannot be blocked by creatures with power <= N.
 */
class CantBeBlockedByPowerOrLessRule : BlockEvasionRule {
    override fun check(ctx: BlockCheckContext): String? {
        val attackerCard = ctx.state.getEntity(ctx.attackerId)?.get<CardComponent>() ?: return null
        val cardDef = ctx.cardRegistry?.getCard(attackerCard.cardDefinitionId) ?: return null
        val powerRestriction = cardDef.staticAbilities.filterIsInstance<CantBeBlockedByPowerOrLess>().firstOrNull()
            ?: return null

        val blockerPower = ctx.projected.getPower(ctx.blockerId) ?: 0
        if (blockerPower <= powerRestriction.maxPower) {
            val blockerName = ctx.state.getEntity(ctx.blockerId)?.get<CardComponent>()?.name ?: "Creature"
            return "$blockerName cannot block ${attackerCard.name} (power $blockerPower or less)"
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
 * CantBeBlockedExceptByKeyword: Can only be blocked by creatures with a specific keyword (card def).
 */
class CantBeBlockedExceptByKeywordRule : BlockEvasionRule {
    override fun check(ctx: BlockCheckContext): String? {
        val attackerCard = ctx.state.getEntity(ctx.attackerId)?.get<CardComponent>() ?: return null
        val cardDef = ctx.cardRegistry?.getCard(attackerCard.cardDefinitionId) ?: return null
        val keywordRestriction = cardDef.staticAbilities.filterIsInstance<CantBeBlockedExceptByKeyword>().firstOrNull()
            ?: return null

        val requiredKeyword = keywordRestriction.requiredKeyword

        if (ctx.projected.hasKeyword(ctx.blockerId, requiredKeyword)) return null

        // Per MTG rules, reach allows blocking creatures with "can't be blocked except by flying"
        if (requiredKeyword == Keyword.FLYING && ctx.projected.hasKeyword(ctx.blockerId, Keyword.REACH)) return null

        val blockerName = ctx.state.getEntity(ctx.blockerId)?.get<CardComponent>()?.name ?: "Creature"
        return "$blockerName cannot block ${attackerCard.name} (can only be blocked by creatures with ${requiredKeyword.displayName.lowercase()})"
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
        val cardDef = ctx.cardRegistry?.getCard(attackerCard.cardDefinitionId) ?: return null
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
        val cardDef = ctx.cardRegistry?.getCard(blockerCard.cardDefinitionId) ?: return null
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
        val cardDef = ctx.cardRegistry?.getCard(blockerCard.cardDefinitionId) ?: return null
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
 * Default set of block evasion rules, ordered for efficient short-circuiting.
 */
fun defaultBlockEvasionRules(): List<BlockEvasionRule> = listOf(
    UnblockableRule(),
    FlyingRule(),
    HorsemanshipRule(),
    ShadowRule(),
    FearRule(),
    LandwalkRule(),
    CantBeBlockedBySubtypeRule(),
    CantBeBlockedByPowerRule(),
    CantBeBlockedByPowerOrLessRule(),
    CantBeBlockedExceptByColorRule(),
    CantBeBlockedExceptByKeywordRule(),
    CantBeBlockedExceptBySubtypeRule(),
    CantBeBlockedUnlessDefenderSharesCreatureTypeRule(),
    ProtectionFromColorRule(),
    ProtectionFromSubtypeRule(),
    CanOnlyBlockCreaturesWithKeywordRule(),
    CantBlockCreaturesWithGreaterPowerRule()
)
