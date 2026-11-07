package com.wingedsheep.mtg.sets.definitions.j25.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Gilded Scuttler — Foundations Jumpstart #7
 * {2}{U} · Artifact Creature — Crab · 1/3
 *
 * This creature can't be blocked.
 * When this creature enters, tap target creature an opponent controls and put a stun counter on it.
 *
 * Frostfist Strider's enters trigger: the stun counter replaces the target's next untap.
 */
val GildedScuttler = card("Gilded Scuttler") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Artifact Creature — Crab"
    power = 1
    toughness = 3
    oracleText = "This creature can't be blocked.\n" +
        "When this creature enters, tap target creature an opponent controls and put a stun counter on it. " +
        "(If a permanent with a stun counter would become untapped, remove one from it instead.)"

    flags(AbilityFlag.CANT_BE_BLOCKED)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(TargetFilter.Creature.opponentControls())
        effect = Effects.Tap(t) then Effects.AddCounters(CounterType.STUN, 1, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "7"
        artist = "Samuel Perin"
        flavorText = "\"ATTENTION BEACHGOERS: This is a litter-free zone. Violators will be pinched.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/e/1e5a6db8-130a-4931-b7a5-2f024331802b.jpg?1783908869"
    }
}
