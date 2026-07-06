package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.model.Rarity

/**
 * Kinjalli's Dawnrunner — {2}{W}
 * Creature — Human Scout
 * 1/1
 * Double strike
 * When this creature enters, it explores.
 */
val KinjallisDawnrunner = card("Kinjalli's Dawnrunner") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Scout"
    oracleText = "Double strike\nWhen this creature enters, it explores. (Reveal the top card of your library. Put that card into your hand if it's a land. Otherwise, put a +1/+1 counter on this creature, then put the card back or put it into your graveyard.)"
    power = 1
    toughness = 1

    keywords(Keyword.DOUBLE_STRIKE)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.Explore(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "19"
        artist = "Arash Radkia"
        imageUri = "https://cards.scryfall.io/normal/front/f/c/fc4c527c-6963-47f4-bcad-841d06bb2211.jpg?1782694597"
    }
}
