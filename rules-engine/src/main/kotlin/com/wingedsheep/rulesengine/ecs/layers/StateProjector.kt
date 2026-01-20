package com.wingedsheep.rulesengine.ecs.layers

import com.wingedsheep.rulesengine.core.CardType
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.ecs.ComponentContainer
import com.wingedsheep.rulesengine.ecs.EntityId
import com.wingedsheep.rulesengine.ecs.GameState
import com.wingedsheep.rulesengine.ecs.components.AttachedToComponent
import com.wingedsheep.rulesengine.ecs.components.BaseColorsComponent
import com.wingedsheep.rulesengine.ecs.components.BaseKeywordsComponent
import com.wingedsheep.rulesengine.ecs.components.BaseStatsComponent
import com.wingedsheep.rulesengine.ecs.components.BaseTypesComponent
import com.wingedsheep.rulesengine.ecs.components.CardComponent
import com.wingedsheep.rulesengine.ecs.components.ControllerComponent
import com.wingedsheep.rulesengine.ecs.components.CountersComponent
import com.wingedsheep.rulesengine.ecs.components.ProjectedAbilitiesComponent
import com.wingedsheep.rulesengine.ecs.components.ProjectedColorsComponent
import com.wingedsheep.rulesengine.ecs.components.ProjectedPTComponent
import com.wingedsheep.rulesengine.ecs.components.ProjectedTypesComponent

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
 * - Dependencies between modifiers are resolved via fixed-point iteration (Rule 613.8)
 * - The result is a "view" of the game state for rules purposes
 *
 * ## Rule 613.8 - Dependencies
 * When Effect A applies to objects based on characteristics that Effect B modifies,
 * and both apply in the same layer, B must be applied first. This is resolved by:
 * 1. Analyzing filter dependencies for each modifier
 * 2. Building a dependency graph
 * 3. Applying topological sort with timestamp as tiebreaker
 * 4. Using fixed-point iteration to handle complex dependency chains
 */
