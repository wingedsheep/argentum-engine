package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Lightning Javelin
 * {3}{R}
 * Sorcery
 * Lightning Javelin deals 3 damage to any target. Scry 1.
 */
val LightningJavelin = card("Lightning Javelin") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Lightning Javelin deals 3 damage to any target. Scry 1. (Look at the top card of your library. You may put that card on the bottom.)"

    spell {
        val t = target("target", Targets.Any)
        effect = Effects.Composite(
            Effects.DealDamage(3, t),
            Effects.Scry(1)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "153"
        artist = "Seb McKinnon"
        flavorText = "The harpies descended without mercy upon Akros, only to find their attack put them within range of the javelineers."
        imageUri = "https://cards.scryfall.io/normal/front/c/1/c1ccaeed-9670-4432-8a45-d5c06119fa9f.jpg?1783938329"
    }
}
