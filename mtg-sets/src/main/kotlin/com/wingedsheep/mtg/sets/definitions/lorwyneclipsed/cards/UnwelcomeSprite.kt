package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Unwelcome Sprite
 * {1}{U}
 * Creature — Faerie Rogue
 * 2/1
 *
 * Flying
 * Whenever you cast a spell during an opponent's turn, surveil 2.
 */
val UnwelcomeSprite = card("Unwelcome Sprite") {
    manaCost = "{1}{U}"
    typeLine = "Creature — Faerie Rogue"
    power = 2
    toughness = 1
    oracleText = "Flying\nWhenever you cast a spell during an opponent's turn, surveil 2. " +
        "(Look at the top two cards of your library. You may put any number of them into " +
        "your graveyard and the rest on top of your library in any order.)"

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.YouCastSpell
        triggerCondition = Conditions.IsNotYourTurn
        effect = EffectPatterns.surveil(2)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "81"
        artist = "Iris Compiet"
        imageUri = "https://cards.scryfall.io/normal/front/9/0/902f8d86-fde2-4cdf-88f0-bf63d616f3af.jpg?1767732556"
    }
}
