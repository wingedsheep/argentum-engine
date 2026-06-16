package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect

/**
 * Essenceknit Scholar
 * {B}{B/G}{G}
 * Creature — Dryad Warlock
 * 3/1
 *
 * When this creature enters, create a 1/1 black and green Pest creature token with
 * "Whenever this token attacks, you gain 1 life."
 * At the beginning of your end step, if a creature died under your control this turn, draw a card.
 *
 * Two triggered abilities: the enters trigger creates one Pest with its self-attack life-gain
 * trigger (`Effects.GainLife(1)`), and the end-step trigger draws a card gated by the intervening-if
 * [Conditions.ControlledCreatureDiedThisTurn].
 */
val EssenceknitScholar = card("Essenceknit Scholar") {
    manaCost = "{B}{B/G}{G}"
    colorIdentity = "BG"
    typeLine = "Creature — Dryad Warlock"
    power = 3
    toughness = 1
    oracleText = "When this creature enters, create a 1/1 black and green Pest creature token " +
        "with \"Whenever this token attacks, you gain 1 life.\"\n" +
        "At the beginning of your end step, if a creature died under your control this turn, " +
        "draw a card."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = CreateTokenEffect(
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
        description = "When this creature enters, create a 1/1 black and green Pest creature " +
            "token with \"Whenever this token attacks, you gain 1 life.\""
    }

    triggeredAbility {
        trigger = Triggers.YourEndStep
        triggerCondition = Conditions.ControlledCreatureDiedThisTurn
        effect = Effects.DrawCards(1)
        description = "At the beginning of your end step, if a creature died under your control " +
            "this turn, draw a card."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "187"
        artist = "Ioannis Fiore"
        flavorText = "\"Life and death is a complex pattern that needs careful stitching.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/a/2a3cba55-3fae-4d45-ae03-4d662ec13718.jpg?1775938295"
    }
}
