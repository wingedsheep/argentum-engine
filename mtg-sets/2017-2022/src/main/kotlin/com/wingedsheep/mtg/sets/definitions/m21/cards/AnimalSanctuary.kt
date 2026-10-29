package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Animal Sanctuary — Core Set 2021 #242
 * Land
 * {T}: Add {C}.
 * {2}, {T}: Put a +1/+1 counter on target Bird, Cat, Dog, Goat, Ox, or Snake.
 */
val AnimalSanctuary = card("Animal Sanctuary") {
    manaCost = ""
    colorIdentity = ""
    typeLine = "Land"
    oracleText = "{T}: Add {C}.\n{2}, {T}: Put a +1/+1 counter on target Bird, Cat, Dog, Goat, Ox, or Snake."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap)
        val t = target(
            TargetFilter(GameObjectFilter.Permanent.withAnySubtype("Bird", "Cat", "Dog", "Goat", "Ox", "Snake"))
        )
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, t)
        description = "{2}, {T}: Put a +1/+1 counter on target Bird, Cat, Dog, Goat, Ox, or Snake."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "242"
        artist = "Alayna Danner"
        flavorText = "\"No owners allowed.\"\n—Sanctuary sign"
        imageUri = "https://cards.scryfall.io/normal/front/f/8/f8d7a2c7-666d-4fc6-bac8-ef8eb66e355d.jpg?1783930653"
    }
}
