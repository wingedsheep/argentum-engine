package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantWard
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Hall of Storm Giants
 * Land
 * If you control two or more other lands, this land enters tapped.
 * {T}: Add {U}.
 * {5}{U}: Until end of turn, this land becomes a 7/7 blue Giant creature with ward {3}.
 * It's still a land.
 */
val HallOfStormGiants = card("Hall of Storm Giants") {
    typeLine = "Land"
    colorIdentity = "U"
    oracleText = "If you control two or more other lands, this land enters tapped.\n{T}: Add {U}.\n" +
        "{5}{U}: Until end of turn, this land becomes a 7/7 blue Giant creature with ward {3}. It's still a land. " +
        "(Whenever it becomes the target of a spell or ability an opponent controls, counter it unless that player pays {3}.)"

    replacementEffect(
        EntersTapped(unlessCondition = Conditions.Not(Conditions.YouControlOtherAtLeast(2, GameObjectFilter.Land)))
    )

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLUE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Mana("{5}{U}")
        effect = Effects.BecomeCreature(
            target = EffectTarget.Self,
            power = 7,
            toughness = 7,
            creatureTypes = setOf("Giant"),
            colors = setOf("BLUE"),
            duration = Duration.EndOfTurn,
        ) then Effects.GrantStaticAbility(
            GrantWard(WardCost.Mana("{3}"), Filters.Self),
            EffectTarget.Self,
            Duration.EndOfTurn,
        )
        description = "{5}{U}: Until end of turn, this land becomes a 7/7 blue Giant creature with ward {3}. It's still a land."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "257"
        artist = "Johannes Voss"
        imageUri = "https://cards.scryfall.io/normal/front/b/f/bf8f052d-8840-4905-a807-9a305f4fd8f7.jpg?1783926434"
    }
}
