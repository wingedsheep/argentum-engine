package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AdditionalCost
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ChooseActionEffect
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.EffectChoice
import com.wingedsheep.sdk.scripting.effects.FeasibilityCheck
import com.wingedsheep.sdk.scripting.effects.ForEachPlayerEffect
import com.wingedsheep.sdk.scripting.effects.ForceSacrificeEffect
import com.wingedsheep.sdk.scripting.effects.LoseLifeEffect
import com.wingedsheep.sdk.scripting.effects.RepeatDynamicTimesEffect
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Rottenmouth Viper {5}{B}
 * Creature — Elemental Snake
 * 6/6
 *
 * As an additional cost to cast this spell, you may sacrifice any number of nonland
 * permanents. This spell costs {1} less to cast for each permanent sacrificed this way.
 * Whenever this creature enters or attacks, put a blight counter on it. Then for each
 * blight counter on it, each opponent loses 4 life unless that player sacrifices a
 * nonland permanent of their choice or discards a card.
 */
val RottenmouthViper = card("Rottenmouth Viper") {
    manaCost = "{5}{B}"
    typeLine = "Creature — Elemental Snake"
    power = 6
    toughness = 6
    oracleText = "As an additional cost to cast this spell, you may sacrifice any number of nonland permanents. " +
        "This spell costs {1} less to cast for each permanent sacrificed this way.\n" +
        "Whenever this creature enters or attacks, put a blight counter on it. Then for each blight counter on it, " +
        "each opponent loses 4 life unless that player sacrifices a nonland permanent of their choice or discards a card."

    additionalCost(
        AdditionalCost.SacrificeCreaturesForCostReduction(
            filter = GameObjectFilter.NonlandPermanent,
            costReductionPerCreature = 1
        )
    )

    // Whenever this creature enters the battlefield
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = rottenmouthViperEffect()
    }

    // Whenever this creature attacks
    triggeredAbility {
        trigger = Triggers.Attacks
        effect = rottenmouthViperEffect()
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "107"
        artist = "Andrea Piparo"
        imageUri = "https://cards.scryfall.io/normal/front/7/3/735e79b1-a3a9-4ddf-8bbc-f756c8a0452b.jpg?1721426484"

        ruling(
            "2024-07-26",
            "If Rottenmouth Viper leaves the battlefield before its triggered ability resolves, " +
                "use the number of blight counters that were on it as it last existed on the battlefield " +
                "to determine how many times each opponent must make the listed choice."
        )
        ruling(
            "2024-07-26",
            "An opponent can always choose not to sacrifice a nonland permanent or discard a card " +
                "(and therefore lose 4 life) even if they have cards in their hand or nonland permanents on the battlefield."
        )
    }
}

/**
 * Creates the triggered effect for Rottenmouth Viper:
 * Put a blight counter on it, then for each blight counter,
 * each opponent chooses: sacrifice nonland permanent, discard, or lose 4 life.
 */
private fun rottenmouthViperEffect(): Effect = CompositeEffect(
    listOf(
        // Step 1: Put a blight counter on Rottenmouth Viper
        Effects.AddCounters(Counters.BLIGHT, 1, EffectTarget.Self),

        // Step 2: For each blight counter, each opponent chooses
        RepeatDynamicTimesEffect(
            amount = DynamicAmounts.countersOnSelf(CounterTypeFilter.Named(Counters.BLIGHT)),
            body = ForEachPlayerEffect(
                players = Player.EachOpponent,
                effects = listOf(
                    ChooseActionEffect(
                        choices = listOf(
                            EffectChoice(
                                label = "Sacrifice a nonland permanent",
                                effect = ForceSacrificeEffect(
                                    filter = GameObjectFilter.NonlandPermanent,
                                    count = 1,
                                    target = EffectTarget.Controller
                                ),
                                feasibilityCheck = FeasibilityCheck.ControlsPermanentMatching(
                                    GameObjectFilter.NonlandPermanent
                                )
                            ),
                            EffectChoice(
                                label = "Discard a card",
                                effect = EffectPatterns.discardCards(1, EffectTarget.Controller),
                                feasibilityCheck = FeasibilityCheck.HasCardsInZone(Zone.HAND)
                            ),
                            EffectChoice(
                                label = "Lose 4 life",
                                effect = LoseLifeEffect(4, EffectTarget.Controller)
                            )
                        ),
                        player = EffectTarget.Controller
                    )
                )
            )
        )
    )
)
