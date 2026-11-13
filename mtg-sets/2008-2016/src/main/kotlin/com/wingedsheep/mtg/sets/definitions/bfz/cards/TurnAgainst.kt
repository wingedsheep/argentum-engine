package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Turn Against
 * {4}{R}
 * Instant
 * Devoid (This card has no color.)
 * Gain control of target creature until end of turn. Untap that creature. It gains haste until end of turn.
 */
val TurnAgainst = card("Turn Against") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Devoid (This card has no color.)\n" +
        "Gain control of target creature until end of turn. Untap that creature. It gains haste until end of turn."

    keywords(Keyword.DEVOID)

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.GainControl(creature, Duration.EndOfTurn) then
            Effects.Untap(creature) then
            Effects.GrantKeyword(Keyword.HASTE, creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "135"
        artist = "Chris Rallis"
        flavorText = "\"No, don't! Nal, it's me—\""
        imageUri = "https://cards.scryfall.io/normal/front/4/d/4de8ef35-f141-4bc6-8ea1-f87aa6f5ecd7.jpg?1783938196"
        ruling("2015-08-25", "Turn Against can target any creature, even one that's untapped or one you already control.")
        ruling("2015-08-25", "Gaining control of a creature doesn't cause you to gain control of any Auras or Equipment attached to it.")
    }
}
