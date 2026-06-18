package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.ManaRestriction

/**
 * Great Hall of the Citadel
 * Land
 *
 * {T}: Add {C}.
 * {1}, {T}: Add two mana in any combination of colors. Spend this mana only to cast legendary spells.
 *
 * Gap 22 (restricted mana) was engine-landed except a "legendary spells only" variant — this set adds
 * `ManaRestriction.LegendarySpellsOnly`. The two-color mana composes from `AddManaInAnyCombination`.
 */
val GreatHallOfTheCitadel = card("Great Hall of the Citadel") {
    typeLine = "Land"
    oracleText = "{T}: Add {C}.\n" +
        "{1}, {T}: Add two mana in any combination of colors. Spend this mana only to cast legendary spells."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap)
        effect = Effects.AddManaInAnyCombination(2, restriction = ManaRestriction.LegendarySpellsOnly)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "254"
        artist = "Campbell White"
        flavorText = "\"Renewed shall be blade that was broken: The crownless again shall be king.\"\n—Bilbo"
        imageUri = "https://cards.scryfall.io/normal/front/2/1/219c7b57-b62b-42d1-85d9-4b57624a3f54.jpg?1686970335"
    }
}
