package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Reckless Racer
 * {2}{R}
 * Creature — Human Pilot
 * 2/3
 * First strike
 * Whenever this creature becomes tapped, you may discard a card. If you do, draw a card.
 */
val RecklessRacer = card("Reckless Racer") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Pilot"
    oracleText = "First strike\n" +
        "Whenever this creature becomes tapped, you may discard a card. If you do, draw a card."
    power = 2
    toughness = 3

    keywords(Keyword.FIRST_STRIKE)

    triggeredAbility {
        trigger = Triggers.self.becomesTapped()
        effect = Effects.May(effect = Effects.IfYouDo(action = Patterns.Hand.discardCards(1), then = Effects.DrawCards(1)))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "95"
        artist = "Viktor Titov"
        flavorText = "Illegal street races turned out to be perfect practice for avoiding Consulate patrols."
        imageUri = "https://cards.scryfall.io/normal/front/2/1/21c0afd6-1242-42bf-a13f-9f218c9d9dfc.jpg"
        ruling("2017-02-09", "This is a triggered ability, not an activated ability. It doesn't allow you to tap Reckless Racer whenever you want; rather, you need some other way of tapping it, such as by attacking or crewing a Vehicle.")
        ruling("2017-02-09", "For the ability to trigger, Reckless Racer has to actually change from untapped to tapped. If an effect attempts to tap it, but it was already tapped at the time, this ability won't trigger.")
    }
}
