package com.wingedsheep.rulesengine.ecs.layers

import com.wingedsheep.rulesengine.core.CardType
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.Subtype
import com.wingedsheep.rulesengine.ecs.ComponentContainer
import com.wingedsheep.rulesengine.ecs.EntityId
import com.wingedsheep.rulesengine.ecs.GameState
import com.wingedsheep.rulesengine.ecs.ZoneId
import com.wingedsheep.rulesengine.ecs.components.AttachedToComponent
import com.wingedsheep.rulesengine.ecs.components.CardComponent
import com.wingedsheep.rulesengine.ecs.components.ControllerComponent
import com.wingedsheep.rulesengine.ecs.components.CountersComponent
import com.wingedsheep.rulesengine.ecs.components.DamageComponent
import com.wingedsheep.rulesengine.ecs.components.SummoningSicknessComponent
import com.wingedsheep.rulesengine.ecs.components.TappedComponent

/**
 * Projects the game state by applying all modifiers in layer order.
 *
 * The StateProjector implements MTG's continuous effects system (Rule 613).
 * It takes the base GameState and produces GameObjectViews that represent
 * the current "true" state of each entity after all continuous effects are applied.
 *
 * Key principles:
 * - The base state is never modified
 * - Modifiers are collected from all sources (static abilities, auras, etc.)
 * - Modifiers are applied in layer order, then by timestamp within each layer
 * - The result is a "view" of the game state for rules purposes
 */
