package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.Component
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CreatureStats
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.KeywordAbility
import kotlinx.serialization.Serializable

/**
 * Marks a prototyped spell, or the permanent it became, and keeps the card's normal mana cost,
 * colors and size for when it leaves the stack or battlefield (CR 718.4). The prototyped values
 * themselves live on the [CardComponent], so they are copiable values (CR 718.2a, 718.3c–d).
 */
@Serializable
data class PrototypedComponent(
    val originalManaCost: ManaCost,
    val originalColors: Set<Color>,
    val originalStats: CreatureStats?,
) : Component

/** Announcement and termination of a prototyped cast (CR 702.160, 718). */
object PrototypeCasts {
    fun prototypeOf(definition: CardDefinition?): KeywordAbility.Prototype? =
        definition?.keywordAbilities?.firstNotNullOfOrNull { it as? KeywordAbility.Prototype }

    /** CR 718.3a: only the prototype mana cost and size count while casting it prototyped. */
    fun definitionForCast(definition: CardDefinition?, action: CastSpell): CardDefinition? {
        if (!action.castPrototyped) return definition
        val prototype = prototypeOf(definition) ?: return definition
        return definition!!.copy(
            manaCost = prototype.cost,
            creatureStats = CreatureStats(prototype.power, prototype.toughness),
        )
    }

    /**
     * Gives the card its prototype characteristics (CR 718.3b — colors follow the mana cost). A
     * trial announcement for validation, committed only by the successful cast's returned state.
     */
    fun announce(state: GameState, action: CastSpell, registry: CardRegistry): GameState {
        if (!action.castPrototyped) return state
        val container = state.getEntity(action.cardId) ?: return state
        if (container.has<PrototypedComponent>()) return state
        val card = container.get<CardComponent>() ?: return state
        val definition = registry.getCard(card.cardDefinitionId)
        val prototype = prototypeOf(definition) ?: return state
        return state.updateEntity(action.cardId) {
            it.with(PrototypedComponent(card.manaCost, card.colors, card.baseStats)).with(card.copy(
                manaCost = prototype.cost,
                colors = prototype.cost.colors + definition?.colorIndicator.orEmpty(),
                baseStats = CreatureStats(prototype.power, prototype.toughness),
            ))
        }
    }

    /** CR 718.4: anywhere but the stack and battlefield, the card has only its normal characteristics. */
    fun end(state: GameState, id: EntityId): GameState =
        if (state.getEntity(id)?.has<PrototypedComponent>() == true) state.updateEntity(id, ::restore) else state

    /** [end] for one container — the battlefield-exit strip runs on containers. */
    fun restore(container: ComponentContainer): ComponentContainer {
        val prototyped = container.get<PrototypedComponent>() ?: return container
        val card = container.get<CardComponent>()
        val restored = if (card == null) container else container.with(card.copy(
            manaCost = prototyped.originalManaCost,
            colors = prototyped.originalColors,
            baseStats = prototyped.originalStats,
        ))
        return restored.without<PrototypedComponent>()
    }
}
