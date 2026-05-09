package com.wingedsheep.mtg.sets.definitions.fdn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.EffectTarget

val SoulstoneSanctuary = card("Soulstone Sanctuary") {
    manaCost = ""
    typeLine = "Land"
    oracleText = "{T}: Add {C}.\n{4}: This land becomes a 3/3 creature with vigilance and all creature types. It's still a land."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Mana("{4}")
        effect = Effects.BecomeCreature(
            target = EffectTarget.Self,
            power = 3,
            toughness = 3,
            keywords = setOf(Keyword.VIGILANCE, Keyword.CHANGELING),
            duration = Duration.Permanent
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "133"
        artist = "Daniel Ljunggren"
        flavorText = "By tradition, travelers touched the pillar for good luck. One day, it decided to travel as well."
        imageUri = "https://cards.scryfall.io/normal/front/6/4/642553a7-6d0f-483d-a873-3a703786db42.jpg?1730489100"
    }
}
