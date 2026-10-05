package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Jace's Scrutiny — Shadows over Innistrad #70
 * {1}{U} · Instant
 *
 * Target creature gets -4/-0 until end of turn. Investigate.
 *
 * The Clue rides on the whole spell resolving: if the only target is illegal on resolution the
 * spell doesn't resolve (CR 608.2b) and you don't investigate (2016-04-08 ruling).
 */
val JacesScrutiny = card("Jace's Scrutiny") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Target creature gets -4/-0 until end of turn.\nInvestigate. " +
        "(Create a Clue token. It's an artifact with \"{2}, Sacrifice this token: Draw a card.\")"

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(-4, 0, creature) then
            Effects.Investigate()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "70"
        artist = "Slawomir Maniak"
        flavorText = "\"Just a collage of images: cryptoliths, the sea, a tangle of shipwrecks.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/d/3d0eea0d-c790-4d91-962f-f05b22d0c8fa.jpg?1783937795"
        ruling(
            "2016-04-08",
            "You can't cast a spell without choosing legal targets. If all of those targets become " +
                "illegal, the spell doesn't resolve and you won't investigate.",
        )
    }
}
