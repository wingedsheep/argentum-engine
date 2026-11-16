package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Dragon Whisperer
 * {R}{R}
 * Creature — Human Shaman
 * 2/2
 *
 * {R}: This creature gains flying until end of turn.
 * {1}{R}: This creature gets +1/+0 until end of turn.
 * Formidable — {4}{R}{R}: Create a 4/4 red Dragon creature token with flying. Activate only if
 * creatures you control have total power 8 or greater.
 *
 * The formidable restriction is the same total-power check as Atarka Pummeler, checked only on
 * activation — once activated, later changes to total power don't matter.
 */
val DragonWhisperer = card("Dragon Whisperer") {
    manaCost = "{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Shaman"
    oracleText = "{R}: This creature gains flying until end of turn.\n" +
        "{1}{R}: This creature gets +1/+0 until end of turn.\n" +
        "Formidable — {4}{R}{R}: Create a 4/4 red Dragon creature token with flying. " +
        "Activate only if creatures you control have total power 8 or greater."
    power = 2
    toughness = 2

    activatedAbility {
        cost = Costs.Mana("{R}")
        effect = Effects.GrantKeyword(Keyword.FLYING, EffectTarget.Self)
    }

    activatedAbility {
        cost = Costs.Mana("{1}{R}")
        effect = Effects.ModifyStats(1, 0, EffectTarget.Self)
    }

    activatedAbility {
        cost = Costs.Mana("{4}{R}{R}")
        effect = Effects.CreateToken(
            power = 4,
            toughness = 4,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Dragon"),
            keywords = setOf(Keyword.FLYING),
            imageUri = "https://cards.scryfall.io/normal/front/1/e/1ee0725f-8562-49c2-83fa-11a702e5c78d.jpg?1783938561"
        )
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
        rarity = Rarity.MYTHIC
        collectorNumber = "137"
        artist = "Chris Rallis"
        imageUri = "https://cards.scryfall.io/normal/front/5/c/5c75b106-349f-44e1-a737-079955cc91b2.jpg?1783938590"
        ruling("2015-02-25", "If you control a creature with power less than 0, use its actual power when calculating the total power of creatures you control. For example, if you control three creatures with powers 4, 5, and -2, the total power of creatures you control is 7.")
        ruling("2015-02-25", "Some formidable abilities are activated abilities that require creatures you control to have total power 8 or greater. Once you activate these abilities, it doesn't matter what happens to the total power of creatures you control.")
    }
}
