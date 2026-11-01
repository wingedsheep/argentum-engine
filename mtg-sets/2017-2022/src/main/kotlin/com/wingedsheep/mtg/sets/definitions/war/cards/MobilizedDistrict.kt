package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mobilized District — War of the Spark #249 (canonical printing)
 * Land · Rare
 *
 * {T}: Add {C}.
 * {4}: This land becomes a 3/3 Citizen creature with vigilance until end of turn. It's still a land.
 * This ability costs {1} less to activate for each legendary creature and planeswalker you control.
 *
 * The reduction counts legendary permanents that are creatures and/or planeswalkers — one filter
 * (`CreatureOrPlaneswalker.legendary()`), so a legendary planeswalker creature counts once, as the
 * ruling requires — through the activated-ability `genericCostReduction` the NEO channel lands use.
 */
val MobilizedDistrict = card("Mobilized District") {
    typeLine = "Land"
    colorIdentity = ""
    oracleText = "{T}: Add {C}.\n" +
        "{4}: This land becomes a 3/3 Citizen creature with vigilance until end of turn. " +
        "It's still a land. This ability costs {1} less to activate for each legendary creature " +
        "and planeswalker you control."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Mana("{4}")
        genericCostReduction = DynamicAmounts.battlefield(
            Player.You,
            GameObjectFilter.CreatureOrPlaneswalker.legendary(),
        ).count()
        effect = Effects.BecomeCreature(
            target = EffectTarget.Self,
            power = 3,
            toughness = 3,
            keywords = setOf(Keyword.VIGILANCE),
            creatureTypes = setOf(Subtype.CITIZEN.value),
            duration = Duration.EndOfTurn,
        )
        description = "{4}: This land becomes a 3/3 Citizen creature with vigilance until end of turn. " +
            "It's still a land. This ability costs {1} less to activate for each legendary creature " +
            "and planeswalker you control."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "249"
        artist = "Jedd Chevrier"
        flavorText = "Sometimes the strongest weapon you can hold in your hand is another hand."
        imageUri = "https://cards.scryfall.io/normal/front/2/1/214792e9-a172-4547-b148-b9b368cb3432.jpg?1783933367"
        ruling(
            "2019-05-03",
            "If you control four or more legendary permanents that are creatures and/or planeswalkers, " +
                "the last ability of Mobilized District costs no mana to activate. A legendary " +
                "planeswalker that's also a creature reduces this cost by only {1}.",
        )
        ruling(
            "2019-05-03",
            "If Mobilized District becomes a creature the same turn it enters the battlefield, you can't " +
                "attack with it or activate its mana ability.",
        )
    }
}
