package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.madness
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Distemper of the Blood (Eldritch Moon #126)
 * {1}{R}
 * Sorcery
 *
 * Target creature gets +2/+2 and gains trample until end of turn.
 * Madness {R} (If you discard this card, discard it into exile. When you do, cast it for its
 * madness cost or put it into your graveyard.)
 */
val DistemperOfTheBlood = card("Distemper of the Blood") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Target creature gets +2/+2 and gains trample until end of turn.\n" +
        "Madness {R} (If you discard this card, discard it into exile. When you do, cast it for " +
        "its madness cost or put it into your graveyard.)"

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(2, 2, creature) then Effects.GrantKeyword(Keyword.TRAMPLE, creature)
    }

    madness("{R}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "126"
        artist = "Ben Maier"
        imageUri = "https://cards.scryfall.io/normal/front/d/0/d0ad2acb-073b-4a98-be8c-2ea39ca85496.jpg"
    }
}
