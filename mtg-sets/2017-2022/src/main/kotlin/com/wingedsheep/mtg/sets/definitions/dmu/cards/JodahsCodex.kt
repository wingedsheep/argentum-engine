package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost

/**
 * Jodah's Codex
 * {5}
 * Artifact — Book
 * Domain — {5}, {T}: Draw a card. This ability costs {1} less to activate for each basic land
 * type among lands you control.
 *
 * The discount rides `genericCostReduction` (Battlefield Butcher's shape) fed by
 * `DynamicAmounts.domain()`, so only the generic {5} shrinks and it can't go below zero.
 */
val JodahsCodex = card("Jodah's Codex") {
    manaCost = "{5}"
    colorIdentity = ""
    typeLine = "Artifact — Book"
    oracleText = "Domain — {5}, {T}: Draw a card. This ability costs {1} less to activate for each basic land type among lands you control."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{5}"), AbilityCost.Tap)
        effect = Effects.DrawCards(1)
        genericCostReduction = DynamicAmounts.domain()
        description = "Domain — {5}, {T}: Draw a card. This ability costs {1} less to activate " +
            "for each basic land type among lands you control."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "233"
        artist = "Aaron Miller"
        flavorText = "\"If you remember nothing else of our world's history, remember this: Phyrexians can be defeated.\"\n—Jodah"
        imageUri = "https://cards.scryfall.io/normal/front/d/6/d636da1e-4cc6-40a1-bd83-cc0dd761b1cc.jpg?1783921267"
        ruling("2022-09-09", "Domain abilities count the number of basic land types among lands you control, not how many lands you control or how many of any type.")
        ruling("2022-09-09", "The basic land types are Plains, Island, Swamp, Mountain, and Forest. Land types other than basic land types (such as Desert) don't contribute to domain abilities.")
    }
}
