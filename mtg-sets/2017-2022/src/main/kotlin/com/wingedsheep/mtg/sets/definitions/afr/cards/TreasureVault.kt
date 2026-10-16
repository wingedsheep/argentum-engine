package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Treasure Vault
 * Artifact Land
 *
 * {T}: Add {C}.
 * {X}{X}, {T}, Sacrifice this land: Create X Treasure tokens.
 *
 * The `{X}{X}` cost announces one X and pays it twice (`ManaCost.xCount`), so the ability costs 2X
 * mana and makes X Treasures — X is read off the ability on the stack, so sacrificing the land as a
 * cost doesn't lose it.
 */
val TreasureVault = card("Treasure Vault") {
    manaCost = ""
    colorIdentity = ""
    typeLine = "Artifact Land"
    oracleText = "{T}: Add {C}.\n" +
        "{X}{X}, {T}, Sacrifice this land: Create X Treasure tokens."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{X}{X}"), Costs.Tap, Costs.SacrificeSelf)
        effect = Effects.CreateTreasure(DynamicAmounts.xValue())
        description = "{X}{X}, {T}, Sacrifice this land: Create X Treasure tokens."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "261"
        artist = "Johannes Voss"
        flavorText = "There is wealth enough inside to cause the rise and the ruin of dynasties."
        imageUri = "https://cards.scryfall.io/normal/front/a/0/a0931eb3-b403-4b1a-ad46-a7b0a51bb9a4.jpg?1783926431"
    }
}
