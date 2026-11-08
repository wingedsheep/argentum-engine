package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Whispers of Emrakul (Eldritch Moon #114)
 * {1}{B}
 * Sorcery
 *
 * Target opponent discards a card at random.
 * Delirium — If there are four or more card types among cards in your graveyard, that player
 * discards two cards at random instead.
 *
 * Delirium is checked as the spell resolves, while Whispers itself is still on the stack, so it
 * never counts its own sorcery type.
 */
val WhispersOfEmrakul = card("Whispers of Emrakul") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Target opponent discards a card at random.\nDelirium — If there are four or more card " +
        "types among cards in your graveyard, that player discards two cards at random instead."

    spell {
        val opponent = target(Targets.Opponent)
        effect = Effects.If(
            condition = Conditions.Delirium(),
            then = Patterns.Hand.discardRandom(2, opponent),
            otherwise = Patterns.Hand.discardRandom(1, opponent),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "114"
        artist = "Jason Rainville"
        flavorText = "Your thoughts are no longer your own."
        imageUri = "https://cards.scryfall.io/normal/front/0/b/0b7ba428-5a35-4062-8e16-39eb927cb316.jpg?1783937472"
    }
}
