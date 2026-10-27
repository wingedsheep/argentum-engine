package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.minus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Waking the Trolls — Kaldheim #234 (canonical printing)
 * {4}{R}{G} · Enchantment — Saga
 *
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)
 * I — Destroy target land.
 * II — Put target land card from a graveyard onto the battlefield under your control.
 * III — Choose target opponent. If they control fewer lands than you, create a number of 4/4
 * green Troll Warrior creature tokens with trample equal to the difference.
 *
 * Chapter III counts both land totals on resolution; "if they control fewer … equal to the
 * difference" is exactly `max(0, yours − theirs)`, so an equal or larger opponent count creates
 * no tokens.
 */
val WakingTheTrolls = card("Waking the Trolls") {
    manaCost = "{4}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I — Destroy target land.\n" +
        "II — Put target land card from a graveyard onto the battlefield under your control.\n" +
        "III — Choose target opponent. If they control fewer lands than you, create a number of 4/4 " +
        "green Troll Warrior creature tokens with trample equal to the difference."

    sagaChapter(1) {
        val land = target(TargetFilter.Land)
        effect = Effects.Destroy(land)
    }

    sagaChapter(2) {
        val land = target(TargetFilter(GameObjectFilter.Land, zone = Zone.GRAVEYARD))
        effect = Effects.Move(
            land,
            Zone.BATTLEFIELD,
            controllerOverride = EffectTarget.Controller,
            fromZone = Zone.GRAVEYARD
        )
    }

    sagaChapter(3) {
        target(Targets.Opponent)
        effect = Effects.CreateToken(
            count = DynamicAmounts.nonNegative(
                DynamicAmounts.count(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Land) -
                    DynamicAmounts.count(Player.TargetOpponent, Zone.BATTLEFIELD, GameObjectFilter.Land)
            ),
            power = 4,
            toughness = 4,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Troll", "Warrior"),
            keywords = setOf(Keyword.TRAMPLE),
            imageUri = "https://cards.scryfall.io/normal/front/8/8/8869a8cc-d196-417f-bba5-5ed31bae6a18.jpg?1783928078"
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "234"
        artist = "Daniel Ljunggren"
        imageUri = "https://cards.scryfall.io/normal/front/3/6/36016324-384c-4c68-8f73-8f39a244c879.jpg?1783928187"
    }
}
