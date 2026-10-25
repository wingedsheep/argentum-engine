package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Skullport Merchant
 * {2}{B}
 * Creature — Dwarf Citizen
 * 1/4
 * When this creature enters, create a Treasure token.
 * {1}{B}, Sacrifice another creature or a Treasure: Draw a card.
 */
val SkullportMerchant = card("Skullport Merchant") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Dwarf Citizen"
    oracleText = "When this creature enters, create a Treasure token. (It's an artifact with " +
        "\"{T}, Sacrifice this token: Add one mana of any color.\")\n" +
        "{1}{B}, Sacrifice another creature or a Treasure: Draw a card."
    power = 1
    toughness = 4

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateTreasure()
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}{B}"),
            Costs.SacrificeAnother(GameObjectFilter.Creature or GameObjectFilter.Artifact.withSubtype("Treasure")),
        )
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "120"
        artist = "Aaron Miller"
        flavorText = "\"Buy something or get out.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/b/2bbb8865-dc37-4d56-9a2d-a92f7462c338.jpg?1783926487"
    }
}
