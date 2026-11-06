package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Metallic Rebuke
 * {2}{U}
 * Instant
 * Improvise (Your artifacts can help cast this spell. Each artifact you tap after you're done activating mana abilities pays for {1}.)
 * Counter target spell unless its controller pays {3}.
 */
val MetallicRebuke = card("Metallic Rebuke") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Improvise (Your artifacts can help cast this spell. Each artifact you tap after you're done activating mana abilities pays for {1}.)\n" +
        "Counter target spell unless its controller pays {3}."

    keywords(Keyword.IMPROVISE)

    spell {
        target(TargetFilter.SpellOnStack)
        effect = Effects.CounterUnlessPays("{3}")
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "39"
        artist = "Eric Deschamps"
        flavorText = "\"Too little, too late. The machine is in motion.\"\n—Tezzeret"
        imageUri = "https://cards.scryfall.io/normal/front/f/7/f712ac26-dca4-459b-84c1-010597007f60.jpg"
        ruling("2020-08-07", "When calculating a spell's total cost, include any alternative costs, additional costs, or anything else that increases or reduces the cost to cast the spell. Improvise applies after the total cost is calculated.")
        ruling("2020-08-07", "Improvise doesn't change a spell's mana cost or mana value.")
        ruling("2020-08-07", "Improvise can't pay for {U} in Metallic Rebuke's total cost.")
        ruling("2020-08-07", "If an artifact you control has a mana ability with {T} in the cost, activating that ability while casting a spell with improvise will result in the artifact being tapped when you pay the spell's costs. You won't be able to tap it again for improvise. Similarly, if you sacrifice an artifact to activate a mana ability while casting a spell with improvise, that artifact won't be on the battlefield when you pay the spell's costs, so you won't be able to tap it for improvise.")
        ruling("2020-08-07", "Tapping an artifact won't cause its abilities to stop applying unless those abilities say so.")
        ruling("2020-08-07", "Equipment attached to a creature doesn't become tapped when that creature becomes tapped, and tapping that Equipment doesn't cause the creature to become tapped.")
    }
}
