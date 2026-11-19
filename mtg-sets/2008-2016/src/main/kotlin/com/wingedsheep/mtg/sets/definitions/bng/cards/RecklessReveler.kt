package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Reckless Reveler
 * {1}{R}
 * Creature — Satyr
 * 2/1
 *
 * {R}, Sacrifice this creature: Destroy target artifact.
 *
 * Canonical printing: Born of the Gods, the card's earliest real printing.
 */
val RecklessReveler = card("Reckless Reveler") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Satyr"
    power = 2
    toughness = 1
    oracleText = "{R}, Sacrifice this creature: Destroy target artifact."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{R}"), Costs.SacrificeSelf)
        val artifact = target(TargetFilter.Artifact)
        effect = Effects.Destroy(artifact)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "106"
        artist = "Mike Sass"
        flavorText = "\"The gods of Theros are born of the expectations and beliefs of mortals. If I have " +
            "found godhood, what does that say about their true desires?\"\n—Xenagos, god of revels"
        imageUri = "https://cards.scryfall.io/normal/front/1/8/188adefd-a808-4991-80be-53249c24df8f.jpg?1783939542"
    }
}
