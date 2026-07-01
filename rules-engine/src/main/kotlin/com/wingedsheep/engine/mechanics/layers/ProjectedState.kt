package com.wingedsheep.engine.mechanics.layers

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent

/**
 * An active "this object is also the chosen creature type" grant that reaches **beyond** the
 * battlefield (the Conspiracy / Leyline of Transformation clause "The same is true for creature
 * spells you control and creature cards you own that aren't on the battlefield").
 *
 * Built by [StateProjector] from each battlefield `GrantChosenSubtype` whose cross-zone flags are
 * set, then consulted by every subtype read-site for objects that have no battlefield projection
 * entry (spells on the stack, cards in hand/library/graveyard/exile/command) via
 * [ProjectedState.crossZoneGrantedSubtypes]. Battlefield permanents are unaffected — they get the
 * type through normal Layer 4 projection.
 *
 * @property controllerId The granting permanent's controller — the player who "controls" the
 *   affected spells and "owns" the affected cards (the asymmetry is in the card text).
 * @property chosenType The creature type chosen as the granting permanent entered.
 * @property includeControlledSpells Grant reaches creature spells [controllerId] controls (stack).
 * @property includeOwnedCardsOutsideBattlefield Grant reaches creature cards [controllerId] owns in
 *   any non-battlefield, non-stack zone.
 */
data class CrossZoneSubtypeGrant(
    val controllerId: EntityId,
    val chosenType: String,
    val includeControlledSpells: Boolean,
    val includeOwnedCardsOutsideBattlefield: Boolean
)

/**
 * Projected values for an entity after all effects are applied.
 */
data class ProjectedValues(
    val power: Int? = null,
    val toughness: Int? = null,
    val name: String? = null,
    val keywords: Set<String> = emptySet(),
    val colors: Set<String> = emptySet(),
    val types: Set<String> = emptySet(),
    val subtypes: Set<String> = emptySet(),
    val controllerId: EntityId? = null,
    val isFaceDown: Boolean = false,
    val isSuspected: Boolean = false,
    val cantAttack: Boolean = false,
    val cantBlock: Boolean = false,
    val cantBeTurnedFaceUp: Boolean = false,
    val mustAttack: Boolean = false,
    val mustBlock: Boolean = false,
    val cantBeBlockedExceptByFilters: List<com.wingedsheep.sdk.scripting.GameObjectFilter> = emptyList(),
    val canOnlyBlockCreaturesWithFilters: List<com.wingedsheep.sdk.scripting.GameObjectFilter> = emptyList(),
    val additionalBlockCount: Int = 0,
    val lostAllAbilities: Boolean = false,
    /**
     * True when a continuous effect SET this permanent's basic land types (Blood Moon,
     * Zhao's "nonbasic lands are Mountains", Spreading Seas, …). Such effects grant the
     * new type's intrinsic mana ability (CR 305.7), which survives even a lose-all-abilities
     * effect from the same source — unlike the intrinsic mana of a land's printed subtype,
     * which Imprisoned in the Moon does remove.
     */
    val basicLandTypesSetByEffect: Boolean = false
)

/**
 * The full projected game state.
 */
