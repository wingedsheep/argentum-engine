package com.wingedsheep.mtg.sets.definitions.m11.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeTargetedBy
import com.wingedsheep.sdk.scripting.ProtectionScope
import com.wingedsheep.sdk.scripting.UntargetableBy

/**
 * Gaea's Revenge — Magic 2011 #174
 * {5}{G}{G} · Creature — Elemental · 8/5
 *
 * This spell can't be countered.
 * Haste
 * This creature can't be the target of nongreen spells or abilities from nongreen sources.
 *
 * [CantBeTargetedBy] with [UntargetableBy.SourcesOfColor] over [ProtectionScope.NonColor] — unlike
 * hexproof from nongreen it binds its own controller too, and colorless sources are nongreen.
 */
val GaeasRevenge = card("Gaea's Revenge") {
    manaCost = "{5}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elemental"
    power = 8
    toughness = 5
    oracleText = "This spell can't be countered.\n" +
        "Haste\n" +
        "This creature can't be the target of nongreen spells or abilities from nongreen sources."

    cantBeCountered = true

    keywords(Keyword.HASTE)

    staticAbility {
        ability = CantBeTargetedBy(UntargetableBy.SourcesOfColor(ProtectionScope.NonColor(Color.GREEN)))
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "174"
        artist = "Kekai Kotaki"
        imageUri = "https://cards.scryfall.io/normal/front/6/8/68f610a9-73b0-4ef8-b725-f3eee1b78fad.jpg?1783941799"
    }
}
