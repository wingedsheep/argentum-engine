package com.wingedsheep.mtg.sets.definitions.dsk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ChooseActionEffect
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.effects.EffectChoice
import com.wingedsheep.sdk.scripting.effects.FeasibilityCheck
import com.wingedsheep.sdk.scripting.effects.ForEachPlayerEffect
import com.wingedsheep.sdk.scripting.effects.ForceSacrificeEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Osseous Sticktwister
 * {1}{B}
 * Artifact Creature — Scarecrow
 * 2/2
 *
 * Lifelink
 * Delirium — At the beginning of your end step, if there are four or more card types among cards in
 * your graveyard, each opponent may sacrifice a nonland permanent of their choice or discard a card.
 * Then this creature deals damage equal to its power to each opponent who didn't sacrifice a
 * permanent or discard a card this way.
 *
 * Composition: a `YourEndStep` trigger with a Delirium intervening-if, whose body iterates each
 * opponent (`ForEachPlayerEffect(EachOpponent)`) and presents that opponent (rebound to
 * `Player.You`/`Controller` inside the iteration) a [ChooseActionEffect]. The first two choices —
 * sacrifice a nonland permanent / discard a card — are feasibility-gated; the third, "take damage",
 * is the "didn't sacrifice or discard this way" branch and deals damage equal to Osseous
 * Sticktwister's power. Leaving the damage source implicit attributes it to the creature, so its
 * lifelink applies. If an opponent can neither sacrifice nor discard, both feasibility checks hide
 * those options and the damage option auto-selects (CR: they simply take the damage).
 */
val OsseousSticktwister = card("Osseous Sticktwister") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Artifact Creature — Scarecrow"
    power = 2
    toughness = 2
    oracleText = "Lifelink\n" +
        "Delirium — At the beginning of your end step, if there are four or more card types among " +
        "cards in your graveyard, each opponent may sacrifice a nonland permanent of their choice or " +
        "discard a card. Then this creature deals damage equal to its power to each opponent who " +
        "didn't sacrifice a permanent or discard a card this way."

    keywords(Keyword.LIFELINK)

    triggeredAbility {
        trigger = Triggers.YourEndStep
        // Delirium intervening-if (CR 603.4): four or more card types in your graveyard.
        triggerCondition = Conditions.Delirium()
        effect = ForEachPlayerEffect(
            players = Player.EachOpponent,
            effects = listOf(
                ChooseActionEffect(
                    // Inside the per-player iteration the controller is rebound to the iterated
                    // opponent, so `Player.You` / `EffectTarget.Controller` is that opponent.
                    player = EffectTarget.Controller,
                    choices = listOf(
                        EffectChoice(
                            label = "Sacrifice a nonland permanent",
                            effect = ForceSacrificeEffect(
                                filter = GameObjectFilter.NonlandPermanent,
                                count = 1,
                                target = EffectTarget.Controller,
                            ),
                            feasibilityCheck = FeasibilityCheck.ControlsPermanentMatching(
                                GameObjectFilter.NonlandPermanent
                            ),
                        ),
                        EffectChoice(
                            label = "Discard a card",
                            effect = Patterns.Hand.discardCards(1, EffectTarget.Controller),
                            feasibilityCheck = FeasibilityCheck.HasCardsInZone(Zone.HAND),
                        ),
                        EffectChoice(
                            label = "Take damage equal to Osseous Sticktwister's power",
                            // damageSource left implicit → attributed to Osseous Sticktwister, so
                            // its lifelink gains its controller that much life.
                            effect = DealDamageEffect(
                                amount = DynamicAmounts.sourcePower(),
                                target = EffectTarget.Controller,
                            ),
                        ),
                    ),
                )
            ),
        )
        description = "Delirium — At the beginning of your end step, if there are four or more card " +
            "types among cards in your graveyard, each opponent may sacrifice a nonland permanent of " +
            "their choice or discard a card. Then this creature deals damage equal to its power to " +
            "each opponent who didn't sacrifice a permanent or discard a card this way."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "112"
        artist = "Mathias Kollros"
        imageUri = "https://cards.scryfall.io/normal/front/4/f/4f43473e-1302-4543-b331-1a86cfbc3ced.jpg?1726286266"
    }
}
