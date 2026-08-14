package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.webSlinging
import com.wingedsheep.sdk.model.Rarity

/**
 * Spider-Sense — Marvel's Spider-Man #46
 * {1}{U} · Instant
 *
 * Web-slinging {U}
 * Counter target instant spell, sorcery spell, or triggered ability.
 *
 * The only instant-speed web-slinger — its web-slinging cast is offered at instant speed like any
 * ordinary instant (CR 702.188 adds no timing permission of its own; the card's instant type already
 * allows it). The counter targets an instant/sorcery spell or *triggered* ability (not activated),
 * via [Targets.InstantSorceryOrTriggeredAbility], and [Effects.CounterSpellOrAbility] dispatches on
 * whichever stack object was chosen.
 */
val SpiderSense = card("Spider-Sense") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Web-slinging {U} (You may cast this spell for {U} if you also return a tapped " +
        "creature you control to its owner's hand.)\n" +
        "Counter target instant spell, sorcery spell, or triggered ability."

    webSlinging("{U}")

    spell {
        val t = target("target instant spell, sorcery spell, or triggered ability", Targets.InstantSorceryOrTriggeredAbility)
        effect = Effects.CounterSpellOrAbility()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "46"
        artist = "Borja Pindado"
        flavorText = "\"My spider-sense—it's tingling! Something behind me!\""
        imageUri = "https://cards.scryfall.io/normal/front/4/4/4499a25b-f4a5-4f2c-9ebd-bc68c7840b39.jpg?1783905348"
    }
}
