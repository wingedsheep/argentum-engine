package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Tamiyo's Logbook
 * {2}{U}
 * Artifact — Book
 * {5}{U}, {T}: Draw a card. This ability costs {1} less to activate for each other artifact you control.
 *
 * The reduction is a battlefield count of artifacts you control with `excludeSelf = true` for
 * "each *other* artifact", riding `genericCostReduction` — only the {5} shrinks; the {U} stays.
 */
val TamiyosLogbook = card("Tamiyo's Logbook") {
    manaCost = "{2}{U}"
    typeLine = "Artifact — Book"
    oracleText = "{5}{U}, {T}: Draw a card. This ability costs {1} less to activate for each other artifact you control."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{5}{U}"), Costs.Tap)
        effect = Effects.DrawCards(1)
        genericCostReduction =
            DynamicAmounts.battlefield(Player.You, GameObjectFilter.Artifact, excludeSelf = true).count()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "70"
        artist = "Piotr Dura"
        flavorText = "\"We are opening a new chapter in the one true story of Phyrexia. Someone must record it for posterity.\"\n—Tamiyo"
        imageUri = "https://cards.scryfall.io/normal/front/0/e/0edb2d01-9a94-410c-8ab7-ca1b404ab5f0.jpg?1783918058"
    }
}
