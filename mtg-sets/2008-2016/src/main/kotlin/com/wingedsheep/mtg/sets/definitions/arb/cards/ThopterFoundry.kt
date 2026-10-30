package com.wingedsheep.mtg.sets.definitions.arb.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Thopter Foundry — Alara Reborn #133
 * {W/B}{U} · Artifact
 *
 * {1}, Sacrifice a nontoken artifact: Create a 1/1 blue Thopter artifact creature token with
 * flying. You gain 1 life.
 *
 * The plain `Costs.Sacrifice` pool includes the source, so the Foundry can sacrifice itself to pay
 * for its own ability (ruling).
 */
val ThopterFoundry = card("Thopter Foundry") {
    manaCost = "{W/B}{U}"
    colorIdentity = "WUB"
    typeLine = "Artifact"
    oracleText = "{1}, Sacrifice a nontoken artifact: Create a 1/1 blue Thopter artifact creature " +
        "token with flying. You gain 1 life."

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}"),
            Costs.Sacrifice(GameObjectFilter.Artifact.nontoken())
        )
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.BLUE),
            creatureTypes = setOf("Thopter"),
            keywords = setOf(Keyword.FLYING),
            artifactToken = true,
            imageUri = "https://cards.scryfall.io/normal/front/e/e/eef8b4fc-238f-4c1f-ad98-a1769fd44eab.jpg?1783930233",
        ) then Effects.GainLife(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "133"
        artist = "Ralph Horsley"
        flavorText = "\"Etherium is limited. Innovation is not.\"\n—Tezzeret"
        imageUri = "https://cards.scryfall.io/normal/front/4/2/42b8d797-b01d-49cf-9818-d84bba17029d.jpg?1783942412"
        ruling("2020-08-07", "You may sacrifice Thopter Foundry to pay the cost of its own ability.")
    }
}
