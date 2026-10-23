package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Fortifying Draught — Strixhaven: School of Mages #132 (canonical printing)
 * {G} · Instant
 *
 * You gain 2 life. Target creature gets +X/+X until end of turn, where X is the amount of life
 * you gained this turn.
 *
 * [Effects.GainLife] then [Effects.ModifyStats] by [DynamicAmounts.lifeGainedThisTurn] — the
 * turn's gross life-gain tally, read after this spell's own 2 life is recorded, so it counts
 * that gain and ignores any life lost (ruling).
 */
val FortifyingDraught = card("Fortifying Draught") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "You gain 2 life. Target creature gets +X/+X until end of turn, where X is the amount " +
        "of life you gained this turn."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.GainLife(2) then
            Effects.ModifyStats(
                power = DynamicAmounts.lifeGainedThisTurn(),
                toughness = DynamicAmounts.lifeGainedThisTurn(),
                target = creature
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "132"
        artist = "Andrey Kuzinskiy"
        flavorText = "\"If a seedling can push through solid stone, there's no limit to what I could do.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/7/6789b717-a000-464a-a39b-32c1aeefe560.jpg?1783927343"
        ruling("2021-04-16", "Fortifying Draught counts the total amount of life gained without considering any life you lost during that turn. For example, if you lost 2 life and gained 3 life earlier in the turn, you will gain 2 more life from Fortifying Draught and the target creature will get +5/+5.")
    }
}
