package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.CardNumericProperty

/**
 * Sigarda, Champion of Light (Innistrad: Midnight Hunt #240)
 * {1}{G}{W}{W}
 * Legendary Creature — Angel
 * 4/4
 * Flying, trample
 * Humans you control get +1/+1.
 * Coven — Whenever Sigarda attacks, if you control three or more creatures with different powers,
 * look at the top five cards of your library. You may reveal a Human creature card from among them
 * and put it into your hand. Put the rest on the bottom of your library in a random order.
 */
val SigardaChampionOfLight = card("Sigarda, Champion of Light") {
    manaCost = "{1}{G}{W}{W}"
    colorIdentity = "GW"
    typeLine = "Legendary Creature — Angel"
    oracleText = "Flying, trample\nHumans you control get +1/+1.\nCoven — Whenever Sigarda attacks, if you " +
        "control three or more creatures with different powers, look at the top five cards of your library. " +
        "You may reveal a Human creature card from among them and put it into your hand. Put the rest on the " +
        "bottom of your library in a random order."
    power = 4
    toughness = 4

    keywords(Keyword.FLYING, Keyword.TRAMPLE)

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(GameObjectFilter.Creature.withSubtype(Subtype.HUMAN).youControl())
        )
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        interveningIf = Conditions.CompareAmounts(
            left = DynamicAmounts.battlefield(
                Player.You,
                GameObjectFilter.Creature,
            ).distinctValues(CardNumericProperty.POWER),
            operator = ComparisonOperator.GTE,
            right = 3,
        )
        effect = Patterns.Library.lookAtTopRevealMatchingToHand(
            count = 5,
            filter = GameObjectFilter.Creature.withSubtype(Subtype.HUMAN),
            prompt = "You may reveal a Human creature card from among them and put it into your hand"
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "240"
        artist = "Howard Lyon"
        imageUri = "https://cards.scryfall.io/normal/front/5/c/5c6cf357-c231-4512-aa9a-ab4a60007136.jpg?1783925554"
        ruling("2021-09-24", "A creature has different power from another if their powers are different numbers. For example, a 1/1 creature and a 2/1 creature have different powers.")
        ruling("2021-09-24", "Many coven abilities, such as that of Dawnhart Wardens above, are triggered abilities with intervening if clauses. You must control three or more creatures with different powers at the time the ability triggers and at the time the ability tries to resolve. They do not, however, need to be the same set of creatures in both cases.")
        ruling("2021-09-24", "For three creatures to have different powers from one another, each of their powers needs to be different. A 1/1 creature, a 2/1 creature, and another 2/1 creature aren't three creatures with different powers, even though both 2/1 creatures have different power than the 1/1 creature.")
    }
}
