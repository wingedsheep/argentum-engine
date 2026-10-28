package com.wingedsheep.mtg.sets.definitions.sth.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersOnlyIfPaid
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Mox Diamond
 * {0}
 * Artifact
 * If this artifact would enter, you may discard a land card instead. If you do, put this artifact
 * onto the battlefield. If you don't, put it into its owner's graveyard.
 * {T}: Add one mana of any color.
 */
val MoxDiamond = card("Mox Diamond") {
    manaCost = "{0}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "If this artifact would enter, you may discard a land card instead. If you do, put this artifact onto the battlefield. If you don't, put it into its owner's graveyard.\n{T}: Add one mana of any color."

    replacementEffect(EntersOnlyIfPaid(Costs.pay.Discard(GameObjectFilter.Land)))

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddAnyColorMana()
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "138"
        artist = "Dan Frazier"
        imageUri = "https://cards.scryfall.io/normal/front/2/8/28028830-83ed-45e2-b495-3b9ad9d3e988.jpg?1783946538"
        ruling("2008-05-01", "If you don't discard a land card, Mox Diamond never enters. It won't trigger abilities that look for something entering, and you won't get the opportunity to tap it for mana.")
    }
}
