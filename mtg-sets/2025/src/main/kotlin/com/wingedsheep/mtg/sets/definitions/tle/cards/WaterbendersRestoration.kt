package com.wingedsheep.mtg.sets.definitions.tle.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Waterbender's Restoration
 * {U}{U}
 * Instant — Lesson
 *
 * As an additional cost to cast this spell, waterbend {X}. (While paying a waterbend cost, you can
 * tap your artifacts and creatures to help. Each one pays for {1}.)
 * Exile X target creatures you control. Return those cards to the battlefield under their owner's
 * control at the beginning of the next end step.
 *
 * The cost is the spell-level `waterbendCost(isX = true)` (CR 701.67a/b — taps pay only the X),
 * and X sizes the targeting through `dynamicMaxCount = X`, the same pairing Foggy Swamp Visions
 * and Crashing Wave use. The body is Eerie Interlude's per-target blink: each target is exiled
 * and gets its own delayed "return it at the beginning of the next end step" trigger, so a target
 * that became illegal (and was therefore never exiled) is not returned.
 */
val WaterbendersRestoration = card("Waterbender's Restoration") {
    manaCost = "{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant — Lesson"
    oracleText = "As an additional cost to cast this spell, waterbend {X}. (While paying a waterbend " +
        "cost, you can tap your artifacts and creatures to help. Each one pays for {1}.)\n" +
        "Exile X target creatures you control. Return those cards to the battlefield under their " +
        "owner's control at the beginning of the next end step."

    waterbendCost(isX = true)

    spell {
        targets(TargetFilter.Creature.youControl(), optional = true, dynamicMaxCount = DynamicAmounts.xValue())
        effect = Effects.ForEachTarget(
            Effects.Move(EffectTarget.ContextTarget(0), Zone.EXILE),
            Effects.CreateDelayedTrigger(
                step = Step.END,
                effect = Effects.Move(
                    target = EffectTarget.ContextTarget(0),
                    destination = Zone.BATTLEFIELD,
                ),
            ),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "99"
        artist = "Phima"
        imageUri = "https://cards.scryfall.io/normal/front/f/e/fe68e08e-8d84-4f87-b2e9-b552788d78e7.jpg?1783904827"
        ruling("2025-10-02", "If a waterbend cost is part of the total cost to cast a spell or activate an ability, you may tap an untapped artifact or creature you control rather than pay one generic mana in that total cost any number of times up to a maximum of the amount of generic mana in the waterbend component of that total cost.")
        ruling("2025-10-02", "You can tap any untapped creature or artifact you control to pay a waterbend cost, even one you haven't controlled continuously since the beginning of your most recent turn.")
    }
}
