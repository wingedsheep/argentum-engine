package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Emissary of Sunrise
 * {2}{W}
 * Creature — Human Cleric
 * 2/1
 * First strike
 * When this creature enters, it explores.
 */
val EmissaryOfSunrise = card("Emissary of Sunrise") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Cleric"
    power = 2
    toughness = 1
    oracleText = "First strike\n" +
        "When this creature enters, it explores. (Reveal the top card of your library. Put that card into your hand if it's a land. Otherwise, put a +1/+1 counter on this creature, then put the card back or put it into your graveyard.)"

    keywords(Keyword.FIRST_STRIKE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Explore(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "10"
        artist = "Deruchenko Alexander"
        imageUri = "https://cards.scryfall.io/normal/front/0/6/0636cd47-d2a3-4320-97bf-40db805fca51.jpg?1783935802"
    }
}
