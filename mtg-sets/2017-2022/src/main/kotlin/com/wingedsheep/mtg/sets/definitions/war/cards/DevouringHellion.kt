package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithDevour
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Devouring Hellion
 * {2}{R}
 * Creature — Hellion
 * 2/2
 * As this creature enters, you may sacrifice any number of creatures and/or planeswalkers. If you
 * do, it enters with twice that many +1/+1 counters on it.
 *
 * The ability is devour's replacement (CR 702.82a) in all but name, widened to planeswalkers, so it
 * is the [EntersWithDevour] replacement with a creature-or-planeswalker sacrifice filter and a
 * multiplier of 2 — without the Devour keyword, which the card doesn't print. The sacrifice is chosen
 * while the Hellion enters, from permanents already on the battlefield, and the counters are placed
 * as it enters, so nothing sacrificed sees it enter.
 */
val DevouringHellion = card("Devouring Hellion") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Hellion"
    power = 2
    toughness = 2
    oracleText = "As this creature enters, you may sacrifice any number of creatures and/or planeswalkers. " +
        "If you do, it enters with twice that many +1/+1 counters on it."

    replacementEffect(
        EntersWithDevour(
            multiplier = 2,
            sacrificeFilter = GameObjectFilter.CreatureOrPlaneswalker
        )
    )

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "124"
        artist = "Bayard Wu"
        flavorText = "There's no crisis a hellion can't make worse."
        imageUri = "https://cards.scryfall.io/normal/front/8/8/88205574-bf2b-46e7-aac7-4b8ea217f94c.jpg?1783933430"
        ruling(
            "2019-05-03",
            "You choose how many and which creatures and planeswalkers to sacrifice for Devouring Hellion's " +
                "ability while it's entering the battlefield. No player may take actions between the time you " +
                "choose which permanents to sacrifice and the time Devouring Hellion is on the battlefield with " +
                "those counters."
        )
        ruling(
            "2019-05-03",
            "If any of the sacrificed permanents have abilities that trigger when Devouring Hellion enters the " +
                "battlefield (such as that of Kronch Wrangler), those abilities won't trigger."
        )
        ruling(
            "2019-05-03",
            "If Devouring Hellion somehow enters the battlefield at the same time as another creature or " +
                "planeswalker, you can't sacrifice that creature or planeswalker. You may choose only permanents " +
                "that are already on the battlefield."
        )
    }
}
