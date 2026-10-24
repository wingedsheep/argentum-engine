package com.wingedsheep.mtg.sets.definitions.dom.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.Triggers

/**
 * Gaea's Blessing
 * {1}{G}
 * Sorcery
 * Target player shuffles up to three target cards from their graveyard into their library.
 * Draw a card.
 * When this card is put into your graveyard from your library, shuffle your graveyard into
 * your library.
 */
val GaeasBlessing = card("Gaea's Blessing") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Target player shuffles up to three target cards from their graveyard into their library.\nDraw a card.\nWhen this card is put into your graveyard from your library, shuffle your graveyard into your library."

    spell {
        // The card slot reads the player slot: only cards from that player's graveyard.
        val player = target(Targets.Player)
        val cards = targets(
            TargetFilter(GameObjectFilter.Any.ownedByTargetPlayer(), zone = Zone.GRAVEYARD),
            count = 3,
            optional = true,
        )
        effect = Effects.ForEachTargetOf(cards, Effects.Move(EffectTarget.IterationEntity, Zone.LIBRARY)) then
            Effects.ShuffleLibrary(player) then
            Effects.DrawCards(1)
    }

    // When this card is put into your graveyard from your library,
    // shuffle your graveyard into your library.
    triggeredAbility {
        trigger = Triggers.self.changesZone(from = Zone.LIBRARY, to = Zone.GRAVEYARD)
        triggerZone = Zone.GRAVEYARD
        effect = Patterns.Library.shuffleGraveyardIntoLibrary(EffectTarget.Controller)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "161"
        artist = "David Palumbo"
        imageUri = "https://cards.scryfall.io/normal/front/2/3/23cf81ed-b86c-42b8-b796-2032b0a3654a.jpg?1562732710"
    }
}
