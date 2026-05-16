package com.wingedsheep.engine.mechanics.mana

import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ChosenColorComponent
import com.wingedsheep.engine.state.components.identity.CommanderRegistryComponent
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.values.LandControllerScope
import com.wingedsheep.sdk.scripting.values.ManaColorSet

/**
 * Resolves a [ManaColorSet] to a concrete `Set<Color>` at the moment of evaluation.
 *
 * One resolver per engine — all special-case branches that used to live in
 * `AddManaOf{AnyColor,ChosenColor,ColorAmong,ColorLandsCouldProduce,ColorInCommanderColorIdentity}Effect`
 * now flow through here, so a single `AddManaOfChoiceEffect` plus a `ManaColorSet`
 * covers every "pick from a constrained set of colors" card in the game.
 */
object ManaColorSetResolver {

    private val predicateEvaluator = PredicateEvaluator()

    /**
     * Resolve [colorSet] given the current game state. Returns the set of colors the
     * controller may pick from; an empty result means no mana is produced.
     *
     * @param sourceId The permanent/spell providing the mana ability (used by
     *   [ManaColorSet.SourceChosenColor] to read `ChosenColorComponent`).
     * @param controllerId The player resolving the ability — used by
     *   [ManaColorSet.CommanderIdentity] (commander lookup),
     *   [ManaColorSet.AmongPermanents] (control filter), and
     *   [ManaColorSet.LandsCouldProduce] (scope resolution).
     */
    fun resolve(
        colorSet: ManaColorSet,
        state: GameState,
        projected: ProjectedState,
        sourceId: EntityId?,
        controllerId: EntityId,
        cardRegistry: CardRegistry,
    ): Set<Color> = when (colorSet) {
        is ManaColorSet.AnyColor -> Color.entries.toSet()
        is ManaColorSet.Specific -> colorSet.colors
        is ManaColorSet.CommanderIdentity -> commanderIdentity(state, controllerId, cardRegistry)
        is ManaColorSet.AmongPermanents -> amongPermanents(colorSet, state, projected, controllerId)
        is ManaColorSet.LandsCouldProduce -> landsCouldProduce(colorSet, state, projected, controllerId, cardRegistry)
        is ManaColorSet.SourceChosenColor -> sourceChosenColor(state, sourceId)
    }

    /**
     * True when [colorSet] is statically "all five colors" (no game-state lookup needed).
     * The mana solver / legal-action enumerator uses this to short-circuit color choice
     * questions for the most common case.
     */
    fun isUniversal(colorSet: ManaColorSet): Boolean =
        colorSet is ManaColorSet.AnyColor

    private fun commanderIdentity(
        state: GameState,
        controllerId: EntityId,
        cardRegistry: CardRegistry,
    ): Set<Color> {
        val registry = state.getEntity(controllerId)
            ?.get<CommanderRegistryComponent>()
            ?: return emptySet()
        val colors = mutableSetOf<Color>()
        for (commanderId in registry.commanderIds) {
            val card = state.getEntity(commanderId)?.get<CardComponent>() ?: continue
            val def = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            colors.addAll(def.colorIdentity)
        }
        return colors
    }

    private fun amongPermanents(
        colorSet: ManaColorSet.AmongPermanents,
        state: GameState,
        projected: ProjectedState,
        controllerId: EntityId,
    ): Set<Color> {
        val predCtx = PredicateContext(controllerId = controllerId)
        val colors = mutableSetOf<Color>()
        for (entityId in state.getBattlefield()) {
            if (!predicateEvaluator.matchesWithProjection(state, projected, entityId, colorSet.filter, predCtx)) continue
            for (colorName in projected.getColors(entityId)) {
                Color.entries.find { it.name == colorName }?.let { colors.add(it) }
            }
        }
        return colors
    }

    private fun landsCouldProduce(
        colorSet: ManaColorSet.LandsCouldProduce,
        state: GameState,
        projected: ProjectedState,
        controllerId: EntityId,
        cardRegistry: CardRegistry,
    ): Set<Color> {
        val targetPlayers = when (colorSet.scope) {
            LandControllerScope.YOU -> setOf(controllerId)
            LandControllerScope.OPPONENTS -> state.turnOrder.filter { it != controllerId }.toSet()
            LandControllerScope.ANY -> state.turnOrder.toSet()
        }
        if (targetPlayers.isEmpty()) return emptySet()
        val landIds = state.getBattlefield().filter { permId ->
            val container = state.getEntity(permId) ?: return@filter false
            val card = container.get<CardComponent>() ?: return@filter false
            card.typeLine.isLand && projected.getController(permId) in targetPlayers
        }
        return LandManaColorInspector.colorsLandsCouldProduce(state, projected, landIds, cardRegistry)
    }

    private fun sourceChosenColor(state: GameState, sourceId: EntityId?): Set<Color> {
        val source = sourceId?.let { state.getEntity(it) } ?: return emptySet()
        return setOfNotNull(source.get<ChosenColorComponent>()?.color)
    }
}
