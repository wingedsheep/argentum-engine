package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Leonin of the Lost Pride
 * {1}{W}
 * Creature — Cat Warrior
 * 3/1
 * When this creature dies, exile target card from an opponent's graveyard.
 */
val LeoninOfTheLostPride = card("Leonin of the Lost Pride") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Cat Warrior"
    power = 3
    toughness = 1
    oracleText = "When this creature dies, exile target card from an opponent's graveyard."

    triggeredAbility {
        trigger = Triggers.self.dies()
        val card = target(TargetFilter.CardInGraveyard.ownedByOpponent())
        effect = Effects.Exile(card)
        description = "When this creature dies, exile target card from an opponent's graveyard."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "28"
        artist = "Aaron Miller"
        flavorText = "Leonin band together in the Underworld, taking solace in their strength until, at last, " +
            "both pride and self are forgotten."
        imageUri = "https://cards.scryfall.io/normal/front/e/1/e1be3729-7970-45df-8c26-4fe09e56629d.jpg?1783931592"
    }
}
