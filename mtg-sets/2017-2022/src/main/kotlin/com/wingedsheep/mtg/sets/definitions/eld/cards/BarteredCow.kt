package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Bartered Cow — Throne of Eldraine #6
 * {3}{W} · Creature — Ox · 3/3
 *
 * When this creature dies and when you discard this card, create a Food token.
 *
 * "Dies and when you discard" is two trigger events sharing one effect, so it is two triggered
 * abilities: a battlefield `dies()` trigger and a `isDiscarded()` trigger that functions from hand
 * and fires on any discard of this card.
 */
val BarteredCow = card("Bartered Cow") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Ox"
    power = 3
    toughness = 3
    oracleText = "When this creature dies and when you discard this card, create a Food token. " +
        "(It's an artifact with \"{2}, {T}, Sacrifice this token: You gain 3 life.\")"

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.CreateFood()
    }

    triggeredAbility {
        trigger = Triggers.self.isDiscarded()
        effect = Effects.CreateFood()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "6"
        artist = "Deruchenko Alexander"
        flavorText = "At the market, no one heeded Hilda's frantic mooing. The fae curse was turning " +
            "out even worse than she had feared."
        imageUri = "https://cards.scryfall.io/normal/front/1/d/1de43c67-7dfe-4282-b433-4e394366d2e9.jpg?1783932677"
        ruling(
            "2019-10-04",
            "Bartered Cow's ability is a triggered ability, not an activated ability. It doesn't allow " +
                "you to discard it or sacrifice it whenever you want; rather, you need some other way " +
                "of discarding it or causing it to die."
        )
    }
}
