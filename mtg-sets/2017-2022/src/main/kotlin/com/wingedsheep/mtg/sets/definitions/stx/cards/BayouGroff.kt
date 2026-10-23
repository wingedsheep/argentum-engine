package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Bayou Groff — Strixhaven: School of Mages #121 (canonical printing)
 * {1}{G} · Creature — Plant Dog · 5/4
 *
 * As an additional cost to cast this spell, sacrifice a creature or pay {3}.
 *
 * Morkrut Behemoth's shape ([Costs.additional.SacrificeOrPay]); with no creature to sacrifice
 * only the pay path is offered.
 */
val BayouGroff = card("Bayou Groff") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Plant Dog"
    oracleText = "As an additional cost to cast this spell, sacrifice a creature or pay {3}."
    power = 5
    toughness = 4

    additionalCost(
        Costs.additional.SacrificeOrPay(
            filter = GameObjectFilter.Creature,
            alternativeManaCost = "{3}",
        )
    )

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "121"
        artist = "Ilse Gort"
        flavorText = "\"Groffs are great for tracking pests through the muck, but securing the pests before the groff devours them is a dicey endeavor.\"\n—Lisette, Witherbloom dean"
        imageUri = "https://cards.scryfall.io/normal/front/4/a/4a82b032-b1ba-456c-abab-c0f7430b0587.jpg?1783927348"
        ruling("4/16/2021", "You must sacrifice exactly one creature or pay an extra {3} to cast this spell; you can't cast it without paying one of those costs, and you can't pay either multiple times.")
    }
}
