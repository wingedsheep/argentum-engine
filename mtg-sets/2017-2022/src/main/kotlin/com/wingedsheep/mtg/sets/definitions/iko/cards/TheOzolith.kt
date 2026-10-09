package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * The Ozolith — Ikoria: Lair of Behemoths #237
 * {1} · Legendary Artifact
 *
 * Whenever a creature you control leaves the battlefield, if it had counters on it, put those
 * counters on The Ozolith.
 * At the beginning of combat on your turn, if The Ozolith has counters on it, you may move all
 * counters from The Ozolith onto target creature.
 *
 * The first ability is Host of the Hereafter's shape over any zone: the leaving creature is gone,
 * so `TriggeringEntityHadCounters` and `MoveAllLastKnownCounters` read its last-known counters.
 * The second is a live move — `MoveAllCounters` carries every kind, and `May` wraps the whole move
 * (all or nothing, never a partial one).
 */
val TheOzolith = card("The Ozolith") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Legendary Artifact"
    oracleText = "Whenever a creature you control leaves the battlefield, if it had counters on it, " +
        "put those counters on The Ozolith.\n" +
        "At the beginning of combat on your turn, if The Ozolith has counters on it, you may move all " +
        "counters from The Ozolith onto target creature."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).leaves()
        interveningIf = Conditions.TriggeringEntityHadCounters
        effect = Effects.MoveAllLastKnownCounters(EffectTarget.Self)
    }

    triggeredAbility {
        val creature = target(TargetFilter.Creature)
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        interveningIf = Conditions.SourceHasCounter(null)
        effect = Effects.May(Effects.MoveAllCounters(EffectTarget.Self, creature))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "237"
        artist = "Sam Burley"
        imageUri = "https://cards.scryfall.io/normal/front/9/3/9341ed06-53db-4604-b60a-3ea9129afbc2.jpg?1783931006"
    }
}
