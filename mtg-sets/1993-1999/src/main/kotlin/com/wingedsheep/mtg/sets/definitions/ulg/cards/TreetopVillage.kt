package com.wingedsheep.mtg.sets.definitions.ulg.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.EffectTarget

val TreetopVillage = card("Treetop Village") {
    typeLine = "Land"
    colorIdentity = "G"
    oracleText = "This land enters tapped.\n{T}: Add {G}.\n{1}{G}: This land becomes a 3/3 green Ape creature with trample until end of turn. It's still a land. (It can deal excess combat damage to the player or planeswalker it's attacking.)"

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.GREEN)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Mana("{1}{G}")
        effect = Effects.BecomeCreature(
            target = EffectTarget.Self,
            power = 3,
            toughness = 3,
            creatureTypes = setOf("Ape"),
            colors = setOf("GREEN"),
            keywords = setOf(Keyword.TRAMPLE),
            duration = Duration.EndOfTurn,
        )
        description = "{1}{G}: This land becomes a 3/3 green Ape creature with trample until end of turn. It's still a land."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "143"
        artist = "Anthony S. Waters"
        imageUri = "https://cards.scryfall.io/normal/front/0/2/02212bd8-0c0f-4e8e-99f1-a8477476c03a.jpg?1783946217"
        ruling("2009-10-01", "Activating the ability that turns it into a creature while it's already a creature will override any effects that set its power and/or toughness to a specific number. However, any effect that raises or lowers power and/or toughness (such as the effect created by Giant Growth, Glorious Anthem, or a +1/+1 counter) will continue to apply.")
        ruling("2008-08-01", "A noncreature permanent that turns into a creature can attack, and its {T} abilities can be activated, only if its controller has continuously controlled that permanent since the beginning of their most recent turn. It doesn't matter how long the permanent has been a creature.")
    }
}
