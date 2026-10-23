package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Elemental Masterpiece — Strixhaven: School of Mages #182 (canonical printing)
 * {5}{U}{R} · Sorcery
 *
 * Create two 4/4 blue and red Elemental creature tokens.
 * {U/R}{U/R}, Discard this card: Create a Treasure token.
 *
 * Elemental Summoning's token twice, plus the Creative Outburst from-hand Treasure ability
 * (`activateFromZone = Zone.HAND`, discard paid as [Costs.DiscardSelf]).
 */
val ElementalMasterpiece = card("Elemental Masterpiece") {
    manaCost = "{5}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Sorcery"
    oracleText = "Create two 4/4 blue and red Elemental creature tokens.\n" +
        "{U/R}{U/R}, Discard this card: Create a Treasure token. (It's an artifact with " +
        "\"{T}, Sacrifice this token: Add one mana of any color.\")"

    spell {
        effect = Effects.CreateToken(
            power = 4,
            toughness = 4,
            colors = setOf(Color.BLUE, Color.RED),
            creatureTypes = setOf("Elemental"),
            count = 2
        )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{U/R}{U/R}"), Costs.DiscardSelf)
        activateFromZone = Zone.HAND
        effect = Effects.CreateTreasure()
        description = "{U/R}{U/R}, Discard this card: Create a Treasure token."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "182"
        artist = "Marta Nael"
        flavorText = "Rootha passed with flying, living colors."
        imageUri = "https://cards.scryfall.io/normal/front/4/3/4331a7a6-e98e-4517-b0f9-ace379426a94.jpg?1783927319"
    }
}