class StateProjector(
    private val state: GameState,
    private val modifiers: List<Modifier> = emptyList()
) {
    // Cache for projected views
    private val viewCache = mutableMapOf<EntityId, GameObjectView>()

    // Modifiers grouped by target entity
    private val modifiersByTarget: Map<EntityId, List<Modifier>> by lazy {
        computeModifiersByTarget()
    }

    /**
     * Get the projected view for a specific entity.
     */
    fun getView(entityId: EntityId): GameObjectView? {
        // Check cache first
        viewCache[entityId]?.let { return it }

        // Get base entity
        val container = state.getEntity(entityId) ?: return null
        val cardComponent = container.get<CardComponent>() ?: return null

        // Get counters
        val countersComponent = container.get<CountersComponent>()
        val counters = countersComponent?.counters ?: emptyMap()

        // Get attachment info
        val attachedTo = container.get<AttachedToComponent>()?.targetId
        val attachments = computeAttachments(entityId)

        // Create base view
        val controllerComponent = container.get<ControllerComponent>()
        val baseView = GameObjectView.fromDefinition(
            entityId = entityId,
            definition = cardComponent.definition,
            ownerId = cardComponent.ownerId,
            controllerId = controllerComponent?.controllerId ?: cardComponent.ownerId,
            isTapped = container.has<TappedComponent>(),
            hasSummoningSickness = container.has<SummoningSicknessComponent>(),
            damage = container.get<DamageComponent>()?.amount ?: 0,
            counters = counters,
            attachedTo = attachedTo,
            attachments = attachments
        )

        // Apply modifiers in layer order
        val projected = applyModifiers(entityId, baseView)

        // Apply counters (Layer 7d) - this modifies P/T based on +1/+1 and -1/-1 counters
        val withCounters = applyCounters(entityId, projected, container)

        // Cache and return
        viewCache[entityId] = withCounters
        return withCounters
    }

    /**
     * Compute what entities are attached to this entity (reverse lookup).
     */
    private fun computeAttachments(entityId: EntityId): List<EntityId> {
        return state.getBattlefield().filter { id ->
            state.getComponent<AttachedToComponent>(id)?.targetId == entityId
        }
    }

    /**
     * Project all entities on the battlefield.
     */
    fun projectBattlefield(): List<GameObjectView> {
        return state.getBattlefield().mapNotNull { getView(it) }
    }

    /**
     * Project all creatures on the battlefield.
     */
    fun projectCreatures(): List<GameObjectView> {
        return projectBattlefield().filter { it.isCreature }
    }

    /**
     * Project creatures controlled by a specific player.
     */
    fun projectCreaturesControlledBy(playerId: EntityId): List<GameObjectView> {
        return projectCreatures().filter { it.controllerId == playerId }
    }

    /**
     * Compute which modifiers affect which entities.
     */
    private fun computeModifiersByTarget(): Map<EntityId, List<Modifier>> {
        val result = mutableMapOf<EntityId, MutableList<Modifier>>()

        for (modifier in modifiers) {
            val targetIds = resolveModifierTargets(modifier)
            for (targetId in targetIds) {
                result.getOrPut(targetId) { mutableListOf() }.add(modifier)
            }
        }

        return result
    }

    /**
     * Resolve which entities a modifier affects based on its filter.
     */
    private fun resolveModifierTargets(modifier: Modifier): List<EntityId> {
        val sourceId = modifier.sourceId
        val sourceContainer = state.getEntity(sourceId)

        return when (val filter = modifier.filter) {
            is ModifierFilter.Self -> listOf(sourceId)

            is ModifierFilter.AttachedTo -> {
                val attachedTo = sourceContainer?.get<AttachedToComponent>()?.targetId
                listOfNotNull(attachedTo)
            }

            is ModifierFilter.Specific -> listOf(filter.entityId)

            is ModifierFilter.ControlledBy -> {
                state.getBattlefield().filter { entityId ->
                    state.getComponent<ControllerComponent>(entityId)?.controllerId == filter.playerId
                }
            }

            is ModifierFilter.Opponents -> {
                val sourceController = sourceContainer?.get<ControllerComponent>()?.controllerId
                    ?: sourceContainer?.get<CardComponent>()?.ownerId
                    ?: return emptyList()

                state.getBattlefield().filter { entityId ->
                    val controller = state.getComponent<ControllerComponent>(entityId)?.controllerId
                    controller != null && controller != sourceController
                }
            }

            is ModifierFilter.All -> {
                state.getBattlefield().filter { entityId ->
                    matchesCriteria(entityId, filter.criteria)
                }
            }
        }
    }

    /**
     * Check if an entity matches the given criteria.
     * Checks if the entity has the CHANGELING keyword when evaluating subtype criteria.
     */
    private fun matchesCriteria(entityId: EntityId, criteria: EntityCriteria): Boolean {
        // We use the base definition to avoid recursion during projection.
        val container = state.getEntity(entityId) ?: return false
        val cardComponent = container.get<CardComponent>() ?: return false
        val definition = cardComponent.definition

        val hasKeywordInDefinition = { kw: Keyword ->
            definition.keywords.contains(kw)
        }

        return when (criteria) {
            is EntityCriteria.Creatures -> definition.isCreature
            is EntityCriteria.Lands -> definition.isLand
            is EntityCriteria.Artifacts -> definition.typeLine.isArtifact
            is EntityCriteria.Enchantments -> definition.typeLine.isEnchantment
            is EntityCriteria.Permanents -> definition.isPermanent
            is EntityCriteria.WithKeyword -> definition.keywords.contains(criteria.keyword)

            is EntityCriteria.WithSubtype -> {
                // Rule 702.73: Changeling (This object is every creature type.)
                // If it's a creature and has changeling, it matches ALL subtypes.
                val isChangeling = hasKeywordInDefinition(Keyword.CHANGELING)

                if (definition.isCreature && isChangeling) {
                    true
                } else {
                    definition.typeLine.subtypes.contains(criteria.subtype)
                }
            }

            is EntityCriteria.WithColor -> definition.manaCost.colors.contains(criteria.color)
            is EntityCriteria.And -> criteria.criteria.all { matchesCriteria(entityId, it) }
            is EntityCriteria.Or -> criteria.criteria.any { matchesCriteria(entityId, it) }
            is EntityCriteria.Not -> !matchesCriteria(entityId, criteria.criteria)
        }
    }

    /**
     * Apply all modifiers to an entity in layer order.
     */
    private fun applyModifiers(entityId: EntityId, baseView: GameObjectView): GameObjectView {
        val entityModifiers = modifiersByTarget[entityId] ?: return baseView

        // Sort modifiers by layer, then by timestamp
        val sorted = entityModifiers.sortedWith(
            compareBy({ it.layer.order }, { it.timestamp })
        )

        // Apply each modifier in order
        val builder = GameObjectViewBuilder.from(baseView)

        for (modifier in sorted) {
            applyModification(builder, modifier.modification)
        }

        return builder.build()
    }

    /**
     * Apply a single modification to the view builder.
     */
    private fun applyModification(builder: GameObjectViewBuilder, modification: Modification) {
        when (modification) {
            // Layer 2: Control
            is Modification.ChangeControl -> {
                builder.controllerId = modification.newControllerId
            }

            // Layer 4: Types
            is Modification.AddType -> {
                builder.types.add(modification.type)
            }
            is Modification.RemoveType -> {
                builder.types.remove(modification.type)
            }
            is Modification.AddSubtype -> {
                builder.subtypes.add(modification.subtype)
            }
            is Modification.RemoveSubtype -> {
                // Cannot remove a subtype if the object has Changeling and is a creature.
                // Rule 702.73a: Changeling grants all creature types.
                if (builder.keywords.contains(Keyword.CHANGELING) && builder.types.contains(CardType.CREATURE)) {
                    // Implicitly keeps all subtypes.
                } else {
                    builder.subtypes.remove(modification.subtype)
                }
            }
            is Modification.SetSubtypes -> {
                // If it has Changeling, setting subtypes doesn't wipe "all creature types".
                if (!builder.keywords.contains(Keyword.CHANGELING)) {
                    builder.subtypes.clear()
                }
                builder.subtypes.addAll(modification.subtypes)
            }

            // Layer 5: Colors
            is Modification.AddColor -> {
                builder.colors.add(modification.color)
            }
            is Modification.RemoveColor -> {
                builder.colors.remove(modification.color)
            }
            is Modification.SetColors -> {
                builder.colors.clear()
                builder.colors.addAll(modification.colors)
            }

            // Layer 6: Abilities
            is Modification.AddKeyword -> {
                builder.keywords.add(modification.keyword)
            }
            is Modification.RemoveKeyword -> {
                builder.keywords.remove(modification.keyword)
            }
            is Modification.RemoveAllAbilities -> {
                builder.hasAbilities = false
                builder.keywords.clear()
            }
            is Modification.AddCantBlockRestriction -> {
                builder.cantBlock = true
            }

            // Layer 7a: CDAs
            is Modification.SetPTFromCDA -> {
                val (power, toughness) = evaluateCDA(modification.cdaType, builder.entityId)
                builder.power = power
                builder.toughness = toughness
            }

            // Layer 7b: P/T setting
            is Modification.SetPT -> {
                builder.power = modification.power
                builder.toughness = modification.toughness
            }
            is Modification.SetPower -> {
                builder.power = modification.power
            }
            is Modification.SetToughness -> {
                builder.toughness = modification.toughness
            }

            // Layer 7c: P/T modification
            is Modification.ModifyPT -> {
                builder.power = builder.power?.plus(modification.powerDelta)
                builder.toughness = builder.toughness?.plus(modification.toughnessDelta)
            }
            is Modification.ModifyPower -> {
                builder.power = builder.power?.plus(modification.delta)
            }
            is Modification.ModifyToughness -> {
                builder.toughness = builder.toughness?.plus(modification.delta)
            }

            // Layer 7e: P/T switching
            is Modification.SwitchPT -> {
                val oldPower = builder.power
                builder.power = builder.toughness
                builder.toughness = oldPower
            }
        }
    }

    /**
     * Apply counters to power/toughness (Layer 7d).
     */
    private fun applyCounters(
        entityId: EntityId,
        view: GameObjectView,
        container: ComponentContainer
    ): GameObjectView {
        if (!view.isCreature) return view

        val counters = container.get<CountersComponent>() ?: return view
        val plusOneCounters = counters.plusOnePlusOneCount
        val minusOneCounters = counters.minusOneMinusOneCount

        if (plusOneCounters == 0 && minusOneCounters == 0) return view

        return view.copy(
            power = view.power?.plus(plusOneCounters)?.minus(minusOneCounters),
            toughness = view.toughness?.plus(plusOneCounters)?.minus(minusOneCounters)
        )
    }

    /**
     * Evaluate a characteristic-defining ability.
     */
    private fun evaluateCDA(cdaType: CDAType, entityId: EntityId): Pair<Int, Int> {
        val container = state.getEntity(entityId)
        val cardComponent = container?.get<CardComponent>()
        val controllerId = container?.get<ControllerComponent>()?.controllerId
            ?: cardComponent?.ownerId
            ?: return 0 to 0

        return when (cdaType) {
            CDAType.CARDS_IN_GRAVEYARD -> {
                val count = state.getGraveyard(controllerId).size
                count to count
            }
            CDAType.CREATURES_YOU_CONTROL -> {
                val count = state.getCreaturesControlledBy(controllerId).size
                count to count
            }
            CDAType.LANDS_YOU_CONTROL -> {
                val count = state.getPermanentsControlledBy(controllerId).count { id ->
                    state.getComponent<CardComponent>(id)?.definition?.isLand == true
                }
                count to count
            }
            CDAType.CARDS_IN_HAND -> {
                val count = state.getHand(controllerId).size
                count to count
            }
            CDAType.DEVOTION -> {
                // Would need to know which color - return 0 for now
                0 to 0
            }
            CDAType.CUSTOM -> {
                // Custom CDAs need script support
                0 to 0
            }
        }
    }

    companion object {
        /**
         * Create a projector with modifiers collected from the game state.
         * This collects modifiers from all static abilities on the battlefield.
         */
        fun forState(state: GameState, modifierProvider: ModifierProvider? = null): StateProjector {
            val modifiers = modifierProvider?.getModifiers(state) ?: emptyList()
            return StateProjector(state, modifiers)
        }
    }
}
