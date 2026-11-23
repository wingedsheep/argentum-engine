package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Counterflux — Return to Ravnica #153 (canonical printing)
 * {U}{U}{R} · Instant
 *
 * This spell can't be countered.
 * Counter target spell you don't control.
 * Overload {1}{U}{U}{R}
 *
 * Overloaded, it counters each spell on the stack controlled by a player other than its
 * caster (spells only, not abilities); Counterflux itself is the caster's, so it is spared.
 */
val Counterflux = card("Counterflux") {
    manaCost = "{U}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Instant"
    oracleText = "This spell can't be countered.\n" +
        "Counter target spell you don't control.\n" +
        "Overload {1}{U}{U}{R} (You may cast this spell for its overload cost. If you do, change \"target\" " +
        "in its text to \"each.\")"

    cantBeCountered = true
    keywordAbility(KeywordAbility.overload("{1}{U}{U}{R}"))

    spell {
        target(TargetFilter.SpellOnStack.opponentControls())
        effect = Effects.CounterSpell()

        overloadEffect = Effects.CounterAllOpponentStackObjects(spells = true, abilities = false)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "153"
        artist = "Scott M. Fischer"
        imageUri = "https://cards.scryfall.io/normal/front/9/4/94e4b773-40a4-4272-85dd-f728ada22748.jpg?1783940343"
        ruling(
            "2024-01-12",
            "Because a spell with overload doesn't target when its overload cost is paid, it may affect " +
                "permanents with hexproof or with protection from the appropriate color."
        )
        ruling(
            "2024-01-12",
            "If you don't pay the overload cost of a spell with overload, that spell will have a single target. " +
                "If you pay the overload cost, the spell won't have any targets."
        )
    }
}
