package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Grim Bounty
 * {2}{B}{B}
 * Sorcery
 * Destroy target creature or planeswalker. Create a Treasure token.
 */
val GrimBounty = card("Grim Bounty") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Destroy target creature or planeswalker. Create a Treasure token. (It's an artifact with \"{T}, Sacrifice this token: Add one mana of any color.\")"

    spell {
        val t = target(Targets.CreatureOrPlaneswalker)
        effect = Effects.Destroy(t) then Effects.CreateTreasure()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "106"
        artist = "Justine Cruz"
        flavorText = "Freed from the need to pay tribute to the dragon, the villages gladly paid the dragon-slayers instead."
        imageUri = "https://cards.scryfall.io/normal/front/b/9/b98e0ab1-dea8-492b-a712-2057f2b1d020.jpg?1783926494"
        ruling("2021-07-23", "If the target is not legal as Grim Bounty tries to resolve, perhaps because the creature or planeswalker is no longer on the battlefield, it will do nothing. You will not create a Treasure token.")
    }
}
