package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Brad Boimler, Eager Ensign
 * {1}{W}
 * Legendary Creature — Human Officer
 * 2/2
 *
 * Lifelink
 * Whenever Brad Boimler becomes tapped, until end of turn, if one or more counters would be put on a
 * permanent you control, that many plus one of each of those kinds of counters are put on that
 * permanent instead.
 *
 * The passive "would be put" names no placer, so an opponent's counters on your permanents grow too
 * (`placer = Player.Any`); every kind counts as its own placement (`counterType = null`).
 */
val BradBoimlerEagerEnsign = card("Brad Boimler, Eager Ensign") {
    manaCost = "{1}{W}"
    typeLine = "Legendary Creature — Human Officer"
    colorIdentity = "W"
    oracleText = "Lifelink\n" +
        "Whenever Brad Boimler becomes tapped, until end of turn, if one or more counters would be put on " +
        "a permanent you control, that many plus one of each of those kinds of counters are put on that " +
        "permanent instead."
    power = 2
    toughness = 2

    keywords(Keyword.LIFELINK)

    triggeredAbility {
        trigger = Triggers.self.becomesTapped()
        effect = Effects.GrantCounterPlacementModifier(
            counterType = null,
            recipient = Recipient.PermanentYouControl,
            placer = Player.Any
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "5"
        artist = "Andreas Zafiratos"
        flavorText = "\"We all joined Starfleet to dive headfirst into the unknown.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/6/a6bf1525-2212-46d0-ad4d-1dbaa2e3b3cd.jpg?1784036739"
    }
}
