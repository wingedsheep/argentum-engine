package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Butch DeLoria, Tunnel Snake — Fallout #43.
 *
 * "Each other Rogue and/or Snake you control" counts permanents (not only creatures) with either
 * subtype, once each — an `Or` subtype predicate counts a Rogue Snake a single time (the ruling).
 * The count is read on resolution. The menace counter is a keyword counter; the Rogue type is a
 * permanent addition independent of the counter.
 */
val ButchDeLoriaTunnelSnake = card("Butch DeLoria, Tunnel Snake") {
    manaCost = "{1}{B}"
    typeLine = "Legendary Creature — Human Rogue"
    power = 2
    toughness = 2
    oracleText = "Menace (This creature can't be blocked except by two or more creatures.)\nTunnel Snakes Rule! — Whenever Butch DeLoria attacks, it gets +1/+1 until end of turn for each other Rogue and/or Snake you control.\n{1}{B}: Put a menace counter on another target creature. It becomes a Rogue in addition to its other types."
    keywords(Keyword.MENACE)
    triggeredAbility {
        trigger = Triggers.self.attacks()
        val count = DynamicAmounts.battlefield(
            Player.You,
            GameObjectFilter.Any.withAnySubtype("Rogue", "Snake"),
            excludeSelf = true,
        ).count()
        effect = Effects.ModifyStats(count, count, EffectTarget.Self)
    }
    activatedAbility {
        cost = Costs.Mana("{1}{B}")
        val creature = target(TargetFilter.OtherCreature)
        effect = Effects.AddCounters(CounterType.MENACE, 1, creature) then
            Effects.AddCreatureType("Rogue", creature, Duration.Permanent)
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "43"
        artist = "Zuzanna Wużyk"
        imageUri = "https://cards.scryfall.io/normal/front/d/6/d6ed508c-ff2c-444c-8149-34b29e2fbebe.jpg?1783912408"
        ruling("2024-03-08", "A permanent that's both a Rogue and a Snake is counted only once for Butch DeLoria's second ability.")
    }
}
