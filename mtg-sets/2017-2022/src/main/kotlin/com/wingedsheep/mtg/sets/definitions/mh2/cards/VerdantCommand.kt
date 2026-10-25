package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Verdant Command
 * {1}{G}
 * Instant
 *
 * Choose two —
 * • Target player creates two tapped 1/1 green Squirrel creature tokens.
 * • Counter target loyalty ability of a planeswalker.
 * • Exile target card from a graveyard.
 * • Target player gains 3 life.
 *
 * The second mode targets with [TargetFilter.LoyaltyAbilityOnStack] (CR 606.2) narrowed to
 * abilities whose source is a planeswalker.
 */
val VerdantCommand = card("Verdant Command") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Choose two —\n" +
        "• Target player creates two tapped 1/1 green Squirrel creature tokens.\n" +
        "• Counter target loyalty ability of a planeswalker.\n" +
        "• Exile target card from a graveyard.\n" +
        "• Target player gains 3 life."

    spell {
        modal(chooseCount = 2) {
            mode("Target player creates two tapped 1/1 green Squirrel creature tokens") {
                val player = target(Targets.Player)
                effect = Effects.CreateToken(
                    power = 1,
                    toughness = 1,
                    colors = setOf(Color.GREEN),
                    creatureTypes = setOf("Squirrel"),
                    count = 2,
                    controller = player,
                    tapped = true
                )
            }
            mode("Counter target loyalty ability of a planeswalker") {
                target(TargetFilter.LoyaltyAbilityOnStack.abilitySourceMatches(GameObjectFilter.Planeswalker))
                effect = Effects.CounterAbility()
            }
            mode("Exile target card from a graveyard") {
                val card = target(TargetFilter.CardInGraveyard)
                effect = Effects.Exile(card)
            }
            mode("Target player gains 3 life") {
                val player = target(Targets.Player)
                effect = Effects.GainLife(3, player)
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "182"
        artist = "Mark Poole"
        imageUri = "https://cards.scryfall.io/normal/front/8/3/83031ea8-a6c9-4318-af16-bba701dd76bb.jpg?1783926823"
        ruling(
            "2021-06-18",
            "If the target for one mode has become illegal as Verdant Command tries to resolve, but it still " +
                "has at least one legal target, it will resolve and do as much as it can to the legal targets."
        )
    }
}
