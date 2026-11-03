package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sunrise Seeker
 * {4}{W}
 * Creature — Human Scout
 * 3/3
 * Vigilance
 * When this creature enters, it explores.
 */
val SunriseSeeker = card("Sunrise Seeker") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Scout"
    power = 3
    toughness = 3
    oracleText = "Vigilance\n" +
        "When this creature enters, it explores. (Reveal the top card of your library. Put that card into your hand if it's a land. Otherwise, put a +1/+1 counter on this creature, then put the card back or put it into your graveyard.)"

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Explore(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "40"
        artist = "Filip Burburan"
        imageUri = "https://cards.scryfall.io/normal/front/7/0/709d87b3-bf42-47ed-961d-d0e86d3e1d90.jpg?1783935788"
    }
}
