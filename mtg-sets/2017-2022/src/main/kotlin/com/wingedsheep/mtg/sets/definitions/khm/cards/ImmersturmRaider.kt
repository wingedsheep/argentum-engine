package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Immersturm Raider
 * {1}{R}
 * Creature — Demon Berserker
 * 2/1
 * When this creature enters, you may discard a card. If you do, draw a card.
 *
 * The optional rummage is Plundering Predator's spelling: [Effects.May] around
 * [Effects.IfYouDo] with a one-card hand discard as the action and the draw as its payoff.
 */
val ImmersturmRaider = card("Immersturm Raider") {
    manaCost = "{1}{R}"
    typeLine = "Creature — Demon Berserker"
    power = 2
    toughness = 1
    oracleText = "When this creature enters, you may discard a card. If you do, draw a card."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.May(
            Effects.IfYouDo(Patterns.Hand.discardCards(1), Effects.DrawCards(1))
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "141"
        artist = "Grzegorz Rutkowski"
        flavorText = "The demons took no captives and claimed no spoils; they sought only to destroy."
        imageUri = "https://cards.scryfall.io/normal/front/7/7/77da4de2-4d34-40e5-8fc0-850aef05356b.jpg?1783928227"
    }
}
