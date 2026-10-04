package com.wingedsheep.mtg.sets.definitions.cn2.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Recruiter of the Guard
 * {2}{W}
 * Creature — Human Soldier
 * 1/1
 * When this creature enters, you may search your library for a creature card with toughness 2 or less,
 * reveal it, put it into your hand, then shuffle.
 */
val RecruiterOfTheGuard = card("Recruiter of the Guard") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 1
    toughness = 1
    oracleText = "When this creature enters, you may search your library for a creature card with toughness 2 or less, reveal it, put it into your hand, then shuffle."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.May(
            Patterns.Library.searchLibrary(
                filter = GameObjectFilter.Creature.toughnessAtMost(2),
                count = 1,
                destination = SearchDestination.HAND,
                reveal = true,
                shuffleAfter = true
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "22"
        artist = "Jason Rainville"
        flavorText = "Before a cause can have supporters, it has to have a voice."
        imageUri = "https://cards.scryfall.io/normal/front/b/b/bb9ad57f-cca2-4717-a951-cbe3c7782efe.jpg?1783937362"

        ruling("2024-06-07", "If a creature card has \"*\" in its toughness, the ability that defines its toughness works in all zones. For example, if Nethergoyf is in your library, its toughness is determined by the ability \"Nethergoyf's power is equal to the number of card types among cards in your graveyard and its toughness is equal to that number plus 1.\" If your graveyard consists of an artifact card, a creature card, and an instant card, you won't be able to find Nethergoyf with Recruiter of the Guard since Nethergoyf's toughness will be 4.")
    }
}
