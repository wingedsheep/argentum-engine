package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Brotherhood Scribe — Fallout #13.
 * {1}{W} · Creature — Human Artificer · 1/3
 *
 * Metalcraft — {T}: You get {E}. Activate only if you control three or more artifacts.
 * Whenever you get one or more {E} during your turn, creatures you control get +1/+1 until end of turn.
 *
 * Metalcraft is an ability word: the activation gate is an ordinary
 * [ActivationRestriction.OnlyIfCondition] over [Conditions.YouControlAtLeast] (Mox Opal's shape).
 * The energy trigger is the player-keyed `Triggers.you.getsCounters` (one grant of several {E}
 * fires it once), narrowed by "during your turn" — a `triggerRestriction`, not an intervening-if.
 * The pump locks in the creatures you control as it resolves (ForEachInGroup).
 */
val BrotherhoodScribe = card("Brotherhood Scribe") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Artificer"
    power = 1
    toughness = 3
    oracleText = "Metalcraft — {T}: You get {E} (an energy counter). Activate only if you control three " +
        "or more artifacts.\n" +
        "Whenever you get one or more {E} during your turn, creatures you control get +1/+1 until end of turn."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.GetEnergy(1)
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(
                Conditions.YouControlAtLeast(3, GameObjectFilter.Artifact)
            )
        )
        description = "Metalcraft — {T}: You get {E}. Activate only if you control three or more artifacts."
    }

    triggeredAbility {
        trigger = Triggers.you.getsCounters(CounterType.ENERGY)
        triggerRestriction = Conditions.IsYourTurn
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.youControl()),
            Effects.ModifyStats(1, 1, EffectTarget.IterationEntity)
        )
        description = "Whenever you get one or more {E} during your turn, creatures you control get " +
            "+1/+1 until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "13"
        artist = "Jarel Threat"
        imageUri = "https://cards.scryfall.io/normal/front/b/7/b751250e-ebf2-42b1-9a45-779b39974ba7.jpg?1783912418"
    }
}
