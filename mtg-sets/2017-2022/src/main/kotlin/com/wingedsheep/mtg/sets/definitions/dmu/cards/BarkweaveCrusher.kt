package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Enlist

val BarkweaveCrusher = card("Barkweave Crusher") {
    manaCost = "{3}{G}"
    typeLine = "Creature — Elemental Warrior"
    power = 2
    toughness = 5
    oracleText = "Enlist (As this creature attacks, you may tap a nonattacking creature you control without summoning sickness. When you do, add its power to this creature's until end of turn.)"

    staticAbility { ability = Enlist }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "154"
        artist = "Kev Walker"
        imageUri = "https://cards.scryfall.io/normal/front/4/1/41c62c55-7b04-4987-9047-1322c842bb59.jpg?1783921307"
        flavorText = "\"Phyrexia may be ancient, but Gaea is eternal.\"\n—Queen Allenal of Ruadach"
    }
}
