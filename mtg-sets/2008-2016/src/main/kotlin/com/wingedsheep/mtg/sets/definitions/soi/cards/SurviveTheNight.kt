package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Survive the Night — Shadows over Innistrad #41
 * {2}{W} · Instant
 *
 * Target creature gets +1/+0 and gains indestructible until end of turn. Investigate.
 *
 * The Clue rides on the whole spell resolving: if the only target is illegal on resolution the
 * spell doesn't resolve and you don't investigate (2016-04-08 ruling).
 */
val SurviveTheNight = card("Survive the Night") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Target creature gets +1/+0 and gains indestructible until end of turn. " +
        "(Damage and effects that say \"destroy\" don't destroy it.)\n" +
        "Investigate. (Create a Clue token. It's an artifact with \"{2}, Sacrifice this token: Draw a card.\")"

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(1, 0, creature) then
            Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, creature) then
            Effects.Investigate()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "41"
        artist = "Svetlin Velinov"
        flavorText = "The longest nights are measured not in hours, but in heartbeats."
        imageUri = "https://cards.scryfall.io/normal/front/5/c/5cb4a0bc-196a-466b-a704-377f5c7eb5b7.jpg?1783937807"
        ruling(
            "2016-04-08",
            "You can't cast a spell without choosing legal targets. If all of those targets become " +
                "illegal, the spell doesn't resolve and you won't investigate.",
        )
    }
}
