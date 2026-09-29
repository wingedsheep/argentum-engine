package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.mechanics.layers.ContinuousEffect
import com.wingedsheep.engine.mechanics.layers.Modification
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.Component
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject
import kotlinx.serialization.Serializable

/** Bestow's status survives stack-to-battlefield, but is not a copiable permanent characteristic. */
@Serializable
data class BestowedComponent(val original: CardComponent, val timestamp: Long, val entered: Boolean = false) : Component

/** Shared announcement, type effect and termination of bestow (CR 702.103). */
object BestowCasts {
    const val ENCHANT_CREATURE = "ENCHANT_CREATURE"
    val enchantCreature = TargetObject(filter = TargetFilter.Creature)

    fun selected(action: CastSpell) = action.useAlternativeCost && action.alternativeCostType == AlternativeCostType.BESTOW

    fun definitionForCast(definition: CardDefinition?, action: CastSpell): CardDefinition? =
        if (selected(action) && definition?.keywordAbilities?.any { it is KeywordAbility.Bestow } == true) {
            definition.copy(
                typeLine = definition.typeLine.copy(cardTypes = setOf(CardType.ENCHANTMENT), subtypes = setOf(Subtype.AURA)),
                script = definition.script.copy(auraTarget = enchantCreature)
            )
        } else definition

    /** A trial announcement for validation, committed only by the successful cast's returned state. */
    fun announce(state: GameState, action: CastSpell, registry: CardRegistry): GameState {
        if (!selected(action)) return state
        val container = state.getEntity(action.cardId) ?: return state
        if (container.has<BestowedComponent>()) return state
        val original = container.get<CardComponent>() ?: return state
        val definition = registry.getCard(original.cardDefinitionId) ?: return state
        if (definition.keywordAbilities.none { it is KeywordAbility.Bestow }) return state
        return state.updateEntity(action.cardId) {
            it.with(BestowedComponent(original, state.timestamp)).with(original.copy(
                typeLine = original.typeLine.copy(cardTypes = setOf(CardType.ENCHANTMENT), subtypes = setOf(Subtype.AURA)),
                baseStats = null
            ))
        }
    }

    /** Stack spells use their announced characteristics; permanents apply the effect in layer 4. */
    fun effects(state: GameState): List<ContinuousEffect> = buildList {
        for (id in state.getBattlefield()) {
            val bestowed = state.getEntity(id)?.get<BestowedComponent>() ?: continue
            add(ContinuousEffect(id, bestowed.timestamp, Modification.SetCardTypes(setOf("ENCHANTMENT")), setOf(id), fromStaticAbility = false))
            add(ContinuousEffect(id, bestowed.timestamp, Modification.SetAllSubtypes(setOf("Aura")), setOf(id), fromStaticAbility = false))
            add(ContinuousEffect(id, bestowed.timestamp, Modification.GrantKeyword(ENCHANT_CREATURE), setOf(id), fromStaticAbility = false))
        }
    }

    fun restoreBaseCharacteristics(state: GameState, id: EntityId): GameState {
        val container = state.getEntity(id) ?: return state
        val bestowed = container.get<BestowedComponent>() ?: return state
        val current = container.get<CardComponent>() ?: return state
        // Bestow only changed card types, subtypes, and P/T. Spell-copy exceptions such as
        // nonlegendary and added token keywords must survive restoring those characteristics.
        return state.updateEntity(id) {
            it.with(current.copy(
                typeLine = current.typeLine.copy(
                    cardTypes = bestowed.original.typeLine.cardTypes,
                    subtypes = bestowed.original.typeLine.subtypes
                ),
                baseStats = bestowed.original.baseStats
            ))
        }
    }

    fun end(state: GameState, id: EntityId): GameState {
        val bestowed = state.getEntity(id)?.get<BestowedComponent>() ?: return state
        val restored = if (bestowed.entered) state else restoreBaseCharacteristics(state, id)
        return restored.updateEntity(id) { it.without<BestowedComponent>() }
    }
}
