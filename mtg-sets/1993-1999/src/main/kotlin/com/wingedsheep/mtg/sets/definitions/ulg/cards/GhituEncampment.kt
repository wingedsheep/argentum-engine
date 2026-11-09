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

val GhituEncampment = card("Ghitu Encampment") {
    typeLine = "Land"
    colorIdentity = "R"
    oracleText = "This land enters tapped.\n{T}: Add {R}.\n{1}{R}: This land becomes a 2/1 red Warrior creature with first strike until end of turn. It's still a land. (It deals combat damage before creatures without first strike.)"

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.RED)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Mana("{1}{R}")
        effect = Effects.BecomeCreature(
            target = EffectTarget.Self,
            power = 2,
            toughness = 1,
            creatureTypes = setOf("Warrior"),
            colors = setOf("RED"),
            keywords = setOf(Keyword.FIRST_STRIKE),
            duration = Duration.EndOfTurn,
        )
        description = "{1}{R}: This land becomes a 2/1 red Warrior creature with first strike until end of turn. It's still a land."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "141"
        artist = "Don Hazeltine"
        imageUri = "https://cards.scryfall.io/normal/front/b/f/bf09ecef-1e30-4206-9648-8fe5c8a71c71.jpg?1783946217"
        ruling("2009-10-01", "Activating the ability that turns it into a creature while it's already a creature will override any effects that set its power and/or toughness to a specific number. However, any effect that raises or lowers power and/or toughness (such as the effect created by Giant Growth, Glorious Anthem, or a +1/+1 counter) will continue to apply.")
        ruling("2008-08-01", "A noncreature permanent that turns into a creature can attack, and its {T} abilities can be activated, only if its controller has continuously controlled that permanent since the beginning of their most recent turn. It doesn't matter how long the permanent has been a creature.")
    }
}
