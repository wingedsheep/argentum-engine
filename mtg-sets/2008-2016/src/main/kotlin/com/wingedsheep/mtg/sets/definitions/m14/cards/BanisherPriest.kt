package com.wingedsheep.mtg.sets.definitions.m14.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Banisher Priest
 * {1}{W}{W}
 * Creature — Human Cleric
 * 2/2
 * When this creature enters, exile target creature an opponent controls until this creature
 * leaves the battlefield.
 */
val BanisherPriest = card("Banisher Priest") {
    manaCost = "{1}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Cleric"
    oracleText = "When this creature enters, exile target creature an opponent controls until this creature leaves the battlefield."
    power = 2
    toughness = 2

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter(GameObjectFilter.Creature.opponentControls()))
        effect = Effects.ExileUntilLeaves(creature)
    }

    triggeredAbility {
        trigger = Triggers.self.leaves()
        effect = Effects.ReturnLinkedExileUnderOwnersControl()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "7"
        artist = "Willian Murai"
        flavorText = "\"Oathbreaker, I cast you out!\""
        imageUri = "https://cards.scryfall.io/normal/front/0/6/06823bf8-2fca-49e1-ba40-9b61c9ae55b3.jpg?1783939945"
    }
}
