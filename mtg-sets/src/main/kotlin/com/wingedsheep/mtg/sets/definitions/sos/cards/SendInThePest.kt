package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Send in the Pest
 * {1}{B}
 * Sorcery
 *
 * Each opponent discards a card. You create a 1/1 black and green Pest creature token
 * with "Whenever this token attacks, you gain 1 life."
 *
 * The created Pest carries its own self-attack life-gain trigger, granted via
 * [CreateTokenEffect.triggeredAbilities] — a `Triggers.Attacks` (SELF binding) event with
 * `Effects.GainLife(1)` as the payoff, gained by the token's controller.
 */
val SendInThePest = card("Send in the Pest") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Each opponent discards a card. You create a 1/1 black and green Pest creature " +
        "token with \"Whenever this token attacks, you gain 1 life.\""

    spell {
        effect = Effects.EachOpponentDiscards(1)
            .then(
                CreateTokenEffect(
                    power = 1,
                    toughness = 1,
                    colors = setOf(Color.BLACK, Color.GREEN),
                    creatureTypes = setOf("Pest"),
                    triggeredAbilities = listOf(
                        TriggeredAbility.create(
                            trigger = Triggers.Attacks.event,
                            binding = Triggers.Attacks.binding,
                            effect = Effects.GainLife(1)
                        )
                    ),
                    imageUri = "https://cards.scryfall.io/normal/front/b/a/ba854032-6ad2-4654-990a-64006e7f92fd.jpg?1777982237"
                )
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "100"
        artist = "Raluca Marinescu"
        flavorText = "Dara couldn't cover her Silverquill bullies in ink but she could do the next best thing."
        imageUri = "https://cards.scryfall.io/normal/front/2/8/283b508b-89f0-4c23-9686-b049e402b73c.jpg?1775937607"
    }
}
