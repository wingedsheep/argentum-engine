package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.ForEachTargetEffect
import com.wingedsheep.sdk.scripting.effects.GrantTriggeredAbilityEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Rabid Attack
 * {1}{B}
 * Instant
 * Until end of turn, any number of target creatures you control each get +1/+0 and gain
 * "When this creature dies, draw a card."
 *
 * "Any number of target creatures you control" uses `unlimited = true` (no cap; the client
 * offers every legal target). [ForEachTargetEffect] applies the same per-target effects to
 * each chosen creature: a +1/+0 buff and a granted dies-trigger that draws a card. The
 * granted triggered ability is scoped to the creature via [GrantTriggeredAbilityEffect.target]
 * = `EffectTarget.ContextTarget(0)` (the current iteration's target), so each creature's own
 * death — not any creature's — draws for that creature's controller.
 */
val RabidAttack = card("Rabid Attack") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Until end of turn, any number of target creatures you control each get +1/+0 " +
        "and gain \"When this creature dies, draw a card.\""

    spell {
        target = TargetCreature(filter = TargetFilter.Creature.youControl(), unlimited = true)
        effect = ForEachTargetEffect(
            listOf(
                Effects.ModifyStats(1, 0, EffectTarget.ContextTarget(0)),
                GrantTriggeredAbilityEffect(
                    ability = TriggeredAbility.create(
                        trigger = Triggers.Dies.event,
                        binding = Triggers.Dies.binding,
                        effect = Effects.DrawCards(1),
                        descriptionOverride = "When this creature dies, draw a card.",
                    ),
                    target = EffectTarget.ContextTarget(0),
                ),
            ),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "96"
        artist = "Izzy"
        flavorText = "\"Let me guess, the scurrids got to you lot?\"\n—Witherbloom infirmary attendant"
        imageUri = "https://cards.scryfall.io/normal/front/f/5/f5e67560-3135-4b27-a344-5859edf8bcd9.jpg?1775937579"
    }
}
