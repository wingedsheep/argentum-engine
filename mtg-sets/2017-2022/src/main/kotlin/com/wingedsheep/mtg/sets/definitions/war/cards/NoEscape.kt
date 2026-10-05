package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * No Escape
 * {2}{U}
 * Instant
 * Counter target creature or planeswalker spell. If that spell is countered this way, exile it
 * instead of putting it into its owner's graveyard.
 * Scry 1.
 *
 * An uncounterable creature or planeswalker spell is still a legal target: the counter does
 * nothing to it, but the scry still happens.
 */
val NoEscape = card("No Escape") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target creature or planeswalker spell. If that spell is countered this way, " +
        "exile it instead of putting it into its owner's graveyard.\nScry 1."

    spell {
        val spell = target(TargetFilter(GameObjectFilter.CreatureOrPlaneswalker, zone = Zone.STACK))
        effect = Effects.CounterSpellToExile() then Effects.Scry(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "63"
        artist = "G-host Lee"
        flavorText = "Jace surmised that they were walking into Bolas's trap. He felt no joy in being right."
        imageUri = "https://cards.scryfall.io/normal/front/b/c/bc9888a1-6f35-4802-b8fb-902017736d4a.jpg?1783933458"
        ruling("2019-05-03", "A creature or planeswalker spell that can't be countered is a legal target for No Escape. The spell won't be countered when No Escape resolves, but you'll still scry 1.")
    }
}
