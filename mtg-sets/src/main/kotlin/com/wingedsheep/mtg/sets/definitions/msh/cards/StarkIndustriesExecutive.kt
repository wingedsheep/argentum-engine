package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Stark Industries Executive (MSH #153) — {R} Creature — Human Advisor, 1/2
 *
 * {2}, {T}: Create a Treasure token.
 *
 * A single activated ability with a composite mana + tap cost; the Treasure token itself is
 * the predefined [Effects.CreateTreasure] token (its own sac-for-any-color mana ability comes
 * from the engine's Treasure definition, so no token script is authored here).
 */
val StarkIndustriesExecutive = card("Stark Industries Executive") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Advisor"
    power = 1
    toughness = 2
    oracleText = "{2}, {T}: Create a Treasure token. (It's an artifact with \"{T}, Sacrifice this " +
        "token: Add one mana of any color.\")"

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap)
        effect = Effects.CreateTreasure()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "153"
        artist = "Xabi Gaztelua"
        flavorText = "\"Tony's ideas are . . . expensive. Someone has to make sure we can put them " +
            "into practice.\"\n—Pepper Potts"
        imageUri = "https://cards.scryfall.io/normal/front/7/0/709ffd07-706c-471f-b74d-4637afa11686.jpg?1783902924"
    }
}
