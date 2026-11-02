package com.wingedsheep.mtg.sets.definitions.m19.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.unaryMinus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Nightmare's Thirst — Core Set 2019 #111 (canonical printing)
 * {B} · Instant
 *
 * You gain 1 life. Target creature gets -X/-X until end of turn, where X is the amount of life
 * you gained this turn.
 *
 * [Effects.GainLife] then [Effects.ModifyStats] by the negated [DynamicAmounts.lifeGainedThisTurn]
 * — the turn's gross life-gain tally, read after this spell's own 1 life is recorded, so it counts
 * that gain and ignores any life lost (ruling). Same shape as Fortifying Draught.
 */
val NightmaresThirst = card("Nightmare's Thirst") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "You gain 1 life. Target creature gets -X/-X until end of turn, where X is the amount " +
        "of life you gained this turn."

    spell {
        val creature = target(TargetFilter.Creature)
        val shrink = -DynamicAmounts.lifeGainedThisTurn()
        effect = Effects.GainLife(1) then
            Effects.ModifyStats(power = shrink, toughness = shrink, target = creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "111"
        artist = "Igor Kieryluk"
        flavorText = "That feeling of something on your chest is usually sleep paralysis. Occasionally, it's something else."
        imageUri = "https://cards.scryfall.io/normal/front/f/1/f12efbc0-ce88-4622-ad07-2808e651ac5a.jpg?1783934564"
        ruling("2018-07-13", "Nightmare's Thirst counts how much life you've gained this turn, not how much your life total changed. For example, if you've gained 5 life and lost 8 before Nightmare's Thirst resolves, Nightmare's Thirst has you gain 1 more life, then it gives the target creature -6/-6.")
        ruling("2018-07-13", "If the target creature is an illegal target when Nightmare's Thirst tries to resolve, the spell doesn't resolve. You won't gain 1 life.")
    }
}
