package com.wingedsheep.mtg.sets.definitions.tmt.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.SneakCostWasPaid
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * The Last Ronin's Technique
 * {3}{W}
 * Instant
 *
 * Sneak {1}{W} (You may cast this spell for {1}{W} if you also return an
 * unblocked attacker you control to hand during the declare blockers step.)
 * Create three 1/1 white Ninja Turtle Spirit creature tokens. If this spell's
 * sneak cost was paid, they enter tapped and attacking.
 */
private const val NINJA_TURTLE_SPIRIT_TOKEN =
    "https://cards.scryfall.io/normal/front/9/8/98c32288-8676-44d0-b480-7eddcac1a0e4.jpg?1771590416"

private fun spiritTokens(tappedAndAttacking: Boolean) = CreateTokenEffect(
    count = DynamicAmount.Fixed(3),
    power = 1,
    toughness = 1,
    colors = setOf(Color.WHITE),
    creatureTypes = setOf("Ninja", "Turtle", "Spirit"),
    tapped = tappedAndAttacking,
    attacking = tappedAndAttacking,
    imageUri = NINJA_TURTLE_SPIRIT_TOKEN
)

val TheLastRoninsTechnique = card("The Last Ronin's Technique") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Sneak {1}{W} (You may cast this spell for {1}{W} if you also return an unblocked attacker you control to hand during the declare blockers step.)\nCreate three 1/1 white Ninja Turtle Spirit creature tokens. If this spell's sneak cost was paid, they enter tapped and attacking."

    sneak("{1}{W}")

    spell {
        effect = ConditionalEffect(
            condition = SneakCostWasPaid,
            effect = spiritTokens(tappedAndAttacking = true),
            elseEffect = spiritTokens(tappedAndAttacking = false)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "12"
        artist = "Adam Volker"
        flavorText = "They stood beside him. Whether they were figments or true spirits no longer mattered."
        imageUri = "https://cards.scryfall.io/normal/front/d/f/dfb18239-d373-4795-8598-c82abae2cb62.jpg?1771342217"
    }
}
