package com.wingedsheep.engine.core

import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ConvertEmptyingManaToRed
import com.wingedsheep.sdk.scripting.RetainUnspentColoredMana

/**
 * Players who control a permanent with the [ConvertEmptyingManaToRed] static ability
 * (Ozai, the Phoenix King: "If you would lose unspent mana, that mana becomes red instead").
 *
 * The static fires at *every* mana-loss point, not just one: both the end-of-turn cleanup emptying
 * ([CleanupPhaseManager.cleanupEndOfTurn]) and the end-of-combat firebending-mana discard
 * ([com.wingedsheep.engine.mechanics.combat.CombatManager.endCombat]) consult this set, so
 * firebending mana that would otherwise be lost as combat ends becomes red and survives the rest of
 * the turn. Controller is read from projected state so a control-changed Ozai converts for its new
 * controller.
 */
fun playersConvertingEmptyingManaToRed(state: GameState, cardRegistry: CardRegistry): Set<EntityId> {
    val projected = state.projectedState
    val result = mutableSetOf<EntityId>()
    for (entityId in state.getBattlefield()) {
        val card = state.getEntity(entityId)?.get<CardComponent>() ?: continue
        val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
        if (cardDef.script.staticAbilities.any { it is ConvertEmptyingManaToRed }) {
            projected.getController(entityId)?.let { result.add(it) }
        }
    }
    return result
}

/**
 * Colours [playerId] keeps at every step/phase-end because they control a permanent with a
 * [RetainUnspentColoredMana] static (Electro, Assaulting Battery: "You don't lose unspent red mana
 * as steps and phases end"). Consulted by [CleanupPhaseManager.emptyManaPools], which unions these
 * into the per-player `retain` set alongside the turn-scoped [RetainUnspentManaComponent] marker.
 * Controller is read from projected state so a stolen Electro retains for its new controller.
 *
 * Sibling of [playersConvertingEmptyingManaToRed]; both scan printed static abilities. A permanent
 * whose abilities are removed by a Layer-6 wipe would still be counted here — an accepted, shared
 * limitation, not modelled by either scan.
 */
fun retainedColorsFromStatics(
    state: GameState,
    cardRegistry: CardRegistry,
    playerId: EntityId
): Set<Color> {
    val projected = state.projectedState
    val colors = mutableSetOf<Color>()
    for (entityId in state.getBattlefield()) {
        if (projected.getController(entityId) != playerId) continue
        val card = state.getEntity(entityId)?.get<CardComponent>() ?: continue
        val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
        for (ability in cardDef.script.staticAbilities) {
            if (ability is RetainUnspentColoredMana) colors.add(ability.color)
        }
    }
    return colors
}
