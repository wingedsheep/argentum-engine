package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.SelfAlternativeCost

/**
 * Stalwart Valkyrie
 * {3}{W}
 * Creature — Angel Warrior
 * 3/2
 * Flying
 * You may pay {1}{W} and exile a creature card from your graveyard rather than pay this spell's
 * mana cost.
 *
 * An unconditional [SelfAlternativeCost]: {1}{W} plus one non-mana additional cost (exile a
 * creature card from your graveyard), the same shape as Force of Vigor's exile-from-hand
 * alternative. The card itself is on the stack while costs are paid, so it is never part of the
 * graveyard pool. Casting it this way changes neither its timing nor its mana value (4).
 */
val StalwartValkyrie = card("Stalwart Valkyrie") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Angel Warrior"
    power = 3
    toughness = 2
    oracleText = "Flying\nYou may pay {1}{W} and exile a creature card from your graveyard rather " +
        "than pay this spell's mana cost."

    keywords(Keyword.FLYING)

    selfAlternativeCost = SelfAlternativeCost(
        manaCost = ManaCost.parse("{1}{W}"),
        additionalCosts = listOf(
            Costs.additional.ExileCards(count = 1, filter = GameObjectFilter.Creature)
        )
    )

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "31"
        artist = "Jason Rainville"
        flavorText = "\"Only the worthy may pass.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/9/89ecb530-e429-4b50-a24c-8437614cf224.jpg?1783928277"
        ruling(
            "2021-02-05",
            "Casting Stalwart Valkyrie for its alternative cost doesn't change when you can cast it."
        )
        ruling(
            "2021-02-05",
            "Once you announce that you're casting Stalwart Valkyrie using its alternative cost, no " +
                "player may take other actions until the spell's been paid for. Notably, players " +
                "can't try to remove creature cards from your graveyard to stop you from casting the spell."
        )
        ruling(
            "2021-02-05",
            "Stalwart Valkyrie has mana value 4, no matter what you actually paid to cast it."
        )
    }
}
