package com.wingedsheep.engine.state.components.identity

import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.Component
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CreatureStats
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.EntryCharacteristics
import kotlinx.serialization.Serializable

/**
 * The [CardComponent] a permanent had before an "as this enters, it becomes your choice of …"
 * option ([com.wingedsheep.sdk.scripting.ModeOption.becomes]) was written into it.
 *
 * Those choices set copiable values (CR 707.2), so they are baked straight into the permanent's
 * [CardComponent] — the component every copy effect copies ([copiableCardComponent]) and
 * projection seeds from. This snapshot undoes the bake when the permanent leaves the battlefield
 * (CR 400.7): the card is a star/star creature card again everywhere else.
 *
 * Not recorded when the permanent is already a copy with its own revert snapshot
 * ([CopyOfComponent.originalCardComponent]); that revert already restores the printed card.
 */
@Serializable
data class EntryCharacteristicsComponent(
    val unbakedCard: CardComponent,
) : Component

object EntryCharacteristicsBaking {

    /**
     * Write the characteristics of the MODE option [modeId] into [container]'s [CardComponent], if
     * the card's own `EntersWithChoice` option says it *becomes* something. The option is looked up
     * on the definition the permanent currently has — a copy entering as a copy makes the copied
     * card's choice, layered over the copied (already chosen) values: the keywords and subtypes
     * accumulate and the power/toughness is replaced.
     */
    fun bake(container: ComponentContainer, modeId: String, cardRegistry: CardRegistry): ComponentContainer {
        val card = container.get<CardComponent>() ?: return container
        val becomes = cardRegistry.getCard(card.cardDefinitionId)
            ?.script?.replacementEffects
            ?.filterIsInstance<EntersWithChoice>()
            ?.filter { it.choiceType == ChoiceType.MODE }
            ?.flatMap { it.modeOptions }
            ?.firstOrNull { it.id == modeId }
            ?.becomes
            ?: return container
        val alreadyReverts = container.get<CopyOfComponent>()?.originalCardComponent != null
        val snapshotted = if (alreadyReverts || container.has<EntryCharacteristicsComponent>()) container
            else container.with(EntryCharacteristicsComponent(card))
        return snapshotted.with(card.becoming(becomes))
    }

    private fun CardComponent.becoming(becomes: EntryCharacteristics): CardComponent = copy(
        baseStats = CreatureStats(becomes.power, becomes.toughness),
        baseKeywords = baseKeywords + becomes.keywords,
        typeLine = typeLine.copy(subtypes = typeLine.subtypes + becomes.subtypes.map { Subtype(it) }),
    )

    /** Undo [bake] on a permanent leaving the battlefield. No-op when nothing was baked. */
    fun unbake(container: ComponentContainer): ComponentContainer {
        val snapshot = container.get<EntryCharacteristicsComponent>() ?: return container
        return container.with(snapshot.unbakedCard).without<EntryCharacteristicsComponent>()
    }
}
