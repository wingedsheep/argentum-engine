package com.wingedsheep.mtg.sets.definitions.clb.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Patriar's Seal
 * {3}
 * Artifact
 * {T}: Add one mana of any color.
 * {1}, {T}: Untap target legendary creature you control.
 */
val PatriarsSeal = card("Patriar's Seal") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{T}: Add one mana of any color.\n" +
        "{1}, {T}: Untap target legendary creature you control."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddAnyColorMana()
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap)
        val t = target(TargetFilter.CreatureYouControl.legendary())
        effect = Effects.Untap(t)
        description = "{1}, {T}: Untap target legendary creature you control."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "332"
        artist = "Kamila Szutenberg"
        flavorText = "Only a fool ignores a letter bearing the official mark of a patriar family."
        imageUri = "https://cards.scryfall.io/normal/front/3/7/37f920f0-4dfc-477b-af7e-a17dfc9ba455.jpg?1783922667"
    }
}
