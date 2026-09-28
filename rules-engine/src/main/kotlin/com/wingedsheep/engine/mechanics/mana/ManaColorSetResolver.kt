package com.wingedsheep.engine.mechanics.mana
import com.wingedsheep.engine.state.components.battlefield.chosenColor

import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.CommanderRegistryComponent
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.targets.EffectTarget
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

    /**
     * Resolve [colorSet] given the current game state. Returns the set of colors the
     * controller may pick from; an empty result means no mana is produced.
     *
     * @param sourceId The permanent/spell providing the mana ability (used by
     *   [ManaColorSet.SourceChosenColor] to read `CastChoicesComponent`).
     * @param controllerId The player resolving the ability — used by
     *   [ManaColorSet.CommanderIdentity] (commander lookup),
     *   [ManaColorSet.AmongPermanents] (control filter), and
     *   [ManaColorSet.LandsCouldProduce] (scope resolution).
     * @param resolveEntity Resolves the object a [ManaColorSet.ColorsOf] names. Effect executors
     *   pass their `EffectContext`'s resolver; the default only knows `Self` (the source).
     */
    fun resolve(
        colorSet: ManaColorSet,
        state: GameState,
        projected: ProjectedState,
        sourceId: EntityId?,
        controllerId: EntityId,
        cardRegistry: CardRegistry,
        predicateEvaluator: PredicateEvaluator,
        resolveEntity: (EffectTarget) -> EntityId? = { if (it == EffectTarget.Self) sourceId else null }
    ): Set<Color> = when (colorSet) {
        is ManaColorSet.AnyColor -> Color.entries.toSet()
        is ManaColorSet.Specific -> colorSet.colors
        is ManaColorSet.CommanderIdentity -> commanderIdentity(state, controllerId, cardRegistry)
        is ManaColorSet.AmongPermanents -> amongPermanents(colorSet, state, projected, controllerId, predicateEvaluator = predicateEvaluator)
        is ManaColorSet.AmongCardsInGraveyard -> amongCardsInGraveyard(colorSet, state, projected, controllerId, predicateEvaluator = predicateEvaluator)
        is ManaColorSet.LandsCouldProduce -> landsCouldProduce(colorSet, state, projected, controllerId, cardRegistry)
        is ManaColorSet.SourceChosenColor -> sourceChosenColor(state, sourceId)
        is ManaColorSet.AmongLinkedExiledCards -> amongLinkedExiledCards(state, sourceId)
        is ManaColorSet.ColorsOf -> colorsOf(state, projected, resolveEntity(colorSet.entity))
        is ManaColorSet.Union -> colorSet.members.flatMapTo(mutableSetOf()) { member ->
            resolve(member, state, projected, sourceId, controllerId, cardRegistry, predicateEvaluator, resolveEntity)
        }
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
        predicateEvaluator: PredicateEvaluator
    ): Set<Color> {
        val predCtx = PredicateContext(controllerId = controllerId)
        val colors = mutableSetOf<Color>()
        for (entityId in state.getBattlefield()) {
            if (!predicateEvaluator.matches(state, projected, entityId, colorSet.filter, predCtx)) continue
            for (colorName in projected.getColors(entityId)) {
                Color.entries.find { it.name == colorName }?.let { colors.add(it) }
            }
        }
        return colors
    }

    private fun amongCardsInGraveyard(
        colorSet: ManaColorSet.AmongCardsInGraveyard,
        state: GameState,
        projected: ProjectedState,
        controllerId: EntityId,
        predicateEvaluator: PredicateEvaluator
    ): Set<Color> {
        val predCtx = PredicateContext(controllerId = controllerId)
        val colors = mutableSetOf<Color>()
        for (entityId in state.getGraveyard(controllerId)) {
            if (!predicateEvaluator.matches(state, projected, entityId, colorSet.filter, predCtx)) continue
            val cardColors = state.getEntity(entityId)
                ?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.colors.orEmpty()
            colors.addAll(cardColors)
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

    private fun colorsOf(state: GameState, projected: ProjectedState, entityId: EntityId?): Set<Color> {
        if (entityId == null) return emptySet()
        if (entityId in state.getBattlefield()) {
            return projected.getColors(entityId).mapNotNullTo(mutableSetOf()) { name -> Color.entries.find { it.name == name } }
        }
        return state.getEntity(entityId)?.get<CardComponent>()?.colors.orEmpty()
    }

    private fun sourceChosenColor(state: GameState, sourceId: EntityId?): Set<Color> {
        val source = sourceId?.let { state.getEntity(it) } ?: return emptySet()
        return setOfNotNull(source.chosenColor())
    }

    /**
     * Union of the base colors of the cards currently exiled with the source permanent.
     *
     * The candidate ids come from the source's [LinkedExileComponent] (stamped by
     * `MoveToZoneEffect(linkToSource = true)`); each is counted only while it is *still in the
     * exile zone* — a card that has since left exile is no longer "exiled with" the source, so it
     * drops out of the color pool (matching the still-in-exile filter used by the linked-exile
     * return executors). Colors are read from each card's base [CardComponent.colors]; exile-zone
     * cards aren't projected. Colorless-only or empty piles yield an empty set → no mana produced.
     */
    private fun amongLinkedExiledCards(state: GameState, sourceId: EntityId?): Set<Color> {
        val source = sourceId?.let { state.getEntity(it) } ?: return emptySet()
        val exiledIds = source.get<LinkedExileComponent>()?.exiledIds ?: return emptySet()
        val colors = mutableSetOf<Color>()
        for (id in exiledIds) {
            // An exiled card lives in its owner's exile zone (ZoneTransitionService keys every
            // non-battlefield zone by owner), so a direct membership check confirms it's still
            // exiled — a card that has since left exile drops out of the color pool.
            val card = state.getEntity(id)?.get<CardComponent>() ?: continue
            val ownerId = card.ownerId ?: continue
            if (id !in state.getExile(ownerId)) continue
            colors.addAll(card.colors)
        }
        return colors
    }
}
