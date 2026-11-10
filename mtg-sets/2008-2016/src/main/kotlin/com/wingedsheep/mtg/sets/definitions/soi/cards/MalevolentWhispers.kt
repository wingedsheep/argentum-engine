package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.madness
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Malevolent Whispers (Shadows over Innistrad #173)
 * {3}{R}
 * Sorcery
 *
 * Gain control of target creature until end of turn. Untap that creature. It gets +2/+0 and gains
 * haste until end of turn.
 * Madness {3}{R}
 */
val MalevolentWhispers = card("Malevolent Whispers") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Gain control of target creature until end of turn. Untap that creature. It gets +2/+0 " +
        "and gains haste until end of turn.\n" +
        "Madness {3}{R} (If you discard this card, discard it into exile. When you do, cast it for its " +
        "madness cost or put it into your graveyard.)"

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.GainControl(t, Duration.EndOfTurn) then
            Effects.Untap(t) then
            Effects.ModifyStats(2, 0, t, Duration.EndOfTurn) then
            Effects.GrantKeyword(Keyword.HASTE, t)
    }

    madness("{3}{R}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "173"
        artist = "Greg Staples"
        imageUri = "https://cards.scryfall.io/normal/front/6/7/674d33d6-dfd4-4972-aaa3-6de0236a8c45.jpg?1783937746"
        ruling(
            "2018-12-07",
            "Gaining control of a creature doesn't cause you to gain control of any Auras or Equipment " +
                "attached to it, even though those remain attached to it.",
        )
        ruling(
            "2018-12-07",
            "Malevolent Whispers can target any creature, even one that's untapped or one you already control.",
        )
    }
}
