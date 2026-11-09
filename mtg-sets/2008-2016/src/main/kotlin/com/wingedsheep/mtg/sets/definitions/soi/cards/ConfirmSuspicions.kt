package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Confirm Suspicions — Shadows over Innistrad #53
 * {3}{U}{U} · Instant
 *
 * Counter target spell. Investigate three times.
 *
 * The Clues ride on the whole spell resolving: if the targeted spell has left the stack by the time
 * Confirm Suspicions resolves, its only target is illegal, the spell doesn't resolve and you don't
 * investigate (2016-04-08 ruling). A target that merely can't be countered is still legal, so the
 * counter does nothing and you still investigate three times.
 */
val ConfirmSuspicions = card("Confirm Suspicions") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target spell.\nInvestigate three times. " +
        "(To investigate, create a Clue token. It's an artifact with \"{2}, Sacrifice this token: Draw a card.\")"

    spell {
        target(TargetFilter.SpellOnStack)
        effect = Effects.CounterSpell() then
            Effects.Investigate(3)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "53"
        artist = "David Gaillet"
        flavorText = "Everyone has something to hide."
        imageUri = "https://cards.scryfall.io/normal/front/c/f/cf7fcbc2-1034-442d-9f2a-7d79ea40ac3d.jpg?1783937803"
        ruling(
            "2016-04-08",
            "You can't cast a spell without choosing legal targets. If all of those targets become " +
                "illegal, the spell doesn't resolve and you won't investigate.",
        )
    }
}
