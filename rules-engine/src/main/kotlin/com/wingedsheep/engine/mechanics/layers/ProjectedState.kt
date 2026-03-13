package com.wingedsheep.engine.mechanics.layers

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.engine.state.GameState

/**
 * Projected values for an entity after all effects are applied.
 */
data class ProjectedValues(
    val power: Int? = null,
    val toughness: Int? = null,
    val keywords: Set<String> = emptySet(),
    val colors: Set<String> = emptySet(),
    val types: Set<String> = emptySet(),
    val subtypes: Set<String> = emptySet(),
    val controllerId: EntityId? = null,
    val isFaceDown: Boolean = false,
    val cantAttack: Boolean = false,
    val cantBlock: Boolean = false,
    val mustAttack: Boolean = false,
    val mustBlock: Boolean = false,
    val cantBeBlockedExceptBySubtypes: Set<String> = emptySet(),
    val additionalBlockCount: Int = 0,
    val lostAllAbilities: Boolean = false
)

/**
 * The full projected game state.
 */
class ProjectedState(
    private val baseState: GameState,
    private val projectedValues: Map<EntityId, ProjectedValues>
) {
    fun getBaseState(): GameState = baseState

    fun getPower(entityId: EntityId): Int? = projectedValues[entityId]?.power

    fun getToughness(entityId: EntityId): Int? = projectedValues[entityId]?.toughness

    fun getKeywords(entityId: EntityId): Set<String> = projectedValues[entityId]?.keywords ?: emptySet()

    fun hasKeyword(entityId: EntityId, keyword: String): Boolean =
        getKeywords(entityId).contains(keyword)

    fun hasKeyword(entityId: EntityId, keyword: Keyword): Boolean =
        hasKeyword(entityId, keyword.name)

    fun hasKeyword(entityId: EntityId, flag: com.wingedsheep.sdk.core.AbilityFlag): Boolean =
        hasKeyword(entityId, flag.name)

    fun getColors(entityId: EntityId): Set<String> = projectedValues[entityId]?.colors ?: emptySet()

    fun hasColor(entityId: EntityId, color: Color): Boolean =
        getColors(entityId).contains(color.name)

    fun getTypes(entityId: EntityId): Set<String> = projectedValues[entityId]?.types ?: emptySet()

    fun hasType(entityId: EntityId, type: String): Boolean =
        getTypes(entityId).contains(type)

    fun isCreature(entityId: EntityId): Boolean = hasType(entityId, "CREATURE")

    fun isPlaneswalker(entityId: EntityId): Boolean = hasType(entityId, "PLANESWALKER")

    fun getSubtypes(entityId: EntityId): Set<String> = projectedValues[entityId]?.subtypes ?: emptySet()

    fun hasSubtype(entityId: EntityId, subtype: String): Boolean =
        getSubtypes(entityId).any { it.equals(subtype, ignoreCase = true) }

    fun isFaceDown(entityId: EntityId): Boolean = projectedValues[entityId]?.isFaceDown == true

    fun cantAttack(entityId: EntityId): Boolean = projectedValues[entityId]?.cantAttack == true

    fun cantBlock(entityId: EntityId): Boolean = projectedValues[entityId]?.cantBlock == true

    fun mustAttack(entityId: EntityId): Boolean = projectedValues[entityId]?.mustAttack == true

    fun mustBlock(entityId: EntityId): Boolean = projectedValues[entityId]?.mustBlock == true

    fun getCantBeBlockedExceptBySubtypes(entityId: EntityId): Set<String> =
        projectedValues[entityId]?.cantBeBlockedExceptBySubtypes ?: emptySet()

    fun getAdditionalBlockCount(entityId: EntityId): Int =
        projectedValues[entityId]?.additionalBlockCount ?: 0

    fun hasLostAllAbilities(entityId: EntityId): Boolean =
        projectedValues[entityId]?.lostAllAbilities == true

    fun getController(entityId: EntityId): EntityId? = projectedValues[entityId]?.controllerId

    fun getProjectedValues(entityId: EntityId): ProjectedValues? = projectedValues[entityId]

    fun getAllProjectedValues(): Map<EntityId, ProjectedValues> = projectedValues

    fun getBattlefieldControlledBy(playerId: EntityId): List<EntityId> {
        return baseState.getBattlefield().filter { entityId ->
            getController(entityId) == playerId
        }
    }
}

/**
 * Build an intermediate ProjectedState from in-progress mutable projected values.
 * Used during CDA resolution and dynamic P/T evaluation so that counting
 * uses updated types/subtypes from layers 1-6.
 */
internal fun buildIntermediateProjectedState(
    state: GameState,
    projectedValues: Map<EntityId, MutableProjectedValues>
): ProjectedState {
    val frozen = projectedValues.mapValues { (_, v) ->
        ProjectedValues(
            power = v.power,
            toughness = v.toughness,
            keywords = v.keywords.toSet(),
            colors = v.colors.toSet(),
            types = v.types.toSet(),
            subtypes = v.subtypes.toSet(),
            controllerId = v.controllerId,
            isFaceDown = v.isFaceDown,
            cantAttack = v.cantAttack,
            cantBlock = v.cantBlock,
            mustAttack = v.mustAttack,
            mustBlock = v.mustBlock,
            cantBeBlockedExceptBySubtypes = v.cantBeBlockedExceptBySubtypes.toSet(),
            additionalBlockCount = v.additionalBlockCount,
            lostAllAbilities = v.lostAllAbilities
        )
    }
    return ProjectedState(state, frozen)
}
