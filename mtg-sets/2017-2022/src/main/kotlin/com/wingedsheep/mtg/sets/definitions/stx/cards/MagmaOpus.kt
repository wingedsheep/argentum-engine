package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.AnyTarget

/**
 * Magma Opus — Strixhaven: School of Mages #203 (canonical printing)
 * {6}{U}{R} · Instant
 *
 * Magma Opus deals 4 damage divided as you choose among any number of targets. Tap two target
 * permanents. Create a 4/4 blue and red Elemental creature token. Draw two cards.
 * {U/R}{U/R}, Discard this card: Create a Treasure token.
 *
 * Two target words: up to four "any targets" (each needs at least 1 of the 4 damage) and exactly two
 * permanents to tap. The division is scoped to the first group (`DividedDamage(among = …)`), so it
 * is announced as the spell is cast among those targets only — a permanent may be in both groups.
 */
val MagmaOpus = card("Magma Opus") {
    manaCost = "{6}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Instant"
    oracleText = "Magma Opus deals 4 damage divided as you choose among any number of targets. Tap two " +
        "target permanents. Create a 4/4 blue and red Elemental creature token. Draw two cards.\n" +
        "{U/R}{U/R}, Discard this card: Create a Treasure token."

    spell {
        val damaged = targets(AnyTarget(count = 4, minCount = 0))
        val (first, second) = targets(TargetFilter.Permanent, count = 2)
        effect = Effects.DividedDamage(total = 4, minTargets = 0, maxTargets = 4, among = damaged) then
            Effects.Tap(first) then
            Effects.Tap(second) then
            Effects.CreateToken(
                power = 4,
                toughness = 4,
                colors = setOf(Color.BLUE, Color.RED),
                creatureTypes = setOf("Elemental"),
            ) then
            Effects.DrawCards(2)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{U/R}{U/R}"), Costs.DiscardSelf)
        activateFromZone = Zone.HAND
        effect = Effects.CreateTreasure()
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "203"
        artist = "Liiga Smilshkalne"
        imageUri = "https://cards.scryfall.io/normal/front/2/7/27c914a3-2294-4feb-9450-c012cfd307a6.jpg?1783927307"
    }
}
