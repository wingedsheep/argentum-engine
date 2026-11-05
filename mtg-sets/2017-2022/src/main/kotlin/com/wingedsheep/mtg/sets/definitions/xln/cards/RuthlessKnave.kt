package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Ruthless Knave
 * {2}{B}
 * Creature — Orc Pirate
 * 3/2
 *
 * {2}{B}, Sacrifice a creature: Create two Treasure tokens.
 * Sacrifice three Treasures: Draw a card.
 */
val RuthlessKnave = card("Ruthless Knave") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Orc Pirate"
    oracleText = "{2}{B}, Sacrifice a creature: Create two Treasure tokens. (They're artifacts with " +
        "\"{T}, Sacrifice this token: Add one mana of any color.\")\nSacrifice three Treasures: Draw a card."
    power = 3
    toughness = 2

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{B}"), Costs.Sacrifice(GameObjectFilter.Creature))
        effect = Effects.CreateTreasure(2)
    }

    activatedAbility {
        cost = Costs.SacrificeMultiple(3, GameObjectFilter.Artifact.withSubtype(Subtype.TREASURE))
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "119"
        artist = "Aaron Miller"
        flavorText = "\"This scum is barely worth the trouble.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/c/1ce91e38-4601-4354-ad1b-2c5c1c70da17.jpg?1783935755"
    }
}
