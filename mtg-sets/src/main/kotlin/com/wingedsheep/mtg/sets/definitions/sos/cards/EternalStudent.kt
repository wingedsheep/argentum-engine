package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Eternal Student
 * {3}{B}
 * Creature — Zombie Warlock
 * 4/2
 *
 * {1}{B}, Exile this card from your graveyard: Create two 1/1 white and black
 * Inkling creature tokens with flying.
 */
val EternalStudent = card("Eternal Student") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie Warlock"
    power = 4
    toughness = 2
    oracleText = "{1}{B}, Exile this card from your graveyard: Create two 1/1 white and black Inkling creature tokens with flying."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{B}"), Costs.ExileSelf)
        activateFromZone = Zone.GRAVEYARD
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE, Color.BLACK),
            creatureTypes = setOf("Inkling"),
            keywords = setOf(Keyword.FLYING),
            count = 2,
            imageUri = "https://cards.scryfall.io/display/front/b/a/bab52920-9d67-4cd4-9015-6e645ff9764f.webp?1782723480"
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "82"
        artist = "Ksenia Kim"
        flavorText = "She was entering her fiftieth year of graduate study with no signs of stopping."
        imageUri = "https://cards.scryfall.io/normal/front/f/9/f97fac69-3e77-4150-9702-cc726daa6d21.jpg?1775937483"
    }
}
