package com.wingedsheep.engine.core

import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ConvertEmptyingMana
import com.wingedsheep.sdk.scripting.RetainUnspentColoredMana

/**
 * The colour each player's would-be-lost mana converts to, for every player who controls a
 * permanent with a [ConvertEmptyingMana] static ability ("If you would lose unspent mana, that mana
 * becomes [color] instead" — Ozai, the Phoenix King; Omnath, Locus of All).
 *
 * The static fires at *every* mana-loss point, not just one: both the step/phase-end emptying
 * ([CleanupPhaseManager.emptyManaPools]) and the end-of-combat firebending-mana discard
 * ([com.wingedsheep.engine.mechanics.combat.CombatManager.endCombat]) consult this map. Controller
 * is read from projected state so a control-changed permanent converts for its new controller.
 *
 * A player who controls two such permanents naming different colours has two replacement effects
 * for the same event; the affected player picks which applies (CR 616.1), and once one has, the mana
 * is no longer lost so the other can't. The engine applies the one on the permanent that has been on
 * the battlefield longest (battlefield order) rather than asking.
 */
fun emptyingManaConversions(state: GameState, cardRegistry: CardRegistry): Map<EntityId, Color> {
    val projected = state.projectedState
    val result = mutableMapOf<EntityId, Color>()
    for (entityId in state.getBattlefield()) {
        val card = state.getEntity(entityId)?.get<CardComponent>() ?: continue
        val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
        val conversion = cardDef.script.staticAbilities.firstNotNullOfOrNull { it as? ConvertEmptyingMana } ?: continue
        val controller = projected.getController(entityId) ?: continue
        result.putIfAbsent(controller, conversion.color)
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
 * Sibling of [emptyingManaConversions]; both scan printed static abilities. A permanent
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
