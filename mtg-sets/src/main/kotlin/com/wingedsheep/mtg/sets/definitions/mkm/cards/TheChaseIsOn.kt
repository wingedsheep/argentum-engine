package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * The Chase Is On — Murders at Karlov Manor #116
 * {2}{R} · Instant
 *
 * Target creature gets +3/+0 and gains first strike until end of turn. Investigate.
 *
 * Same shape as [AuspiciousArrival] / [ToxinAnalysis]: the Clue rides on the whole spell
 * resolving, so an illegal target by resolution counters the spell (CR 608.2b) and no Clue
 * is created.
 */
val TheChaseIsOn = card("The Chase Is On") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Target creature gets +3/+0 and gains first strike until end of turn. Investigate. " +
        "(Create a Clue token. It's an artifact with \"{2}, Sacrifice this token: Draw a card.\")"

    spell {
        val creature = target("target creature", Targets.Creature)
        effect = Effects.Composite(
            Effects.ModifyStats(3, 0, creature),
            Effects.GrantKeyword(Keyword.FIRST_STRIKE, creature),
            Effects.Investigate()
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "116"
        artist = "Diego Gisbert"
        flavorText = "Etrata evaded the Boros arresters and Azorius lawmages, but not even a " +
            "master assassin could give Kaya the slip."
        imageUri = "https://cards.scryfall.io/normal/front/1/d/1d54d596-f7aa-4b05-ab13-19b246698c04.jpg?1783912886"
    }
}
