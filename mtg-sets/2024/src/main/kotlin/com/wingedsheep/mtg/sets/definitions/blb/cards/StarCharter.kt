package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Star Charter {3}{W}
 * Creature — Bat Cleric
 * 3/1
 *
 * Flying
 * At the beginning of your end step, if you gained or lost life this turn,
 * look at the top four cards of your library. You may reveal a creature card
 * with power 3 or less from among them and put it into your hand. Put the rest
 * on the bottom of your library in a random order.
 */
val StarCharter = card("Star Charter") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Bat Cleric"
    oracleText = "Flying\nAt the beginning of your end step, if you gained or lost life this turn, look at the top four cards of your library. You may reveal a creature card with power 3 or less from among them and put it into your hand. Put the rest on the bottom of your library in a random order."
    power = 3
    toughness = 1

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.YourEndStep
        interveningIf = Conditions.YouGainedOrLostLifeThisTurn
        // `Patterns.Library.lookAtTopRevealMatchingToHand` names this card in its own KDoc, and the
        // recipe was restated here by hand instead of called — which lost the `revealed = true` on
        // the move to hand, so the card the text says to *reveal* went to hand unseen.
        effect = Patterns.Library.lookAtTopRevealMatchingToHand(
            count = DynamicAmount.Fixed(4),
            filter = GameObjectFilter.Creature.powerAtMost(3),
            prompt = "You may reveal a creature card with power 3 or less from among them and put it into your hand",
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "33"
        artist = "Valera Lutfullina"
        imageUri = "https://cards.scryfall.io/normal/front/0/e/0e209237-00f7-4bf0-8287-ccde02ce8e8d.jpg?1721425964"
    }
}
