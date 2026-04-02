package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Swarm of Bloodflies
 * {4}{B}
 * Creature — Insect
 * 0/0
 * Flying
 * This creature enters with two +1/+1 counters on it.
 * Whenever another creature dies, put a +1/+1 counter on this creature.
 */
val SwarmOfBloodflies = card("Swarm of Bloodflies") {
    manaCost = "{4}{B}"
    typeLine = "Creature — Insect"
    power = 0
    toughness = 0
    oracleText = "Flying\nThis creature enters with two +1/+1 counters on it.\nWhenever another creature dies, put a +1/+1 counter on this creature."

    keywords(Keyword.FLYING)

    replacementEffect(EntersWithCounters(count = 2, selfOnly = true))

    triggeredAbility {
        trigger = Triggers.AnyOtherCreatureDies
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "92"
        artist = "Marco Nelor"
        flavorText = "In the Gudul delta, bloodfly bites are indistinguishable from spear wounds."
        imageUri = "https://cards.scryfall.io/normal/front/c/b/cbd36319-e05d-4a9e-a6d4-48fba9111648.jpg?1562793596"
    }
}
