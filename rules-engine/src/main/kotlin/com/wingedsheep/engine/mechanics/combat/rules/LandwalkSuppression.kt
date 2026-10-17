package com.wingedsheep.engine.mechanics.combat.rules

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.mechanics.combat.CombatStaticScan
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.SuppressLandwalkForGroup

/**
 * Single source of truth for "which of this attacker's landwalk keywords are being ignored for
 * blocking?" — the [SuppressLandwalkForGroup] statics ("Creatures with swampwalk can be blocked as
 * though they didn't have swampwalk", Quagmire / Ur-Drago).
 *
 * Reads the board through [CombatStaticScan], like the other statics enforced at declaration:
 * live statics on battlefield permanents (not face down per CR 708.2, not ability-stripped, text
 * changes applied, conditions evaluated, Room faces honoured) plus runtime grants still in force,
 * each matched against the attacker with its holder as predicate source.
 *
 * Read by the engine's [LandwalkRule] and by the AI's combat math, so the blockers the AI
 * predicts agree with the ones the engine allows.
 */
object LandwalkSuppression {

    /** The landwalk keywords of [attackerId] that can be blocked as though it didn't have them. */
    fun suppressedLandwalks(
        state: GameState,
        attackerId: EntityId,
        cardRegistry: CardRegistry,
        predicateEvaluator: PredicateEvaluator
    ): Set<Keyword> {
        val entries = CombatStaticScan.collect(state, cardRegistry, predicateEvaluator) { it as? SuppressLandwalkForGroup }
        if (entries.isEmpty()) return emptySet()
        val suppressed = mutableSetOf<Keyword>()
        for (entry in entries) {
            if (suppressed.containsAll(entry.ability.landwalks)) continue
            if (CombatStaticScan.covers(state, predicateEvaluator, entry.holder, entry.controller, entry.ability.filter, attackerId)) {
                suppressed += entry.ability.landwalks
            }
        }
        return suppressed
    }
}
