package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Ghoulcaller's Accomplice (Shadows over Innistrad #112)
 * {1}{B}
 * Creature — Human Rogue
 * 2/2
 *
 * {3}{B}, Exile this card from your graveyard: Create a 2/2 black Zombie creature token.
 * Activate only as a sorcery.
 */
val GhoulcallersAccomplice = card("Ghoulcaller's Accomplice") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Rogue"
    power = 2
    toughness = 2
    oracleText = "{3}{B}, Exile this card from your graveyard: Create a 2/2 black Zombie creature token. " +
        "Activate only as a sorcery."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}{B}"), Costs.ExileSelf)
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Zombie")
        )
        timing = TimingRule.SorcerySpeed
        activateFromZone = Zone.GRAVEYARD
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "112"
        artist = "Dave Kendall"
        flavorText = "\"Once in the box, we're all the same.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/f/6f48828d-5f97-434f-a3b8-9e8c75cc8932.jpg"
    }
}
