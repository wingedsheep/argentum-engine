package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Atarka Pummeler
 * {4}{R}
 * Creature — Ogre Warrior
 * 4/5
 *
 * Formidable — {3}{R}{R}: Creatures you control gain menace until end of turn. Activate only if
 * creatures you control have total power 8 or greater.
 *
 * "Formidable" is an ability word; the activation restriction is a total-power check via
 * [Conditions.CompareAmounts] over `sumPower()`, checked only on activation — once activated, later
 * changes to total power don't matter. The affected group is locked in as the ability resolves, so
 * creatures that come under your control afterwards don't gain menace.
 */
val AtarkaPummeler = card("Atarka Pummeler") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Ogre Warrior"
    oracleText = "Formidable — {3}{R}{R}: Creatures you control gain menace until end of turn. " +
        "Activate only if creatures you control have total power 8 or greater. " +
        "(They can't be blocked except by two or more creatures.)"
    power = 4
    toughness = 5

    activatedAbility {
        cost = Costs.Mana("{3}{R}{R}")
        effect = Patterns.Group.grantKeywordToAll(Keyword.MENACE, Filters.Group.creaturesYouControl)
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
        collectorNumber = "129"
        artist = "Lucas Graciano"
        imageUri = "https://cards.scryfall.io/normal/front/d/5/d55f2528-f37f-46b9-b95f-4d142f656723.jpg?1783938592"
        ruling("2015-02-25", "Activating Atarka Pummeler's ability more than once conveys no additional benefit to creatures you control.")
        ruling("2015-02-25", "If you control a creature with power less than 0, use its actual power when calculating the total power of creatures you control. For example, if you control three creatures with powers 4, 5, and -2, the total power of creatures you control is 7.")
        ruling("2015-02-25", "Some formidable abilities are activated abilities that require creatures you control to have total power 8 or greater. Once you activate these abilities, it doesn't matter what happens to the total power of creatures you control.")
    }
}
