package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Viridescent Bog
 * Land
 * {1}, {T}: Add {B}{G}.
 */
val ViridescentBog = card("Viridescent Bog") {
    manaCost = ""
    colorIdentity = "BG"
    typeLine = "Land"
    oracleText = "{1}, {T}: Add {B}{G}."
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap)
        effect = Effects.AddMana(Color.BLACK, 1) then Effects.AddMana(Color.GREEN, 1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "154"
        artist = "Rafater"
        flavorText = "The Master had literally built his army one mutant at a time. Humans, preferably with little radiation damage, were captured and sent to the vats."
        imageUri = "https://cards.scryfall.io/normal/front/e/a/ea4ce16b-39bb-4580-9c38-a7c6b9a33609.jpg?1783912372"
    }
}
