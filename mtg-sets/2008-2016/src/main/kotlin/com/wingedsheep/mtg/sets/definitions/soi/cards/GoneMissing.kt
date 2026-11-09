package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Gone Missing — Shadows over Innistrad #67
 * {4}{U} · Sorcery
 *
 * Put target permanent on top of its owner's library. Investigate.
 *
 * The Clue rides on the whole spell resolving: if the only target is illegal on resolution the
 * spell doesn't resolve and you don't investigate (2016-04-08 ruling).
 */
val GoneMissing = card("Gone Missing") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Put target permanent on top of its owner's library.\nInvestigate. " +
        "(Create a Clue token. It's an artifact with \"{2}, Sacrifice this token: Draw a card.\")"

    spell {
        val permanent = target(TargetFilter.Permanent)
        effect = Effects.PutOnTopOfLibrary(permanent) then
            Effects.Investigate()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "67"
        artist = "James Paick"
        flavorText = "\"Sometimes it's best for the lost to stay lost.\"\n—Herzag, mayor of Havengul"
        imageUri = "https://cards.scryfall.io/normal/front/e/8/e88ae6bf-9c58-4543-ba66-19ea41d01e9b.jpg?1783937796"
        ruling(
            "2016-04-08",
            "You can't cast a spell without choosing legal targets. If all of those targets become " +
                "illegal, the spell doesn't resolve and you won't investigate.",
        )
    }
}
