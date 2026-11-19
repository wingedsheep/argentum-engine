package com.wingedsheep.engine.mechanics.targeting

import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ProtectionScope

/**
 * The one reading of "protection from <card type>s your opponents control" (Crypsis) —
 * [ProtectionScope.CardType] with `controller = ControlledByOpponent`, projected as
 * `PROTECTION_FROM_OPPONENTS_CARDTYPE_<TYPE>`. Shared by every leg that enforces protection
 * (targeting, target re-validation on resolution, damage prevention, attachment and blocking,
 * CR 702.16b–f) so they can't drift apart.
 *
 * A source matches when it has the card type *and* its controller is an opponent of the protected
 * permanent's controller, both read when protection is checked: a creature that changes hands
 * changes whether it matches, and the protected permanent's own controller's creatures never do.
 */
object OpponentCardTypeProtection {

    private const val PREFIX = ProtectionScope.CardType.OPPONENTS_KEYWORD_PREFIX

    /**
     * The card type (as given in [sourceTypes]) [protectedId] has protection from, given a source
     * of those types controlled by [sourceController] — or null.
     */
    fun matchedType(
        state: GameState,
        projected: ProjectedState,
        protectedId: EntityId,
        sourceTypes: Collection<String>,
        sourceController: EntityId?
    ): String? {
        if (sourceController == null || sourceTypes.isEmpty()) return null
        val keywords = projected.getKeywords(protectedId)
        if (keywords.none { it.startsWith(PREFIX) }) return null
        val protectedController = projected.getController(protectedId)
            ?: state.getEntity(protectedId)?.get<ControllerComponent>()?.playerId
            ?: return null
        if (!state.isOpponentOf(sourceController, protectedController)) return null
        return sourceTypes.firstOrNull { PREFIX + it.uppercase() in keywords }
    }

    /**
     * [matchedType] for a live [sourceId]: its projected types and controller on the battlefield,
     * its printed types elsewhere (a spell, or a card being cast) with [fallbackController] — the
     * spell's or ability's controller — standing in for the controller.
     */
    fun matchedType(
        state: GameState,
        protectedId: EntityId,
        sourceId: EntityId?,
        fallbackController: EntityId?
    ): String? {
        if (sourceId == null) return null
        val projected = state.projectedState
        if (projected.getKeywords(protectedId).none { it.startsWith(PREFIX) }) return null
        val onBattlefield = sourceId in state.getBattlefield()
        val types = if (onBattlefield) projected.getTypes(sourceId)
            else state.getEntity(sourceId)?.get<CardComponent>()?.typeLine?.cardTypes?.map { it.name }.orEmpty()
        val controller = (if (onBattlefield) projected.getController(sourceId) else null) ?: fallbackController
        return matchedType(state, projected, protectedId, types, controller)
    }

    /** "creatures your opponents control" for the matched [cardType]. */
    fun describe(cardType: String): String = "${cardType.lowercase()}s your opponents control"

    /**
     * Why [targetId] can't be targeted by a spell or ability from [sourceId] controlled by
     * [casterId], or null (CR 702.16b — protection blocks every controller, but this quality
     * only matches opponents' objects).
     */
    fun targetingError(state: GameState, targetId: EntityId, sourceId: EntityId?, casterId: EntityId): String? {
        if (targetId !in state.getBattlefield()) return null
        val type = matchedType(state, targetId, sourceId, casterId) ?: return null
        val name = state.getEntity(targetId)?.get<CardComponent>()?.name ?: "target"
        return "$name has protection from ${describe(type)}"
    }
}
