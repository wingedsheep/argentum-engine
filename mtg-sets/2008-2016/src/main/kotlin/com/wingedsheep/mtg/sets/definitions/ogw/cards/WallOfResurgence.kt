package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Wall of Resurgence — Oath of the Gatewatch #39 (canonical printing)
 * {2}{W} · Creature — Wall · 0/6
 *
 * Defender
 * When this creature enters, you may put three +1/+1 counters on target land you control. If you
 * do, that land becomes a 0/0 Elemental creature with haste that's still a land.
 *
 * The target is chosen when the trigger goes on the stack; the "may" is answered on resolution.
 * Declining skips both halves ("If you do"). Accepting runs Roaring Earth's channel shape:
 * [Effects.AddCounters] then a permanent [Effects.BecomeCreature] (base 0/0, Elemental, haste),
 * which keeps the land type, subtypes and abilities.
 */
val WallOfResurgence = card("Wall of Resurgence") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Wall"
    power = 0
    toughness = 6
    oracleText = "Defender\n" +
        "When this creature enters, you may put three +1/+1 counters on target land you control. " +
        "If you do, that land becomes a 0/0 Elemental creature with haste that's still a land."

    keywords(Keyword.DEFENDER)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val land = target(TargetFilter(GameObjectFilter.Land.youControl()))
        effect = Effects.May(
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 3, land) then
                Effects.BecomeCreature(
                    target = land,
                    power = 0,
                    toughness = 0,
                    keywords = setOf(Keyword.HASTE),
                    creatureTypes = setOf("Elemental"),
                    duration = Duration.Permanent,
                )
        )
        description = "When this creature enters, you may put three +1/+1 counters on target land you " +
            "control. If you do, that land becomes a 0/0 Elemental creature with haste that's still a land."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "39"
        artist = "Daniel Ljunggren"
        imageUri = "https://cards.scryfall.io/normal/front/c/9/c99f0dc1-c8cf-4733-a874-5ae57b38258e.jpg?1783937923"
        ruling(
            "2016-01-22",
            "If you target a land that's already a creature with Wall of Resurgence's triggered ability, " +
                "that land creature's base power and toughness will become 0/0, overwriting its previous " +
                "base power and toughness. Other effects that modify its power and/or toughness (including " +
                "the new and any previously-existing +1/+1 counters) will continue to apply."
        )
    }
}