class StateProjector(
    private val state: GameState,
    private val modifiers: List<Modifier> = emptyList()
) {
    // Cache for projected containers
    private val containerCache = mutableMapOf<EntityId, ComponentContainer>()

    // Cache for GameObjectView (converted from projected containers)
    private val viewCache = mutableMapOf<EntityId, GameObjectView>()

    // Modifiers grouped by target entity
    private val modifiersByTarget: Map<EntityId, List<Modifier>> by lazy {
        computeModifiersByTarget()
    }

    /**
     * Get the projected view for a specific entity.
     *
     * Uses the component-based projection system to create a GameObjectView
     * that reads values from projected components.
     */
    fun getView(entityId: EntityId): GameObjectView? {
        // Check cache first
        viewCache[entityId]?.let { return it }

        // Project using component system
        val projectedContainer = projectEntity(entityId) ?: return null

        // Compute attachments for this entity
        val attachments = computeAttachments(entityId)

        // Create GameObjectView directly from projected container
        val view = GameObjectView(projectedContainer, entityId, attachments)

        // Cache and return
        viewCache[entityId] = view
        return view
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
     * Project an entity using the component-based projection system.
     *
     * This method applies all modifiers using the Modification.apply() extension,
     * storing results in projected components rather than a POJO builder.
     *
     * Per Rule 613.8, dependencies between modifiers are resolved using fixed-point
     * iteration when modifiers in the same layer depend on each other's results.
     *
     * @param entityId The entity to project
     * @return The projected ComponentContainer, or null if the entity doesn't exist
     */
    fun projectEntity(entityId: EntityId): ComponentContainer? {
        // Check cache first
        containerCache[entityId]?.let { return it }

        // Get base entity
        val baseContainer = state.getEntity(entityId) ?: return null
        val cardComponent = baseContainer.get<CardComponent>() ?: return null
        val controllerComponent = baseContainer.get<ControllerComponent>()
        val controllerId = controllerComponent?.controllerId ?: cardComponent.ownerId

        // Initialize projected components from base components
        var container = initializeProjectedComponents(baseContainer)

        // Get modifiers for this entity
        val entityModifiers = modifiersByTarget[entityId] ?: emptyList()

        // Build context for CDA/dynamic evaluation
        val context = ProjectionContext(
            state = state,
            entityId = entityId,
            sourceId = entityId,
            controllerId = controllerId
        )

        // Group modifiers by layer for dependency resolution
        val modifiersByLayer = entityModifiers.groupBy { it.layer }

        // Apply modifiers layer by layer
        for (layer in Layer.entries.sortedBy { it.order }) {
            val layerModifiers = modifiersByLayer[layer] ?: continue

            // Resolve dependencies within this layer
            val sortedLayerModifiers = if (layerModifiers.size > 1 && hasPotentialDependencies(layerModifiers)) {
                val resolver = DependencyResolver(state)
                resolver.sortWithDependencies(layerModifiers)
            } else {
                // No dependencies possible, just sort by timestamp
                layerModifiers.sortedBy { it.timestamp }
            }

            // Apply each modifier in the resolved order
            for (modifier in sortedLayerModifiers) {
                container = modifier.modification.apply(
                    container,
                    context.copy(sourceId = modifier.sourceId)
                )
            }
        }

        // Apply counters (Layer 7d)
        container = applyCountersToProjected(container, baseContainer)

        // Cache and return
        containerCache[entityId] = container
        return container
    }

    /**
     * Quick check if a list of modifiers might have dependencies.
     * Used to skip expensive dependency analysis when not needed.
     */
    private fun hasPotentialDependencies(modifiers: List<Modifier>): Boolean {
        // Need at least one modifier with a characteristic-based filter
        val hasCharacteristicFilter = modifiers.any { it.filter is ModifierFilter.All }
        if (!hasCharacteristicFilter) return false

        // Need at least one modifier that changes characteristics
        return modifiers.any { modifier ->
            when (modifier.modification) {
                is Modification.AddType,
                is Modification.RemoveType,
                is Modification.AddSubtype,
                is Modification.RemoveSubtype,
                is Modification.SetSubtypes,
                is Modification.AddColor,
                is Modification.RemoveColor,
                is Modification.SetColors,
                is Modification.AddKeyword,
                is Modification.RemoveKeyword,
                is Modification.RemoveAllAbilities -> true
                else -> false
            }
        }
    }

    /**
     * Initialize projected components from base components.
     *
     * Copies base components to projected components as a starting point.
     * Falls back to CardDefinition for entities without base components
     * (e.g., spells on the stack which haven't entered the battlefield).
     */
    private fun initializeProjectedComponents(baseContainer: ComponentContainer): ComponentContainer {
        var container = baseContainer

        // Initialize ProjectedTypesComponent from BaseTypesComponent or CardDefinition
        val baseTypes = baseContainer.get<BaseTypesComponent>()
        if (baseTypes != null) {
            container = container.with(baseTypes.toProjected())
        } else {
            // Fallback to CardDefinition
            baseContainer.get<CardComponent>()?.definition?.let { definition ->
                container = container.with(ProjectedTypesComponent(
                    types = definition.typeLine.cardTypes,
                    subtypes = definition.typeLine.subtypes
                ))
            }
        }

        // Initialize ProjectedColorsComponent from BaseColorsComponent or CardDefinition
        val baseColors = baseContainer.get<BaseColorsComponent>()
        if (baseColors != null) {
            container = container.with(baseColors.toProjected())
        } else {
            baseContainer.get<CardComponent>()?.definition?.let { definition ->
                container = container.with(ProjectedColorsComponent(definition.colors))
            }
        }

        // Initialize ProjectedAbilitiesComponent from BaseKeywordsComponent or CardDefinition
        val baseKeywords = baseContainer.get<BaseKeywordsComponent>()
        if (baseKeywords != null) {
            container = container.with(baseKeywords.toProjected())
        } else {
            baseContainer.get<CardComponent>()?.definition?.let { definition ->
                container = container.with(ProjectedAbilitiesComponent(
                    keywords = definition.keywords,
                    hasAbilities = true,
                    cantBlock = false,
                    assignsDamageEqualToToughness = false
                ))
            }
        }

        // Initialize ProjectedPTComponent from BaseStatsComponent or CardDefinition
        val baseStats = baseContainer.get<BaseStatsComponent>()
        if (baseStats != null) {
            container = container.with(baseStats.toProjected())
        } else {
            baseContainer.get<CardComponent>()?.definition?.creatureStats?.let { stats ->
                container = container.with(ProjectedPTComponent(stats.basePower, stats.baseToughness))
            }
        }

        return container
    }

    /**
     * Apply counters to projected P/T (Layer 7d).
     *
     * Modifies the ProjectedPTComponent based on +1/+1 and -1/-1 counters.
     */
    private fun applyCountersToProjected(
        container: ComponentContainer,
        baseContainer: ComponentContainer
    ): ComponentContainer {
        // Check if it's a creature
        val types = container.get<ProjectedTypesComponent>()
        if (types == null || CardType.CREATURE !in types.types) return container

        val counters = baseContainer.get<CountersComponent>() ?: return container
        val plusOneCounters = counters.plusOnePlusOneCount
        val minusOneCounters = counters.minusOneMinusOneCount

        if (plusOneCounters == 0 && minusOneCounters == 0) return container

        val currentPT = container.get<ProjectedPTComponent>() ?: return container
        return container.with(ProjectedPTComponent(
            power = currentPT.power?.plus(plusOneCounters)?.minus(minusOneCounters),
            toughness = currentPT.toughness?.plus(plusOneCounters)?.minus(minusOneCounters)
        ))
    }

    companion object {
        /**
         * Create a projector with modifiers collected from the game state.
         * This collects modifiers from all static abilities on the battlefield.
         *
         * @param state The game state to project
         * @param modifierProvider Optional provider for collecting modifiers
         */
        fun forState(
            state: GameState,
            modifierProvider: ModifierProvider? = null
        ): StateProjector {
            val modifiers = modifierProvider?.getModifiers(state) ?: emptyList()
            return StateProjector(state, modifiers)
        }
    }
}
