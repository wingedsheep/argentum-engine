package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Halo Scarab
 * {2}
 * Artifact Creature — Insect
 * 2 / 1
 * {2}, Exile this card from your graveyard: Create a Treasure token.
 */
val HaloScarab = card("Halo Scarab") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Insect"
    oracleText = "{2}, Exile this card from your graveyard: Create a Treasure token. (It's an artifact with \"{T}, Sacrifice this token: Add one mana of any color.\")"
    power = 2
    toughness = 1

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.ExileSelf)
        effect = Effects.CreateTreasure()
        activateFromZone = Zone.GRAVEYARD
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "239"
        artist = "Khurrum"
        flavorText = "It pulses with the colors that best complement the wearer's outfit."
        imageUri = "https://cards.scryfall.io/normal/front/2/e/2eead159-b9a1-4c64-affe-c77b0a9e4775.jpg"
    }
}
