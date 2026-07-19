package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Machinesmith Automaton
 * {2}{R}
 * Artifact Creature — Robot Villain
 * 2/2
 *
 * Trample
 * Whenever another artifact you control enters, put a +1/+1 counter on this creature.
 *
 * Implementation notes:
 * - "another" is [TriggerBinding.OTHER], which excludes the source's own entry — so the
 *   Automaton doesn't trigger off itself.
 * - The trigger is per-permanent: several artifacts entering at once each fire it.
 */
val MachinesmithAutomaton = card("Machinesmith Automaton") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Artifact Creature — Robot Villain"
    oracleText = "Trample\n" +
        "Whenever another artifact you control enters, put a +1/+1 counter on this creature."
    power = 2
    toughness = 2

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.entersBattlefield(
            filter = GameObjectFilter.Artifact.youControl(),
            binding = TriggerBinding.OTHER
        )
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "144"
        artist = "Wero Gallo"
        flavorText = "\"Every day, I find new ways to improve upon my designs. This is the " +
            "inherent superiority of metal over flesh and blood.\"\n—Machinesmith, Samuel Saxon"
        imageUri = "https://cards.scryfall.io/normal/front/8/0/802e663e-9529-46da-8af4-eb9ca07971b1.jpg?1783902927"
    }
}
