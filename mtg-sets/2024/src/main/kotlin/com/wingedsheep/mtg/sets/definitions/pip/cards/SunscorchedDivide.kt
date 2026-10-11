package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Sunscorched Divide
 * Land
 * {1}, {T}: Add {R}{W}.
 */
val SunscorchedDivide = card("Sunscorched Divide") {
    manaCost = ""
    colorIdentity = "RW"
    typeLine = "Land"
    oracleText = "{1}, {T}: Add {R}{W}."
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap)
        effect = Effects.AddMana(Color.RED, 1) then Effects.AddMana(Color.WHITE, 1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "153"
        artist = "Zezhou Chen"
        flavorText = "\"To the Divide they came. In the Divide, they rest.\"\n—Ulysses"
        imageUri = "https://cards.scryfall.io/normal/front/f/8/f84a8f54-9ede-40ee-a783-e58a7e4f2e94.jpg?1783912372"
    }
}
