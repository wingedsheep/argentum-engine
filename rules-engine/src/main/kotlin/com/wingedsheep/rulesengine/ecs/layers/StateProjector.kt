package com.wingedsheep.rulesengine.ecs.layers

import com.wingedsheep.rulesengine.ability.DynamicAmount
import com.wingedsheep.rulesengine.core.CardType
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.Subtype
import com.wingedsheep.rulesengine.ecs.ComponentContainer
import com.wingedsheep.rulesengine.ecs.EntityId
import com.wingedsheep.rulesengine.ecs.GameState
import com.wingedsheep.rulesengine.ecs.ZoneId
import com.wingedsheep.rulesengine.ecs.components.AttachedToComponent
import com.wingedsheep.rulesengine.ecs.components.BaseColorsComponent
import com.wingedsheep.rulesengine.ecs.components.BaseKeywordsComponent
import com.wingedsheep.rulesengine.ecs.components.BaseStatsComponent
import com.wingedsheep.rulesengine.ecs.components.BaseTypesComponent
import com.wingedsheep.rulesengine.ecs.components.CardComponent
import com.wingedsheep.rulesengine.ecs.components.ControllerComponent
import com.wingedsheep.rulesengine.ecs.components.CountersComponent
import com.wingedsheep.rulesengine.ecs.components.DamageComponent
import com.wingedsheep.rulesengine.ecs.components.ProjectedAbilitiesComponent
import com.wingedsheep.rulesengine.ecs.components.ProjectedColorsComponent
import com.wingedsheep.rulesengine.ecs.components.ProjectedControlComponent
import com.wingedsheep.rulesengine.ecs.components.ProjectedPTComponent
import com.wingedsheep.rulesengine.ecs.components.ProjectedTypesComponent
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
    private val modifiers: List<Modifier> = emptyList(),
    private val useComponentProjection: Boolean = false
) {
    // Cache for projected views (POJO system)
    private val viewCache = mutableMapOf<EntityId, GameObjectView>()

    // Cache for projected containers (component-based system)
    private val containerCache = mutableMapOf<EntityId, ComponentContainer>()

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
            applyModification(builder, modifier)
        }

        return builder.build()
    }

    /**
     * Apply a single modification to the view builder.
     */
    private fun applyModification(builder: GameObjectViewBuilder, modifier: Modifier) {
        val modification = modifier.modification
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

            is Modification.AssignDamageEqualToToughness -> {
                // For conditional variant, only apply if toughness > power
                if (modification.onlyWhenToughnessGreaterThanPower) {
                    val power = builder.power ?: 0
                    val toughness = builder.toughness ?: 0
                    if (toughness > power) {
                        builder.assignsDamageEqualToToughness = true
                    }
                } else {
                    builder.assignsDamageEqualToToughness = true
                }
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
            is Modification.ModifyPTDynamic -> {
                val controllerId = state.getEntity(modifier.sourceId)?.get<ControllerComponent>()?.controllerId
                if (controllerId != null) {
                    val powerBonus = evaluateDynamicAmount(modification.powerSource, modifier.sourceId, controllerId)
                    val toughnessBonus = evaluateDynamicAmount(modification.toughnessSource, modifier.sourceId, controllerId)
                    builder.power = builder.power?.plus(powerBonus)
                    builder.toughness = builder.toughness?.plus(toughnessBonus)
                }
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

    // =========================================================================
    // Component-based projection system (new architecture)
    // =========================================================================

    /**
     * Project an entity using the component-based projection system.
     *
     * This method applies all modifiers using the Modification.apply() extension,
     * storing results in projected components rather than a POJO builder.
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

        // Get and sort modifiers by layer, then timestamp
        val sorted = (modifiersByTarget[entityId] ?: emptyList())
            .sortedWith(compareBy({ it.layer.order }, { it.timestamp }))

        // Build context for CDA/dynamic evaluation
        val context = ProjectionContext(
            state = state,
            entityId = entityId,
            sourceId = entityId,
            controllerId = controllerId
        )

        // Apply each modifier using the component-based system
        for (modifier in sorted) {
            container = modifier.modification.apply(
                container,
                context.copy(sourceId = modifier.sourceId)
            )
        }

        // Apply counters (Layer 7d)
        container = applyCountersToProjected(container, baseContainer)

        // Cache and return
        containerCache[entityId] = container
        return container
    }

    /**
     * Initialize projected components from base components.
     *
     * Copies base components to projected components as a starting point.
     * If base components don't exist, falls back to CardDefinition.
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

    /**
     * Evaluate a dynamic amount for effects like "+X/+X where X is..."
     */
    private fun evaluateDynamicAmount(amount: DynamicAmount, sourceId: EntityId, controllerId: EntityId): Int {
        return when (amount) {
            is DynamicAmount.Fixed -> amount.amount
            is DynamicAmount.OtherCreaturesYouControl -> {
                state.getCreaturesControlledBy(controllerId).count { it != sourceId }
            }
            is DynamicAmount.CreaturesYouControl -> {
                state.getCreaturesControlledBy(controllerId).size
            }
            is DynamicAmount.AllCreatures -> {
                state.getBattlefield().count { entityId ->
                    state.getComponent<CardComponent>(entityId)?.definition?.isCreature == true
                }
            }
            is DynamicAmount.YourLifeTotal -> {
                state.getComponent<com.wingedsheep.rulesengine.ecs.components.LifeComponent>(controllerId)?.life ?: 0
            }
            is DynamicAmount.CreaturesEnteredThisTurn -> {
                // TODO: Track creatures that entered this turn
                // This requires turn tracking infrastructure to be added
                // For now returns 0 - the card Kinbinding will need this implemented
                0
            }
            is DynamicAmount.AttackingCreaturesYouControl -> {
                // Count creatures with AttackingComponent that we control
                state.getBattlefield().count { entityId ->
                    state.getComponent<com.wingedsheep.rulesengine.ecs.components.AttackingComponent>(entityId) != null &&
                        state.getComponent<ControllerComponent>(entityId)?.controllerId == controllerId
                }
            }
            is DynamicAmount.ColorsAmongPermanentsYouControl -> {
                state.getPermanentsControlledBy(controllerId)
                    .mapNotNull { state.getComponent<CardComponent>(it)?.definition?.colors }
                    .flatten()
                    .toSet()
                    .size
            }
            is DynamicAmount.OtherCreaturesWithSubtypeYouControl -> {
                // Count other creatures you control with the specific subtype
                state.getCreaturesControlledBy(controllerId).count { entityId ->
                    entityId != sourceId &&
                        state.getComponent<CardComponent>(entityId)?.definition?.typeLine?.subtypes?.contains(amount.subtype) == true
                }
            }
        }
    }

    companion object {
        /**
         * Create a projector with modifiers collected from the game state.
         * This collects modifiers from all static abilities on the battlefield.
         *
         * @param state The game state to project
         * @param modifierProvider Optional provider for collecting modifiers
         * @param useComponentProjection Enable the new component-based projection system
         */
        fun forState(
            state: GameState,
            modifierProvider: ModifierProvider? = null,
            useComponentProjection: Boolean = false
        ): StateProjector {
            val modifiers = modifierProvider?.getModifiers(state) ?: emptyList()
            return StateProjector(state, modifiers, useComponentProjection)
        }
    }
}
