package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.MustBeBlocked

/**
 * Breaker of Armies
 * {8}
 * Creature — Eldrazi
 * 10/8
 * All creatures able to block this creature do so.
 *
 * The Lure clause is the static `MustBeBlocked(allCreatures = true)` (Nath's Elite, Nessian Boar),
 * which only binds creatures that can block it — a tapped creature or one whose block carries a
 * cost stays free.
 */
val BreakerOfArmies = card("Breaker of Armies") {
    manaCost = "{8}"
    colorIdentity = ""
    typeLine = "Creature — Eldrazi"
    power = 10
    toughness = 8
    oracleText = "All creatures able to block this creature do so."

    staticAbility {
        ability = MustBeBlocked(allCreatures = true)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "3"
        artist = "Richard Wright"
        flavorText = "\"There are some who have lost everyone and everything. They actively seek out the " +
            "Eldrazi, eager for the smallest measure of vengeance.\"\n—Munda, ambush leader"
        imageUri = "https://cards.scryfall.io/normal/front/7/8/784e843e-7010-466d-adbd-1dd1595aead1.jpg?1783938225"
        ruling(
            "2015-08-25",
            "If more than one Breaker of Armies is attacking, the controller of each creature that could " +
                "block them chooses which one that creature blocks. In this case, creatures that can block " +
                "multiple creatures must block as many attacking Breaker of Armies as possible."
        )
        ruling(
            "2015-08-25",
            "If, during its controller's declare blockers step, a creature the defending player controls is " +
                "tapped or is affected by a spell or ability that says it can't block, then it doesn't block. " +
                "If there's a cost associated with having it block, its controller isn't forced to pay that " +
                "cost. If they don't, the creature doesn't have to block."
        )
        ruling(
            "2015-08-25",
            "If a creature can't legally block Breaker of Armies but could block another attacking creature, " +
                "it may do so. Likewise, if a creature the defending player controls can't block Breaker of " +
                "Armies unless its controller pays a cost, its controller may decline to pay that cost and " +
                "block a different attacking creature."
        )
    }
}
