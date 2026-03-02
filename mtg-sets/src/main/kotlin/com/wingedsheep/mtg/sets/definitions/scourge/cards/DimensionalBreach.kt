package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Dimensional Breach
 * {5}{W}{W}
 * Sorcery
 * Exile all permanents. For as long as any of those cards remain exiled, at the
 * beginning of each player's upkeep, that player returns one of the exiled cards
 * they own to the battlefield.
 *
 * Original text used "remove from the game" — current Oracle text uses "exile".
 */
val DimensionalBreach = card("Dimensional Breach") {
    manaCost = "{5}{W}{W}"
    typeLine = "Sorcery"
    oracleText = "Exile all permanents. For as long as any of those cards remain exiled, at the beginning of each player's upkeep, that player returns one of the exiled cards they own to the battlefield."

    spell {
        effect = Effects.ExileGroupAndLink(GroupFilter(GameObjectFilter.Any))
            .then(Effects.CreatePermanentGlobalTriggeredAbility(
                TriggeredAbility.create(
                    trigger = Triggers.EachUpkeep.event,
                    binding = Triggers.EachUpkeep.binding,
                    effect = Effects.ReturnOneFromLinkedExile()
                )
            ))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "9"
        artist = "Dave Dorman"
        imageUri = "https://cards.scryfall.io/normal/front/f/1/f18f2832-07c5-47be-8966-b250fb997f78.jpg?1562536853"
        ruling("2004-10-04", "Each player chooses which one of their exiled cards to return during each of their upkeeps.")
        ruling("2004-10-04", "You can't pick a card that can't legally be returned to the battlefield. If there are no cards that can be legally returned, then you can't pick one.")
    }
}
