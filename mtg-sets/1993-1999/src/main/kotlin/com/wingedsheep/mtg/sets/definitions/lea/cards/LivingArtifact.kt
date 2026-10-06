package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Living Artifact — Limited Edition Alpha #208
 * {G} · Enchantment — Aura
 *
 * Enchant artifact
 * Whenever you're dealt damage, put that many vitality counters on this Aura.
 * At the beginning of your upkeep, you may remove a vitality counter from this Aura.
 * If you do, you gain 1 life.
 *
 * The same shape as Sun Droplet, on an Aura and on *your* upkeep only. "You" is the Aura's
 * controller; "that many" reads the damage off the trigger context. The "may … if you do" is a
 * [Effects.May] over remove-then-gain, gated on the Aura actually holding a vitality counter so an
 * empty Aura doesn't ask an unanswerable question each upkeep.
 */
val LivingArtifact = card("Living Artifact") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant artifact\n" +
        "Whenever you're dealt damage, put that many vitality counters on this Aura.\n" +
        "At the beginning of your upkeep, you may remove a vitality counter from this Aura. " +
        "If you do, you gain 1 life."

    auraTarget = TargetObject(filter = TargetFilter.Artifact)

    triggeredAbility {
        trigger = Triggers.you.isDealtDamage()
        effect = Effects.AddDynamicCounters(
            counterType = CounterType.VITALITY,
            amount = DynamicAmounts.triggerDamageAmount(),
            target = EffectTarget.Self,
        )
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.If(
            condition = Conditions.SourceHasCounter(CounterType.VITALITY),
            then = Effects.May(
                Effects.RemoveCounters(CounterType.VITALITY, 1, EffectTarget.Self) then Effects.GainLife(1),
                descriptionOverride = "Remove a vitality counter from Living Artifact to gain 1 life?",
            ),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "208"
        artist = "Anson Maddocks"
        imageUri = "https://cards.scryfall.io/normal/front/c/9/c9e753a2-a7d0-4d37-ae65-b5a1b5039a6e.jpg?1783948675"
        ruling("2004-10-04", "You can cast it targeting your opponent's artifacts. The controller of the Aura (not the controller of the artifact) controls the Living Artifact ability.")
        ruling("2004-10-04", "Does not trigger on loss of life, just on damage.")
    }
}
