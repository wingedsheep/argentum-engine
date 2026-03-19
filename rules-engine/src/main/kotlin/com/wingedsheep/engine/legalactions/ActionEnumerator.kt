package com.wingedsheep.engine.legalactions

/**
 * Interface for action enumerators.
 *
 * Each enumerator is responsible for discovering one category of legal actions
 * (e.g., spells, lands, combat, abilities). This follows the same Strategy pattern
 * as ActionHandler but for enumeration rather than validation/execution.
 */
interface ActionEnumerator {
    /**
     * Enumerate all legal actions in this category.
     *
     * @param context Shared precomputed state for the enumeration
     * @return All legal actions in this category (including unaffordable ones)
     */
    fun enumerate(context: EnumerationContext): List<LegalAction>
}
