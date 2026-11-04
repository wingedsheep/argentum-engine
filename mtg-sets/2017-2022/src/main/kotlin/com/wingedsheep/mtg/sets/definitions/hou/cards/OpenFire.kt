package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

val OpenFire = card("Open Fire") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Open Fire deals 3 damage to any target."

    spell {
        val t = target(Targets.Any)
        effect = Effects.DealDamage(3, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "105"
        artist = "Jason Kang"
        flavorText = "Some khenra archers fashion their arrows with a personal touch."
        imageUri = "https://cards.scryfall.io/normal/front/4/4/448f9fb5-ffb5-4325-9f81-ce8782e5f9e9.jpg?1783936023"
    }
}
