package com.wingedsheep.mtg.sets.definitions.j25.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.StatePredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Slinza, the Spiked Stampede
 * {4}{G}
 * Legendary Creature — Beast
 * 5/5
 * Beast spells you cast cost {2} less to cast.
 * Each other Beast creature you control enters with an additional +1/+1 counter on it.
 * Whenever Slinza or another creature with power 4 or greater enters, you may pay {1}{R/G}.
 * When you do, Slinza fights target creature you don't control.
 *
 * - "Slinza or another creature with power 4 or greater": the power clause binds only to the
 *   other creature, so Slinza triggers on its own entry whatever its power. That is the
 *   heterogeneous union `Creature.IsSource or Creature.powerAtLeast(4)`, matched on any
 *   controller's creature (2024-11-08 ruling). The power is read from the projected state after
 *   entry, so the +1/+1 counter from the second ability counts toward the 4.
 */
val SlinzaTheSpikedStampede = card("Slinza, the Spiked Stampede") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Beast"
    power = 5
    toughness = 5
    oracleText = "Beast spells you cast cost {2} less to cast.\n" +
        "Each other Beast creature you control enters with an additional +1/+1 counter on it.\n" +
        "Whenever Slinza or another creature with power 4 or greater enters, you may pay {1}{R/G}. " +
        "When you do, Slinza fights target creature you don't control."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(GameObjectFilter.Any.withSubtype(Subtype.BEAST)),
            modification = CostModification.ReduceGeneric(2),
        )
    }

    replacementEffect(
        EntersWithCounters(
            counterType = CounterType.PLUS_ONE_PLUS_ONE,
            count = 1,
            otherOnly = true,
            appliesTo = EventPattern.ZoneChangeEvent(
                filter = GameObjectFilter.Creature.withSubtype(Subtype.BEAST).youControl(),
                to = Zone.BATTLEFIELD,
            ),
        )
    )

    triggeredAbility {
        trigger = Triggers.a(
            GameObjectFilter.Creature.withStatePredicate(StatePredicate.IsSource) or
                GameObjectFilter.Creature.powerAtLeast(4)
        ).enters()
        effect = Effects.ReflexiveTrigger(
            action = Effects.PayMana("{1}{R/G}"),
            optional = true,
        ) {
            val theirCreature = target(TargetFilter.CreatureOpponentControls)
            effect = Effects.Fight(EffectTarget.Self, theirCreature)
        }
        description = "Whenever Slinza or another creature with power 4 or greater enters, you may " +
            "pay {1}{R/G}. When you do, Slinza fights target creature you don't control."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "55"
        artist = "Ishikawa Kenta"
        imageUri = "https://cards.scryfall.io/normal/front/9/c/9c442742-902c-49ae-813e-38a21ff8a78f.jpg?1783908854"
        ruling(
            "2024-11-08",
            "Slinza's first ability can't reduce the amount of colored mana you pay for a spell. " +
                "It reduces only the generic mana component of that spell."
        )
        ruling(
            "2024-11-08",
            "Any Beasts that enter at the same time as Slinza won't enter with an additional +1/+1 counter."
        )
        ruling(
            "2024-11-08",
            "Slinza's last ability triggers whenever any creature with power 4 or greater enters, " +
                "not just one you control."
        )
        ruling(
            "2024-11-08",
            "If one or more static abilities that apply to a creature entering change its power, " +
                "those abilities are considered when determining whether Slinza's last ability " +
                "triggers. The same is true for replacement effects that apply to it, such as " +
                "entering with one or more +1/+1 counters (such as the effect of Slinza's second " +
                "ability, not coincidentally) or entering as a copy of another creature."
        )
        ruling(
            "2024-11-08",
            "If Slinza enters at the same time as one or more other creatures with power 4 or " +
                "greater, its last ability triggers once for each of those other creatures, plus " +
                "one more time for itself."
        )
    }
}
