package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.exploit
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.values.CardNumericProperty

/**
 * Profaner of the Dead
 * {3}{U}
 * Creature — Snake Wizard
 * 3/3
 * Exploit (When this creature enters, you may sacrifice a creature.)
 * When this creature exploits a creature, return to their owners' hands all creatures your
 * opponents control with toughness less than the exploited creature's toughness.
 *
 * The mass bounce rides the exploit reflexive, which inherits the sacrifice's last-known snapshot:
 * `sacrificedToughness()` reads the exploited creature's toughness as it last existed on the
 * battlefield (counters and pumps included), while each opposing creature's toughness is read live
 * when the gather runs. Exploiting the Profaner itself compares against its own last toughness.
 */
val ProfanerOfTheDead = card("Profaner of the Dead") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Snake Wizard"
    power = 3
    toughness = 3
    oracleText = "Exploit (When this creature enters, you may sacrifice a creature.)\n" +
        "When this creature exploits a creature, return to their owners' hands all creatures your " +
        "opponents control with toughness less than the exploited creature's toughness."

    exploit(
        onExploit = Patterns.Group.returnAllToHand(
            GroupFilter(
                GameObjectFilter.Creature.opponentControls().compareNumericProperty(
                    CardNumericProperty.TOUGHNESS,
                    ComparisonOperator.LT,
                    DynamicAmounts.sacrificedToughness(),
                )
            )
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "70"
        artist = "Vincent Proce"
        imageUri = "https://cards.scryfall.io/normal/front/8/7/87630910-47cc-4347-b126-fd779e7dadc0.jpg?1783938605"
        ruling("2015-02-25", "Use the toughness of the exploited creature as it last existed on the battlefield to determine which creatures to return to their owners' hands.")
        ruling("2015-02-25", "If the creature with exploit isn't on the battlefield as the exploit ability resolves, you won't get any bonus from the creature with exploit, even if you sacrifice a creature. Because the creature with exploit isn't on the battlefield, its other triggered ability won't trigger.")
    }
}
