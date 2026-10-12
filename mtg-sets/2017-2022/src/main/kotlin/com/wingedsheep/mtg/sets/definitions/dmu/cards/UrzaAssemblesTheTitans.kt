package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.readAhead
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Urza Assembles the Titans
 * {3}{W}{W}
 * Enchantment — Saga
 *
 * Read ahead (Choose a chapter and start with that many lore counters. Add one after your draw
 * step. Skipped chapters don't trigger. Sacrifice after III.)
 * I — Scry 4, then you may reveal the top card of your library. If a planeswalker card is revealed
 *     this way, put it into your hand.
 * II — You may put a planeswalker card with mana value 6 or less from your hand onto the
 *      battlefield.
 * III — You may activate the loyalty abilities of planeswalkers you control twice this turn rather
 *       than only once.
 *
 * Chapter III is a player-held grant, not a static on the Saga: the Saga is sacrificed right after
 * it resolves, and the grant still covers planeswalkers that enter later that turn.
 */
val UrzaAssemblesTheTitans = card("Urza Assembles the Titans") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Saga"
    oracleText = "Read ahead (Choose a chapter and start with that many lore counters. Add one " +
        "after your draw step. Skipped chapters don't trigger. Sacrifice after III.)\n" +
        "I — Scry 4, then you may reveal the top card of your library. If a planeswalker card is " +
        "revealed this way, put it into your hand.\n" +
        "II — You may put a planeswalker card with mana value 6 or less from your hand onto the " +
        "battlefield.\n" +
        "III — You may activate the loyalty abilities of planeswalkers you control twice this turn " +
        "rather than only once."

    readAhead()

    sagaChapter(1) {
        effect = Patterns.Library.scry(4) then Effects.May(
            Effects.Pipeline {
                val revealed = gather(CardSource.TopOfLibrary(1), revealed = true)
                val planeswalker = selectAll(from = revealed, filter = GameObjectFilter.Planeswalker)
                toHand(planeswalker)
            },
            descriptionOverride = "You may reveal the top card of your library.",
        )
    }

    sagaChapter(2) {
        effect = Patterns.Hand.putFromHand(
            filter = GameObjectFilter.Planeswalker.manaValueAtMost(6),
        )
    }

    sagaChapter(3) {
        effect = Effects.ExtraLoyaltyActivations()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "37"
        artist = "Josu Hernaiz"
        imageUri = "https://cards.scryfall.io/normal/front/1/0/10a544d7-ead7-42fd-a35a-c1b672771d5b.jpg?1783921356"

        ruling("2022-09-09", "After this Saga's third chapter ability resolves, you may activate the same ability of a planeswalker twice that turn, or you may activate two different abilities of that planeswalker that turn.")
        ruling("2022-09-09", "If more than one effect allows you to activate loyalty abilities of planeswalkers your control twice rather than only once, those effects don't continue to increase the number of activations. You won't be able to activate abilities of planeswalkers you control more than twice in a turn.")
        ruling("2022-09-09", "As a Saga with read ahead enters the battlefield, its controller chooses a number from one to that Saga's greatest chapter number. The Saga enters the battlefield with the chosen number of lore counters. Neither choosing the number nor putting the counters on the Saga use the stack, and neither can be responded to.")
    }
}
