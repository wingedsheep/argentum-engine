package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * The Wretched
 * {3}{B}{B}
 * Creature — Demon
 * 2/5
 * At end of combat, gain control of all creatures blocking this creature for as long as you
 * control this creature.
 *
 * Clockwork Avian's `Triggers.anyPlayer.beginningOf(Step.END_COMBAT)` trigger (creatures stay in
 * combat until the end of combat step ends, CR 511.3), over Battle-Scarred Goblin's
 * `blockingSource()` group — read as the ability resolves, so a blocker removed from combat
 * earlier is not taken. Each blocker is stolen with Old Man of the Sea's
 * [Duration.WhileYouControlSource] gate: the control ends for good when The Wretched leaves the
 * battlefield or changes controller.
 */
val TheWretched = card("The Wretched") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Demon"
    power = 2
    toughness = 5
    oracleText = "At end of combat, gain control of all creatures blocking this creature for as long " +
        "as you control this creature."

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.END_COMBAT)
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.blockingSource()),
            Effects.GainControl(EffectTarget.IterationEntity, Duration.WhileYouControlSource("The Wretched")),
        )
        description = "At end of combat, gain control of all creatures blocking this creature for as " +
            "long as you control this creature."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "121"
        artist = "Christopher Rush"
        imageUri = "https://cards.scryfall.io/normal/front/1/4/14c45416-a826-42e9-9967-8838158cf16d.jpg?1783948062"
    }
}
