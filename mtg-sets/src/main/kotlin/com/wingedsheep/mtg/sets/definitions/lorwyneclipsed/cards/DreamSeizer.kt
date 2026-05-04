package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.MayEffect

/**
 * Dream Seizer
 * {3}{B}
 * Creature — Faerie Rogue
 * 3/2
 *
 * Flying
 * When this creature enters, you may blight 1. If you do, each opponent discards a card.
 */
val DreamSeizer = card("Dream Seizer") {
    manaCost = "{3}{B}"
    typeLine = "Creature — Faerie Rogue"
    power = 3
    toughness = 2
    oracleText = "Flying\n" +
        "When this creature enters, you may blight 1. If you do, each opponent discards a card. " +
        "(To blight 1, put a -1/-1 counter on a creature you control.)"

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = MayEffect(
            effect = EffectPatterns.blight(1)
                .then(Effects.EachOpponentDiscards(1)),
            descriptionOverride = "You may blight 1. If you do, each opponent discards a card."
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "101"
        artist = "Omar Rayyan"
        flavorText = "\"Every fae has their preference. Mine are the dreams most bitter and pungent.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/5/3573f425-9251-4c17-9619-15278ce5d8fb.jpg?1767658140"
    }
}
