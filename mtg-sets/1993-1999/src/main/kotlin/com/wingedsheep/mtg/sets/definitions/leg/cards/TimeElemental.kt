package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.StatePredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Time Elemental
 * {2}{U}
 * Creature — Elemental
 * 0/2
 * When this creature attacks or blocks, at end of combat, sacrifice it and it deals 5 damage to you.
 * {2}{U}{U}, {T}: Return target permanent that isn't enchanted to its owner's hand.
 *
 * "Attacks or blocks" is the Mardu Blazebringer pair: two triggers, each scheduling the same delayed
 * end-of-combat effect. The sacrifice is a no-op when you no longer control it, but the 5 damage is
 * still dealt (Gatherer ruling) — the damage is sourced from the delayed trigger's source, so it
 * resolves even once the Elemental is in the graveyard. "Isn't enchanted" is the Brainspoil filter
 * (no Auras attached).
 */
private val sacrificeAndBurnAtEndOfCombat = Effects.CreateDelayedTrigger(
    step = Step.END_COMBAT,
    effect = SacrificeSelfEffect then Effects.DealDamage(5, EffectTarget.Controller),
)

val TimeElemental = card("Time Elemental") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Elemental"
    power = 0
    toughness = 2
    oracleText = "When this creature attacks or blocks, at end of combat, sacrifice it and it deals 5 damage to you.\n" +
        "{2}{U}{U}, {T}: Return target permanent that isn't enchanted to its owner's hand."

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = sacrificeAndBurnAtEndOfCombat
    }

    triggeredAbility {
        trigger = Triggers.self.blocks()
        effect = sacrificeAndBurnAtEndOfCombat
    }

    activatedAbility {
        val permanent = target(
            TargetFilter(
                GameObjectFilter.Permanent.copy(
                    statePredicates = listOf(StatePredicate.Not(StatePredicate.IsEnchanted))
                )
            )
        )
        cost = Costs.Composite(Costs.Mana("{2}{U}{U}"), Costs.Tap)
        effect = Effects.ReturnToHand(permanent)
        description = "{2}{U}{U}, {T}: Return target permanent that isn't enchanted to its owner's hand."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "81"
        artist = "Amy Weber"
        imageUri = "https://cards.scryfall.io/normal/front/6/1/61510e88-97d0-410a-9431-ebf12990e33d.jpg?1783948070"
    }
}
