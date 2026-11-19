package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Knowledge and Power (Journey into Nyx #101)
 * {4}{R}
 * Enchantment
 *
 * Whenever you scry, you may pay {2}. If you do, this enchantment deals 2 damage to any target.
 *
 * The target is chosen as the trigger goes on the stack; the {2} is offered on resolution, once
 * per scry regardless of how many cards were looked at.
 */
val KnowledgeAndPower = card("Knowledge and Power") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "Whenever you scry, you may pay {2}. If you do, this enchantment deals 2 damage to any target."

    triggeredAbility {
        trigger = Triggers.you.scries()
        val t = target(Targets.Any)
        effect = Effects.MayPay(
            cost = ManaCost.parse("{2}"),
            then = Effects.DealDamage(2, t)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "101"
        artist = "Jung Park"
        flavorText = "\"Information is a powerful weapon.\"\n—Cymede, queen of Akros"
        imageUri = "https://cards.scryfall.io/normal/front/7/c/7cd48a18-df3d-4381-a7e7-a3e9ae18d4d7.jpg?1783939422"
        ruling(
            "2014-04-26",
            "Knowledge and Power's ability triggers only once each time you scry, no matter how many cards you look at."
        )
        ruling(
            "2014-04-26",
            "You choose the target of the ability as it's put on the stack. You choose whether to pay {2} as it " +
                "resolves. You can pay {2} only once each time you scry."
        )
    }
}
