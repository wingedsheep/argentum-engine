package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Unleash Fury — Core Set 2021 #170 (canonical printing)
 * {1}{R} · Instant
 *
 * Double the power of target creature until end of turn.
 *
 * "Double" is the Bulk Up / Exponential Growth shape: +X/+0 until end of turn where X is the
 * creature's power as the effect begins to apply, locked in at resolution. A negative power
 * doubles downward because the bonus is the (negative) power itself.
 */
val UnleashFury = card("Unleash Fury") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Double the power of target creature until end of turn."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(
            power = DynamicAmounts.powerOf(creature),
            toughness = DynamicAmounts.fixed(0),
            target = creature
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "170"
        artist = "Izzy"
        flavorText = "Might doesn't make right, but it does win the fight."
        imageUri = "https://cards.scryfall.io/normal/front/e/8/e854e6a3-8684-43fa-9560-ef4c3b62c935.jpg?1783930681"
        ruling("2020-06-23", "If an effect instructs you to \"double\" a creature's power, that creature gets +X/+0, where X is its power as that effect begins to apply. If its power is negative, instead it gets -X/-0 where X is how far below 0 its power is. The value of X won't change if another effect alters the creature's power later in the turn.")
    }
}
