package com.wingedsheep.mtg.sets.definitions.dom.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.effects.ForEachTargetEffect
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.effects.ShuffleLibraryEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Gaea's Blessing
 * {1}{G}
 * Sorcery
 * Target player shuffles up to three target cards from their graveyard into their
 * library. Draw a card.
 * When Gaea's Blessing is put into your graveyard from your library, shuffle your
 * graveyard into your library.
 */
val GaeasBlessing = card("Gaea's Blessing") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Target player shuffles up to three target cards from their graveyard into their library. Draw a card.\nWhen Gaea's Blessing is put into your graveyard from your library, shuffle your graveyard into your library."

    spell {
        // Target up to 3 cards from any graveyard, move them to owner's library,
        // shuffle, and draw a card.
        target = TargetObject(
            count = 3,
            optional = true,
            filter = TargetFilter.CardInGraveyard
        )
        effect = ForEachTargetEffect(
            effects = listOf(MoveToZoneEffect(EffectTarget.ContextTarget(0), Zone.LIBRARY))
        ).then(ShuffleLibraryEffect())
            .then(Effects.DrawCards(1))
    }

    // When this card is put into your graveyard from your library,
    // shuffle your graveyard into your library.
    triggeredAbility {
        trigger = TriggerSpec(
            event = EventPattern.ZoneChangeEvent(from = Zone.LIBRARY, to = Zone.GRAVEYARD),
            binding = TriggerBinding.SELF
        )
        triggerZone = Zone.GRAVEYARD
        effect = EffectPatterns.shuffleGraveyardIntoLibrary(EffectTarget.Controller)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "161"
        artist = "David Palumbo"
        imageUri = "https://cards.scryfall.io/normal/front/2/3/23cf81ed-b86c-42b8-b796-2032b0a3654a.jpg?1562732710"
    }
}
