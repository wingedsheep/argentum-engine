package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Triassic Egg — Legends #297
 * {4} · Artifact
 *
 * {3}, {T}: Put a hatchling counter on this artifact.
 * Sacrifice this artifact: Choose one. Activate only if there are two or more hatchling counters
 * on this artifact.
 * • You may put a creature card from your hand onto the battlefield.
 * • Return target creature card from your graveyard to the battlefield.
 *
 * The sacrifice ability has no {T}, so it can be activated while the Egg is tapped (2004-10-04
 * ruling). A modal *ability*, not a modal spell, so `countsAsModalSpell = false`.
 */
val TriassicEgg = card("Triassic Egg") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{3}, {T}: Put a hatchling counter on this artifact.\n" +
        "Sacrifice this artifact: Choose one. Activate only if there are two or more hatchling " +
        "counters on this artifact.\n" +
        "• You may put a creature card from your hand onto the battlefield.\n" +
        "• Return target creature card from your graveyard to the battlefield."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}"), Costs.Tap)
        effect = Effects.AddCounters(CounterType.HATCHLING, 1, EffectTarget.Self)
        description = "{3}, {T}: Put a hatchling counter on this artifact."
    }

    activatedAbility {
        cost = Costs.SacrificeSelf
        effect = ModalEffect.chooseOne(
            Mode.noTarget(
                Patterns.Hand.putFromHand(
                    filter = GameObjectFilter.Creature,
                    count = 1,
                    prompt = "Put a creature card from your hand onto the battlefield?",
                ),
                "You may put a creature card from your hand onto the battlefield"
            ),
            mode("Return target creature card from your graveyard to the battlefield") {
                val creatureCard = target(TargetFilter.CreatureInYourGraveyard)
                effect = Effects.PutOntoBattlefieldFromGraveyard(creatureCard)
            },
            countsAsModalSpell = false
        )
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(
                Conditions.SourceCounterCountAtLeast(CounterType.HATCHLING, 2)
            )
        )
        description = "Sacrifice this artifact: Put a creature card from your hand onto the " +
            "battlefield, or return target creature card from your graveyard to the battlefield."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "297"
        artist = "Dan Frazier"
        imageUri = "https://cards.scryfall.io/normal/front/c/e/ce3756f7-0d99-4562-b32d-66de18a58fdf.jpg?1783948024"
        ruling("2004-10-04", "You can use the sacrifice ability even if it is tapped.")
    }
}
