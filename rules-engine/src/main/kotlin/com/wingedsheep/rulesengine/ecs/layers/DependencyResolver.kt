package com.wingedsheep.rulesengine.ecs.layers

import com.wingedsheep.rulesengine.core.CardType
import com.wingedsheep.rulesengine.core.Color
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.Subtype
import com.wingedsheep.rulesengine.ecs.EntityId
import com.wingedsheep.rulesengine.ecs.GameState

/**
 * Resolves dependencies between modifiers per Rule 613.8.
 *
 * A dependency exists when:
 * - Both effects apply in the same layer
 * - Effect A applies to objects based on characteristics (like "green creatures")
 * - Effect B modifies those same characteristics (like "makes creatures green")
 *
 * When B would affect whether A applies to an object, B must be applied first,
 * regardless of timestamp ordering.
 *
 * This resolver detects such dependencies and provides a correct ordering.
 *
 * ## Rule 613.8
 * "One continuous effect can depend on another. A dependency exists if:
 * (a) both effects apply in the same layer (or sublayer);
 * (b) both effects would affect the same characteristic(s); and
 * (c) the result of one effect would change what objects the other effect applies to,
 *     change the characteristics those objects would have, or change whether the other
 *     effect's source exists."
 */
class DependencyResolver(
    private val state: GameState
) {
    /**
     * Characteristic types that can create dependencies.
     */
    enum class Characteristic {
        TYPE,
        SUBTYPE,
        COLOR,
        KEYWORD,
        POWER,
        TOUGHNESS
    }

    /**
     * Describes what characteristics a modifier's filter depends on.
     */
    data class FilterDependencies(
        val dependsOnTypes: Set<CardType> = emptySet(),
        val dependsOnSubtypes: Set<Subtype> = emptySet(),
        val dependsOnColors: Set<Color> = emptySet(),
        val dependsOnKeywords: Set<Keyword> = emptySet()
    ) {
        val isEmpty: Boolean
            get() = dependsOnTypes.isEmpty() &&
                    dependsOnSubtypes.isEmpty() &&
                    dependsOnColors.isEmpty() &&
                    dependsOnKeywords.isEmpty()
    }

    /**
     * Describes what characteristics a modifier modifies.
     */
    data class ModificationEffects(
        val modifiesTypes: Set<CardType> = emptySet(),
        val modifiesSubtypes: Set<Subtype> = emptySet(),
        val modifiesColors: Set<Color> = emptySet(),
        val modifiesKeywords: Set<Keyword> = emptySet(),
        val modifiesPower: Boolean = false,
        val modifiesToughness: Boolean = false
    )

    /**
     * Sort modifiers within a layer respecting dependencies.
     *
     * Uses topological sort with fixed-point iteration to handle
     * complex dependency chains.
     *
     * @param modifiers The modifiers to sort (should all be in the same layer)
     * @return Modifiers sorted respecting dependencies, then by timestamp
     */
    fun sortWithDependencies(modifiers: List<Modifier>): List<Modifier> {
        if (modifiers.size <= 1) return modifiers

        // Build dependency graph
        val dependencies = buildDependencyGraph(modifiers)

        // Topological sort with fallback to timestamp
        return topologicalSort(modifiers, dependencies)
    }

    /**
     * Build a dependency graph where edges represent "A depends on B".
     *
     * @return Map from modifier index to set of indices it depends on
     */
    private fun buildDependencyGraph(modifiers: List<Modifier>): Map<Int, Set<Int>> {
        val graph = mutableMapOf<Int, MutableSet<Int>>()

        for (i in modifiers.indices) {
            graph[i] = mutableSetOf()

            val modifierA = modifiers[i]
            val filterDeps = analyzeFilterDependencies(modifierA.filter)

            if (filterDeps.isEmpty) continue

            for (j in modifiers.indices) {
                if (i == j) continue

                val modifierB = modifiers[j]
                val modEffects = analyzeModificationEffects(modifierB.modification)

                // Check if B modifies characteristics that A's filter depends on
                if (createsDependency(filterDeps, modEffects)) {
                    // A depends on B (B must be applied first)
                    graph[i]!!.add(j)
                }
            }
        }

        return graph
    }

    /**
     * Analyze what characteristics a filter depends on.
     */
    private fun analyzeFilterDependencies(filter: ModifierFilter): FilterDependencies {
        return when (filter) {
            is ModifierFilter.Self -> FilterDependencies()
            is ModifierFilter.AttachedTo -> FilterDependencies()
            is ModifierFilter.Specific -> FilterDependencies()
            is ModifierFilter.ControlledBy -> FilterDependencies()
            is ModifierFilter.Opponents -> FilterDependencies()
            is ModifierFilter.All -> analyzeCriteriaDependencies(filter.criteria)
        }
    }

    /**
     * Recursively analyze criteria to find characteristic dependencies.
     */
    private fun analyzeCriteriaDependencies(criteria: EntityCriteria): FilterDependencies {
        return when (criteria) {
            is EntityCriteria.Creatures -> FilterDependencies(
                dependsOnTypes = setOf(CardType.CREATURE)
            )
            is EntityCriteria.Lands -> FilterDependencies(
                dependsOnTypes = setOf(CardType.LAND)
            )
            is EntityCriteria.Artifacts -> FilterDependencies(
                dependsOnTypes = setOf(CardType.ARTIFACT)
            )
            is EntityCriteria.Enchantments -> FilterDependencies(
                dependsOnTypes = setOf(CardType.ENCHANTMENT)
            )
            is EntityCriteria.Permanents -> FilterDependencies(
                dependsOnTypes = setOf(
                    CardType.CREATURE, CardType.LAND, CardType.ARTIFACT,
                    CardType.ENCHANTMENT, CardType.PLANESWALKER
                )
            )
            is EntityCriteria.WithKeyword -> FilterDependencies(
                dependsOnKeywords = setOf(criteria.keyword)
            )
            is EntityCriteria.WithSubtype -> FilterDependencies(
                dependsOnSubtypes = setOf(criteria.subtype)
            )
            is EntityCriteria.WithColor -> FilterDependencies(
                dependsOnColors = setOf(criteria.color)
            )
            is EntityCriteria.And -> {
                // Combine dependencies from all sub-criteria
                criteria.criteria.fold(FilterDependencies()) { acc, c ->
                    val sub = analyzeCriteriaDependencies(c)
                    FilterDependencies(
                        dependsOnTypes = acc.dependsOnTypes + sub.dependsOnTypes,
                        dependsOnSubtypes = acc.dependsOnSubtypes + sub.dependsOnSubtypes,
                        dependsOnColors = acc.dependsOnColors + sub.dependsOnColors,
                        dependsOnKeywords = acc.dependsOnKeywords + sub.dependsOnKeywords
                    )
                }
            }
            is EntityCriteria.Or -> {
                // Combine dependencies from all sub-criteria
                criteria.criteria.fold(FilterDependencies()) { acc, c ->
                    val sub = analyzeCriteriaDependencies(c)
                    FilterDependencies(
                        dependsOnTypes = acc.dependsOnTypes + sub.dependsOnTypes,
                        dependsOnSubtypes = acc.dependsOnSubtypes + sub.dependsOnSubtypes,
                        dependsOnColors = acc.dependsOnColors + sub.dependsOnColors,
                        dependsOnKeywords = acc.dependsOnKeywords + sub.dependsOnKeywords
                    )
                }
            }
            is EntityCriteria.Not -> analyzeCriteriaDependencies(criteria.criteria)
        }
    }

    /**
     * Analyze what characteristics a modification changes.
     */
    private fun analyzeModificationEffects(modification: Modification): ModificationEffects {
        return when (modification) {
            is Modification.ChangeControl -> ModificationEffects()

            is Modification.AddType -> ModificationEffects(
                modifiesTypes = setOf(modification.type)
            )
            is Modification.RemoveType -> ModificationEffects(
                modifiesTypes = setOf(modification.type)
            )
            is Modification.AddSubtype -> ModificationEffects(
                modifiesSubtypes = setOf(modification.subtype)
            )
            is Modification.RemoveSubtype -> ModificationEffects(
                modifiesSubtypes = setOf(modification.subtype)
            )
            is Modification.SetSubtypes -> ModificationEffects(
                modifiesSubtypes = modification.subtypes
            )

            is Modification.AddColor -> ModificationEffects(
                modifiesColors = setOf(modification.color)
            )
            is Modification.RemoveColor -> ModificationEffects(
                modifiesColors = setOf(modification.color)
            )
            is Modification.SetColors -> ModificationEffects(
                modifiesColors = modification.colors
            )

            is Modification.AddKeyword -> ModificationEffects(
                modifiesKeywords = setOf(modification.keyword)
            )
            is Modification.RemoveKeyword -> ModificationEffects(
                modifiesKeywords = setOf(modification.keyword)
            )
            is Modification.RemoveAllAbilities -> ModificationEffects(
                modifiesKeywords = Keyword.entries.toSet()
            )
            is Modification.AddCantBlockRestriction -> ModificationEffects()
            is Modification.AssignDamageEqualToToughness -> ModificationEffects()

            is Modification.SetPTFromCDA -> ModificationEffects(
                modifiesPower = true,
                modifiesToughness = true
            )
            is Modification.SetPT -> ModificationEffects(
                modifiesPower = true,
                modifiesToughness = true
            )
            is Modification.SetPower -> ModificationEffects(modifiesPower = true)
            is Modification.SetToughness -> ModificationEffects(modifiesToughness = true)
            is Modification.ModifyPT -> ModificationEffects(
                modifiesPower = true,
                modifiesToughness = true
            )
            is Modification.ModifyPower -> ModificationEffects(modifiesPower = true)
            is Modification.ModifyToughness -> ModificationEffects(modifiesToughness = true)
            is Modification.ModifyPTDynamic -> ModificationEffects(
                modifiesPower = true,
                modifiesToughness = true
            )
            is Modification.SwitchPT -> ModificationEffects(
                modifiesPower = true,
                modifiesToughness = true
            )
        }
    }

    /**
     * Check if modification effects would affect filter dependencies.
     */
    private fun createsDependency(
        filterDeps: FilterDependencies,
        modEffects: ModificationEffects
    ): Boolean {
        // Type dependencies
        if (filterDeps.dependsOnTypes.any { it in modEffects.modifiesTypes }) return true

        // Subtype dependencies
        if (filterDeps.dependsOnSubtypes.any { it in modEffects.modifiesSubtypes }) return true

        // Color dependencies
        if (filterDeps.dependsOnColors.any { it in modEffects.modifiesColors }) return true

        // Keyword dependencies
        if (filterDeps.dependsOnKeywords.any { it in modEffects.modifiesKeywords }) return true

        return false
    }

    /**
     * Perform topological sort with cycle detection.
     *
     * In MTG, dependency cycles are handled by using timestamps as tiebreaker
     * when no clear ordering exists.
     */
    private fun topologicalSort(
        modifiers: List<Modifier>,
        dependencies: Map<Int, Set<Int>>
    ): List<Modifier> {
        val result = mutableListOf<Int>()
        val visited = mutableSetOf<Int>()
        val inProgress = mutableSetOf<Int>()

        fun visit(index: Int) {
            if (index in visited) return
            if (index in inProgress) {
                // Cycle detected - use timestamp to break tie
                // This is allowed per MTG rules when dependencies are circular
                return
            }

            inProgress.add(index)

            // Visit dependencies first (they need to be applied before this modifier)
            for (depIndex in dependencies[index] ?: emptySet()) {
                visit(depIndex)
            }

            inProgress.remove(index)
            visited.add(index)
            result.add(index)
        }

        // Sort by timestamp first, then apply topological ordering
        val timestampSorted = modifiers.indices.sortedBy { modifiers[it].timestamp }
        for (index in timestampSorted) {
            visit(index)
        }

        return result.map { modifiers[it] }
    }

    companion object {
        /**
         * Check if two modifiers might have a dependency relationship.
         *
         * This is a quick check to avoid full analysis when not needed.
         */
        fun mightHaveDependency(modifierA: Modifier, modifierB: Modifier): Boolean {
            // Must be in the same layer
            if (modifierA.layer != modifierB.layer) return false

            // Check if either has a filter that could depend on characteristics
            val hasCharacteristicFilter = { filter: ModifierFilter ->
                filter is ModifierFilter.All
            }

            val aHasFilter = hasCharacteristicFilter(modifierA.filter)
            val bHasFilter = hasCharacteristicFilter(modifierB.filter)

            // At least one needs a characteristic-based filter
            return aHasFilter || bHasFilter
        }
    }
}
