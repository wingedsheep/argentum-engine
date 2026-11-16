package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Stampeding Elk Herd
 * {3}{G}{G}
 * Creature — Elk
 * 5/5
 *
 * Formidable — Whenever this creature attacks, if creatures you control have total power 8 or
 * greater, creatures you control gain trample until end of turn.
 *
 * The formidable clause is an intervening-if (CR 603.4), the same total-power check as Sabertooth
 * Outrider; the payoff is Ondu Champion's team-wide trample grant.
 */
val StampedingElkHerd = card("Stampeding Elk Herd") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elk"
    oracleText = "Formidable — Whenever this creature attacks, if creatures you control have total power 8 " +
        "or greater, creatures you control gain trample until end of turn."
    power = 5
    toughness = 5

    triggeredAbility {
        trigger = Triggers.self.attacks()
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature).sumPower(),
            ComparisonOperator.GTE,
            8
        )
        effect = Patterns.Group.grantKeywordToAll(
            Keyword.TRAMPLE,
            Filters.Group.creaturesYouControl,
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "208"
        artist = "Carl Frank"
        flavorText = "The Atarka use them not just for food, but also to clear away snow, trees, and enemy forces."
        imageUri = "https://cards.scryfall.io/normal/front/7/e/7e55e213-6eab-4086-96cd-024de6150fbe.jpg?1783938575"
        ruling("2015-02-25", "If you control a creature with power less than 0, use its actual power when calculating the total power of creatures you control. For example, if you control three creatures with powers 4, 5, and -2, the total power of creatures you control is 7.")
        ruling("2015-02-25", "Other formidable abilities are triggered abilities with an \"intervening 'if'\" clause. Such abilities check the total power of creatures you control twice: once at the appropriate time to see if the ability will trigger, and again as the ability tries to resolve. If, at that time, the total power of creatures you control is no longer 8 or greater, the ability will have no effect.")
    }
}
