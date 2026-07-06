package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.model.Rarity

/**
 * River Herald Scout — {1}{U}
 * Creature — Merfolk Scout
 * 1/2
 * When this creature enters, it explores.
 */
val RiverHeraldScout = card("River Herald Scout") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Merfolk Scout"
    oracleText = "When this creature enters, it explores. (Reveal the top card of your library. Put that card into your hand if it's a land. Otherwise, put a +1/+1 counter on this creature, then put the card back or put it into your graveyard.)"
    power = 1
    toughness = 2

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.Explore(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "72"
        artist = "Josu Hernaiz"
        flavorText = "\"Don't worry, little swimmers. I'm here for the ruins, not you.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/a/8ae8ab66-5840-4b2e-bd4e-94ead0c79b61.jpg?1782694553"
    }
}
