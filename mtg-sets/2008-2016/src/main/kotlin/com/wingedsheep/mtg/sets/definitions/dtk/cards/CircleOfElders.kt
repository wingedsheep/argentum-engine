package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Circle of Elders
 * {2}{G}{G}
 * Creature — Human Shaman
 * 2/4
 *
 * Vigilance
 * Formidable — {T}: Add {C}{C}{C}. Activate only if creatures you control have total power 8 or
 * greater.
 *
 * A mana ability gated by an activation restriction (the Tainted Isle shape); the formidable check
 * is the same total-power comparison as Crater Elemental, checked only on activation. The mana
 * solver skips the ability while the restriction is unmet.
 */
val CircleOfElders = card("Circle of Elders") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Shaman"
    oracleText = "Vigilance\n" +
        "Formidable — {T}: Add {C}{C}{C}. Activate only if creatures you control have total power 8 or greater."
    power = 2
    toughness = 4

    keywords(Keyword.VIGILANCE)

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(3)
        manaAbility = true
        timing = TimingRule.ManaAbility
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(
                Conditions.CompareAmounts(
                    DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature).sumPower(),
                    ComparisonOperator.GTE,
                    8
                )
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "176"
        artist = "Jakub Kasper"
        flavorText = "They whisper of an ancient hero who saved the dragons of their world."
        imageUri = "https://cards.scryfall.io/normal/front/a/9/a99df522-cc97-41e3-8073-72e092afe8bb.jpg?1783938581"
        ruling("2015-02-25", "If you control a creature with power less than 0, use its actual power when calculating the total power of creatures you control. For example, if you control three creatures with powers 4, 5, and -2, the total power of creatures you control is 7.")
        ruling("2015-02-25", "Some formidable abilities are activated abilities that require creatures you control to have total power 8 or greater. Once you activate these abilities, it doesn't matter what happens to the total power of creatures you control.")
    }
}
