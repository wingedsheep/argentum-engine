package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Gnarled Effigy
 * {4}
 * Artifact
 *
 * {4}, {T}: Put a -1/-1 counter on target creature.
 *
 * - The target is any creature, not just an opponent's — the printed line has no controller
 *   restriction, so [Targets.Creature] is left unfiltered.
 * - Instant-speed by default: an activated ability with no timing clause needs no [TimingRule].
 */
val GnarledEffigy = card("Gnarled Effigy") {
    manaCost = "{4}"
    typeLine = "Artifact"
    oracleText = "{4}, {T}: Put a -1/-1 counter on target creature."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{4}"), Costs.Tap)
        target = Targets.Creature
        effect = Effects.AddCounters(Counters.MINUS_ONE_MINUS_ONE, 1, EffectTarget.ContextTarget(0))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "251"
        artist = "Ron Brown"
        flavorText = "\"Bits of fallen scarecrow, laces of elfskin leather, teeth of an axeshark merrow . . . An industrious soul can find new uses for the most mundane items.\"\n" +
            "—Mowagh the Gwyllion"
        imageUri = "https://cards.scryfall.io/normal/front/e/6/e6aee954-2a04-425d-8d05-09dad58af656.jpg?1783942712"
    }
}
