package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.EmitLibrarySearchedEventEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.ShuffleLibraryEffect
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Guidelight Pathmaker — Aetherdrift #206
 * {4}{W}{U} · Artifact — Vehicle · 6/5
 *
 * Vigilance
 * When this Vehicle enters, you may search your library for an artifact card and reveal it. Put it
 * onto the battlefield if its mana value is 2 or less. Otherwise, put it into your hand. If you
 * search your library this way, shuffle.
 * Crew 2
 *
 * The destination depends on what was found, so this can't be `Patterns.Library.searchLibrary`
 * (one fixed destination). It is an inline Gather → Select → branch pipeline instead: the found
 * card is revealed, then `ifNotEmpty(found, filter = manaValueAtMost(2))` routes it to the
 * battlefield and the `orElse` branch to hand. A declined "up to one" (or an artifact-less library)
 * leaves the collection empty, so both branches move nothing and only the shuffle runs.
 *
 * The outer [MayEffect] is the "you may search" — per the card's 2025-02-07 Oracle update the
 * shuffle only happens if you *choose* to search, which is exactly why the shuffle sits inside the
 * may rather than after it.
 */
val GuidelightPathmaker = card("Guidelight Pathmaker") {
    manaCost = "{4}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Artifact — Vehicle"
    oracleText = "Vigilance\n" +
        "When this Vehicle enters, you may search your library for an artifact card and reveal it. " +
        "Put it onto the battlefield if its mana value is 2 or less. Otherwise, put it into your " +
        "hand. If you search your library this way, shuffle.\n" +
        "Crew 2 (Tap any number of creatures you control with total power 2 or more: This Vehicle " +
        "becomes an artifact creature until end of turn.)"
    power = 6
    toughness = 5

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = MayEffect(
            Effects.Pipeline {
                val searchable = gather(
                    CardSource.FromZone(Zone.LIBRARY, Player.You, GameObjectFilter.Artifact),
                )
                val found = chooseUpTo(1, from = searchable, prompt = "Search for an artifact card")
                reveal(found)
                ifNotEmpty(found, filter = GameObjectFilter.Any.manaValueAtMost(2)) {
                    move(found, CardDestination.ToZone(Zone.BATTLEFIELD))
                } orElse {
                    toHand(found)
                }
                run(ShuffleLibraryEffect())
                run(EmitLibrarySearchedEventEffect)
            },
        )
        description = "When this Vehicle enters, you may search your library for an artifact card " +
            "and reveal it. Put it onto the battlefield if its mana value is 2 or less. Otherwise, " +
            "put it into your hand. If you search your library this way, shuffle."
    }

    keywordAbility(KeywordAbility.crew(2))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "206"
        artist = "Stephan Martiniere"
        imageUri = "https://cards.scryfall.io/normal/front/b/2/b2e00cd7-925e-4bab-b064-c96aa2935f8c.jpg?1783907857"
        ruling(
            "2025-02-07",
            "Guidelight Pathmaker has received an update to its Oracle text. Specifically, you'll " +
                "only shuffle if you choose to search your library.",
        )
    }
}
