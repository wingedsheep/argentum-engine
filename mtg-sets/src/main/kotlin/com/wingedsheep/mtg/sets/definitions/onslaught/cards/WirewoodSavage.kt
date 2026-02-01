package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.scripting.DrawCardsEffect
import com.wingedsheep.sdk.scripting.MayEffect
import com.wingedsheep.sdk.scripting.OnCreatureWithSubtypeEnters

/**
 * Wirewood Savage
 * {2}{G}
 * Creature — Elf
 * 2/2
 * Whenever a Beast enters the battlefield, you may draw a card.
 */
val WirewoodSavage = card("Wirewood Savage") {
    manaCost = "{2}{G}"
    typeLine = "Creature — Elf"
    power = 2
    toughness = 2

    triggeredAbility {
        trigger = OnCreatureWithSubtypeEnters(Subtype("Beast"))
        effect = MayEffect(DrawCardsEffect(1))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "296"
        artist = "Carl Critchlow"
        flavorText = "\"Only beasts know the one true law—everything is prey to something else.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/9/99982622-98bc-45ae-8642-41cd543f32a8.jpg"
    }
}