class ProjectedState(
    private val baseState: GameState,
    private val projectedValues: Map<EntityId, ProjectedValues>,
    /**
     * Active cross-zone "is the chosen type" grants (Conspiracy / Leyline of Transformation).
     * Empty for virtually every game state, so [crossZoneGrantedSubtypes] short-circuits to no-op.
     */
    val crossZoneSubtypeGrants: List<CrossZoneSubtypeGrant> = emptyList()
) {
    fun getBaseState(): GameState = baseState

    /**
     * The chosen creature types granted to a **non-battlefield** object (a creature spell on the
     * stack, or a creature card in hand/library/graveyard/exile/command) by cross-zone
     * [CrossZoneSubtypeGrant]s. Battlefield permanents return empty here — they get the type via
     * normal Layer 4 projection ([getSubtypes]). Returns empty (fast path) when no such grant is
     * active, which is the common case.
     *
     * Read by the subtype predicates in `PredicateEvaluator` and by `SelectFromCollectionExecutor`
     * for objects with no projection entry, so a Leyline-granted type drives type-matters checks
     * (targeting "a Zombie spell" / "a Zombie card in your graveyard", search filters, …).
     */
    fun crossZoneGrantedSubtypes(state: GameState, entityId: EntityId): Set<String> {
        if (crossZoneSubtypeGrants.isEmpty()) return emptySet()
        val container = state.getEntity(entityId) ?: return emptySet()
        val card = container.get<CardComponent>() ?: return emptySet()
        // Only creature spells / creature cards gain the type ("creature spells ... creature cards ...").
        if (!card.typeLine.isCreature) return emptySet()

        return if (state.isSpellOnStack(entityId)) {
            // A spell's controller is its ControllerComponent if present, else its caster.
            val controller = container.get<ControllerComponent>()?.playerId
                ?: container.get<SpellOnStackComponent>()?.casterId
                ?: return emptySet()
            crossZoneSubtypeGrants
                .filter { it.includeControlledSpells && it.controllerId == controller }
                .mapTo(mutableSetOf()) { it.chosenType }
        } else {
            // Battlefield permanents are covered by Layer 4 projection, not this overlay.
            if (entityId in state.getBattlefield()) return emptySet()
            val owner = card.ownerId ?: return emptySet()
            crossZoneSubtypeGrants
                .filter { it.includeOwnedCardsOutsideBattlefield && it.controllerId == owner }
                .mapTo(mutableSetOf()) { it.chosenType }
        }
    }

    fun getPower(entityId: EntityId): Int? = projectedValues[entityId]?.power

    fun getToughness(entityId: EntityId): Int? = projectedValues[entityId]?.toughness

    /**
     * The object's projected name, if a continuous effect has overwritten it (CR 612.8 —
     * Layer 3 text-changing, e.g. Witness Protection's [Modification.SetName]). Null when no
     * such effect is active; callers should fall back to [com.wingedsheep.engine.state.components.identity.CardComponent.name].
     */
    fun getName(entityId: EntityId): String? = projectedValues[entityId]?.name

    fun getKeywords(entityId: EntityId): Set<String> = projectedValues[entityId]?.keywords ?: emptySet()

    fun hasKeyword(entityId: EntityId, keyword: String): Boolean =
        getKeywords(entityId).contains(keyword)

    fun hasKeyword(entityId: EntityId, keyword: Keyword): Boolean =
        hasKeyword(entityId, keyword.name)

    fun hasKeyword(entityId: EntityId, flag: com.wingedsheep.sdk.core.AbilityFlag): Boolean =
        hasKeyword(entityId, flag.name)

    /**
     * Whether counters can be put on this object, per "[this] can't have counters put on it"
     * effects (e.g. Blossombind). Centralizes the prohibition so every counter-placing path —
     * effects (AddCounters, DoubleCounters, ...) and costs (Blight) alike — agrees. CR 614.17b:
     * because the event can't happen, a player can't even choose to pay a cost that includes it,
     * so blight target pools must exclude such creatures rather than silently dropping the counter.
     */
    fun canReceiveCounters(entityId: EntityId): Boolean =
        !hasKeyword(entityId, com.wingedsheep.sdk.core.AbilityFlag.CANT_RECEIVE_COUNTERS)

    fun getColors(entityId: EntityId): Set<String> = projectedValues[entityId]?.colors ?: emptySet()

    fun hasColor(entityId: EntityId, color: Color): Boolean =
        getColors(entityId).contains(color.name)

    fun getTypes(entityId: EntityId): Set<String> = projectedValues[entityId]?.types ?: emptySet()

    fun hasType(entityId: EntityId, type: String): Boolean =
        getTypes(entityId).contains(type)

    fun isCreature(entityId: EntityId): Boolean = hasType(entityId, "CREATURE")

    fun isPlaneswalker(entityId: EntityId): Boolean = hasType(entityId, "PLANESWALKER")

    fun isLegendary(entityId: EntityId): Boolean = hasType(entityId, "LEGENDARY")

    fun getSubtypes(entityId: EntityId): Set<String> = projectedValues[entityId]?.subtypes ?: emptySet()

    /**
     * Supertypes projected onto this entity (e.g. LEGENDARY). Supertypes are stored in the same
     * projected [types] set as card types, so they are isolated by name here for protection checks.
     */
    fun getSupertypes(entityId: EntityId): Set<String> =
        getTypes(entityId).intersect(setOf("BASIC", "LEGENDARY", "SNOW", "WORLD"))

    fun hasSubtype(entityId: EntityId, subtype: String): Boolean =
        getSubtypes(entityId).any { it.equals(subtype, ignoreCase = true) }

    fun isFaceDown(entityId: EntityId): Boolean = projectedValues[entityId]?.isFaceDown == true

    fun isSuspected(entityId: EntityId): Boolean = projectedValues[entityId]?.isSuspected == true

    fun cantAttack(entityId: EntityId): Boolean = projectedValues[entityId]?.cantAttack == true

    fun cantBlock(entityId: EntityId): Boolean = projectedValues[entityId]?.cantBlock == true

    fun cantBeTurnedFaceUp(entityId: EntityId): Boolean =
        projectedValues[entityId]?.cantBeTurnedFaceUp == true

    fun mustAttack(entityId: EntityId): Boolean = projectedValues[entityId]?.mustAttack == true

    fun mustBlock(entityId: EntityId): Boolean = projectedValues[entityId]?.mustBlock == true

    fun getCantBeBlockedExceptByFilters(entityId: EntityId): List<com.wingedsheep.sdk.scripting.GameObjectFilter> =
        projectedValues[entityId]?.cantBeBlockedExceptByFilters ?: emptyList()

    fun getCanOnlyBlockCreaturesWithFilters(entityId: EntityId): List<com.wingedsheep.sdk.scripting.GameObjectFilter> =
        projectedValues[entityId]?.canOnlyBlockCreaturesWithFilters ?: emptyList()

    fun getAdditionalBlockCount(entityId: EntityId): Int =
        projectedValues[entityId]?.additionalBlockCount ?: 0

    fun hasLostAllAbilities(entityId: EntityId): Boolean =
        projectedValues[entityId]?.lostAllAbilities == true

    /** See [ProjectedValues.basicLandTypesSetByEffect]. */
    fun hasBasicLandTypesSetByEffect(entityId: EntityId): Boolean =
        projectedValues[entityId]?.basicLandTypesSetByEffect == true

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
            isSuspected = v.isSuspected,
            cantAttack = v.cantAttack,
            cantBlock = v.cantBlock,
            cantBeTurnedFaceUp = v.cantBeTurnedFaceUp,
            mustAttack = v.mustAttack,
            mustBlock = v.mustBlock,
            cantBeBlockedExceptByFilters = v.cantBeBlockedExceptByFilters.toList(),
            canOnlyBlockCreaturesWithFilters = v.canOnlyBlockCreaturesWithFilters.toList(),
            additionalBlockCount = v.additionalBlockCount,
            lostAllAbilities = v.lostAllAbilities,
            name = v.name
        )
    }
    return ProjectedState(state, frozen)
}
