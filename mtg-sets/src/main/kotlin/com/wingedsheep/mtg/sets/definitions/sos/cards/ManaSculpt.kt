package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.CreateDelayedTriggerEffect
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerTiming
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mana Sculpt
 * {1}{U}{U}
 * Instant
 * Counter target spell. If you control a Wizard, add an amount of {C} equal to the amount of mana
 * spent to cast that spell at the beginning of your next main phase.
 *
 * The Wizard rider creates a delayed triggered ability ("at the beginning of your next main phase,
 * add an amount of {C} equal to the amount of mana spent to cast that spell"). "That spell" is the
 * countered spell — gone from the stack by the time the delayed trigger fires — so the amount must
 * be captured at resolution. The delayed-trigger payoff is authored as
 * `AddColorlessMana(targetManaSpent(0))`; [CreateDelayedTriggerExecutor] snapshots that dynamic
 * amount into a [com.wingedsheep.sdk.scripting.values.DynamicAmount.Fixed] literal while the target
 * spell is still on the stack. For that snapshot to read the real amount, the gated delayed-trigger
 * creation is sequenced **before** the counter (the target spell still carries its
 * `SpellOnStackComponent` payment buckets at that point).
 *
 * "Your next main phase" is modeled as the controller's next precombat main phase
 * ([Step.PRECOMBAT_MAIN] gated to [Player.You] via [DelayedTriggerTiming.CURRENT_TURN_OR_LATER]) —
 * the common reading for a "next main phase" mana payoff.
 */
val ManaSculpt = card("Mana Sculpt") {
    manaCost = "{1}{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target spell. If you control a Wizard, add an amount of {C} equal to the " +
        "amount of mana spent to cast that spell at the beginning of your next main phase."

    spell {
        target = Targets.Spell
        effect = ConditionalEffect(
            condition = Conditions.YouControl(GameObjectFilter.Creature.withSubtype("Wizard")),
            effect = CreateDelayedTriggerEffect(
                step = Step.PRECOMBAT_MAIN,
                fireOnPlayer = EffectTarget.PlayerRef(Player.You),
                timing = DelayedTriggerTiming.CURRENT_TURN_OR_LATER,
                effect = Effects.AddColorlessMana(DynamicAmounts.targetManaSpent(0)),
            ),
        ) then Effects.CounterSpell()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "57"
        artist = "Cristi Balanescu"
        flavorText = "The harder a foe strikes, the stronger a Dragonsguard's defenses become."
        imageUri = "https://cards.scryfall.io/normal/front/2/0/200c8e3d-c53b-40c7-a29a-fccc1281bfc6.jpg"
    }
}
