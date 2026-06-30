package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Transit Mage
 * {2}{U}
 * Creature — Human Wizard
 * 2/2
 * When this creature enters, you may search your library for an artifact card with mana value 4
 * or 5, reveal it, put it into your hand, then shuffle.
 */
val TransitMage = card("Transit Mage") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Wizard"
    power = 2
    toughness = 2
    oracleText = "When this creature enters, you may search your library for an artifact card with " +
        "mana value 4 or 5, reveal it, put it into your hand, then shuffle."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = MayEffect(
            Patterns.Library.searchLibrary(
                filter = GameObjectFilter.Artifact.manaValueAtLeast(4).manaValueAtMost(5),
                count = 1,
                destination = SearchDestination.HAND,
                reveal = true,
                shuffleAfter = true
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "70"
        artist = "Mark Poole"
        flavorText = "\"Remember: You can take the wheel if you want, but I choose the music.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/7/6727169f-c33a-4ca5-889d-a63bcfc5a3f0.jpg?1782687907"
    }
}
