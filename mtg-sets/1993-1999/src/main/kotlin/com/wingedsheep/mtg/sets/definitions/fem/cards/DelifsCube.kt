package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.effects.CreateDelayedTriggerEffect
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import com.wingedsheep.sdk.scripting.effects.GrantKeywordEffect
import com.wingedsheep.sdk.scripting.effects.RegenerateEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Delif's Cube
 * {1}
 * Artifact
 * {2}, {T}: This turn, when target creature you control attacks and isn't blocked, it assigns no
 * combat damage this turn and you put a cube counter on this artifact.
 * {2}, Remove a cube counter from this artifact: Regenerate target creature.
 *
 * [DelifsCone]'s reusable sibling: the same one-shot delayed trigger, but the payoff is a charge
 * on the Cube rather than life, and the rider is mandatory rather than optional. The counters are
 * spent by the second ability, which needs no tap and so can be used the same turn.
 */
val DelifsCube = card("Delif's Cube") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{2}, {T}: This turn, when target creature you control attacks and isn't " +
        "blocked, it assigns no combat damage this turn and you put a cube counter on this artifact.\n" +
        "{2}, Remove a cube counter from this artifact: Regenerate target creature."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap)
        val t = target(
            "target creature you control",
            TargetCreature(filter = TargetFilter(GameObjectFilter.Creature.youControl()))
        )
        effect = CreateDelayedTriggerEffect(
            trigger = Triggers.AttacksAndIsntBlocked.copy(binding = TriggerBinding.ANY),
            watchedTarget = t,
            effect = Effects.Composite(
                GrantKeywordEffect(
                    AbilityFlag.ASSIGNS_NO_COMBAT_DAMAGE.name,
                    EffectTarget.TriggeringEntity,
                    Duration.EndOfTurn,
                ),
                Effects.AddCounters(Counters.CUBE, 1, EffectTarget.Self),
            ),
            expiry = DelayedTriggerExpiry.EndOfTurn,
            fireOnce = true,
        )
        description = "{2}, {T}: This turn, when target creature you control attacks and isn't blocked, it assigns no combat damage this turn and you put a cube counter on this artifact."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.RemoveCounterFromSelf(Counters.CUBE, 1))
        val t = target("target creature", TargetCreature(filter = TargetFilter.Creature))
        effect = RegenerateEffect(t)
        description = "{2}, Remove a cube counter from this artifact: Regenerate target creature."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "85"
        artist = "Mark Tedin"
        imageUri = "https://cards.scryfall.io/normal/front/1/4/14749600-9eca-4122-b04f-30ddda091b74.jpg?1783947881"
    }
}
